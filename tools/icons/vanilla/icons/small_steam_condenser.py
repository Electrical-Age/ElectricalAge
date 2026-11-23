"""small_steam_condenser - Small Steam Condenser (cadmodels Condenser 'condenser50v', untiered): grey shell wound with copper fins, exhaust stack, the model's thin green (PAINT_50V) ring, on the skid; in SMALL_BOX. Drawing in families/steam_plant.py."""
from families.steam_plant import condenser


def draw():
    return condenser('lv', small=True)
