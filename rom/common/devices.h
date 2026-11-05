/* The computer's devices (mods.eln.robotics.computer.hw), one 4 KB page each from 0x1000_0000. */
#ifndef EA_DEVICES_H
#define EA_DEVICES_H
#include <stdint.h>

#define EA_IO(page, off) (*(volatile uint32_t *)(0x10000000u + (page) * 0x1000u + (off)))

/* info: read-only */
#define INFO_MAGIC        EA_IO(0, 0x00)   /* 'EAC1' */
#define INFO_SLOT_KIND(k) EA_IO(0, 0x10 + 4 * (k))   /* 0 none, 1 RAM, 2 ROM */
#define INFO_SLOT_SIZE(k) EA_IO(0, 0x30 + 4 * (k))   /* bytes */
#define INFO_TICKS_LO     EA_IO(0, 0x50)   /* ticks this computer has run (loaded and powered) */
#define INFO_TICKS_HI     EA_IO(0, 0x54)
#define INFO_RESET_CAUSE  EA_IO(0, 0x58)

/* timer */
#define TIMER_CMP_LO      EA_IO(1, 0x00)   /* the timer interrupt is pending while ticks >= compare */
#define TIMER_CMP_HI      EA_IO(1, 0x04)

/* console (terminal) and the editor channel: same registers */
#define CON_TX(p)         EA_IO(p, 0x00)   /* write a byte */
#define CON_RX(p)         EA_IO(p, 0x04)   /* read a byte, or -1 */
#define CON_STATUS(p)     EA_IO(p, 0x08)   /* bit 0: input waiting; bit 1: room to send */
#define CON_IRQ(p)        EA_IO(p, 0x0C)   /* bit 0: interrupt on input; bit 1: on room to send */
#define CONSOLE 2
#define EDITOR 7

/* the RS485 port */
#define BUS_TX_TARGET     EA_IO(3, 0x00)
#define BUS_TX_SOURCE     EA_IO(3, 0x04)
#define BUS_TX_TYPE       EA_IO(3, 0x08)
#define BUS_TX_INDEX      EA_IO(3, 0x0C)
#define BUS_TX_VALUE      EA_IO(3, 0x10)   /* float bits */
#define BUS_SEND          EA_IO(3, 0x14)   /* write 1: send as itself, 2: send as TX_SOURCE */
#define BUS_RX_COUNT      EA_IO(3, 0x20)
#define BUS_RX_POP        EA_IO(3, 0x24)   /* write: the oldest packet into the RX registers */
#define BUS_RX_TARGET     EA_IO(3, 0x28)
#define BUS_RX_SOURCE     EA_IO(3, 0x2C)
#define BUS_RX_TYPE       EA_IO(3, 0x30)
#define BUS_RX_INDEX      EA_IO(3, 0x34)
#define BUS_RX_VALUE      EA_IO(3, 0x38)
#define BUS_ADDRESS       EA_IO(3, 0x40)
#define BUS_IRQ           EA_IO(3, 0x44)   /* bit 0: interrupt while packets wait */
#define BUS_STATUS        EA_IO(3, 0x48)   /* bit 0: packets wait; bit 1: room to send; bit 2: on a bus */
#define BUS_DROPPED       EA_IO(3, 0x4C)
#define BUS_LEVEL(c)      EA_IO(3, 0x80 + 4 * (c))   /* float bits, as of the tick's start */

/* the floppy drives (A = 0, B = 1) */
#define FD_DRIVE          EA_IO(4, 0x00)
#define FD_LBA            EA_IO(4, 0x04)
#define FD_ADDR           EA_IO(4, 0x08)   /* RAM only */
#define FD_COUNT          EA_IO(4, 0x0C)   /* sectors of 512 bytes */
#define FD_CMD            EA_IO(4, 0x10)   /* write 1: read the disk into RAM, 2: write RAM to the disk */
#define FD_STATUS         EA_IO(4, 0x14)   /* bit 0 busy, 1 done, 2 error, 3/4 a disk in A/B, 5/6 A/B write-protected; write: clear done */
#define FD_IRQ            EA_IO(4, 0x18)   /* bit 0: interrupt when done */
#define FD_ERROR          EA_IO(4, 0x1C)   /* 1 no disk, 2 out of range, 3 not RAM, 4 write-protected, 5 busy, 6 bad command */
#define FD_CHANGED        EA_IO(4, 0x20)   /* bit per drive: its disk changed; write bits to clear */

/* power */
#define POWER_CTRL        EA_IO(5, 0x00)   /* write 1: halt (power off), 2: reset */
#define POWER_BREAK       EA_IO(5, 0x04)   /* read: 1 if Break was pressed (the software interrupt); write: clear it */

#define SLOT_BYTES 0x100000u
#endif
