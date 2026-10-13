"""Colour-vision-deficiency checks for the tier ramps and the shared materials.

Simulation: Machado, Oliveira & Fernandes 2009, severity 1.0 (full dichromacy), applied in
linear sRGB.  Distance: CIE76 Delta E in CIE Lab (the same numbers ``python3 -m kit --cvd``
prints).  These are kit-maintenance tools; artists only need them to check a new family ramp
against the tiers (``nearest_tier``).

Rule the tier ramps are tuned to (STYLE.md section 5):
    on every face step a tier plate or cable jacket uses (ramp[2], ramp[3]), every pair of tiers
    is at least ``TIER_MIN_DE`` apart for normal vision and under protan, deutan and tritan.
    ramp[4] is the contract colour and cannot move, so the separation lives in ramp[0..3].
"""
import itertools

from .color import to_lab

#: Machado 2009 matrices, severity 1.0, linear RGB.
MACHADO = {
    'protan': ((0.152286, 1.052583, -0.204868), (0.114503, 0.786281, 0.099216),
               (-0.003882, -0.048116, 1.051998)),
    'deutan': ((0.367322, 0.860646, -0.227968), (0.280085, 0.672501, 0.047413),
               (-0.011820, 0.042940, 0.968881)),
    'tritan': ((1.255528, -0.076749, -0.178779), (-0.078411, 0.930809, 0.147602),
               (0.004733, 0.691367, 0.303900)),
}
MODES = ('normal', 'protan', 'deutan', 'tritan')

#: Minimum pairwise Delta E between tiers on ramp[2] and ramp[3] (see module doc).
TIER_MIN_DE = 15.0


def _lin(v):
    v /= 255.0
    return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4


def _gam(v):
    v = min(1.0, max(0.0, v))
    v = 12.92 * v if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055
    return int(round(v * 255))


def simulate(c, mode):
    """The colour ``c`` as seen with full protanopia / deuteranopia / tritanopia ('normal' = c)."""
    if mode == 'normal':
        return c
    m = MACHADO[mode]
    lin = [_lin(v) for v in c[:3]]
    out = [_gam(sum(m[i][j] * lin[j] for j in range(3))) for i in range(3)]
    return tuple(out) + (255,)


def delta_e(a, b, mode='normal'):
    """CIE76 Delta E between two colours, after simulating ``mode`` on both."""
    la, lb = to_lab(simulate(a, mode)), to_lab(simulate(b, mode))
    return sum((x - y) ** 2 for x, y in zip(la, lb)) ** 0.5


def tier_pairs(tier_ramps, steps=(2, 3, 4), modes=MODES):
    """[(dE, mode, step, tier_a, tier_b)] for every tier pair, step and mode, smallest first."""
    out = []
    for mode in modes:
        for i in steps:
            for a, b in itertools.combinations(tier_ramps, 2):
                out.append((delta_e(tier_ramps[a][i], tier_ramps[b][i], mode), mode, i, a, b))
    return sorted(out)


def plate_pairs(plate_ramps, faces=None, modes=MODES):
    """[(dE, mode, face, tier_a, tier_b)] comparing the colours a tier PLATE actually paints.

    ``plate_ramps`` = {tier: plate_ramp(tier)} (the re-indexed ramps), ``faces`` = {name: index}
    (default front 3, top 4, side 2).  Unlike ``tier_pairs`` this compares a shifted mapping
    (e.g. a signal front on a different ramp step) against the other tiers' real fronts."""
    faces = faces or {'front': 3, 'top': 4, 'side': 2}
    out = []
    for mode in modes:
        for name, i in faces.items():
            for a, b in itertools.combinations(plate_ramps, 2):
                out.append((delta_e(plate_ramps[a][i], plate_ramps[b][i], mode), mode, name, a, b))
    return sorted(out)


def nearest_tier(c, tier_ramps, steps=(2, 3, 4)):
    """(dE, mode, tier, step) of the tier colour closest to ``c`` under any mode.

    Use it on a new material: below ~15 the material will be read as that tier somewhere,
    so keep it off tiered devices or change it."""
    best = None
    for mode in MODES:
        for t, r in tier_ramps.items():
            for i in steps:
                d = delta_e(c, r[i], mode)
                if best is None or d < best[0]:
                    best = (d, mode, t, i)
    return best


def report(tier_ramps, materials=None, floor=TIER_MIN_DE, plate_ramps=None):
    """Text report: the worst tier pairs per mode/step, the plate faces as painted, then each
    material's nearest tier."""
    lines = ['tier pairs (CIE76 dE; rule: >= %.0f on ramp[2] and ramp[3]; ramp[4] is fixed)' % floor]
    pairs = tier_pairs(tier_ramps)
    for mode in MODES:
        for i in (2, 3, 4):
            sel = [p for p in pairs if p[1] == mode and p[2] == i][:3]
            bad = any(p[0] < floor for p in sel) and i != 4
            lines.append('  %-6s ramp[%d]  %s%s' % (
                mode, i, ', '.join('%s-%s %.1f' % (p[3], p[4], p[0]) for p in sel),
                '   <-- below rule' if bad else ''))
    if plate_ramps:
        lines.append('plate faces as plate_ramp paints them (rule: fronts >= %.0f; top/side for '
                     'information)' % floor)
        pp = plate_pairs(plate_ramps)
        for mode in MODES:
            for face in ('front', 'top', 'side'):
                sel = [p for p in pp if p[1] == mode and p[2] == face][:3]
                bad = face == 'front' and any(p[0] < floor for p in sel)
                lines.append('  %-6s %-5s  %s%s' % (
                    mode, face, ', '.join('%s-%s %.1f' % (p[3], p[4], p[0]) for p in sel),
                    '   <-- below rule' if bad else ''))
    if materials:
        lines.append('materials: nearest tier colour (steps 2-4 vs 2-4), normal vision and worst CVD.')
        lines.append('  A material under ~15 (normal) must not sit on a tiered device as a large area.')
        for name, r in materials.items():
            idx = [k for k in (2, 3, 4) if k < len(r)]
            nm = min((delta_e(r[k], R[j]), t, j, k) for k in idx for t, R in tier_ramps.items()
                     for j in (2, 3, 4))
            cv = min((delta_e(r[k], R[j], m), t, j, k, m) for k in idx for t, R in tier_ramps.items()
                     for j in (2, 3, 4) for m in MODES[1:])
            lines.append('  %-12s normal: [%d]~%s[%d] %5.1f%s   cvd: [%d]~%s[%d] %5.1f (%s)' % (
                name, nm[3], nm[1], nm[2], nm[0], ' <--' if nm[0] < 15 else '    ',
                cv[3], cv[1], cv[2], cv[0], cv[4]))
    return '\n'.join(lines)
