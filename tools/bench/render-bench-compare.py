#!/usr/bin/env python3
"""Compare two builds' render benchmark results (todo #424; docs/testing.md "Render benchmark").

  render-bench-compare.py A.json[,A2.json...] [B.json[,B2.json...]] [--band PCT] [--top N]

Each side is one run or several of the same build (comma-separated; times are the median over them). Both tiers:
- CPU tier (runGameTestServer -PelnRenderBench=FILE): totals; every device whose exact fields (vertices, what the
  chunk takes, what the renderer keeps, settled calls) differ between the sides or between runs of one side; devices
  whose times differ by more than the band (default 25 %); the N devices that cost most per draw after the bake.
  Exit 1 when exact fields differ between A and B.
- Client tier (-Deln.renderBench=FILE in a client): per view fps, frame and EA milliseconds, EA's share, vertices
  written a frame; warnings when the runs are capped or their settings differ.
With one side only, it is summarised alone.
"""
import json, statistics, sys

EXACT = ['vertices', 'rendererVertices', 'chunkVertices', 'meshVertices', 'settledCalls']
TIMES = ['recordNs', 'packNs', 'bakedRecordNs', 'meshNs']


def load(arg):
    runs = [json.load(open(f)) for f in arg.split(',')]
    kinds = {r.get('bench') for r in runs}
    if len(kinds) != 1:
        sys.exit(f"{arg}: mixed kinds of result {kinds}")
    return runs


def med(values):
    return statistics.median(values)


def pct(a, b):
    return 'n/a' if not a else f"{100.0 * (b - a) / a:+.1f} %"


def name_of(runs):
    r = runs[0]
    return f"{r.get('label', '?')} ({r.get('commit', '?')}, {len(runs)} run{'s' if len(runs) > 1 else ''})"


def keyed(devices):
    """key -> device; two devices may share a name (two "Signal Switch"): the second is "Signal Switch #2"."""
    out = {}
    for d in devices:
        key, n = d['name'], 1
        while key in out:
            n += 1
            key = f"{d['name']} #{n}"
        out[key] = dict(d, name=key)
    return out


def cpu_devices(runs, notes, side):
    """key -> fields: exact ones from the first run (a note when runs disagree), times as medians."""
    out = keyed(runs[0]['devices'])
    rest = [keyed(r['devices']) for r in runs[1:]]
    for name, d in out.items():
        others = [r.get(name) for r in rest]
        for k in EXACT:
            vals = {d[k]} | {o[k] for o in others if o}
            if len(vals) > 1:
                notes.append(f"{side}: {name}: {k} differs between runs of the same build: {sorted(vals)}")
        for k in TIMES:
            d[k] = med([d[k]] + [o[k] for o in others if o])
    return out


def cpu(a_runs, b_runs, band, top):
    notes = []
    a = cpu_devices(a_runs, notes, 'A')
    print(f"CPU tier. A = {name_of(a_runs)}" + (f", B = {name_of(b_runs)}" if b_runs else ''))
    differ = 0
    if b_runs:
        b = cpu_devices(b_runs, notes, 'B')
        print("\n| total over devices | A | B | change |\n|---|---|---|---|")
        for k in ['devices'] + EXACT[:4] + TIMES:
            va, vb = (med(r['totals'][k] for r in runs) for runs in (a_runs, b_runs))
            unit = ' us' if k.endswith('Ns') else ''
            fa, fb = (f"{v / 1000:.1f}" if unit else f"{v:.0f}" for v in (va, vb))
            print(f"| {k[:-2] if unit else k}{unit} | {fa} | {fb} | {pct(va, vb)} |")
        sa, sb = (med(r['totals']['chunkShare'] for r in runs) for runs in (a_runs, b_runs))
        print(f"| share in the chunk | {100 * sa:.2f} % | {100 * sb:.2f} % | |")
        for name in sorted(set(a) | set(b)):
            if name not in a or name not in b:
                print(f"\nEXACT: {name} is only in {'A' if name in a else 'B'}")
                differ += 1
                continue
            diffs = [f"{k} {a[name][k]} -> {b[name][k]}" for k in EXACT if a[name][k] != b[name][k]]
            if diffs:
                differ += 1
                print(f"EXACT: {name}: " + ', '.join(diffs))
        print(f"\n{differ} device(s) differ in exact fields." if differ else "\nExact fields: the same for every device.")
        slow = []
        for name in sorted(set(a) & set(b)):
            for k in ['recordNs', 'bakedRecordNs', 'meshNs']:
                va, vb = a[name][k], b[name][k]
                if abs(vb - va) > 500 and va and abs(vb - va) / va * 100 > band:
                    slow.append((abs(vb - va), f"TIME: {name}: {k} {va:.0f} -> {vb:.0f} ns ({pct(va, vb)})"))
        for _, line in sorted(slow, reverse=True)[:40]:
            print(line)
        print(f"{len(slow)} time(s) outside the {band:.0f} % band (times are loose: rerun before believing one).")
    last = b if b_runs else a
    print(f"\nThe {top} devices that cost most per draw with their parts in the chunk ({'B' if b_runs else 'A'}):")
    print("| device | vertices | left with the renderer | ns/draw unbaked | ns/draw baked | settled at frame |\n|---|---|---|---|---|---|")
    for d in sorted(last.values(), key=lambda d: -d['bakedRecordNs'])[:top]:
        print(f"| {d['name']} | {d['vertices']} | {d['rendererVertices']} | {d['recordNs']:.0f} | {d['bakedRecordNs']:.0f} | {d['settleFrame']} |")
    for n in notes:
        print("NOTE " + n)
    return 1 if differ else 0


