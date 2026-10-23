"""Every shared ramp of the vanilla set, as frozen hex values.  Read-only for family artists.

Ramp convention (all ramps): index 0 = darkest (bottom/right outline) .. 4 = lit top face,
optional 5 = bevel / specular.  Box faces: top = [4], front = [3], right side = [2];
outline top/left = [1], bottom/right = [0].

Every ramp is a **tuple** and ``TIER`` is a read-only mapping: build.py renders each icon in its
own process anyway, but a module that tries ``STEEL[3] = ...`` now fails loudly instead of
recolouring other icons.  Need a variant?  Build a new tuple: ``MY = STEEL[:3] + (h('#..'),) + STEEL[4:]``.

Hue shift: darker steps drift towards blue/violet (cool), lighter steps towards yellow (warm),
roughly 4-8 HSV-degrees per step (the near-neutral materials keep one hue).  Floor: no pixel
below CIE L* 12 (``kit.color.L_FLOOR``).

Material ramps exist for every material that recurs across families (ingots, dusts, plates,
heating elements, wires, cores, fuses, filaments, lamps, cells, poles).  Use them; do not derive
your own "lead" or "tungsten".  A material that is genuinely new: ``derive_ramp``, freeze the hex in
``families/<family>.py`` and report it so it can be promoted here.

``python3 -m kit`` prints every ramp with L*/hue; ``python3 -m kit --cvd`` prints the tier
separation under colour-vision deficiency and each material's distance to the nearest tier.
"""
from types import MappingProxyType

from .color import h


def _r(*hexes):
    return tuple(h(x) for x in hexes)


# ------------------------------------------------------------------ voltage tiers
#: The mod's own voltage colours (VoltageLevelColor.setGLColor), = ramp[4] of each tier ramp.
TIER_HEX = MappingProxyType({'signal': '#ccded1', 'lv': '#8cd6ad', 'mv': '#8cbdd9', 'hv': '#f5cc8f',
                             'vhv': '#db948c'})

#: Tier ramps.  ramp[4] is exactly the contract colour.  ramp[0..3] were set in CIE LCh so the
#: five tiers form a LIGHTNESS ladder on the steps a plate front or a cable's shaded half uses:
#:     ramp[3] L*:  signal 76 > HV 72 > LV 61 > MV 46 > VHV 40
#:     ramp[2] L*:  signal 59 > HV 52 > LV 44 > MV 31 > VHV 29
#: so they stay apart (dE >= 15 on ramp[2] and ramp[3], every pair) for protan, deutan and
#: tritan viewers too, not only by hue.  Check: ``python3 -m kit --cvd``.  LV is green, MV a
#: deep sky-to-royal blue, HV amber, VHV brick red, signal a pale grey-mint.
TIER = MappingProxyType({
    'signal': _r('#212f2d', '#3e554f', '#739686', '#9fc4ae', '#ccded1', '#edf6ec'),
    'lv':     _r('#063029', '#02513f', '#1a7656', '#40a575', '#8cd6ad', '#cbeed2'),
    'mv':     _r('#18203e', '#18355e', '#0c4c78', '#2473a0', '#8cbdd9', '#c6e1ed'),
    'hv':     _r('#442413', '#77431f', '#b06c28', '#e5a44c', '#f5cc8f', '#feecc9'),
    'vhv':    _r('#391a20', '#582028', '#7b272d', '#a63836', '#db948c', '#edc3b5'),
})
TIERS = ('signal', 'lv', 'mv', 'hv', 'vhv')

#: Thermal (textures/voltages/thermal.png = #424242 at ramp[3]): a neutral charcoal with a faint
#: warm lift in the lights.  Thermal cables, dissipators, heat sinks.  On a dark background it
#: is close to CAV/CHIP: give thermal parts their lit rim (ramp[4]/[5]) so they keep an edge.
THERMAL = _r('#232226', '#2e2d30', '#363537', '#424242', '#5c5a58', '#7a7672')

