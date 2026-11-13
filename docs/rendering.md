# Rendering

Client rendering of the 26.x port: the GL11 recording facade, block-entity renderers, OBJ models, items, burnt
look, cables, client sounds, X-ray scanner, HUD and probe compat. Wiring is in `client/ElnClient.java`.

## Pipeline (one device, one frame)

Decision (P3 plan): port 1.12's immediate-mode GL code mechanically onto a recorder first; bake into chunk meshes
only where measurements demand it. No raw GL anywhere, so OpenGL and Vulkan backends render identically.

```
BER.extractRenderState  copy element render refs (SixNodeRender: renders[6]; TransparentNodeRender: render)
BER.submit -> GlSubmit.open(poseStack)            GlFrame.open(pose, normal): frame becomes "current"
   1.12 draw code: render.draw() -> descriptor.draw() -> Obj3DPart.draw() / CableRender / GL11.glXxx
   mods.eln.gl.GL11 (static facade) -> GlFrame state machine: matrix stack, colour, texture, flags
   vertices transformed into pose space, grouped into Batch by Key(texture, blend, cull, fullBright, lit)
GlFrame.close()                                     current = DISCARD (stray GL11 calls do nothing)
GlSubmit.submit(frame, pose, collector, light)      per batch: collector.submitCustomGeometry(ElnPipelines.type(key, tex))
   extras: GlFrame.Text -> collector.submitText; ItemStack -> ItemStackRenderState (items on machines); GlFrame.FlatItem -> the FIXED (item-frame) view, lying flat at the matrix (UtilsClient.drawFlatItem: the battery charger's batteries, scaled to their cubicles and thickened 4x; Baughn 2026-10-28)
```

| Class | Side | Role |
|---|---|---|
| `src/main/java/mods/eln/gl/GL11.java` | both (server-safe) | the GL 1.1 subset EA uses: matrices, colour, normals, UVs, glBegin/glEnd (all polygon modes -> quads; lines -> LINES batches), enable/disable TEXTURE_2D/LIGHTING/BLEND/CULL_FACE, blend funcs, display lists, push/pop attrib, `bindTexture(Identifier)`, lightmap override = full-bright |
| `src/main/java/mods/eln/gl/GlFrame.java` | client, render thread only | recorder; `Key`, `Blend {NONE, ALPHA, ADDITIVE, MULTIPLY, LINES}`, `Text`, `Extra`; display lists compiled once into list-space quads and transformed on call; batch pool reset per frame by `beginRenderFrame()` |
| `src/main/java/mods/eln/client/render/GlSubmit.java` | client | frame -> custom geometry; extras submitted from a copy (an EA item drawn inside a frame reopens the shared frame) |
| `src/main/java/mods/eln/client/render/ElnPipelines.java` | client | 16 entity pipelines `eln:pipeline/node_{cull,nocull}_{lit,unlit}_{opaque,alpha,additive,multiply}`: alpha cutout 0.1, unlit = `NO_CARDINAL_LIGHTING`, additive halos without depth write, multiply = soot; RenderTypes memoised per pipeline+texture; blended types get OIT pipelines; registered via `RegisterRenderPipelinesEvent` |
| `src/main/java/mods/eln/client/render/ElnRenderErrors.java` | client | a throwing render is logged once per kind+message, not every frame |
| `src/main/java/mods/eln/misc/UtilsClient.java` | both (server-safe) | 1.12 drawing helpers on the shim; client lookups (player, light, pause) through `UtilsClient.hooks`, set by `ElnClient.ClientHooks` |
| `src/main/java/mods/eln/client/FrameTime.java` | client | `RenderFrameEvent.Pre`: `GlFrame.beginRenderFrame()`, `clientRefresh(deltaT)` on every `NodeBlockEntity.clientList` entry (deltaT capped 0.1 s, 0 when paused), `LoopedSoundPlayer.update` |

## Block-entity renderers and render objects

