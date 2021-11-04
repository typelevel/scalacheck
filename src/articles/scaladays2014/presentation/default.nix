{ stdenv, fetchurl, substituteAll, unzip }:

let

  remarkjs = stdenv.mkDerivation rec {
    name = "remarkjs-${version}";
    version = "0.13.0";
    src = fetchurl {
      url = "https://github.com/gnab/remark/archive/v0.13.0.tar.gz";
      sha256 = "0ds19h17ya1xvk8jyphdyc7zs4l90210qknldg0j9fb9n3q45ad8";
    };
    phases = [ "unpackPhase" "installPhase" ];
    installPhase = ''
      mkdir -p $out/lib
      cp -v out/remark.min.js $out/lib/
      cp -v out/remark.js $out/lib/
    '';
  };

  index = substituteAll {
    src = ./index.html;
    body = builtins.readFile ./presentation.markdown;
    style = builtins.readFile ./style.css;
  };

in stdenv.mkDerivation {
  name = "scalacheck-scaladays2014";

  src = ./.;

  preBuild = ''
    cp -fT ${remarkjs}/lib/remark.min.js remark.js
    cp -fT ${index} index.html
  '';

  installPhase = ''
    mkdir $out
    mv img index.html remark.js $out/
  '';
}
