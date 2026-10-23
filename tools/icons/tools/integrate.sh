#!/usr/bin/env bash
# Copy the built icon sets into the mod (vanilla = default textures, schematic = bundled pack).
# Build first: python3 vanilla/build.py, python3 vanilla/extra/build_extra.py, python3 schematic/build.py.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
REPO=$(git -C "$ROOT" rev-parse --show-toplevel)
V=$REPO/src/main/resources/assets/eln/textures/item
P=$REPO/src/main/resources/resourcepacks/schematic_icons/assets/eln/textures/item
mkdir -p "$V" "$P"
rm -f "$V"/*.png "$P"/*.png
cp "$ROOT"/vanilla/out/*.png "$V"/
cp "$ROOT"/vanilla/extra/*_on.png "$V"/
cp "$ROOT"/schematic/out/*.png "$P"/
echo "vanilla: $(ls "$V" | wc -l) files, schematic pack: $(ls "$P" | wc -l) files"
