"""Palette for the schematic glyph set.

ONE colour axis: hue = voltage tier. Nothing else in this set carries a hue.

* HALO  - the 1px dark outline every glyph gets (added by Glyph.render(); never draw it yourself).
* INK   - warm white. Leads, letters, arrows, grit, and the WHOLE glyph of every untiered item.
* TIER  - five ramps (signal, LV, MV, HV, VHV). `main` is EXACTLY the mod's in-world voltage colour,
          textures/voltages/{signal,low,medium,high,veryhigh}.png, the palette players already know
          (owner decision 2026-10-12; not retuned for colour blindness: Minecraft 26.3 has no
          colour-blind mode). `light` is the contract hex from VoltageLevelColor.setGLColor(); `dark`
          is main with its L* lowered by the same step as before (15-19) at main's hue. Icons use only
          `main`; `dark`/`light` exist for legends, docs and the cross-set reference, and Glyph.check()
          rejects them in icons.
* THERMAL - neutral grey ramp from textures/voltages/thermal.png (#424242 = `dark`). `main` (#767676)
          is light enough to read on the dark row; on the slot grey it reads as a dark, halo-outlined
          conduit, which matches the dark in-world thermal cable. Owner-approved, on the condition
          that every thermal glyph carries the INK heat-wave cue (heat_waves()).

Lightness order (CIE L*): INK 93 > signal 79 > HV 75 > LV 66 > MV 58 = slot grey 58 > VHV 50 = thermal 50.

Colour-vision deficiency: run `python3 -m kit.palette --cvd` (CIEDE2000 distances between every pair
of stroke colours under simulated protan/deutan/tritan vision). Weakest pairs: INK-signal (protan
8.2, deutan 9.5), LV-MV (tritan 9.3), LV-HV (protan 11.6), LV-VHV (deutan 12.5). STYLE.md section
5.4 has the structural rules that back these pairs up.

Every constant here is immutable (tuples, frozenset, MappingProxyType): an icon module cannot change
what later icons are checked against.
"""
import os
import sys
from types import MappingProxyType

_TOOLS = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), 'tools')
if _TOOLS not in sys.path:
    sys.path.insert(0, _TOOLS)
from tiers import normalize_tier, CANONICAL, VOLTAGE_TIERS   # noqa: E402,F401  (shared with the vanilla kit)


def _rgba(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def hexof(c):
    """'#rrggbb' for an RGBA tuple."""
    return '#%02x%02x%02x' % tuple(c[:3])


HALO = _rgba('#18161e')
INK = _rgba('#eeeade')

# Canonical tier keys, low to high.  ramp = (dark, main, light); light == contract hex.
TIER_RAMPS = MappingProxyType({
    'signal': tuple(map(_rgba, ('#7c9b89', '#adcbb9', '#ccded1'))),
    'lv':     tuple(map(_rgba, ('#177f4e', '#36b776', '#8cd6ad'))),
    'mv':     tuple(map(_rgba, ('#086991', '#3a93c3', '#8cbdd9'))),
    'hv':     tuple(map(_rgba, ('#be7e10', '#f5ab3a', '#f5cc8f'))),
    'vhv':    tuple(map(_rgba, ('#903328', '#cb5040', '#db948c'))),
})
THERMAL_RAMP = tuple(map(_rgba, ('#424242', '#767676', '#a8a8a8')))

TIERS = VOLTAGE_TIERS                                              # ('signal', 'lv', 'mv', 'hv', 'vhv')
TIER = MappingProxyType({k: v[1] for k, v in TIER_RAMPS.items()})  # main shade per tier: what icons draw with
THERMAL = THERMAL_RAMP[1]


def tier_key(tier):
    """Normalise a tier spelling to one of TIERS ('signal', 'lv', 'mv', 'hv', 'vhv').

    Accepts the canonical keys and the shared synonyms of tools/tiers.py ('low_voltage', '50V',
    'medium_power', 'VeryHighVoltage', 3200 ...). Raises KeyError for anything else, for bare
    numbers 1..5, for None and for 'thermal' (not a voltage tier: use THERMAL / hue_for)."""
    k = normalize_tier(tier)
    if k not in TIERS:
        raise KeyError(f'{tier!r} is not a voltage tier; use one of {TIERS}')
    return k


def tier_color(tier):
    """Main stroke colour for a voltage tier (any spelling accepted by tier_key)."""
    return TIER[tier_key(tier)]


def tier_ramp(tier):
    """(dark, main, light) for a tier. Only `main` may appear in an icon."""
    return TIER_RAMPS[tier_key(tier)]


def hue_for(tier):
    """Colour of an item's defining element: the tier hue, THERMAL for 'thermal', INK for None.

    This is the one function an artist needs to colour a glyph from the manifest's `tier` field.
    Raises KeyError for an unknown spelling (never silently INK)."""
    k = normalize_tier(tier)
    if k is None:
        return INK
    if k == 'thermal':
        return THERMAL
    return TIER[k]


STROKE_COLOURS = frozenset({INK, THERMAL, *TIER.values()})   # everything Glyph.check() accepts
NAMED = MappingProxyType({'INK': INK, 'THERMAL': THERMAL, 'HALO': HALO, **dict(TIER)})


# ---- colour-vision self-check -------------------------------------------------------------------
_CVD = {   # Machado et al. 2009, severity 1.0, applied in linear RGB
    'normal': ((1, 0, 0), (0, 1, 0), (0, 0, 1)),
    'protan': ((0.152286, 1.052583, -0.204868), (0.114503, 0.786281, 0.099216), (-0.003882, -0.048116, 1.051998)),
    'deutan': ((0.367322, 0.860646, -0.227968), (0.280085, 0.672501, 0.047413), (-0.011820, 0.042940, 0.968881)),
    'tritan': ((1.255528, -0.076749, -0.178779), (-0.078411, 0.930809, 0.147602), (0.004733, 0.691367, 0.303900)),
}


def _lin(v):
    return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4


def _delin(v):
    v = min(1.0, max(0.0, v))
    return 12.92 * v if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055


def lab(c, vision='normal'):
    """CIE L*a*b* (D65) of an RGB(A) colour as seen with the given simulated vision
    ('normal', 'protan', 'deutan', 'tritan')."""
    rgb = [_lin(x / 255) for x in c[:3]]
    m = _CVD[vision]
    rgb = [_lin(_delin(sum(m[i][j] * rgb[j] for j in range(3)))) for i in range(3)]
    X = 0.4124 * rgb[0] + 0.3576 * rgb[1] + 0.1805 * rgb[2]
    Y = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]
    Z = 0.0193 * rgb[0] + 0.1192 * rgb[1] + 0.9505 * rgb[2]

    def f(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(X / 0.95047), f(Y), f(Z / 1.08883)
    return (116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))


