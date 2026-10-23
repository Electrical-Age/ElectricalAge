"""small_flashlight_on - the ON sprite of small_flashlight (not a registry id; built by extra/build_extra.py).

items/small_flashlight.json switches on the client condition eln:lit; on_true shows this sprite.
It is families.tools.flashlight with lit=True: the same drawing as icons/small_flashlight.py (OFF)
except the lens glows LIT and a short LIT cone leaves it.  Quality 0 (QUALITY_TRIM[0] = None): no collar.
"""
from kit import QUALITY_TRIM, quality
from families.tools import flashlight


def draw():
    return flashlight(QUALITY_TRIM[quality('small_flashlight')], lit=True)
