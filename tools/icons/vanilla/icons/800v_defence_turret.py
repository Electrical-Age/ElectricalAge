"""800v_defence_turret - white head, gun barrels, black joint (turret.obj); tier cue = HV slab (the holder); see families/machines.py."""
from families.machines import turret


def draw():
    return turret('hv')
