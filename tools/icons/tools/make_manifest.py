"""Merge manifest/rows_raw.json (foundation agents) + central decisions -> manifest/manifest.json."""
import json, sys, os
sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'vanilla'))
from tiers import normalize_tier
from kit.decisions import tier_cue as _cue
from kit.chip import CHIP_FACES

ROOT = os.path.join(os.path.dirname(__file__), '..')
rows = json.load(open(os.path.join(ROOT, 'manifest/rows_raw.json')))

# Central decisions (orchestrator, 2026-10-11)
VANILLA_32 = set(CHIP_FACES)            # all 21 logic + analog chips; every other family is 16
SCOPE_OFF = {'tree_resin_collector', 'tutorial_sign'}   # placeable but not electrical
TIER_FIX = {'large_rheostat': None}   # VoltageLevelColor is the source of truth (Neutral)
CUE_FIX = {'signal_diode': 'plate'}
NOTE_FIX = {
    'utility_pole': 'Wave-1 finding: the plain pole has no foot (foot is drawn only with the transformer).',
    'hub': 'Wave-1 finding: connector quads are grey (unconnected) or dark (connected), never white.',
    'signal_relay': 'Wave-1 finding: the armature renders white/light grey in the front view.',
    'very_high_voltage_relay': 'Wave-1 finding: the armature renders white/light grey in the front view.',
    'signal_processor': 'Wave-1 finding: the item render pose is the OPEN door (drawItem draw(0.7f, ledDefault)); drawn open.',
    'thermistor': 'Wave-1 finding: same rectangular ResistorCore as power_resistor, not ribbed.',
    'large_rheostat': 'Central decision: untiered (VoltageLevelColor Neutral is the source of truth).',
    'item_creative_tab': 'Central decision: NOT redrawn; keeps the 1.12 elncreativetab texture Baughn restored (commit a4e409f4).',
}
SKIP = {'removed_from_mod_copper_thermal_cable', 'signal_20h_inductor'}   # hidden placeholders keep empty-texture

out = []
for r in rows:
    if r['item_id'] in SKIP:
        continue
    m = {k: r[k] for k in ('item_id', 'display_name', 'family', 'placeable', 'node_kind')}
    t = TIER_FIX.get(r['item_id'], r['tier'])
    m['tier'] = normalize_tier(t)
    m['vanilla_size'] = 32 if r['item_id'] in VANILLA_32 else 16
    m['schematic_scope'] = bool(r['schematic_scope']) and r['item_id'] not in SCOPE_OFF
    m['tier_cue'] = CUE_FIX.get(r['item_id']) or _cue(r['item_id'])   # only items with tier siblings get a vanilla tier cue
    for k in ('model', 'parts', 'model_texture_notes', 'appearance', 'function',
              'current_texture', 'shares_texture_with', 'notes'):
        m[k] = r[k]
    if r['item_id'] in NOTE_FIX:
        m['notes'] = (m['notes'] + ' ' if m['notes'] else '') + NOTE_FIX[r['item_id']]
    m['redraw'] = r['item_id'] != 'item_creative_tab'
    out.append(m)
out.sort(key=lambda m: m['item_id'])
json.dump(out, open(os.path.join(ROOT, 'manifest/manifest.json'), 'w'), indent=1)
print(len(out), 'rows;', sum(m['vanilla_size'] == 32 for m in out), 'at 32;',
      sum(m['schematic_scope'] for m in out), 'in schematic scope')
