"""Chip family: the logic/analog chip package (models logicgates / analogchips) and its faces.

DECIDED CENTRALLY: every logic and analog chip is ``vanilla_size = 32`` (``kit.decisions``).
At 16 px half the faces (NOR, XNOR, PAL, the oscillators, every 3-letter analog face) cannot
be drawn without touching the marks, and the ones that fit are cramped.  At 32 px the package
is the 16 px plan x2 (2 px outline, depth 4) and every face is drawn at ONE stroke weight:
2 px strokes, i.e. a half-resolution bitmap drawn at 2x.

The faces are a frozen table, ``CHIP_FACES``: item id -> kind, marks and a half-res bitmap
transcribed from the in-world texture (model/logicgates/logicgates.png,
model/analogchips/analogchips.png) and simplified to fit.  A chip icon is one call:

    chip_icon('nor_chip')            # the production icon, 32 px

Bitmap characters: '#' glyph colour (GLYPH for logic, GLYPH_A for analog), 'a' the dimmer
second ink (GLYPH_DIM / GLYPH_A_DIM: PAL's A, the S&H tick), 'r' MARK_RED, 'g' MARK_GREEN,
'b' MARK_BLUE, 'W' WHITE, '.' or ' ' nothing.  A negation bar is simply the first bitmap row,
followed by one empty row (as on the textures).

Resolution: a face is normally a HALF-RES bitmap drawn at 2x (every cell = 2x2 px).  A face
that needs 1 px letter spacing to fit (VCA, PID, LPF) is given at FULL resolution with
``scale=1`` and drawn with 2 px strokes by hand: the stroke weight is the same 2 px either way.

Marks: ``marks`` picks which of 't' (top, blue), 'l' (left input, red), 'b' (bottom, green) and
'o' (output square, white) are drawn.  ``mark_style`` swaps a mark's bar for a texture shape
(``MARK_SHAPES``: 'caret' for b = clock caret, 'gt' for l = clock '>'); ``mark_at`` moves a mark
(top-left, canvas px) where the texture has it elsewhere (VCA's input mark sits high-left).

Placement (``place_face``): the bitmap goes inside the free face (``CHIP_GEOM[size]['face']``)
at the position closest to the face centre where NO inked pixel is on, or 4-adjacent to, a
mark or the output square.  Faces that cannot be placed raise ValueError - that is how the
"glyph touches the output square" bug of the 16 px layout is excluded for good.

Lower-level calls, for a chip-shaped device that is not in the table:
    chip_body(size=32, kind='logic', marks='tlbo', pins='lbr', ramp=None,
              mark_style=None, mark_at=None) -> (Canvas, geom)
    chip(face, size=32, kind='logic', marks='tlbo', pins='lbr', pal=None, at=None, ramp=None,
         scale=None, mark_style=None, mark_at=None)  -> Canvas
       face = bitmap rows, or a short text string (rendered with FONT3; tokens like '{GE}')
    mark_pixels(size, marks, mark_style=None, mark_at=None) -> [(x, y, colour)]
"""
from .canvas import Canvas
from .oblique import box
from .palette import (CHIP, ACHIP, STEEL, MARK_RED, MARK_BLUE, MARK_GREEN, WHITE, GLYPH, GLYPH_A,
                      GLYPH_DIM, GLYPH_A_DIM)
from .font import FONT3, _tokens, glyph_rows

#: Geometry per canvas size.  Boxes are (x, y, w, h, d); rects are (x, y, w, h).
#: ``face`` is the free interior of the face (inside outline and rim) where a face may go.
CHIP_GEOM = {
    16: dict(
        body=(2, 4, 11, 11, 2),
        pins={'l': (0, 8, 3, 3, 1), 'b': (6, 14, 3, 2, 1), 'r': (13, 7, 3, 3, 1)},
        rim_top=(3, 4, 10, 1), rim_left=(3, 5, 1, 9), rim_w=1,
        marks={'t': (7, 4, 2, 1, MARK_BLUE), 'l': (3, 9, 1, 2, MARK_RED),
               'b': (7, 13, 2, 1, MARK_GREEN), 'o': (10, 5, 2, 2, WHITE)},
        face=(5, 5, 8, 8), centre=(9.0, 9.0), scale=1,
    ),
    32: dict(
        body=(4, 8, 22, 22, 4),
        pins={'l': (0, 16, 6, 6, 2), 'b': (12, 28, 6, 4, 2), 'r': (26, 14, 6, 6, 2)},
        rim_top=(6, 8, 20, 2), rim_left=(6, 10, 2, 18), rim_w=2,
        marks={'t': (14, 8, 4, 2, MARK_BLUE), 'l': (6, 18, 2, 4, MARK_RED),
               'b': (14, 26, 4, 2, MARK_GREEN), 'o': (21, 10, 4, 4, WHITE)},
        # free face: cols 8..25 (inner edge of the left rim .. last column before the side
        # face), rows 10..26 (under the top rim .. above the bottom outline).  ``centre`` is the
        # point placement aims for; it is the centre of the revision-2 face (9, 10, 17, 16), so
        # faces that fitted before keep their exact position.
        face=(8, 10, 18, 17), centre=(17.5, 18.0), scale=2,
    ),
}

