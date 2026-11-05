#!/usr/bin/env bash
# Builds the official riscv-tests (physical-memory "p" env) for the computer's CPU and packs them for the JUnit
# suite: one flat image per test (linked and loaded at 0x100000, the computer's slot 1) plus index.txt (name, tohost offset).
# Usage: tools/rv32/build-riscv-tests.sh RISCV_TESTS_CHECKOUT OUT_DIR
# Needs riscv32-none-elf-gcc (nix-shell -p pkgsCross.riscv32-embedded.buildPackages.gcc).
set -eu
src=$(cd "$1" && pwd); out=$2
mkdir -p "$out"; rm -f "$out"/*.bin "$out"/index.txt
tmp=$(mktemp -d)
sed 's/0x80000000/0x00100000/' "$src"/env/p/link.ld > "$tmp/link.ld"
for suite in rv32ui rv32um rv32ua rv32uf rv32ud rv32uc rv32mi; do
  case $suite in
    rv32uc) march=rv32gc_zifencei_zicsr ;;
    rv32mi) march=rv32gc_zifencei_zicsr ;;
    *) march=rv32g_zifencei_zicsr ;;
  esac
  for s in "$src"/isa/$suite/*.S; do
    t=$(basename "$s" .S)
    name=$suite-p-$t
    if ! riscv32-none-elf-gcc -march=$march -mabi=ilp32 -static -mcmodel=medany -fvisibility=hidden -nostdlib \
        -nostartfiles -I"$src"/env/p -I"$src"/isa/macros/scalar -T"$tmp/link.ld" "$s" -o "$tmp/$name" 2>"$tmp/err"; then
      echo "skip $name: $(head -c 200 "$tmp/err")"; continue
    fi
    riscv32-none-elf-objcopy -O binary "$tmp/$name" "$out/$name.bin"
    base=$(riscv32-none-elf-nm "$tmp/$name" | awk '$3=="_start"{print $1}')
    host=$(riscv32-none-elf-nm "$tmp/$name" | awk '$3=="tohost"{print $1}')
    printf '%s %d\n' "$name" $(( 0x$host - 0x$base )) >> "$out/index.txt"
  done
done
rm -rf "$tmp"
wc -l "$out/index.txt"; du -sh "$out"
