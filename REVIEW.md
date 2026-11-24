# REVIEW.md: what a pre-push review checks in Electrical Age

Read by the reviewer subagent before a change is enqueued (wiki proc/merge-queue). Its first question is whether
the change **adds a second way to do something that already exists**: a helper, a test fixture, a UX. Use the
canonical pieces below. If one doesn't fit, extend it, or say in the commit why not. Don't copy it.

Kept current by the weekly consolidation (first: 2026-11-21). When a finding keeps coming back in review, add it
here.

## Canonical helpers (use these, don't re-write them)

| Need | Use | Not |
|---|---|---|
| Route a cable or pipe (A*) | `node/voxel/Router`; what's in the way: `RouteView.obstacles` (and `freeAround` a junction) | another pathfinder; a cable-only or pipe-only copy of a router step or of the obstacle test |
| A kit's route UX (start, waypoints, finish, undo, lapse, preview) | `node/voxel/RoutePlacer` with the kind's `RoutePlacer.Spec` (`CablePlacer.ROUTE`, `PipePlacer.ROUTE`); the preview `KitPreview.tick`; laying `Laying.lay` (joins included) | a per-kind plan, plans map, click handler or preview tick; a kind check (`pipe()`) in a caller instead of a spec hook |
| Do two fluid ports couple | `FluidWorld.couples(p, q)` (the rule the rebuild's plan uses); in the world: `FluidPlacement.partners(level, p)` (its block lookup `FluidPlacement.portsAt`; a multiblock with ports on several blocks, its ghosts included, implements `fluid/GhostPortHost`); the topology: `FluidWorld.plan` | another facing / opposite-face / same-spot check by hand (PipePlacer, FluidPlacement and TankElement each had one); an instanceof case per multiblock in `portsAt` |
| A device with one static model and nothing animated | `node/transparent/FrontDrawnRender` (the descriptor implements `FrontDrawnRender.Drawn`) | a new `XRender` whose draw() is `front.glRotateXnRef(); desc.draw();` |
| Item custom data (counters, lengths, settings) | `generic/ItemNbt` | hand-rolled `CustomData` get/put in the item class |
| Numbers with units in text | `api/Meters` (`volts`, `amps`, `watts`, `joules`, `ohms`, `celsius`, `kelvin`, `number`) for a bare value; `misc/Utils.plot*(header, v)` only for the padded meter lines built from them | `String.format("%.2f kV", …)`, `(int) w + " W"`, `plotPower("", w).trim()`, a local `fmt()` |
| A cable cross-section in text | `CableSpec.mm2Number(mm2)` ("2.5", "4") | a local `fmt()` dropping ".0" |
| A power tier's voltage (48 V … 11 kV) | `node/voxel/CableSpec.voltsLabel(tier)` ("48V", "11kV", as the device names; readings use Meters.volts) | `(int) v / 1000 + " kV"` |
| Temperatures | Tc scale (K above ambient, thermal loads): `Utils.plotCelsius(t)`; absolute °C: `Meters.celsius`; a difference: `Meters.kelvin` | `plotValue(t + Tamb) + "°C"`, `"%.0f °C"`, plotCelsius on an absolute °C or a difference |
| Percent | `Utils.plotPercent(share)` ("50%"; 1 is 100 %) | `(int) (x * 100) + "%"`, `tr("…%1$%", round(x * 100))`, `plotPercent("", x).trim()` |
| Fluid amounts and contents | `sim/fluid/EaUnits` (`volume`, `tank` for "fill / capacity", `contents`, `composition`, `steamGrams`; viscosity: `viscosityText` for a value, `viscosity(node)`/`putViscosity` for a container's line; the scale: `LITRES_PER_MB`, `KG_PER_MB_WATER`, `STEAM_MB_PER_WATER_MB`) | `+ " mB"` or `"%.0f / %d mB"` by hand (EaUnits switches to buckets at 1000 mB); a restated 6.75e-5 or 67.5 |
| Telling the player something | Transient (a refusal, "needs X", a mode toggle, out of range, a passing reading): the action bar, `player.sendOverlayMessage(Component.literal(…))`; why a kit click failed: `KitPreview.note` (the kit's preview owns the action bar). Chat (`Utils.sendMessage`) only for what is read later: meter readings, multi-line reports, lasting warnings, commands and debug | `Utils.sendMessage` for a refusal |
| Voltage tiers | `Eln.LVU/MVU/HVU/VVU` (48, 230, 400, 11000 V) | a literal 50, 200, 800 or other old 1.12 voltage in code or comments |
| Wrench turning | six-node: `SixNodeElement.onBlockActivatedRotate` (`wrenchStep` for half turns); transparent: override `TransparentNodeElement.wrenchTurn`; voxel: `VoxelNode.wrenchTurn`; fluid devices: implement `fluid/FluidPlacement.Turnable` (the base wrenchTurn turns it); a wire's brush: `SixNodeElement.brushPaint` | a per-device rotate handler; a wrench turn in onBlockActivated |
| GUI text colours | `ElnScreen.LABEL`, `ERROR_TEXT` (a fault, a missing input), `ERROR_TEXT_BRIGHT`, `OK_TEXT` (at work) | a literal `0xFFA02020` / `0xFF206020` in a screen |
| A battery-charge bar | `client/gui/EaGauge.chargeBar` (`chargeColor`) | another bar fill or charge palette |
| Clamping | `misc/Utils.limit` | another local `clamp` |
| A saved or computed value that may be NaN/infinite | `misc/Utils.finiteOr(v, fallback)` | `if (isNaN(x)) x = 0; if (x == POSITIVE_INFINITY) …` |
| A client packet to the server | `ElnNet.sendToServer(byte[])` (or the stream form) | a ByteArrayOutputStream filled only to be sent |
| Kit recipes | `node/voxel/KitRecipes` | a per-kit recipe skeleton |
| GameTest ids | `gametest/cases/Ids` | a per-file `transparent(String)` / `six(String)` lookup |
| GameTest world access | `TestContext` (`at`, `floor`, `placeSix`, `player()`, `holding`, `nodeAt`/`voxelAt`/`voxelByEntity`, `source`/`setVolts`, static `field`; docs/testing.md); six-node electrical fixtures `Wp9aCases` (`orient`, `load`, `resistor`, `current`); `Wp10aCases.rs()`/`off()`; shaft lines `ShaftSteamCases.turbineLine`; HP plants `HpPlantCases.hpTurbine`/`condensateOut`/`WallMeter`; steam in pipes `PipeCases.steam` | per-file `node(ctx,pos)`, `player(ctx)`, `source(u)`, `resistor()` or reflective `field()` copies; a hand-written `setVoltageId` packet |

## Invariants

- **One UX per player action.** Placing cables and pipes is one interaction (kits, waypoints, undo, lapse), and route
  results and failures appear in the same place. A new placement or configuration path matches the existing one, or
  the change says why it's different and Baughn agreed.
- **Text the player reads uses the canonical formatters,** so the same quantity reads the same everywhere (same
  unit, prefix and precision). Check the GUI JSON `sample` of a fixed-width line when its text changes.
- **GameTest outcomes don't depend on neighbours** (21-block padding; Baughn). Cases that differ only in parameters
  are rows of one parameterised case (see `HpPlantCases.addTo`), not copies.
- **The simulation runs in unloaded chunks too:** nodes stay. A test or feature that assumes otherwise is wrong.
- **Comments state the current numbers.** When a tier, rating or recipe changes, grep the comments and docs too:
  stale "800 V" comments outlived the 400 V anvil by three weeks.

## Findings that keep coming back

- Unit formatting re-done locally (consolidation 2026-11-21 found 5 voltage formatters, 3 Celsius formatters, and
  4 ways of showing percent; folded the same day, todo #553). Use `Meters`/`Utils.plot*`/`EaUnits`.
- Feeding the wrong scale to a formatter: plotCelsius takes the Tc scale, so an absolute °C or a difference read
  20 °C off (the tank's and servo's thermometers, the turbine's dT, until 2026-11-21).
- Hard-coded GUI colours (the error red `0xFFA02020` was in 8 places; now `ElnScreen.ERROR_TEXT`). Put a named constant in `ElnScreen`.
- Copy-pasted GameTest fixtures (sources, resistors, `node()`, `player()`; folded into `TestContext` 2026-11-21). Use or extend `TestContext`.
- Copy-pasted Element/Render/Container pairs differing only in constants (capacitor vs inductor, six-node vs
  transparent power capacitor). Parameterise the descriptor instead of copying the class.

## Known duplication not yet folded (don't add to it)

`EaUnits.compact` (two significant figures, no prefix) stays beside `Meters.number`: fluid units switch unit
themselves (mB to B, g to kg), and Meters.number would print "120m mB". Descriptor tooltips that state ratings in
prose (`"%.0f V, %.0f W"`), amps in cable and winding names (`"%.0f A"`) and mB/s rates are not yet on the formatters.
Many Jade lines still use the padded `Utils.plot*("", v)` (trailing spaces in the value); new ones use `Meters`.
