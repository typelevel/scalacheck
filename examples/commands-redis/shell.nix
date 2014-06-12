{ pkgs ? import <nixpkgs> {} }:

with pkgs;

stdenv.mkDerivation {
  name = "commands-redis";
  buildInputs = [ simpleBuildTool ];
}
