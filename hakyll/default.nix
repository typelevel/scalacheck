{ lib, stdenv, haskellPackages }:

haskellPackages.mkDerivation {
  pname = "sc-hakyll";
  version = "0.0.0.0";
  isLibrary = false;
  isExecutable = true;
  license = lib.licenses.unfree;
  src = ./.;
  buildDepends = with haskellPackages; [
    pandoc hakyll filepath yaml split
  ];
}
