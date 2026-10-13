"""monster_filter - entity-sensor filter for monsters: red-tinted glass lens (descriptor colour 1,0,0) in a steel ring (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.lens(F.RED)
