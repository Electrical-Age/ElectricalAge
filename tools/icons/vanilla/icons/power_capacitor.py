"""power_capacitor - placeable sixnode, 16 px reference (cabinet oblique, depth 2).

Model powerelectricprimitives: Base + CapacitorCore + CapacitorCables - a steel U-bracket
(base plate and two end walls) holding a blue can lying along x, with a grey lead between
the can's left end and the left wall.

Worked example of three kit rules:
 * one material = one ``solid()`` union: the bracket is ONE solid, so it has one outline and no
   seams where the walls meet the plate;
 * round parts use ``cylinder()`` (diameter 6 -> band table CYL_BANDS[6]);
 * occlusion: the can sits between the walls, so the right wall is drawn again on top of it,
   copied from a separate render of the bracket (``faces(zb, 'rw')``), outline included.
"""
from kit import Canvas, solid, paint_solid, faces, cylinder, STEEL, CABLE, CAP_BLUE


def bracket():
    zb = solid([('base', 1, 13, 12, 2, 2), ('lw', 1, 4, 2, 11, 2), ('rw', 11, 4, 2, 11, 2)])
    cv = Canvas(16)
    paint_solid(cv, zb, STEEL)
    return cv, zb


def draw():
    cv, zb = bracket()
    front = cv.copy()
    cylinder(cv, 4, 5, 8, 6, CAP_BLUE)                 # can: x 4..11, rows 5..10 (behind the right wall)
    cv.vline(3, 7, 8, CABLE[3])                        # the lead from the can to the left wall
    cv.set(3, 7, CABLE[4])
    for p in faces(zb, 'rw'):                          # right wall in front of the can
        cv.set(*p, front.get(*p))
    return cv
