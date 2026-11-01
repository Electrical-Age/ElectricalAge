"""bus_io_module - rtu.obj (BusModuleDescriptor) without its door, on the field bus.

The Modbus RTU's light grey speckled box (STEEL), seen from the front: its dark window with the brown
display in its navy frame and the three status LEDs (power lit green).  Its front edge takes the
field bus: a violet ribbon pin along the top, the bus cable's colour.  The other three edges hold
three pale signal pins each, as the module's nine terminals."""
from kit import Canvas, STEEL, CAV, WOOD, SOLAR, MARK_GREEN
from kit.color import h

VIOLET = (h('#4a2a63'), h('#6b3d8c'), h('#8e62b0'))   # the bus cable's 0.42, 0.24, 0.55, shaded
PIN = h('#e6e6dc')                                     # a signal pin (SixNodeElementRender.pinColour)


def draw():
    cv = Canvas(16)
    S = STEEL
    cv.rect(3, 3, 10, 10, S[3])                       # the box, cols 3..12, rows 3..12
    cv.hline(3, 12, 12, S[1])                         # its shaded lower edge
    cv.vline(12, 3, 12, S[2])                         # and right edge
    cv.rect(5, 5, 6, 6, CAV[1])                       # window cols 5..10, rows 5..10
    cv.hline(5, 10, 5, CAV[2])
    cv.rect(6, 6, 3, 4, SOLAR[2])                     # display frame cols 6..8, rows 6..9
    cv.rect(6, 6, 2, 3, WOOD[3])                      # brown display
    cv.set(9, 7, MARK_GREEN)                          # power LED
    cv.set(9, 8, CAV[4])
    cv.set(9, 9, CAV[4])
    cv.set(4, 10, S[2]); cv.set(11, 4, S[4])          # speckles
    # the bus pin: a violet ribbon on the top edge
    cv.hline(5, 10, 1, VIOLET[2])
    cv.hline(5, 10, 2, VIOLET[1])
    cv.set(5, 2, VIOLET[0]); cv.set(10, 2, VIOLET[0])
    # nine signal pins, three on each other edge
    for k in (4, 7, 10):
        cv.hline(1, 2, k, PIN)                        # left
        cv.hline(13, 14, k, PIN)                      # right
        cv.vline(k, 13, 14, PIN)                      # bottom
    return cv
