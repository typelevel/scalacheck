{ pkgs ? import <nixpkgs> {} }:

with pkgs.lib;
with builtins;
with pkgs;

let
  scalacheckRepoUrl = "https://github.com/rickynils/scalacheck";

  docFooter = version: rev:
    let shortRev = builtins.substring 0 7 rev;
    in "ScalaCheck ${version} / ${shortRev} API documentation. © 2007-2014 Rickard Nilsson";

  docSourceUrl = version: rev:
    let tag = if hasSuffix "SNAPSHOT" version then rev else version;
    in "${scalacheckRepoUrl}/tree/${tag}/src/main/scala€{FILE_PATH}.scala";

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
      urls = [
        "http://www.scala-lang.org/files/archive/${name}.tgz"
        "http://downloads.typesafe.com/scala/${args.version}/${name}.tgz"
      ];
      inherit (args) sha256;
    };
    installPhase = ''
      mkdir -p $out
      rm -f bin/*.bat lib/scalacheck.jar
      mv bin lib $out/
    '';
  };

  test-interface = {
    "1.0" = mkTestInterface {
      version = "1.0";
      sha256 = "17klrl4ylpsy9d16fk6npb7lxfqr1w1m9slsxhph1wwmpcw0pxqm";
    };
  };

in rec {

  # Fetches the given ScalaCheck with the given Scala version from Maven
  fetchScalaChecks = scVer: sc: mapAttrsToList (scalaVer: scala:
    fetchScalaCheck scalaVer scala scVer sc
  ) sc.scalaVersions;

  # Fetches the given ScalaCheck with the given Scala version from Maven
  fetchScalaCheck = scalaVer: scala: scVer: sc: stdenv.mkDerivation rec {
    name = "scalacheck_${scalaVer}-${scVer}";

    src = fetchurl {
      url = "http://search.maven.org/remotecontent?filepath=org/scalacheck/scalacheck_${scalaVer}/${scVer}/${name}.jar";
      sha256 = getAttr scalaVer sc.prebuiltSha256;
    };

    phases = [ "installPhase" ];

    installPhase = ''
      mkdir -p "$out"
      cp -v "$src" "$out/${name}.jar"
    '';
  };

  # Builds the given ScalaCheck with all Scala versions it supports
  mkScalaChecks = scVer: sc: mapAttrsToList (scalaVer: scala:
    mkScalaCheck scalaVer scala scVer sc
  ) sc.scalaVersions;

  # Builds the given ScalaCheck with the given Scala version
  mkScalaCheck = scalaVer: scala: scVer: sc: stdenv.mkDerivation rec {
    name = "scalacheck_${scalaVer}-${scVer}";

    src = fetchgit {
      url = scalacheckRepoUrl;
      inherit (sc) rev sha256;
    };

    buildInputs = [ openjdk (mkScala scala) ];

    doCheck = if sc ? runTests then sc.runTests else true;

    buildPhase = ''
      export LANG="en_US.UTF-8"
      export LOCALE_ARCHIVE=${glibcLocales}/lib/locale/locale-archive
      testinterface="${test-interface."1.0"}/lib/test-interface.jar"
      find src/main/scala -name '*.scala' > sources
      scalac \
        -classpath "$testinterface" \
        -optimise \
        -deprecation \
        -d ${name}.jar \
        @sources
    '';

    checkPhase = ''
      find src/test/scala -name '*.scala' > sources-test
      scalac \
        -classpath ${name}.jar \
        -optimise \
        -d ${name}-test.jar \
        @sources-test
      scala \
        -classpath ${name}.jar:${name}-test.jar \
        org.scalacheck.TestAll
    '';

    installPhase = ''
      mkdir -p "$out"
      mv "${name}.jar" "$out/"
    '';
  };

  mkScalaCheckDoc = scalaVer: scala: scVer: sc: stdenv.mkDerivation rec {
    name = "scalacheck-doc_${scalaVer}-${scVer}";
    fname = "scalacheck_${scalaVer}-${scVer}";

    src = fetchgit {
      url = scalacheckRepoUrl;
      inherit (sc) rev sha256;
    };

    buildInputs = [ xz zip openjdk (mkScala scala) ];

    buildPhase = ''
      export LANG="en_US.UTF-8"
      export LOCALE_ARCHIVE=${glibcLocales}/lib/locale/locale-archive
      testinterface="${test-interface."1.0"}/lib/test-interface.jar"
      find src/main/scala -name '*.scala' > sources
      mkdir "${fname}-api" && scaladoc \
        -doc-title ScalaCheck \
        -doc-version "${scVer}" \
        -doc-footer "${docFooter scVer sc.rev}" \
        -doc-source-url "${docSourceUrl scVer sc.rev}" \
        -classpath "$testinterface" \
        -sourcepath src/main/scala \
        -d "${fname}-api" \
        @sources
      mkdir "${fname}-sources"
      cp -rt "${fname}-sources" LICENSE RELEASE src/main/scala/*
    '';

    installPhase = ''
      mkdir -p "$out"
      jar cf "$out/${fname}-sources.jar" -C "${fname}-sources" .
      zip -r "$out/${fname}-sources.zip" "${fname}-sources"
      tar -czf "$out/${fname}-sources.tar.gz" "${fname}-sources"
      jar cf "$out/${fname}-javadoc.jar" -C "${fname}-api" .
      zip -r "$out/${fname}-javadoc.zip" "${fname}-api"
      tar -czf "$out/${fname}-javadoc.tar.gz" "${fname}-api"
      mv "${fname}-api" "$out/"
    '';
  };

  scalaVersions = {
    "2.9.0" = {
      version = "2.9.0.final";
      sha256 = "0gvz5krhj56yl0xcw37wxnqpw8j4pd5l36sdsv9gidb16961dqid";
    };
    "2.9.0-1" = {
      version = "2.9.0.1";
      sha256 = "01ylbwswmdgwj07h6y5szf6y0j7p1l4ryvc435p43hriv1l11dhd";
    };
    "2.9.1" = {
      version = "2.9.1.final";
      sha256 = "04szkhfnd0ffhz4a9gh1v026wdwf9ixq78bipviz3xb37ha9kz0b";
    };
    "2.9.1-1" = {
      version = "2.9.1-1";
      sha256 = "0b5lhfy33ng6q4rjz11j89k694h5x12ms9qdnv0ym3jav10r6qab";
    };
    "2.9.2" = {
      version = "2.9.2";
      sha256 = "0s1shpzw2hyz7bwxdqq19rcrzbpq4d7b0kvdvjvhy7h05x496b46";
    };
    "2.9.3" = {
      version = "2.9.3";
      sha256 = "1fg7qizxhgw6957plv99jh8p6z3rpi2w0cgxx1im154cywlv5aps";
    };
    "2.10.1" = {
      version = "2.10.1";
      sha256 = "0krzm6ln4ldy6wvk7mw93vnjsjf8pkylccnwz08hzj7wnvspgijk";
    };
    "2.10.2" = {
      version = "2.10.2";
      sha256 = "18v6jr42rif84q5cb82kynigqls2q5q3qnb61040m92496ygv4zn";
    };
    "2.10.3" = {
      version = "2.10.3";
      sha256 = "16ac935wydrxrvijv4ldnz4vl2xk8yb3yzb9bsi3nb9sic7fxl95";
    };
    "2.10.4" = {
      version = "2.10.4";
      sha256 = "1hqhm1xvd7g78jspvl30zgdzw79xq5zl837h47p6w1n6qlwbcvdl";
    };
    "2.10" = scalaVersions."2.10.4";
    "2.11.0" = {
      version = "2.11.0";
      sha256 = "00lap31c6rxvg7vipmj0j7f4mv6c58wpfyd3785bxwlhrzmmwgq7";
    };
    "2.11" = scalaVersions."2.11.0";
  };

  scalacheckVersions = {
    "1.11.6" = {
      rev = "229e54afc4700a9ccb70e18ee5527e01a004ee66";
      sha256 = "02gsb73ajzw17q2n85wyd04vrljgzxz133qf3n4crvy902ablzsw";
      prebuiltSha256 = {
        "2.11" = "0mxn075xsk7bk9454a4p83slvh4am2im4azj7zpbb3p3lxbj23cm";
        "2.10" = "12h1y7kg4cxbbzaf4q7774q3z92rd99kkp5la15br18j0z2pb0iy";
        "2.9.3" = "11w3lcpxxy2f3agvqsqlp9cfvhj08z4a7h6fa6xphmy8nwcm2q4f";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3";
      };
    };
    "1.11.5" = {
      rev = "229e54afc4700a9ccb70e18ee5527e01a004ee66";
      sha256 = "02gsb73ajzw17q2n85wyd04vrljgzxz133qf3n4crvy902ablzsw";
      prebuiltSha256 = {
        "2.11" = "1lvcwafn3vs706z81k4jmb3ryp0fcx6dbvi1vmmps64jjnl20d8c";
        "2.10" = "0vwrn9v6wyq3778izmnp1a7yj49hdymi951y3mh1p69891i0b02w";
        "2.9.3" = "1wyxpqg6hrykkd98znvxhsxd45x0fhjdwfdwfa0imq5nxdnd1s61";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3";
      };
    };
    "1.11.4" = {
      rev = "1e2cf540f6da181235dfd2baad862a07de6c0500";
      sha256 = "0gbwglx5qhggxmq18lpjglc6as8bq2nzwj0rpik174a5fkamckmw";
      prebuiltSha256 = {
        "2.11" = "1lfiz5ghy9z1wyvwba33vxqij43cs1yvv5x5alvzlykp2g79w1i3";
        "2.10" = "11bxfabhgcxqn521nkcwvdyqw6xgv79lwhz92d435cz9r9hk2zza";
        "2.9.3" = "1ajlcv3rvdvn6rm64xa8a3x6k02s0ms2iacdxqr8dr6r9bhw2g14";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3";
      };
    };
    "1.11.3" = {
      rev = "1f648d52c93c1846b9bfdbb30ac456aa2baa58d4";
      sha256 = "05f3p434zjsi8p1cvdc6igj7phxllic5lzrimx7q2v78pqdv1vjx";
      prebuiltSha256 = {
        "2.11" = "1536p257v2m3i9mg6jvg33h6sx8nfk2rdl6s13rg2yjw01zhgxfy";
        "2.10" = "0hzis765mxmrphigm5hw4s63iqhjy7bhympxwjhr1sjidvqfxrbx";
        "2.9.3" = "1cc77xvx6kggwsrfgra7414i3y4miw2f1ya5f1lwaj8jk64vpmgd";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3";
      };
    };
    "1.11.1" = {
      rev = "d7b285bf6f9a4171d41639c5d0f641c46555f7ee";
      sha256 = "12pidk5jr463gd72c2kanz9jjad6zhcxamppqq3fcjcvm93rrnjg";
      prebuiltSha256 = {
        "2.11" = "17gifpzd5hkrvx27d10s6ggr61qppn2p0pn9v7akf4sd9df7ak5s";
        "2.10" = "03j0d0iwvqd07clgaq77vvg6hzzwbz080jcr0fynmdmybq33m9rk";
        "2.9.3" = "1pc959pb7l1g4i1q95k5n18cyx87s4ypc2n5bwmv8df066v4ms19";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3";
      };
    };
    "1.11.0" = {
      rev = "062d9a3347860ecb0d947fda1cef923a66b6555d";
      sha256 = "1088hsg5734cp8qq2865gdrzp61lhwwjvwrwa56qqkr8pbfzqglp";
      prebuiltSha256 = {
        "2.11" = "0zf5g77nhw1z9iazyhvd8gijq527x4lbj5ndwl2gi2xa0m6d3f6k";
        "2.10" = "0cx4b950pdp4xvay7jgyxn5vzhhfacl72fk16m9wki92y29nyai9";
        "2.9.3" = "0hy730h0nnph8dcbcijvsqqzjxwr72m57q6gr7gqvwjd5f93c436";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3";
      };
    };
    "1.10.1" = {
      rev = "ecb39d126f919795738e3b0bb66dc088e31ccef3";
      sha256 = "1yjvh1r2fp46wdmsajmljryp03qw92albjh07vvgd15qw3v6vz3k";
      prebuiltSha256 = {
        "2.11" = "1q2cw2ag6sd4j99wabl1ysnjzk1fvxnfhz0sadcd2b2c67azy47y";
        "2.10" = "0zfwsqbyd1zs40vhjxmyaqhdrdpqzgkm4wirqy0wgq2q7khfm32m";
        "2.9.3" = "08pivf2igli4b27in9zdk7i31if0a5sa6b2v8a4fhk1wmkp9n50x";
        "2.9.2" = "0qqksckksbjs36x68x1vl7w82skig2266sn79zz8cc8bapmf86is";
        "2.9.1-1" = "0nabfrjpsssvb5qnpcnvkhnmbvbky8y4jcmslrmfcvc3hphw99lb";
        "2.9.1" = "1m7xaj6wd44abrbs749dw33l2l9jm3k2ssl7w19nfi89m307gk55";
        "2.9.0-1" = "1525b8hwys7rvprqnq40994mxw6s17n3mkfj71q1lr2a8w48nc22";
        "2.9.0" = "1jigdpc3zars1501ina2hps8519rbsbj8qjz8hiiynj6fpjwsp2f";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3" "2.9.2" "2.9.1-1" "2.9.1" "2.9.0-1" "2.9.0";
      };
    };
    "1.10.0" = {
      rev = "2338afc425a905fdc55b4fd67bd8bfc3358e3390";
      sha256 = "0a3kgdqpr421k624qhpan3krjdhii9fm4zna7fbbz3sk30gbcsnj";
      prebuiltSha256 = {
        "2.11" = "1mvizdxdhdfjfx5bcy4q4szy53yjl282sz4cx6w70wfsm6d29nyc";
        "2.10" = "1i2xw6kcfsn89kpyxaprxac0zgfa9i4qsk6pgzasalhsl5ds7314";
        "2.9.3" = "1im0asc3si2r9svzwli36q56xv1sybcvxrjcwqxd7049z40klk73";
        "2.9.2" = "1x6bsrr6a36f4zbx7p7qy124v59c0gf83sj27ff15idhp70iwz0n";
        "2.9.1-1" = "0i2dzbgxc9m931ps8956rmz953m4m8g0briqdklacbirji0ngazv";
        "2.9.1" = "1a6mmpb5hlrbhk7w5gqf6abp3fl2hzdf632qjyz48jcrcszd5ns4";
        "2.9.0-1" = "18amqr0wnp2arpsf0xhfjxz752izii1vv59xmhzn9w81lwrr3rls";
        "2.9.0" = "0b794mmazvxza81n21his08nb5fjdikq4dx2qqf08czdwbbjss5b";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3" "2.9.2" "2.9.1-1" "2.9.1" "2.9.0-1" "2.9.0";
      };
    };
    "1.9" = {
      rev = "ddfd03e49b05fba702539a4aff03e60ea35a57e0";
      sha256 = "1dbxp9w7pjw1zf50x3hz6kwzm8xp3hvdbswnxsrcswagvqmc5kkm";
      runTests = false;
      prebuiltSha256 = {
        "2.11" = "0gvj00n64mlri9rp8s4ifw0kpb8f8ffxh0kirwpkhkg0hy63cbg9";
        "2.10" = "0xldlrqxbzyi9l8v5vdwf8inry3xgjad63mmqw2amy8zsqgmjfmg";
        "2.9.3" = "10jd7dfx39q3wamm4dj49bak7msxzfwc4mx8bq6xd35g8j52j6ya";
        "2.9.2" = "0nfvx9zax3cf8gc165za84dxg8qy8jb5m4svmhn87fh1wgadja4g";
        "2.9.1-1" = "1fkrs3pwrjgz0cmmyx5n2l987i9xqanp9cifwrfz7ls16ajn9gyk";
        "2.9.1" = "012d8bwx646w1wzzbmr5q36vxywx2slwv5k0d7lk00d7296b3sqh";
        "2.9.0-1" = "1npm10nrmicnqnv8iq862fflxkf79gwn9fq7wvvdgaa3n8bgjgm3";
        "2.9.0" = "0wqp8xid73viq4y4cgzmh42035p99h7z8wcrimr9gqwglg51xwq4";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3" "2.9.2" "2.9.1-1" "2.9.1" "2.9.0-1" "2.9.0";
      };
    };
    "1.8" = {
      rev = "f2175ed5ac20f37c1a7ec320b1c58de7a8e3c27f";
      sha256 = "0gkwnqw9wcv0p0dbc6w0i54zw9lb32qnl8qdsp3mn6ylkwj5zx0h";
      prebuiltSha256 = {
        "2.11" = "1m3jvp96j9vsis9n3vlzcvjmqd2vsj9nmfr92133qijmzbc74bx0";
        "2.10" = "1jbq92k3ni70swhgpwsq12zgwrww1lvc208by6sc6cpgd6wsff97";
        "2.9.3" = "1fgj3wv4zmj09isvbmb38r9f5nwixh5qcxg7d055dbw24jzh2h5i";
        "2.9.2" = "1k0ccvsncmg18kqkl24xrhnsmxnbyjb4zr0590skbldjm1kcb3wh";
        "2.9.1-1" = "140sk83km0c01fp69sa5q24sz47wbsywsfwbzppibng1xkyqscfi";
        "2.9.1" = "06kmh2g7120wjkl368zr8s3jr5s32bgk2kgp7s2jzzm38gdrpr7l";
        "2.9.0-1" = "1zlx1p38qfjhs5d69lb66sd4v7yav37jwsds77cx8slnm0yikk4m";
        "2.9.0" = "1r12ig2fjn50ka3l87k47fcwywvq189idgxllqdaszvglhg4958f";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10" "2.9.3" "2.9.2" "2.9.1-1" "2.9.1" "2.9.0-1" "2.9.0";
      };
    };
  };

}
