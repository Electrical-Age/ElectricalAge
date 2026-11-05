/* The Janet ROM: boot Janet and run its REPL on the console. */
#include <stdio.h>
#include "janet.h"

int main(void) {
    janet_init();
    JanetTable *env = janet_core_env(NULL);
    printf("Janet %s on Electrical Age\n", JANET_VERSION);
    janet_dostring(env, "(repl nil nil (make-env))", "boot", NULL);
    janet_deinit();
    return 0;
}
