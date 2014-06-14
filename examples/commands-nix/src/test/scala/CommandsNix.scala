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
    kernelVer: String,
    memory: Int,
    running: Boolean
  )

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

  // don't allow duplicate uuids
  def initialPreCondition(state: State) = {
    val machines = state.values.toSeq
    machines.map(_.uuid).distinct.length == machines.length
  }

  def genMachine(id: String): Gen[Machine] = for {
    uuid <- Gen.uuid
    memory <- Gen.choose(96, 256)
    kernel <- Gen.oneOf("3.14", "3.13", "3.12", "3.10")
  } yield Machine (id, uuid, kernel, memory, false)

  val genInitialState: Gen[State] = for {
    machineCount <- Gen.choose(1,4)
    idGen = Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
    ids <- Gen.listOfN(machineCount, idGen)
    machines <- Gen.sequence[List,Machine](ids.map(genMachine))
  } yield Map(ids zip machines: _*)

  def genCommand(state: State): Gen[Command] =
    if(state.values.forall(_.running)) NoOp
    else Gen.oneOf(state.values.toSeq.filterNot(_.running).map(Boot))

  case class Boot(m: Machine) extends Command {
    type Result = Boolean
    def run(sut: Sut) = {
      sut(m.id).create()
      sut(m.id).isActive != 0
    }
    def nextState(state: State) =
      state + (m.id -> m.copy(running = true))
    def preCondition(state: State) = !m.running
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

}
