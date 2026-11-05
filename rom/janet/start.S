/* Janet ROM start: find the RAM, set up the stack, copy .data, zero .bss, run main. */
    .section .init, "ax"
    .globl _start
_start:
    .option push
    .option norelax
    la gp, __global_pointer$
    .option pop
    /* the run of full RAM chips from slot 1: its top is the stack's */
    li t0, 0x10000000          /* info page */
    li t1, 1
    li t2, 0x100000
1:  li t3, 8
    bge t1, t3, 2f
    slli t4, t1, 2
    add t4, t4, t0
    lw t5, 0x10(t4)            /* slot kind */
    li t6, 1
    bne t5, t6, 2f
    lw t5, 0x30(t4)            /* chip size */
    slli t3, t1, 20
    add t2, t3, t5
    li t6, 0x100000
    bne t5, t6, 2f             /* a smaller chip ends the run */
    addi t1, t1, 1
    j 1b
2:  li t3, 0x300000            /* at least 2 MB: slots 1 and 2 */
    bltu t2, t3, no_ram
    mv sp, t2
    li t0, 0x6000              /* mstatus.FS: the FPU on */
    csrs mstatus, t0
    csrw fcsr, zero
    la t0, trap_entry
    csrw mtvec, t0
    la a0, _data_lma
    la a1, _data
    la a2, _edata
3:  bgeu a1, a2, 4f
    lw t0, 0(a0)
    sw t0, 0(a1)
    addi a0, a0, 4
    addi a1, a1, 4
    j 3b
4:  la a0, __bss_start
    la a1, _end
5:  bgeu a0, a1, 6f
    sw zero, 0(a0)
    addi a0, a0, 4
    j 5b
6:  la t0, __ram_top
    sw t2, 0(t0)
    call __libc_init_array
    call main
    call exit
7:  j 7b

no_ram:
    la a0, no_ram_msg
    li t0, 0x10002000          /* console */
8:  lbu t1, 0(a0)
    beqz t1, 9f
    sw t1, 0(t0)
    addi a0, a0, 1
    j 8b
9:  li t0, 0x10005000          /* power: halt */
    li t1, 1
    sw t1, 0(t0)
    j 9b

/* An unexpected trap (a bad pointer, an illegal instruction): say where, and halt. */
    .align 2
trap_entry:
    csrr a0, mcause
    csrr a1, mepc
    csrr a2, mtval
    la sp, __ram_top
    lw sp, 0(sp)
    call ea_trap
10: j 10b

    .section .rodata
no_ram_msg:
    .asciz "Janet needs at least 2 MB of RAM: full RAM chips in slots 1 and 2.\n"
