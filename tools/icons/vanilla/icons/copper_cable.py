"""copper_cable - loose loop of bare wire (families.materials.wire: kit strip(), one open loop with a free end crossing over it, no tinned ends; told apart from the tier cables by silhouette) in the frozen VCOPPER ramp.  See families/materials.py."""
from families.materials import icon


def draw():
    return icon('copper_cable')
