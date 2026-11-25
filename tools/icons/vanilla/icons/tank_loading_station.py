"""tank_loading_station - Tank Loading Station: a rail on a bed with a side flange and a hose arm over the track
(TankCartParts station)."""
from kit import *
from families.tank_carts import station


def draw():
    cv = Canvas(16)
    station(cv)
    return cv
