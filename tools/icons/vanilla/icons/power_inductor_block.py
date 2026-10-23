"""power_inductor_block - hidden full-block inductor (invisible in world, no model).  Drawn as
the power inductor's drum in a full-block steel cabinet.  See families/power_passive.py."""
from families.power_passive import inductor_block


def draw():
    return inductor_block()
