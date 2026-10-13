"""Voltage-tier helpers: how a tier is shown on an icon.

Owner decision: tiers are told apart by COLOUR (never by thickness alone), and items that used
to share one texture across tiers (switches, relays, fuses, antennas, 50V/200V lamps and
chargers, ...) each get their own colour-coded icon.

Every tiered item has ONE primary cue, chosen centrally per item (``kit.decisions.tier_cue``,
later the manifest's ``tier_cue`` column), never by the artist:
    'body'  the machine body itself is the tier ramp (its texture is tier-coloured in the world)
    'plate' the mounting base / backplate is a ``tier_plate``
    'stub'  the terminals / cable stubs leaving the device (``tier_stub``)
    'band'  a band or end caps on a cartridge-like body (``tier_band``)
    'jacket' cables: the jacket (``cable``)
A ``tier_pip`` may be added as a SUPPLEMENT (never the only cue; it vanishes at GUI 2).
Everything else on the device stays in neutral materials so the tier colour is the only hue.
"""
from .oblique import box
from .palette import TIER, TIERS, THERMAL, STEEL

#: Every spelling we accept -> canonical key.  'grid'/'highgrid' are VoltageLevelColor's pole
#: tiers: textures/voltages/grid.png is the HV colour, highgrid.png the VHV colour (striped).
#: 'none'/'neutral' mean "untiered" and normalise to None.
_ALIASES = {
    'signal': 'signal', 'sig': 'signal', 'signalvoltage': 'signal', 'signal_voltage': 'signal',
    'lv': 'lv', 'low': 'lv', 'low_voltage': 'lv', 'lowvoltage': 'lv', '50v': 'lv', 'low_power': 'lv',
    'mv': 'mv', 'medium': 'mv', 'medium_voltage': 'mv', 'mediumvoltage': 'mv', '200v': 'mv',
    'medium_power': 'mv',
    'hv': 'hv', 'high': 'hv', 'high_voltage': 'hv', 'highvoltage': 'hv', '800v': 'hv', 'high_power': 'hv',
    'grid': 'hv',
    'vhv': 'vhv', 'very_high': 'vhv', 'very_high_voltage': 'vhv', 'veryhighvoltage': 'vhv',
    'veryhigh': 'vhv', '3200v': 'vhv', '3.2kv': 'vhv', 'highgrid': 'vhv', 'high_grid': 'vhv',
    'thermal': 'thermal',
    'none': None, 'neutral': None, '': None, 'untiered': None,
}

#: Face mapping of ``tier_plate`` per tier, as ramp indices (top, front, side, bevel).
#: Revision 3: signal uses the normal mapping for top, front and side and only drops the bevel
#: (its ramp[5] is near-white and made the top read as steel).  The revision-2 shift (front =
#: ramp[2]) put the signal front only dE 9.8 (deutan) from the LV front (ramp[3]); with the
#: front on ramp[3] the pair is 17.7.  The front is the cue (STYLE 5.1), and the plate-face check
#: (``kit.cvd.plate_pairs``, ``python3 -m kit``) holds every tier pair's FRONTS >= 15 apart.
#: Signal's top (ramp[4] = the contract colour) stays close to STEEL[4]: accepted, the front carries it.
PLATE_FACES = {'signal': (4, 3, 2, 4)}
_DEFAULT_FACES = (4, 3, 2, 5)
#: Which plate_ramp index each face of a plate uses (for checks): front, top, side.
PLATE_FACE_INDEX = {'front': 3, 'top': 4, 'side': 2}


def normalize_tier(t):
    """Map any spelling ('50V', 'low_voltage', 'medium_power', 'VeryHigh', 'grid', ...) to
    'signal' | 'lv' | 'mv' | 'hv' | 'vhv' | 'thermal', or None for untiered ('none', 'neutral').

    Antennas: low/medium/high power = LV/MV/HV (VoltageLevelColor.fromVoltage of their nominal U).
    """
    if t is None:
        return None
    k = str(t).strip().lower().replace(' ', '_').replace('-', '_')
    if k not in _ALIASES:
        raise KeyError(f'unknown tier {t!r}; use one of {TIERS + ("thermal",)} (or none/neutral)')
    return _ALIASES[k]


def tier_ramp(t):
    """The 6-step ramp of a tier (accepts any alias); 'thermal' gives THERMAL.

    Raises ValueError for an untiered item: draw it in STEEL / CABLE instead."""
    k = normalize_tier(t)
    if k is None:
        raise ValueError(f'tier {t!r} is untiered: draw it in neutral materials (STEEL, CABLE)')
    return THERMAL if k == 'thermal' else TIER[k]


def swap_ramp(cv, src, dst):
    """Recolour every pixel of ramp ``src`` to the same index of ramp ``dst`` (tier twins).

    Safe only between TIER ramps (and THERMAL): they share no step with any other ramp
    (``python3 -m kit`` checks this).  Material ramps may share steps with each other."""
    cv.recolor({a: b for a, b in zip(src, dst)})
    return cv


def tier_variants(draw_fn, tiers=TIERS):
    """``{tier: draw_fn(tier)}`` - convenience for checking a family's tiers side by side."""
    return {t: draw_fn(t) for t in tiers}


