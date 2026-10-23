"""fuel_heat_furnace - fuelheater.obj: the copper-orange (VCOPPER) block with its carrying
handle lying along the top, the LED plate at its front end, and the dark round THERMAL burner
port centred on the front (no burner installed,
as drawItem renders it).  See families/solar_thermal.py."""
from families.solar_thermal import fuel_furnace


def draw():
    return fuel_furnace()
