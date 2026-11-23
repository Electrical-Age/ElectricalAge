"""Steam turbines family (vanilla, 16 px): the CAD-era 48V/230V Steam Turbines, the 48V Compact Steam Turbine and the
230V Air Blower (src/cadmodels SteamTurbine, CompactTurbine, AirBlower).

    steam_turbine_set(tier) -> Canvas     steamturbine50v / steamturbine200v, tier 'lv' / 'mv'
    compact_turbine()       -> Canvas     compactturbine50v (LV)
    air_blower()            -> Canvas     airblower200v (MV)

Shared visual system
--------------------
* Materials as the models paint them: STEEL (steel: generator stator, flanges, intake ring), CAST (castiron: the
  scroll casing, riser, firebox, saddles; the shaft-machine cast iron, = shaft/draw_devices.CAST and
  steam_plant.IRON_D, also standing in for the darksteel skids, fins and bearings to keep 16 colours), the tier
  ramp on the parts the model paints (paint_green / paint_blue), one COPPER column or coupling as the only accent.
* The two Steam Turbines are seen side-on from their z+ flank (shaft along the picture, generator end left):
  a steel generator with dark cooling fins and a broad tier band, the copper coupling, the dark cast-iron scroll
  casing (taller than the generator) with its riser up to the steel top flange, all on the dark skid. This is
  what tells them from the plain 50V/200V heat turbines (whole stepped housing in the tier colour): here the
  tier is only the generator band, the riser collar and the terminal stripe, the rest is iron and steel.
  The 230V twin is the bigger machine (generator 9 px, casing 11 px, three fins against two).
* The Compact Steam Turbine is seen from its front end (x = 0), in cabinet oblique: two masses on one skid, the
  cast-iron firebox carrying the steel drum boiler (end-on, receding) on the left and the finned air-cooled
  condenser box on the right (a single column of its copper fins on the receding side, kept small since COPPER is close to HV; its rain-capped
  vent stack on top), the green-banded generator end between them. Squat and boxy where the Steam Turbines are
  a long open shaft line.
* The Air Blower is seen from its intake flank (z = 0): the blue volute drum side-on with the big steel-ringed
  intake grille standing out of it towards us (dark louvres, two bars), the finned steel motor on its saddle
  to the left, on the skid. The Inline Pump's icon shows the same drum end-on with the pipe through; here the
  grille is the one large feature.

Family ramps: CAST (above), reported earlier by the shaft pass; nothing new.
"""
from kit import Canvas, box, cylinder, disc, outline_region, tier_ramp, h, STEEL, COPPER
from families.machines import sub

#: cast iron (castiron) of the shaft machines; [5] = [4] so the cylinders' highlight adds no colour.  It also
#: stands in for the models' darksteel (skids, fins, bearings): a separate dark ramp broke the 16-colour budget.
CAST = (h('#242228'), h('#353236'), h('#47433f'), h('#5c5752'), h('#79736b'), h('#79736b'))
#: STEEL with a [5] (= [4]) for cylinder()'s highlight row.
STEEL6 = tuple(STEEL[:5]) + (STEEL[4],)


def _band_col(cv, x, y, d, B):
    """One column of a painted band round a side-on cylinder (rows y..y+d-1): outline [1]/[0], one lit row [4]
    only on the big (9 px) stator, the rest ramp[3] then ramp[2], so the tier shows as a real area of [3]."""
    n = d - 2
    lit = 1 if d >= 9 else 0
    shade = 2 if d >= 9 else 1
    rows = [B[1]] + [B[4]] * lit + [B[3]] * (n - lit - shade) + [B[2]] * shade + [B[0]]
    for j, c in enumerate(rows):
        cv.set(x, y + j, c)


def _skid(cv, x, w, y=13, d=2):
    sub(cv, lambda c: box(c, x, y, w, 2, d, CAST, bevel=False))


# ------------------------------------------------------------------------------- steam turbine
#: per tier: generator (y, diameter, fin columns, band columns), casing (x, y, diameter, length)
_ST = {
    'lv': dict(gy=5, gd=7, fins=(1, 6), band=(3, 4), tx=8, ty=4, td=9, tl=5),
    'mv': dict(gy=4, gd=9, fins=(1, 4, 6), band=(2, 3), tx=8, ty=2, td=11, tl=5),
}


