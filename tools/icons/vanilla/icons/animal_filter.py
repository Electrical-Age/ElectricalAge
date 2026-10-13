"""animal_filter - entity-sensor filter for animals: blue-tinted glass lens (descriptor colour .3,.3,1) in a steel ring (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lens(F.BLUE_GLASS)
