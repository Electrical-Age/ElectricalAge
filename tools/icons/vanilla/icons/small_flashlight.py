"""small_flashlight - handheld-tool family (families/tools.py, 'diagonal tools').

Black flashlight on the slot diagonal (grip bottom-left, head top-right), drawn OFF: this PNG
is the item's off model (items/small_flashlight.json: condition eln:lit, on_false =
item/small_flashlight), so the lens is unlit glass with one glint.  The lit drawing
(``flashlight(None, lit=True)``) is for the separate small_flashlight_on sprite.  Quality 0
(QUALITY_TRIM[0] = None): no collar, unlike the improved flashlight's gold one.
"""
from kit import QUALITY_TRIM, quality
from families.tools import flashlight


def draw():
    return flashlight(QUALITY_TRIM[quality('small_flashlight')], lit=False)
