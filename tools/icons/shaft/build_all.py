#!/usr/bin/env python3
"""Rebuild every shaft-family icon and the contact sheet.
  nix-shell -p 'python3.withPackages(p:[p.pillow p.numpy])' --run 'python3 tools/icons/shaft/build_all.py'
draw_devices.py: the 15 device icons (drawn after the OBJ models); draw_items.py: the 4 non-placeable items;
sheet.py: notes/ea/shaft/p5-previews/icons_sheet.png. render_icons.py (the automatic reduction + oblique reference
sheet) and peek.py / ascii.py are working tools and not part of the build."""
import os, subprocess, sys
d = os.path.dirname(os.path.abspath(__file__))
for s in ("draw_devices.py", "draw_items.py", "sheet.py"):
    subprocess.run([sys.executable, os.path.join(d, s)], check=True)
