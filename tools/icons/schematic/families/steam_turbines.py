"""steam_turbines family helpers (schematic): the 48V/230V Steam Turbines, the 48V Compact Steam Turbine and the
230V Air Blower. All IEC rotating machines with the ring in the tier hue (STYLE 6, 7.3), kit marks only:

    steam_generator(g)          'steam' mark + 'G' ring at RING_M (the generators family convention for a
                                steam-driven generator: kit generator(g, 'steam'))
    steam_generator(g, small)   the same at RING_M_SMALL: the compact turbine is the small variant (STYLE 8:
                                "small" variants use the small symbol size, never a letter)
    blower(g)                   'wind' mark (the air it moves) + 'M' ring at RING_M: a motor driving a fan
"""
from kit import RING_M, RING_M_SMALL, machine, mark


def steam_generator(g, small=False):
    """kit generator(g, 'steam') at the small ring when small (the compact turbine)."""
    mark(g, 'steam')
    machine(g, 'G', g.hue, ring=RING_M_SMALL if small else RING_M)


def blower(g):
    mark(g, 'wind')
    machine(g, 'M', g.hue, ring=RING_M)
