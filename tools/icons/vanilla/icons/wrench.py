"""wrench - handheld-tool family (families/tools.py, 'diagonal tools').

A silver open-end spanner in stamped STEEL (flat fill: face ramp[3], lit bevel top-left, shade
bottom-right): diagonal handle from a rounded tail at the bottom-left to the round head at
the top-right, whose jaw slot runs along the handle axis and opens to the top-right.
"""
from families.tools import wrench


def draw():
    return wrench()
