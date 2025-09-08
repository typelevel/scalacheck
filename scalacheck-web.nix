{ lib, stdenv, callPackage, runCommand, writeScript, bash, writeText
, glibcLocales
}:

let

  inherit (lib)
    attrNames getAttr concatStringsSep mapAttrsToList concatMapStrings flatten;

  env = {
    repoUrl = "https://github.com/typelevel/scalacheck";
    scalaVer = "2.11";
    allVersions = concatStringsSep "," (attrNames versions);
    scalaVersions = concatStringsSep "," (mapAttrsToList (v: sc:
      "${v}=${concatStringsSep ":" (attrNames sc.scalaVersions)}") versions
    );
    currentVer = "1.19.0";
  };

  sclib = callPackage ./scalacheck.nix {};

  versions = sclib.scalacheckVersions;

  scaladays2014 = callPackage src/articles/scaladays2014/presentation {};

  hakyllWrapper = runCommand "sc-hakyll-wrapper-builder" {
    wrapper = writeScript "sc-hakyll-wrapper" ''
      #!${bash}/bin/bash
      if [ -d src ]; then
        rm -r src/files
        mkdir -p src/files

        ln -s ${scaladays2014} src/files/scaladays2014

        ${with sclib; concatMapStrings (o: ''
          ln -sft src/files ${o}/*
        '') (flatten (mapAttrsToList fetchScalaChecks scalacheckVersions))}

        ${concatMapStrings (o: ''
          ln -sft src/files ${o}/*
        '') (flatten (mapAttrsToList (sclib.mkScalaCheckDoc env.scalaVer (getAttr env.scalaVer sclib.scalaVersions)) versions))}

        export HAKYLL_CTX="${writeText "sc-env.yaml" (
          concatStringsSep "\n" (mapAttrsToList (k: v: ''${k}: "${v}"'') env)
        )}"

        ${callPackage ./hakyll {}}/bin/hakyll "$@"
      else
        echo "No src directory found"
        exit 1
      fi
    '';
  } ''
    mkdir -p $out/bin
    ln -s $wrapper $out/bin/hakyll
  '';

  LOCALE_ARCHIVE =
    if stdenv.isLinux then "${glibcLocales}/lib/locale/locale-archive" else "";

in stdenv.mkDerivation {
  name = "scalacheck-web";
  src = ./src;
  buildInputs = [ hakyllWrapper ];
  phases = [ "buildPhase" "installPhase" ];
  buildPhase = ''
    export LANG="en_US.UTF-8"
    export LOCALE_ARCHIVE="${LOCALE_ARCHIVE}"
    cp -r --no-preserve=mode $src src
    hakyll build
  '';
  installPhase = "mv _site $out";
}
