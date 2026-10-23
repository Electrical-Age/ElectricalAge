"""wireless_switch - ledswitch.obj (the latching toggle), untiered (white plate: tier_cue null).

Drawn latched ON, as signal_switch: button pressed in flush, LED and halo green
(ledOnOffColor(true)), the halo as a lit ring round it, so it differs from wireless_button by
geometry (ring vs rays, flush vs proud) and survives colour blindness.  Wireless mark: the same
printed wave arc as wireless_button.  See families/wireless.py."""
from kit import MARK_GREEN
from families.wireless import led_switch


def draw():
    return led_switch(MARK_GREEN, glow=MARK_GREEN, pressed=True)
