#!/bin/sh
# Render reference sheets of one mod model, all parts: tools/render_all.sh <modeldir>/<obj>.obj
# (relative to src/main/resources/assets/eln/model).  Output: refs/<modeldir>__<obj>_*.png (not committed).
# Needs Blender: nix develop .#refs
ROOT=$(cd "$(dirname "$0")/.." && pwd)
MODELS=$(git -C "$ROOT" rev-parse --show-toplevel)/src/main/resources/assets/eln/model
rel="$1"; name=$(echo "$rel" | sed 's#/#__#g; s#\.obj$##')
mkdir -p "$ROOT/refs"
RES=${RES:-320} blender -b -P "$ROOT/tools/render_ref.py" -- "$MODELS/$rel" "$ROOT/refs/$name" ALL > /dev/null 2>&1 || echo "FAIL $rel"
