"""cheap_ferromagnetic_core - ferromagnetic core, quality 0 (plain FERRITE frame, no trim).

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import core


def draw():
    return core(0)
