/* The Floppy Boot ROM: all of it in slot 0's chip from the boot vector. */
OUTPUT_ARCH(riscv)
ENTRY(_start)
MEMORY { ROM (rx) : ORIGIN = 0x400, LENGTH = 0x100000 - 0x400 }
SECTIONS {
    .text : { KEEP(*(.init)) *(.text .text.* .rodata .rodata.*) } > ROM
    /DISCARD/ : { *(.comment) *(.riscv.attributes) *(.note*) *(.data*) *(.bss*) }
}
