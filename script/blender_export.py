import bpy
import os

for dirpath, dirnames, filenames in os.walk('src/main/resources'):
  for name in filenames:
    if name.endswith('.blend'):
      src = os.path.join(dirpath, name)
      obj = src[:-5] + 'obj'
      if (not os.path.exists(obj)) or (os.path.getmtime(obj) < os.path.getmtime(src)):
        bpy.ops.wm.open_mainfile(filepath=src)
        bpy.ops.export_scene.obj(
          filepath=obj,
          axis_forward='-Z',
          axis_up='X',
          use_triangles=True)
