# REVIEW.md: what a pre-push review checks in Electrical Age

Read by the reviewer subagent before a change is enqueued (wiki proc/merge-queue). Its first question is whether
the change **adds a second way to do something that already exists**: a helper, a test fixture, a UX. Use the
canonical pieces below. If one doesn't fit, extend it, or say in the commit why not. Don't copy it.

Kept current by the weekly consolidation (first: 2026-11-21). When a finding keeps coming back in review, add it
here.

## Canonical helpers (use these, don't re-write them)

| Need | Use | Not |
|---|---|---|
| Route a cable or pipe (A*) | `node/voxel/Router` | another pathfinder; a cable-only or pipe-only copy of a router step |
| A device with one static model and nothing animated | `node/transparent/FrontDrawnRender` (the descriptor implements `FrontDrawnRender.Drawn`) | a new `XRender` whose draw() is `front.glRotateXnRef(); desc.draw();` |
| Item custom data (counters, lengths, settings) | `generic/ItemNbt` | hand-rolled `CustomData` get/put in the item class |
| Numbers with units in text | `api/Meters` (`volts`, `amps`, `watts`, `joules`, `ohms`, `celsius`, `kelvin`, `number`); `misc/Utils.plot*` | `String.format("%.2f kV", …)`, `(int) w + " W"`, a local `fmt()` |
| Fluid amounts and contents | `sim/fluid/EaUnits` (`volume`, `contents`, `composition`, `steamGrams`) | `+ " mB"` by hand (EaUnits switches to buckets at 1000 mB) |
| Voltage tiers | `Eln.LVU/MVU/HVU/VVU` (48, 230, 400, 11000 V) | a literal 50, 200, 800 or other old 1.12 voltage in code or comments |
| Wrench turning | six-node: `SixNodeElement.onBlockActivatedRotate` (`wrenchStep` for half turns); transparent: override `TransparentNodeElement.wrenchTurn`; voxel: `VoxelNode.wrenchTurn`; fluid devices: implement `fluid/FluidPlacement.Turnable` (the base wrenchTurn turns it); a wire's colour care and brush: `SixNodeElement.toggleColorCare` / `brushPaint` | a per-device rotate handler; a wrench turn in onBlockActivated |
| A battery-charge bar | `client/gui/EaGauge.chargeBar` (`chargeColor`) | another bar fill or charge palette |
| Clamping | `misc/Utils.limit` | another local `clamp` |
| Kit recipes | `node/voxel/KitRecipes` | a per-kit recipe skeleton |
| GameTest ids | `gametest/cases/Ids` | a per-file `transparent(String)` / `six(String)` lookup |
| GameTest world access | `TestContext` (`at`, `floor`, `placeSix`, …; docs/testing.md) | per-file `node(ctx,pos)`, `player(ctx)`, `source(u)` or `resistor()` copies (consolidation found 6-7 of each) |

## Invariants

- **One UX per player action.** Placing cables and pipes is one interaction (kits, waypoints, undo, lapse), and route
  results and failures appear in the same place. A new placement or configuration path matches the existing one, or
  the change says why it's different and Baughn agreed.
- **Text the player reads uses the canonical formatters,** so the same quantity reads the same everywhere (same
  unit, prefix and precision).
- **GameTest outcomes don't depend on neighbours** (21-block padding; Baughn). Cases that differ only in parameters
  are rows of one parameterised case (see `HpPlantCases.addTo`), not copies.
- **The simulation runs in unloaded chunks too:** nodes stay. A test or feature that assumes otherwise is wrong.
- **Comments state the current numbers.** When a tier, rating or recipe changes, grep the comments and docs too:
  stale "800 V" comments outlived the 400 V anvil by three weeks.

## Findings that keep coming back

- Unit formatting re-done locally (consolidation 2026-11-21 found 5 voltage formatters, 3 Celsius formatters, and
  4 ways of showing percent). Use `Meters`/`Utils.plot*`.
- Hard-coded GUI colours (the error red `0xFFA02020` appears in 8 places). Put a named constant in `ElnScreen`.
- Copy-pasted GameTest fixtures (sources, resistors, `node()`, `player()`). Use or extend `TestContext`.
- Copy-pasted Element/Render/Container pairs differing only in constants (capacitor vs inductor, six-node vs
  transparent power capacitor). Parameterise the descriptor instead of copying the class.

## Known duplication not yet folded (don't add to it)

Pending Baughn's decision where the UX differs:
CablePlacer/PipePlacer route state machine, KitPreview cable/pipe ticks, chat vs action-bar error messages.
