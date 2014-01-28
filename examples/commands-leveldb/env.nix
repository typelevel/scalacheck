with (import <nixpkgs> {});

myEnvFun {
  name = "commands-leveldb";
  buildInputs = [ gcc simpleBuildTool ];
  extraCmds = ''
    export LD_LIBRARY_PATH="${gcc.gcc}/lib:$LD_LIBRARY_PATH"
  '';
}