- BERs registered in `ElnClient` (`EntityRenderersEvent.RegisterRenderers`): `SixNodeRender` for `SIX_NODE_ENTITY` and `VOXEL_NODE_ENTITY` (hosted elements only; `shouldRender` false without elements), `TransparentNodeRender` for `TRANSPARENT_NODE_ENTITY`, `ReplicatorRenderer` for the entity.
- `SixNodeRender` rotates per face (`Direction.glRotateXnRef`, then `-0.5` x) and calls each `SixNodeElementRender.draw()`.
- Render objects: `node/six/SixNodeElementRender.java`, `node/transparent/TransparentNodeElementRender.java`. Created on the client when the publish/description packet names a new element id, through `node/ElnRenders.java` factories; the factories are registered in `client/ClientRenders.java` (by descriptor class, closest superclass wins, or `sixByElement` for shared descriptor classes). A descriptor without a factory draws nothing.
- Render classes are server-safe (the BE holds them and the BE class loads on the server); no client imports in them.
- Extract/submit caveat: `submit` reads the render objects live (not a snapshot). Fine while extract and submit share the render thread in one frame; if submit moves threads, render state must snapshot what `draw()` reads.
- `node/voxel` parts are not drawn per frame: `client/render/VoxelModel.java` bakes them into the chunk mesh (blockstate model type `eln:voxel_node`). See voxel-cables.md.
- The FE exporter blocks are the only device with a static baked block model: `assets/eln/models/block/energy_converter_{lvu,mvu,hvu}.json` (`neoforge:obj`); its render object only carries GUI values.
- Outlines: `client/render/KitOutline.java` (cable kit preview from the server's `KitPreview`), `client/render/ChiselOutline.java` (the chisel's brush box, size and shape, computed on the client from `ChiselItem.brushBox` / `ChiselBrush.box`, the same geometry as carving), both on `ExtractBlockOutlineRenderStateEvent`.

## OBJ models