def steam_turbine_set(tier):
    B = tier_ramp(tier)
    p = _ST[tier]
    cv = Canvas(16)
    _skid(cv, 1, 13)
    gy, gd, tx, ty, td, tl = p['gy'], p['gd'], p['tx'], p['ty'], p['td'], p['tl']
    cv.hline(2, 4, 13, B[3])                              # the front terminal plate's tier stripe, on the skid
    # rear bearing behind the casing (dark stub)
    sub(cv, lambda c: cylinder(c, tx + tl - 1, ty + td // 2 - 1, 3, 3, CAST, round_ends=False))
    # scroll casing (cast iron): a squat side-on drum, the tallest mass, on the right
    sub(cv, lambda c: cylinder(c, tx, ty, tl, td, CAST))
    # riser (cast iron, 3 wide) from the casing's top up to the steel top flange
    rx = tx + 1
    top = 2 if tier == 'lv' else 1
    sub(cv, lambda c: cylinder(c, rx, top, 3, ty - top + 1, CAST, axis='y', round_ends=False))
    sub(cv, lambda c: box(c, rx - 1, top - 1, 5, 1, 1, STEEL, bevel=False))
    for x in range(rx, rx + 3):                           # tier collar where the riser leaves the casing
        cv.set(x, ty, B[3] if x < rx + 2 else B[2])
        cv.set(x, ty + 1, B[2] if x < rx + 2 else B[1])
    # generator (steel), left: stator with dark fins and the tier band
    sub(cv, lambda c: cylinder(c, 1, gy, 6, gd, STEEL6))
    for x in p['fins']:
        for j in range(1, gd - 1):
            cv.set(x, gy + j, CAST[3] if j < gd // 2 else CAST[2])
    for x in p['band']:
        _band_col(cv, x, gy, gd, B)
    # coupling between them on the shaft line: the copper disc
    cy = gy + gd // 2
    for dy in (-1, 0, 1):
        cv.set(7, cy + dy, COPPER[3])
    return cv


# ------------------------------------------------------------------------------- compact turbine
def _capsule(cv, x, y, d, recede, R):
    """A drum lying along the depth axis: round front face d at (x, y), its body receding up-right."""
    from kit.shapes import _disc_mask
    reg = set()
    for k in range(recede + 1):
        reg |= {(px + k, py - k) for (px, py) in _disc_mask(x, y, d)}
    front = _disc_mask(x, y, d)
    for (px, py) in reg - front:
        cv.set(px, py, R[4] if py - y < d // 2 - 1 else R[2])
    for q in front:
        cv.set(*q, R[3])
    outline_region(cv, reg, R[0], R[1])
    return reg


def compact_turbine():
    B = tier_ramp('lv')
    cv = Canvas(16)
    _skid(cv, 1, 11, d=3)
    # right mass: the condenser box, copper fins on its receding side (one column, dark gaps either side: kept
    # small, COPPER is close to HV), the vent stack with its rain cap on top
    sub(cv, lambda c: box(c, 8, 6, 4, 7, 3, CAST, bevel=False))
    for y in range(4, 11):
        cv.set(13, y, COPPER[3])
    sub(cv, lambda c: box(c, 12, 2, 1, 2, 1, CAST, bevel=False))
    cv.hline(11, 14, 1, CAST[2])
    # left mass: cast-iron firebox, the steel drum boiler (end-on, receding) on top
    sub(cv, lambda c: box(c, 1, 9, 5, 4, 2, CAST, bevel=False))
    sub(cv, lambda c: _capsule(c, 1, 4, 5, 2, STEEL))
    # the generator end in front, between the masses: the green band round the steel end bell, the hub
    sub(cv, lambda c: disc(c, 4, 5, 8, B, rim=False))
    sub(cv, lambda c: disc(c, 6, 7, 4, STEEL, rim=False))
    cv.set(7, 8, STEEL[4])
    return cv


# ------------------------------------------------------------------------------- air blower
def air_blower():
    B = tier_ramp('mv')
    cv = Canvas(16)
    _skid(cv, 1, 13)
    # motor (steel, finned) on its cast saddle, left
    sub(cv, lambda c: box(c, 2, 10, 3, 3, 1, CAST, bevel=False))
    sub(cv, lambda c: cylinder(c, 1, 5, 5, 6, STEEL6))
    for x in (2, 4):
        for j in range(1, 5):
            cv.set(x, 5 + j, CAST[3] if j < 3 else CAST[2])
    # volute drum (MV paint), side-on, right
    sub(cv, lambda c: cylinder(c, 5, 1, 10, 12, B))
    # the intake standing out of the drum towards us (down-left): steel ring, dark louvres, two bars
    from kit.shapes import _disc_mask
    sub(cv, lambda c: disc(c, 4, 5, 8, STEEL))
    hole = _disc_mask(6, 7, 4)
    for (x, y) in hole:
        cv.set(x, y, STEEL[3] if y == 8 else (STEEL[2] if y == 10 else CAST[0]))
    return cv
