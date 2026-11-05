# The computer's ROM images (Robotics). Needs a native cc and riscv32-none-elf-gcc (newlib); `nix build .#roms`
# from the repository root runs this with both. The images go to build/, and are committed under
# src/main/resources/eln/computer/rom/.
#   make JANET_SRC=/path/to/janet-1.41.2
CROSS ?= riscv32-none-elf-
CC_X = $(CROSS)gcc
OBJCOPY = $(CROSS)objcopy
JANET_SRC ?= $(error set JANET_SRC to a Janet 1.41.2 source tree)
CFLAGS = -Os -ffunction-sections -fdata-sections -Wall -Wno-unused-function -Ibuild -Ijanet
LDFLAGS = -nostartfiles --specs=nosys.specs -Wl,--gc-sections -Wl,-Map,build/janet.map

all: build/janet.rom

# Janet's amalgamation, its boot image regenerated with our configuration (no ev, threads, net, FFI, ...)
build/janet.c build/janet.h: janet/janetconf.h
	rm -rf build/janet-src && mkdir -p build && cp -r $(JANET_SRC) build/janet-src && chmod -R u+w build/janet-src
	cp janet/janetconf.h build/janet-src/src/conf/janetconf.h
	$(MAKE) -C build/janet-src build/c/janet.c
	cp build/janet-src/build/c/janet.c build/janet.c
	cp build/janet-src/src/include/janet.h build/janet.h

build/janet.elf: janet/start.S janet/syscalls.c janet/main.c janet/link.ld build/janet.c build/janet.h common/devices.h
	$(CC_X) $(CFLAGS) -T janet/link.ld $(LDFLAGS) janet/start.S janet/syscalls.c janet/main.c build/janet.c -lm -o $@

# The slot image: the boot vector is at offset 0x400
build/janet.rom: build/janet.elf
	$(OBJCOPY) -O binary $< build/janet.bin
	( head -c 1024 /dev/zero; cat build/janet.bin ) > $@
	@ls -l $@

clean:
	rm -rf build
.PHONY: all clean
