#!/usr/bin/env python3
"""Generate the data files of EA's ores (26.x port, Wp12c): loot tables (lead and tungsten drop raw pieces as iron
ore, silk touch keeps the ore: Baughn 2026-10-19; others drop themselves, as 1.12), the mineable/pickaxe tag, and world
generation for the ores that generate with the 1.12 config defaults (production's eln.cfg too): tungsten, and on 26.x
lead (no other mod brings lead), in the middle of the deepslate range with a deepslate variant (Baughn 2026-10-20: y < 24
was 1.7.10's world). Keep ORES in sync with registry/batch/Wp12cContent.registerOre.

1.12 generated spawnRate veins per chunk at y spawnHeightMin..spawnHeightMax-1 with a random size
spawnSizeMin..spawnSizeMax-1 (WorldGenMinable on plain stone). A 26.x ore feature has one size: the mean is used.
Usage: gen_ore_data.py
"""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data')

# name, generates (1.12 default config), spawnRate, sizeMin, sizeMax, heightMin, heightMax, raw drop, deepslate variant,
# height distribution ('uniform' over [min, max-1] as 1.12, or 'trapezoid' with no plateau: a triangle peaking in the middle)
ORES = [
    # 26.x: on (Baughn, 2026-10-11): batteries need lead, no other mod supplies it; deepslate's middle (2026-10-20)
    ('lead_ore', True, 8, 3, 9, -64, 1, 'raw_lead', 'deepslate_lead_ore', 'trapezoid'),
    ('tungsten_ore', True, 6, 3, 9, 0, 32, 'raw_tungsten', None, 'uniform'),
    ('cinnabar_ore', False, 3, 3, 9, 0, 32, None, None, 'uniform'),
]


def loot(block, raw):
    if raw is None:
        return {'type': 'minecraft:block',
                'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                           'entries': [{'type': 'minecraft:item', 'name': f'eln:{block}'}], 'rolls': 1}],
                'random_sequence': f'eln:blocks/{block}'}
    return {'type': 'minecraft:block',
            'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': f'eln:{block}'},
                {'type': 'minecraft:item', 'modifier': [
                    {'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                    {'type': 'minecraft:explosion_decay'}], 'name': f'eln:{raw}'}]}], 'rolls': 1}],
            'random_sequence': f'eln:blocks/{block}'}


def write(rel, obj):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    print(rel)


for name, gen, rate, smin, smax, hmin, hmax, raw, deep, dist in ORES:
    write(f'eln/loot_table/blocks/{name}.json', loot(name, raw))
    if deep:
        write(f'eln/loot_table/blocks/{deep}.json', loot(deep, raw))
    if not gen:
        continue
    targets = [{'state': f'eln:{name}', 'target': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:stone'}}]
    if deep:   # (as vanilla's ores: stone and its kin, deepslate and tuff)
        targets = [{'state': f'eln:{name}', 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}},
                   {'state': f'eln:{deep}', 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}}]
    write(f'eln/worldgen/feature/{name}.json', {
        'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': round((smin + smax - 1) / 2), 'targets': targets})
    write(f'eln/worldgen/placed_feature/{name}.json', {
        'feature': f'eln:{name}',
        'placement': [{'type': 'minecraft:count', 'count': rate}, {'type': 'minecraft:in_square'},
                      {'type': 'minecraft:height_range', 'height': {'type': f'minecraft:{dist}',
                                                                    'min_inclusive': {'absolute': hmin},
                                                                    'max_inclusive': {'absolute': hmax - 1}}},
                      {'type': 'minecraft:biome'}]})

write('minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'eln:{o[0]}' for o in ORES] + [f'eln:{o[8]}' for o in ORES if o[8]]})
write('eln/neoforge/biome_modifier/ores.json', {
    'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld',
    'features': [f'eln:{o[0]}' for o in ORES if o[1]], 'step': 'underground_ores'})
