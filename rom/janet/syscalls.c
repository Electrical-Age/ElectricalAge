/* newlib's system calls over the computer's devices: the console for stdin/stdout/stderr, the RAM for the heap. */
#include <errno.h>
#include <stdint.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/times.h>
#include "../common/devices.h"

uint32_t __ram_top;             /* set by start.S: the top of the RAM run, where the stack starts */
#define STACK_BYTES (256 * 1024)
extern char _end[];
static char *brk = _end;

static void wait_irq(uint32_t mask) {
    /* the interrupt handler turns the line off again (ea_irq), so it is turned on before each WFI */
    CON_IRQ(CONSOLE) = mask;
    while (!(CON_STATUS(CONSOLE) & mask)) {
        __asm__ volatile("csrs mie, %0" ::"r"(1u << 11));
        __asm__ volatile("wfi");
    }
    __asm__ volatile("csrc mie, %0" ::"r"(1u << 11));
    CON_IRQ(CONSOLE) = 0;
}

int file_read(int fd, char *buf, int n);
int file_write(int fd, const char *buf, int n);

int _write(int fd, const char *buf, int n) {
    if (fd >= 3) return file_write(fd, buf, n);
    if (fd != 1 && fd != 2) {
        errno = EBADF;
        return -1;
    }
    for (int i = 0; i < n; i++) {
        if (!(CON_STATUS(CONSOLE) & 2)) wait_irq(2);
        CON_TX(CONSOLE) = (unsigned char) buf[i];
    }
    return n;
}

int _read(int fd, char *buf, int n) {
    if (fd >= 3) return file_read(fd, buf, n);
    if (fd != 0) {
        errno = EBADF;
        return -1;
    }
    /* stdin is at its end: nobody types on the console (the screen talks to the scheduler), and a read waiting for it
       would stop every task */
    (void) buf;
    return 0;
}

void *_sbrk(ptrdiff_t incr) {
    char *limit = (char *) (__ram_top - STACK_BYTES);
    if (brk + incr > limit || brk + incr < _end) {
        errno = ENOMEM;
        return (void *) -1;
    }
    char *old = brk;
    brk += incr;
    return old;
}

void _exit(int code) {
    (void) code;
    for (;;) POWER_CTRL = 1;
}

int _link(const char *a, const char *b) { (void) a; (void) b; errno = EMLINK; return -1; }
int _kill(int pid, int sig) { (void) pid; (void) sig; errno = EINVAL; return -1; }
int _getpid(void) { return 1; }

/* Time: the ticks this computer has run (20 a second). */
static uint64_t ticks(void) {
    uint32_t hi, lo;
    do { hi = INFO_TICKS_HI; lo = INFO_TICKS_LO; } while (hi != INFO_TICKS_HI);
    return (uint64_t) hi << 32 | lo;
}
int _gettimeofday(struct timeval *tv, void *tz) {
    (void) tz;
    uint64_t t = ticks();
    tv->tv_sec = (time_t) (t / 20);
    tv->tv_usec = (suseconds_t) (t % 20) * 50000;
    return 0;
}
clock_t _times(struct tms *t) {
    clock_t c = (clock_t) ticks();
    t->tms_utime = c; t->tms_stime = 0; t->tms_cutime = 0; t->tms_cstime = 0;
    return c;
}

/* start.S's trap handler: an unexpected exception ends the program, saying where. */
void ea_trap(uint32_t cause, uint32_t epc, uint32_t tval) {
    static const char hex[] = "0123456789abcdef";
    char msg[] = "\nTrap: cause 00000000 at 00000000 (address 00000000); halted.\n";
    uint32_t v[3] = {cause, epc, tval};
    int at[3] = {13, 25, 43};
    for (int k = 0; k < 3; k++)
        for (int i = 0; i < 8; i++) msg[at[k] + i] = hex[v[k] >> (28 - 4 * i) & 15];
    for (const char *p = msg; *p; p++) {
        while (!(CON_STATUS(CONSOLE) & 2)) {}
        CON_TX(CONSOLE) = (unsigned char) *p;
    }
    for (;;) POWER_CTRL = 1;
}
