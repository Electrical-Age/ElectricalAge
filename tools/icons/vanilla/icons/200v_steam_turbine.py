"""200v_steam_turbine - steamturbine200v (src/cadmodels SteamTurbine), MV: the 48V drawing scaled up as the model is (bigger generator with three fins, taller casing), MV band. Drawing in families/steam_turbines.py."""
from families.steam_turbines import steam_turbine_set


def draw():
    return steam_turbine_set('mv')
