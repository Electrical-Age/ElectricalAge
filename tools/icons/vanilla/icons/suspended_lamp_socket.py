"""suspended_lamp_socket - robustlampsuspended.obj, chain of 3: ceiling plate, short chain, dark mount, caged lit glass.

Drawn by families/lighting_fixtures (see its docstring for the family system)."""
from families.lighting_fixtures import suspended


def draw():
    return suspended("short")
