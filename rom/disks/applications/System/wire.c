/* Wire 2's canonical messages parsed in C (see wire.h). Public domain. */
#include "wire.h"

struct p {
    char *c;
    struct wire_val *v;
    int n, max;
};

static void skip(struct p *p) {
    while (*p->c == ' ' || *p->c == '\t') p->c++;
}

static int word_char(char c) {
    return c && c != ' ' && c != '\t' && c != '(' && c != ')' && c != '[' && c != ']' && c != '"';
}

static int value(struct p *p, int depth);

static int items(struct p *p, char close, int depth, int at) {
    int count = 0;
    for (;;) {
        skip(p);
        if (*p->c == close) {
            p->c++;
            if (at >= 0) p->v[at].len = count;
            return 0;
        }
        if (!*p->c || value(p, depth) < 0) return -1;
        count++;
    }
}

static int value(struct p *p, int depth) {
    if (p->n >= p->max) return -1;
    struct wire_val *v = &p->v[p->n];
    int at = p->n++;
    v->s = 0;
    v->len = 0;
    v->i = 0;
    char c = *p->c;
    if (c == '[') {
        if (depth >= 4) return -1;   /* (vectors at most three deep in a message) */
        p->c++;
        v->type = WIRE_VECTOR;
        if (items(p, ']', depth + 1, at) < 0) return -1;
    } else if (c == '"') {
        char *out = ++p->c;
        v->type = WIRE_STR;
        v->s = out;
        for (;;) {
            char ch = *p->c++;
            if (!ch) return -1;
            if (ch == '"') break;
            if (ch == '\\') {
                char e = *p->c;
                if (!e) return -1;
                p->c++;
                if (e == 'n') ch = '\n';
                else if (e == 't') ch = '\t';
                else if (e == 'r') ch = '\r';
                else if (e == '"' || e == '\\') ch = e;
                else if (e == 'x') {
                    int h = 0;
                    for (int k = 0; k < 2; k++) {
                        char d = *p->c;
                        if (!d) return -1;   /* (the line ended inside the escape) */
                        p->c++;
                        h = h * 16 + (d >= '0' && d <= '9' ? d - '0' : d >= 'a' && d <= 'f' ? d - 'a' + 10 : d >= 'A' && d <= 'F' ? d - 'A' + 10 : -1000);
                    }
                    if (h < 0) return -1;
                    ch = (char) h;
                } else return -1;
            }
            *out++ = ch;
        }
        v->len = (int32_t) (out - v->s);
        *out = 0;   /* (the unescaped text is never longer than the quoted: room for its end) */
    } else if (word_char(c)) {
        char *start = p->c;
        while (word_char(*p->c)) p->c++;
        char *end = p->c;
        char after = *end;
        int len = (int) (end - start);
        int neg = start[0] == '-', digits = len > neg;
        for (int k = neg; k < len; k++) if (start[k] < '0' || start[k] > '9') digits = 0;
        if (digits) {
            int32_t x = 0;
            for (int k = neg; k < len; k++) x = x * 10 + (start[k] - '0');
            v->type = WIRE_INT;
            v->i = neg ? -x : x;
        } else if (start[0] == ':') {
            v->type = WIRE_KEYWORD;
            v->s = start + 1;
            v->len = len - 1;
        } else if (len == 4 && start[0] == 't' && start[1] == 'r' && start[2] == 'u' && start[3] == 'e') v->type = WIRE_TRUE;
        else if (len == 5 && start[0] == 'f' && start[1] == 'a' && start[2] == 'l' && start[3] == 's' && start[4] == 'e') v->type = WIRE_FALSE;
        else if (len == 3 && start[0] == 'n' && start[1] == 'i' && start[2] == 'l') v->type = WIRE_NIL;
        else {
            v->type = WIRE_WORD;
            v->s = start;
            v->len = len;
        }
        (void) after;   /* (words and keywords are known by s and len, not ended in place: a bracket may follow) */
    } else return -1;
    p->v[at].next = p->n;
    return 0;
}

int wire_parse(char *line, struct wire_val *vals, int max) {
    struct p p = {line, vals, 0, max};
    skip(&p);
    if (*p.c++ != '(') return -1;
    skip(&p);
    if (!word_char(*p.c)) return -1;
    if (items(&p, ')', 1, -1) < 0) return -1;
    skip(&p);
    if (*p.c && *p.c != '\n' && *p.c != '\r') return -1;
    return p.n > 0 && vals[0].type == WIRE_WORD ? p.n : -1;
}
