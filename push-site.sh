#!/bin/sh

set -euxo pipefail

nix --extra-experimental-features "nix-command flakes" build .#scalacheck-web
REV=$(git rev-parse --short HEAD)
if [ -d target ]; then rm -rf target; fi
git worktree add -f target gh-pages
cp -r result/* target
cd target
git add -A .
git commit -m "Built site from $REV"
git push origin gh-pages
cd ..
