"""Glyph: the 32x32 drawing surface of the schematic set, plus the rule checker.

Coordinates are integer pixels, (0, 0) top-left. Every stroke is 2px: a call that takes a single
coordinate for a stroke (hline's y, vline's x) draws that pixel AND the next one (y, y+1).

Key constants
    N     = 32          canvas size
    CORE  = (4, 27)     every stroke pixel lies in cols/rows 4..27 (the 24x24 optical core box);
                        the halo lands on 3/28; rows/cols 0-2 and 29-31 stay empty.
    MID   = 15          the centred 2px stroke occupies rows (or cols) 15 and 16: centre line 15.5.
    S     = 31          mirror sum: mirror_x / mirror_y map x -> S - x, i.e. about the 15.5 centre line.

    MIN_HUE_PX = 40     a tiered/thermal glyph shows its hue on at least this many pixels
    MIN_GAP = 3         empty px required between two separate parts (Chebyshev); see check()

Workflow
    g = Glyph(tier='lv')          # tier from the manifest (None for untiered); g.hue is its colour
    g.vline(...); g.line(...); symbols.resistor(g, c=g.hue) ...
    return g                      # build.py calls g.check() and g.render()

Arguments are validated as you draw: coordinates must be ints (a float raises TypeError naming the
call), colours must be RGBA tuples from the palette constants (INK, g.hue, THERMAL...), never '#hex'.
"""
import math
import numbers
from contextlib import contextmanager
from PIL import Image

from .palette import HALO, INK, THERMAL, TIER, STROKE_COLOURS, hue_for, tier_key, hexof
from .font import FONT, MICRO, LOWER_DROP, text_width

N = 32
CORE = (4, 27)
MID = 15
S = 31
MIN_HUE_PX = 40          # a tier/thermal hue must cover at least this many pixels (a 2x20 bar)
MIN_GAP = 3              # empty px between separate parts: 2 are halo, >= 1 stays clear

_TIER_COLOURS = frozenset(TIER.values())


def _need_int(v, what):
    if isinstance(v, bool) or not isinstance(v, numbers.Integral):
        raise TypeError(f'{what} must be an int pixel coordinate, got {v!r}'
                        + (' (use // or round())' if isinstance(v, float) else ''))
    return int(v)


def _need_colour(c):
    if isinstance(c, str):
        raise TypeError(f'colour must be a palette constant (INK, g.hue, THERMAL, tier_color(...)), not {c!r}')
    if not (isinstance(c, tuple) and len(c) == 4 and all(isinstance(v, numbers.Integral) for v in c)):
        raise TypeError(f'colour must be an RGBA 4-tuple palette constant (INK, g.hue, THERMAL...), got {c!r}')
    return c


class RuleBreak(Exception):
    """Raised by Glyph.check(). build.py catches it, reports it and keeps going."""

    def __init__(self, msg, warnings=()):
        super().__init__(msg)
        self.warnings = list(warnings)