#: Alternative mark shapes (32 px only): name -> (mark letter, full-res bitmap, default top-left).
#: Strokes are 2 px, like the bars.  'caret' = the clock caret under D flip-flop / S&H faces,
#: 'gt' = the clock '>' of the JK flip-flop input.
MARK_SHAPES = {
    'caret': ('b', ['..##..', '..##..', '##..##', '##..##'], (13, 24)),
    'gt': ('l', ['##..', '##..', '..##', '..##', '##..', '##..'], (6, 17)),
}

KINDS = {
    'logic': dict(ramp=CHIP, glyph=GLYPH, dim=GLYPH_DIM),
    'analog': dict(ramp=ACHIP, glyph=GLYPH_A, dim=GLYPH_A_DIM),
}

# ---------------------------------------------------------------------------- the face table
_BAR4, _BAR5 = '####', '#####'
_ONE = ['.#..', '##..', '.#..', '.#..', '###.']
#: buffer triangle as on analogchips.png: shallow (1:2) flanks, 7 x 7 half-res
_TRI = ['##.....', '#.##...', '#...##.', '#.....#', '#...##.', '#.##...', '##.....']
_AND = ['.##..', '.##..', '#..##', '#..#.', '.##.#']        # orthogonal joins only
_GE1 = ['#...#', '.#.##', '#...#', '....#', '###.#']           # '>=1' as on the texture
_EQ1 = ['...##', '###.#', '....#', '###.#', '....#']           # '=1'


def _full(*letters):
    """Join full-res letter bitmaps (lists of equal height) with 1 px gaps (scale=1 faces)."""
    return ['.'.join(rows) for rows in zip(*letters)]


def _x2(rows):
    """A half-res letter -> full-res rows (each cell 2x2): 2 px strokes in a scale=1 face."""
    return [''.join(c * 2 for c in r) for r in rows for _ in (0, 1)]


_V = _x2(['#.#', '#.#', '#.#', '.#.'])
_C = _x2(['##', '#.', '#.', '##'])
_A = _x2(['###', '#.#', '###', '#.#'])
_P5 = _x2(['###', '#.#', '###', '#..', '#..'])
_I5 = _x2(['#', '#', '#', '#', '#'])
_D5 = _x2(['##.', '#.#', '#.#', '#.#', '##.'])
_L4 = _x2(['#.', '#.', '#.', '##'])
_P4 = _x2(['###', '#.#', '###', '#..'])
_F4 = _x2(['##', '#.', '##', '#.'])
#: lowpass: the roll-off curve over LPF, as on the texture (full-res, 16 x 15)
_LPF = (['########........'] * 2 + ['........##......'] * 2 + ['..........##....'] * 2
        + ['.' * 16] + _full(_L4, _P4, _F4))

