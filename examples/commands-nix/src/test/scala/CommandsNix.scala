import org.scalacheck.Gen
import org.scalacheck.commands.Commands

import util.{Try, Success, Failure}

object CommandsNix extends org.scalacheck.Properties("CommandsNix") {

  property("machinespec") = MachineSpec.property()

}

object MachineSpec extends Commands {

  val con = new org.libvirt.Connect("qemu:///session")

  def runSshCmd(ip: String, cmd: String): Either[String, String] = {
    import scala.sys.process.*
    val err = new StringBuffer()
    val logger = ProcessLogger(err.append(_))

    val sshcmd =
      s"ssh -q -i test-key_rsa -l root -o UserKnownHostsFile=/dev/null " +
        s"-o StrictHostKeyChecking=no -o ConnectTimeout=1 ${ip}"

    if (s"$sshcmd true" ! logger != 0)
      throw new Exception(err.toString)
    else {
      val out = new StringBuffer()
      val err = new StringBuffer()
      val logger = ProcessLogger(out.append(_), err.append(_))
      if ((s"$sshcmd $cmd" ! logger) == 0) Right(out.toString)
      else Left(err.toString)
    }
  }

  def toNixNetwork(machines: State): String = {
    def mkConf(m: Machine): String = raw"""
      ${m.id} = { config, pkgs, lib, ... }: {
        imports = [ ./common.nix ];
        ${toNixMachine(m)}
      };
    """
    s"import ./qemu-network.nix { ${machines.map(mkConf).mkString} }"
  }

  def toNixMachine(m: Machine): String = raw"""
    deployment.libvirt = {
      netdevs.netdev0.mac = "$$MAC0";
      memory = ${m.memory};
      uuid = "${m.uuid}";
    };
    networking.hostName = "${m.id}";
    networking.interfaces.eth0 = {
      ipAddress = "${m.ip}";
      prefixLength = 24;
    };
    boot.kernelPackages =
      pkgs.linuxPackages_${m.kernelVer.replace('.', '_')};
  """

  def toLibvirtXMLs(machines: State): Map[String, String] = {
    import scala.sys.process.*
    import java.io.ByteArrayInputStream

    val out = new StringBuffer()
    val err = new StringBuffer()
    val logger = ProcessLogger(out.append(_), err.append(_))
    val is = new ByteArrayInputStream(toNixNetwork(machines).getBytes("UTF-8"))

    // Run nix-build and capture stdout and stderr
    "nix-build --no-out-link -" #< is ! logger

    val xmlFiles = Map(machines.map(m => m.id -> s"${out.toString.trim}/${m.id}.xml")*)

    // Check that all expected output files can be read
    xmlFiles.values foreach { f =>
      if (!(new java.io.File(f)).canRead) throw new Exception(raw"""
        No Libvirt XML produced (${f})
        out = ${out.toString}
        err = ${err.toString}
      """)
    }

    xmlFiles map { case (id, f) => id -> io.Source.fromFile(f).mkString }
  }

  case class Machine(
      id: String,
      uuid: java.util.UUID,
      ip: String,
      kernelVer: String,
      memory: Int,
      running: Boolean
  )

  // Machine.id mapped to a machine state
  type State = List[Machine]

  // Machine.id mapped to a LibVirt machine
  type Sut = Map[String, org.libvirt.Domain]

  // TODO we should check for example total amount of memory used here
  def canCreateNewSut(newState: State, initSuts: Traversable[State], runningSuts: Traversable[Sut]): Boolean = true

  def newSut(state: State): Sut = {
    toLibvirtXMLs(state) map { case (id, xml) => id -> con.domainDefineXML(xml) }
  }

  def destroySut(sut: Sut) = sut.values foreach { d =>
    if (d.isActive != 0) d.destroy()
    d.undefine()
  }

  def initialPreCondition(state: State) = {
    state.forall(!_.running) &&
    !hasDuplicates(state.map(_.uuid)) &&
    !hasDuplicates(state.map(_.ip))
  }

