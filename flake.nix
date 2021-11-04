{
  description = "scalacheck.org";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/release-21.05";
  };

  outputs = { self, nixpkgs }:

    let

      inherit (nixpkgs.lib) singleton genAttrs optionalAttrs;
      inherit (builtins) mapAttrs;

      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      pkgsSets = genAttrs systems (system: import nixpkgs {
        inherit system;
        config = {
          allowUnfree = true;
        };
      });

    in {

      packages = mapAttrs (_: pkgs: {

        scalacheck-web = pkgs.callPackage ./scalacheck-web.nix {};

      }) pkgsSets;

    };
}
