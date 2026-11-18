/* FatFs's disk layer over the floppy controller: drive 0 is A, 1 is B; sectors are 512 bytes, 2880 a disk. Drive 2 is
   RAM:, a ramdisk in the heap (Baughn 2026-11-18: as the Amiga had), made on first use and gone at a reset. */
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "ff.h"
#include "diskio.h"
#include "../common/devices.h"

#define RAM_SECTORS 384
static BYTE *ram;

DSTATUS disk_status(BYTE pdrv) {
    if (pdrv == 2) return ram ? 0 : STA_NOINIT;
    if (pdrv > 1) return STA_NOINIT;
    uint32_t s = FD_STATUS;
    if (!(s & (8u << pdrv))) return STA_NODISK | STA_NOINIT;
    return (s & (32u << pdrv)) ? STA_PROTECT : 0;
}

DSTATUS disk_initialize(BYTE pdrv) {
    if (pdrv == 2 && !ram) ram = calloc(RAM_SECTORS, 512);
    return disk_status(pdrv);
}

/* One transfer: start it, and sleep on WFI until the controller says done. */
static DRESULT transfer(BYTE pdrv, uint32_t cmd, const BYTE *buff, LBA_t sector, UINT count) {
    FD_STATUS = 0;                     /* clear an old done */
    FD_DRIVE = pdrv;
    FD_LBA = (uint32_t) sector;
    FD_ADDR = (uint32_t) (uintptr_t) buff;
    FD_COUNT = count;
    FD_IRQ = 1;
    FD_CMD = cmd;
    while (FD_STATUS & 1) {
        __asm__ volatile("csrs mie, %0" ::"r"(1u << 11));
        __asm__ volatile("wfi");
    }
    __asm__ volatile("csrc mie, %0" ::"r"(1u << 11));
    FD_IRQ = 0;
    uint32_t e = FD_ERROR;
    FD_STATUS = 0;
    if (e == 0) return RES_OK;
    if (e == 1) return RES_NOTRDY;
    if (e == 4) return RES_WRPRT;
    return RES_ERROR;
}

DRESULT disk_read(BYTE pdrv, BYTE *buff, LBA_t sector, UINT count) {
    if (pdrv == 2) {
        if (!ram || sector + count > RAM_SECTORS) return RES_ERROR;
        memcpy(buff, ram + sector * 512, count * 512);
        return RES_OK;
    }
    if (pdrv > 1) return RES_PARERR;
    return transfer(pdrv, 1, buff, sector, count);
}

DRESULT disk_write(BYTE pdrv, const BYTE *buff, LBA_t sector, UINT count) {
    if (pdrv == 2) {
        if (!ram || sector + count > RAM_SECTORS) return RES_ERROR;
        memcpy(ram + sector * 512, buff, count * 512);
        return RES_OK;
    }
    if (pdrv > 1) return RES_PARERR;
    return transfer(pdrv, 2, buff, sector, count);
}

DRESULT disk_ioctl(BYTE pdrv, BYTE cmd, void *buff) {
    if (pdrv > 2) return RES_PARERR;
    switch (cmd) {
        case CTRL_SYNC: return RES_OK;
        case GET_SECTOR_COUNT: *(LBA_t *) buff = pdrv == 2 ? RAM_SECTORS : 2880; return RES_OK;
        case GET_SECTOR_SIZE: *(WORD *) buff = 512; return RES_OK;
        case GET_BLOCK_SIZE: *(DWORD *) buff = 1; return RES_OK;
        default: return RES_PARERR;
    }
}
