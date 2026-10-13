#!/usr/bin/env python3
"""Build the vanilla icon set: icons/<item_id>.py -> out/<item_id>.png.

Usage (from anywhere):
    python3 build.py                        build every icon module
    python3 build.py and_chip multimeter    build only these ids          <- artists: always this
    python3 build.py --family chips         build one manifest family (needs manifest/manifest.json)
    python3 build.py --list [--family F]    list item ids with module / built status
    python3 build.py --strict ...           exit non-zero on lint warnings as well as errors
    python3 build.py --contact SHEET.png ...  also write a contact sheet of what was built
    python3 build.py --scratch NAME ...     build icons/NAME.py even though NAME is not an item id;
                                            written to scratch/NAME.png, never to out/
    python3 build.py -j 8 --timeout 60      parallel workers / seconds per icon

Each icon module exposes ``draw()`` returning a ``kit.Canvas`` or a PIL RGBA image.  Every icon
is rendered in its OWN child process (so a module cannot change shared state for another, and a
crash, ``sys.exit`` or endless loop only fails that icon), linted (``kit.check.lint``) and
written atomically.  If an icon fails, its old out/<id>.png is deleted, so integration never
ships stale art.

Size: an icon must be exactly ``vanilla_size`` (manifest, else ``kit.decisions.vanilla_size``:
16, or 32 for the families decided centrally).  Anything else is an ERROR.
Ids: only registry item ids (manifest/manifest.json, else manifest/base.json) are written to
out/; other names need --scratch.

Run in the repo's dev shell (direnv or nix develop):  python3 build.py ...
"""
import argparse
import concurrent.futures as cf
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ICONS = os.path.join(HERE, 'icons')
OUT = os.path.join(HERE, 'out')
SCRATCH = os.path.join(HERE, 'scratch')
MANIFEST = os.path.join(ROOT, 'manifest', 'manifest.json')
BASE = os.path.join(ROOT, 'manifest', 'base.json')


def _load(p):
    with open(p) as f:
        data = json.load(f)
    if isinstance(data, dict):
        data = data.get('items', list(data.values()))
    return {e['item_id']: e for e in data if isinstance(e, dict) and 'item_id' in e}


def load_manifest():
    """(entries, path, full): manifest.json if present (full=True), else base.json (ids only)."""
    if os.path.exists(MANIFEST):
        return _load(MANIFEST), MANIFEST, True
    if os.path.exists(BASE):
        return _load(BASE), BASE, False
    return {}, None, False


def module_ids():
    return sorted(f[:-3] for f in os.listdir(ICONS) if f.endswith('.py') and not f.startswith('_'))


