"""signal_switch - ledswitch.obj (the gate source 'Signal Switch', a latching toggle), wall sixnode.

Tier cue 'plate' (manifest): the square wall plate is the signal plate, as on
signal_switch_with_led.  The model is shared with signal_button and signal_switch_with_led, so the
state axis tells them apart: the toggle is drawn latched ON, LED and halo in ledOnOffColor(true)
= green, with the button pressed in (it latches) and the halo as a lit ring round it: a geometric
difference that survives colour blindness (signal_button: cube out, red, off;
signal_switch_with_led: cube out, LIT LED, no halo)."""
from kit import MARK_GREEN
from families.signal_io import led_switch


def draw():
    return led_switch('signal', MARK_GREEN, glow=MARK_GREEN, pressed=True)
