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
- It switches in when the line is at `resume` (95%: 760 V) of 800 V, or when the line has levelled off (rising under
  1 V/s for 2 s) at `plateau` (93%: 744 V) or more; it drops out below `sag` (90%), keeping its progress, then holds off
  1 s (no chattering on a line that can't carry it). 98% was too high: a bank's own leak through a 100 ohm feed holds
  the line near 780 V, and the desk waited forever (AdventurAgent). A weak plant with a capacitor bank (or an inductor) behind
  the desk charges between pulses and lets it work.
- Energy: `joulesPerUnit` (40 kJ: 8 s at 5 kW) for a unit's worth of repair, pro rata for less; `joulesPerName` (2 kJ).
- **Bursts** (Baughn 2026-10-28): the 40 kJ of a unit come in `burstsPerUnit` (2) bursts of 20 kJ. After each it rests
  (state "Between bursts", `RECOVERING`) for at least `burstGapSeconds` (3 s) and until it may start again, so a
  bank (or a battery bank) recovers. Engraving stays one small pulse. `burstStart` is saved with `energy`.
- Over 800 V by the usual margin it fails like other machines (`VoltageStateWatchDog`).

## Config (`[anvil]` in `config/eln-synced.toml`)
`power` 5000 W, `joulesPerUnit` 40000, `joulesPerName` 2000, `resumeFraction` 0.95, `plateauFraction` 0.93, `sagFraction` 0.9, `burstsPerUnit` 2,
`burstGapSeconds` 3.

## The capacitor bank (`transparentnode/capacitorbank`, `eln:transparent_node@5444`, `capacitor_bank`)
Baughn 2026-10-28: a workshop-sized store for the anvil's bursts. Six capacitor cans in two layers of three (model
`tools/models/capacitorbank.py`), one capacitor inside.
- **Size:** one bank carries one 20 kJ burst from 800 V down to the anvil's 720 V: C >= 20000 / (0.5 (800^2 - 720^2))
  = 0.33 F; it has **0.36 F** (21.9 kJ between 800 and 720 V; 115 kJ at 800 V).
- **Polar:** + on the left of its front, - on the right, low on the flanks: the terminal plates' bands and the cans'
  studs are red (+) and blue (-) (Baughn); the cans' sleeve bands keep the HV tier colour.
- **Rating:** 1000 V; above 1.3 x that (`BipoleVoltageWatchdog`, as the Power Capacitor) it **explodes** (Baughn
  2026-10-28: "should probably explode if overheated or overvoltage'd ... realistic, at this size"):
  `Failure.capacitorBank`, strength `bankStrength(E) = sqrt(E / 40 kJ)`, 1 .. 3 (a creeper at most): a full bank at 800 V
  (115 kJ) is 1.7, one blown at 1300 V (300 kJ) 2.7. With explosions off (`Eln.explosionEnable`) it only pops.
