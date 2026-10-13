"""Worked examples of the kit primitives for families that have no reference icon yet.

These are NOT item icons (make_examples.py renders them to examples/png/prim_*.png and
examples/primitives.png).  Copy the patterns, not the exact coordinates.
"""
from kit import *                                   # noqa: F401,F403


def heating_element(tier='lv', mat=VCOPPER):
    """Material = element ramp (cylinder), tier = the terminal stubs (tier_cue 'stub')."""
    cv = Canvas(16)
    tier_stub(cv, 0, 6, 4, tier, 'right')
    tier_stub(cv, 15, 6, 4, tier, 'left')
    cylinder(cv, 3, 4, 10, 8, mat)                   # diameter 8 -> CYL_BANDS[8]
    for x in (5, 7, 9, 11):                          # coil grooves: one darker column each
        cv.vline(x, 6, 9, mat[2])
    return cv


def fuse(tier='lv'):
    """Cartridge: glass body, tier-coloured end caps drawn as their own cylinders (tier_cue 'band')."""
    cv = Canvas(16)
    cylinder(cv, 4, 5, 8, 6, GLASS)
    line(cv, (5, 8), (10, 8), LEAD[2])               # the lead fuse wire seen through the glass
    cylinder(cv, 1, 4, 4, 8, tier_ramp(tier))
    cylinder(cv, 11, 4, 4, 8, tier_ramp(tier))
    return cv


def battery():
    """Standing cylinder with a steel top cap (portable batteries, cells)."""
    cv = Canvas(16)
    cylinder(cv, 4, 1, 14, 8, RUBBER_BLACK, axis='y', cap=STEEL)
    cv.rect(5, 8, 6, 2, GOLD[3])                     # label band (a part colour, not a tier)
    return cv


def gauge():
    """Front-on round face: disc() on a steel housing, needle with line()."""
    cv = Canvas(16)
    box(cv, 1, 3, 12, 12, 2, STEEL)
    disc(cv, 3, 5, 8, GLASS)
    line(cv, (7, 9), (9, 6), LEAD_RED[1])
    return cv


def fan():
    """ring() shroud + disc() hub + line() blades."""
    cv = Canvas(16)
    ring(cv, 1, 1, 14, STEEL)
    for a, b in (((7, 7), (4, 4)), ((8, 8), (11, 11)), ((8, 7), (11, 4)), ((7, 8), (4, 11))):
        line(cv, a, b, STEEL[3])
    disc(cv, 6, 6, 4, CAV, rim=False)
    return cv


def bulb(tier='lv'):
    """Lamps are drawn LIT: LIT glass disc, tier on the base collar (tier_cue 'band')."""
    cv = Canvas(16)
    disc(cv, 3, 1, 10, LIT, face=4)
    cv.set(5, 4, LIT[5]); cv.set(6, 3, LIT[5])       # glint
    cylinder(cv, 5, 10, 6, 6, tier_ramp(tier), axis='y')
    return cv


def signal_relay(tier='signal'):
    """tier_plate on a steel device: the signal plate uses the shifted face mapping."""
    cv = Canvas(16)
    tier_plate(cv, 1, 11, 12, 4, 3, tier)
    box(cv, 3, 5, 8, 7, 2, STEEL)
    return cv


def solar_cell():
    """SOLAR panel on a steel frame (kept clear of the MV blue)."""
    cv = Canvas(16)
    box(cv, 1, 6, 12, 8, 2, SOLAR)
    for x in (4, 7, 10):
        cv.vline(x, 7, 12, SOLAR[4])
    cv.hline(2, 11, 9, SOLAR[4])
    return cv


def utility_pole():
    """POLE_WOOD post and cross-arm (the production poles use one solid(): families/cables.pole)."""
    cv = Canvas(16)
    box(cv, 6, 3, 3, 12, 1, POLE_WOOD)
    box(cv, 1, 4, 13, 2, 1, POLE_WOOD)
    for x in (2, 12):
        cv.rect(x, 2, 1, 2, TIER['hv'][4])
    return cv


def knurled_cap(tier='mv'):
    """knurled_disc(): an end-on D-fuse cap with the tier disc in the window (the lead fuses)."""
    cv = Canvas(16)
    knurled_disc(cv, 1, 1, 14, STEEL, teeth=8)
    disc(cv, 4, 4, 8, tier_ramp(tier))
    return cv


def toroid_32():
    """oblique_ring(): a copper toroid standing proud of a dark block's top face (32 px)."""
    cv = Canvas(32)
    box(cv, 2, 12, 20, 18, 8, CARBON)
    oblique_ring(cv, 2, 12, 20, 8, COPPER, height=2, floor=CAV[0])
    return cv


EXAMPLES = {
    'prim_heating_element_lv': lambda: heating_element('lv'),
    'prim_heating_element_vhv': lambda: heating_element('vhv', TUNGSTEN),
    'prim_fuse_signal': lambda: fuse('signal'),
    'prim_fuse_mv': lambda: fuse('mv'),
    'prim_battery': battery,
    'prim_gauge': gauge,
    'prim_fan': fan,
    'prim_bulb_lv': lambda: bulb('lv'),
    'prim_relay_signal': lambda: signal_relay('signal'),
    'prim_relay_steel_for_comparison': lambda: (lambda cv: (box(cv, 1, 11, 12, 4, 3, STEEL),
                                                            box(cv, 3, 5, 8, 7, 2, STEEL), cv)[2])(Canvas(16)),
    'prim_solar_cell': solar_cell,
    'prim_utility_pole': utility_pole,
    'prim_plate_iron': lambda: plate(IRON),
    'prim_ingot_lead': lambda: ingot(LEAD),
    'prim_dust_vcopper': lambda: dust(VCOPPER),
    'prim_knurled_cap_mv': knurled_cap,
    'prim_toroid_32': toroid_32,
}
