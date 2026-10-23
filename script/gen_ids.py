#!/usr/bin/env python3
"""Generate src/main/resources/eln-ids.json: the 1.12 item (+ damage) -> 26.x registry name map, from
golden/1.12/inventory.json. Run ONCE, then hand-edit and commit: the file is the source of truth (registry names must
stay stable for saved worlds; display names may change). Collisions get a suffix and must be reviewed.
"""
import json, re, collections, os
root = os.path.join(os.path.dirname(__file__), '..')
inv = json.load(open(os.path.join(root, 'golden', '1.12', 'inventory.json')))
# Display-name collisions, resolved by hand (registry names are forever)
OVERRIDES = {
    'eln:six_node@6211': 'signal_cable_switch',        # ElectricalSwitch "Signal Switch" (6081 is the gate source one)
    'eln:transparent_node@80': 'power_inductor_block',  # vs six-node "Power Inductor"
    'eln:transparent_node@84': 'power_capacitor_block', # vs six-node "Power Capacitor"
}
out, used = {}, collections.Counter()
def slug(name):
    s = re.sub(r'\.name$', '', name)
    s = re.sub(r'[^a-z0-9]+', '_', s.lower()).strip('_')
    return s
for it in inv['items']:
    legacy = it['id'] + '@' + str(it['damage'])
    if it['id'].startswith('eln:') and it.get('descriptorClass') is None and it['damage'] == 0 and 'subId' not in it:
        name = it['id'].split(':')[1]          # plain items (tools, armour, ores block items): keep their 1.12 name
    else:
        name = slug(it['name'])
    name = OVERRIDES.get(legacy, name)
    used[name] += 1
    if used[name] > 1:
        name = f'{name}_{used[name]}'
    out[legacy] = {'name': name, 'display': it['name'], 'kind': it['id'].split(':')[1],
                   'descriptor': (it.get('descriptorClass') or '').split('.')[-1]}
json.dump(out, open(os.path.join(root, 'src', 'main', 'resources', 'eln-ids.json'), 'w'), indent=1, sort_keys=True)
print(len(out), 'ids;', 'suffixed:', [v['name'] for v in out.values() if re.search(r'_\d+$', v['name']) and v['name'].rsplit('_',1)[0] in [x['name'] for x in out.values()]])