# ------------------------------------------------------------------ neutral device materials
#: Device housings, frames, brackets, pins, screws: the default "metal" of every placeable.
STEEL = _r('#24232e', '#4c4b58', '#7b7c86', '#aeafb0', '#dcdcd0')
#: Plain grey insulation / plastic (sprites/cable.png): plastic housings, untiered cable jackets.
CABLE = _r('#25242b', '#4e4d56', '#83848c', '#a9aaad', '#c6c7c2')
#: Dark interior: windows, troughs, slots, sockets, screens off.  Never used as an outline.
CAV = _r('#202129', '#262730', '#383a43', '#5a5c66', '#8a8b94')
#: Logic-chip charcoal (logicgates.png).  Note [2] < [3] < [4]: side, face, top of a flat package.
#: Only through ``chip()`` / ``chip_body()``.
CHIP = _r('#212229', '#4a4c58', '#2a2b33', '#34363f', '#454752', '#6a6c78')
#: Analog-chip navy (analogchips.png body #12233a).  Only through ``chip()``.
ACHIP = _r('#1a2133', '#36465e', '#1a2638', '#223249', '#30435e', '#557090')

# ------------------------------------------------------------------ vanilla metals (exact)
#: Sampled from the 26.3 client's iron_ingot.png: iron_dust / iron_plate / iron_cable /
#: iron heating elements must sit next to vanilla iron without a seam.
IRON = _r('#353535', '#5e5e5e', '#828282', '#a8a8a8', '#d8d8d8', '#ffffff')
#: 26.3 gold_ingot.png: gold_dust, gold_plate, quality trim (STYLE section 7b).
GOLD = _r('#752802', '#b26411', '#dc9613', '#e9b115', '#fdf55f', '#fffde0')
#: 26.3 copper_ingot.png (salmon): copper_dust, copper_plate, copper_cable, copper heating elements.
VCOPPER = _r('#6d3421', '#9c4529', '#c15a36', '#e77c56', '#fc9982', '#fbc3b6')
#: Orange "wire" copper: thin conductors, coil windings, bare wire inside devices.  Not for the
#: copper_* materials (those are VCOPPER) and not for cable ends (tinned = STEEL).
COPPER = _r('#3d1510', '#76291a', '#ad4a24', '#d9773a', '#f6b46a', '#ffe2ac')

# ------------------------------------------------------------------ ELN materials
LEAD = _r('#2a3137', '#444c54', '#5f6973', '#7f8a94', '#a3acb5', '#c6cdd3')       # dull blue-grey
TUNGSTEN = _r('#2e2620', '#473b33', '#62554a', '#807267', '#a19389', '#c5b9b1')   # warm grey-brown
SILICON = _r('#1f2334', '#2b3249', '#3e4662', '#595f7d', '#7d829e', '#b2b5c7')    # glossy blue-violet
ALLOY = _r('#302f3b', '#4b4a5b', '#6a687e', '#8e8ca3', '#b3b1c4', '#dad9e4')      # lavender steel
FERRITE = _r('#1e2528', '#293236', '#364248', '#49545a', '#626c72', '#858d91')    # matte slate
CARBON = _r('#1f2021', '#272829', '#313335', '#414245', '#55575a', '#75777a')     # coal, graphite, filaments
CINNABAR = _r('#451a1e', '#6c262d', '#993340', '#c14e5a', '#dd787e', '#edabac')   # crimson ore
RUBBER_BLACK = _r('#1e2022', '#282a2e', '#35373b', '#47494e', '#5e6166', '#7f8184')  # rubber, black plastic
WOOD = _r('#2e220e', '#493615', '#684e1e', '#896727', '#a98638', '#c9a65a')       # vanilla stick hues
#: Dark reddish-brown creosoted pole wood (powerpole texture #62380b / #472c0f): the utility poles.
#: derive_ramp('#6a3e10', at=3), bevel warmed by hand.  Not for handles (those are WOOD).
POLE_WOOD = _r('#39190c', '#502003', '#5a2900', '#6a3e10', '#7e5622', '#93693a')
#: Amber resistor ceramic (model texture #e49a00 / #a06300): power resistor, rheostat, thermistor
#: bodies.  derive_ramp('#d4922c', at=3); every face step is dE >= 19.5 from every WOOD step.
#: Close to HV[3] (dE 9): untiered parts only, never beside an HV cue.
CERAMIC = _r('#451c00', '#713700', '#995800', '#d4922c', '#ebb445', '#f4d99a')
#: Untiered terracotta / copper-coloured housing (groundcable texture #ab4f14): the ground cable
#: plate.  Tuned against HV and VHV on the pairs a box shows (front [3] vs HV/VHV [2] and [3],
#: side [2] vs HV[2] and VHV[2]/[3], top [4] vs HV/VHV [3] and [4]): dE >= 10.5 in normal,
#: protan, deutan and tritan vision (COPPER, used before, was 5.5).  A terracotta lies between
#: HV and VHV in lightness, so 15 is out of reach: a housing for UNTIERED devices only, never
#: next to an HV or VHV cue.
TERRACOTTA = _r('#4b1611', '#6e1705', '#872800', '#9c4913', '#a67448', '#bb9067')
#: Clear glass (bulb envelopes when drawn unlit, windows, tubes): cool near-white.
GLASS = _r('#587176', '#809ba0', '#a7c0c5', '#cadee1', '#e8f3f5', '#f8fdfe')
#: Warm emissive light: lit filaments, lit bulbs, lamp lenses, LEDs on.  Lamps are drawn LIT.
LIT = _r('#7a4a1c', '#b8792e', '#eeb54a', '#ffda70', '#fff1ad', '#fffbe8')
#: Photovoltaic cells: dark violet-navy, kept clear of the MV blue.  Grid lines in SOLAR[4].
SOLAR = _r('#221f35', '#2f2a4a', '#3f3860', '#524b74', '#6d668a', '#9893ab')

