"""very_high_voltage_cable - cable template (kit.cable): flat strip with one S-bend, jacket in the 'vhv' tier
ramp (lit half ramp[4], shaded half ramp[3]), tinned STEEL ends.  The tier is read from the jacket
colour only; width follows the world but is never the cue."""
from kit import cable


def draw():
    return cable('vhv')
