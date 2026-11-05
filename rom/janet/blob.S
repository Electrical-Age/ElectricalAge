/* The stdlib's source, built into the ROM (main.c compiles it at boot). */
    .section .rodata
    .globl ea_janet_src
ea_janet_src:
    .incbin "janet/ea.janet"
    .byte 0
