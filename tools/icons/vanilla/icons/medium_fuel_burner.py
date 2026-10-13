"""medium_fuel_burner - fuel burner for the fuel heat furnace; size axis: the shared burner drawing
fitted into MEDIUM_BOX (STYLE 7b: small/medium/big = SMALL_BOX / MEDIUM_BOX / BODY_BOX).

Drawing code lives in families/parts_mechanical.py.
"""
from kit import MEDIUM_BOX
from families.parts_mechanical import burner


def draw():
    return burner(MEDIUM_BOX)
