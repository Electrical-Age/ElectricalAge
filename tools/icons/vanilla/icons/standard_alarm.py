"""standard_alarm - alarmmedium.obj (ElectricalAlarmDescriptor, sound smallalarm_critical),
wall sixnode, drawn in the ON texture.  The lamp spans nearly the whole front (9 px), lit salmon
(face LEAD_RED[2], top/left outline LEAD_RED[1], bottom/right LEAD_RED[0]); the LIT beacon
blade is anchored at the lamp centre and recedes down-left over the lower-left rim.  See
families/signal_io.alarm."""
from families.signal_io import alarm


def draw():
    return alarm()