#: item id -> dict(kind, marks, face[, at, scale, mark_style, mark_at]).  ``at`` = explicit
#: top-left (canvas px) when the automatic placement is not the best-looking one.  Frozen:
#: artists do not edit this table; request a change instead.
CHIP_FACES = {
    # ---- logic gates (logicgates.png) -------------------------------------------------------
    'and_chip':   dict(kind='logic', marks='tlbo', face=_AND),
    'nand_chip':  dict(kind='logic', marks='tlbo', face=[_BAR5, '.....'] + _AND),
    'or_chip':    dict(kind='logic', marks='tlbo', face=_GE1),
    'nor_chip':   dict(kind='logic', marks='tlbo', face=[_BAR5, '.....'] + _GE1),
    'xor_chip':   dict(kind='logic', marks='tlbo', face=_EQ1),
    'xnor_chip':  dict(kind='logic', marks='tlbo', face=[_BAR5, '.....'] + _EQ1),
    'not_chip':   dict(kind='logic', marks='tlbo', face=[_BAR4, '....'] + _ONE),
    'pal_chip':   dict(kind='logic', marks='tlbo',                # P, A (second ink), L
                       face=['###aaa#.', '#.#a.a#.', '###aaa#.', '#..a.a#.', '#..a.a##']),
    'schmitt_trigger_chip': dict(kind='logic', marks='lo',
                       face=['.####', '.#.#.', '.#.#.', '.#.#.', '####.']),
    'd_flip_flop_chip': dict(kind='logic', marks='lbo', mark_style={'b': 'caret'},
                       face=['rr.', 'r.r', 'r.r', 'r.r', 'rr.']),
    'jk_flip_flop_chip': dict(kind='logic', marks='lo', mark_style={'l': 'gt'},
                       face=['..b.g.g', '..b.g.g', '..b.gg.', 'b.b.g.g', '.b..g.g']),
    'oscillator_chip': dict(kind='logic', marks='lo',
                       face=['..###.##', '..#.#.#.', '..#.#.#.', '..#.#.#.', '###.###.']),
    # ---- analog chips (analogchips.png) ------------------------------------------------------
    'opamp':      dict(kind='analog', marks='lbo', face=['###.###', '#.#.#.#', '#.#.###', '#.#.#..',
                                                         '###.#..']),
    'amplifier':  dict(kind='analog', marks='lo', face=_TRI),
    'voltage_controlled_amplifier': dict(kind='analog', marks='lbo', scale=1, at=(8, 15),
                       mark_at={'l': (6, 10)}, face=_full(_V, _C, _A)),   # input mark high-left
    'pid_regulator': dict(kind='analog', marks='lbo', scale=1, face=_full(_P5, _I5, _D5)),
    'voltage_controlled_sawtooth_oscillator': dict(kind='analog', marks='lo',
                       face=['...#...#', '..##..##', '.#.#.#.#', '#..##..#']),
    'voltage_controlled_sine_oscillator': dict(kind='analog', marks='lo',
                       face=['.##.....', '#..#...#', '....#..#', '.....##.']),
    'configurable_summing_unit': dict(kind='analog', marks='tlbo',
                       face=['####', '#...', '.#..', '#...', '####']),
    'sample_and_hold': dict(kind='analog', marks='lbo', mark_style={'b': 'caret'},
                       face=_TRI[:3] + ['#.a...#'] + _TRI[4:]),       # buffer + hold tick
    'lowpass_filter': dict(kind='analog', marks='lo', scale=1, at=(9, 11), face=_LPF),
}


def chip_face(size=32):
    """The geometry dict for a chip of this size (see CHIP_GEOM)."""
    return CHIP_GEOM[size]


def mark_pixels(size=32, marks='tlbo', mark_style=None, mark_at=None):
    """[(x, y, colour)] of the marks a chip draws (bars, or MARK_SHAPES; see module doc)."""
    g = CHIP_GEOM[size]
    style, moved = mark_style or {}, mark_at or {}
    out = []
    for m in marks:
        x, y, w, h, c = g['marks'][m]
        shape = style.get(m)
        if shape is not None:
            if size != 32:
                raise ValueError('mark shapes exist at 32 px only')
            letter, rows, (x, y) = MARK_SHAPES[shape]
            if letter != m:
                raise ValueError(f'mark shape {shape!r} is for mark {letter!r}, not {m!r}')
            if m in moved:
                x, y = moved[m]
            out += [(x + i, y + j, c) for j, r in enumerate(rows) for i, ch in enumerate(r) if ch == '#']
        else:
            if m in moved:
                x, y = moved[m]
            out += [(xx, yy, c) for yy in range(y, y + h) for xx in range(x, x + w)]
    return out


def chip_body(size=32, kind='logic', marks='tlbo', pins='lbr', ramp=None, mark_style=None,
              mark_at=None):
    """Draw the package without a face; returns (Canvas, geometry)."""
    g = CHIP_GEOM[size]
    R = ramp or KINDS[kind]['ramp']
    cv = Canvas(size)
    for p in 'lb':                                    # pins behind the body
        if p in pins:
            box(cv, *g['pins'][p], STEEL)
    box(cv, *g['body'], R)
    x, y, w, h = g['rim_top']
    for i in range(g['rim_w']):                       # lit rim: top rows, then left columns
        cv.hline(x, x + w - 1, y + i, R[5] if i == 0 else R[4])
    x, y, w, h = g['rim_left']
    for i in range(g['rim_w']):
        cv.vline(x + i, y, y + h - 1, R[4])
    if 'r' in pins:                                   # output pin leaves the side face
        box(cv, *g['pins']['r'], STEEL)
    for (x, y, c) in mark_pixels(size, marks, mark_style, mark_at):
        cv.set(x, y, c)
    return cv, g


def text_face(s):
    """A text string -> half-res bitmap rows using FONT3 (1 empty column between glyphs)."""
    toks = _tokens(s)
    rows = None
    for t in toks:
        g = glyph_rows(t, FONT3)
        if rows is None:
            rows = list(g)
        else:
            rows = [a + '.' + b for a, b in zip(rows, g)]
    return rows or []


def _inked(face, scale, x, y):
    out = []
    for j, row in enumerate(face):
        for i, ch in enumerate(row):
            if ch not in '. ':
                for dy in range(scale):
                    for dx in range(scale):
                        out.append((x + i * scale + dx, y + j * scale + dy, ch))
    return out


