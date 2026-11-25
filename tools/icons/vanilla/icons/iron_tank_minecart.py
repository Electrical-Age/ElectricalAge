"""iron_tank_minecart - Iron Tank Minecart: a minecart carrying a dark steel tank on its side (TankCartParts load)."""
from kit import *
from families.tank_carts import cart


def draw():
    cv = Canvas(16)
    cart(cv, STEEL)
    return cv
