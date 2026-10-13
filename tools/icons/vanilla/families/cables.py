"""Family helper: cable_junction (cables, wall sockets, hub, ground plate, utility poles).

Shared visual system
--------------------
* Cables (the five tier cables and the thermal cable) are the kit template ``cable(tier)``:
  one S-bent jacket, tier colour on the jacket, tinned STEEL ends.  Nothing here.
* Wall parts (power sockets, ground plate, hub) are sixnode plates drawn front-on with the
  oblique depth receding up-right.  Sockets are only 0.03 blocks thick, so they use
  ``depth('flat')`` (= a chamfered square); the ground plate and the hub stand proud of the wall
  and use ``depth('sixnode')``.
* Power sockets: ``tier_cue = plate`` -> the square wall plate is the tier plate (LV green /
  MV blue); the socket recess is CAV with STEEL contacts.  The recess SHAPE is the 50V/200V
  model difference (flat hexagon with three pins vs octagon with two pins and an earth clip);
  colour is the tier cue.
* Utility poles: one POLE_WOOD ``solid()`` (post + crossarm, + the foot on the converter pole, which
  is the only pole whose descriptor draws the ``foot`` part), grey STEEL line clamps (p0/g0)
  under the arm.  The converter pole adds the STEEL transformer box hanging beside the post,
  and the widened foot.

    socket(tier, kind)            kind '50v' | '200v'
    pole(converter=False)
"""
from kit import (Canvas, box, solid, paint_solid, plate_ramp, depth,
                 STEEL, CAV, POLE_WOOD, GOLD)


# --------------------------------------------------------------------------- power sockets
#: socket recesses, 'o' recess floor, 'x' pin hole, 'e' earth clip (STEEL), 's' shadow row
_SOCK = {
    # 50V: Swiss-style flat hexagon, two pins high left/right, one pin low in the centre
    '50v': ["..ssssss..",
            ".sooooooo.",
            "soxxooxxoo",
            "sooooooooo",
            ".sooxxooo.",
            "..oooooo.."],
    # 200V: Schuko-style octagon, two pins, earth clip at the top
    '200v': ["..ssss..",
             ".sseesoo",
             "ssoooooo",
             "sxxooxxo",
             "sxxooxxo",
             "soooooo.",
             ".oooooo.",
             "..oooo.."],
}


def socket(tier, kind):
    cv = Canvas(16)
    P = plate_ramp(tier)
    d = depth(cv, 'flat')
    box(cv, 1, 2, 13, 13, d, P)                      # square wall plate, chamfered by the oblique
    rows = _SOCK[kind]
    w = max(len(r) for r in rows)
    ox = 2 + (12 - w) // 2
    oy = 2 + (12 - len(rows)) // 2
    rec = set()
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch == '.':
                continue
            p = (ox + i, oy + j)
            rec.add(p)
            cv.set(*p, {'o': CAV[3], 's': CAV[2], 'x': CAV[0], 'e': STEEL[4]}[ch])
    # the recess lip: plate shaded above/left of the opening, lit below/right (an opening)
    for (x, y) in list(rec):
        for (ax, ay), c in (((x + 1, y), P[4]), ((x, y + 1), P[4])):
            if (ax, ay) not in rec and cv.get(ax, ay) == P[3]:
                cv.set(ax, ay, c)
    return cv


# --------------------------------------------------------------------------- utility poles
def pole(converter=False):
    cv = Canvas(16)
    px = 6 if not converter else 5                   # post front column
    # one POLE_WOOD union (one outline, no seams); the post is listed last so it wins the z-ties
    parts = [('arm', 1, 4, 13, 2, 1),                # crossarm: cols 1..13 (+1 depth), rows 4..5
             ('post', px, 3, 3, 12, 1)]              # post: cols px..px+2, rows 3..14 (+1 depth)
    if converter:
        parts.append(('foot', px - 2, 13, 7, 2, 1))  # widened base foot (static part 'foot')
    zb = solid(parts)
    paint_solid(cv, zb, POLE_WOOD)
    # line clamps p0/g0 under the arm (grey, as in the model)
    for x in (3, 11) if not converter else (2,):    # on the converter pole g0 is behind the box
        cv.set(x, 6, STEEL[4])
        cv.set(x, 7, STEEL[2])
    if converter:
        _transformer(cv, px)
    return cv


def _transformer(cv, px):
    """STEEL transformer box hanging from the arm right of the post, one vent slot, warning label.
    (The model's feed wires and conduit are left out: at 16 px they have no room.)"""
    bx, by, bw, bh = px + 4, 7, 5, 4                  # front face cols px+4.., rows 7..10 (top 6)
    box(cv, bx, by, bw, bh, 1, STEEL)                # leaves a run of post visible above the foot
    cv.hline(bx + 1, bx + bw - 2, 8, CAV[1])         # vent slot
    cv.set(bx + bw - 2, by + bh - 2, GOLD[4])        # warning label, bottom right as in the model
