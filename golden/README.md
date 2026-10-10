# Golden data

Behaviour reference for porting Electrical Age to new Minecraft versions (plan: port step T4).

- `1.12/inventory.json`: `/eln selftest inventory`. Every EA block, item and sub-item, with each descriptor's primitive
  fields (nominal voltages, resistances, thermal limits, capacities...). `selftestPlaced` marks devices placed by a
  selftest in the same server session.
- `1.12/recipes.json`: `/eln selftest recipes`. Every EA recipe: crafting (ingredients as the stacks they match),
  smelting, and the macerator/compressor/plate machine/magnetizer lists. Taken in the E36 pack, so ore-dictionary
  ingredients include other mods' items.

- `1.12/slots.json`: written by the selftest's smoke case (`/eln selftest`). For every device with a GUI, each of its
  container slots: the class, stack limit, and which probe items it accepts (every EA shared item, every six-node item,
  some vanilla items).

A port regenerates both with the same commands and diffs them against these files; every difference must be explained.

## Known differences (26.x port)

`golden_slots` (GoldenSlotsCase.KNOWN_MISSING) accepts these, and reports each as an INFO line:
- Coal dust slots (resistor, rheostat, thermistor, large rheostat) refuse Mercury and Gold Plate. 1.12's
  `ItemStackFilter.OreDict` masked the damage to 8 bits, so shared items 520 and 7688 passed as Coal Dust (8). A bug.
- The stone heat furnace's fuel slot refuses paper. The E36 pack made paper a furnace fuel; vanilla 26.x has none.

`golden_recipes`: the macerator turns vanilla ore blocks (iron, gold, copper; deepslate too, by tag) into raw chunks
about like Fortune II (iron/gold 1-3, mean 1.75; copper 3-8), and each chunk into one dust with a 20% chance of two
(Baughn, 2026-10-10). 1.12 gave two dust per ore block and had no chunks.
