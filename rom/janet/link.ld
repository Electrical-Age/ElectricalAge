/* The Janet ROM: code and constants in the ROM chip in slot 0 from the boot vector, data in the RAM from slot 1. */
OUTPUT_ARCH(riscv)
ENTRY(_start)
MEMORY {
    ROM (rx)  : ORIGIN = 0x400, LENGTH = 0x100000 - 0x400
    RAM (rwx) : ORIGIN = 0x100000, LENGTH = 7M
}
SECTIONS {
    .text : { KEEP(*(.init)) *(.text .text.*) } > ROM
    .rodata : { *(.rodata .rodata.* .srodata .srodata.*) } > ROM
    .init_array : { PROVIDE_HIDDEN(__init_array_start = .); KEEP(*(SORT(.init_array.*))) KEEP(*(.init_array)) PROVIDE_HIDDEN(__init_array_end = .); } > ROM
    .fini_array : { PROVIDE_HIDDEN(__fini_array_start = .); KEEP(*(.fini_array)) PROVIDE_HIDDEN(__fini_array_end = .); } > ROM
    .preinit_array : { PROVIDE_HIDDEN(__preinit_array_start = .); PROVIDE_HIDDEN(__preinit_array_end = .); } > ROM
    .data : {
        _data = .;
        *(.data .data.*)
        . = ALIGN(8);
        __global_pointer$ = . + 0x800;
        *(.sdata .sdata.*)
        . = ALIGN(8);
        _edata = .;
    } > RAM AT> ROM
    _data_lma = LOADADDR(.data);
    .bss (NOLOAD) : {
        __bss_start = .;
        *(.sbss .sbss.* .bss .bss.* COMMON)
        . = ALIGN(16);
        _end = .;
    } > RAM
    _rom_end = _data_lma + SIZEOF(.data);
    /DISCARD/ : { *(.comment) *(.riscv.attributes) *(.note*) }
}
