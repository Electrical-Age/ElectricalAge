"""Tank Minecarts and the Tank Loading Station (mods.eln.oil.TankCart, TankStationBlock; Baughn 2026-11-25, todo #611)."""
from kit import *

#: the minecart's dark iron body (vanilla's minecart is grey iron)
CART = IRON


def cart(cv, tank):
    """A minecart seen obliquely carrying a horizontal tank (ramp ``tank``) along its length."""
    # wheels under the body
    cv.rect(3, 13, 2, 2, CAV[1])
    cv.rect(10, 13, 2, 2, CAV[1])
    # the open body, low
    box(cv, 1, 9, 12, 4, 2, CART)
    # the tank lying in it, its ends rounded
    cylinder(cv, 2, 3, 12, 7, tank, axis='x')
    # a band round it
    cv.vline(7, 4, 8, tank[1])


def station(cv):
    """A short length of rail on a dark bed, a flange on its side and a post with a hose arm over the track."""
    # the bed and the rails along it, seen obliquely
    box(cv, 1, 12, 13, 2, 2, STEEL)
    cv.hline(2, 13, 11, IRON[4])
    cv.hline(3, 14, 10, IRON[3])
    # the flange on the near side, low
    cylinder(cv, 6, 10, 2, 4, IRON, axis='x', round_ends=False)
    # the post and the hose arm over the track, its hose end hanging
    box(cv, 12, 2, 2, 9, 1, IRON)
    cv.hline(5, 12, 2, IRON[4])
    cv.hline(5, 12, 3, IRON[2])
    cv.vline(6, 4, 6, CAV[2])
    # the signal lamp
    cv.set(14, 5, h('#c4302a'))
