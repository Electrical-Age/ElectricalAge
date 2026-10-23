#!/usr/bin/env python3
"""Build the schematic glyph set: icons/<item_id>.py -> out/<item_id>.png

Usage (artists: ALWAYS name your own ids or your --family; the bare full build is the orchestrator's)
    python3 build.py and_chip power_capacitor        # a subset
    python3 build.py --family lamps                  # the in-scope ids of a manifest family
    python3 build.py --family lamps --contact /tmp/sheet.png   # + inventory contact sheet
    python3 build.py --list-missing                  # in-scope manifest ids without icons/<id>.py
    python3 build.py --info 50v_led_bulb             # what the manifest says about an id
    python3 build.py                                 # everything in icons/ (orchestrator)
    options: -j N parallel workers (default: CPU count), -v (bbox / hue stats), --strict (no manifest = FAIL)

Each icon module defines TIER (the manifest's tier, canonical spelling) and draw(), which returns a
kit.Glyph (preferred) or a 32x32 PIL image (converted back into a Glyph and checked the same way).

Every module runs in its OWN Python process (a crash, sys.exit(), os._exit(), an endless loop or a
module that edits kit state cannot affect any other icon; timeout 60 s). Results:
    ok    -> out/<id>.png                (written to a temp file, then os.replace()d into place)
    FAIL  -> out/_fail/<id>.png when anything was drawn, and out/<id>.png is REMOVED,
             so out/ only ever holds icons that passed their last build.
With manifest/manifest.json present, an id FAILS when it is missing from the manifest, when its row
lacks `tier` or `schematic_scope`, when schematic_scope is not true, when the manifest tier is not
spelled canonically (signal/lv/mv/hv/vhv/thermal/null), or when the glyph's tier differs from it.
Without the manifest those checks are skipped with one WARN (FAIL with --strict).
Exit status 1 if anything failed. ELN_MANIFEST=path overrides the manifest location (testing).
"""
import argparse
import json
import os
import subprocess
import sys
import tempfile
import traceback
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MANIFEST = os.environ.get('ELN_MANIFEST', os.path.join(ROOT, 'manifest', 'manifest.json'))
OUT = os.path.join(HERE, 'out')
FAIL_DIR = os.path.join(OUT, '_fail')
TMP_DIR = os.path.join(OUT, '_tmp')
RESULT = '@@BUILD-RESULT@@ '
sys.path.insert(0, os.path.join(ROOT, 'tools'))
from tiers import normalize_tier, CANONICAL   # noqa: E402


