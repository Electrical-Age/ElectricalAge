"""generators family helpers (fuel generators, turbines, the shaft generator, the electrical source).

The family's shared visual system (STYLE.md sections 6, 7.3 and 5.4):
- Every machine is the IEC rotating machine: a ring with an INK 'G'. The ring is the defining
  element, so it carries the tier hue (LV green, MV blue, VHV red) or is INK when untiered.
- What drives the machine is the kit input pictogram in the mark slot (top right), with the ring
  moved down-left to RING_M (kit generator()):
      fuel drop   50v/200v_fuel_generator   (tier siblings: LV / MV, colour only)
      steam       50v/200v_turbine          (heat-difference turbines, tier siblings: LV / MV)
      steam       steam_turbine             (untiered: all INK)
      flame       gas_turbine               (untiered: all INK)
      water       water_turbine             (LV)
      wind        wind_turbine              (LV)
- The shaft generator (`generator`, VHV) is driven by the shaft network, not by a fuel, so it has
  no mark: the plain 'G' machine at the full RING (kit machine()).
- The electrical source is a source, not a machine: no 'G'. It is untiered (all INK) and the
  signal_source (signal tier) uses the plain IEC voltage source (ring with the conductor through it,
  horizontal leads). STYLE 5.4 forbids an untiered item reusing a signal symbol unchanged, so the
  electrical source is drawn as the DC voltage source with polarity instead: the ring with '+' in
  the upper half and '-' in the lower half and its terminals top and bottom (the symbol printed on
  the in-world plate). Different in structure, not only in colour.
"""
from kit import FONT, RING, generator, machine, ring_center

DRIVES = ('fuel', 'steam', 'gas', 'water', 'wind')


def driven_generator(g, drive):
    """A generator or turbine with a named input: the 'G' ring (RING_M) in the tier hue (INK if
    untiered) + the drive's input pictogram top right."""
    assert drive in DRIVES, drive
    generator(g, drive)


def shaft_generator(g):
    """The shaft-driven generator: the plain 'G' machine, full RING, ring in the tier hue."""
    machine(g, 'G', g.hue)


def dc_source(g, c=None, ring=RING):
    """IEEE/IEC DC voltage source with polarity: a ring (RING) with a '+' in the upper half and a
    '-' in the lower half, leads out of the top and bottom on the centre column (cols 15-16).
    c: ring colour (default g.hue, which is INK for an untiered glyph); signs and leads INK."""
    if c is None:
        c = g.hue
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    xm = int(cx) - 1                      # 15: left column of the centred 2px stroke
    g.circ(x0, y0, D, c)
    # leads: top edge -> ring, ring -> bottom edge (they join the ring, one part with it)
    g.vline(xm, 4, y0 - 1)
    g.vline(xm, y0 + D, 27)
    # the kit font's '+' (6x6, 2px arms) in the upper half and '-' in the lower half: '+' rows 10..15
    # (one clear row below the ring's inner edge at the top), the '-' bar on rows 19-20, 3 clear rows apart
    top = y0 + 3                          # 10
    g.bitmap(xm - 2, top, FONT['+'])
    g.bitmap(xm - 2, top + 7, FONT['-'])  # bitmap rows 17..22, bar rows 19-20
