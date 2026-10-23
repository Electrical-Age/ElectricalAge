"""signal_source - signalsource.obj (ElectricalSourceDescriptor, signalSource=true), wall sixnode.

A green square module with a black square-wave glyph and a small bright green LED in a black
frame (bottom right).  The model's green (#3a8459) sits on the LV hue, so the body is drawn as
the signal plate (plate_ramp('signal'), the signal tier's own green) rather than a new green
ramp: same hue family as the model, and no false LV cue."""
from kit import Canvas, MARK_GREEN
from families.signal_io import wall_plate, INK

WAVE = [
    "..###.###",
    "..#.#.#.#",
    "..#.#.#.#",
    "###.###.#",
]


def draw():
    cv = Canvas(16)
    wall_plate(cv, 'signal')
    cv.overlay(WAVE, {'#': INK}, ox=2, oy=4)                 # cols 2..10, rows 4..7
    cv.rect(7, 9, 4, 4, INK)                                 # LED frame cols 7..10, rows 9..12
    cv.rect(8, 10, 2, 2, MARK_GREEN)
    return cv
