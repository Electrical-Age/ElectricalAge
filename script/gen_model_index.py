#!/usr/bin/env python3
"""Write src/main/resources/assets/eln/model/index.txt: every OBJ model path under assets/eln/model (relative to it).
Obj3DFolder reads it to load all models (scanning the mod jar is not possible under the 26.x module system).
Re-run after adding or removing models."""
import os
root = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'eln', 'model')
paths = sorted(os.path.relpath(os.path.join(d, f), root).replace(os.sep, '/')
               for d, _, fs in os.walk(root) for f in fs if f.lower().endswith('.obj'))
open(os.path.join(root, 'index.txt'), 'w').write('\n'.join(paths) + '\n')
print(len(paths), 'models')
