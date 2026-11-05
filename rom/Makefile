# The computer's ROM images (Robotics). Needs a native cc and riscv32-none-elf-gcc (newlib); `nix build .#roms`
# from the repository root runs this with both. The images go to build/, and are committed under
# src/main/resources/eln/computer/rom/.
#   make JANET_SRC=/path/to/janet-1.41.2
CROSS ?= riscv32-none-elf-
CC_X = $(CROSS)gcc
OBJCOPY = $(CROSS)objcopy
JANET_SRC ?= $(error set JANET_SRC to a Janet 1.41.2 source tree)
CFLAGS = -Os -ffunction-sections -fdata-sections -Wall -Wno-unused-function -Ibuild -Ijanet -Ifatfs
LDFLAGS = -nostartfiles --specs=nosys.specs -Wl,--gc-sections -Wl,-Map,build/janet.map

all: build/janet.rom build/boot.rom build/elevator.img

# Janet's amalgamation, its boot image regenerated with our configuration (no ev, threads, net, FFI, ...)
build/janet.c build/janet.h: janet/janetconf.h
	rm -rf build/janet-src && mkdir -p build && cp -r $(JANET_SRC) build/janet-src && chmod -R u+w build/janet-src
	cp janet/janetconf.h build/janet-src/src/conf/janetconf.h
	$(MAKE) -C build/janet-src build/c/janet.c
	cp build/janet-src/build/c/janet.c build/janet.c
	cp build/janet-src/src/include/janet.h build/janet.h

build/janet.elf: janet/start.S janet/blob.S janet/ea.janet janet/syscalls.c janet/main.c janet/ea.c janet/files.c janet/diskio.c fatfs/ff.c fatfs/ffunicode.c fatfs/ffconf.h janet/link.ld build/janet.c build/janet.h common/devices.h
	$(CC_X) $(CFLAGS) -T janet/link.ld $(LDFLAGS) janet/start.S janet/blob.S janet/syscalls.c janet/main.c janet/ea.c janet/files.c janet/diskio.c fatfs/ff.c fatfs/ffunicode.c build/janet.c -lm -o $@

# The slot image: the boot vector is at offset 0x400
build/janet.rom: build/janet.elf
	$(OBJCOPY) -O binary $< build/janet.bin
	( head -c 1024 /dev/zero; cat build/janet.bin ) > $@
	@ls -l $@

# The Floppy Boot ROM: a few instructions, no C
build/boot.rom: boot/boot.S boot/link.ld
	mkdir -p build
	$(CC_X) -nostdlib -nostartfiles -T boot/link.ld boot/boot.S -o build/boot.elf
	$(OBJCOPY) -O binary build/boot.elf build/boot.bin
	( head -c 1024 /dev/zero; cat build/boot.bin ) > $@
	@ls -l $@

# Canned floppies (FAT12, 1.44 MB): the files in disks/NAME. A fixed serial and the sources' times make them reproducible.
build/%.img: disks/%/*
	mkdir -p build
	rm -f $@
	mformat -C -i $@ -f 1440 -N 45410001 -v $(shell echo $* | tr a-z A-Z | cut -c1-11) ::
	mcopy -m -i $@ $^ ::
	@ls -l $@

clean:
	rm -rf build
.PHONY: all clean
