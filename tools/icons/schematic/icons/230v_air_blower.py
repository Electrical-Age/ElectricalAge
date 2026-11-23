"""230v_air_blower: motor driving a fan: IEC 'M' ring (RING_M) in the mv hue + kit 'wind' mark for the air it blows (STYLE 7.3 motor + kit mark)."""
from kit import Glyph
from families.steam_turbines import blower

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    blower(g)
    return g
