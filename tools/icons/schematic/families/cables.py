"""cable_junction family helpers (schematic set): power sockets, utility poles, the small transformer.

The family's visual system:
    cables        kit cable_route() in the tier hue (THERMAL + heat_waves for the thermal cable)
    ground_cable  kit ground(), all INK
    hub           four INK wires meeting at a kit contact() node (6x6, wider than the 2px cross)
    power sockets IEC socket outlet: a bar with a half ring hanging under it (the kit bell() flipped),
                  in the tier hue, one INK lead up to the top core edge
    utility poles the overhead line in the hv hue with the pole drawn as a plan-view node (an hv ring
                  threaded on the line, as in network maps), never as a side-view T pole (decision 5).
                  Both poles use the same D=8 node. The converter pole raises it to the top and hangs
                  the INK kit transformer (transformer(y0=12), as in dc_dc_converter) directly under it.
"""
from kit import INK, MID, LO, HI, contact, transformer


def power_socket(g, lead=INK):
    """IEC socket outlet in g.hue: kit bell() proportions flipped: a 2px bar (cols 6..25, rows 12-13)
    with a 16px half ring (cols 8..23) hanging under it, so the bar ends stand proud of the cup,
    and a single INK lead from the top core edge down to the middle of the bar."""
    bar = 12                                             # bar rows 12-13
    g.hline(6, 25, bar, g.hue)
    g.circ(8, bar - 7, 16, g.hue, keep=lambda dx, dy: dy > 0.5)   # lower half: rows 13..20
    g.vline(15, LO, bar - 1, lead)


def pole_node(g, y, D):
    """Overhead line on rows y, y+1 across the core box, with the pole as a ring of diameter D
    (plan view, as in network maps) threaded on it at the centre; all in g.hue. Returns the ring's bbox."""
    x0 = 16 - D // 2
    y0 = y + 1 - D // 2
    g.circ(x0, y0, D, g.hue)
    g.hline(LO, x0 + 1, y, g.hue)
    g.hline(x0 + D - 2, HI, y, g.hue)
    return x0, y0, x0 + D - 1, y0 + D - 1


def utility_pole(g, converter=False):
    """Utility pole: the hv overhead line with a D=8 ring pole node threaded on it.
    Plain: line + node on MID. converter=True: line + node raised (line rows 7-8, node rows 4..11)
    with the INK kit transformer moved down to rows 12..27 (transformer(y0=12)) hanging directly
    from the node's bottom."""
    if not converter:
        pole_node(g, MID, D=8)
        return
    pole_node(g, 7, D=8)
    transformer(g, INK, y0=12)


def hub(g):
    """Hub: four INK wires from the core edges meeting at a kit contact() node (STYLE 11: a 4-way
    node on 2px wires uses contact(); kit junction()'s 4x4 DOT would vanish inside the cross)."""
    g.hline(LO, HI, MID)
    g.vline(MID, LO, HI)
    contact(g, MID, MID)
