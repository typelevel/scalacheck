with (import <nixpkgs> {});

myEnvFun {
  name = "commands-nix";
  buildInputs = [ libvirt simpleBuildTool ];
  extraCmds = ''
    export LD_LIBRARY_PATH="${libvirt}/lib:$LD_LIBRARY_PATH"
  '';
}
