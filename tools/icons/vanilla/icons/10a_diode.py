"""10a_diode - sixnode: steel bracket holding the black diode can between its walls (white cathode
band by the right wall; the bracket is 1 px wider than the reference so rating + band fit).
The 10 A and 25 A diodes share one model; the rating is printed on the can (FONT3, orchestrator
decision).  See families/power_passive.py."""
from families.power_passive import diode


def draw():
    return diode("10")
