"""200v_power_socket - wall sixnode (powersocket.obj: SocketBase + Socket200V), front-on, 'flat' depth.

Tier cue 'plate' (decided centrally): the square wall plate is the MV tier plate.  The 200V
recess is the model's octagon (Schuko-like) with two pin holes and the steel earth clip at the
top; the 50V sibling has a flat hexagon instead.  See families/cables.py."""
from families.cables import socket


def draw():
    return socket('mv', '200v')
