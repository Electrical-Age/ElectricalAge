"""analogic_regulator - proportional regulator upgrade: the family control module, tall screen with a smooth rising curve (families/parts_modules)."""
from families import parts_modules as F


def draw():
    return F.regulator('analog')
