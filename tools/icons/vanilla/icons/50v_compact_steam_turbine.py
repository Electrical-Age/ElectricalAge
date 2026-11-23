"""50v_compact_steam_turbine - compactturbine50v (src/cadmodels CompactTurbine), LV: front end in cabinet oblique, cast firebox with the steel drum boiler, the green-banded generator end, the finned condenser box with its vent stack, on one skid. Drawing in families/steam_turbines.py."""
from families.steam_turbines import compact_turbine


def draw():
    return compact_turbine()
