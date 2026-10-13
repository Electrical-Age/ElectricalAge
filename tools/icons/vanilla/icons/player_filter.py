"""player_filter - entity-sensor filter for players: green-tinted glass lens (descriptor colour 0,1,0) in a steel ring (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lens(F.GREEN_GLASS)
