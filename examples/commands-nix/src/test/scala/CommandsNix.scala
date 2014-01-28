import org.scalacheck.Gen
import org.scalacheck.commands.Commands

object CommandsNix extends org.scalacheck.Properties("CommandsNix") {

  property("machinespec") = MachineSpec.property

}

object MachineSpec extends Commands {

  val con = new org.libvirt.Connect("qemu:///session")

  case class Machine (
    name: String,
    uuid: java.util.UUID,
    kernelVer: String,
    memory: Int,
    running: Boolean
  )

  def toNix(m: Machine): String = raw"""
    let
      ${m.name} = { config, pkgs, ... }: {
        require = [
          <nix-qemu/module.nix>
          <common/env-base.nix>
        ];
        deployment.libvirt = {
          memory = ${m.memory};
          uuid = "${m.uuid}";
          netdevs.netdev0 = {
            vdeSocket = "/run/vde0.ctl";
            mac = "$$MAC0";
          };
        };
        users.extraUsers.admin.password = "admin";
        boot.kernelPackages = pkgs.linuxPackages_${m.kernelVer.replace('.','_')};
      };
    in import <nix-qemu> { network = { inherit ${m.name}; }; }
  """

  def toLibvirtXML(m: Machine): String = {
    println(s"Building: ${m.name}...")
    import scala.sys.process._
    import java.io.ByteArrayInputStream
    val out = new StringBuffer()
    val err = new StringBuffer()
    val logger = ProcessLogger(out.append(_), err.append(_))
    val is = new ByteArrayInputStream(toNix(m).getBytes("UTF-8"))
    val cmd = List(
      "nix-build",
      "-I /home/rickard/workspace/nixos/rnlunarc",
      "-I nix-qemu=/home/rickard/workspace/nixlab/nix-qemu",
      "--no-out-link", "-"
    ).mkString(" ")
    cmd #< is ! logger
    val f = s"${out.toString.trim}/${m.name}.xml"
    val s = if((new java.io.File(f)).canRead) io.Source.fromFile(f).mkString else {
      throw new Exception(
        s"No Libvirt XML produced\ncmd = $cmd\n" ++
        s"out = ${out.toString}\nerr = ${err.toString}"
      )
    }
    println(s"Built: ${m.name}")
    s
  }

  type State = Machine
  type Sut = org.libvirt.Domain

  def canCreateNewSut(newState: State, initStates: Traversable[State]) = true

  def newSutInstance(state: State): Sut = {
    println(s"Creating SUT: ${state.name}")
    val d = con.domainDefineXML(toLibvirtXML(state))
    try {
      if(state.running) d.create()
      d
    } catch { case e: Throwable =>
      destroySutInstance(d)
      throw e
    }
  }

  def destroySutInstance(sut: Sut) = {
    println(s"Destroying SUT")
    if (sut.isActive != 0) sut.destroy()
    sut.undefine()
  }

  def initialPreCondition(state: State) = true

  val genInitialState = for {
    uuid <- Gen.uuid
    name <- Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
    memory <- Gen.choose(96, 256)
    kernel <- Gen.oneOf("3.11", "3.10")
  } yield Machine (name, uuid, kernel, memory, false)

  def genCommand(state: State): Gen[Command] =
    if(!state.running) Gen.oneOf(NoOp, Boot)
    else NoOp

  case object Boot extends Command {
    type Result = Boolean
    def run(sut: Sut) = {
      sut.create()
      sut.isActive != 0
    }
    def nextState(state: State) = state.copy(running = true)
    def preCondition(state: State) = !state.running
    def postCondition(state: State, result: Result) = result
  }

}
