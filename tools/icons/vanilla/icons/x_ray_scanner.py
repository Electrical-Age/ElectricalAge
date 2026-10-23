"""x_ray_scanner - handheld-tool family (families/tools.py, 'others').

Its 3D model (xrayscanner.obj, held as a 3D item) seen from above, as the reference sheet's
'top' view: black head carrying the white-framed screen (inside a black margin, the model's
white dot to its right) with a grey ore picture, the button cross with the orange LED left
of the screen, and the narrower grip running out to the left with its yellow stripe.
"""
from families.tools import xray_scanner


def draw():
    return xray_scanner()
