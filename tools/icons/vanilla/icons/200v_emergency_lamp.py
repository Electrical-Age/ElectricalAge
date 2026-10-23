"""200v_emergency_lamp - emergencyexitlighting.obj on the MV tier slab (tier cue 'plate'): tall narrow white housing standing on the slab, its front a lit neutral exit sign with a dark up arrow.

Drawn by families/lighting_fixtures (see its docstring for the family system)."""
from families.lighting_fixtures import emergency_lamp


def draw():
    return emergency_lamp('mv')
