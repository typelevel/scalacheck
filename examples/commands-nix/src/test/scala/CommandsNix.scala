import org.scalacheck.Gen
import org.scalacheck.commands.Commands

import util.{Try,Success,Failure}

object CommandsNix extends org.scalacheck.Properties("CommandsNix") {

  property("machinespec") = MachineSpec.property()

}

object MachineSpec extends Commands {

  val con = new org.libvirt.Connect("qemu:///session")

  case class Machine (
    id: String,
    uuid: java.util.UUID,
    ip: String,
    kernelVer: String,
    memory: Int,
    running: Boolean
  )

  def runSshCmd(ip: String, cmd: String): Either[String,String] = {
    import scala.sys.process._
    val err = new StringBuffer()
    val logger = ProcessLogger(err.append(_))

    val sshcmd =
      s"ssh -i test-key_rsa -l root -o UserKnownHostsFile=/dev/null " +
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

  def toNixNetwork(machines: Iterable[Machine]): String = {
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
      pkgs.linuxPackages_${m.kernelVer.replace('.','_')};
  """

  def toLibvirtXMLs(machines: State): Map[String,String] = {
    import scala.sys.process._
    import java.io.ByteArrayInputStream

    val out = new StringBuffer()
    val err = new StringBuffer()
    val logger = ProcessLogger(out.append(_), err.append(_))
    val is = new ByteArrayInputStream(toNixNetwork(machines.values).getBytes("UTF-8"))

    // Run nix-build and capture stdout and stderr
    "nix-build --no-out-link -" #< is ! logger

    val xmlFiles = machines.mapValues(m => s"${out.toString.trim}/${m.id}.xml")

    // Check that all expected output files can be read
    xmlFiles.values foreach { f =>
      if(!(new java.io.File(f)).canRead) throw new Exception(raw"""
        No Libvirt XML produced (${f})
        out = ${out.toString}
        err = ${err.toString}
      """)
    }

    xmlFiles.mapValues(io.Source.fromFile(_).mkString)
  }

  // Machine.id mapped to a machine state
  type State = Map[String,Machine]

  // Machine.id mapped to a LibVirt machine
  type Sut = Map[String,org.libvirt.Domain]

  // TODO we should check for example total amount of memory used here
  def canCreateNewSut(newState: State, initSuts: Traversable[State],
    runningSuts: Traversable[Sut]
  ): Boolean = true

  def newSut(state: State): Sut = {
    val sut = toLibvirtXMLs(state).mapValues(con.domainDefineXML)

    // Start the machines that should be running
    sut foreach { case (id,d) =>
      try if(state(id).running) d.create()
      catch { case e: Throwable =>
        destroySut(sut)
        throw e
      }
    }

    sut
  }

  def destroySut(sut: Sut) = sut.values foreach { d =>
    if (d.isActive != 0) d.destroy()
    d.undefine()
  }

  def initialPreCondition(state: State) = {
    val machines = state.values.toSeq
    !hasDuplicates(machines.map(_.uuid)) &&
    !hasDuplicates(machines.map(_.ip))
  }

  // generate a 10.x.y subnet
  val genSubnet: Gen[List[Int]] = for {
    x <- Gen.choose(0,255)
    y <- Gen.choose(0,255)
  } yield List(10,x,y)

  def hasDuplicates(xs: Seq[Any]): Boolean = xs.distinct.length != xs.length

  def genMachine(id: String, subnet: List[Int]): Gen[Machine] = for {
    uuid <- Gen.uuid
    //ip <- Gen.choose(2,254).map(n => (subnet :+ n).mkString("."))
    ip <- Gen.choose(2,254).map(n => s"172.16.2.$n")
    memory <- Gen.choose(96, 256)
    kernel <- Gen.oneOf("3.14", "3.13", "3.12", "3.10")
  } yield Machine (id, uuid, ip, kernel, memory, false)

  val genInitialState: Gen[State] = for {
    machineCount <- Gen.choose(1,4)
    idGen = Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
    ids <- Gen.listOfN(machineCount, idGen)
    subnet <- genSubnet
    machines <- Gen.sequence[List,Machine](ids.map(genMachine(_, subnet)))
  } yield Map(ids zip machines: _*)

  def genPing(state: State): Gen[Ping] = for {
    from <- Gen.oneOf(state.values.toSeq.filter(_.running))
    to <- Gen.oneOf(state.values.toSeq)
  } yield Ping(from, to)

  def genBoot(state: State): Gen[Boot] = Gen.oneOf(
    state.values.toSeq.filterNot(_.running).map(Boot)
  )

  def genCommand(state: State): Gen[Command] =
    if(!state.values.exists(_.running)) genBoot(state)
    else if(state.values.forall(_.running)) genPing(state)
    else Gen.oneOf(genBoot(state), genPing(state))


  case class Boot(m: Machine) extends Command {
    type Result = Boolean
    def run(sut: Sut) = {
      sut(m.id).create()
      import scala.sys.process._
      "sleep 15".!!
      sut(m.id).isActive != 0
    }
    def nextState(state: State) =
      state + (m.id -> m.copy(running = true))
    def preCondition(state: State) = !m.running
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

  case class Ping(from: Machine, to: Machine) extends Command {
    type Result = Boolean
    def run(sut: Sut) =
      runSshCmd(from.ip, s"fping -c 1 ${to.ip}") match {
        case Right(_) => true
        case Left(_) => false
      }
    def nextState(state: State) = state
    def preCondition(state: State) = from.running
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(to.running)
  }

}
