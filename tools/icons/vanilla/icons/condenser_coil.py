"""condenser_coil - Condenser Coil (cadmodels CondenserCoil, 1x1x2, untiered): the tall drum on legs wound with its copper cooling coil, a heat plate on its flank, the vapour flange on top. Drawing in families/steam_plant.py."""
from families.steam_plant import condenser_coil


def draw():
    return condenser_coil()
