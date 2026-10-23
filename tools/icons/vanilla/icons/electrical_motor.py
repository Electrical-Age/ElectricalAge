"""electrical_motor - electric motor seen end-on (barrel receding up-right, ribs, feet), quality 0
(the 1 px end-bell rim stays steel).

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import motor


def draw():
    return motor(0)
