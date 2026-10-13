"""casing - transformer casing (the in-world transformatorCase): light steel box, vent slots low on
the front as on the model's louvre face, louvres on the side; label band and latch left out.

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import casing


def draw():
    return casing()
