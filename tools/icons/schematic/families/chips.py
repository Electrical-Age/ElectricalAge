"""Chips family helpers (logic_chip, analog_signal), STYLE.md section 9. The wave-1 helpers were
promoted into the kit (kit 2026-10-12); the names below are kept as thin wrappers so the icon modules
stay unchanged:

    gate3(g, kind, invert)   = gate(g, kind, g.hue, invert=invert)          (3 inputs, IN_3, the standard)
    opamp_signed(g)          = opamp(g, g.hue)                              (2 signed inputs, rows 11 / 17)
    amp(g, control)          = opamp(g, g.hue, inputs=1, control=control)   (amplifier, VCA)
    function_block(g)        signal_processor: function ring + 'f', three inputs left, one output right
"""
from kit import INK, LO, IN_3, gate, opamp, ring_letter

GATE3_INPUTS = IN_3


def _lead_to_body(g, y, lead=INK, stop=16):
    """2px INK lead on rows y, y+1 from the core edge to the first body pixel on those rows (the kit
    gate() rule)."""
    xe = next((x for x in range(LO, stop) if g.get(x, y) or g.get(x, y + 1)), None)
    if xe is not None and xe - 1 >= LO + 1:
        g.hline(LO, xe - 1, y, lead)


def gate3(g, kind, invert=False, lead=INK):
    """3-input IEEE gate (and/or/xor, optionally inverted) in g.hue: kit gate() with inputs=3."""
    gate(g, kind, g.hue, lead=lead, invert=invert, inputs=3)


def opamp_signed(g, lead=INK):
    """Op-amp in g.hue with the signed inputs (rows 11 / 17): kit opamp()."""
    opamp(g, g.hue, lead=lead)


def amp(g, control=False, lead=INK):
    """Single-input amplifier (amplifier, VCA) in g.hue: kit opamp(inputs=1, control=...)."""
    opamp(g, g.hue, lead=lead, inputs=1, control=control)


def function_block(g, letter='f', inputs=(10, 15, 20)):
    """Programmable function block (signal_processor): RING in g.hue with an INK FONT letter (STYLE 9:
    ring_letter(g, 'f', g.hue)), one output lead right on the centre line and THREE input leads left
    (rows 10-11, 15-16, 20-21; the outer two meet the ring on both rows), each running into the ring.
    The three-input fan is the structural difference from the summing point (inputs left + top) and
    from meters (one lead each side)."""
    ring_letter(g, letter, g.hue, INK, with_leads=True)   # centre input + output
    for y in inputs:
        if y != 15:
            _lead_to_body(g, y)
