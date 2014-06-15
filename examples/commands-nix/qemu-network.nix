network:

with (import <nixpkgs> {});
with lib;
with builtins;

let

  hosts = attrNames network;

  sys = name: import <nixpkgs/nixos> {
    configuration = getAttr name network;
  };

  system = name: (sys name).system;
  config = name: (sys name).config;

  # Only used to create a unique source for MAC generation that is guaranteed
  # to change if anything in the network changes.
  allSystems = runCommand "qemu-network-systems" {} ''
    mkdir -p $out
    ${concatMapStrings (s: "ln -s ${s} $out/;") (map system hosts)}
  '';

  mkNetdev = n: attrs: ''
    <qemu:arg value='-netdev'/>
    <qemu:arg value='vde,sock=${attrs.vdeSocket},id=${n}'/>
    <qemu:arg value='-device'/>
    <qemu:arg value='virtio-net-pci,netdev=${n},mac=${attrs.mac}'/>
  '';

  mkNetdevQemu = n: attrs: ''
    -netdev \
    vde,sock=${attrs.vdeSocket},id=${n} \
    -device \
    virtio-net-pci,netdev=${n},mac=${attrs.mac} \
  '';

  libvirtxml = name: let
    sys = system name;
    cfg = (config name).deployment.libvirt;
  in runCommand "libvirtxml" {} ''
    MAC0=$(echo "${name}${allSystems}0" | md5sum | \
      sed 's/^\(..\)\(..\)\(..\)\(..\)\(..\).*$/02:\1:\2:\3:\4:\5/')
    MAC1=$(echo "${name}${allSystems}1" | md5sum | \
      sed 's/^\(..\)\(..\)\(..\)\(..\)\(..\).*$/02:\1:\2:\3:\4:\5/')
    MAC2=$(echo "${name}${allSystems}2" | md5sum | \
      sed 's/^\(..\)\(..\)\(..\)\(..\)\(..\).*$/02:\1:\2:\3:\4:\5/')
    MAC3=$(echo "${name}${allSystems}3" | md5sum | \
      sed 's/^\(..\)\(..\)\(..\)\(..\)\(..\).*$/02:\1:\2:\3:\4:\5/')
    MAC4=$(echo "${name}${allSystems}4" | md5sum | \
      sed 's/^\(..\)\(..\)\(..\)\(..\)\(..\).*$/02:\1:\2:\3:\4:\5/')
    UUID=$(echo "${name}${allSystems}" | md5sum | \
      sed 's/^\(........\)\(....\)\(....\)\(....\)\(............\).*$/\1-\2-\3-\4-\5/')
    mkdir $out
    cat << EOF > $out/qemu-cmd
      ${qemu}/bin/qemu-system-x86_64 \
        -m ${toString cfg.memory} \
        -kernel ${sys}/kernel \
        -initrd ${sys}/initrd \
        -append "$(cat ${sys}/kernel-params) init=${sys}/init" \
        -fsdev \
        local,id=nixstore,path=/nix/store,security_model=none,readonly \
        -device \
        virtio-9p-pci,fsdev=nixstore,mount_tag=nixstore \
        -fsdev \
        local,id=nixvar,path=/nix/var,security_model=none,readonly \
        -device \
        virtio-9p-pci,fsdev=nixvar,mount_tag=nixvar \
        ${concatStrings (mapAttrsToList mkNetdevQemu cfg.netdevs)}

    EOF
    chmod +x $out/qemu-cmd

    cat << EOF > $out/libvirt.xml
    <domain type='kvm' xmlns:qemu='http://libvirt.org/schemas/domain/qemu/1.0'>
      <name>${name}</name>
      <uuid>${cfg.uuid}</uuid>
      <memory unit='M'>${toString cfg.memory}</memory>
      <currentMemory unit='M'>${toString cfg.memory}</currentMemory>
      <vcpu placement='static'>2</vcpu>
      <os>
        <type>hvm</type>
        <kernel>${sys}/kernel</kernel>
        <initrd>${sys}/initrd</initrd>
        <cmdline>$(cat ${sys}/kernel-params) console=ttyS0 init=${sys}/init</cmdline>
      </os>
      <features><acpi/></features>
      <cpu><model>kvm64</model></cpu>
      <clock offset='utc'/>
      <on_poweroff>destroy</on_poweroff>
      <on_reboot>restart</on_reboot>
      <on_crash>destroy</on_crash>
      <devices>
        <emulator>${qemu}/bin/qemu-system-x86_64</emulator>
        <memballoon model='virtio'/>
        <serial type='pty'><target port='0'/></serial>
        <console type='pty'><target type='serial' port='0'/></console>
      </devices>
      <qemu:commandline>
        <qemu:arg value='-fsdev'/>
        <qemu:arg value='local,id=nixstore,path=/nix/store,security_model=none,readonly'/>
        <qemu:arg value='-device'/>
        <qemu:arg value='virtio-9p-pci,fsdev=nixstore,mount_tag=nixstore'/>
        <qemu:arg value='-fsdev'/>
        <qemu:arg value='local,id=nixvar,path=/nix/var,security_model=none,readonly'/>
        <qemu:arg value='-device'/>
        <qemu:arg value='virtio-9p-pci,fsdev=nixvar,mount_tag=nixvar'/>
        ${concatStrings (mapAttrsToList mkNetdev cfg.netdevs)}
      </qemu:commandline>
    </domain>
    EOF
  '';

in runCommand "qemu-network" {} ''
  mkdir -p $out
  ${concatStrings (map (n: "ln -sfT ${libvirtxml n}/libvirt.xml $out/${n}.xml;") hosts)}
  ${concatStrings (map (n: "ln -sfT ${libvirtxml n}/qemu-cmd $out/${n}.cmd;") hosts)}
''
