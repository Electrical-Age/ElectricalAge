#!/usr/bin/env python3
"""Build the extra (non-registry) sprites: extra/<name>.py -> extra/<name>.png.

These are state sprites that item models switch to (e.g. <id>_on for the flashlights'
eln:lit condition).  They are not item ids, so build.py does not build them.  Each module
exposes draw() like an icon module; it is rendered at 16 px and linted with kit.check.lint
exactly as build.py does (optional LINT_SKIP in the module).

Usage (from vanilla/):
    python3 extra/build_extra.py [--strict] [names...]   (in the repo's dev shell)
"""
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
VANILLA = os.path.dirname(HERE)
sys.path.insert(0, VANILLA)                 # same import root as build.py: kit, families

from kit import Canvas, lint                # noqa: E402

SIZE = 16


def build(name):
    spec = importlib.util.spec_from_file_location('extra_' + name, os.path.join(HERE, name + '.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    res = mod.draw()
    im = res.image() if isinstance(res, Canvas) else res.convert('RGBA')
    msgs = lint(im, SIZE, getattr(mod, 'LINT_SKIP', ()))
    ok = not any(lv == 'ERROR' for lv, _ in msgs)
    if ok:
        im.save(os.path.join(HERE, name + '.png'), format='PNG')
    return ok, msgs


def main(argv):
    strict = '--strict' in argv
    names = [a for a in argv if not a.startswith('--')] or sorted(
        f[:-3] for f in os.listdir(HERE)
        if f.endswith('.py') and not f.startswith('_') and f != os.path.basename(__file__))
    bad = warned = 0
    for n in names:
        try:
            ok, msgs = build(n)
        except Exception as e:
            ok, msgs = False, [('ERROR', f'draw() raised: {e!r}')]
        bad += not ok
        warned += any(lv == 'WARN' for lv, _ in msgs)
        print(f"{'ok  ' if ok else 'FAIL'} {n}")
        for lv, m in msgs:
            print(f'     {lv}: {m}')
    print(f'build_extra: {len(names) - bad} built, {bad} failed, {warned} with warnings')
    return 1 if bad or (strict and warned) else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
