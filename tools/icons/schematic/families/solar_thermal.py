"""solar_thermal family helpers (manifest families `solar` and `thermal`).

The family's shared visual system (STYLE.md sections 5.1, 6, 7, 8.6):
- Solar panels (tiered LV/MV): the photodiode in the tier hue (the PV cell is the defining element), INK
  arrows / marks. Fixed small panel = the full photodiode with incident-light arrows; the 2x3 panel = mark
  '2' + the diode on MID_M; rotating panels (both sizes) = the sun mark ('follows the sun') + the diode on
  MID_M. Size siblings of the rotating panel differ by tier colour only (LV small, MV 2x3), as the table
  allows ("adds nothing else").
- Heat sources (thermal furnaces, tier thermal): the THERMAL heater (IEC resistor) on MID_M, two INK heat
  waves at HEAT_TOP (mandatory) and the fuel as the mark (flame = solid fuel, drop = liquid fuel).
- Heat sinks (dissipators): a flat THERMAL fin comb, fins pointing up (heat rises off them): 2px fins
  on a 2px base rail, nothing else (no block, no second rail, no shading) so it stays a glyph, not the
  in-world finned block. The manifest offers 'heat-sink fins' as the symbol for both kinds.
    passive (tier thermal): three full-height fins, each centred under one of the three INK heat waves at
      HEAT_TOP (heat rises off each fin); the comb shares the waves' axis (14.5), as the kit waves do.
    active (tier lv / mv: the fan runs on a 50V / 200V cable): 'top' layout. A short four-fin comb across
      the top (fins rows 4..6, rail rows 7-8), three clear rows, then the IEC motor ring 'M' in the tier
      hue (D=16, rows 12..27, centred on MID_M) with INK supply leads. The comb and the motor are two
      separate parts (no 1px coupling). The motor (tier hue) is the primary, electrical element; the
      comb is the thermal element in THERMAL (STYLE 5.1: the one case with two hues). The 50V and 200V
      siblings are the same program with a different TIER.
"""
from kit import (THERMAL, MID_M, HEAT_TOP, mark, photodiode, diode, resistor, heat_waves, machine,
                 sensor_diode)

COMB_FIN_X = (6, 12)           # left fins (cols 6-7, 12-13); mirror_x gives 18-19 and 24-25


def comb(g, c, base_y, fin_top, x0=4):
    """Flat heat-sink comb: a 2px base rail on rows base_y, base_y+1 from col x0 to its mirror, with
    four 2px fins rising from it to row fin_top (4 px between fins, so 2 clear px survive the halo).
    The left half is drawn, then mirrored (exact symmetry about 15.5)."""
    g.hline(x0, 15, base_y, c)
    for x in COMB_FIN_X:
        g.vline(x, fin_top, base_y - 1, c)
    g.mirror_x(only=lambda x, y: x <= 15 and fin_top <= y <= base_y + 1)


# ---- solar -------------------------------------------------------------------------------------
def solar_panel(g, large=False):
    """Fixed panels: the photodiode (small) or mark '2' + the diode on MID_M (2x3), STYLE 8.6."""
    if large:
        mark(g, '2')
        diode(g, g.hue, x0=6, w=10, h=10, y=MID_M)
    else:
        photodiode(g, g.hue)


def rotating_panel(g):
    """Rotating panels (both sizes): the sun mark (follows the sun) + the diode on MID_M, STYLE 8.6."""
    sensor_diode(g, 'daylight')


# ---- thermal -----------------------------------------------------------------------------------
def heat_furnace(g, fuel):
    """Thermal furnaces: two INK heat waves (mandatory), the fuel mark ('fire' = flame for solid fuel,
    'fuel' = drop for liquid fuel) and the THERMAL heater on MID_M, STYLE 8.6."""
    heat_waves(g, *HEAT_TOP, n=2)
    mark(g, fuel)
    resistor(g, g.hue, y=MID_M)


def passive_dissipator(g):
    """Passive heat sink: three INK heat waves (HEAT_TOP: cols 6..23, rows 4..10) rising off three
    THERMAL fins (cols 7-8, 14-15, 21-22, one under each wave, rows 14..25) on a base rail (cols 5..24,
    rows 26-27). 5 clear px between fins, 3 clear rows under the waves."""
    heat_waves(g, *HEAT_TOP)
    x0, pitch = HEAT_TOP[0] + 1, 7                 # fin under the middle of each 4-col wave
    g.hline(x0 - 2, x0 + 2 * pitch + 3, 26, g.hue)
    for k in range(3):
        g.vline(x0 + k * pitch, 14, 25, g.hue)


def active_dissipator(g):
    """Fan-cooled heat sink: a short THERMAL four-fin comb on top (fins rows 4..6, rail rows 7-8,
    cols 4..27), rows 9..11 clear, then the motor ring 'M' in the tier hue (D=16, cols 8..23,
    rows 12..27, centred on MID_M) with INK supply leads on rows 19-20."""
    comb(g, THERMAL, 7, 4)
    machine(g, 'M', g.hue, ring=(8, 12, 16))
