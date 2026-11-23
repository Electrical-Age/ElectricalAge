"""steam_condenser - Steam Condenser (cadmodels Condenser 'condenser200v', untiered): grey shell wound with copper fins, exhaust stack, the model's thin blue (PAINT_200V) ring, on the skid. Drawing in families/steam_plant.py."""
from families.steam_plant import condenser


def draw():
    return condenser('mv')
