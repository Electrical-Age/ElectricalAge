"""oil_chain family helpers (schematic set): the electrical members of the oil chain only (the oil pumps and
the tar sand separators; the wellhead, the column and the flare stack take no power and are out of scope).

- Oil pump (50v/200v_oil_pump): a motor driving a pump, so the IEC rotating machine 'M' (as the electrical
  motor) in the tier hue at RING_M, with the kit fuel-drop pictogram in the mark slot for what it moves. It
  shares the 'drop' mark with the fuel generators but not the letter: 'M' (motor, it takes power) against
  'G' (generator, it gives power), the same structural split as motor / generator.
- Tar sand separator (50v/200v_tar_sand_separator): a process machine (STYLE 7.2). INK feed arrow in at the
  top, the process element in the tier hue in the middle: a separating hopper (straight sides tapering
  in 45-degree arms to an open neck), and the product below it, the kit drop pictogram (the crude it gives). Deliberately not the
  macerator's rollers, although the in-world model is built like a macerator: the funnel is what tells the
  two apart.
"""
from kit import RING_M, LO, mark, machine, feed_arrow, pictogram


def oil_pump(g):
    mark(g, 'fuel')
    machine(g, 'M', g.hue, ring=RING_M)


def tar_sand_separator(g):
    feed_arrow(g, x=15)
    g.vline(4, LO, 8, g.hue)
    g.line(4, 8, 11, 15, g.hue)
    g.mirror_x()
    pictogram(g, 'drop', 12, 20)
