"""average_ferromagnetic_core - ferromagnetic core, quality 1 (VCOPPER rim round the window).

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import core


def draw():
    return core(1)
