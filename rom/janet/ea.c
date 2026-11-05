/* The ea module: Janet's view of the computer's devices (the stdlib in ea.janet builds on it). */
#include <stdint.h>
#include <string.h>
#include "janet.h"
#include "../common/devices.h"

static uint64_t ticks(void) {
    uint32_t hi, lo;
    do { hi = INFO_TICKS_HI; lo = INFO_TICKS_LO; } while (hi != INFO_TICKS_HI);
    return (uint64_t) hi << 32 | lo;
}

static uint32_t fbits(float f) { uint32_t u; memcpy(&u, &f, 4); return u; }
static float bitsf(uint32_t u) { float f; memcpy(&f, &u, 4); return f; }

/* Set while a task fiber runs, so Break interrupts the task, not the scheduler; and whether it did. */
static volatile int task_running, interrupted;
static volatile int break_seen;

JANET_FN(cfun_send, "(ea/send target type index value &opt source)",
         "Send a bus packet: target 0 broadcast, 1-16 the colour channels, 17-255 devices; type by number. "
         "With source, send as that address (the raw API); else as the computer's own. False if the send buffer is full.") {
    janet_arity(argc, 4, 5);
    BUS_TX_TARGET = (uint32_t) janet_getinteger(argv, 0) & 0xFF;
    BUS_TX_TYPE = (uint32_t) janet_getinteger(argv, 1) & 0xFF;
    BUS_TX_INDEX = (uint32_t) janet_getinteger(argv, 2) & 0xFF;
    BUS_TX_VALUE = fbits((float) janet_getnumber(argv, 3));
    if (!(BUS_STATUS & 2)) return janet_wrap_false();
    if (argc == 5) {
        BUS_TX_SOURCE = (uint32_t) janet_getinteger(argv, 4) & 0xFF;
        BUS_SEND = 2;
    } else BUS_SEND = 1;
    return janet_wrap_true();
}

JANET_FN(cfun_recv, "(ea/recv)", "The oldest packet waiting, as [target source type index value], or nil.") {
    janet_fixarity(argc, 0);
    (void) argv;
    if (BUS_RX_COUNT == 0) return janet_wrap_nil();
    BUS_RX_POP = 1;
    Janet t[5] = {janet_wrap_integer((int32_t) BUS_RX_TARGET), janet_wrap_integer((int32_t) BUS_RX_SOURCE),
                  janet_wrap_integer((int32_t) BUS_RX_TYPE), janet_wrap_integer((int32_t) BUS_RX_INDEX),
                  janet_wrap_number(bitsf(BUS_RX_VALUE))};
    return janet_wrap_tuple(janet_tuple_n(t, 5));
}

JANET_FN(cfun_level, "(ea/level channel)", "A colour channel's level (0 white .. 15 black), 0..1, as of this tick.") {
    janet_fixarity(argc, 1);
    int32_t c = janet_getinteger(argv, 0);
    if (c < 0 || c > 15) janet_panic("channels are 0 to 15");
    return janet_wrap_number(bitsf(BUS_LEVEL(c)));
}

JANET_FN(cfun_address, "(ea/address)", "The computer's bus address (0: none set), and whether it is on a bus.") {
    janet_fixarity(argc, 0);
    (void) argv;
    Janet t[2] = {janet_wrap_integer((int32_t) BUS_ADDRESS), janet_wrap_boolean(BUS_STATUS & 4)};
    return janet_wrap_tuple(janet_tuple_n(t, 2));
}

JANET_FN(cfun_ticks, "(ea/ticks)", "Game ticks this computer has run (20 a second; only while loaded and powered).") {
    janet_fixarity(argc, 0);
    (void) argv;
    return janet_wrap_number((double) ticks());
}

JANET_FN(cfun_input, "(ea/input)", "The terminal's typed bytes waiting, as a string (empty if none).") {
    janet_fixarity(argc, 0);
    (void) argv;
    uint8_t buf[256];
    int32_t n = 0;
    while (n < (int32_t) sizeof buf && (CON_STATUS(CONSOLE) & 1)) {
        int32_t c = (int32_t) CON_RX(CONSOLE);
        if (c < 0) break;
        buf[n++] = (uint8_t) c;
    }
    return janet_stringv(buf, n);
}

