"""200v_copper_heating_corp - heating element: VCOPPER coil body (material), MV terminal leads
(tier_cue 'stub'), normal body (10 long x 8 across).  See families/heating_elements.py."""
from families.heating_elements import icon


def draw():
    return icon('200v_copper_heating_corp', 'mv')