def client_views(runs):
    out = {}
    for v in runs[0]['views']:
        same = [next((x for x in r['views'] if x['name'] == v['name']), None) for r in runs]
        same = [x for x in same if x]
        out[v['name']] = {
            'fps': med(x['fps'] for x in same),
            'p50': med(x['frameMs']['p50'] for x in same), 'p95': med(x['frameMs']['p95'] for x in same),
            'p99': med(x['frameMs']['p99'] for x in same),
            'ea50': med(x['eaMs']['p50'] for x in same), 'ea95': med(x['eaMs']['p95'] for x in same),
            'share': med(x['eaShare'] for x in same),
            'verts': med(x['perFrame']['writeVertices'] for x in same),
            'devices': med(x['perFrame']['devices'] for x in same),
            'inChunk': med(x['bake']['settledVertices'] for x in same),
            'paused': max(x['pausedFrames'] for x in same),
            'settling': max(abs(x['bake']['settledVertices'] - x['bake']['settledVerticesBefore']) for x in same),
        }
    return out


COLS = [('fps', 'fps', '{:.1f}'), ('p50', 'frame p50 ms', '{:.3f}'), ('p95', 'p95 ms', '{:.3f}'), ('p99', 'p99 ms', '{:.3f}'),
        ('ea50', 'EA p50 ms', '{:.3f}'), ('ea95', 'EA p95 ms', '{:.3f}'), ('share', 'EA share', '{:.3f}'),
        ('verts', 'vertices written/frame', '{:.0f}'), ('devices', 'devices/frame', '{:.1f}'), ('inChunk', 'vertices in chunks', '{:.0f}')]


def client(a_runs, b_runs):
    print(f"Client tier. A = {name_of(a_runs)}" + (f", B = {name_of(b_runs)}" if b_runs else ''))
    sides = [('A', a_runs)] + ([('B', b_runs)] if b_runs else [])
    for side, runs in sides:
        for r in runs:
            if r.get('capped'):
                print(f"WARNING {side}: capped (vsync {r.get('vsync')}, limit {r.get('framerateLimit')}): fps and frame ms show the cap, not the cost")
            if r.get('bakeStats'):
                print(f"WARNING {side}: ELN_BAKE_STATS was on: its bookkeeping is in EA's times")
    if b_runs:
        for k in ['width', 'height', 'renderDistance', 'gpu', 'backend', 'bakeMode', 'measureSeconds', 'settleSeconds']:
            va, vb = a_runs[0].get(k), b_runs[0].get(k)
            if va != vb:
                print(f"{'NOTE' if k == 'bakeMode' else 'WARNING'}: {k} differs: A {va}, B {vb}")
    r0 = a_runs[0]
    print(f"{r0.get('gpu')} ({r0.get('backend')}), {r0.get('width')}x{r0.get('height')}, render distance {r0.get('renderDistance')}, "
          f"{r0.get('measureSeconds')} s measured after {r0.get('settleSeconds')} s")
    a = client_views(a_runs)
    b = client_views(b_runs) if b_runs else {}
    for name, va in a.items():
        vb = b.get(name)
        print(f"\nView {name}:")
        print("| | A |" + (" B | change |" if vb else '') + "\n|---|---|" + ("---|---|" if vb else ''))
        for key, title, fmt in COLS:
            row = f"| {title} | {fmt.format(va[key])} |"
            if vb:
                row += f" {fmt.format(vb[key])} | {pct(va[key], vb[key])} |"
            print(row)
        for side, v in (('A', va), ('B', vb)):
            if v and v['paused']:
                print(f"WARNING {side}: {v['paused']} frames with the game paused (window not focused?)")
            if v and v['settling']:
                print(f"NOTE {side}: the chunk's share changed by {v['settling']} vertices while measured (parts still settling or moving)")
    return 0


def main():
    args = [x for x in sys.argv[1:]]
    band, top = 25.0, 15
    for flag in ('--band', '--top'):
        if flag in args:
            i = args.index(flag)
            value = float(args[i + 1])
            del args[i:i + 2]
            if flag == '--band':
                band = value
            else:
                top = int(value)
    if not 1 <= len(args) <= 2:
        sys.exit(__doc__)
    a = load(args[0])
    b = load(args[1]) if len(args) == 2 else None
    kind = a[0].get('bench')
    if b and b[0].get('bench') != kind:
        sys.exit("the two sides are different kinds of result")
    if kind == 'eln-render-cpu':
        sys.exit(cpu(a, b, band, top))
    if kind == 'eln-render-client':
        sys.exit(client(a, b))
    sys.exit(f"not a render benchmark result: bench = {kind}")


main()