class Glyph:
    """A sparse pixel canvas. Draw with INK / g.hue / THERMAL only; the halo is added on render().

    tier: the item's tier as the manifest spells it ('lv', 'signal', 50, 'thermal', None...).
          It sets g.hue and lets check() verify the hue rules."""

    def __init__(self, tier=None, size=N):
        self.size = size
        self.px = {}
        self.tier = tier
        self.hue = hue_for(tier)
        self.grp = {}            # (x, y) -> group id: pixels of one multi-part mark (text, dashes...)
        self._gstack = []
        self._gnext = 0
        self.clipped = 0         # pixels drawn off the canvas (check() reports them)
        self.micro_used = ''     # MICRO text drawn (check() warns)

    # ---- raw pixels ---------------------------------------------------------------------
    def put(self, x, y, c=INK):
        x = _need_int(x, 'x')
        y = _need_int(y, 'y')
        _need_colour(c)
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[(x, y)] = c
            if self._gstack:
                self.grp[(x, y)] = self._gstack[-1]
        else:
            self.clipped += 1

    @contextmanager
    def group(self):
        """Pixels drawn inside `with g.group():` form ONE mark for the gap check, even when they are
        separate pieces (the letters of a label, the dashes of a dashed line, three heat waves,
        grains of grit). The group as a whole must still keep MIN_GAP from everything else.
        text(), micro(), dashed(), heat_waves(), grains() and the pictograms group themselves."""
        if self._gstack:                 # nested: join the outer group
            self._gstack.append(self._gstack[-1])
        else:
            self._gnext += 1
            self._gstack.append(self._gnext)
        try:
            yield
        finally:
            self._gstack.pop()

    def get(self, x, y):
        return self.px.get((x, y))

    def erase(self, x0, y0, x1=None, y1=None):
        """Remove pixels in the inclusive rectangle (a single pixel if x1/y1 omitted).
        Use it to cut a wire where another crosses without connecting (a 2px gap becomes halo)."""
        x1 = x0 if x1 is None else x1
        y1 = y0 if y1 is None else y1
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1):
                self.px.pop((x, y), None)
                self.grp.pop((x, y), None)

    def rect(self, x0, y0, x1, y1, c=INK):
        """FILLED inclusive rectangle. Only for 2px strokes and the small allowed marks
        (2x2 grains/teeth, 4x4 junction dots); large fills are banned."""
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1):
                self.put(x, y, c)

    # ---- strokes --------------------------------------------------------------------------
    def hline(self, x0, x1, y, c=INK):
        """Horizontal 2px stroke, cols x0..x1 inclusive, rows y and y+1."""
        self.rect(min(x0, x1), y, max(x0, x1), y + 1, c)

    def vline(self, x, y0, y1, c=INK):
        """Vertical 2px stroke, rows y0..y1 inclusive, cols x and x+1."""
        self.rect(x, min(y0, y1), x + 1, max(y0, y1), c)

    def frame(self, x0, y0, x1, y1, c=INK):
        """2px OUTLINED rectangle; (x0, y0)-(x1, y1) is the outer bound, inclusive.
        Keep the inner size >= 3 in both directions so a transparent pixel survives the halo."""
        self.hline(x0, x1, y0, c)
        self.hline(x0, x1, y1 - 1, c)
        self.vline(x0, y0, y1, c)
        self.vline(x1 - 1, y0, y1, c)

    def wire(self, pts, c=INK):
        """Orthogonal 2px polyline through pts [(x, y), ...]; each point is the top-left pixel of the
        2x2 stroke cross-section, so corners join cleanly. Diagonal segments fall back to line()."""
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            if y0 == y1:
                self.rect(min(x0, x1), y0, max(x0, x1) + 1, y0 + 1, c)
            elif x0 == x1:
                self.rect(x0, min(y0, y1), x0 + 1, max(y0, y1) + 1, c)
            else:
                self.line(x0, y0, x1, y1, c)

    def line(self, x0, y0, x1, y1, c=INK):
        """Any-angle 2px line: Bresenham thickened along the minor axis; near-45 degree lines get a
        third pixel per step so they look as heavy as orthogonal ones. 1:2 slopes come out as clean
        2px stair-steps. Endpoints are pixel coordinates (top-left of the stroke)."""
        dx, dy = abs(x1 - x0), abs(y1 - y0)
        sx, sy = (1 if x1 > x0 else -1), (1 if y1 > y0 else -1)
        err = dx - dy
        x, y = x0, y0
        steep = dy > dx
        diag = dx > 0 and dy > 0 and 0.4 < dy / dx < 2.5
        while True:
            self.put(x, y, c)
            self.put(x + 1, y, c) if steep else self.put(x, y + 1, c)
            if diag:
                self.put(x + 1, y + 1, c)
            if x == x1 and y == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x += sx
            if e2 < dx:
                err += dx
                y += sy

    def poly(self, pts, c=INK, closed=False):
        """line() through a list of points (optionally closing the loop)."""
        pts = list(pts) + ([pts[0]] if closed else [])
        for a, b in zip(pts, pts[1:]):
            self.line(*a, *b, c)

    def stroke(self, pts, c=INK, w=2.0):
        """Rasterise a smooth path (dense list of float points, pixel-corner coordinates, e.g. from
        bezier()) as every pixel whose CENTRE lies within w/2 of the path. Use for gate curves and
        other freeform shapes; draw one half and mirror for exact symmetry."""
        pts = list(pts)
        if not pts:
            return
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        r = w / 2
        for y in range(max(0, int(min(ys) - r) - 1), min(self.size, int(max(ys) + r) + 2)):
            for x in range(max(0, int(min(xs) - r) - 1), min(self.size, int(max(xs) + r) + 2)):
                px, py = x + 0.5, y + 0.5
                if any(_seg_dist(px, py, a, b) <= r + 1e-9 for a, b in zip(pts, pts[1:] or pts)):
                    self.put(x, y, c)

    def circ(self, x0, y0, D, c=INK, keep=None):
        """Clean 2px ring of EVEN outer diameter D; bounding box top-left (x0, y0), so the ring spans
        cols x0..x0+D-1 and rows y0..y0+D-1. keep(dx, dy) -> bool (offsets from the centre, pixel
        centres) selects an arc: e.g. keep=lambda dx, dy: dy < 0 draws the upper half."""
        cx, cy, r = x0 + D / 2, y0 + D / 2, D / 2 + 0.4
        for y in range(y0 - 1, y0 + D + 1):
            for x in range(x0 - 1, x0 + D + 1):
                ddx, ddy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(ddx, ddy)
                if r - 2.0 < d <= r and (keep is None or keep(ddx, ddy)):
                    self.put(x, y, c)

    def bitmap(self, x0, y0, rows, c=INK, colors=None):
        """Stamp a bitmap: rows of strings, 'X' = c; extra chars can map to colours via colors={'v': ...}.
        A bitmap is one mark: its pixels are grouped for the gap check."""
        if not self._gstack:
            with self.group():
                return self.bitmap(x0, y0, rows, c, colors)
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                col = c if ch == 'X' else (colors or {}).get(ch)
                if col is not None:
                    self.put(x0 + i, y0 + j, col)

    # ---- symmetry ---------------------------------------------------------------------------
    def mirror_x(self, s=S, only=None):
        """Copy every pixel to x' = s - x (default: about the vertical centre line 15.5).
        only(x, y) -> bool restricts which source pixels are copied. A mirrored copy of a grouped
        mark is a new, separate group (the gap check still applies between the two halves)."""
        remap = {}
        for (x, y), c in list(self.px.items()):
            if only is None or only(x, y):
                self.put(s - x, y, c)
                if (x, y) in self.grp and 0 <= s - x < self.size:
                    self.grp[(s - x, y)] = self._mirror_gid(remap, self.grp[(x, y)])

    def mirror_y(self, s=S, only=None):
        """Copy every pixel to y' = s - y (default: about the horizontal centre line 15.5)."""
        remap = {}
        for (x, y), c in list(self.px.items()):
            if only is None or only(x, y):
                self.put(x, s - y, c)
                if (x, y) in self.grp and 0 <= s - y < self.size:
                    self.grp[(x, s - y)] = self._mirror_gid(remap, self.grp[(x, y)])

    def _mirror_gid(self, remap, gid):
        if gid not in remap:
            self._gnext += 1
            remap[gid] = self._gnext
        return remap[gid]

    # ---- text ------------------------------------------------------------------------------
    def text(self, x0, y0, s, c=INK, gap=1, font=None):
        """Set string s with its top-left at (x0, y0). y0 is the cap top; lowercase in LOWER_DROP
        drops 2 rows automatically. FONT by default (2px), pass font=MICRO for the 1px micro font."""
        font = FONT if font is None else font
        text_width(s, font, gap)           # raises a clear KeyError for a missing glyph
        x = x0
        with self.group():
            for ch in s:
                bm = font[ch]
                drop = 2 if (font is FONT and ch in LOWER_DROP) else 0
                self.bitmap(x, y0 + drop, bm, c)
                x += len(bm[0]) + gap
        return x - gap - 1     # last column used

    def text_center(self, y0, s, c=INK, s2=S, gap=1, font=None):
        """Set s horizontally centred on the line x = s2/2 (default the canvas centre 15.5).
        Odd-width strings sit half a pixel left of centre."""
        w = text_width(s, FONT if font is None else font, gap)
        return self.text((s2 + 1 - w) // 2, y0, s, c, gap, font)

    def micro(self, x0, y0, s, c=INK, gap=1):
        """MICRO (3x5, 1px) label. Only for 3+ character labels that FONT cannot fit; check() warns."""
        self.micro_used += s
        return self.text(x0, y0, s, c, gap, MICRO)

    # ---- output ----------------------------------------------------------------------------
    def bbox(self):
        xs = [x for x, _ in self.px]
        ys = [y for _, y in self.px]
        return (min(xs), min(ys), max(xs), max(ys)) if xs else None

    def render(self):
        """RGBA image: strokes, then an opaque 8-connected 1px HALO on every empty neighbour."""
        im = Image.new('RGBA', (self.size, self.size), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), c)
        for y in range(self.size):
            for x in range(self.size):
                if (x, y) not in self.px and any((x + dx, y + dy) in self.px
                                                 for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                    im.putpixel((x, y), HALO)
        return im

    @classmethod
    def from_image(cls, im, tier=None):
        """Turn a raw RGBA image (what a draw() may return instead of a Glyph) back into a Glyph so it
        can be checked: HALO pixels are dropped, every other opaque pixel becomes a stroke pixel.
        Raises RuleBreak for a wrong size or partial alpha. check() then also verifies that the
        kit's own halo reproduces the image exactly when the image has a halo (a halo-free image gets
        the kit halo, as a Glyph does)."""
        im = im.convert('RGBA')
        if im.size != (N, N):
            raise RuleBreak(f'raw image is {im.size[0]}x{im.size[1]}; this set is {N}x{N} only')
        g = cls(tier=tier)
        bad_alpha = 0
        for y in range(N):
            for x in range(N):
                p = im.getpixel((x, y))
                if p[3] == 0:
                    continue
                if p[3] != 255:
                    bad_alpha += 1
                    continue
                if p != HALO:
                    g.px[(x, y)] = tuple(p)
        if bad_alpha:
            raise RuleBreak(f'raw image has {bad_alpha} partially transparent pixels (alpha must be 0 or 255)')
        g._source_image = im
        return g

    def components(self):
        """8-connected parts of the stroke mask, with pixels of one group() merged into one part.
        Returns a list of sets of (x, y)."""
        parent = {}

        def find(a):
            while parent[a] != a:
                parent[a] = parent[parent[a]]
                a = parent[a]
            return a

        def union(a, b):
            ra, rb = find(a), find(b)
            if ra != rb:
                parent[ra] = rb
        for p in self.px:
            parent[p] = p
        for (x, y) in self.px:
            for dx, dy in ((1, 0), (0, 1), (1, 1), (1, -1)):
                q = (x + dx, y + dy)
                if q in parent:
                    union((x, y), q)
        first = {}
        for p, gid in self.grp.items():
            if p in parent:
                if gid in first:
                    union(p, first[gid])
                else:
                    first[gid] = p
        parts = {}
        for p in self.px:
            parts.setdefault(find(p), set()).add(p)
        return sorted(parts.values(), key=lambda c: min(c))

    def _enclosed(self, inner, outer):
        """True when `inner` sits in a hole of `outer` (a letter inside a ring)."""
        seen, stack = set(), [(x, y) for x in range(self.size) for y in (0, self.size - 1)] + \
            [(x, y) for y in range(self.size) for x in (0, self.size - 1)]
        while stack:
            p = stack.pop()
            if p in seen or p in outer or not (0 <= p[0] < self.size and 0 <= p[1] < self.size):
                continue
            seen.add(p)
            if p in inner:
                return False
            x, y = p
            stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        return True

    def gaps(self):
        """[(gap, part_a_bbox, part_b_bbox, enclosed)] for every pair of parts closer than MIN_GAP
        empty px (Chebyshev: the empty pixels between their nearest pixels along a row, column or
        diagonal). gap 1: the halos overlap (the parts merge into one blob). gap 2: the halos touch,
        no clear pixel between them. gap >= MIN_GAP (3): at least one clear pixel."""
        parts = self.components()
        out = []
        for i in range(len(parts)):
            for j in range(i + 1, len(parts)):
                a, b = parts[i], parts[j]
                best = 99
                for (x, y) in a:
                    for (u, v) in b:
                        d = max(abs(x - u), abs(y - v)) - 1
                        if d < best:
                            best = d
                            if best <= 1:
                                break
                    if best <= 1:
                        break
                if best < MIN_GAP:
                    enc = self._enclosed(a, b) or self._enclosed(b, a)
                    out.append((best, _bb(a), _bb(b), enc))
        return out

    def check(self, name=''):
        """Enforce the measurable rules of STYLE.md.

        Raises RuleBreak listing every ERROR; returns a stats dict when there is none:
        {'bbox', 'colours', 'hue_px', 'warnings': [...]}. build.py prints the warnings; a reference
        or finished icon has none (or explains each one in its module docstring).

        ERROR  size != 32; empty; pixels off the canvas; strokes outside the core box 4..27;
               colours outside {INK, tier mains, THERMAL}; more than one tier hue; tier hue missing
               or < MIN_HUE_PX px; untiered glyph using a hue; THERMAL < MIN_HUE_PX px;
               two separate parts less than MIN_GAP (3) empty px apart, unless one lies inside a
               hole of the other (a label inside a ring needs only 1); a raw image whose dark
               outline is not exactly the kit's halo.
        WARN   a MICRO (1px) label is present."""
        errs, warns = [], []
        if self.size != N:
            errs.append(f'size {self.size} != {N}')
        if not self.px:
            raise RuleBreak(f'{name}: empty glyph')
        if self.clipped:
            errs.append(f'{self.clipped} pixels drawn off the canvas')
        bb = self.bbox()
        if bb[0] < CORE[0] or bb[1] < CORE[0] or bb[2] > CORE[1] or bb[3] > CORE[1]:
            errs.append(f'strokes span x{bb[0]}..{bb[2]} y{bb[1]}..{bb[3]}, outside core box {CORE[0]}..{CORE[1]}')
        cols = {}
        for c in self.px.values():
            cols[c] = cols.get(c, 0) + 1
        bad = [hexof(c) for c in cols if c not in STROKE_COLOURS]
        if bad:
            errs.append(f'colours not in palette (INK, tier main, THERMAL): {", ".join(bad)}')
        tiers = [c for c in cols if c in _TIER_COLOURS]
        if len(tiers) > 1:
            errs.append(f'{len(tiers)} tier hues in one icon (max 1): {", ".join(map(hexof, tiers))}')
        expect = self.hue
        if expect == INK:
            if tiers or THERMAL in cols:
                errs.append('untiered glyph (tier=None) uses a tier/thermal hue; pass Glyph(tier=...)')
        else:
            n = cols.get(expect, 0)
            if n < MIN_HUE_PX:
                errs.append(f'tier {self.tier!r}: hue {hexof(expect)} covers {n}px < {MIN_HUE_PX}px '
                            f'(colour the whole defining element, see STYLE section 6)')
            other = [c for c in tiers if c != expect]
            if other:
                errs.append(f'tier {self.tier!r} glyph uses another tier hue {", ".join(map(hexof, other))}')
        if THERMAL in cols and cols[THERMAL] < MIN_HUE_PX:
            errs.append(f'thermal grey covers {cols[THERMAL]}px < {MIN_HUE_PX}px')
        for gap, ba, bb2, enc in self.gaps():
            if enc:
                continue                 # a label inside a ring: >= 1 empty px is enough
            where = f'part at x{ba[0]}..{ba[2]} y{ba[1]}..{ba[3]} and part at x{bb2[0]}..{bb2[2]} y{bb2[1]}..{bb2[3]}'
            if gap <= 1:
                errs.append(f'gap {gap}px between {where}: halos merge (need >= {MIN_GAP}, or join them)')
            else:
                errs.append(f'gap {gap}px between {where}: halos touch (need >= {MIN_GAP})')
        if self.micro_used:
            warns.append(f'MICRO label {self.micro_used!r}: 1px text, never the only thing that tells two items apart')
        src = getattr(self, '_source_image', None)
        if src is not None and not errs and any(p == HALO for p in src.getdata()):
            if list(self.render().getdata()) != list(src.getdata()):
                errs.append('raw image: its dark outline is not the kit halo (return the Glyph instead)')
        if errs:
            raise RuleBreak(f'{name}: ' + '; '.join(errs), warns)
        return {'bbox': bb, 'colours': {hexof(c): n for c, n in cols.items()}, 'hue_px': cols.get(expect, 0),
                'warnings': warns}


def _bb(pts):
    xs = [x for x, _ in pts]
    ys = [y for _, y in pts]
    return (min(xs), min(ys), max(xs), max(ys))


def _seg_dist(px, py, a, b):
    ax, ay = a
    bx, by = b
    vx, vy = bx - ax, by - ay
    L = vx * vx + vy * vy
    t = 0.0 if L == 0 else max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / L))
    return math.hypot(px - (ax + t * vx), py - (ay + t * vy))


def bezier(*ctrl, n=48):
    """Points along a quadratic (3 control points) or cubic (4) Bezier, for Glyph.stroke()."""
    out = []
    for i in range(n + 1):
        t = i / n
        pts = list(ctrl)
        while len(pts) > 1:
            pts = [((1 - t) * a[0] + t * b[0], (1 - t) * a[1] + t * b[1]) for a, b in zip(pts, pts[1:])]
        out.append(pts[0])
    return out


def arc_points(cx, cy, r, a0, a1, n=48):
    """Points on a circular arc (angles in degrees, 0 = +x, 90 = DOWN since y grows downward)."""
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]
