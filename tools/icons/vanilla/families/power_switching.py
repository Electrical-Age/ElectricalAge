"""Family helper: power switching (cable switches, relays, LED switch, breaker), 16 px.

Shared visual system
--------------------
* Tier cue = 'plate' (manifest).  Every tiered device except the LED switch stands on the SAME
  tier slab: ``base()`` = ``tier_plate`` 12 wide, front rows 11..14, depth 2, so the tier is read
  from one large ramp[3] (signal: ramp[2]) area in the same place on every sibling.  The cable
  switches' models have no plate (the cue is decided centrally); the relays additionally show
  their real tier part, the tinted ``backplate``, through the open cage (see ``relay``).
* Above the slab, the device is drawn front-on as a likeness of its model.  Siblings that share
  an OBJ share a drawing; only the tier colour changes:
    - ``lever_switch``  lowvoltageswitch.obj (signal cable switch, LV, MV): charcoal box; the grey
      contact plate shows only as its two slotted ends either side of the octagonal rotary hub
      (``disc`` d5), which carries the white lever bar (rx view);
    - ``knife_switch``  highvoltageswitch.obj (HV, VHV): charcoal box, white U-shaped double knife
      blade with a clip block at both ends of both bars, black handle leaving the box on the left;
    - ``relay``         relaysmall / relaybig / relay800.obj: an open cage with a white frame; the
      tier-tinted backplate fills the field behind (plate_ramp(t)[3], the seen-through backplate);
      the copper coil runs the full width under the top rail on a steel yoke (so copper never
      touches the tier field), the white hex armature and its contact block hang below.  Cage
      width 8 / 10 / 12 follows the three models (depth 1: the cage stands slim and upright);
    - ``led_switch``    ledswitch.obj: the square wall plate IS the plate cue (signal), with a dark
      recess (lit lower/right lip), the white push-button cube and its LED drawn lit (drawItem
      draws state 0 = on);
    - ``breaker``       electricalbreaker.obj: untiered, a long light-steel case with the raised
      white centre module: blue band and red stripe at its left end, the blue rotary knob in its
      dark saddle with the flag through it, the grey bar at its right end.
* Neutral materials only besides the tier: RUBBER_BLACK (black plastic housings), STEEL (frames,
  blades, contacts), CAV (slots, recesses), COPPER (coil windings), LIT (the LED).
"""
from kit import (Canvas, box, disc, tier_plate, plate_ramp, STEEL, RUBBER_BLACK, CAV, COPPER, LIT,
                 WHITE, MARK_RED, CAP_BLUE)

HOUSING = RUBBER_BLACK
#: the breaker's white centre module: STEEL shifted one step lighter, outlined in STEEL[1]/[0]
MODULE = (STEEL[0], STEEL[1], STEEL[3], STEEL[4], WHITE)


def base(cv, tier):
    """The tier slab every tiered switch/relay stands on: front cols 1..12, rows 11..14, depth 2."""
    return tier_plate(cv, 1, 11, 12, 4, 2, tier, bevel=False)


# ------------------------------------------------------------------------------- cable switches
def _housing(cv):
    """Charcoal switch box standing on the slab one step back (a lit ledge of slab in front).
    Front cols 3..11, rows 3..9; top rows 1..2; side cols 12..13."""
    box(cv, 3, 3, 9, 7, 2, HOUSING, bevel=False)


def lever_switch(tier):
    cv = Canvas(16)
    base(cv, tier)
    _housing(cv)
    S = STEEL
    # contact plate: only its two slotted ends show either side of the hub (rx view)
    for x in (4, 10):
        cv.set(x, 5, S[3])
        cv.set(x, 6, CAV[1])
        cv.set(x, 7, S[3])
    # octagonal rotary hub (d5 disc: corners cut), lever bar through it
    disc(cv, 5, 4, 5, S)
    cv.vline(7, 4, 8, WHITE)
    cv.set(6, 5, S[4])                       # lit top-left of the hub face
    return cv


def knife_switch(tier):
    cv = Canvas(16)
    base(cv, tier)
    _housing(cv)
    S = STEEL
    face = [
        "...............",
        "...............",
        "...............",
        "......cc..cc...",   # 3  clips (hinge, contact) on the upper bar
        ".....WWWWWWW...",   # 4  upper blade
        ".hhhhWssssss...",   # 5  handle + blade shadow
        ".kkkkW.........",   # 6  handle lower edge
        ".....WWWWWWW...",   # 7  lower blade
        "......cc..cc...",   # 8  clips on the lower bar
    ]
    cv.overlay(face, {'W': S[4], 's': S[2], 'c': S[3],
                      'h': HOUSING[4], 'k': HOUSING[1]})
    cv.set(1, 6, HOUSING[1])
    cv.set(0, 5, HOUSING[2])                 # tapered tip of the handle
    return cv


