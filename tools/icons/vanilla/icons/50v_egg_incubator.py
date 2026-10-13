"""50v_egg_incubator - open cage with golden tray and zig-zag heater (eggincubator.obj); tier cue = LV slab; see families/machines.py."""
from families.machines import egg_incubator


def draw():
    return egg_incubator('lv')
