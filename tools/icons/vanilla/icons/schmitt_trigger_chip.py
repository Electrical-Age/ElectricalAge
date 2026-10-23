"""schmitt_trigger_chip - chip (sixnode), vanilla_size 32 (chips are 32, decided centrally).

Package + the frozen face from kit.chip.CHIP_FACES; see icons/and_chip.py.
"""
from kit import chip_icon


def draw():
    return chip_icon('schmitt_trigger_chip')
