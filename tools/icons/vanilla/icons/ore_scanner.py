"""ore_scanner - handheld-tool family (families/tools.py, 'others').

The in-game texture's expansion card, front-on: green PCB (families.tools.PCB, a new material
ramp frozen in the family helper) with a big and a small black chip and two traces, gold edge
fingers along the bottom, and the steel mounting bracket on the left edge with its top tab.
"""
from families.tools import ore_scanner


def draw():
    return ore_scanner()
