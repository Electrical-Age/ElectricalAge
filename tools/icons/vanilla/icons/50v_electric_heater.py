"""50v_electric_heater - 48V Electric Heater (cadmodels ElectricHeater 'electricheater50v'): steel block, glowing element behind a grille, LV lid (2-row ramp[3] lip), copper heat pad on the side. Drawing in families/steam_plant.py."""
from families.steam_plant import electric_heater


def draw():
    return electric_heater('lv')
