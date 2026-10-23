"""thermometer - handheld-tool family (families/tools.py, 'meters').

The multimeter body and face (RUBBER_Y handheld template, LCD, jacks); its one accessory is
the yellow temperature probe (the in-game texture's yellow lead) running from the right jack
down-right to a long steel sensing needle.  The lead leaves the body through one dark
LEAD_BLK boot step, so the yellow lead does not merge into the yellow body.
"""
from kit import LEAD_BLK
from families.tools import meter, probe, LEAD_YEL


def draw():
    cv = meter()
    probe(cv, 9, 8, 1, LEAD_BLK, tip_n=0)        # (9..10, 8) dark boot at the jack
    probe(cv, 10, 9, 2, LEAD_YEL, tip_n=3)       # (10..11, 9..10) lead, (12..14, 11..13) needle
    return cv
