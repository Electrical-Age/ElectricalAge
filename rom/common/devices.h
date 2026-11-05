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

/* power */
#define POWER_CTRL        EA_IO(5, 0x00)   /* write 1: halt (power off), 2: reset */

#define SLOT_BYTES 0x100000u
#endif
