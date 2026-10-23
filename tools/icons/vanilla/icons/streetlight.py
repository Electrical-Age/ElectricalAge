"""streetlight - streetlight.obj stood upright: dark base, thin pole, frustum lantern with its lit lens.

Drawn by families/lighting_fixtures (see its docstring for the family system)."""
from families.lighting_fixtures import streetlight


def draw():
    return streetlight()
