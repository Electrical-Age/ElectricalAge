"""modbus_rtu - rtu.obj (ModbusRtuDescriptor), wall sixnode; hidden in the 26.x port.

Light grey speckled box (STEEL, the texture's #c6c6c6, two speckles), nearly as deep as wide.
drawItem draws the door 70 % open: the dark interior shows the brown display panel in its navy
frame with the three status LEDs on its right edge (power lit green, activity/error dim).

As in the model's front view (rx), the front keeps a light STEEL[3] frame on all four sides
round the dark opening (row 4, col 5, col 12, row 13), the layout of signal_processor; the
inner left jamb is the body ramp[1] (STYLE section 3).  The open door is the foreshortened
panel on the hinge side, its face light (STEEL[4]/[3]) so the whole device reads light grey
with a dark window, not a dark box."""
from kit import Canvas, box, STEEL, CAV, WOOD, SOLAR, MARK_GREEN

#: door panel columns 1..3 -> (first row, last row); the hinge edge (col 3) is full height
DOOR = ((5, 12), (4, 13), (3, 14))


def draw():
    cv = Canvas(16)
    S = STEEL
    box(cv, 4, 3, 9, 12, 2, S, bevel=False)           # front cols 4..12, rows 3..14
    cv.rect(6, 5, 6, 8, CAV[1])                       # opening cols 6..11, rows 5..12
    cv.vline(6, 5, 12, S[1])                          # inner left jamb
    cv.hline(7, 11, 5, CAV[2])                        # inner top
    cv.rect(7, 6, 4, 7, SOLAR[2])                     # display frame cols 7..10, rows 6..12
    cv.rect(7, 6, 3, 6, WOOD[3])                      # brown display cols 7..9, rows 6..11
    cv.set(7, 6, WOOD[4])
    cv.set(10, 9, MARK_GREEN)                         # power LED
    cv.set(10, 10, CAV[4])
    cv.set(10, 11, CAV[4])
    # the open door: outer edge col 1 and the top/bottom ends outlined, face cols 2..3 light
    for x, (a, b) in zip(range(1, 4), DOOR):
        cv.vline(x, a, b, S[3])
        cv.set(x, a, S[1])
        cv.set(x, b, S[0])
    cv.vline(1, 5, 12, S[1])
    cv.vline(2, 5, 12, S[4])                          # lit free edge of the door face
    cv.set(13, 7, S[1]); cv.set(3, 9, S[2])           # speckles
    return cv
