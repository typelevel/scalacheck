{ pkgs ? import <nixpkgs> {} }:

with pkgs;

stdenv.mkDerivation {
  name = "commands-leveldb";
  buildInputs = [ gcc simpleBuildTool ];
  shellHook = ''
    export LD_LIBRARY_PATH="${gcc.gcc}/lib:$LD_LIBRARY_PATH"
  '';
}
