"""improved_flashlight_on - the ON sprite of improved_flashlight (not a registry id; built by extra/build_extra.py).

items/improved_flashlight.json switches on the client condition eln:lit; on_true shows this sprite.
It is families.tools.flashlight with lit=True: the same drawing as icons/improved_flashlight.py (OFF)
except the lens glows LIT and a short LIT cone leaves it.  Quality 2: the GOLD collar of QUALITY_TRIM[quality('improved_flashlight')].
"""
from kit import QUALITY_TRIM, quality
from families.tools import flashlight


def draw():
    return flashlight(QUALITY_TRIM[quality('improved_flashlight')], lit=True)
