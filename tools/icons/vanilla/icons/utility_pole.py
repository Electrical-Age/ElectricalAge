"""utility_pole - transparent node (powerpole/utilitypole.obj: main + p0 + g0), front-on (model rx view).

One POLE_WOOD solid: the post and the crossarm, the post rising a pixel above the arm; the two grey
line clamps (p0/g0) under the arm.  No foot: ElectricalPoleDescriptor draws the 'foot' part only
on the DC-DC converter pole.  tier_cue is null (decided centrally), so no tier colour."""
from families.cables import pole


def draw():
    return pole(converter=False)
