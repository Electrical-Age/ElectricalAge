"""nuclear_alarm - alarmmedium.obj (ElectricalAlarmDescriptor, sound alarma, range 11), wall
sixnode, drawn in the ON texture.  Same model and texture as standard_alarm, so the print axis
tells them apart: a black radiation trefoil printed inside the lamp outline (one blade up, two
down: the standard orientation reads as a face at 7 px), no beacon blade.  The lamp spans
nearly the whole front (9 px, cols 3..11) as in the model, lit salmon: face LEAD_RED[2],
top/left outline LEAD_RED[1], bottom/right LEAD_RED[0].  See families/signal_io.alarm."""
from families.signal_io import alarm


def draw():
    return alarm('trefoil')