# ---- worker: runs ONE module in a fresh interpreter -------------------------------------------------
def worker(item_id, tmp_png):
    """Load icons/<id>.py, draw, check, render to tmp_png. Prints one RESULT json line."""
    res = {'id': item_id, 'ok': False, 'msg': '', 'warnings': [], 'drawn': False, 'tier': None, 'stats': None}
    try:
        sys.path.insert(0, HERE)                   # `import kit`, `from families import x`
        import importlib.util
        import kit
        from PIL import Image
        path = os.path.join(HERE, 'icons', item_id + '.py')
        spec = importlib.util.spec_from_file_location('icon_' + item_id, path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        out = mod.draw()
        mod_tier = getattr(mod, 'TIER', '<missing>')
        if mod_tier != '<missing>' and mod_tier is not None and not isinstance(mod_tier, (str, int)):
            raise TypeError(f'TIER must be a canonical tier key string or None, not {type(mod_tier).__name__}')
        if isinstance(out, Image.Image):
            g = kit.Glyph.from_image(out, tier=None if mod_tier == '<missing>' else mod_tier)
        elif isinstance(out, kit.Glyph):
            g = out
        else:
            raise TypeError(f'draw() returned {type(out).__name__}; expected kit.Glyph or a 32x32 PIL image')
        res['tier'] = g.tier
        errs = []
        if mod_tier == '<missing>':
            errs.append('module defines no TIER constant (the manifest tier; None if untiered)')
        elif normalize_tier(mod_tier) != normalize_tier(g.tier):
            errs.append(f'module TIER {mod_tier!r} != Glyph(tier={g.tier!r})')
        try:
            stats = g.check(item_id)
            res['warnings'] = stats['warnings']
            res['stats'] = {'bbox': stats['bbox'], 'hue_px': stats['hue_px']}
        except kit.RuleBreak as e:
            errs.append('RULE BREAK ' + str(e))
            res['warnings'] = e.warnings
        if g.px:
            g.render().save(tmp_png)
            res['drawn'] = True
        res['ok'] = not errs
        res['msg'] = '; '.join(errs)
    except KeyboardInterrupt:
        raise
    except BaseException as e:                     # SystemExit included
        tb = [f for f in traceback.extract_tb(e.__traceback__) if '/icons/' in f.filename or '/families/' in f.filename]
        where = ''.join(f'\n    at {os.path.relpath(f.filename, HERE)}:{f.lineno}: {f.line}' for f in tb[-2:])
        res['msg'] = f'ERROR in module: {type(e).__name__}: {e}{where}'
    sys.stdout.write('\n' + RESULT + json.dumps(res) + '\n')
    sys.stdout.flush()
    os._exit(0)                                    # skip atexit handlers a module may have registered


def run_worker(item_id, timeout=60):
    os.makedirs(TMP_DIR, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=item_id + '.', suffix='.png', dir=TMP_DIR)
    os.close(fd)
    os.unlink(tmp)
    try:
        p = subprocess.run([sys.executable, os.path.abspath(__file__), '--worker', item_id, tmp],
                           capture_output=True, text=True, timeout=timeout, cwd=HERE)
        lines = [ln for ln in p.stdout.splitlines() if ln.startswith(RESULT)]
        if lines:
            res = json.loads(lines[-1][len(RESULT):])
        else:
            tail = (p.stderr or p.stdout).strip().splitlines()[-6:]
            res = {'id': item_id, 'ok': False, 'drawn': False, 'warnings': [], 'tier': None, 'stats': None,
                   'msg': f'ERROR: module process ended without a result (exit {p.returncode})\n  ' + '\n  '.join(tail)}
    except subprocess.TimeoutExpired:
        res = {'id': item_id, 'ok': False, 'drawn': False, 'warnings': [], 'tier': None, 'stats': None,
               'msg': f'ERROR: timeout after {timeout}s (endless loop?)'}
    res['tmp'] = tmp
    return res


# ---- manifest -------------------------------------------------------------------------------------
def load_manifest():
    if not os.path.exists(MANIFEST):
        return None
    with open(MANIFEST) as f:
        data = json.load(f)
    items = data.get('items', data) if isinstance(data, dict) else data
    if isinstance(items, dict):        # {item_id: {...}}
        items = [dict(v, item_id=k) for k, v in items.items()]
    return {it['item_id']: it for it in items}


def scope_of(it):
    """True / False, or None when the key is missing or not a boolean."""
    v = it.get('schematic_scope')
    return v if isinstance(v, bool) else None


def manifest_errors(item_id, tier, manifest):
    it = manifest.get(item_id)
    if it is None:
        return [f'{item_id!r} is not in the manifest (wrong id?)']
    errs = []
    if 'schematic_scope' not in it or scope_of(it) is None:
        errs.append('manifest row has no boolean schematic_scope')
    elif not it['schematic_scope']:
        errs.append('manifest says schematic_scope=false: this item falls through to the vanilla art; do not draw it')
    if 'tier' not in it:
        errs.append('manifest row has no tier key')
    else:
        mt = it['tier']
        if mt is not None and mt not in CANONICAL:
            errs.append(f'manifest tier {mt!r} is not canonical {CANONICAL + (None,)}')
        else:
            try:
                gt = normalize_tier(tier)
            except KeyError as e:
                gt = ('?', str(e))
            if gt != mt:
                errs.append(f'glyph tier {tier!r} != manifest tier {mt!r} (colour = tier)')
    return errs


# ---- placement ------------------------------------------------------------------------------------
def place(item_id, res):
    """Move the rendered temp PNG to out/ (ok) or out/_fail/ (failed); keep out/ = passing icons only."""
    good = os.path.join(OUT, item_id + '.png')
    bad = os.path.join(FAIL_DIR, item_id + '.png')
    tmp = res.get('tmp')
    have_tmp = tmp and os.path.exists(tmp)
    if res['ok']:
        os.replace(tmp, good)
        if os.path.exists(bad):
            os.unlink(bad)
    else:
        if os.path.exists(good):
            os.unlink(good)
        if have_tmp:
            os.makedirs(FAIL_DIR, exist_ok=True)
            os.replace(tmp, bad)


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('ids', nargs='*', help='item ids (default: every icons/*.py)')
    ap.add_argument('--family', action='append', default=[], help='build the in-scope ids of this manifest family')
    ap.add_argument('--contact', metavar='OUT.png', help='write a contact sheet of the icons that passed')
    ap.add_argument('--list-missing', action='store_true', help='list in-scope ids without a module')
    ap.add_argument('--info', metavar='ID', help="print the manifest row of an id")
    ap.add_argument('--strict', action='store_true', help='a missing manifest is a FAIL')
    ap.add_argument('-j', '--jobs', type=int, default=os.cpu_count() or 4)
    ap.add_argument('-v', '--verbose', action='store_true')
    a = ap.parse_args(argv)

    manifest = load_manifest()
    have = sorted(f[:-3] for f in os.listdir(os.path.join(HERE, 'icons'))
                  if f.endswith('.py') and not f.startswith('_'))
    if a.info:
        if manifest is None:
            print(f'no manifest at {MANIFEST}')
            return 2
        print(json.dumps(manifest.get(a.info, f'{a.info!r} not in manifest'), indent=1))
        return 0
    if a.list_missing:
        if manifest is None:
            print(f'no manifest at {MANIFEST}')
            return 2
        miss = sorted(i for i, it in manifest.items() if scope_of(it) and i not in have)
        print('\n'.join(miss) or '(none)')
        return 0

    ids = list(a.ids)
    for fam in a.family:
        if manifest is None:
            print(f'--family needs {MANIFEST}, which does not exist yet; pass ids instead')
            return 2
        fam_ids = sorted(i for i, it in manifest.items() if it.get('family') == fam and scope_of(it))
        if not fam_ids:
            print(f'family {fam!r}: no in-scope items in the manifest')
        ids += fam_ids
    if not ids and not a.family:
        ids = have
    ids = list(dict.fromkeys(ids))
    os.makedirs(OUT, exist_ok=True)

    fails = 0
    if manifest is None:
        print(f'WARN no manifest at {MANIFEST}: tier and scope are NOT checked'
              + (' (FAIL: --strict)' if a.strict else ''))
        if a.strict:
            fails += 1
    todo = [i for i in ids if i in have]
    for i in ids:
        if i not in have:
            print(f'{i:44s} FAIL MISSING icons/{i}.py')
            fails += 1
    passed = []
    with ThreadPoolExecutor(max_workers=max(1, a.jobs)) as ex:
        for res in ex.map(run_worker, todo):
            item_id = res['id']
            if manifest is not None and res['msg'].startswith('ERROR') is False:
                merr = manifest_errors(item_id, res['tier'], manifest)
                if merr:
                    res['ok'] = False
                    res['msg'] = '; '.join(filter(None, [res['msg']] + merr))
            place(item_id, res)
            info = ''
            if a.verbose and res.get('stats'):
                info = f"bbox {tuple(res['stats']['bbox'])} hue_px {res['stats']['hue_px']}"
            print(f"{item_id:44s} {'ok  ' if res['ok'] else 'FAIL'} {res['msg'] or info}".rstrip())
            for w in res['warnings']:
                print(f"{'':44s} WARN {w}")
            if res['ok']:
                passed.append(item_id)
            else:
                fails += 1
    try:
        os.rmdir(TMP_DIR)                          # only succeeds when no other build is running
    except OSError:
        pass
    print(f'{len(passed)}/{len(ids)} ok' + (f', {fails} failed' if fails else ''))
    if a.contact and passed:
        subprocess.run([sys.executable, os.path.join(ROOT, 'tools', 'contact.py'), a.contact, OUT, *passed])
    return 1 if fails else 0


if __name__ == '__main__':
    if len(sys.argv) >= 2 and sys.argv[1] == '--worker':
        worker(sys.argv[2], sys.argv[3])
    sys.exit(main(sys.argv[1:]))
