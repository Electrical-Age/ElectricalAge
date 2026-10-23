# Schematic glyph 32px: production style guide

Last updated: 2026-10-12 (revision 3, after wave 1: the mod's own tier palette, thermal approved, wave-1
helpers promoted into the kit: `gate(inputs=3)`, `opamp(inputs=, control=)`, `transformer(x0=, y0=)`, fixed
`breaker()` and `relay()`, `terminal(d=)`, FONT `°`, MARKS `10` `25` `B` `temperature`). Replaces the style-study spec
(the style study's 04-schematic32 spec, not kept). Binding above this file: `../CONTRACT.md`.

Reference images. Regenerate them with the commands in section 14; every cell in them passes `check()`:
- `ref/reference_sheet.png`: the eight reference icons (`icons/`) plus the out-of-scope multimeter (`ref/out_of_scope/`, last), in inventory slots at GUI 2 and 3, the dark row and a zoom.
- `ref/catalog.png`: every kit primitive, layout and pictogram, labelled with the call that drew it.
- `ref/conventions.png`: the **binding family conventions** of sections 8 and 9, one cell per item kind, labelled with the item id. The code is in `kit/conventions.py` and `kit/catalog.py` (`CONVENTIONS`).
- `ref/dodont.png`: the do/don't pairs of section 12.
- `ref/font.png`: FONT and MICRO.

**Before you start (blocker).** `manifest/manifest.json` must exist and give every id `tier`, `family`,
`schematic_scope` (true/false) and `placeable`. The tier is the colour, so never guess it. If your id has
no manifest row yet, stop and ask; `python3 build.py --info <id>` prints the row.

---

## 0. What this set is

This is an optional resource pack. Every in-scope item becomes a flat electrical-schematic glyph (an IEC/IEEE
symbol or a functional pictogram), so the inventory reads like a circuit diagram. A glyph must resemble no
physical device: it has no silhouette, no depth and no shading (contract decision 5).

**Scope.** Draw only items with `schematic_scope = true` in the manifest. That covers everything placeable,
plus electrical components that have a standard symbol (fuses, bulbs, heating elements, portable batteries
and capacitors, motors). Materials, machine parts (chips, cores, magnets, casings, upgrades, filters,
burners), tools, armour, brushes and the creative-tab icon are out of scope: they fall through to the
vanilla art. `build.py` FAILS an icon whose manifest row says `schematic_scope = false`.

---

## 1. Canvas and resolution

| rule | value |
|---|---|
| size | **32x32** RGBA for every icon in this set (`Glyph()` default). No other size. |
| alpha | only 0 or 255. No antialiasing, no partial alpha. |
| background | transparent |
| core box | every **stroke** pixel lies in cols/rows **4..27** (24x24). The halo lands on 3 and 28; rows/cols 0-2 and 29-31 stay empty. |
| centre | the canvas centre line is **15.5**. A centred 2px stroke occupies rows (or cols) **15 and 16** (`MID = 15`). Mirror helpers map `x -> 31 - x`. |

**Why 2px.** The game draws the 32px icon in a 16-GUI-px slot:
- GUI scale 2: 1:1. This is the design target.
- GUI scale 3: 1.5x nearest-neighbour. Every 2px stroke becomes exactly 3 screen px. 1px details become 1 or 2 px, unevenly.
- GUI scale 1: 0.5x. 2px strokes keep at least 1px when halved; 1px strokes can drop out.

---

## 2. Projection and light

There is none. Glyphs are flat and orthographic: no light direction, shading, gradient, highlight, depth,
isometry or drop shadow. The kit has no projection helpers, on purpose.

---

## 3. Stroke and gaps

- **One weight: 2px.** Use `hline` and `vline` (2px by construction), `frame`, `wire`, `line` (any angle;
  near-45° lines get a third pixel so they weigh the same), `circ` (2px ring, even diameter) and `stroke`
  (smooth curves from `bezier()` / `arc_points()`).
- Coordinates are **ints**. A float raises `TypeError` at the call; colours are palette constants (`INK`,
  `g.hue`, `THERMAL`), never `'#hex'` strings (also a `TypeError`).
- Rings: **even diameters only**. Arcs are a `circ` with a `keep` predicate.
- **Symmetry is exact.** Draw half, then `mirror_x()` / `mirror_y()`. Never draw both halves by hand.
- **Solid fills are banned**, except the kit's own filled marks: 2x2 grains and gear teeth, the 4x4
  junction `DOT`, the 6x6 switch `CONTACT`, arrowheads (`ARROW_HEADS`, `DIAG_ARROW`) and the kit
  `PICTOGRAMS`. Never invent a filled shape of your own.
- **1px strokes are banned**, with one exception: the MICRO font (section 10).

### 3.1 The gap rule (enforced)

**gap** = the number of empty pixels between the nearest pixels of two separate parts, counted along a
row, a column or a diagonal (Chebyshev distance - 1). The halo takes one pixel on each side, so:

| gap | what you see | `check()` |
|---|---|---|
| 1 | the halos overlap: the parts fuse into one dark-outlined blob | **ERROR** |
| 2 | the halos touch: separated only by a dark line, reads as one shape at GUI 2 | **ERROR** |
| **>= 3** (`MIN_GAP`) | at least one clear pixel: two parts | ok |

Exceptions, both automatic:
- A part that lies **inside a hole** of another (a letter inside a ring, the X inside a lamp) only needs gap >= 1.
- The pieces of **one mark** are not checked against each other: the letters of a label, the dashes of a
  `dashed()` line, the three `heat_waves`, grains of grit, the earth bars of `ground()`, the arcs of
  `waves()`. They form a `g.group()`; the group as a whole still keeps gap >= 3 from everything else.
  Use `with g.group():` only for a genuinely multi-piece mark, never to silence the check.

The error message names both parts by bounding box. Fix it by moving a part, or by joining the two parts
if they are meant to touch (a wire into a body).

- Inside an outlined shape, keep the hole at least 3px in both directions, or the halo fills it and it
  reads as a dark solid.

---

## 4. Halo

Every glyph gets an opaque, 8-connected, 1px halo in `HALO #18161e`, made by dilating the stroke mask in
`Glyph.render()`. **Never draw the halo yourself**; `check()` rejects HALO as a stroke colour, and a raw
image whose dark outline is not exactly the kit halo fails. The halo carries the glyph on the light slot
grey (#8b8b8b) and on the GUI panel (#c6c6c6); on dark backgrounds it disappears and the light strokes read
on their own.

---

## 5. Colour: hue encodes ONE axis, the voltage tier

### 5.1 The decision

**Coloured means voltage-rated; white means not voltage-rated.** Any hue in an icon is a tier hue (or the
thermal grey). There are no domain colours: no "power yellow", "signal cyan", "machine steel".

| item kind | colour |
|---|---|
| tiered item (manifest `tier` = signal / lv / mv / hv / vhv) | its **defining element** (section 6) in the tier hue; leads, letters, arrows, marks and grit in INK |
| thermal item (`tier = thermal`) | defining element in THERMAL grey, plus the **mandatory** INK heat-wave cue (`heat_waves`: at `HEAT_CABLE` on the cable, at `HEAT_TOP` above a part on `MID_M`, `n=2` when a mark shares the top band). A flame/drop pictogram may be added as a mark, never in place of the waves |
| tiered electrical device that also handles heat (e.g. an active dissipator) | tier hue on the electrical element, THERMAL on the thermal element (the one case with two hues) |
| untiered item (`tier` = null) | **all INK**. The symbol's structure carries the meaning. |

### 5.2 Palette

| name | hex | L* | use |
|---|---|---|---|
| HALO | `#18161e` | 8 | added by `render()` only |
| INK | `#eeeade` | 93 | leads, letters, arrows, marks, grit; the whole glyph when untiered |
| THERMAL (main) | `#767676` | 50 | thermal defining element (owner-approved, with the mandatory heat waves) |

Tier ramps. **Only `main` may appear in an icon** (`check()` enforces this). `main` is **exactly** the
mod's in-world voltage colour (`textures/voltages/{signal,low,medium,high,veryhigh}.png`), the palette
players already know (owner decision, 2026-10-12). `light` is the contract value from
`VoltageLevelColor.setGLColor()`; `dark` is `main` with L* lowered 15-19 at the same hue.

| tier (manifest key) | dark | **main** | light (contract) | L* main | hue family |
|---|---|---|---|---|---|
| `signal` | `#7c9b89` | **`#adcbb9`** | `#ccded1` | 79 | pale mint / sage |
| `lv` (50V) | `#177f4e` | **`#36b776`** | `#8cd6ad` | 66 | green |
| `mv` (200V) | `#086991` | **`#3a93c3`** | `#8cbdd9` | 58 | blue |
| `hv` (800V) | `#be7e10` | **`#f5ab3a`** | `#f5cc8f` | 75 | amber |
| `vhv` (3200V) | `#903328` | **`#cb5040`** | `#db948c` | 50 | red |
| `thermal` | `#424242` | **`#767676`** | `#a8a8a8` | 50 | neutral grey (`thermal.png` = dark) |

Tier spellings: the manifest uses only the canonical keys above (or `null`). The kit also accepts the
shared synonyms of `tools/tiers.py` (`low_voltage`, `50V`, `medium_power`, `3.2kV` ...); bare numbers
(`1`, `5`) are rejected as ambiguous. Always write the canonical key in your `TIER`.

### 5.3 Measurable colour rules (enforced by `Glyph.check()` / `build.py`)

1. Stroke colours ⊆ {INK, the five tier `main`s, THERMAL}.
2. At most **one** tier hue per icon.
3. The module's `TIER` == `Glyph(tier=...)` == the manifest's `tier` (build FAILS otherwise).
4. A tiered or thermal glyph shows its hue on **at least 40 px** (`MIN_HUE_PX`, a 2x20 bar). A thin hued
   stroke on an INK glyph does not carry the tier at GUI 2: colour the whole defining element.
5. An untiered glyph uses no tier hue and no THERMAL.
6. THERMAL, when present, covers at least 40 px.

### 5.4 Colour-vision deficiency (measured, `python3 -m kit.palette --cvd`)

CIEDE2000 distances between stroke colours (Machado simulation, severity 1); below ~10 is hard to tell
apart at 2px:

| vision | closest pairs | vs slot grey #8b8b8b (lowest) |
|---|---|---|
| normal | INK-signal 15.9, signal-LV 18.4, MV-thermal 21.4, VHV-thermal 25.8 | thermal 7.9, MV 19.9 |
| protan (~1% of men) | **INK-signal 8.2**, LV-HV 11.6 (L* 69 vs 71), signal-LV 14.6 | thermal 7.9, MV 18.0 |
| deutan (~5% of men) | **INK-signal 9.5**, LV-VHV 12.5, signal-LV 14.4 | thermal 7.9, LV 14.9 |
| tritan (rare) | **LV-MV 9.3**, signal-LV 16.6 | thermal 7.9, signal 21.2 |

So for red/green colour-blind players the signal hue is close to INK, and for tritanopes LV and MV are
close. MV main (L* 58) has the slot grey's lightness, so it reads by hue and by its halo. The palette is
**final**: the owner keeps the mod's own colours and does not retune for colour blindness (Minecraft 26.3
has no colour-blind mode). These structural rules back it up and are binding:

- **Signal vs untiered.** A symbol used by a signal-tier item is never used, unchanged, by an untiered item
  in scope. Gates, op-amps, waveforms, regulators, sensors and the wireless family are signal-only. Where
  both exist (meters, switches, sources, diodes), the untiered one must differ in structure, not only in
  colour. Example: if the manifest tiers 10a/25a_diode null and signal_diode signal, the untiered
  diodes carry their current mark (10 / 25) and signal_diode does not.
- **LV vs HV.** Never rely on tier alone to tell two items of one family apart when the family has both
  an LV and an HV member *and* they differ in function. Tier siblings (the same device at 50V and 800V)
  are fine: the tooltip carries the voltage.

---

## 6. The defining element (what carries the tier hue)

| kind | defining element (tier hue) | INK |
|---|---|---|
| cables | the whole routed conductor (`cable_route`) | nothing |
| ground cable | — (untiered) | IEC earth (`ground`) |
| thermal cable | the route in THERMAL (`thermal_cable`) | `heat_waves` above the route |
| switches, relays, breakers, buttons | **the whole contact: both leads, both contact dots and the blade** (`switch`, `relay`, `button`, `breaker`) | relay coil and dashed link, button actuator |
| fuses | the frame and element (`fuse`) | leads |
| lamps / bulbs | ring + X (`lamp`, `bulb`) | leads, type mark |
| heating corps | the resistor rectangle (`heating_corp`) | leads, material mark |
| batteries, chargers | the plates (`battery`, `battery_block`) | leads, kind mark |
| capacitors (tiered) | the plates (`capacitor`) | leads, '+' |
| motors, generators, turbines | the ring (`machine`, `generator`) | letter (M / G), input pictogram |
| meters, probes | the ring (`meter`) | quantity letter, leads |
| sensors | the sensing element (diode, resistor, meter ring) | what is sensed (arrows / pictogram), leads |
| process machines | the process element (rollers, plates, ram, coil, heater) | feed arrow, product |
| antennas | mast + V (`transmitter`, `receiver`, `repeater`) | waves / arrows |
| logic gates, analog blocks | the body / waveform / loop: signal tier | leads, signs, labels |
| tiered converters (`energy_converter_*`) | the symbol body | leads |
| converters between variable tiers (`dc_dc_converter`) | — (untiered) | `transformer` all INK |

Items that used to share one texture across tiers get **one icon per tier**: the same program with a
different `TIER`. Never show a tier by anything but colour (no thickness, no slash count).

---

## 7. Layouts

Every icon uses exactly one of three layouts. All the constants are in `kit.symbols`; the kit primitives
already follow them.

| layout | where things go | constants |
|---|---|---|
| **standard** | the symbol alone, centred. Two-terminal parts on rows 15-16, leads out to cols 4 and 27. Rings at `RING` (D=18, cols/rows 7..24; leads 4..6 and 25..27). Small variant: `RING_SMALL` (D=14). | `MID`, `RING`, `RING_SMALL` |
| **mark** | a **mark** (variant letter or input pictogram, section 8) top right in `MARK_BOX` (cols 21..27, rows 4..11), drawn by `mark(g, key)`. The symbol moves down-left: rings at `RING_M` (D=16, cols 4..19, rows 12..27, one lead to the right on rows 19-20), small `RING_M_SMALL`; two-terminal parts on `MID_M` (rows 19-20). A two-glyph mark (Cu, Fe, JK, 10) spans cols 15..27 and only fits over a two-terminal part on `MID_M`. | `MARK_BOX`, `RING_M`, `RING_M_SMALL`, `MID_M` |
| **top** | a full-width INK element on rows 4..10 (heat waves at `HEAT_TOP`, radiation arrows, feed arrow) above a symbol on `MID_M`. | `HEAT_TOP`, `MID_M` |

Further layout rules:
- **Gates**: body cols 9..19, rows 7..24; **three inputs** (the standard: ELN gates have up to 3,
  `LogicGate.kt`) on rows 10-11, 15-16 and 20-21 (`IN_3`); `gate(inputs=2)` keeps rows 12-13 / 18-19;
  NOT/buffer has one input on rows 15-16. Output on rows 15-16; bubble cols 20..25. All gates share this box.
- **Flow is top to bottom.** Process machines take their input from the top (`feed_arrow`).
- **Stack count.** Minecraft draws the stack count over roughly icon cols 12..31, rows 18..31 (one digit:
  cols 24..31). Nothing that alone tells an item apart may sit only there. That is why the mark slot is top
  right, why the thermal cable's heat waves are above the route, and why a process machine is identified by
  its **process element** (middle), not by its product: put the product bottom-left or centred.

### 7.1 Standards

- **IEC 60617** for passives, sources and machines: resistor = empty rectangle; fuse = rectangle with the
  conductor through it; capacitor = two plates; battery = long/short plates (the long plate is +, no sign);
  inductor = humps; lamp = circle + X; voltage source = circle with the conductor through it; current source
  = circle with a perpendicular bar; rotating machine = circle + M/G; transformer = two overlapping circles;
  earth = three bars; antenna = mast + V; adjustable = diagonal arrow through the body; pre-set
  (trimmer) = diagonal ending in a bar; non-linear = slash with a foot; switch = blade between two contacts;
  breaker = switch with a cross on the fixed contact; bell = dome on a bar.
- **IEEE/ANSI distinctive shapes for logic gates** (AND = D, OR = shield, XOR = shield + a separate back
  curve 3 clear px behind it, NOT = triangle + bubble). An IEC gate is a labelled box: unreadable at 32px and
  it looks like the in-world chip (decision 5).

### 7.2 Process machines

**Feed arrow in (INK, top) -> process element (tier hue, middle) -> product out (INK, bottom-left or
centred).** Show what the machine does to the item. The **process element is the distinguishing part**; the
product only completes the story (it may be under the stack count). No motor circle.

| machine | process element |
|---|---|
| macerator | `macerator_rollers()`: two toothed rollers offset diagonally (never side by side: that reads as a face), two grains of grit (reference icon) |
| compressor | two plates (2px bars in the hue) closing on the item from above and below, INK arrows pointing inward |
| plate machine | a press ram (T shape, hue) coming down onto an anvil bar (hue), a thin INK plate out |
| magnetizer | `inductor()` humps wrapped round an INK bar |
| electrical furnace | heater (`resistor`) on `MID_M` with `heat_waves` at `HEAT_TOP` |
| egg incubator | heater as above + an INK egg (8px ring) beside it |
| auto miner | a drill arrow (hue) going down into INK grains |

### 7.3 Sources, generators, turbines, motors

- Generators and turbines: `generator(g, kind)`: 'G' ring at `RING_M` in the tier hue + the input pictogram
  in the mark slot (`fuel` drop, `gas` flame, `steam`, `water`, `wind`). Electric `50v_turbine`/`200v_turbine`
  (heat to electricity): kind `steam`. `generator` (shaft-driven): `machine(g, 'G', g.hue)` with no mark.
- Electrical motor: `machine(g, 'M', g.hue)`; `advanced_electrical_motor`: mark `advanced` ('+') + 'M' ring at `RING_M`.
- Electrical source: `source(g, 'V', g.hue)`. Signal source: `source(g, 'V', g.hue)` in the signal tier.
- Solar panels: `photodiode(g, g.hue)` (the cell in the tier hue); rotating panels and the tracker use
  the sun mark instead of the arrows (section 8.6).

### 7.4 Sensors, meters

See section 8.5 for the binding table. Meters: the quantity as an INK letter in a ring in the tier hue
(`meter`); letters: V voltage, A current, W power, T temperature, n speed, E energy.

### 7.5 Antennas and wireless

`transmitter(g, c)` (two radiation arcs), `receiver(g, c)` (two arrows in), `repeater(g, c)` (one in, one
out). Power tiers come from the antenna colour (low/medium/high power -> lv/mv/hv in the manifest).

---

## 8. Variant marks: the binding tables

When several items share a symbol and a tier, the difference goes in the **mark slot** (top right,
`MARK_BOX`) as a kit mark: `mark(g, key)`. Keys are in `kit.symbols.MARKS`; pictograms in `PICTOGRAMS`.
Marks are always INK. **Do not invent marks**; if a family needs one that is not here, request it.
"small" variants use the small symbol size (`RING_SMALL`, `RING_M_SMALL`, `resistor(small=True)`,
`battery(small=True)`), never a letter. Every row below is drawn in `ref/conventions.png`.

### 8.1 Light bulbs (`bulb(g, kind=None, small=False)`; lamp in the tier hue)

| item ids | call |
|---|---|
| 50v/200v_incandescent_light_bulb | `bulb(g)` (no mark: the plain IEC lamp) |
| 50v_carbon_incandescent_light_bulb | `bulb(g, 'carbon')` -> **C** |
| 50v/200v_economic_light_bulb | `bulb(g, 'economic')` -> **E** |
| 50v/200v_led_bulb | `led(g, g.hue)` (the LED symbol, no lamp) |
| 50v/200v_farming_lamp | `bulb(g, 'farming')` -> **F** |
| 50v/200v_emergency_lamp | `bulb(g, 'emergency')` -> **!** |
| small_50v_incandescent / carbon / economic | `bulb(g, <kind>, small=True)` |

### 8.2 Lamp sockets (`socket(g, kind)`; untiered, all INK)

| item id | call | glyph |
|---|---|---|
| simple_lamp_socket | `socket(g, 'simple')` | the plain lamp with leads |
| suspended_lamp_socket | `socket(g, 'suspended')` | lamp hanging from a short lead from the top |
| long_suspended_lamp_socket | `socket(g, 'long_suspended')` | the same, hanging lower on a longer lead |
| flat_lamp_socket | `socket(g, 'flat')` | ceiling bar across the top, lamp under it |
| sconce_lamp_socket | `socket(g, 'sconce')` | wall bar on the left, arm to the lamp |
| fluorescent_lamp_socket | `socket(g, 'fluorescent')` | mark **E** (as the economic = fluorescent bulb) |
| lamp_socket_a | `socket(g, 'socket_a')` | mark **A** |
| lamp_socket_b_projector | `socket(g, 'socket_b')` | mark **B** |
| robust_lamp_socket | `socket(g, 'robust')` | mark **R** |
| streetlight, lamp_supply | family lead proposes (streetlight: `socket` with a pole: a vline from the bottom edge) |

### 8.3 Heating corps (`heating_corp(g, metal, small=False)`; resistor in the tier hue)

| item ids | mark |
|---|---|
| *_copper_heating_corp | **Cu** |
| *_iron_heating_corp | **Fe** |
| *_tungsten_heating_corp | **W** |
| small_* | the same mark, `small=True` (12x8 resistor) |

50V/200V = LV/MV colour (per the manifest). A tier-coloured plain resistor is reserved for heating corps:
if the manifest tiers power_resistor, it must add a structural difference (request a mark). No heat waves on heating corps: the mark slot needs the space and the colour already
says "rated heater".

### 8.4 Batteries, capacitors, chargers (`battery_block(g, kind=None, cells=1, small=False)`)

| item id | call |
|---|---|
| portable_battery | `battery_block(g)` |
| portable_battery_pack | `battery_block(g, cells=2)` |
| portable_capacitor | `capacitor(g)` |
| portable_capacitor_pack | `capacitor(g, cells=2)` |
| cost_oriented_battery | `battery_block(g, 'cost')` -> **$** |
| capacity_oriented_battery | `battery_block(g, 'capacity')` -> **C** |
| current_oriented_battery | `battery_block(g, 'current')` -> **A** |
| life_oriented_battery | `battery_block(g, 'life')` -> **L** |
| voltage_oriented_battery | `battery_block(g, 'voltage')` -> **V** |
| single_use_battery | `battery_block(g, 'single_use')` -> **1** |
| 50v/200v_battery_charger | `battery_block(g, 'charge')` -> lightning **bolt** |
| weak_50v_battery_charger | `battery_block(g, 'charge', small=True)` |
| power_capacitor | `capacitor(g, polar=True)` (reference icon) |
| power_capacitor_block, power_inductor_block | both in scope: the component's symbol in the mark layout + mark **B**, the accepted block mark for every `*_block` item (`mark(g, 'B')` + `capacitor(g, x=8, h=14, y=MID_M)` / `inductor(g, y=MID_M)`) |

### 8.5 Sensors and probes (signal hue on the sensing element)

| item id | call |
|---|---|
| electrical_light_sensor | `photodiode(g, g.hue)` (arrows in) |
| electrical_daylight_sensor | `sensor_diode(g, 'daylight')` (sun mark) |
| electrical_fire_detector | `sensor_diode(g, 'fire')` (flame mark) |
| electrical_entity_sensor | `sensor_diode(g, 'entity')` (person mark; an IR presence detector) |
| electrical_weather_sensor | `mark(g, 'weather')` + `resistor(g, g.hue, y=MID_M)` (cloud mark; a rain resistor) |
| electrical_anemometer_sensor | `mark(g, 'wind')` + `meter(g, 'n', g.hue, ring=RING_M)` |
| thermometer | `meter(g, 'T', g.hue)` |
| temperature_probe | `mark(g, 'P')` + `meter(g, 'T', g.hue, ring=RING_M)` (probe) |
| thermal_probe | `mark(g, 'H')` + `meter(g, 'T', g.hue, ring=RING_M)` (heat side of a thermal network) |
| voltage_probe | `meter(g, 'V', g.hue)` |
| electrical_probe | `meter(g, 'A', g.hue)` provisionally: the family lead checks in the Java code what it measures and asks before choosing another letter |
| energy_meter / advanced_energy_meter | `meter(g, 'E', g.hue)`; advanced: mark **+** with `ring=RING_M` |
| tachometer | `meter(g, 'n', g.hue)` |
| electrical_timer | `mark(g, 'timer')` + `switch(g, g.hue, mark=True)` (clock mark) |
| electrical_fire_buzzer | `mark(g, 'fire')` + `bell(g, g.hue, y=MID_M + 4)` |

### 8.6 Everything else with variants

| item ids | rule |
|---|---|
| fuses: lead_fuse_for_{low..very_high}_voltage_cables | `fuse(g, g.hue)`, one per tier |
| blown_lead_fuse | `fuse(g, blown=True)`, untiered |
| electrical_fuse_holder | untiered: a shortened INK fuse (`fuse(g, x0=11, x1=20, with_leads=False)`) between two open terminal rings at the lead ends (`terminal(g, 6, MID)`, `terminal(g, 24, MID)`, 1px joins at cols 10 and 21). The rings tell the holder apart from the fuses in structure (`families/power_passive.fuse_holder`) |
| thermistor | `mark(g, 'temperature')` (**t°**, IEC `-t°`) + `resistor(g, y=MID_M)` + `nonlinear_slash(g, 9, 26, 21, 14)` |
| switches / relays: {low..very_high}_voltage_switch / _relay, signal_switch, signal_relay | `switch(g, g.hue)` / `relay(g, g.hue)` |
| signal_button, wireless_button | `button(g, g.hue)`; wireless: + mark **radio** and the mark layout (family lead) |
| signal_cable_switch | accepted: signal_cable's routed step with the make contact cut into the run, all in the signal hue (`families/power_switching.cable_switch`) |
| signal_switch_with_led | `mark(g, 'led')` + `switch(g, g.hue, mark=True)` (emission arrow mark) |
| wireless_switch | `mark(g, 'wireless')` + `switch(g, g.hue, mark=True)` |
| electrical_breaker | `breaker(g, g.hue)`, untiered (all INK): the cross replaces the fixed-contact dot |
| 10a_diode / 25a_diode / signal_diode | `mark(g, '10')` / `mark(g, '25')` (registered MARKS) + `diode(g, y=MID_M, h=10)` INK; signal_diode: `diode(g, g.hue)` |
| solar: small_solar_panel, 2x3_solar_panel | `photodiode(g, g.hue)`; 2x3: `mark(g, '2')` + `diode(g, g.hue, x0=6, w=10, h=10, y=MID_M)` |
| rotating: small_rotating_solar_panel, 2x3_rotating_solar_panel, solar_tracker | `sensor_diode(g, 'daylight')` (sun mark = follows the sun); 2x3: the family lead adds nothing else (tier differs) or requests a mark |
| power sockets: 50v/200v_power_socket | IEC socket outlet (an upward half ring on a bar, `bell` flipped) in the tier hue; family lead draws it in `families/` |
| alarms: standard_alarm / nuclear_alarm | `bell(g, g.hue)` / `mark(g, 'nuclear')` + `bell(g, g.hue, y=MID_M + 4)` |
| data loggers: data_logger / modern_data_logger / industrial_data_logger | `trace(g, g.hue)`; modern: `mark(g, 'M')` + `trace(g, g.hue, ring=RING_M)`; industrial: the same with mark **I** |
| furnaces: electrical_furnace / stone_heat_furnace / fuel_heat_furnace | electrical (untiered, INK): `heat_waves(g, *HEAT_TOP)` + `resistor(g, y=MID_M)`; thermal furnaces (tier thermal): `heat_waves(g, *HEAT_TOP, n=2)` (mandatory) + the fuel as a mark (stone: `mark(g, 'fire')` flame, solid fuel; fuel: `mark(g, 'fuel')` drop) + `resistor(g, g.hue, y=MID_M)` (THERMAL heater) |

---

## 9. Analog and sequential signal blocks (signal tier, per id)

| item id | glyph (call) |
|---|---|
| and/nand/or/nor/xor/xnor_chip | `gate(g, kind, g.hue, invert=...)`: **3 inputs** (the default, `IN_3`; and_chip is the reference) |
| not_chip | `gate(g, 'buf', g.hue, invert=True)` (one input) |
| opamp | `opamp(g, g.hue)`: two inputs on rows 11-12 / 17-18, the INK `-` and `+` level with their leads, clear of the slopes |
| amplifier | `opamp(g, g.hue, inputs=1)`: one input on the centre line, no signs |
| voltage_controlled_amplifier | `opamp(g, g.hue, inputs=1, control=True)`: + the control input as a bare INK lead from the top onto the upper slope (an arrowhead would fuse with the slope; the VCOs' arrow vs the VCA's bare lead is accepted) |
| oscillator_chip | `oscillator(g, 'square', controlled=True)` (voltage-controlled, like the VCOs) |
| voltage_controlled_sine_oscillator | `oscillator(g, 'sine', controlled=True)` (INK control arrow from the top) |
| voltage_controlled_sawtooth_oscillator | `oscillator(g, 'sawtooth', controlled=True)` |
| lowpass_filter | `response(g, 'lowpass', g.hue)` (Bode curve on INK axes) |
| schmitt_trigger_chip | the bare loop, large: `hysteresis(g, 4, 9, 24, 14, g.hue)` (no triangle: a loop inside the 32px buf triangle closes into a box) |
| on_off_regulator_1 / _10 | out of schematic scope (machine parts). Should they come in: the smaller loop plus the 1/10 mark, `mark(g, 'one'/'ten')` + `hysteresis(g, 4, 15, 18, 12, g.hue)`, which tells them from the Schmitt trigger |
| pid_regulator / analogic_regulator | `regulator(g, 'PID')` / `regulator(g, 'P')` (FONT label over a feedback loop) |
| configurable_summing_unit | `summing(g, g.hue)` |
| sample_and_hold | `sample_hold(g)` (series switch + hold capacitor) |
| d_flip_flop_chip / jk_flip_flop_chip | `mark(g, 'd'/'jk')` + `edge(g, 4, 27, 15, 25, g.hue)` (clock edge) |
| pal_chip | `matrix(g, g.hue)` (programmable crossing grid) |
| signal_trimmer | `resistor(g, g.hue)` + `preset_mark(g, 9, 23, 21, 8)` |
| signal_processor | ring `RING` in the hue + FONT **f** (`ring_letter(g, 'f', g.hue)`) with **three** input leads left (rows 10 / 15 / 20, each into the ring) and one output (`families/chips.function_block`) |
| signal_20h_inductor | `inductor(g, g.hue)` |
| rheostat / large_rheostat | `resistor(g, small=True)` / `resistor(g)` + `adjust_arrow(g, 6, 25, 25, 6)`, INK unless tiered |
| redstone_to_voltage / voltage_to_redstone_converter | family lead proposes: a converter must show direction (arrow) and must not use a box |

Chips that are *components* (cheap_chip, advanced_chip, electrical_probe_chip, thermal_probe_chip) are
out of scope unless the manifest says otherwise (open question in section 16).

---

## 10. Letters

- **FONT** (`kit/font.py`, `ref/font.png`): cap height 7; stems and top/bottom bars 2px, middle bars 1px.
  Width 6 (M, W, V8 = 8). A-Z, 0-9, `+ - = ~ / . % & ! $ °`, lowercase `a b e f g h i k l n o r s t u z`
  (so `Cu`, `Fe`, `Hz`, `kW`, `h`, `t°` are drawable). `°` is a raised 5x4 ring. A missing glyph raises a clear `KeyError`.
- Letters are always INK.
- An 8-wide letter (M, W) is comfortable only in `RING` (D=18); in a D=16 ring (`RING_M`) it comes within
  1 px of the ring (allowed for an enclosed label, but prefer 6-wide letters there).
- **MICRO** (3x5, 1px) is the only 1px exception: for 3+ character labels FONT cannot fit. `check()` warns
  whenever MICRO is used, and a MICRO label must never be the only thing telling two items apart. No current
  convention uses it.

---

## 11. Realistic-vs-different rule (decision 5)

- **No device silhouettes, depth or brackets.** Never draw the block, the casing or the in-world model.
- **No outlined box enclosing a label or other content** (it would read as an in-world chip). Labelled
  functions use a ring (`ring_letter`) or a label above a structure (`regulator`). Empty IEC rectangles that
  are themselves symbols are fine: resistor, fuse (with its through-line), relay coil.
- A placeable item with no standard symbol gets a flat symbol of its *function* (turret: a crosshair; utility
  pole: the wire plus a pole mark; hub/joint: wires meeting at a node).
- **Nodes on 2px wires use `contact()`** (6x6): on an orthogonal 2px cross (a 4-way node, the hub) the 4x4
  `junction()` dot lies wholly inside the wires and vanishes, so the node would read as wires crossing
  without a connection. `junction()` is only for a dot in another colour than its wires (`matrix()`).

---

## 12. Do / don't (`ref/dodont.png`)

| do | don't |
|---|---|
| tier hue on the defining element (lamp ring + X) | an INK glyph with a small tier pip |
| the whole switch contact (leads, dots, blade) in the tier hue | hue on the thin blade only |
| IEEE gate silhouette | an IEC `&` box (looks like the in-world chip) |
| one 2px stroke weight | 1px lines |
| flat functional symbol; offset rollers, no face | device silhouette, depth, shading; two "eyes" side by side |
| untiered = all INK (power capacitor) | domain colours (power yellow reads as HV) |
| thermal = THERMAL grey + INK heat waves above the route | thermal in red (reads as VHV); the cue under the stack count |
| mark in MARK_BOX, symbol at RING_M (gap >= 3) | a mark crowding the symbol (halos merge) |
| variant = kit mark (FONT) in the mark slot | a private MICRO label inside the body |
| mirror one half for symmetry | hand-drawing both halves |
| `Glyph(tier=TIER)` and `g.hue` | hard-coded hex, or tier colour on INK parts |
| reuse kit primitives and conventions | private copies of a symbol with different geometry |

---

## 13. Writing an icon

`icons/<item_id>.py`:

```python
"""<item_id>: one line saying what the glyph shows and which convention it follows."""
from kit import *

TIER = 'lv'          # REQUIRED: exactly the manifest's tier (canonical key); None if untiered


def draw():
    g = Glyph(tier=TIER)
    bulb(g, 'carbon')        # a convention (section 8/9), or primitives in a layout (section 7)
    return g                 # or a 32x32 PIL image (converted back and checked the same way)
```

A raw PIL image is accepted either halo-free (the kit adds the halo, as for a Glyph) or as `g.render()`
output; a hand-drawn dark outline fails.

- `TIER` is required; `build.py` FAILS a module without it, or whose Glyph tier differs from it or from the manifest.
- Family-wide helpers go in `families/<family>.py` (`from families.<family> import ...`). Never copy kit code.
- **`kit/` is read-only for family artists.** Request changes in your result: the signature, what it
  draws, and the items that need it.
- Tier variants: write the program once in `families/<family>.py`, one three-line module per id.
- Leave no WARN in the build output unless the module docstring explains it.

## 14. Commands

Run everything in the repo's dev shell (`direnv allow` or `nix develop`), from `tools/icons/schematic`. **Always pass your own ids or your `--family`**: the bare `build.py`
(everything) is for the orchestrator, and other artists' unfinished modules would fail it.

```
python3 build.py and_chip power_capacitor     # a subset (each module in its own process)
python3 build.py --family <family>            # in-scope ids of a manifest family
python3 build.py --family <family> --contact /tmp/x.png     # + contact sheet of the passing icons
python3 build.py --info <id>                  # the manifest row (tier, family, scope ...)
python3 build.py --list-missing               # in-scope items with no module yet
python3 ../tools/contact.py OUT.png out [ids...]           # inventory slots GUI 2/3, dark row, zoom
python3 -m kit.catalog ref/catalog.png        # every primitive
python3 -m kit.catalog ref/conventions.png conventions     # the family conventions
python3 -m kit.dodont ref/dodont.png
python3 -m kit.palette --cvd [signal=#8fb8a0 ...]          # colour-vision distances (try a retune)
```

Build results: a passing icon goes to `out/<id>.png` (atomic write); a failing one to `out/_fail/<id>.png`
and its old `out/<id>.png` is removed, so **`out/` only holds icons that passed their last build**.
Exit status 1 when anything failed. Each module runs in its own process, so a crash, `sys.exit()`, an
endless loop (60 s timeout) or a module that edits kit state affects nothing else.

**Always look at the contact sheet** before handing an icon in: GUI-2 legibility next to its family and
tier siblings, the tier reads instantly, nothing blobs on the dark row.

## 15. Integration notes (orchestrator)

- Copy `out/*.png` (not `out/_fail/`) for ids with `schematic_scope = true`.
- Run `python3 build.py --strict` for the final build: a missing manifest is then a FAIL too.

## 16. Known weaknesses and open owner decisions

1. **Palette vs colour-blindness** (section 5.4). Decided 2026-10-12: the owner keeps the mod's own tier
   colours and does not retune (Minecraft 26.3 has no colour-blind mode). The structural rules of 5.4 stay.
2. **Thermal grey.** `#767676` is ΔE 7.9 from the slot grey, so a thermal stroke reads mostly as its halo
   outline; the contract names `thermal.png` (#424242), which is darker still. Approved by the owner with
   the INK heat waves mandatory on every thermal glyph (5.1).
3. **Density mismatch.** These 32px glyphs with a sticker-like halo read as a UI icon font next to vanilla's
   shaded items. Accepted: the set is optional.
4. **Pictograms are tiny** (7x8). They are kit-defined so they are at least identical everywhere; a
   pictogram plus the tier colour plus the tooltip is the design, not the pictogram alone.
5. **Learning curve.** Many devices share a symbol and differ by a mark; a legend page would help.
