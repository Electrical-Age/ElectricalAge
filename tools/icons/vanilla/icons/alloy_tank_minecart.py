"""alloy_tank_minecart - Alloy Tank Minecart: a minecart carrying an alloy tank on its side (TankCartParts load)."""
from kit import *
from families.tank_carts import cart


def draw():
    cv = Canvas(16)
    cart(cv, ALLOY)
    return cv
