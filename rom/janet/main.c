/* The Janet ROM: boot Janet, compile the stdlib (ea.janet) and start its scheduler with the REPL on the terminal. */
#include <stdint.h>
#include <stdio.h>
#include "janet.h"
#include "../common/devices.h"

extern const char ea_janet_src[];
void ea_register(JanetTable *env);
void ea_irq(uint32_t cause);
void ea_trap(uint32_t cause, uint32_t epc, uint32_t tval);

/* Every trap comes here: interrupts go to ea_irq, an exception ends the program (ea_trap says where). */
__attribute__((interrupt("machine"))) void ea_trap_entry(void) {
    uint32_t cause, epc, tval;
    __asm__ volatile("csrr %0, mcause" : "=r"(cause));
    if ((int32_t) cause < 0) {
        ea_irq(cause & 0x7FFFFFFFu);
        return;
    }
    __asm__ volatile("csrr %0, mepc" : "=r"(epc));
    __asm__ volatile("csrr %0, mtval" : "=r"(tval));
    ea_trap(cause, epc, tval);
}

int main(void) {
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
    __asm__ volatile("csrw mtvec, %0" ::"r"(ea_trap_entry));
    __asm__ volatile("csrs mie, %0" ::"r"(1u << 3));      /* Break: the software interrupt */
    __asm__ volatile("csrs mstatus, %0" ::"r"(1u << 3));  /* interrupts on */
    janet_init();
    JanetTable *env = janet_core_env(NULL);
    ea_register(env);
    printf("Janet %s on Electrical Age\n", JANET_VERSION);
    if (janet_dostring(env, ea_janet_src, "ea.janet", NULL) == 0)
        janet_dostring(env, "(ea/start)", "boot", NULL);
    janet_deinit();
    return 0;
}
