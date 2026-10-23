"""signal_button - ledswitch.obj (ElectricalGateSourceDescriptor 'button'), wall sixnode.

The model's own white plate (tier_cue none), recessed square with the push-button cube.
drawItem draws factor 0: the LED and its halo in ledOnOffColor(false) = red, so the LED is red
and the halo shows as four short red rays on the recess floor.  Siblings on the same model:
signal_switch (signal plate, LED green = latched on) and signal_switch_with_led (signal plate,
LIT LED, no halo)."""
from kit import MARK_RED
from families.signal_io import led_switch


def draw():
    return led_switch('white', MARK_RED, glow=MARK_RED)
