"""Pixel fonts for the schematic glyph set.

FONT  - label font, cap height 7 rows. Stems 2px. Top/bottom bars 2px; a MIDDLE bar is 1px (7 rows cannot
        hold three 2px bars with gaps). Width 6 (M, W, V8 = 8; i, l, '.' = 2). Lowercase: x-height 5 rows,
        drawn 2 rows lower (LOWER_DROP); b, i, l, t are full height; g has a 2-row descender.
        'V8' is a wide V for single-letter meters. Symbols: + - = ~ / . % & ! $ and the degree sign
        '°' (a raised 5x4 ring; MARKS['temperature'] = 't°', the IEC thermistor cue).
        Lowercase available: a b e f g h i k l n o r s t u z (enough for Cu, Fe, Hz, kW, h, ...).
MICRO - 3x5 font with 1px strokes. ONLY for labels of 3+ characters that FONT cannot fit (PID, VCO,
        50V). It is the single exception to the 2px stroke rule; at GUI scale 3 (1.5x) its strokes
        render 1-2px unevenly, so keep MICRO labels short and never make them the defining element.

Use Glyph.text()/text_center()/micro() rather than these tables directly.
`python3 -m kit.font OUT.png` renders the sheet.
"""
from types import MappingProxyType

_F = """
A .XXXX. XXXXXX XX..XX XXXXXX XX..XX XX..XX XX..XX
B XXXXX. XXXXXX XX..XX XXXXX. XX..XX XXXXXX XXXXX.
C .XXXXX XXXXXX XX.... XX.... XX.... XXXXXX .XXXXX
D XXXXX. XXXXXX XX..XX XX..XX XX..XX XXXXXX XXXXX.
E XXXXXX XXXXXX XX.... XXXXX. XX.... XXXXXX XXXXXX
F XXXXXX XXXXXX XX.... XXXXX. XX.... XX.... XX....
G .XXXXX XXXXXX XX.... XX.XXX XX..XX XXXXXX .XXXXX
H XX..XX XX..XX XX..XX XXXXXX XX..XX XX..XX XX..XX
I XXXXXX XXXXXX ..XX.. ..XX.. ..XX.. XXXXXX XXXXXX
J ..XXXX ..XXXX ....XX ....XX XX..XX XXXXXX .XXXX.
K XX..XX XX.XX. XXXX.. XXX... XXXX.. XX.XX. XX..XX
L XX.... XX.... XX.... XX.... XX.... XXXXXX XXXXXX
M XX....XX XXX..XXX XXXXXXXX XX.XX.XX XX....XX XX....XX XX....XX
N XX..XX XXX.XX XXXXXX XX.XXX XX..XX XX..XX XX..XX
O .XXXX. XXXXXX XX..XX XX..XX XX..XX XXXXXX .XXXX.
P XXXXX. XXXXXX XX..XX XXXXX. XX.... XX.... XX....
Q .XXXX. XXXXXX XX..XX XX..XX XX.XX. XXXXXX .XX.XX
R XXXXX. XXXXXX XX..XX XXXXX. XX.XX. XX..XX XX..XX
S .XXXXX XXXXXX XX.... .XXXX. ....XX XXXXXX XXXXX.
T XXXXXX XXXXXX ..XX.. ..XX.. ..XX.. ..XX.. ..XX..
U XX..XX XX..XX XX..XX XX..XX XX..XX XXXXXX .XXXX.
V8 XX....XX XX....XX XX....XX .XX..XX. .XX..XX. ..XXXX.. ...XX...
V XX..XX XX..XX XX..XX XX..XX .XXXX. .XXXX. ..XX..
W XX....XX XX....XX XX....XX XX.XX.XX XXXXXXXX XXX..XXX XX....XX
X XX..XX XX..XX .XXXX. ..XX.. .XXXX. XX..XX XX..XX
Y XX..XX XX..XX .XXXX. ..XX.. ..XX.. ..XX.. ..XX..
Z XXXXXX XXXXXX ...XX. ..XX.. .XX... XXXXXX XXXXXX
0 .XXXX. XXXXXX XX..XX XX.XXX XXX.XX XXXXXX .XXXX.
1 ..XX.. .XXX.. XXXX.. ..XX.. ..XX.. XXXXXX XXXXXX
2 .XXXX. XXXXXX ....XX ..XXX. .XX... XXXXXX XXXXXX
3 XXXXX. XXXXXX ....XX ..XXX. ....XX XXXXXX XXXXX.
4 XX..XX XX..XX XX..XX XXXXXX ....XX ....XX ....XX
5 XXXXXX XXXXXX XX.... XXXXX. ....XX XXXXXX XXXXX.
6 .XXXXX XXXXXX XX.... XXXXX. XX..XX XXXXXX .XXXX.
7 XXXXXX XXXXXX ....XX ...XX. ..XX.. ..XX.. ..XX..
8 .XXXX. XXXXXX XX..XX .XXXX. XX..XX XXXXXX .XXXX.
9 .XXXX. XXXXXX XX..XX .XXXXX ....XX XXXXXX XXXXX.
+ ..XX.. ..XX.. XXXXXX XXXXXX ..XX.. ..XX..
- ...... ...... XXXXXX XXXXXX ...... ......
a .XXXX. ....XX .XXXXX XX..XX .XXXXX
b XX.... XX.... XXXXX. XXXXXX XX..XX XXXXXX XXXXX.
e .XXXX. XX..XX XXXXXX XX.... .XXXXX
g .XXXXX XX..XX XX..XX XXXXXX .XXXXX ....XX XXXXX.
i XX .. XX XX XX XX XX
l XX XX XX XX XX XX XX
n XXXXX. XXXXXX XX..XX XX..XX XX..XX
o .XXXX. XXXXXX XX..XX XXXXXX .XXXX.
r XX.XXX XXXXXX XXX... XX.... XX....
t .XX... .XX... XXXXX. .XX... .XX... .XXXXX ..XXXX
u XX..XX XX..XX XX..XX XXXXXX .XXXX.
= ...... XXXXXX XXXXXX ...... XXXXXX XXXXXX
~ .XXX...X XXXXX.XX XX.XXXXX X...XXX.
/ ....XX ....XX ...XX. ..XX.. .XX... XX.... XX....
. .. .. .. .. .. XX XX
% XX..XX XX..XX ...XX. ..XX.. .XX... XX..XX XX..XX
& .XXX.. XX.XX. .XXX.. XXX.XX XX.XX. XX..XX .XXX.X
! XX XX XX XX XX .. XX
$ ..XX.. .XXXXX XX.... .XXXX. ....XX XXXXX. ..XX..
f ..XXX. .XXXX. .XX... XXXXX. .XX... .XX... .XX...
h XX.... XX.... XXXXX. XXXXXX XX..XX XX..XX XX..XX
k XX.... XX.... XX..XX XX.XX. XXXX.. XX.XX. XX..XX
s .XXXXX XX.... .XXXX. ....XX XXXXX.
z XXXXXX ...XX. ..XX.. .XX... XXXXXX
° .XXX. XX.XX XX.XX .XXX. ..... ..... .....
"""
_M = """
A .X. X.X XXX X.X X.X
B XX. X.X XX. X.X XX.
C .XX X.. X.. X.. .XX
D XX. X.X X.X X.X XX.
E XXX X.. XX. X.. XXX
F XXX X.. XX. X.. X..
G .XX X.. X.X X.X .XX
H X.X X.X XXX X.X X.X
I XXX .X. .X. .X. XXX
J ..X ..X ..X X.X .X.
K X.X X.X XX. X.X X.X
L X.. X.. X.. X.. XXX
M X.X XXX XXX X.X X.X
N XX. X.X X.X X.X X.X
O .X. X.X X.X X.X .X.
P XX. X.X XX. X.. X..
Q .X. X.X X.X XX. .XX
R XX. X.X XX. X.X X.X
S .XX X.. .X. ..X XX.
T XXX .X. .X. .X. .X.
U X.X X.X X.X X.X XXX
V X.X X.X X.X X.X .X.
W X.X X.X XXX XXX X.X
X X.X X.X .X. X.X X.X
Y X.X X.X .X. .X. .X.
Z XXX ..X .X. X.. XXX
0 XXX X.X X.X X.X XXX
1 .X. XX. .X. .X. XXX
2 XX. ..X .X. X.. XXX
3 XX. ..X .X. ..X XX.
4 X.X X.X XXX ..X ..X
5 XXX X.. XX. ..X XX.
6 .XX X.. XXX X.X XXX
7 XXX ..X .X. .X. .X.
8 XXX X.X XXX X.X XXX
9 XXX X.X XXX ..X XX.
+ ... .X. XXX .X. ...
- ... ... XXX ... ...
= ... XXX ... XXX ...
~ ..... .X... X.X.X ...X. .....
. ... ... ... ... .X.
/ ..X ..X .X. X.. X..
% X.X ..X .X. X.. X.X
& .X. X.X .X. X.X .XX
"""


