"""50v_oil_pump - OilPump oilpump50v, 48V LV body cue (families/oil_chain.py)."""
from families.oil_chain import oil_pump


def draw():
    return oil_pump('lv')
