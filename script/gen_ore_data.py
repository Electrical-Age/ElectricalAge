#!/usr/bin/env python3
"""Generate the data files of EA's ores (26.x port, Wp12c): loot tables (each ore drops itself, as 1.12), the
mineable/pickaxe tag, and world generation for the ores that generate with the 1.12 config defaults (production's
eln.cfg too): only tungsten. Keep ORES in sync with registry/batch/Wp12cContent.registerOre.

1.12 generated spawnRate veins per chunk at y spawnHeightMin..spawnHeightMax-1 with a random size
spawnSizeMin..spawnSizeMax-1 (WorldGenMinable on plain stone). A 26.x ore feature has one size: the mean is used.
Usage: gen_ore_data.py
"""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data')

# name, generates (1.12 default config), spawnRate, sizeMin, sizeMax, heightMin, heightMax
ORES = [
    ('copper_ore', False, 30, 6, 10, 0, 80),
    ('lead_ore', False, 8, 3, 9, 0, 24),
    ('tungsten_ore', True, 6, 3, 9, 0, 32),
    ('cinnabar_ore', False, 3, 3, 9, 0, 32),
]


def write(rel, obj):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    print(rel)


for name, gen, rate, smin, smax, hmin, hmax in ORES:
    write(f'eln/loot_table/blocks/{name}.json', {
        'type': 'minecraft:block',
        'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                   'entries': [{'type': 'minecraft:item', 'name': f'eln:{name}'}], 'rolls': 1}],
        'random_sequence': f'eln:blocks/{name}'})
    if not gen:
        continue
    write(f'eln/worldgen/feature/{name}.json', {
        'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': round((smin + smax - 1) / 2),
        'targets': [{'state': f'eln:{name}',
                     'target': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:stone'}}]})
    write(f'eln/worldgen/placed_feature/{name}.json', {
        'feature': f'eln:{name}',
        'placement': [{'type': 'minecraft:count', 'count': rate}, {'type': 'minecraft:in_square'},
                      {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform',
                                                                    'min_inclusive': {'absolute': hmin},
                                                                    'max_inclusive': {'absolute': hmax - 1}}},
                      {'type': 'minecraft:biome'}]})

write('minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'eln:{o[0]}' for o in ORES]})
write('eln/neoforge/biome_modifier/ores.json', {
    'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld',
    'features': [f'eln:{o[0]}' for o in ORES if o[1]], 'step': 'underground_ores'})
