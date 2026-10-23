"""Family helper for the "chips" job (logic_chip, analog_signal).

The 21 chip icons are one-line ``chip_icon(id)`` modules (kit.chip, frozen CHIP_FACES); nothing
for them lives here.  WHITE_PLASTIC (the plc cabinet of signal_processor) was promoted to
``kit.palette``; it is re-exported here so older imports keep working.
"""
from kit import WHITE_PLASTIC  # noqa: F401  (promoted to kit.palette in kit revision 3)
