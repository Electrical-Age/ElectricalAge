"""200v_compressor - the press (compressorb.obj), body = MV tier ramp (blue texture); see families/machines.py."""
from families.machines import compressor


def draw():
    return compressor('mv')