  // generate a 10.x.y subnet
  val genSubnet: Gen[List[Int]] = for {
    x <- Gen.choose(0, 255)
    y <- Gen.choose(0, 255)
  } yield List(10, x, y)

  def hasDuplicates(xs: Seq[Any]): Boolean = xs.distinct.length != xs.length

  def genMachine(id: String, subnet: List[Int]): Gen[Machine] = for {
    uuid <- Gen.uuid
    // ip <- Gen.choose(2,254).map(n => (subnet :+ n).mkString("."))
    ip <- Gen.choose(2, 254).map(n => s"172.16.2.$n")
    memory <- Gen.choose(96, 256)
    kernel <- Gen.oneOf("3.14", "3.13", "3.12", "3.10")
  } yield Machine(id, uuid, ip, kernel, memory, false)

  val genInitialState: Gen[State] = for {
    machineCount <- Gen.choose(5, 5)
    idGen = Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
    ids <- Gen.listOfN(machineCount, idGen)
    subnet <- genSubnet
    machines <- Gen.sequence[List[Machine], Machine](ids.map(genMachine(_, subnet)))
  } yield machines

  def genPingOffline(state: State): Gen[Ping] = for {
    from <- Gen.oneOf(state.filter(_.running))
    to <- Gen.oneOf(state.filter(!_.running))
  } yield Ping(from, to)

  def genPingOnline(state: State): Gen[Ping] = for {
    from <- Gen.oneOf(state.filter(_.running))
    to <- Gen.oneOf(state.filter(_.running))
  } yield Ping(from, to)

  def genBoot(state: State): Gen[Boot] = Gen.oneOf(
    state.filterNot(_.running).map(Boot.apply)
  )

  def genShutdown(state: State): Gen[Shutdown] = Gen.oneOf(
    state.filter(_.running).map(Shutdown.apply)
  )

  def genCommand(state: State): Gen[Command] =
    if (state.forall(!_.running)) genBoot(state)
    else if (state.forall(_.running)) Gen.frequency(
      (1, genShutdown(state)),
      (4, genPingOnline(state))
    )
    else Gen.frequency(
      (2, genBoot(state)),
      (1, genShutdown(state)),
      (4, genPingOnline(state)),
      (4, genPingOffline(state))
    )

  case class Boot(m: Machine) extends Command {
    type Result = Boolean
    def run(sut: Sut) = {
      println(s"booting machine ${m.ip}...")
      sut(m.id).create()
      var n = 0
      while (n < 20) {
        Thread.sleep(500)
        try {
          runSshCmd(m.ip, "true")
          println(s"machine ${m.ip} is up!")
          n = Int.MaxValue
        } catch { case e: Throwable => n = n + 1 }
      }
      sut(m.id).isActive != 0 && n == Int.MaxValue
    }
    def nextState(state: State) =
      state.filterNot(_.id == m.id) :+ m.copy(running = true)
    def preCondition(state: State) = !m.running
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

  case class Shutdown(m: Machine) extends Command {
    type Result = Boolean
    def run(sut: Sut) = {
      println(s"shutting down machine ${m.ip}...")
      sut(m.id).destroy()
      sut(m.id).isActive == 0
    }
    def nextState(state: State) =
      state.filterNot(_.id == m.id) :+ m.copy(running = false)
    def preCondition(state: State) = m.running
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

  case class Ping(from: Machine, to: Machine) extends Command {
    type Result = Boolean
    def run(sut: Sut) =
      runSshCmd(from.ip, s"fping -c 1 ${to.ip}") match {
        case Right(out) =>
          println(s"${from.ip} -> $out")
          true
        case Left(out) =>
          println(s"${from.ip} -> $out")
          false
      }
    def nextState(state: State) = state
    def preCondition(state: State) = from.running
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(to.running)
  }

}
