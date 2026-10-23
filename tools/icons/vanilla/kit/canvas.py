"""The drawing surface: a square RGBA pixel grid of 16 or 32 px.

Every icon module builds a ``Canvas`` and returns it (or ``cv.image()``) from ``draw()``.
There is no antialiasing anywhere: a pixel is either a solid palette colour or transparent.

``cv.u`` is the *unit*: 1 on a 16 canvas, 2 on a 32 canvas.  Kit primitives scale their
structural sizes (outline width, oblique depth, pins, margins) by ``u`` so the same call
produces the same look at both sizes; fine interior detail stays 1 px at either size.
"""
from PIL import Image

from .color import T


class Canvas:
    """A ``size`` x ``size`` grid of RGBA tuples.  Coordinates are (x, y), y down, origin top-left.

    Off-canvas writes are silently ignored, so primitives may overhang the edge.
    """

    def __init__(self, size=16):
        if size not in (16, 32):
            raise ValueError('icon canvases are 16 or 32 px')
        self.n = size
        self.u = size // 16
        self.px = [[T] * size for _ in range(size)]

    # --- single pixels ----------------------------------------------------------
    def set(self, x, y, c):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.px[y][x] = c

    def get(self, x, y):
        if 0 <= x < self.n and 0 <= y < self.n:
            return self.px[y][x]
        return T

    def opaque(self, x, y):
        return self.get(x, y)[3] != 0

    def clear(self, x, y):
        self.set(x, y, T)

    # --- areas ------------------------------------------------------------------
    def rect(self, x, y, w, h, c):
        """Fill the rectangle x..x+w-1, y..y+h-1 with colour c (T clears)."""
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c)

    def hline(self, x0, x1, y, c):
        for xx in range(min(x0, x1), max(x0, x1) + 1):
            self.set(xx, y, c)

    def vline(self, x, y0, y1, c):
        for yy in range(min(y0, y1), max(y0, y1) + 1):
            self.set(x, yy, c)

    def pixels(self, pts, c):
        """Set every (x, y) in ``pts`` to ``c``."""
        for (x, y) in pts:
            self.set(x, y, c)

    def overlay(self, grid, pal, ox=0, oy=0, scale=1):
        """Paint a text grid: one character per pixel, looked up in ``pal`` (dict char -> colour).

        '.' and ' ' leave the pixel alone; '_' clears it to transparent.  ``scale=2`` paints each
        character as a 2x2 block (use it to lay a 16-grid on a 32 canvas, then add 1 px detail).
        Unknown characters raise KeyError, which build.py reports with the module name.
        """
        for y, row in enumerate(grid):
            for x, ch in enumerate(row):
                if ch in '. ':
                    continue
                c = T if ch == '_' else pal[ch]
                for dy in range(scale):
                    for dx in range(scale):
                        self.set(ox + x * scale + dx, oy + y * scale + dy, c)

    def paste(self, other, ox=0, oy=0):
        """Copy the opaque pixels of another Canvas (or PIL RGBA image) onto this one."""
        if isinstance(other, Canvas):
            for y in range(other.n):
                for x in range(other.n):
                    c = other.px[y][x]
                    if c[3]:
                        self.set(ox + x, oy + y, c)
        else:
            im = other.convert('RGBA')
            for y in range(im.height):
                for x in range(im.width):
                    c = im.getpixel((x, y))
                    if c[3]:
                        self.set(ox + x, oy + y, c)

    def recolor(self, mapping):
        """Replace colours: ``mapping`` is {old: new}.  Use ``kit.tier.swap_ramp`` for whole ramps."""
        for y in range(self.n):
            for x in range(self.n):
                c = self.px[y][x]
                if c in mapping:
                    self.px[y][x] = mapping[c]

    def region(self):
        """Set of all opaque pixels (the silhouette)."""
        return {(x, y) for y in range(self.n) for x in range(self.n) if self.px[y][x][3]}

    def colours(self):
        return {c for row in self.px for c in row if c[3]}

    def copy(self):
        cv = Canvas(self.n)
        cv.px = [row[:] for row in self.px]
        return cv

    def flip_h(self):
        """Mirror left-right in place.  Careful: light must stay top-left, so re-outline afterwards."""
        for row in self.px:
            row.reverse()
        return self

    # --- output -----------------------------------------------------------------
    def image(self):
        im = Image.new('RGBA', (self.n, self.n))
        im.putdata([c for row in self.px for c in row])
        return im

    def save(self, path):
        im = self.image()
        im.save(path)
        return im

    @classmethod
    def from_image(cls, im):
        im = im.convert('RGBA')
        cv = cls(im.width)
        for y in range(im.height):
            for x in range(im.width):
                c = im.getpixel((x, y))
                cv.px[y][x] = c if c[3] >= 128 else T
                if c[3] >= 128:
                    cv.px[y][x] = c[:3] + (255,)
        return cv
