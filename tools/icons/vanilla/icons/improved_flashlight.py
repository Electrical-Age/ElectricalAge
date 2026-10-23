"""improved_flashlight - handheld-tool family (families/tools.py, 'diagonal tools').

The small flashlight's drawing, OFF (this PNG is the item's off model, items/
improved_flashlight.json on_false; the lit drawing is for improved_flashlight_on), with the
quality-2 trim of the STYLE 7b ladder taken from QUALITY_TRIM[quality(id)] (GOLD): a collar
ring at the grip/head shoulder, clear of the lens.
"""
from kit import QUALITY_TRIM, quality
from families.tools import flashlight


def draw():
    return flashlight(QUALITY_TRIM[quality('improved_flashlight')], lit=False)
