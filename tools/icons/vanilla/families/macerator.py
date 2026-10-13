"""Reference family helper: the macerator (maceratora/maceratorb.obj), at 16 and 32 px.

Model: a slab-shaped frame (16 wide, 15 tall, 8 deep) - two end posts rising above a beam
that carries the roller trough, a see-through window under it, a sill and feet.
The front face (the model's rx view) carries identity; depth recedes up-right.

    macerator(tier='lv', size=16) -> Canvas
"""
from kit import Canvas, solid, paint_solid, faces, tier_ramp, CAV, depth


def macerator(tier='lv', size=16):
    return _m16(tier) if size == 16 else _m32(tier)


def _m16(tier):
    B = tier_ramp(tier)
    cv = Canvas(16)
    d, x0, y0, fw, fh, pw = depth(cv, 'block'), 1, 5, 10, 10, 2
    wy0, wy1 = 8, 11
    zb = solid([('lp', x0, y0, pw, fh, d), ('rp', x0 + fw - pw, y0, pw, fh, d),
                ('beam', x0 + pw, y0 + 1, fw - 2 * pw, wy0 - y0 - 1, d),
                ('sill', x0 + pw, wy1 + 1, fw - 2 * pw, 2, d)])
    # chamfered outer top corners: front-left post, back-right post
    paint_solid(cv, zb, B, bevel=False, cut=[(x0, y0), (x0 + fw - 1 + d, y0 - d)])
    # roller trough = the beam's top face, by depth: front roller, gap, back roller, rim
    trough = {1: CAV[4], 2: CAV[0], 3: CAV[3], 4: B[1]}
    for p in faces(zb, 'beam', 'top'):
        cv.set(*p, trough[zb[p][2]])
    cv.hline(x0 + pw, x0 + fw - pw - 1, y0 + 1, B[4])        # front lip of the trough
    for p in faces(zb, 'lp', 'top', 1) + faces(zb, 'rp', 'top', 1):
        if cv.get(*p) == B[4]:
            cv.set(*p, B[5])                                  # end-post tops: bevel row
    wx0, wx1 = x0 + pw, x0 + fw - pw - 1                      # window
    for yy in range(wy0, wy1 + 1):
        for xx in range(wx0, wx1 + 1):
            cv.set(xx, yy, B[1] if xx == wx0 else (CAV[2] if yy == wy1 else CAV[0]))
    return cv


def _m32(tier):
    """Same object at 2x density: geometry = the 16 px layout x2, outline 2 px, then 1 px detail
    the 16 cannot hold - rollers as shaded cylinders (no 1 px teeth: they read as dithering at
    GUI 2), the inner jamb and the sill top
    seen through the window, feet separated by a gap, 2-step chamfers."""
    B = tier_ramp(tier)
    cv = Canvas(32)
    d = depth(cv, 'block')                      # 8
    x0, y0, fw, fh, pw = 2, 10, 20, 20, 4
    wy0, wy1 = 16, 23                           # window rows (front plane)
    zb = solid([('lp', x0, y0, pw, fh, d), ('rp', x0 + fw - pw, y0, pw, fh, d),
                ('beam', x0 + pw, y0 + 2, fw - 2 * pw, wy0 - y0 - 2, d),
                ('sill', x0 + pw, wy1 + 1, fw - 2 * pw, 3, d)])
    xr, yt = x0 + fw - 1 + d, y0 - d            # back top-right corner
    cut = [(x0, y0), (x0 + 1, y0), (x0, y0 + 1), (xr, yt), (xr - 1, yt), (xr, yt + 1)]
    paint_solid(cv, zb, B, bevel=False, cut=cut)
    # trough (beam top face) by depth k = 1..8:
    #   1 lip shadow, 2-3 front roller (lit/dark), 4 gap, 5-6 back roller, 7 gap, 8 rim
    trough = {1: CAV[1], 2: CAV[4], 3: CAV[2], 4: CAV[0], 5: CAV[3], 6: CAV[1], 7: CAV[0], 8: B[1]}
    for p in faces(zb, 'beam', 'top'):
        cv.set(*p, trough[zb[p][2]])
    cv.hline(x0 + pw, x0 + fw - pw - 1, y0 + 2, B[5])         # lit lip of the trough
    cv.hline(x0 + pw, x0 + fw - pw - 1, y0 + 3, B[4])
    for p in faces(zb, 'lp', 'top', 1) + faces(zb, 'rp', 'top', 1):
        if cv.get(*p) == B[4]:
            cv.set(*p, B[5])
    # window: interior dark; inner face of the left post (receding band); sill top inside
    wx0, wx1 = x0 + pw, x0 + fw - pw - 1
    for yy in range(wy0, wy1 + 1):
        for xx in range(wx0, wx1 + 1):
            c = CAV[0]
            if yy == wy0:
                c = CAV[1]                                    # beam underside shadow
            elif yy >= wy1 - 1:
                c = CAV[3] if yy == wy1 else CAV[2]           # sill top seen through the window
            if xx - wx0 < 2:
                c = B[1] if xx == wx0 else B[2]               # inner face of the left post
            cv.set(xx, yy, c)
    # 1 px seams on the front face where the posts meet the beam and the sill
    for yy in list(range(y0 + 2, wy0)) + list(range(wy1 + 1, wy1 + 4)):
        cv.set(x0 + fw - pw, yy, B[2])
    return cv
