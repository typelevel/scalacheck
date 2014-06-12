{ pkgs ? import <nixpkgs> {} }:

with pkgs;

stdenv.mkDerivation {
  name = "commands-nix";

  buildInputs = [ libvirt simpleBuildTool ];

  shellHook = ''
    export NIX_PATH="nixpkgs=${toString <nixpkgs>}"
    export LD_LIBRARY_PATH="${libvirt}/lib:$LD_LIBRARY_PATH"
  '';
}
