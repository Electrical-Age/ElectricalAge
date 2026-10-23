"""50v_power_socket - wall sixnode (powersocket.obj: SocketBase + Socket50V), front-on, 'flat' depth.

Tier cue 'plate' (decided centrally): the square wall plate is the LV tier plate.  The 50V
recess is the model's flat horizontal hexagon with two pins high left/right and one low centre
pin (CAV); the 200V sibling has an octagon instead.  See families/cables.py."""
from families.cables import socket


def draw():
    return socket('lv', '50v')