def _forbidden(size, marks, mark_style=None, mark_at=None):
    """Pixels a face may not touch: every mark/output-square pixel and its 4-neighbours."""
    bad = set()
    for (xx, yy, _) in mark_pixels(size, marks, mark_style, mark_at):
        for (a, b) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            bad.add((xx + a, yy + b))
    return bad


def place_face(face, size=32, marks='tlbo', scale=None, mark_style=None, mark_at=None):
    """Top-left (x, y) for a bitmap ``face`` on a chip of ``size``, or None if it cannot be
    placed inside the free face without touching a mark (see module doc).  ``scale`` None =
    the size's half-res scale (2 at 32 px); 1 = a full-resolution bitmap."""
    g = CHIP_GEOM[size]
    sc = g['scale'] if scale is None else scale
    fx, fy, fw, fh = g['face']
    W, H = len(face[0]) * sc, len(face) * sc
    bad = _forbidden(size, marks, mark_style, mark_at)
    cx, cy = g['centre']
    best = None
    for y in range(fy, fy + fh - H + 1):
        for x in range(fx, fx + fw - W + 1):
            if any((px, py) in bad for (px, py, _) in _inked(face, sc, x, y)):
                continue
            d = abs(x + W / 2.0 - cx) + abs(y + H / 2.0 - cy) + (0.01 if x + W / 2.0 > cx else 0)
            if best is None or d < best[0]:
                best = (d, x, y)
    return None if best is None else best[1:]


def chip(face, size=32, kind='logic', marks='tlbo', pins='lbr', pal=None, at=None, ramp=None,
         scale=None, mark_style=None, mark_at=None):
    """A chip with an arbitrary face.  ``face``: bitmap rows (half-res, or full-res with
    ``scale=1``), or a text string (FONT3, e.g. 'JK', '{GE}1').  ``mark_style`` /
    ``mark_at``: see the module doc.  Raises ValueError if it cannot be placed cleanly."""
    if isinstance(face, str):
        face = text_face(face)
    widths = {len(r) for r in face}
    if len(widths) != 1:
        raise ValueError('face rows must all have the same width')
    cv, g = chip_body(size, kind, marks, pins, ramp, mark_style, mark_at)
    sc = g['scale'] if scale is None else scale
    pos = at or place_face(face, size, marks, sc, mark_style, mark_at)
    if pos is None:
        raise ValueError(f'face {face!r} does not fit a {size} px chip without touching a mark'
                         + (' - chips are vanilla_size=32' if size == 16 else ''))
    P = {'#': KINDS[kind]['glyph'], 'a': KINDS[kind]['dim'], 'r': MARK_RED, 'g': MARK_GREEN,
         'b': MARK_BLUE, 'W': WHITE}
    P.update(pal or {})
    for (x, y, ch) in _inked(face, sc, *pos):
        cv.set(x, y, P[ch])
    return cv


def chip_icon(item_id, size=32):
    """The production icon of a chip in ``CHIP_FACES`` (32 px; 16 raises - see module doc)."""
    if size != 32:
        raise ValueError(f'{item_id}: chips are vanilla_size=32 (kit.decisions.VANILLA_32)')
    f = CHIP_FACES[item_id]
    return chip(f['face'], 32, f['kind'], f['marks'], at=f.get('at'), scale=f.get('scale'),
                mark_style=f.get('mark_style'), mark_at=f.get('mark_at'))


def face_ok(face, at, size=32, marks='tlbo', scale=None, mark_style=None, mark_at=None):
    """True if ``face`` drawn at ``at`` stays inside the free face and clear of every mark."""
    g = CHIP_GEOM[size]
    sc = g['scale'] if scale is None else scale
    fx, fy, fw, fh = g['face']
    bad = _forbidden(size, marks, mark_style, mark_at)
    return all(fx <= x < fx + fw and fy <= y < fy + fh and (x, y) not in bad
               for (x, y, _) in _inked(face, sc, *at))


def check_faces():
    """[(item_id, problem)] for every table face that fails to place (used by ``python3 -m kit``)."""
    bad = []
    for k, f in CHIP_FACES.items():
        if len({len(r) for r in f['face']}) != 1:
            bad.append((k, 'ragged rows'))
        elif f.get('at') is not None and not face_ok(f['face'], f['at'], 32, f['marks'],
                                                     f.get('scale'), f.get('mark_style'),
                                                     f.get('mark_at')):
            bad.append((k, f"explicit at={f['at']} leaves the face or touches a mark"))
        elif f.get('at') is None and place_face(f['face'], 32, f['marks'], f.get('scale'),
                                                f.get('mark_style'), f.get('mark_at')) is None:
            bad.append((k, 'cannot be placed without touching a mark'))
    return bad
