"""Mechanical style rules, enforced by build.py on every icon (see STYLE.md "Hard rules").

``lint(im, expected=16, skip=())`` returns a list of (level, message); level is 'ERROR' or 'WARN'.
ERRORs are things the pack must never ship (wrong size, partial alpha, empty icon).
WARNs are style rules an artist may break only with a reason noted in the module docstring;
a module silences one with ``LINT_SKIP = {'baseline'}`` (names: colours, floor, edge, cover,
baseline, width) - and says why in its docstring.
"""
from .color import lstar, L_FLOOR, hexs

MAX_COLOURS = {16: 16, 32: 24}
#: Pixels allowed on the outer ring (row/col 0 and n-1): protrusions only (pins, tips, stubs).
EDGE_BUDGET = {16: 12, 32: 24}


def lint(im, expected=16, skip=()):
    out = []
    im = im.convert('RGBA') if im.mode != 'RGBA' else im
    w, h = im.size
    if w != h or w not in (16, 32):
        return [('ERROR', f'size {w}x{h}; icons are 16x16 or 32x32')]
    if expected is not None and w != expected:
        out.append(('ERROR', f'size {w}x{w} but this item is vanilla_size={expected} '
                             '(manifest / kit.decisions; never the artist\'s choice)'))
    data = list(im.get_flattened_data() if hasattr(im, 'get_flattened_data') else im.getdata())
    alphas = {p[3] for p in data}
    if alphas - {0, 255}:
        out.append(('ERROR', f'partial alpha {sorted(alphas - {0, 255})[:5]}; only 0/255 allowed'))
    opaque = [p for p in data if p[3] == 255]
    if not opaque:
        return out + [('ERROR', 'icon is empty')]
    cols = {p[:3] for p in opaque}
    if len(cols) > MAX_COLOURS[w] and 'colours' not in skip:
        out.append(('WARN', f'{len(cols)} colours (> {MAX_COLOURS[w]})'))
    lo = min(opaque, key=lstar)
    if lstar(lo) < L_FLOOR - 0.05 and 'floor' not in skip:
        out.append(('WARN', f'darkest pixel {hexs(lo)} L*{lstar(lo):.1f} < floor {L_FLOOR}'))
    edge = sum(1 for y in range(h) for x in range(w)
               if (x in (0, w - 1) or y in (0, h - 1)) and data[y * w + x][3])
    if edge > EDGE_BUDGET[w] and 'edge' not in skip:
        out.append(('WARN', f'{edge} px on the outer ring (protrusions only, budget {EDGE_BUDGET[w]})'))
    cover = len(opaque) / (w * h)
    if cover < 0.12 and 'cover' not in skip:
        out.append(('WARN', f'covers only {cover:.0%} of the canvas; icons should fill the body box'))
    u = w // 16
    xs = [i % w for i, p in enumerate(data) if p[3]]
    ys = [i // w for i, p in enumerate(data) if p[3] and i // w < h - u]   # ignore row 15 protrusions
    base = 15 * u - 1                                                       # row 14 / 29
    if ys and max(ys) < base - u and 'baseline' not in skip:
        out.append(('WARN', f'lowest body row is {max(ys)}; sit the object on row {base} '
                            f'(baseline) - icons do not float'))
    if max(xs) - min(xs) + 1 < 10 * u and 'width' not in skip:
        out.append(('WARN', f'only {max(xs) - min(xs) + 1} px wide; fill the body box '
                            f'(at least {10 * u}) - small devices are drawn big'))
    return out
