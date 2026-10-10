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
