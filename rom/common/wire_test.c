/* Host test of wire.c: cc -o /tmp/wire_test rom/common/wire.c rom/common/wire_test.c && /tmp/wire_test */
#include <stdio.h>
#include <string.h>
#include "wire.h"

static int fails;
#define CHECK(c) do { if (!(c)) { printf("FAIL line %d: %s\n", __LINE__, #c); fails++; } } while (0)

static int eq(struct wire_val *v, const char *s) {
    return (int) strlen(s) == v->len && strncmp(v->s, s, v->len) == 0;
}

int main(void) {
    struct wire_val v[64];
    char a[] = "(save 2 4 7 \"r1\" [[1 1 [\"TWO\" \"2b\"]]])";
    int n = wire_parse(a, v, 64);
    CHECK(n == 12);
    CHECK(v[0].type == WIRE_WORD && eq(&v[0], "save"));
    CHECK(v[1].type == WIRE_INT && v[1].i == 2);
    CHECK(v[3].i == 7);
    CHECK(v[4].type == WIRE_STR && eq(&v[4], "r1"));
    CHECK(v[5].type == WIRE_VECTOR && v[5].len == 1);
    CHECK(v[6].type == WIRE_VECTOR && v[6].len == 3);
    CHECK(v[7].i == 1 && v[8].i == 1);
    CHECK(v[9].type == WIRE_VECTOR && v[9].len == 2);
    CHECK(eq(&v[10], "TWO") && eq(&v[11], "2b") && v[5].next == 12);
    char b[] = "(submit 1 2 5 \"a\\\"b\\nc\\x41\\\\\")";
    n = wire_parse(b, v, 64);
    CHECK(n == 5 && eq(&v[4], "a\"b\ncA\\"));
    char c[] = "(wire 2 :modes [[80 25] [120 40]] :pens [\"Grid Blue\" \"Amber\"] :ok true :no false :none nil :neg -7)";
    n = wire_parse(c, v, 64);
    CHECK(n > 0);
    CHECK(v[2].type == WIRE_KEYWORD && eq(&v[2], "modes"));
    CHECK(v[3].type == WIRE_VECTOR && v[3].len == 2);
    int k = v[3].next;
    CHECK(v[k].type == WIRE_KEYWORD && eq(&v[k], "pens"));
    char d[] = "(press 2 3)";
    CHECK(wire_parse(d, v, 64) == 3);
    char e[] = "(a [[[[1]]]])";
    CHECK(wire_parse(e, v, 64) == -1);   /* too deep */
    char f[] = "(text 1 \"unclosed)";
    CHECK(wire_parse(f, v, 64) == -1);
    char g[] = "(a 1) (b 2)";
    CHECK(wire_parse(g, v, 64) == -1);
    char h[] = "(press 2 3)";
    CHECK(wire_parse(h, v, 2) == -1);    /* no room */
    char i[] = "(keyup 2 9 \"left\" 1)";
    CHECK(wire_parse(i, v, 64) == 5 && eq(&v[3], "left") && v[4].i == 1);
    char j[] = "(a \"x\\x";   /* (ends inside an escape: refused, nothing read past it) */
    CHECK(wire_parse(j, v, 64) == -1);
    char l[] = "(a \"x\\";
    CHECK(wire_parse(l, v, 64) == -1);
    printf(fails ? "%d failed\n" : "wire: all passed\n", fails);
    return fails != 0;
}
