with (import <nixpkgs> {});

myEnvFun {
  name = "scnix";
  buildInputs = [ libvirt simpleBuildTool ];
  extraCmds = ''
    export LD_LIBRARY_PATH="${libvirt}/lib:$LD_LIBRARY_PATH"
  '';
}
