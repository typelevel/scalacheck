{ config, pkgs, ... }:

with pkgs.lib;

{
  imports = [ ./qemu-module.nix ./common.nix ];

  deployment.libvirt = {
    netdevs.netdev0.vdeSocket = "/run/vde0.ctl";
  };

  users.extraUsers.root.password = "root";
}
