with (import <nixpkgs> {});
with lib;

let

  mkTestInterface = args: stdenv.mkDerivation rec {
    name = "test-interface-${args.version}";
    src = fetchurl {
      url = "http://search.maven.org/remotecontent?filepath=org/scala-sbt/test-interface/${args.version}/${name}.jar";
      inherit (args) sha256;
    };
    phases = [ "installPhase" ];
    installPhase = ''
      mkdir -p $out/lib
      cp $src $out/lib/${name}.jar
      ln -s $out/lib/${name}.jar $out/lib/test-interface.jar
    '';
  };

  mkScala = args: stdenv.mkDerivation rec {
    name = "scala-${args.version}";
    src = fetchurl {
      url = "http://www.scala-lang.org/files/archive/${name}.tgz";
      inherit (args) sha256;
    };
    installPhase = ''
      mkdir -p $out
      rm -f bin/*.bat lib/scalacheck.jar
      mv bin lib $out/
    '';
  };

  mkScalaCheck = args: stdenv.mkDerivation rec {
    inherit (args) name;
    src = fetchurl {
      url = "https://github.com/rickynils/scalacheck/archive/${args.version}.tar.gz";
      inherit (args) sha256;
    };
    buildInputs = [ openjre args.scala ];
    buildPhase = ''
      find src/main -name '*.scala' > sources
      find src/test -name '*.scala' > sources-test
      scalac -classpath ${args.test-interface}/lib/test-interface.jar \
        -sourcepath src/main -d ${name}.jar @sources
      scalac -classpath ${name}.jar \
        -sourcepath src/test -d ${name}-test.jar @sources-test
      scala -classpath ${name}.jar:${name}-test.jar org.scalacheck.TestAll
    '';
    installPhase = ''
      mkdir -p $out/lib
      mv ${name}.jar $out/lib/
    '';
  };

in rec {

  test-interface = {
    "1.0" = mkTestInterface {
      version = "1.0";
      sha256 = "17klrl4ylpsy9d16fk6npb7lxfqr1w1m9slsxhph1wwmpcw0pxqm";
    };
  };

  scala = {
    "2.9.1-1" = mkScala {
      version = "2.9.1-1";
      sha256 = "0b5lhfy33ng6q4rjz11j89k694h5x12ms9qdnv0ym3jav10r6qab";
    };
    "2.9.2" = mkScala {
      version = "2.9.2";
      sha256 = "0s1shpzw2hyz7bwxdqq19rcrzbpq4d7b0kvdvjvhy7h05x496b46";
    };
    "2.9.3" = mkScala {
      version = "2.9.3";
      sha256 = "1fg7qizxhgw6957plv99jh8p6z3rpi2w0cgxx1im154cywlv5aps";
    };
    "2.10.1" = mkScala {
      version = "2.10.1";
      sha256 = "0krzm6ln4ldy6wvk7mw93vnjsjf8pkylccnwz08hzj7wnvspgijk";
    };
    "2.10.2" = mkScala {
      version = "2.10.2";
      sha256 = "18v6jr42rif84q5cb82kynigqls2q5q3qnb61040m92496ygv4zn";
    };
    "2.11.0-M4" = mkScala {
      version = "2.11.0-M4";
      sha256 = "15wv35nizyi50dv7ig2dscv7yapa65mp32liqvmwwhyvzx1h9pnh";
    };
  };

  scalacheckVersions = {
    "1.11.0-SNAPSHOT" = {
      version = "171cc42";
      sha256 = "03naiwg7ibc3yi7dmzfcxnpfbxlpvni60pc7dhyvdz0bzjlaw08a";
      test-interface = test-interface."1.0";
    };
    "1.10.1" = {
      version = "1.10.1";
      sha256 = "1gxqx8yjyldcy4x6q7dg7h3827fd0l7396psr1gkk87dxjh9l7hg";
      test-interface = test-interface."1.0";
    };
    "1.10.0" = {
      version = "1.10.0";
      sha256 = "0jjvsmd8qj8b4zgdyypabj14p6am4b18cz19i69vgiwfqb4r86w1";
      test-interface = test-interface."1.0";
    };
  };

  scalacheck = listToAttrs (flatten (mapAttrsToList (versionName: scalacheck:
    mapAttrsToList (scalaVersion: scala: rec {
      name = "scalacheck_${scalaVersion}-${versionName}";
      value = mkScalaCheck {
        inherit name scala;
        inherit (scalacheck) version sha256 test-interface;
      };
    }) scala) scalacheckVersions));
}
