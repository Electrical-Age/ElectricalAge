# The electrical anvil

Baughn's design (2026-10-28): late-midgame gear, power-hungry, a workshop desk. It repairs tools without the vanilla
anvil's ratchet. Code in `src/main/java/mods/eln/anvil/`; model `tools/models/electricalanvil.py`; tests
`gametest/cases/AnvilCases` and `AnvilRulesTest`.

## Baughn's decisions
1. No XP at all. 2. Renaming almost free: a small pulse (engraving). 3. Annealing: an old penalty steps down ("we
shouldn't penalise players for not knowing about the block"). 4. Repair as vanilla (a quarter per unit of material), but
the desk stores fractions of a unit so topping a tool up wastes nothing. 5. A pulse on an 800 V input at its full
rating, so players need capacitors or inductors (the handbook explains the problem). 6. Mending is rare on Incognito
(villages are frozen), so this is the main way to keep enchanted gear: repair only, no enchanting.

## Rules (`AnvilRules`, `ElectricalAnvilElement`)

| Rule | Value |
|---|---|
| Slots | 0 the tool (one, mended in place), 1 its material. Hoppers may only top up the material. |
| A step | restores a unit's worth (`maxDamage / 4`, as vanilla) or what's missing, if less |
| Material | the tool's repair item (`isValidRepairItem`), or an unenchanted copy of the tool (vanilla's two-tools repair: its durability + 12% of the maximum, the copy consumed). Enchanted books and enchanted copies are refused: no combining. |
| Credit | per material id, fractions of a unit, saved in the desk (`credit` compound; lost if the desk is broken: under a unit per material). A step uses credit first, then whole units from the slot; the change stays as credit. With the slot empty, any credited material that repairs the tool is used. |
| Penalty | never raised. Each repair step anneals it a notch of vanilla's ladder ((c - 1) >> 1: 31, 15, 7, 3, 1, 0), not below `penaltyFloor(n) = 2^ceil(log2(n + 1)) - 1` for n enchantments: the least a vanilla anvil tree could leave, so it can't make room for more books than a fresh tool takes. |
| Engraving | the screen's name field (Enter): a 2 kJ pulse, then the name is set (blank removes it). No penalty change. |
| Not mended | items whose bar is charge (`IItemEnergyBattery`, `IItemEnergySelfManaged`: EA tools and batteries, Stasis suits), undamageable items |
| XP | none, ever |

Why the credit is in the desk and not on the tool: it's material, not the tool's state; a top-up of tool A then pays
part of tool B, which is what "fractional materials stored in the block" means. Carrying it on the tool would need a
new data component and would leak into other mods' views of the item.

## Power (the pulse)
- One electrical connection: the 800 V terminal plate low on the back (`CableTerminals.BACK`), HV cable resistance.
- Working, it is a resistor `R = 800^2 / power` (128 ohm at 5 kW): it draws its rating at 800 V. Idle, it's open.
- It switches in when the line is at `resume` (98%) of 800 V and drops out below `sag` (90%), keeping its progress, then
  holds off 1 s (no chattering on a line that can't carry it). A weak plant with a capacitor bank (or an inductor) behind
  the desk charges between pulses and lets it work.
- Energy: `joulesPerUnit` (40 kJ: 8 s at 5 kW) for a unit's worth of repair, pro rata for less; `joulesPerName` (2 kJ).
- Over 800 V by the usual margin it fails like other machines (`VoltageStateWatchDog`).

## Config (`[anvil]` in `config/eln-synced.toml`)
`power` 5000 W, `joulesPerUnit` 40000, `joulesPerName` 2000, `resumeFraction` 0.98, `sagFraction` 0.9.

## Client
- `ElectricalAnvilRender`: the desk (Obj3D part `main`), the tool lying face up on the work plate (`drawFlatItem` at the
  model's `toolX/Y/Z`), electric sparks from the torch tip (`sparkX/Y/Z`) and the `eln:arc_weld` loop while it welds
  (higher pitch while engraving).
- `ElectricalAnvilScreen` (layout `gui/electrical_anvil.json`): tool -> progress <- material, the name field, the state
  (waiting for the line / welding with the power / supply too weak), the credit per material, the tool's anvil cost and
  its floor ("no XP needed").
- Jade: state, power while welding, credit.

## Recipe
`HAH` / `pMp` / `i i`: High Voltage Cable, an anvil / iron plates, Advanced Machine Block / iron ingot legs.
New in 26.x (`GoldenCase.NEW_IN_26`, `GoldenSlotsCase.NEW_SPECS`).

## Tests
`AnvilRulesTest` (floor, annealing, units, credit), GameTests `anvil_repairs_without_xp_and_anneals` (steps, credit,
refusals, plain copy, engraving), `anvil_800v_pulse` (5 kW on a stiff 800 V line, one quarter in 8 s; waits on 700 V),
`anvil_pauses_on_a_sag` (start at 98%, pause below 90% keeping progress, 1 s hold-off).
