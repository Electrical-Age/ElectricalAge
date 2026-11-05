# The robotics computer's ROMs

Images for the ROM chips of the computer (`mods.eln.robotics.computer`): RV32IMAFDC, machine mode, newlib.
A slot image starts at the slot's first byte; the boot vector is offset 0x400.

- `janet/`: Janet 1.41.2 (pinned in `flake.nix`) with its REPL on the terminal. Code runs from the ROM in slot 0;
  data, heap and stack live in the run of full RAM chips from slot 1 (at least two).
- `janet/ea.janet`: the stdlib (scheduler, REPL, bus, devices, drives, disks), compiled at boot; `janet/ea.c` and
  `janet/files.c` are its natives (devices, Break, and newlib's file calls over FatFs).
- `fatfs/`: FatFs R0.16 by ChaN (1-clause BSD, `fatfs/LICENSE.txt`), configured for two FAT12 floppies, A: and B:.
- `common/devices.h`: the device pages, as `hw.Machine` implements them.

Build with `nix build .#roms` and copy `result/*.rom` to `src/main/resources/eln/computer/rom/`. The committed images
come from the Nix build, so they are reproducible; a local `make JANET_SRC=...` embeds local paths and differs.
