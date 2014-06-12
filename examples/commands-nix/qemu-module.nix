{ config, pkgs, ... }:

with pkgs.lib;

let

  cfg = config.deployment.libvirt;

in {

  options = {
    deployment.libvirt.memory = mkOption {
      default = 128;
      type = types.int;
      description = "The amount of memory in MB";
    };
    deployment.libvirt.mountHostStore = mkOption {
      default = true;
      type = types.bool;
      description = "Wether to mount /nix/store from the host";
    };
    deployment.libvirt.uuid = mkOption {
      default = "$UUID";
      type = types.string;
      description = ''
        The UUID of the machine. Set it to $UUID to automatically
        generate a UUID
      '';
    };
    deployment.libvirt.netdevs = mkOption {
      default = {};
      type = types.attrsOf types.optionSet;
      description = "netdevs";
      options = {
        vdeSocket = mkOption {
          type = types.string;
          description = "The path to the VDE ctl socket directory";
        };
        mac = mkOption {
          type = types.string;
          description = ''
            The MAC address of the device. You can use the special values
            $MAC0, $MAC1, $MAC2 etc to get generated MAC addresses.
          '';
        };
      };
    };
  };

  config = {

    boot = {
      kernelParams = [ "logo.nologo" ];
      initrd.kernelModules = [ "9p" "virtio_pci" "9pnet_virtio" ];
      kernelModules = [ "virtio_net" ];
      loader.grub.enable = false;
      vesa = false;
      postBootCommands = ''
        touch /etc/NIXOS
      '';
    };

    fileSystems."/" = {
      fsType = "tmpfs";
      device = "tmpfs";
      options = "mode=0755";
    };

    fileSystems."/nix/store" = mkIf cfg.mountHostStore {
      fsType = "9p";
      device = "nixstore";
      neededForBoot = true;
      options = "trans=virtio,version=9p2000.L,ro";
    };

  };

}
