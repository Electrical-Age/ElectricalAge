"""universal_meter - handheld-tool family (families/tools.py, 'meters').

Multimeter plus thermometer: the multimeter body, face and crossed red/black probes, and the
thermometer's yellow probe leaving the body's top-right corner (the in-game texture's three
leads: red, yellow, black).
"""
from kit import LEAD_RED, LEAD_BLK
from families.tools import meter, probe, LEAD_YEL


def draw():
    cv = meter()
    probe(cv, 14, 8, 4, LEAD_BLK, direction=(-1, 1))
    probe(cv, 9, 8, 4, LEAD_RED)
    probe(cv, 11, 4, 2, LEAD_YEL, tip_n=2, direction=(1, -1))   # up-right from the corner
    return cv
