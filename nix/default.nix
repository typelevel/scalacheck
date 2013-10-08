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

in rec {

  mkScalaCheck = args: stdenv.mkDerivation rec {
    inherit (args) name;
    src = fetchgit {
      url = https://github.com/rickynils/scalacheck;
      inherit (args) rev sha256;
    };
    buildInputs = [ openjre args.scala ];
    buildPhase = ''
      find src/main -name '*.scala' > sources
      scalac -classpath ${test-interface."1.0"}/lib/test-interface.jar \
        -sourcepath src/main -d ${name}.jar @sources
      ${lib.optionalString args.runTests ''
        find src/test -name '*.scala' > sources-test
        scalac -classpath ${name}.jar \
          -sourcepath src/test -d ${name}-test.jar @sources-test
        scala -classpath ${name}.jar:${name}-test.jar org.scalacheck.TestAll
      ''}
    '';
    installPhase = ''
      mkdir -p $out/lib
      mv ${name}.jar $out/lib/
    '';
  };

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
      rev = "5ea4cbd06a2016966f57216d665545597acd306b";
      sha256 = "16lxckdk8gbb0jig5zvh1j9jdidb2gh81gnsbwgay7dk8lg00df4";
    };
    "1.10.1" = {
      rev = "ecb39d126f919795738e3b0bb66dc088e31ccef3";
      sha256 = "1yjvh1r2fp46wdmsajmljryp03qw92albjh07vvgd15qw3v6vz3k";
    };
    "1.10.0" = {
      rev = "2338afc425a905fdc55b4fd67bd8bfc3358e3390";
      sha256 = "0a3kgdqpr421k624qhpan3krjdhii9fm4zna7fbbz3sk30gbcsnj";
    };
    "1.9" = {
      rev = "ddfd03e49b05fba702539a4aff03e60ea35a57e0";
      sha256 = "1dbxp9w7pjw1zf50x3hz6kwzm8xp3hvdbswnxsrcswagvqmc5kkm";
      runTests = false;
    };
    "1.8" = {
      rev = "f2175ed5ac20f37c1a7ec320b1c58de7a8e3c27f";
      sha256 = "0gkwnqw9wcv0p0dbc6w0i54zw9lb32qnl8qdsp3mn6ylkwj5zx0h";
    };
  };

  scalacheck = mapAttrs' (scalaVer: scala: {
    name = scalaVer;
    value = mapAttrs' (scalacheckVer: scalacheck: {
      name = scalacheckVer;
      value = mkScalaCheck {
        name = "scalacheck_${scalaVer}-${scalacheckVer}";
        runTests = if scalacheck ? runTests then scalacheck.runTests else true;
        inherit scala;
        inherit (scalacheck) rev sha256;
      };
    }) scalacheckVersions;
  }) scala;

  scalacheck_flat = listToAttrs (flatten (mapAttrsToList (scalaVer: ss:
    mapAttrsToList (v: s: nameValuePair "scalacheck_${scalaVer}-${v}" s) ss
  ) scalacheck));
}