- Source: `src/main/resources/assets/eln/model/<name>/<name>.obj` (+ `.mtl`, `.txt` properties, textures in the same folder). List of models: `assets/eln/model/index.txt`, generated by `script/gen_model_index.py` (the jar can't be scanned under the module system). Missing index = no models (logged).
- `misc/Obj3DFolder.loadAllElnModels()` runs in the `Eln` ctor on both sides: servers need geometry too (e.g. `Utils.getSixNodePinDistance` in descriptor ctors). `misc/Obj3D.java` parses parts (`Obj3DPart`); drawing compiles each face group into a display list lazily at first draw.
- Textures are bound per model folder (`Obj3D.bindTexture` -> `eln:model/<dir>/<file>`), standalone entity textures, no atlas.

## Items

- Item JSONs are datagen output (`datagen/ElnModels.java` -> `src/generated/resources/assets/eln/items/*.json`). Devices with a 3D draw: `minecraft:select` on `display_context`: `gui` -> flat icon; `ground`/`fixed`/`head` -> `minecraft:special` `eln:device` with `context: "entity"`; everything else -> `context: "hand"`.
- Display poses: `src/main/resources/assets/eln/models/item/device_six.json`, `device_transparent.json`, `device_obj.json` (X-ray scanner via `itemDrawParent()`), as the base of per-item `<name>_3d` models.
- `client/render/ElnItemRenderer.java` (`SpecialModelRenderer`, registered as `eln:device`): opens a GlFrame and calls `GenericItemBlockUsingDamageDescriptor.drawItem(entity)` or `GenericItemUsingDamageDescriptor.drawItem(stack, entity)` when `hasItemDraw()`.
- `client/render/LitProperty.java`: condition `eln:lit` (flashlight on/off icon).
- `client/LampBeam.java`: lamp beams (lit flashlights in either hand, the worn headlamp, the bike's front lamp:
  `LampItem.beamLight`) drawn per pixel by a post effect (`post_effect/lamp_beam.json`, `shaders/post/lamp_beam.fsh`),
  for up to 5 players within 64 blocks, off under an Iris shaderpack. It goes first among the active post effects. The
  beam data is a 128x1 float texture (`eln:textures/effect/beamdata.png`); the level's projection with the view bob
  comes from `mixin/client/GameRendererMixin`. The server lights only the holder's own block (`LampItem.light`);
  1.12's trail of light blocks along the beam is gone (Baughn 2026-11-06). Moved from Stasis Delvers, which drew it
  first.
- `client/render/WornCurios.java` (Curios loaded only): `ICurioRenderer`s draw the worn accessories on players (Baughn,
  todo #166). Battery belt: a band 3 px tall round the waist, 1.2 px off the body (over a chestplate), its faces cut from
  the item icon. Headlamp: its item model on the forehead at 1.15x, following the head. In the helmet slot the headlamp
  is leather armour and draws as a leather cap. Curios' per-slot render toggle hides them.
- Optional built-in resource pack `resourcepacks/schematic_icons` (schematic item icons), added in `ElnClient`.

## Burnt devices

- Publish header bit `HEADER_BURNT` (in `SixNodeElement` / `TransparentNodeElement`) -> render field `burnt`.
- BER draws the device, then draws it again with `frame.soot = true`: everything recorded goes to `Blend.MULTIPLY` in the soot colour (`GlFrame.SOOT`); additive/faint-alpha parts are skipped so glow doesn't darken air.
- `SixNodeElementRender.drawBurnt()` returning true replaces both passes (electrical cables draw their own burnt-through look).
- `NodeBlockEntity.burntSmoke()`: occasional smoke particle; the entity skips the render's `refresh` while burnt (no
  animation, no working particles), and `LoopedSoundPlayer` plays no loop of a burnt render.

## Cables

- Six-node cables: `cable/CableRender.java` (`connectionType`, `drawCable`, `drawKnuckles`), `cable/CableRenderDescriptor.java` (sprite, width, height), drawn per frame through the shim by e.g. `sixnode/electricalcable/ElectricalCableRender.java`. `tileEntityNeighborSpawn()` -> `notifyNeighborSpawn()` would refresh the connection look when a neighbour block entity appears, but nothing calls it in the port.
- `cable/CableTerminals.java` (26.x): devices opt in to floor terminal plates; a floor cable connecting there draws a lug.
- Grid lines: `gridnode/GridRender.java` builds `Catenary` lists from the published links.
- Voxel cables: baked, see voxel-cables.md.

## Sounds (client)

- Looped: render objects describe loops (`sound/LoopedSound.kt`, server-safe) and register them with `addLoopedSound`; `sound/LoopedSoundPlayer.java` (client) ranks the audible loops every half second (volume faded over 16 blocks of linear attenuation) and gives the `MAX_PLAYING` (24) loudest a ticking sound instance that follows its volume/pitch; an instance stops after 2 s of silence or when it drops out of the ranking, so idle machines hold no OpenAL source (the pool is ~247 and a full pool silences vanilla too).
- One-shots: server packet `Eln.packetPlaySound` -> `SoundCommand.clientPlayer` = `sound/SoundClient::play` (linear falloff; 1.12 block muffling not ported). Event names: `sound/ElnSounds.java` + `assets/eln/sounds.json`.
- Decision (Baughn): later, synthesize machine sound client-side from device state instead of server packets.

## X-ray scanner, tutorial sign, probes

- X-ray: `client/XRayScan.java` (client) scans the grid every other client tick for the held, switched-on scanner and installs `XRayScannerDescriptor.pictures`; the item draw shows it. Ray marching in `item/xray/XRayRayMarch.java`, ore matching by tags in `item/xray/XRayFilter.java` (invalidated on `TagsUpdatedEvent`), filter screen `client/gui/XRayScannerScreen.java`.
- Tutorial sign HUD: `sixnode/tutorialsign/TutorialSignHud.java`, GUI layer `eln:tutorial_sign` (registered above all): nearest sign within one block shows its text top-left.
- Jade: `compat/jade/ElnJadePlugin.java` (`@WailaPlugin`, compileOnly dep): server data provider on `NodeBlockEntity`, lines from the device's `getWaila`; details while sneaking or with config `probe.details`. TheOneProbe: not ported (no 26.x build).

## Radial menu

- `client/gui/RadialMenu.java` (the chisel's brush menu; see networking-gui.md): drawn in `extractRenderState` with `GuiGraphicsExtractor.fill` only, one 1-pixel row of spans at a time (the extractor has no arbitrary-polygon fill and its render state is private): translucent dark segments (`0x90101010`), the hovered one lighter, the current choice green, 1-pixel gaps between segments and rings, labels with `centeredText`. No background blur (`extractBackground` is empty), not a pause screen.

## Performance (R6, 2026-10-11, OpenGL and Vulkan alike)

- Per device per frame: LV cable ~1.2 us, macerator ~29 us; cost is per-vertex submission (~11 ns/vertex), recording a macerator ~8 us. Heaviest: Gas Turbine ~18.9k vertices, Steam Turbine ~15k.
- Typical base: ~1-3 ms/frame. Bad case: hundreds of machines or many big turbines in view.
- Voxel cables/pipes (`VoxelModel`) and the FE exporter are block models. The devices' unchanging parts are drawn by the chunk (next section); what moves, blends or glows stays per frame.

## Static parts in the chunk (Baughn 2026-11-12)

A base of machines cost the client most of its frame in device geometry sent again every frame (about 133,000
vertices a frame at the Stasis Delvers workshop). Now the chunk's own mesh draws whatever part of a device does not
change, and the block entity renderer draws the rest. The draw code is untouched: nothing declares a part static.

- How: the renderer still runs the device's draw every frame. `GlSubmit.openBlock` keeps the camera-relative
  translation out of the matrix stack (`GlFrame.openBlock`), so a part's matrix is the same from frame to frame.
  Each display-list replay is a `Bake.Call` (primitive, matrix, colour, texture, cull/fullBright/lit). A call that
  stayed the same for 0.4 s settles: `ChunkBake` adds it to its section and has the section meshed again
  (`AddSectionGeometryEvent`, `BakeMesh.write`). Once the mesh on screen holds it (`Call.shown`), `Device.skip`
  answers true and the renderer leaves that replay out. A call that changes or is not drawn again leaves the section
  in the same frame and the renderer draws it. A part that changes soon after it settled (within 16 times its wait)
  waits twice as long next time, up to 60 s: blinking parts end up with the renderer.
- Never in the chunk: blended draws, soot and ghost (preview) draws, a burnt device, immediate-mode geometry (not a
  display list), lines, texture coordinates outside 0..1 (a sprite cannot repeat), textures made at run time
  (`eln:dynamic/...`, the coil windings), a frame whose pose is more than a translation, and text and item extras.
- Exactly once: a quad is on screen from the renderer or from the chunk, never both or neither. The mesh a section
  shows is found by its compile task's start time: each compile of a section with calls takes a snapshot on the
  render thread, and the mesh on screen holds the last snapshot taken before its task started. A mesh that maps to no
  snapshot counts as holding nothing. `ELN_BAKE_STATS` counts quads a mesh holds and the renderer also drew
  ("stale in meshes": the frames between a part's change and the section's next mesh) and any miscount.
- Section: all of a device's calls go into the section of its block (`Device.home`), however far the model reaches.
  The game draws a block entity only while its own section is drawn, so chunk and renderer show the device under the
  same condition; and a section of nothing but air is never compiled, so a part could not be put where it lies.
- Far away: a block entity is drawn within 64 blocks. Past that the settled parts stay in the chunk (Baughn: keep
  them visible), and what moves or glows is not drawn, as before. Calls only ever come from the renderer's own
  frames, so until the device is drawn again its parts in the chunk are as it last drew them, and a device that was
  never within 64 blocks has none (not done yet: frames for far devices whose state was published).
- Light: not part of a call. The snapshot reads each device's packed light as `BlockEntityRenderState` does, and the
  game meshes a section again when light in it changes. The colour carries the entity shader's shading (two lights
  of 0.6 over 0.4 ambient, the normal a signed byte per axis), since the chunk shader has none.
- Textures: `ModelSprites` (a `SpriteSource` named in `atlases/blocks.json`) stitches `assets/eln/model` and
  `assets/eln/sprites` (not `sprites/gui`) into the block atlas, padded to multiples of 16 pixels, alpha made 0 or
  255 at the renderers' cutoff. Always, whether or not the chunk draws parts: 211 textures made no difference to
  the atlas' size on the Stasis Delvers pack (4096x2048).
- Switches: client config `rendering.staticPartsInChunks` (`config/eln-client.toml`, default true; `ElnClientConfig`),
  applied at once. `-Deln.bake` or `ELN_BAKE` = `off`, `on` or `force` (leave settled parts out without asking
  whether the mesh shows them) wins over it. If writing a section's parts throws, it is logged once and the baking
  switches itself off; reloading the config turns it on again.
- Measuring: `-Deln.bakeStats=true` or `ELN_BAKE_STATS=1` logs every 600 frames where the vertices went (in-chunk,
  const, moving, blended...), per render class, and why constant ones are still with the renderer (changed, waiting,
  waiting longer, no sprite, mesh not shown yet). At the workshop: 80-86% in the chunk.

| Class | Role |
|---|---|
| `gl/Bake.java` | calls, devices, settling; no client classes |
| `gl/BakeMesh.java` | a call's quads as chunk vertices, shaded |
| `gl/BakeStats.java` | the measurement |
| `client/render/bake/ChunkBake.java` | sections, snapshots, which mesh is on screen, the switch |
| `client/render/bake/ModelSprites.java` | the textures in the block atlas |

## Tests

- GameTest `gametest/PublishRenderCase.java`: every device with a render: server publish packet read by a fresh client BE, consumed exactly, draw does not throw and emits vertices.
- `gametest/BakeOnceCheck.java` (run by `publish_render` for every device, and by `autominer_spin` with a part that starts to move): the device drawn with its parts in the chunk shows every quad exactly once, in the same place and colour; see testing.md. `src/test/java/mods/eln/gl/BakeTest.java`: settling, waits, the section, the override.
- `gametest/ItemDrawCase.java`: every 3D item draw records vertices without exceptions. `gametest/XRayCase.java`: X-ray filter, state machine, menu, item draw. `src/test/java/mods/eln/item/xray/XRayRayMarchTest.java`.
- `/elnlab gallery|burn|cables` (`gametest/LabCommands.java`): visual comparison scenes and stress scenes for frame times.

## Gotchas

- Every GL11 call outside an open frame is silently discarded; draw code must run inside a BER/item `submit`.
- `GlFrame` is single-threaded and shared; drawing an EA item inside a device draw reopens it (hence GlSubmit's extras copy).
- A render's `networkUnserialize` must read exactly what the element wrote; a short read garbles the following six-node faces (guarded by `PublishRenderCase`).
- Opaque draws in different states (`Key`s) have no order among themselves: each key is a render type, and the game
  draws a phase's render types in the order of a hash map over them, which differs from launch to launch. Draw code
  that puts two opaque things in the same place and counts on the second covering the first (1.12's lit suspended
  lamp: its head shaded, then again at full light) shows either one. Draw the one that should be seen
  (`SuspendedLampDrawTest`). Blended draws come after the opaque ones, so glow and halo overlays are safe.
- Stale comments: `GL11` javadoc says lines are dropped (they are recorded as LINES batches now); element render javadocs still say looped sounds are "not here yet".

## Battery charger (Baughn 2026-10-28)

- LEDs: green and steady when a battery is charged; red and blinking (1 Hz, 0.5 s on, 0.5 s off) while it charges, so
  the two read apart without colour; all off without power (switched off, or under a tenth of the rated voltage at
  the input: `BatteryChargerElement.powered`, published when it changes; the render reads `powerOn` and the voltage).
- The batteries lie flat in their cubicles (3/16 square, a pixel deep), 0.18 across and four times an item frame's
  thickness, instead of small spinning items. On a ceiling the body (cubicles, LEDs, batteries) is turned half round, so it reads as on a floor
  or wall from where it was placed: battery tops away from the viewer, slots in the same order.
- The cubicles are grey: their faces map to the top-left 5 x 5 px of `model/batterychargera/batterychargera.png`,
  painted by `tools/textures/charger_cubicles.py`.

## Hot things glow: Incandescence (Baughn 2026-10-28)

`misc/Incandescence` is the one rule for every device that heats up on the outside: the Large Rheostat (its `main`
part), the electric heater (its coils, a separate model part `element` since `mods.eln.cad.models.ElectricHeater` splits
them out), the power resistor (its core), six-node thermal cables and voxel thermal cables ("not truly realistic for
the insulated thermal cables, but ... for charm"). The rheostat's drum is its own model part (`coil`, split out of
`main`: faces with every corner within 0.30 of the axis) so its fins stay dark: glowing the whole `main` lit every fin face like a pane of coloured glass
(AdventurAgent).

- **Look:** nothing below the Draper point (525 C); colour from a smith's heat scale (dull red, blood red, cherry,
  orange-red, orange, yellow-orange, yellow, white at 1300 C); brightness = sqrt of the black-body radiance above
  525 C, 1 at 1100 C, up to 1.3. The power resistor burns out at 130 C, so it glows on its own warning scale
  (`warningCelsius`: dark below 60% of its limit, 900 C's orange at the limit).
- **Self-lit:** `drawGlow(celsius, part)` draws the part again over its normally lit self, full-bright and additive in
  the heat colour; very hot parts get a second additive pass toward yellow-white. The 1.12 rheostat only tinted the
  mesh by black-body colour (never brighter than the texture), from 281 C on; it now follows the physical scale.
- **Halo:** `drawHalo(celsius, size)` is a camera-facing additive disc (`GL11.faceCamera`, the camera's rotation from
  `UtilsClient.Hooks.cameraRotation`; as vanilla's conduit eye, the BER pose has no view rotation), bright in the
  middle and black (adding nothing) at its rim: fake bloom. Daylight on the block (`Hooks.skyLight`) fades it to 15%
  (Baughn: subtle by day).
- **Voxel cables** are in the chunk mesh, so their glow is an overlay: the node publishes one glow step byte per part
  after the parts' bytes (`VoxelNode.publishBeforeElements`, 25 K steps, outside the part hash: a glow change doesn't
  re-mesh the chunk); `SixNodeRender` draws the outer surface of the block's hot cable boxes (`cableSurface`: no faces at the
  joints with the next block or between a run's own pieces, coincident faces once, merged into rectangles; drawing
  whole boxes doubled up there, a bright line at every joint), additive and untextured, 0.1 px proud, and one small
  halo per block; it renders a voxel block with glowing cables even without elements.
- **Block light (server):** the device's own node block emits it (`Node.setLightValue`, as a lamp on its own block):
  `lightLevel` 1 at 525 C, 2 dull red (575), 8 orange (900), 14 at 1250, 15 at 1350. `Incandescence.Light` checks
  every 10 ticks, goes up at once and down only 25 K below the step (`HYSTERESIS`), takes each change from a per-level,
  per-tick budget of 16 (`BUDGET_PER_TICK`) so a long hot cable run can't flood the light engine, and always reports
  its first check so a node's saved light can't outlive a cooled device. A voxel block lights to its hottest thermal
  cable (`VoxelNode.recalculateLightValue`). Devices publish their temperature when the glow's 25 K step changes.
- Tests: `IncandescenceTest` (levels, hysteresis, the first check, look, steps, the warning scale) and the GameTests
  `hot_things_light_their_block` and `electric_heater_heats_a_thermal_cable` (the heater's back face feeds a floor
  thermal cable, six-node and voxel). In a lab, `/elnlab heat <pos> <celsius>` holds a node's thermal loads at a
  temperature (above a device's limit it burns, as it would).

Animations (turning parts, levers, blinking, particles, glow, levels, items in machines): see [animation.md](animation.md).
