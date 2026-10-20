#!/usr/bin/env python3
"""EA's guide advancements (Baughn's hints, 2026-10-20): writes data/eln/advancement/guide/*.json and prints the lang
entries for datagen/ElnLang. Usage: make_guide.py REPO"""
import json, os, sys
repo = sys.argv[1]
out = os.path.join(repo, 'src/main/resources/data/eln/advancement/guide')
os.makedirs(out, exist_ok=True)
# key, parent, icon, items, title, description
A = [
 ('root', None, 'eln:rubber', ['eln:rubber'], 'Electrical Age', 'Every tree is a rubber tree.'),
 ('copper_cable', 'root', 'eln:copper_cable', ['eln:copper_cable'], 'Live Wire', 'It all starts with three copper in a row.'),
 ('heat_furnace', 'copper_cable', 'eln:stone_heat_furnace', ['eln:stone_heat_furnace', 'eln:fuel_heat_furnace'], 'Fire Starter', 'Ouch - this one is hot.'),
 ('boiler', 'heat_furnace', 'eln:boiler', ['eln:boiler'], 'Boiling Point', 'Pipe in your water.'),
 ('safety_valve', 'boiler', 'eln:pipe_safety_valve', ['eln:pipe_safety_valve'], 'Safety First', 'A safety valve beats a burst.'),
 ('steam_turbine', 'boiler', 'eln:50v_steam_turbine', ['eln:50v_steam_turbine', 'eln:200v_steam_turbine'], 'Full Steam Ahead', 'Set it under the boiler. Or don\'t.'),
 ('big_iron', 'steam_turbine', 'eln:steam_turbine', ['eln:steam_turbine'], 'Big Iron', 'Mind the rads.'),
 ('machine_block', 'copper_cable', 'eln:machine_block', ['eln:machine_block'], 'Machine Shop', 'Most machines start as a block.'),
 ('macerator', 'machine_block', 'eln:50v_macerator', ['eln:50v_macerator', 'eln:200v_macerator'], 'Grinding Gears', 'Better than cheap magic. (Worse than expensive magic.)'),
 ('electrical_furnace', 'machine_block', 'eln:electrical_furnace', ['eln:electrical_furnace'], 'Electric Oven', 'An oven for your kitchen. Put your roast next to your ore!'),
 ('auto_miner', 'macerator', 'eln:auto_miner', ['eln:auto_miner'], 'Hands Off', 'Let machines do the mining, while you do the cooking.'),
 ('cable_kit', 'copper_cable', 'eln:cable_kit_lv', ['eln:cable_kit_signal', 'eln:cable_kit_lv', 'eln:cable_kit_mv', 'eln:cable_kit_hv', 'eln:cable_kit_vhv', 'eln:cable_kit_thermal'], 'Bundled', 'Sensible electricians bundle their cables.'),
 ('lead', 'copper_cable', 'eln:raw_lead', ['eln:raw_lead', 'eln:lead_ore', 'eln:deepslate_lead_ore'], 'Heavy Metal', 'Lead hides down below.'),
 ('battery', 'lead', 'eln:cost_oriented_battery', ['eln:cost_oriented_battery', 'eln:capacity_oriented_battery', 'eln:voltage_oriented_battery', 'eln:current_oriented_battery', 'eln:life_oriented_battery'], 'Bottled Lightning', 'Don\'t stress them.'),
 ('solar', 'steam_turbine', 'eln:small_solar_panel', ['eln:small_solar_panel', 'eln:small_rotating_solar_panel', 'eln:2x3_solar_panel', 'eln:2x3_rotating_solar_panel'], 'Sunbather', 'Free power, then no power.'),
 ('chisel', 'cable_kit', 'eln:chisel', ['eln:chisel'], 'Chip Off the Old Block', 'It\'s not a wrench. Press C anyway.'),
 ('multimeter', 'copper_cable', 'eln:multimeter', ['eln:multimeter'], 'Measure Twice', 'Can you imagine needing one of these?'),
 ('converter', 'steam_turbine', 'eln:dc_dc_converter', ['eln:dc_dc_converter'], 'Step Up', 'Place the cables, then connect the power.'),
 ('fuse', 'converter', 'eln:electrical_fuse_holder', ['eln:electrical_fuse_holder'], 'Fuse Box', 'A fuse dies so your machines don\'t.'),
 ('lamp', 'steam_turbine', 'eln:lamp_socket_a', ['eln:lamp_socket_a', 'eln:simple_lamp_socket', 'eln:flat_lamp_socket', 'eln:robust_lamp_socket', 'eln:sconce_lamp_socket', 'eln:suspended_lamp_socket', 'eln:long_suspended_lamp_socket', 'eln:fluorescent_lamp_socket'], 'Light Bulb Moment', 'Bulbs wear out.'),
]
lang = []
for key, parent, icon, items, title, desc in A:
    d = {}
    if parent: d['parent'] = 'eln:guide/' + parent
    d['criteria'] = {'has': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': items if len(items) > 1 else items[0]}]}}}
    disp = {'icon': {'id': icon}, 'title': {'translate': f'advancements.eln.guide.{key}.title'},
            'description': {'translate': f'advancements.eln.guide.{key}.description'}, 'announce_to_chat': False}
    if not parent:
        disp['background'] = 'minecraft:gui/advancements/backgrounds/stone'
        disp['show_toast'] = False
    d['display'] = disp
    d['requirements'] = [['has']]
    with open(os.path.join(out, key + '.json'), 'w') as f:
        json.dump(d, f, indent=2)
        f.write('\n')
    lang.append((f'advancements.eln.guide.{key}.title', title))
    lang.append((f'advancements.eln.guide.{key}.description', desc))
for k, v in lang:
    print(f'        add("{k}", "{v}");')
