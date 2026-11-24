"""pipe_breather_valve - Pipe Breather Valve (VoxelGeometry.safetyValve, breather: green cap), a fitting standing on a
pipe: the safety and purge valves' brass body on a run of iron pipe, under a wide green hood (the vent's rain cap) that
tells it apart from the safety valve's red cap and the purge valve's blue one."""
from kit import *

#: the breather's green (derived from #4caa52, hand-tuned darker at the shade end)
VENT_GREEN = (h('#002e22'), h('#025138'), h('#017847'), h('#049346'), h('#4caa52'), h('#96bc86'))


def draw():
    cv = Canvas(16)
    # the pipe it stands on, across the slot
    cylinder(cv, 1, 11, 14, 4, IRON, axis='x', round_ends=False)
    # the brass body standing on it
    cylinder(cv, 6, 5, 6, 4, GOLD, axis='y', round_ends=False)
    # the hood: a wide cap with a lip turned down at both ends
    cylinder(cv, 3, 2, 10, 4, VENT_GREEN, axis='x', round_ends=False)
    cv.rect(3, 6, 1, 1, VENT_GREEN[1])
    cv.rect(12, 6, 1, 1, VENT_GREEN[0])
    return cv
