"""200v_oil_pump - OilPump oilpump200v, 230V MV body cue (families/oil_chain.py)."""
from families.oil_chain import oil_pump


def draw():
    return oil_pump('mv')
