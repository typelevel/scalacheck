{ config, pkgs, ... }:

with pkgs.lib;

{
  imports = [ ./qemu-module.nix ./common.nix ];

  deployment.libvirt = {
    netdevs.netdev0.vdeSocket = "/run/vde0.ctl";
  };

  environment.systemPackages = [ pkgs.fping ];

  networking = {
    usePredictableInterfaceNames = false;
    firewall.enable = false;
    useDHCP = false;
  };

  services.openssh.enable = true;

  users.mutableUsers = false;

  users.extraUsers.root = {
    password = "root";
    hashedPassword = null;
    openssh.authorizedKeys.keyFiles = [ ./test-key_rsa.pub ];
  };
}
