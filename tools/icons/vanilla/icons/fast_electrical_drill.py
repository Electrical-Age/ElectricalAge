"""fast_electrical_drill - auto-miner drill bit pointing down-left, quality 2 (GOLD 2 px band at the
shank/flute joint).

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import drill


def draw():
    return drill(2)
