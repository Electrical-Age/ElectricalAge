"""hub - wall sixnode (hub/hub.obj: 'main' cube 0.25 x 0.25 x 0.13 + six 'con' quads on its face),
front 12 x 12, sixnode depth 2, STEEL body.

The con quads form the 9 x 9 face motif (four corner brackets + a broken cross), transcribed
pixel for pixel from hub.png.  Drawn dark: HubDescriptor tints a connected con quad 40/255 grey,
and the item shows all six connected.  Untiered."""
from kit import Canvas, box, depth, STEEL, CAV

MOTIF = ["..#.#.#..",
         "..#.#.#..",
         "###.#.###",
         "....#....",
         "####.####",
         "....#....",
         "###.#.###",
         "..#.#.#..",
         "..#.#.#..",
         ]


def draw():
    cv = Canvas(16)
    box(cv, 1, 3, 12, 12, depth(cv, 'sixnode'), STEEL)    # front cols 1..12, rows 3..14
    cv.overlay(MOTIF, {'#': CAV[1]}, ox=3, oy=4)   # centred: 1 px margin all round
    return cv