# ------------------------------------------------------------------ part-specific ramps
RUBBER_Y = _r('#4a2a08', '#8a5a10', '#d49a1c', '#f2c43a', '#fbe58a')               # multimeter body
CAP_BLUE = _r('#16233a', '#1b3352', '#22496e', '#336a94', '#5592ba', '#94c8e0')    # capacitor can
LCD = _r('#2c3a12', '#7e9a2a', '#b4cf4a', '#d6ea7a')                              # screen: bg..lit seg
LEAD_RED = _r('#6e1a14', '#b8352b', '#e8705a')       # probe lead: dark, body, lit
LEAD_BLK = _r('#202128', '#33353d', '#666975')       # probe lead: dark, body, lit
#: White plastic (plc cabinet, white sixnodes, white switch modules): the IRON steps with the
#: face mapping shifted up one step - outline dark, outline light, side, front, top = IRON[1..5].
#: Five steps, no bevel; front IRON[4] = the plc texture's ~#e0e0e0.
WHITE_PLASTIC = _r('#5e5e5e', '#828282', '#a8a8a8', '#d8d8d8', '#ffffff')

# ------------------------------------------------------------------ single colours
MARK_RED = h('#c45a52')     # chip face: input mark (left)
MARK_BLUE = h('#5a7cc4')    # chip face: top mark
MARK_GREEN = h('#5ab85a')   # chip face: bottom mark
WHITE = h('#ececec')        # chip output square, LEDs off-white
GLYPH = h('#c8c9cc')        # logic chip glyph (texture #929292 on #222, lifted for GUI 2)
GLYPH_A = h('#a9c0d6')      # analog chip glyph (texture #8ea6c0 on #12233a)
GLYPH_DIM = h('#87888e')    # logic chip second ink, chip-face key 'a' (texture #787878: PAL's A)
GLYPH_A_DIM = h('#71869c')  # analog chip second ink, chip-face key 'a' (texture #637280: S&H tick)

#: Material ramps, for the STYLE table and the ``--cvd`` report.
MATERIALS = ('IRON', 'GOLD', 'VCOPPER', 'COPPER', 'LEAD', 'TUNGSTEN', 'SILICON', 'ALLOY', 'FERRITE',
             'CARBON', 'CINNABAR', 'RUBBER_BLACK', 'WOOD', 'POLE_WOOD', 'CERAMIC', 'TERRACOTTA',
             'WHITE_PLASTIC', 'GLASS', 'LIT', 'SOLAR')


def ramp_names():
    """All named ramps in this module, {name: tuple}, tiers as TIER_<t> (for reports)."""
    out = {k: v for k, v in globals().items()
           if k.isupper() and isinstance(v, tuple) and v and isinstance(v[0], tuple)}
    out.update({'TIER_' + k: v for k, v in TIER.items()})
    return out
