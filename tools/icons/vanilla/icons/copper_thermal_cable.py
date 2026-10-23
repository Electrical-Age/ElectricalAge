"""copper_thermal_cable - cable template (kit.cable) in the THERMAL ramp (textures/voltages/thermal.png
grey), same S-bend strip and tinned STEEL ends as the five tier cables, 4 px like the in-world
thermal cable."""
from kit import cable


def draw():
    return cable('thermal')