# ----------------------------------------------------------------------------- worker side
def worker(ident, dest, expected):
    """Runs in the child: import icons/<ident>.py, draw, lint, save to ``dest``.
    Prints one JSON line {ok, msgs}."""
    import importlib.util
    import traceback
    sys.path.insert(0, HERE)
    msgs = []
    ok = False
    try:
        from kit import Canvas, lint
        path = os.path.join(ICONS, ident + '.py')
        spec = importlib.util.spec_from_file_location('icon_' + ident, path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        res = mod.draw()
        if isinstance(res, Canvas):
            im = res.image()
        elif hasattr(res, 'convert') and hasattr(res, 'size'):
            im = res.convert('RGBA')
        else:
            raise TypeError(f'draw() returned {type(res).__name__}, not Canvas/PIL image')
        msgs = lint(im, expected, getattr(mod, 'LINT_SKIP', ()))
        if not any(lv == 'ERROR' for lv, _ in msgs):
            im.save(dest, format='PNG')
            ok = True
    except BaseException as e:                      # SystemExit included: report, do not die
        if isinstance(e, KeyboardInterrupt):
            raise
        tb = traceback.format_exc().strip().splitlines()
        msgs.append(('ERROR', 'draw() raised:\n      ' + '\n      '.join(tb[-6:])))
    sys.stdout.write('\n@@RESULT@@' + json.dumps({'ok': ok, 'msgs': msgs}) + '\n')
    sys.stdout.flush()
    os._exit(0)


# ----------------------------------------------------------------------------- parent side
def build_one(ident, manifest, full, scratch, timeout):
    """Render one icon in a child process.  Returns (ok, messages, written_path or None)."""
    from kit.decisions import vanilla_size
    msgs = []
    path = os.path.join(ICONS, ident + '.py')
    is_item = ident in manifest
    dest_dir = SCRATCH if (scratch and not is_item) else OUT
    final = os.path.join(dest_dir, ident + '.png')
    if not os.path.exists(path):
        return False, [('ERROR', 'no module icons/%s.py' % ident)], None
    if manifest and not is_item and not scratch:
        return False, [('ERROR', 'not a registry item id (manifest); refusing to write out/. '
                                 'Use --scratch for experiments.')], None
    entry = manifest.get(ident) or {}
    expected = vanilla_size(ident)
    if full and entry.get('vanilla_size') is not None:
        if int(entry['vanilla_size']) != expected:
            msgs.append(('WARN', f"manifest vanilla_size={entry['vanilla_size']} overrides "
                                 f"kit.decisions ({expected})"))
        expected = int(entry['vanilla_size'])
    os.makedirs(dest_dir, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix='.' + ident + '.', suffix='.png', dir=dest_dir)
    os.close(fd)
    try:
        try:
            p = subprocess.run([sys.executable, os.path.abspath(__file__), '--worker', ident, tmp,
                                str(expected)], capture_output=True, text=True, timeout=timeout,
                               cwd=HERE)
        except subprocess.TimeoutExpired:
            res = {'ok': False, 'msgs': [('ERROR', f'timed out after {timeout}s')]}
        else:
            marker = p.stdout.rfind('@@RESULT@@')
            if marker < 0:
                tail = ((p.stderr or '') + (p.stdout or '')).strip().splitlines()[-6:]
                res = {'ok': False, 'msgs': [('ERROR', f'worker died (exit {p.returncode}):\n      '
                                              + '\n      '.join(tail))]}
            else:
                res = json.loads(p.stdout[marker + len('@@RESULT@@'):].strip().splitlines()[0])
        msgs += [tuple(m) for m in res['msgs']]
        if res['ok']:
            os.replace(tmp, final)
            return True, msgs, final
        if os.path.exists(final):                   # never leave stale art behind
            os.remove(final)
            msgs.append(('ERROR', f'removed stale {os.path.relpath(final, HERE)}'))
        return False, msgs, None
    finally:
        if os.path.exists(tmp):
            os.remove(tmp)


def main(argv):
    if argv[:1] == ['--worker']:
        worker(argv[1], argv[2], int(argv[3]))
        return 0
    sys.path.insert(0, HERE)
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('ids', nargs='*', help='item ids to build (default: every module)')
    ap.add_argument('--family', help='build every id of this manifest family')
    ap.add_argument('--list', action='store_true', help='list ids and build status')
    ap.add_argument('--strict', action='store_true', help='warnings fail the build too')
    ap.add_argument('--contact', metavar='SHEET.png', help='write a contact sheet of the built icons')
    ap.add_argument('--scratch', action='store_true',
                    help='allow non-item names; they go to scratch/, never out/')
    ap.add_argument('-j', '--jobs', type=int, default=os.cpu_count() or 4)
    ap.add_argument('--timeout', type=float, default=60.0, help='seconds per icon')
    a = ap.parse_args(argv)

    manifest, mpath, full = load_manifest()
    mods = module_ids()
    if a.family:
        if not full:
            print('build: --family needs manifest/manifest.json, which does not exist yet')
            return 2
        want = sorted(k for k, e in manifest.items() if e.get('family') == a.family)
        if not want:
            print(f'build: no items with family {a.family!r} in the manifest')
            return 2
        missing = [i for i in want if i not in mods]
        if missing and not a.list:
            print(f'build: {len(missing)} item(s) of {a.family} have no module yet: ' + ' '.join(missing))
        ids = want if a.list else [i for i in want if i in mods]
    else:
        ids = a.ids or mods
    if a.list:
        for i in (ids if (a.family or a.ids) else sorted(set(manifest) | set(mods))):
            built = os.path.exists(os.path.join(OUT, i + '.png'))
            tag = ('built ' if built else 'module' if i in mods else '      ')
            fam = (manifest.get(i) or {}).get('family', '')
            print(f'{tag}  {i}' + (f'   [{fam}]' if fam else '')
                  + ('' if i in manifest else '   (not an item id)'))
        return 0

    ok = bad = warned = 0
    built = []
    with cf.ThreadPoolExecutor(max_workers=max(1, a.jobs)) as ex:
        futs = {i: ex.submit(build_one, i, manifest, full, a.scratch, a.timeout) for i in ids}
        for ident in ids:
            good, msgs, path = futs[ident].result()
            if good:
                ok += 1
                built.append(path)
            else:
                bad += 1
            warned += any(lv == 'WARN' for lv, _ in msgs)
            print(f"{'ok  ' if good else 'FAIL'} {ident}")
            for lv, m in msgs:
                print(f'     {lv}: {m}')
    src = os.path.relpath(mpath, ROOT) if mpath else 'none'
    print(f'build: {ok} built, {bad} failed, {warned} with warnings (ids: {src}'
          + ('' if full else '; sizes from kit.decisions') + ')')
    if a.contact and built:
        tool = os.path.join(ROOT, 'tools', 'contact.py')
        subprocess.run([sys.executable, tool, a.contact, OUT] + built)
    if bad or (a.strict and warned):
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
