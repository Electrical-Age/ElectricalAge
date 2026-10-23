# blender -b -P render_ref.py -- OBJ OUT_PREFIX part1,part2,...|ALL
import bpy, sys, os, math
from mathutils import Vector
argv = sys.argv[sys.argv.index('--')+1:]
obj_path, out, parts = argv[0], argv[1], argv[2]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.wm.obj_import(filepath=obj_path)
d = os.path.dirname(obj_path); files = {f.lower(): f for f in os.listdir(d)}
for img in bpy.data.images:
    p = bpy.path.abspath(img.filepath)
    if not os.path.exists(p):
        alt = files.get(os.path.basename(p).lower())
        if alt: img.filepath = os.path.join(d, alt); img.reload()
for m in bpy.data.materials:
    if m.use_nodes:
        for n in m.node_tree.nodes:
            if n.type == 'TEX_IMAGE': n.interpolation = 'Closest'
open(out + '_parts.txt','w').write('\n'.join(o.name for o in bpy.data.objects if o.type=='MESH'))
keep = None if parts == 'ALL' else set(parts.split(','))
for o in list(bpy.data.objects):
    if o.type != 'MESH': continue
    if 'Minecraft' in o.name or (keep is not None and o.name not in keep):
        bpy.data.objects.remove(o)
objs = [o for o in bpy.data.objects if o.type == 'MESH']
pts = [o.matrix_world @ Vector(c) for o in objs for c in o.bound_box]
lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
ctr = (lo+hi)/2; size = (hi-lo).length
sc = bpy.context.scene
sc.render.engine = 'BLENDER_WORKBENCH'
sc.display.shading.light = 'STUDIO'; sc.display.shading.color_type = 'TEXTURE'
sc.display.shading.show_cavity = True
sc.view_settings.exposure = 0.8
sc.render.film_transparent = True
sc.render.resolution_x = sc.render.resolution_y = int(os.environ.get('RES','384'))
cam_d = bpy.data.cameras.new('c'); cam_d.type = 'ORTHO'; cam_d.ortho_scale = size*1.05
cam = bpy.data.objects.new('c', cam_d); sc.collection.objects.link(cam); sc.camera = cam
views = {'iso_a': (45, 30), 'iso_b': (-45, 30), 'iso_c': (135, 30), 'iso_d': (-135, 30), 'isolow_a': (45, -30), 'isolow_b': (-45, -30), 'isolow_c': (135, -30), 'isolow_d': (-135, -30), 'fy': (0, 0), 'by': (180, 0), 'rx': (90, 0), 'lx': (-90, 0), 'top': (0, 89.9), 'bot': (0, -89.9)}
for name, (yaw, el) in views.items():
    y, e = math.radians(yaw), math.radians(el)
    # Blender Z-up after import; front = -Y
    dirv = Vector((math.sin(y)*math.cos(e), -math.cos(y)*math.cos(e), math.sin(e)))
    cam.location = ctr + dirv*size*3
    cam.rotation_euler = (-dirv).to_track_quat('-Z', 'Y').to_euler()
    sc.render.filepath = f'{out}_{name}.png'
    bpy.ops.render.render(write_still=True)
