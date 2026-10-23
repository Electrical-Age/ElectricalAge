"""sensors family: sensors, probes and the fire buzzer (STYLE.md section 8.5).

Shared system. Every member is a thin wrapper over a kit convention; nothing is re-placed here.
The SENSING element carries the signal hue; what is sensed is INK (a mark, arrows or a letter).
Three sub-families, told apart by structure:

    optical   photodiode (diode on MID_M) + what it sees:
              electrical_light_sensor     two incident-light arrows (kit photodiode)
              electrical_daylight_sensor  sun mark      (kit sensor_diode 'daylight')
              electrical_fire_detector    flame mark    (kit sensor_diode 'fire')
              electrical_entity_sensor    person mark   (kit sensor_diode 'entity', IR presence)
    resistive electrical_weather_sensor   cloud mark + rain resistor on MID_M
    meters    ring in the signal hue + INK quantity letter (kit meter):
              voltage_probe V, electrical_probe A (provisional, see probe()), tachometer n,
              scanner L (level), and with a mark (RING_M): temperature_probe P+T, thermal_probe H+T,
              electrical_anemometer_sensor wind+n
    alarm     electrical_fire_buzzer (untiered, INK): flame mark + IEC bell

Light vs daylight: the light sensor keeps the IEC 'incident radiation' arrows over a centred diode;
the daylight sensor has no arrows, the sun pictogram in the mark slot and the diode shifted left.

scanner (family proposal, no table row): it reads the block it faces like a comparator (tank /
inventory fill as a 0..1 signal), so it is a meter whose quantity is the fill level: 'L' in the ring
(ISA 5.1 first letter L = level). '%' was tried first and rejected: the FONT '%' knots into a blob at
GUI 2 and its diagonal echoed analog_gauge's needle.

RING_M meters (temperature_probe, thermal_probe, electrical_anemometer_sensor): in a D=16 ring the clear
interior is 10 rows and a 7-row letter plus its two halo rows needs 9, so the letter's halo always
touches the ring's inner halo on one side. A kit geometry limit (set-wide), not a placement error.
"""
from kit import (RING, RING_M, MID_M, mark, meter, resistor, photodiode, sensor_diode, bell)

__all__ = ['optical', 'weather', 'probe', 'fire_buzzer']


def optical(g, kind=None):
    """Optical / presence sensors: kind None = the plain photodiode with incident arrows (light
    sensor); 'daylight' / 'fire' / 'entity' = the sensed thing as a mark over the diode."""
    if kind is None:
        photodiode(g, g.hue)
    else:
        sensor_diode(g, kind)


def weather(g):
    """Weather sensor: cloud (rain) mark + the rain resistor in the signal hue on MID_M."""
    mark(g, 'weather')
    resistor(g, g.hue, y=MID_M)


def probe(g, letter, mark_key=None):
    """Meters and probes: ring in the signal hue with the INK quantity letter. With a mark
    (P probe, H heat side, wind) the ring moves to RING_M and the mark goes top right.
    electrical_probe uses 'A' provisionally (STYLE 8.5): in the Java code it is a switchable
    V / A / W probe (ElectricalSensorElement typeOfSensor power/current/voltage), default voltage
    (ElectricalSensorElement.java:55). 'V' is voltage_probe's glyph, so the owner choice is 'A' or 'W'."""
    if mark_key is None:
        meter(g, letter, g.hue, ring=RING)
    else:
        mark(g, mark_key)
        meter(g, letter, g.hue, ring=RING_M)


def fire_buzzer(g):
    """Fire buzzer (battery powered, untiered: g.hue is INK): flame mark + IEC bell (STYLE 8.5)."""
    mark(g, 'fire')
    bell(g, g.hue, y=MID_M + 4)
