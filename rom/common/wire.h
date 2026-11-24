/* Wire 2's canonical messages, parsed in C: for a system of your own booted from the Floppy Boot ROM, which has no
   Janet to read the screen's events with. The screen always sends this subset of Janet's syntax: one message a line,
   a list in ( ) of words (the verb first), integers, strings (escapes \" \\ \n \t \r and \xNN only), keywords
   (:name), true, false, nil and vectors in [ ] at most three deep (a save's hunks are three). No allocation: values go into an array the caller
   gives, a vector's items right after it. Public domain; copy it into your system. */
#ifndef EA_WIRE_H
#define EA_WIRE_H
#include <stdint.h>

enum wire_type { WIRE_INT, WIRE_STR, WIRE_KEYWORD, WIRE_WORD, WIRE_TRUE, WIRE_FALSE, WIRE_NIL, WIRE_VECTOR };

struct wire_val {
    enum wire_type type;
    int32_t i;           /* WIRE_INT */
    char *s;             /* WIRE_STR: unescaped in place and NUL-ended; WIRE_KEYWORD (without its colon), WIRE_WORD:
                            in the line, len long (not ended: a bracket may follow) */
    int32_t len;         /* string length; for WIRE_VECTOR, how many items follow it (nested vectors count as one) */
    int32_t next;        /* index of the value after this one (past a vector's items) */
};

/* Parses one message line (changed in place: strings are unescaped into it). vals[0] is the verb, then the
   message's parts in order; returns how many values it filled, or -1 if the line isn't a canonical message or needs
   more than max values. */
int wire_parse(char *line, struct wire_val *vals, int max);

#endif
