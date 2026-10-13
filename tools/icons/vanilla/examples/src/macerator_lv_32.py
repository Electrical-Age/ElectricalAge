"""50v_macerator at 32 px - reference for how the style scales (not an item id).

Same silhouette, ramps and light as the 16 px icon at 2x: outline 2 px, depth 8, chamfers 2
steps; the extra density is spent only on 1 px interior detail (roller teeth, inner jamb, sill
seen through the window, seams).  Compare with out/50v_macerator.png in tools/contact.py.
"""
from families.macerator import macerator


def draw():
    return macerator('lv', 32)