def de2000(l1, l2):
    """CIEDE2000 colour difference between two L*a*b* triples."""
    import math
    L1, a1, b1 = l1
    L2, a2, b2 = l2
    C1, C2 = math.hypot(a1, b1), math.hypot(a2, b2)
    Cb = (C1 + C2) / 2
    G = 0.5 * (1 - math.sqrt(Cb ** 7 / (Cb ** 7 + 25 ** 7)))
    a1p, a2p = (1 + G) * a1, (1 + G) * a2
    C1p, C2p = math.hypot(a1p, b1), math.hypot(a2p, b2)
    h1 = math.degrees(math.atan2(b1, a1p)) % 360
    h2 = math.degrees(math.atan2(b2, a2p)) % 360
    dL, dC = L2 - L1, C2p - C1p
    if C1p * C2p == 0:
        dh = 0
    elif abs(h2 - h1) <= 180:
        dh = h2 - h1
    else:
        dh = h2 - h1 - 360 if h2 > h1 else h2 - h1 + 360
    dH = 2 * math.sqrt(C1p * C2p) * math.sin(math.radians(dh / 2))
    Lb, Cbp = (L1 + L2) / 2, (C1p + C2p) / 2
    if C1p * C2p == 0:
        hb = h1 + h2
    elif abs(h1 - h2) <= 180:
        hb = (h1 + h2) / 2
    else:
        hb = (h1 + h2 + 360) / 2 if h1 + h2 < 360 else (h1 + h2 - 360) / 2
    T = (1 - 0.17 * math.cos(math.radians(hb - 30)) + 0.24 * math.cos(math.radians(2 * hb))
         + 0.32 * math.cos(math.radians(3 * hb + 6)) - 0.2 * math.cos(math.radians(4 * hb - 63)))
    dth = 30 * math.exp(-((hb - 275) / 25) ** 2)
    RC = 2 * math.sqrt(Cbp ** 7 / (Cbp ** 7 + 25 ** 7))
    SL = 1 + 0.015 * (Lb - 50) ** 2 / math.sqrt(20 + (Lb - 50) ** 2)
    SC, SH = 1 + 0.045 * Cbp, 1 + 0.015 * Cbp * T
    RT = -math.sin(math.radians(2 * dth)) * RC
    return math.sqrt((dL / SL) ** 2 + (dC / SC) ** 2 + (dH / SH) ** 2 + RT * (dC / SC) * (dH / SH))


def cvd_report(colours=None, worst=6):
    """Text table: per simulated vision, L* of each stroke colour and the `worst` closest pairs (dE2000).

    colours: {name: '#hex' or RGBA}; default = INK, the five tier mains and THERMAL. Use it to
    measure a proposed retune before asking the owner."""
    import itertools
    cols = colours or {'INK': INK, **dict(TIER), 'thermal': THERMAL}
    cols = {k: (_rgba(v) if isinstance(v, str) else v) for k, v in cols.items()}
    slot = _rgba('#8b8b8b')
    out = []
    for vision in _CVD:
        L = {k: lab(v, vision) for k, v in cols.items()}
        ls = lab(slot, vision)
        pairs = sorted((de2000(L[a], L[b]), a, b) for a, b in itertools.combinations(cols, 2))
        out.append(f'{vision:7s} L*: ' + ' '.join(f'{k}={L[k][0]:.0f}' for k in cols))
        out.append('        closest: ' + ', '.join(f'{a}-{b} {d:.1f}' for d, a, b in pairs[:worst]))
        out.append('        vs slot #8b8b8b: ' + ', '.join(f'{k} {de2000(L[k], ls):.1f}' for k in cols))
    return '\n'.join(out)


if __name__ == '__main__':
    if '--cvd' in sys.argv:
        extra = [a for a in sys.argv[1:] if '=' in a]          # e.g. signal=#8fb8a0 to test a retune
        cols = None
        if extra:
            cols = {'INK': INK, **dict(TIER), 'thermal': THERMAL}
            for a in extra:
                k, v = a.split('=', 1)
                cols[k] = _rgba(v)
        print(cvd_report(cols))
    else:
        for k in TIERS:
            print(f'{k:7s}', ' '.join(hexof(c) for c in TIER_RAMPS[k]))
        print('thermal', ' '.join(hexof(c) for c in THERMAL_RAMP))
        print('INK', hexof(INK), 'HALO', hexof(HALO))
        print('(python3 -m kit.palette --cvd [name=#hex ...] for the colour-vision table)')
