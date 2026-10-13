"""tools family helpers: only the portable energy stores are in schematic scope (STYLE 8.4).

The family's shared visual system:
- All four items are untiered (manifest tier null), so every stroke is INK; the symbol's structure
  carries the meaning (STYLE 5.1). Nothing of the in-world cans/cells is drawn.
- Battery vs capacitor: the IEC cell (long + short plate) vs the IEC capacitor (two equal plates).
- Single vs pack: one element vs two elements in series. Both packs share one geometry (plates at
  6/12/18/24, a 4 px series wire at cols 14..17, symmetric 2 px leads) and differ only in plate
  heights; the kit's cells=2 layouts are worked around (see battery_pack / capacitor_pack).
- No mark: the stationary batteries and chargers carry kind marks and a tier hue, the power capacitor
  carries the polar '+'; the portable ones are the bare symbols, so they differ in structure, not
  only in colour.
"""
from kit import MID, battery, battery_block, capacitor, leads

# The single capacitor is centred (plates at cols 11-12 / 19-20); kit capacitor() defaults to x=13,
# which leaves room for power_capacitor's polar '+' that the portable one does not have.
CAP_X = 11

KINDS = ('battery', 'capacitor')


def portable_store(g, kind, pack=False):
    """A portable battery / capacitor (pack=True: the two-element pack), STYLE 8.4."""
    assert kind in KINDS, kind
    cells = 2 if pack else 1
    if kind == 'battery' and pack:
        battery_pack(g)
    elif kind == 'battery':
        battery_block(g)
    elif pack:
        capacitor_pack(g)
    else:
        capacitor(g, x=CAP_X)


def capacitor_pack(g):
    """Two IEC capacitors in series, symmetric: plates 6-7 / 12-13 | 18-19 / 24-25 (4 clear px
    between the plates of one capacitor), a 4 px wire (cols 14..17) between the two, leads 4-5 and
    26-27. Workaround for kit capacitor(cells=2), whose series wire is 1 px wide (the two middle
    plates fuse into an 'H') and whose right lead is 1 px (col 27 only)."""
    capacitor(g, x=6, gap=4, with_leads=False)          # left capacitor: plates 6-7 and 12-13
    g.mirror_x()                                        # right capacitor: plates 18-19 and 24-25
    g.hline(14, 17, MID)                                # series wire between the two
    leads(g, 5, 26)


def battery_pack(g):
    """Two IEC cells in series, the same geometry as capacitor_pack: long 6-7 / short 12-13 |
    long 18-19 / short 24-25, a 4 px wire (cols 14..17) between the cells, leads 4-5 and 26-27.
    Workaround for kit battery(cells=2) (battery_block(g, cells=2)), whose 3 px inter-cell gap is
    narrower than the 4 px gap inside each cell, so at GUI 2 the middle short + long plates pair
    up (plate / pair / plate), and whose leads are uneven (3 px left, 2 px right). Same cell
    orientation for both cells (long plate left), so not a mirror. In the tier hue (INK: untiered)."""
    battery(g, g.hue, x=6, with_leads=False)            # left cell: long 6-7, short 12-13
    battery(g, g.hue, x=18, with_leads=False)           # right cell: long 18-19, short 24-25
    g.hline(14, 17, MID, g.hue)                         # series wire between the cells
    leads(g, 5, 26, lead=g.hue)