# ------------------------------------------------------------------------------------- relays
def relay(tier, width):
    """Open-cage relay on the tier slab.  ``width`` 8 (relaysmall), 10 (relaybig), 12 (relay800).

    Cage front rows 2..10, depth 1 (the box's own S[1]/S[0] outline is the silhouette, its side
    column included).  Inside the outline: a white frame (top rail, both posts); the field
    behind is the tier-tinted backplate seen through the glass (plate_ramp(t)[3]).  The coil
    runs post to post under the top rail and sits on a dark steel yoke, so copper never touches
    the tier field; the white hex armature, its stem and the contact block hang below the yoke.
    """
    cv = Canvas(16)
    base(cv, tier)
    S = STEEL
    P = plate_ramp(tier)
    x0 = 1 + (12 - width) // 2
    x1 = x0 + width - 1
    box(cv, x0, 2, width, 9, 1, S, bevel=False)                # front rows 2..10, top row 1
    for yy in range(3, 10):                                    # seen-through tier backplate
        for xx in range(x0 + 2, x1):
            cv.set(xx, yy, P[3])
    cv.hline(x0 + 1, x1, 2, S[4])                              # white frame: top rail
    cv.vline(x0 + 1, 3, 9, S[4])                               # left post
    cv.vline(x1, 3, 9, S[4])                                   # right post
    a0, a1 = x0 + 2, x1 - 1
    cv.hline(a0, a1, 3, COPPER[3])                             # coil, post to post
    cv.hline(a0, a1, 4, COPPER[2])
    cv.hline(a0, a1, 5, S[1])                                  # yoke under the coil
    c = (a0 + a1) // 2                                         # armature (hexagon) + stem
    cv.hline(c - 1, c + 1, 6, WHITE)
    cv.hline(c - 1, c + 1, 7, S[4])
    cv.set(c, 8, S[3])
    cv.hline(c - 1, c + 1, 9, S[1])                            # contact block on the bottom rail
    return cv


# --------------------------------------------------------------------------------- LED switch
def led_switch():
    """ledswitch.obj: square wall plate (= signal plate), recessed centre, white push-button cube
    with its LED lit (LIT, the model's orange glow)."""
    cv = Canvas(16)
    P = plate_ramp('signal')
    tier_plate(cv, 1, 3, 12, 12, 2, 'signal', bevel=False)     # front cols 1..12, rows 3..14
    # recess cols 3..11, rows 4..12: floor in the plate's dark step, lit lower/right lip
    cv.rect(3, 4, 9, 9, P[1])
    cv.hline(4, 11, 12, P[4])
    cv.vline(11, 5, 12, P[4])
    cv.hline(3, 10, 4, P[0])                 # shaded top/left jambs
    cv.vline(3, 4, 11, P[0])
    # push-button: a white cube standing out of the recess, front cols 5..8, rows 7..10
    box(cv, 5, 7, 4, 4, 1, STEEL, bevel=False)
    cv.hline(6, 8, 7, WHITE)                 # lit top edge of the button face
    cv.set(7, 8, LIT[3])                     # the LED, lit
    return cv


# ------------------------------------------------------------------------------------ breaker
def breaker():
    """electricalbreaker.obj: long light-steel case (12x6 front); the raised white centre module
    carries the blue band and red stripe at its left end, the blue rotary knob in its dark saddle
    with the flag through it, and the grey bar at its right end (rx view).  Untiered: no slab."""
    cv = Canvas(16)
    S = STEEL
    box(cv, 1, 8, 12, 6, 2, S)                                 # case: front cols 1..12, rows 8..13
    box(cv, 3, 9, 8, 4, 1, MODULE, bevel=False)                # module: front cols 3..10, rows 9..12
    B = CAP_BLUE
    cv.vline(4, 9, 11, B[3])                                   # blue band
    cv.vline(5, 9, 11, MARK_RED)                               # red stripe
    cv.set(6, 10, CAV[2])                                      # saddle, left of the knob
    cv.vline(10, 9, 11, S[2])                                  # grey bar (= right saddle cheek)
    cv.hline(7, 9, 10, B[3])                                   # knob, 3x2
    cv.hline(7, 9, 11, B[2])
    cv.vline(8, 7, 12, B[5])                                   # flag, rising and dropping equally
    return cv
