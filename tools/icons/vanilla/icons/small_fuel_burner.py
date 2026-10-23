"""small_fuel_burner - fuel burner for the fuel heat furnace; size axis: the shared burner drawing
fitted into SMALL_BOX (STYLE 7b: small/medium/big = SMALL_BOX / MEDIUM_BOX / BODY_BOX).

Drawing code lives in families/parts_mechanical.py.
"""
from kit import SMALL_BOX
from families.parts_mechanical import burner


def draw():
    return burner(SMALL_BOX)
