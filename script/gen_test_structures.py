#!/usr/bin/env python3
"""Generate GameTest structure templates for the Electrical Age selftest harness (26.x port).

Each SelfTestCase gets an empty box with a stone floor (y = 0) of its own size, like the 1.12 selftest's platform
rows. Writes src/main/resources/data/eln/structure/floor_<w>x<h>x<d>.nbt for the sizes given (default: the set the
harness uses). Structure NBT: DataVersion, size, palette, blocks (only the floor; air is implicit), entities.
Usage: gen_test_structures.py [WxHxD ...]
"""
import gzip, os, struct, sys

DATA_VERSION = 5023  # 26.3 world_version (version.json)
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'eln', 'structure')

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10

def s(x):
    b = x.encode('utf-8')
    return struct.pack('>H', len(b)) + b

def named(tag, name, payload):
    return bytes([tag]) + s(name) + payload

def int_list(vals):
    return bytes([TAG_INT]) + struct.pack('>i', len(vals)) + b''.join(struct.pack('>i', v) for v in vals)

def compound(entries):  # entries: list of bytes (named tags)
    return b''.join(entries) + bytes([TAG_END])

def compound_list(items):
    return bytes([TAG_COMPOUND]) + struct.pack('>i', len(items)) + b''.join(items)

def structure(w, h, d):
    palette = [compound([named(TAG_STRING, 'Name', s('minecraft:stone'))])]
    blocks = [compound([named(TAG_LIST, 'pos', int_list([x, 0, z])), named(TAG_INT, 'state', struct.pack('>i', 0))])
              for x in range(w) for z in range(d)]
    root = compound([
        named(TAG_INT, 'DataVersion', struct.pack('>i', DATA_VERSION)),
        named(TAG_LIST, 'size', int_list([w, h, d])),
        named(TAG_LIST, 'palette', compound_list(palette)),
        named(TAG_LIST, 'blocks', compound_list(blocks)),
        named(TAG_LIST, 'entities', bytes([TAG_END]) + struct.pack('>i', 0)),
    ])
    return bytes([TAG_COMPOUND]) + s('') + root

DEFAULT = ['3x4x3', '8x4x3', '16x4x3']

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for spec in sys.argv[1:] or DEFAULT:
        w, h, d = (int(v) for v in spec.split('x'))
        path = os.path.join(OUT, f'floor_{w}x{h}x{d}.nbt')
        with gzip.open(path, 'wb') as f:
            f.write(structure(w, h, d))
        print(path)
