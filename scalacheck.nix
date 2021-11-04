{ lib, stdenv, fetchurl, fetchgit, xz, zip, openjdk, gnugrep
, glibcLocales
}:

let

  inherit (lib) getAttr hasSuffix mapAttrsToList optionalString versionAtLeast;

  scalacheckRepoUrl = "https://github.com/typelevel/scalacheck";

  docFooter = version: rev:
    let shortRev = builtins.substring 0 7 rev;
    in "ScalaCheck ${version} / ${shortRev} API documentation. © 2007-2016 Rickard Nilsson";

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
      urls = [
        "http://search.maven.org/remotecontent?filepath=org/scalacheck/scalacheck_${scalaVer}/${scVer}/${name}.jar"
        "https://oss.sonatype.org/service/local/repositories/releases/content/org/scalacheck/scalacheck_${scalaVer}/${scVer}/${name}.jar"
        "https://oss.sonatype.org/service/local/repositories/snapshots/content/org/scalacheck/scalacheck_${scalaVer}/${scVer}/${name}.jar"
      ];
      sha256 = getAttr scalaVer sc.prebuiltSha256;
    };

    phases = [ "installPhase" ];

    installPhase = ''
      mkdir -p "$out"
      cp -v "$src" "$out/${name}.jar"
    '';
  };

  mkScalaCheckDoc = scalaVer: scala: scVer: sc: stdenv.mkDerivation rec {
    name = "scalacheck-doc_${scalaVer}-${scVer}";
    fname = "scalacheck_${scalaVer}-${scVer}";

    src = fetchgit {
      url = scalacheckRepoUrl;
      inherit (sc) rev sha256;
    };

    buildInputs = [ xz zip openjdk (mkScala scala) gnugrep ];

    buildPhase = ''
      type -P scaladoc
      export LANG="en_US.UTF-8"
      export LOCALE_ARCHIVE=${glibcLocales}/lib/locale/locale-archive
      testinterface="${test-interface."1.0"}/lib/test-interface.jar"
      find src/main/scala \
        ${optionalString (versionAtLeast scVer "1.12.1") "jvm/src/main/scala"} -name '*.scala' | grep -v ScalaCheckFramework.scala > sources
      mkdir "${fname}-api" && scaladoc \
        -doc-title ScalaCheck \
        -doc-version "${scVer}" \
        -doc-footer "${docFooter scVer sc.rev}" \
        -doc-source-url "${docSourceUrl scVer sc.rev}" \
        -nobootcp \
        -classpath "$testinterface" \
        -sourcepath src/main/scala \
        -d "${fname}-api" \
        @sources
      mkdir "${fname}-sources"
      cp -rt "${fname}-sources" LICENSE RELEASE* {src,jvm/src}/main/scala/*
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
    "2.10.7" = {
      version = "2.10.7";
      sha256 = "00qhm1xvd7g78jspvl30zgdzw79xq5zl837h47p6w1n6qlwbcvdl";
    };
    "2.10" = scalaVersions."2.10.7";
    "2.11.1" = {
      version = "2.11.1";
      sha256 = "1vjsmqjwpxavyj29wrbvvx7799fsa65d4iha5mj63cgs8qp605gk";
    };
    "2.11" = scalaVersions."2.11.1";
    "2.12.5" = {
      version = "2.12.5";
      sha256 = "18bah2n3jfvc3cb10n4b3d6pwm3rwlqp7lrfvafjxccmlklzyqdj";
    };
    "2.12" = scalaVersions."2.12.5";
  };

  scalacheckVersions = {
    "1.14.0" = {
      rev = "9b4d4314662c86414c3e4ce25f1aeaac2223b5af";
      sha256 = "05iysfmiv7xkzx7b9c97vlbfy9x2yw8vxxg1alrkjnacacg2kyaz";
      prebuiltSha256 = {
        "2.12" = "0bp6xkzbllrcblyw3yg781gdkdrjcs0lfl0rfjz06jxp5clmnvqy";
        "2.11" = "0xmjll5m3hz9y85ypvh690a9lsaakypwya60lirb2vrrlf2kmkww";
        "2.10" = "1184z4f2wazzijsiksb0k453cynhffspgbpnx0piqdxda1clxjg1";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.12" "2.11" "2.10";
      };
    };
    "1.13.5" = {
      rev = "7d255e1b03d57d1fa475778ac07a3c6933629da1";
      sha256 = "0f2p59vz3r265kg3iyahsw93cggn0rkdhsb9ckjpcnwz0w7z46a8";
      prebuiltSha256 = {
        "2.12" = "1m4fxczj0a661mg3b5l7mpj2h9vdp34yaqv54755hykrx9ymhjq4";
        "2.11" = "0m7qs5dpv9xrgdz3f1cj8xp2m7lcggliwmlkbjdpjviphlsmjmby";
        "2.10" = "0828i0ncripi22syzdmp5n8di0lpw4089rnzlyi89lgfqigvsyak";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.12" "2.11" "2.10";
      };
    };
    "1.13.4" = {
      rev = "e65c90a85225db9c9530ae85b606388d0bcbc6ca";
      sha256 = "1m25wcwcnj6h0486yk8ghb7nlhrjh3bf99bh6z65p4s1zxngsm6h";
      prebuiltSha256 = {
        "2.12" = "11q1vzx5xz97yxdpqqpdwc9pzbyavw1zi7d11xwrs3d11xjfc9j5";
        "2.11" = "1mwgbzp1plb3znsbh450nzg0xlnkksb2f24dhll7vds3sr5gylp3";
        "2.10" = "1yny1jh46r6r6x8dnsqh4vl1122frzpchxc42r3k2zl8nj409c0z";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.12" "2.11" "2.10";
      };
    };
    "1.13.3" = {
      rev = "d6a99ead5caa4644f5e01505820a07a45e52397d";
      sha256 = "1r52bs24ps3zq79fgr152sg8g7rfv6gz8wp88wl4zb2z5cmshyb3";
      prebuiltSha256 = {
        "2.11" = "12ffbjdycmyln981lvsbys7rpwcwhnd9d03z658qkk4fmpblgynx";
        "2.10" = "0i7j0fc25amds1zvsxj7scfr6056g213i8ayg5np559x346lr1vy";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.13.2" = {
      rev = "2813e07a7133da9c987de2a638955dd770a5ad88";
      sha256 = "1anhc9c6kcgs7aqj4340gsw4qc0ya99vzp0cr9kyqv9msi6d30id";
      prebuiltSha256 = {
        "2.11" = "07s5ycpykhmfdz609yrn6sd8ny7p2x67z61yay4v7chf7i01p7f0";
        "2.10" = "1fdarzqqqw5flrdl25kmp3v9d9d20fimm9rfkmq8mcamawn3gssx";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.13.1" = {
      rev = "fe817107372ddcddef77d9890526bc7813f5696c";
      sha256 = "0z3l6vp69f95rsswlddl9plj1adv7frjk1xxhfhszla7r1zj3ybf";
      prebuiltSha256 = {
        "2.11" = "0fmxaa0im2yr1l0b252kimg5nw8164xp7b0kk0wq2nrj72fp9kyp";
        "2.10" = "0c7xmrwjph67147b1xaz60fdn2qdw4z9d8k1n3dxql4yzfkcjyf2";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.13.0" = {
      rev = "58f7c02cfa23b5314abab0b64437f9172d9a84dd";
      sha256 = "1x89la87shx8885nzyscdd4ympdwvy7d3g6az9vig0syvyjjwk19";
      prebuiltSha256 = {
        "2.11" = "1jwls5swg0q2d4p3p68scs2xpgl4sdvsp77dkmyrp4ci9g01vvf2";
        "2.10" = "1nc6vvvn3kqsrjacmzj77h9qbdkjhqs5ql0bf20x97qscdb9nk1d";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.12.6" = {
      rev = "d3989de3e48ae26a709de23c36ac46903104e0b8";
      sha256 = "1bnqdk6sdsrbz9y4cdrsxa05i0l80zqj6903hrzrjil24fr634ni";
      prebuiltSha256 = {
        "2.12" = "1cnravaabj85wn0sm1jcl5hlv8yf2fgslyzi74kh3i3p99z4p6xp";
        "2.11" = "1c8dr0d6qm1lkrbrxjavmc4bybdw55j5j0y4s4yb5vnjlq2wyxhb";
        "2.10" = "0qpdp4al15hl0rh64fk46j7yj0684hjimmihpiv20kwj04yk649n";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.12" "2.11" "2.10";
      };
    };
    "1.12.4" = {
      rev = "d271667c51c8002dd81526350ee8a29aa9f21c82";
      sha256 = "03q2sv244d8hhnfjbxq2dx113lqjvink0c4g5lfskllk83hp73ss";
      prebuiltSha256 = {
        "2.11" = "0c5ff3z47056008yvb6fla9fwra6qp7r5164nym95j5wly6jyzph";
        "2.10" = "07rlhrvkaq6y0x623vvwq050s4wq3j2am97546j9ifsi7m02z7nz";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.12.2" = {
      rev = "83f63aebf84eb7cadb3591f96dbe2b1f9257b519";
      sha256 = "01dv2cgbgfrj9ycaid2bns53xv3g9y26pdq22rnj4rgjkpgysfh8";
      prebuiltSha256 = {
        "2.11" = "04jiqxnil273ybfwmv2hv810xrz7v92vwy09m6dqjp6pzmhnirk2";
        "2.10" = "1rjcmr9zarbic06ybid6dwmqb5l64k2mqkqjhk8slkw0zsicvr0i";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.12.1" = {
      rev = "5fc454971b0bd720135f47648f5bc78d192e2d26";
      sha256 = "1vb7gd3jka33bd8937bcx3fs6126a124cadb6qf5l29cb6as0ywk";
      prebuiltSha256 = {
        "2.11" = "1jd09rpx8cmp3zm0plqs7rqa59y80dad88ccrjsh5w0hcvmg7iln";
        "2.10" = "03n9zm1ly7qpifqp57wmbq6izmn1p2zig93857kwj35lb2z3xr99";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.12.0" = {
      rev = "d694d9c0d7d99d1160e49572485fb297fa667ee1";
      sha256 = "0mimg2vqygvz6czh18fkbyh4njclw68m65xj3r696ysdb8vi9r6n";
      prebuiltSha256 = {
        "2.11" = "1va6va42lbysrnkwvm0r87x2l6ss59l2wipjfxkbzjkw1fkbipir";
        "2.10" = "0qq1hwnv6yypp5zxmpawbf9ynf0khqzv7ic8481fvgsxracg7dv9";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.11" "2.10";
      };
    };
    "1.11.6" = {
      rev = "2809d8d5616eebaa365c890717d30fd8a28368da";
      sha256 = "15xgvx6fw3xapbqbf4x7vz1fz8464yab8x8hrx3ahbnz54ynigf5";
      prebuiltSha256 = {
        "2.12" = "1lazby70n9abbnsk08fsmsv8y73zv2am1iinvjl5d6likxvim9av";
        "2.11" = "1pfv2vd83216brwfjqakdl8yry0b1a35g5kzs52y925pfbf70qvc";
        "2.10" = "055pnfvgqg2nayr0qrnymms0h60if1g06ffaj6cjqwpfasgflah4";
        "2.9.3" = "1wl6hrdn9ppplap6vylmwzrsjkd5x4a7q35ym9wvpcgxzin893qr";
      };
      scalaVersions = {
        inherit (scalaVersions) "2.12" "2.11" "2.10" "2.9.3";
      };
    };
    "1.11.5" = {
      rev = "229e54afc4700a9ccb70e18ee5527e01a004ee66";
      sha256 = "1yq26c1vq3d7k2xzf4cy8fnrg7b6zb1s4qpflqyyq7fmdxwh9ahb";
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
      sha256 = "0w6b6409h0z035mr4aamrqyqhiw90x3arnyi84g0dfzbdyapm6a8";
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
      sha256 = "1kcl44ddsnkjm8w54z6q23wykx0x0aiz1lsavwyg9xysp4plv3sd";
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
      sha256 = "0cckzc6f2hc77q9zn9izclli71iw98lm6gqadrpi2a0zaszigikv";
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
      sha256 = "1k572984pvr3pi0n48rc5gmvimml1gckj9ld1ny6bdai0k9wg1xn";
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
      sha256 = "1d90rfgxqfqn2k7gkl7b1729fsz9lqskybx0baki1vk3dyq9n8ps";
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
      sha256 = "1fj4c2vvqxccjsa0izshlfzxjsf5lvkfxwm8dfidwj12jvslpnqa";
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
