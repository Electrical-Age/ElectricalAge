"""50v_tungsten_heating_corp - heating element: TUNGSTEN coil body (material), LV terminal leads
(tier_cue 'stub'), normal body (10 long x 8 across).  See families/heating_elements.py."""
from families.heating_elements import icon


def draw():
    return icon('50v_tungsten_heating_corp', 'lv')
