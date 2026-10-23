"""parts_mechanical (manifest family machine_part, schematic scope: the two electrical motors only).

Shared system (STYLE 7.3): the IEC rotating machine, a ring with an INK 'M'. Both motors are untiered
(manifest tier null), so the whole glyph is INK. The plain motor is the standard layout (full RING, leads
both sides); the advanced motor adds the kit mark 'advanced' (+) top right and moves the 'M' ring to RING_M,
exactly as the advanced_energy_meter does next to the energy_meter."""
from kit import mark, machine, RING, RING_M


def motor(g, advanced=False):
    """Electrical motor glyph: machine(g, 'M') at RING; advanced: mark '+' + the same machine at RING_M."""
    if advanced:
        mark(g, 'advanced')
        machine(g, 'M', g.hue, ring=RING_M)
    else:
        machine(g, 'M', g.hue, ring=RING)
