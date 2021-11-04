# ScalaCheck Web Site

This repo is used to generate the [scalacheck.org](https://scalacheck.org/)
static web site.

The site is built with a combination of Nix and Hakyll. [nix
flakes](https://nixos.org/manual/nix/stable/command-ref/new-cli/nix3-flake.html)
is used for pinning `nixpkgs` so you need to use [nix
2.4](https://nixos.org/manual/nix/stable/release-notes/rl-2.4.html).

## Build Instructions

```sh
nix --extra-experimental-features "nix-command flakes" build .#scalacheck-web
```

You can then browse the site locally from `./result/index.html`

## Publish instructions

The following script builds the site and pushes the contents of `result` to the `gh-pages` branch:

```sh
./push-site.sh
```