JANET_FN(cfun_wait, "(ea/wait &opt until)",
         "Sleep until a packet or a keystroke arrives, Break is pressed, or the tick count reaches until.") {
    janet_arity(argc, 0, 1);
    uint64_t until = argc > 0 && !janet_checktype(argv[0], JANET_NIL) ? (uint64_t) janet_getnumber(argv, 0) : UINT64_MAX;
    if (break_seen || BUS_RX_COUNT || (CON_STATUS(CONSOLE) & 1) || ticks() >= until) return janet_wrap_nil();
    TIMER_CMP_HI = 0xFFFFFFFFu;
    TIMER_CMP_LO = (uint32_t) until;
    TIMER_CMP_HI = (uint32_t) (until >> 32);
    BUS_IRQ = 1;
    CON_IRQ(CONSOLE) = 1;
    /* WFI wakes on any enabled pending interrupt; the handler (ea_irq) turns the line off again */
    __asm__ volatile("csrs mie, %0" ::"r"((1u << 11) | (1u << 7)));
    __asm__ volatile("wfi");
    __asm__ volatile("csrc mie, %0" ::"r"((1u << 11) | (1u << 7)));
    BUS_IRQ = 0;
    CON_IRQ(CONSOLE) = 0;
    TIMER_CMP_HI = 0xFFFFFFFFu;
    TIMER_CMP_LO = 0xFFFFFFFFu;
    return janet_wrap_nil();
}

JANET_FN(cfun_run, "(ea/run fiber value)",
         "Resume a task's fiber with value until it yields, ends or fails (the scheduler's business). Break interrupts "
         "only a task: the interrupt is handled here, before the scheduler runs on.") {
    janet_fixarity(argc, 2);
    JanetFiber *f = janet_getfiber(argv, 0);
    Janet out = janet_wrap_nil();
    interrupted = 0;
    task_running = 1;
    janet_continue(f, argv[1], &out);
    task_running = 0;
    if (interrupted) {
        interrupted = 0;
        janet_interpreter_interrupt_handled(NULL);
    }
    return out;
}

JANET_FN(cfun_break, "(ea/break?)", "Was Break pressed since last asked? Clears it.") {
    janet_fixarity(argc, 0);
    (void) argv;
    int b = break_seen;
    break_seen = 0;
    return janet_wrap_boolean(b);
}

JANET_FN(cfun_halt, "(ea/halt)", "Power the computer off (it stays off until reset).") {
    janet_fixarity(argc, 0);
    (void) argv;
    for (;;) POWER_CTRL = 1;
}

JANET_FN(cfun_reset, "(ea/reset)", "Start the computer again from slot 1's chip.") {
    janet_fixarity(argc, 0);
    (void) argv;
    for (;;) POWER_CTRL = 2;
}

/* Interrupts: Break (the software interrupt) stops a running task; the others only end a WFI. */
void ea_irq(uint32_t cause) {
    if (cause == 3) {
        POWER_BREAK = 0;
        break_seen = 1;
        if (task_running && !interrupted) {
            interrupted = 1;
            janet_interpreter_interrupt(NULL);
        }
    } else {
        uint32_t bit = 1u << cause;
        __asm__ volatile("csrc mie, %0" ::"r"(bit));
    }
}

void ea_register(JanetTable *env) {
    JanetRegExt fns[] = {
        JANET_REG("ea/send", cfun_send), JANET_REG("ea/recv", cfun_recv), JANET_REG("ea/level", cfun_level),
        JANET_REG("ea/address", cfun_address), JANET_REG("ea/ticks", cfun_ticks), JANET_REG("ea/input", cfun_input),
        JANET_REG("ea/wait", cfun_wait), JANET_REG("ea/run", cfun_run), JANET_REG("ea/break?", cfun_break),
        JANET_REG("ea/halt", cfun_halt), JANET_REG("ea/reset", cfun_reset), JANET_REG_END};
    janet_cfuns_ext(env, "ea", fns);
}
