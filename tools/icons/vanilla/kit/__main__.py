"""python3 -m kit [--cvd]   (run from vanilla/)

Default: every shared ramp with hex, CIE L* and HSV hue; L*-floor violations; tier ramps that
share a step with another ramp (would break swap_ramp); tier pairs under the CVD rule on ramp[2]
and ramp[3] AND on the plate fronts plate_ramp actually paints; chip faces that cannot be placed.
--cvd: tier separation under protan/deutan/tritan and each material's nearest tier colour.
"""
import sys

from kit.color import ramp_report, lstar, L_FLOOR
from kit.palette import ramp_names, TIER, MATERIALS
from kit import palette
from kit.chip import check_faces

names = ramp_names()
problems = 0
from kit.tier import plate_ramp
PLATES = {t: plate_ramp(t) for t in TIER}
if '--cvd' in sys.argv:
    from kit.cvd import report
    print(report(dict(TIER), {m: getattr(palette, m) for m in MATERIALS}, plate_ramps=PLATES))
    sys.exit(0)
for name, r in names.items():
    print(ramp_report(name, r))
    if lstar(r[0]) < L_FLOOR - 0.05:
        print(f'   ^ {name}[0] is below the L* floor {L_FLOOR}')
        problems += 1
owners = {}
for name, r in names.items():
    for c in r:
        owners.setdefault(c, set()).add(name)
for t in list(TIER) + ['THERMAL']:
    key = 'TIER_' + t if t in TIER else t
    for c in names[key]:
        others = owners[c] - {key}
        if others:
            print(f'   ! {key} shares {c} with {sorted(others)} (swap_ramp would recolour both)')
            problems += 1
from kit.cvd import tier_pairs, TIER_MIN_DE
for d, mode, step, a, b in tier_pairs(dict(TIER), steps=(2, 3)):
    if d < TIER_MIN_DE:
        print(f'   ! tiers {a}/{b} only dE {d:.1f} apart on ramp[{step}] for {mode} vision (rule >= {TIER_MIN_DE})')
        problems += 1
from kit.cvd import plate_pairs
for d, mode, face, a, b in plate_pairs(PLATES, {'front': 3}):
    if d < TIER_MIN_DE:
        print(f'   ! plate fronts {a}/{b} only dE {d:.1f} apart for {mode} vision (rule >= {TIER_MIN_DE})')
        problems += 1
for k, why in check_faces():
    print(f'   ! chip face {k}: {why}')
    problems += 1
print('kit: %s' % ('ok' if not problems else f'{problems} problem(s)'))
sys.exit(1 if problems else 0)
