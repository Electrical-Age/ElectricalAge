"""and_chip - logic chip (sixnode, model logicgates), vanilla_size 32 (chips are 32, decided centrally).

The whole icon is the kit's chip family: package + the frozen '&' face from kit.chip.CHIP_FACES
(2 px strokes, placed clear of the marks).  Every other chip id is the same one-liner.
"""
from kit import chip_icon


def draw():
    return chip_icon('and_chip')
