"""advanced_electrical_motor - electric motor seen end-on (barrel receding up-right, ribs, feet),
quality 2 (GOLD 1 px end-bell rim).

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import motor


def draw():
    return motor(2)
