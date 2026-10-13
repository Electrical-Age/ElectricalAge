"""small_200v_copper_heating_corp - heating element: VCOPPER coil body (material), MV terminal leads
(tier_cue 'stub'), small body (8 x 6).  See families/heating_elements.py."""
from families.heating_elements import icon


def draw():
    return icon('small_200v_copper_heating_corp', 'mv')
