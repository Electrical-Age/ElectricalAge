"""50v_steam_turbine - steamturbine50v (src/cadmodels SteamTurbine), LV: side-on from the z+ flank, steel finned generator with a broad LV band, copper coupling, dark cast-iron scroll casing with riser and steel top flange, on the dark skid. Drawing in families/steam_turbines.py."""
from families.steam_turbines import steam_turbine_set


def draw():
    return steam_turbine_set('lv')
