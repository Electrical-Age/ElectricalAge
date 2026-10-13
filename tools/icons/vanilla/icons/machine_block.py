"""machine_block - machine casing block, quality 0 (steel rim round the grille).

Drawing code lives in families/parts_mechanical.py (shared visual system in its docstring).
"""
from families.parts_mechanical import machine_block


def draw():
    return machine_block(0)
