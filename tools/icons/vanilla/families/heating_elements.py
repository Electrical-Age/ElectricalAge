"""Family helper: heating_elements - the twelve ``*_heating_corp`` items (furnace / heater
resistors; no world model, never placed).

Shared visual system (STYLE.md section 8 "Heating elements", 7 and 7b)
---------------------------------------------------------------------
One drawing, three independent variant axes, each on its own fixed cue:

* **material** (copper / iron / tungsten) = the element body: a lying ``cylinder()`` in the
  frozen material ramp (``VCOPPER`` / ``IRON`` / ``TUNGSTEN``, via ``kit.material_of``), with the
  coil turns as single darker columns (one step darker than the band they cross: ramp[2] on
  the lit/face rows, ramp[1] on the shaded row).  Nothing else on the icon is in the material.
* **tier** (50 V = LV, 200 V = MV; manifest ``tier_cue`` = 'stub') = the two terminal leads, one
  at each end of the body: kit ``tier_stub()`` (4 px thick, own outline, ramp[4] lit row over a
  ramp[3] row).  They are the only hue besides the material and run out of the slot
  (``tip=None``: like the primitive example, the lead leaves the item, so it has no tinned end).
  Jacket visible: 3 px each side (normal), 4 px each side (small).
* **size** (``small_`` prefix) = geometry only: the same body at a smaller diameter and length,
  centred and on the baseline.  Normal: body 10 long x 8 across (cols 3..12, rows 6..13), the
  elongated tube of the STYLE 8 primitive (a square 10 x 10 drum read as a spool / capacitor
  can).  Small: body 8 x 6 (cols 4..11, rows 8..13), inside ``SMALL_BOX`` (cols 3..12, rows
  5..14): the same tube scaled ~0.8 in both axes, a clear step down at GUI 2 that keeps three
  coil turns.  (A 6 long x 8 across small body was tried: it stands prouder of the leads and
  lines them up with the normal size, but with two turns and the full diameter it reads as a
  bead / spool and is not the same drawing scaled down, so it was not used.)  Leads sit on
  rows 8..11 (normal) and 9..12 (small): a 1 px offset.
  The leads are protrusions: they keep their full 4 px thickness and reach the slot edge in
  both sizes, so the tier cue never shrinks below a readable area (STYLE 7: jacket >= 4 px
  long, >= 3 px shown).

The 1.12 icons' "heat dots" (2/4 dots for 50/200 V) are dropped: their reds and oranges were a
second tier code in hues that collide with HV/VHV, and STYLE 7 allows one hue for the tier.
"""
from kit import Canvas, cylinder, tier_stub, CYL_BANDS, material_of, is_small
import kit

#: (body x, body y, length, diameter) per size; leads are centred on the body's axis
GEOM = {
    False: (3, 6, 10, 8),     # normal
    True: (4, 8, 8, 6),       # small_
}


def _groove(cv, x, y, d, mat):
    """One coil turn: a single column one step darker than the cylinder band it crosses."""
    for j, k in enumerate(CYL_BANDS[d]):
        k = 5 if k == 'H' else k
        if k in (0, 1):
            continue                     # keep the outline rows intact
        cv.set(x, y + j, mat[1] if k <= 2 else mat[2])


def heating_element(metal, tier, small=False):
    """``metal``: a §5.3 ramp (VCOPPER / IRON / TUNGSTEN); ``tier``: 'lv' | 'mv'."""
    cv = Canvas(16)
    x, y, length, d = GEOM[bool(small)]
    sy = y + (d - 4) // 2                                   # 4 px lead centred on the axis
    # leads first: the body's end outline is drawn over their inner end
    tier_stub(cv, 0, sy, x + 1, tier, 'right', tip=None)
    tier_stub(cv, 15, sy, 16 - (x + length - 1), tier, 'left', tip=None)
    cylinder(cv, x, y, length, d, metal)
    for gx in range(x + 2, x + length - 1, 2):              # coil turns, every other column
        _groove(cv, gx, y, d, metal)
    return cv


def icon(item_id, tier):
    """The production icon for ``item_id`` (material and size read from the id; ``tier`` is the
    manifest tier, passed explicitly by each module)."""
    metal = getattr(kit, material_of(item_id))
    return heating_element(metal, tier, small=is_small(item_id))