def plate_ramp(tier):
    """The tier ramp re-indexed for a plate face mapping (see PLATE_FACES): a 6-tuple whose
    [4]/[3]/[2]/[5] are the top/front/side/bevel colours a tier plate uses.  Use it when you
    build a tier-coloured base with ``solid()`` instead of ``tier_plate``."""
    r = tier_ramp(tier)
    k = normalize_tier(tier)
    top, front, side, bev = PLATE_FACES.get(k, _DEFAULT_FACES)
    return (r[0], r[1], r[side], r[front], r[top], r[bev] if bev < len(r) else r[top])


def tier_backplate(cv, pixels, tier):
    """The tier backplate SEEN THROUGH an open device (relay cages, open frames): fill
    ``pixels`` (an iterable of (x, y), or a rect (x, y, w, h)) with the plate front colour
    ``plate_ramp(tier)[3]``.  Keep parts in a material that collides with the tier (COPPER on
    HV, CINNABAR on VHV, GLASS/IRON on signal...; STYLE 5.3) off this field: put them on a
    STEEL or CAV strip instead.  Returns the pixel set."""
    if isinstance(pixels, tuple) and len(pixels) == 4 and all(isinstance(v, int) for v in pixels):
        x, y, w, h = pixels
        pixels = [(xx, yy) for yy in range(y, y + h) for xx in range(x, x + w)]
    c = plate_ramp(tier)[3]
    out = set()
    for p in pixels:
        cv.set(p[0], p[1], c)
        out.add(tuple(p))
    return out


def tier_plate(cv, x, y, w, h, d, tier, **kw):
    """A mounting base / backplate as an oblique box in the tier colour (see ``oblique.box``).

    Uses ``plate_ramp(tier)`` so the signal plate stays distinct from steel."""
    return box(cv, x, y, w, h, d, plate_ramp(tier), **kw)


def tier_band(cv, x, y, w, h, tier, vertical=False):
    """A painted band on an already-outlined body (fuse cap, label stripe, collar).

    Horizontal band (default): top row ramp[4], middle ramp[3], bottom row ramp[2].
    ``vertical=True``: left column ramp[4], middle ramp[3], right column ramp[2]
    (the band wraps a horizontal cylinder lit from the top-left).
    The band is painted exactly in the rectangle given: keep it INSIDE the body's outline
    (one pixel in from the silhouette, ``cv.u`` at 32 px) so the body keeps its outline;
    for end caps that form the silhouette, draw the cap as its own ``box``/``cylinder`` in
    ``tier_ramp(t)`` instead, which outlines it.
    """
    r = tier_ramp(tier)
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if vertical:
                c = r[4] if xx == x else (r[2] if xx == x + w - 1 and w > 2 else r[3])
            else:
                c = r[4] if yy == y else (r[2] if yy == y + h - 1 and h > 2 else r[3])
            cv.set(xx, yy, c)


def tier_stub(cv, x, y, length, tier, direction='right', width=None, tip=STEEL):
    """A short straight cable stub in the tier colour, starting at (x, y) (its top/left edge).

    ``direction`` 'right' | 'left' | 'down' | 'up'.  ``width`` (default 4 px, 8 at 32) is the
    stub's full thickness including its vanilla outline (ramp[1] on the lit side, ramp[0] on
    the shaded side); inside: a lit row/column ramp[4], the rest ramp[3].
    ``tip``: the bare conductor on the last ``cv.u`` px - tinned STEEL by default (copper ends
    sat next to the HV colour); None runs the jacket to the end.
    The stub's far end is open (it leaves the device), so it gets no end outline.
    """
    r = tier_ramp(tier)
    u = cv.u
    width = width if width is not None else 4 * u
    horiz = direction in ('right', 'left')
    pts = {}
    for i in range(length):
        for j in range(width):
            if j < u:
                c = r[1]
            elif j >= width - u:
                c = r[0]
            elif j < 2 * u:
                c = r[4]
            else:
                c = r[3]
            if tip is not None and i >= length - u and u <= j < width - u:
                c = tip[4] if j < 2 * u else tip[2]
            if horiz:
                xx = x + i if direction == 'right' else x - i
                pts[(xx, y + j)] = c
            else:
                yy = y + i if direction == 'down' else y - i
                pts[(x + j, yy)] = c
    for (px, py), c in pts.items():
        cv.set(px, py, c)
    return set(pts)


def tier_pip(cv, x, y, tier, size=None):
    """A small square indicator in the tier colour (2x2 at 16 px, 4x4 at 32 px).

    Top-left pixel(s) ramp[5] (glint), body ramp[4], bottom/right edge ramp[2] at 32.
    A SUPPLEMENT only, on a dark or neutral surface - never the only tier cue.
    """
    r = tier_ramp(tier)
    s = size if size is not None else 2 * cv.u
    for j in range(s):
        for i in range(s):
            c = r[4]
            if s > 2 and (i == s - 1 or j == s - 1):
                c = r[2]
            if i < max(1, s // 4) and j < max(1, s // 4):
                c = r[5] if len(r) > 5 else r[4]
            cv.set(x + i, y + j, c)
