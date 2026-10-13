"""power_passive family helpers (power passives, fuses, diodes, rheostats, the DC-DC converter).

The family's shared visual system (STYLE.md sections 6, 8.4, 8.6 and 9):
- Two-terminal IEC passives threaded on the centre line (MID) with INK leads to cols 4 and 27.
- Untiered parts (every power passive except signal_diode and the four lead fuses) are all INK; the
  symbol's structure tells them apart.
- Tiered parts carry the tier hue on the defining element only (the fuse frame + element, the diode).
- Variants of one symbol differ by a kit mark in MARK_BOX (rating '10' / '25', block 'B'), with the
  symbol moved down onto MID_M; tier siblings differ by colour only.
"""
from kit import (MID, MID_M, mark, fuse, diode, resistor, capacitor, inductor, terminal,
                 adjust_arrow, nonlinear_slash)


def lead_fuse(g):
    """Lead fuses (one icon per tier): the IEC fuse, frame and element in the tier hue, INK leads."""
    fuse(g, g.hue)


def blown_fuse(g):
    """The burnt-out lead fuse: the same fuse, untiered (all INK), its element cut in the middle."""
    fuse(g, blown=True)


def fuse_holder(g):
    """The fuse holder (untiered device, STYLE 8.6): a shortened INK fuse (cols 11..20) between two open
    terminal rings (kit terminal(), D=6, cols 4..9 and 22..27) at the lead ends: the place a fuse goes.
    It differs from the lead fuses (tiered, plain leads) and from the blown fuse (cut element) in
    structure, not only in colour."""
    terminal(g, 6, MID)
    terminal(g, 24, MID)
    fuse(g, x0=11, x1=20, with_leads=False)
    g.hline(10, 10, MID)
    g.hline(21, 21, MID)


def rated_diode(g, rating):
    """Untiered power diodes: the current rating ('10' / '25') as the mark, INK diode on MID_M."""
    mark(g, rating)
    diode(g, y=MID_M, h=10)


def signal_diode(g):
    """The signal diode: the plain diode in the signal hue, no mark (so it differs from the untiered
    power diodes in structure as well as colour, STYLE 5.4)."""
    diode(g, g.hue)


def rheostat(g, large=False):
    """Rheostats: IEC resistor + diagonal 'adjustable' arrow, INK; the large one has the full-size body."""
    resistor(g, small=not large)
    adjust_arrow(g, 6, 25, 25, 6)


def thermistor(g):
    """NTC thermistor (IEC 60617): the temperature mark 't°' top right, the INK resistor on MID_M and
    the non-linear slash with its foot bottom-left, re-fitted under the mark."""
    mark(g, 'temperature')
    resistor(g, y=MID_M)
    nonlinear_slash(g, 9, 26, 21, 14)


def block_capacitor(g):
    """power_capacitor_block: the capacitor on MID_M under the block mark 'B', exactly as
    STYLE 8.4 calls it (no polar '+': one mark per variant in the top band, and '+' would echo the
    kit's 'advanced' mark)."""
    mark(g, 'B')
    capacitor(g, x=8, h=14, y=MID_M)


def block_inductor(g):
    """power_inductor_block: the inductor humps on MID_M under the block mark 'B' (STYLE 8.4)."""
    mark(g, 'B')
    inductor(g, y=MID_M)