- **Heat:** an ESR of 0.1 ohm in series with the capacitor, and the leak, heat a thermal load (2000 J/K, 0.3 K/W to
  ambient; Jade and the thermometer show it). Past 75 K over ambient (~95 C, an electrolytic's limit) it explodes the same
  way. The anvil's bursts (~6 A) put ~4 W into the ESR, the leak ~128 W at 800 V (steady ~38 K over ambient): fine. A
  stiff line's inrush puts most of the bank's energy into the ESR at once (~57 K per full charge or discharge): two of
  those and it goes.
- **Inrush:** none limited (Baughn 2026-10-28: "inrush current is supposed to be a problem; inductors are the solution.
  There are other solutions, like not using a stiff source"). An empty bank straight on a stiff 800 V source draws
  hundreds of amps and burns its HV cable (GameTest `capacitor_bank_inrush`). Charge it through a resistance, and mind
  the bank's own leak (~5 kohm): it makes a divider with the feed, so the bank parks at 800 x 5000 / (5000 + R). A
  1 kohm feed parks it at ~667 V, under the anvil's 720 V stop: useless. Use a Large Rheostat at ~330 ohm from empty
  (peak ~2.4 A; it parks at ~750 V, enough for the anvil's levelled-line start at 744 V), and turn it down to ~100 ohm
  once the bank is past ~600 V (2 A at that point; it parks at ~784 V). An inductor alone can't: with EA's largest, 100 H, an empty
  0.36 F bank on 800 V rings with a peak of 800 sqrt(C/L) = 48 A and overshoots towards 1.6 kV, past its 1.3 kV limit;
  an inductor helps smooth a plant's draw once the bank is charged, in series with some resistance.
- **Leakage:** a resistor across it, R = `leakTauS` / C (RC = 30 min): ~128 W at 800 V; the charge halves in ~10 min
  (energy), 2% is left after an hour. It's for bursts, not for keeping (Baughn: lead-acid batteries stay the only
  sensible bulk store).
- No GUI; Jade shows voltage, stored energy, capacitance. Config `[capacitorBank]`: `farads`, `ratedVolts`,
  `leakTauSeconds` (and `powerCapacitorLeakTauSeconds`, the six-node Power Capacitor's).
- **Recipe:** Capacitor Cell (`eln:capacitor_cell`): `dpd` / `dRd` / `dpd` (6 Dielectric, 2 copper plate, a redstone block);
  bank: `CCC` / `CCC` / `pHp` (6 cells, 2 iron plate, High Voltage Cable): 36 Dielectrics. The compressor now makes 4
  Dielectrics a sand (was 1), so that's 9 sand.

### Storage economy (from the code; Baughn: batteries stay the bulk store)

| Store | Voltage | Energy | Usable for the anvil (800 -> 720 V) | Power | Self-discharge | Cost |
|---|---|---|---|---|---|---|
| Capacitor Bank | up to 1000 V | 115 kJ at 800 V | 21.9 kJ | any, either way (empty, its inrush burns a stiff line's cable) | RC 30 min (~128 W at 800 V) | 36 Dielectric (9 sand), 54 redstone, 12 copper plate, 2 iron plate, HV cable |
| Power Capacitor (six-node), 13 redstone, 16 Dielectric | 800 V rating | 12.5 kJ | 2.4 kJ | any | RC 30 min (was 33 h in 1.12; Baughn 2026-10-28) | 13 redstone, 16 Dielectric (4 sand), 2 iron cable, iron plate, 3 iron |
| Cost Oriented Battery | 50 V | 60 kJ | - (needs a converter) | 250 W (300 max) | none (wears with use: half-life 2 MC days of nominal current) | 6 lead, 2 LV cable |
| Capacity Oriented Battery | 12.5 V | 240 kJ | - | 125 W | none (wears) | 14 lead, 2 LV cable |
| Voltage Oriented Battery | 200 V | 60 kJ | - | 250 W | none (wears) | 6 lead, 8 iron, 2 LV cable |
| Current Oriented Battery | 50 V | 10 kJ | - | 1 kW | none (wears) | 6 lead, 8 copper, 2 LV cable |

A charged bank keeps 2% after an hour: no use for storing a night's power; a battery keeps it all.

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
`anvil_pauses_on_a_sag` (no start at a level 740 V; a level 750 V starts it after 2 s; pause below 90% keeping progress,
1 s hold-off; a 20 kJ burst, then a 3 s rest), `anvil_welds_from_a_bank_on_a_weak_plant` (800 V behind 100 ohm, a bank at
780 V: it welds),
`capacitor_bank_inrush` (straight on 800 V its cable burns; behind 1 kohm it charges to 43 V in 20 s, nothing harmed),
`capacitor_bank_holds_and_pops` (800 V kept), `capacitor_bank_explodes_over_voltage` (1600 V: it explodes; strength by
energy), `capacitor_bank_overheats_and_explodes` (a source right on its plate switched 800/0 V every 2 s: 105 K, boom),
`anvil_welds_from_a_bank_on_a_weak_plant` also checks the bank stays under 15 K over ambient, `power_capacitor_leaks_like_the_bank`,
`capacitor_bank_carries_an_anvil_burst` (a charged bank alone carries one 20 kJ burst, left at ~723 V).
