"""long_suspended_lamp_socket - robustlampsuspended.obj, chain of 7: the same drawing as suspended_lamp_socket with a longer chain and a shorter cage (same width, one vertical bar).

Drawn by families/lighting_fixtures (see its docstring for the family system)."""
from families.lighting_fixtures import suspended


def draw():
    return suspended("long")
