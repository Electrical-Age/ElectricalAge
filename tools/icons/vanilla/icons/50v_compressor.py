"""50v_compressor - the press (compressora.obj), body = LV tier ramp (green texture); see families/machines.py."""
from families.machines import compressor


def draw():
    return compressor('lv')
