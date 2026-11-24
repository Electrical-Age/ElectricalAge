# The robotics computer's ROMs

Images for the ROM chips of the computer (`mods.eln.robotics.computer`): RV32IMAFDC, machine mode, newlib.
A slot image starts at the slot's first byte; the boot vector is offset 0x1000 (page-aligned).

- `janet/`: Janet 1.41.2 (pinned in `flake.nix`) with its REPL on the terminal. Code runs from the ROM in slot 0;
  data, heap and stack live in the run of full RAM chips from slot 1 (at least two).
- `janet/ea.janet`: the stdlib (scheduler, REPL, bus, devices, drives, disks), compiled at boot; `janet/ea.c` and
  `janet/files.c` are its natives (devices, Break, and newlib's file calls over FatFs).
- `boot/`: the Floppy Boot ROM: loads drive A's sector 0 to 0x100000 (slot 2's RAM) and jumps there with a0 = 0 and
  a1 = the info page, for players' own systems.
- `disks/NAME/`: canned floppies (FAT12 images made with mtools), e.g. the Elevator floppy.
- `fatfs/`: FatFs R0.16 by ChaN (1-clause BSD, `fatfs/LICENSE.txt`), configured for two FAT12 floppies, A: and B:.
- `common/devices.h`: the device pages, as `hw.Machine` implements them (the screen channel's block-transfer
  registers too).
- `common/wire.h`, `wire.c`: a parser in C for Wire 2's canonical messages (what the screen sends), for systems of
  your own; 1.1 KB on RV32. Host test: `cc -o /tmp/wire_test common/wire.c common/wire_test.c && /tmp/wire_test`.

Build with `nix build .#roms` and copy `result/*.rom` to `src/main/resources/eln/computer/rom/` and `result/*.img` to
`src/main/resources/eln/computer/disks/`. The committed images
come from the Nix build, so they are reproducible; a local `make JANET_SRC=...` embeds local paths and differs.
