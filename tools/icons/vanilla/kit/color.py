"""Colour maths for the vanilla kit: hex parsing, CIE L*/Lab/LCh, ramp derivation, checks.

Colours are plain RGBA tuples ``(r, g, b, 255)``.  ``T`` is fully transparent.
Nothing here draws; see ``kit.canvas`` for that.
"""
import colorsys
import math

T = (0, 0, 0, 0)

#: Darkest allowed pixel (CIE L*).  Checked by build.py on every icon.
L_FLOOR = 12.0


def h(s):
    """``h('#4c4b58') -> (76, 75, 88, 255)``.  Accepts 'rrggbb' or '#rrggbb'."""
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def hexs(c):
    """RGBA tuple -> '#rrggbb'."""
    return '#%02x%02x%02x' % tuple(c[:3])


# --- CIE Lab (D65) ---------------------------------------------------------------
def _lin(v):
    v /= 255.0
    return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4


def _gam(v):
    v = 12.92 * v if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055
    return v * 255.0


_WX, _WY, _WZ = 0.95047, 1.0, 1.08883


def to_lab(c):
    r, g, b = (_lin(v) for v in c[:3])
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / _WX
    y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / _WY
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / _WZ

    def f(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(x), f(y), f(z)
    return (116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))


def from_lab(L, a, b, clip=True):
    """Lab -> RGBA.  Returns None if out of sRGB gamut and ``clip`` is False."""
    fy = (L + 16) / 116
    fx, fz = fy + a / 500, fy - b / 200

    def fi(t):
        return t ** 3 if t ** 3 > 0.008856 else (t - 16 / 116) / 7.787
    x, y, z = fi(fx) * _WX, fi(fy) * _WY, fi(fz) * _WZ
    r = 3.2406 * x - 1.5372 * y - 0.4986 * z
    g = -0.9689 * x + 1.8758 * y + 0.0415 * z
    bb = 0.0557 * x - 0.2040 * y + 1.0570 * z
    out = [_gam(v) for v in (r, g, bb)]
    if not clip and any(v < -0.5 or v > 255.5 for v in out):
        return None
    return tuple(max(0, min(255, int(round(v)))) for v in out) + (255,)


def to_lch(c):
    L, a, b = to_lab(c)
    return L, math.hypot(a, b), math.degrees(math.atan2(b, a)) % 360


def from_lch(L, C, H):
    """LCh -> RGBA; chroma is reduced until the colour is inside sRGB."""
    while C > 0:
        c = from_lab(L, C * math.cos(math.radians(H)), C * math.sin(math.radians(H)), clip=False)
        if c is not None:
            return c
        C -= 0.5
    return from_lab(L, 0, 0)


def lstar(c):
    """CIE L* (0..100) of an RGBA colour."""
    return to_lab(c)[0]


def hue(c):
    """HSV hue in degrees (the unit used for the 'hue shift per step' rule)."""
    return colorsys.rgb_to_hsv(*(v / 255 for v in c[:3]))[0] * 360


def mix(a, b, t):
    """Linear RGB mix of two colours, t=0 -> a, t=1 -> b (no gamma games; for quick tints)."""
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def _rot(h0, target, amount):
    """Rotate LCh hue h0 by up to `amount` degrees towards `target` without passing it.

    The direction avoids the long way through the wrong colours: lights of blues/greens go
    via cyan towards yellow, lights of reds/magentas via orange; darks of greens/cyans go via
    blue towards violet, darks of reds/oranges/yellows via red/magenta."""
    if target == WARM_H:
        sign = -1 if WARM_H <= h0 <= 300 else 1
    else:
        sign = 1 if 110 <= h0 <= COOL_H else -1
    d = (sign * (target - h0)) % 360          # remaining distance along the chosen direction
    return (h0 + sign * min(d, abs(amount))) % 360


#: LCh hue the dark steps drift towards (blue-violet) and the light steps towards (yellow).
COOL_H, WARM_H = 290.0, 95.0


def derive_ramp(anchor, at=4, L=(16, 30, 44, None, None, None), chroma=(0.55, 0.9, 1.25, 1.25, 1.0, 0.55),
                shift=7.0, bevel_dL=10.0):
    """Derive a 6-step ramp (index 0..4 + bevel 5) from one anchor colour.

    The anchor lands exactly at index ``at`` (4 = lit top face, as in the tier ramps; use 3 when
    the colour you sampled is a front face).  ``L`` gives L* targets per step; None means:
    step 0 -> 16, steps below the anchor -> evenly between their known neighbours, steps above
    -> +11 L* per step (+``bevel_dL`` for the bevel when at=4).  Darker steps rotate ``shift``
    LCh-degrees per step towards blue-violet, lighter steps towards yellow (see ``_rot``).
    ``chroma`` scales the anchor's chroma per step (mid-tones more saturated, ends less).
    The tier ramps in ``kit.palette`` were produced this way and then hand-nudged; the frozen
    hex values there are authoritative.  Always hand-check a derived ramp at GUI 2.
    """
    La, Ca, Ha = to_lch(anchor)
    Ls = list(L)
    Ls[at] = La
    if Ls[0] is None:
        Ls[0] = 16.0
    for i in range(at + 1, 6):               # above the anchor: +11 L* per step unless given
        if Ls[i] is None:
            Ls[i] = min(97.0, La + (bevel_dL if (at == 4 and i == 5) else 11.0 * (i - at)))
    for i in range(1, at):                   # below: halfway between the known neighbours
        if Ls[i] is None:
            lo = max(j for j in range(i) if Ls[j] is not None)
            hi = min(j for j in range(i + 1, 6) if Ls[j] is not None)
            Ls[i] = Ls[lo] + (Ls[hi] - Ls[lo]) * (i - lo) / (hi - lo)
    out = []
    for i in range(6):
        if i == at:
            out.append(anchor)
            continue
        Hi = _rot(Ha, COOL_H if i < at else WARM_H, shift * abs(at - i))
        out.append(from_lch(Ls[i], Ca * chroma[i], Hi))
    return out


def ramp_report(name, ramp):
    """One line per ramp: hex, L*, HSV hue of every step (for STYLE.md tables and debugging)."""
    return f'{name:12s} ' + '  '.join(f'{hexs(c)} L{lstar(c):4.1f} h{hue(c):3.0f}' for c in ramp)
