"""combustion_chamber - firebox upgrade for the heat furnaces.

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import combustion_chamber


def draw():
    return combustion_chamber()
