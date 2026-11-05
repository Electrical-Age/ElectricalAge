/* The Floppy Boot ROM: load sector 0 of drive A (512 bytes) to 0x100000, the RAM chip in slot 2, and jump there
   with a0 = 0 (the drive) and a1 = 0x10000000 (the info page), as a PC's BIOS loads a boot sector. What it loads
   does the rest: a bootloader that reads more sectors, then a player's own system. */
    .section .init, "ax"
    .globl _start
_start:
    li t0, 0x10004000          /* the floppy controller */
    lw t1, 0x14(t0)            /* status: bit 3, a disk in A */
    andi t1, t1, 8
    beqz t1, no_disk
    sw zero, 0x14(t0)          /* clear an old done */
    sw zero, 0x00(t0)          /* drive A */
    sw zero, 0x04(t0)          /* sector 0 */
    li t2, 0x100000
    sw t2, 0x08(t0)            /* into slot 2's RAM */
    li t3, 1
    sw t3, 0x0C(t0)            /* one sector */
    sw t3, 0x10(t0)            /* read */
1:  lw t1, 0x14(t0)
    andi t1, t1, 1
    bnez t1, 1b                /* busy */
    lw t1, 0x1C(t0)            /* error code */
    bnez t1, no_read
    li a0, 0
    li a1, 0x10000000
    jr t2

no_disk:
    la a0, no_disk_msg
    j say
no_read:
    la a0, no_read_msg
say:
    li t0, 0x10002000          /* the terminal */
2:  lbu t1, 0(a0)
    beqz t1, 3f
    sw t1, 0(t0)
    addi a0, a0, 1
    j 2b
3:  li t0, 0x10005000          /* power: halt */
    li t1, 1
    sw t1, 0(t0)
    j 3b

no_disk_msg:
    .asciz "Floppy Boot: no disk in drive A.\n"
no_read_msg:
    .asciz "Floppy Boot: can't read drive A's sector 0 into slot 2 (a RAM chip there?).\n"
