"""230v_air_blower - airblower200v (src/cadmodels AirBlower), MV: from the intake flank, the blue volute drum side-on with the steel-ringed intake grille standing out of it, the finned steel motor on its saddle, on the skid. Drawing in families/steam_turbines.py."""
from families.steam_turbines import air_blower


def draw():
    return air_blower()