def _parse(s):
    d = {}
    for line in s.strip().splitlines():
        k, *rows = line.split()
        d[k] = rows
    return d


FONT = MappingProxyType({k: tuple(v) for k, v in _parse(_F).items()})     # read-only
MICRO = MappingProxyType({k: tuple(v) for k, v in _parse(_M).items()})
LOWER_DROP = frozenset('aegnorsuz')       # x-height glyphs: drawn 2 rows lower so they sit on the baseline
assert all(len(set(map(len, v))) == 1 for v in list(FONT.values()) + list(MICRO.values())), 'ragged glyph'


def text_width(s, font=None, gap=1):
    """Width in px of string s set in `font` (FONT by default) with `gap` px between glyphs.
    Multi-character keys such as 'V8' are not reachable through strings; use Glyph.bitmap for those."""
    font = FONT if font is None else font
    missing = [ch for ch in s if ch not in font]
    if missing:
        raise KeyError(f'no glyph for {missing} in this font; available: {"".join(sorted(k for k in font if len(k) == 1))}')
    return sum(len(font[ch][0]) for ch in s) + gap * (len(s) - 1) if s else 0


if __name__ == '__main__':
    import sys
    from PIL import Image
    S = 8
    keys, mk = list(FONT), list(MICRO)
    im = Image.new('RGBA', (20 * 10 * S, 6 * 12 * S), (139, 139, 139, 255))

    def blit(rows, x0, y0, c):
        for j, r in enumerate(rows):
            for q, ch in enumerate(r):
                if ch == 'X':
                    im.paste(c, (x0 + q * S, y0 + j * S, x0 + q * S + S, y0 + j * S + S))
    for i, k in enumerate(keys):
        blit(FONT[k], (i % 20) * 10 * S + S, (i // 20) * 12 * S + S + (2 * S if k in LOWER_DROP else 0), (238, 234, 222, 255))
    for i, k in enumerate(mk):
        blit(MICRO[k], (i % 20) * 10 * S + S, 4 * 12 * S + (i // 20) * 8 * S + S, (178, 212, 188, 255))
    im.save(sys.argv[1])
    print('font sheet', len(keys), 'label glyphs,', len(mk), 'micro glyphs')
