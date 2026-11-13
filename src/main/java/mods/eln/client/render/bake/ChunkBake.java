package mods.eln.client.render.bake;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import mods.eln.gl.Bake;
import mods.eln.gl.BakeMesh;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

import java.util.ArrayList;
import java.util.Map;

/**
 * The chunk side of {@link Bake}: keeps the settled calls of each chunk section (those of the devices whose block is
 * in it), writes them into the section's mesh when it is compiled, and knows which calls the mesh on screen contains.
 * <p>
 * Every compile of a section that has calls gets a snapshot of them, taken when the section's extra renderers are
 * gathered (on the render thread, just before the compile task is created). A compiled mesh carries its task's start
 * time, so the mesh on screen contains exactly the last snapshot taken before that time, provided the snapshot's
 * renderer ran to its end. A mesh that maps to no snapshot contains nothing as far as the block entity renderers are
 * told, and they draw everything themselves.
 * <p>
 * A snapshot also holds the light each device stands in, read as the block entity renderer reads it. The game compiles
 * a section again whenever light in it changes, so the mesh follows the light as the blocks around the device do.
 */
public final class ChunkBake implements Bake.Host {
    public static final ChunkBake INSTANCE = new ChunkBake();

    private ChunkBake() {
    }

    private static final class Snap {
        final long time;
        final Bake.Call[] calls;
        /** The packed light of each call's device when the snapshot was taken. */
        final int[] light;
        volatile boolean complete;

        Snap(long time, Bake.Call[] calls, int[] light) {
            this.time = time;
            this.calls = calls;
            this.light = light;
        }
    }

    private static final class Sec {
        final long node;
        final ReferenceLinkedOpenHashSet<Bake.Call> calls = new ReferenceLinkedOpenHashSet<>();
        final ArrayList<Snap> snaps = new ArrayList<>();
        SectionMesh lastMesh;
        Snap active;

        Sec(long node) {
            this.node = node;
        }
    }

    private final Long2ObjectOpenHashMap<Sec> secs = new Long2ObjectOpenHashMap<>();
    private Level level;
    private long dirties;
    private final java.util.concurrent.atomic.LongAdder compiles = new java.util.concurrent.atomic.LongAdder(),
        compiledVertices = new java.util.concurrent.atomic.LongAdder();
    private volatile boolean failed;

    /** Forget everything (the atlas was rebuilt: the sprites moved, and every section is compiled again anyway). */
    public void reset() {
        Minecraft.getInstance().execute(this::clear);
    }

    /**
     * The client's config says whether the chunk draws the devices' unchanging parts ({@link Bake#OVERRIDE} wins).
     * Applies at once, from any thread; also the way back on after a failure switched it off.
     */
    public void configure(boolean on) {
        int mode = Bake.OVERRIDE >= 0 ? Bake.OVERRIDE : on ? 1 : 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {   // (data generation: no game to draw)
            Bake.mode = mode;
            return;
        }
        mc.execute(() -> {
            failed = false;
            setMode(mode);
        });
    }

    /** Off: the sections that hold parts are meshed again without them, and the renderers draw everything from now. */
    private void setMode(int mode) {
        if (mode == Bake.mode) return;
        boolean was = Bake.mode != 0;
        Bake.mode = mode;
        if (was == (mode != 0)) return;
        for (Sec s : secs.values()) dirty(s.node);
        clear();
        org.slf4j.LoggerFactory.getLogger("eln").info("[eln bake] static device parts are drawn {}", mode != 0 ? "by the chunk" : "by the block entity renderers");
    }

    /** Let the chunk draw what it can of this block entity's frame (call right after opening the frame). */
    public static void attach(GlFrame frame, BlockEntity be) {
        if (Bake.mode == 0) return;
        if (be == null || be.isRemoved() || be.getLevel() != INSTANCE.level) {
            frame.noBake(Bake.NO_BLOCK_ENTITY);
            return;
        }
        frame.bake(be, be.getBlockPos().asLong());
    }

    private void clear() {
        Bake.clear();
        secs.clear();
    }

    // ---- Bake.Host

    @Override
    public boolean hasSprite(Identifier texture) {
        return ModelSprites.table.containsKey(texture == null ? UtilsClient.WHITE : texture);
    }

    @Override
    public void settled(Bake.Call c) {
        long s = c.device.home;
        Sec sec = secs.get(s);
        if (sec == null) {
            sec = new Sec(s);
            secs.put(s, sec);
        }
        sec.calls.add(c);
        dirty(s);
    }

    @Override
    public void unsettled(Bake.Call c) {
        Sec sec = secs.get(c.device.home);
        if (sec != null && sec.calls.remove(c)) dirty(sec.node);
    }

    private void dirty(long section) {
        dirties++;
        Minecraft.getInstance().levelExtractor.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
    }

    @Override
    public String stats() {
        int active = 0;
        for (Sec s : secs.values()) if (s.active != null) active++;
        // audit: whether the mesh on screen contains a call, against the call's own flag
        java.util.IdentityHashMap<Bake.Call, int[]> shown = new java.util.IdentityHashMap<>();
        for (Sec s : secs.values()) {
            for (Bake.Call c : s.calls) shown.computeIfAbsent(c, k -> new int[1]);
            if (s.active != null) for (Bake.Call c : s.active.calls) shown.computeIfAbsent(c, k -> new int[1])[0]++;
        }
        int wrong = 0, stale = 0, skippable = 0;
        for (var e : shown.entrySet()) {
            Bake.Call c = e.getKey();
            if ((c.shown ? 1 : 0) != e.getValue()[0]) wrong++;
            if (!c.isSettled()) stale++;
            else if (c.shown) skippable++;
        }
        String out = secs.size() + " sections with calls (" + active + " shown), " + skippable + " calls shown by the chunk, " + stale
            + " stale in meshes, " + wrong + " MISCOUNTED, " + dirties + " made dirty, " + compiles.sumThenReset()
            + " compiled with " + compiledVertices.sumThenReset() + " vertices" + (failed ? ", A COMPILE FAILED" : "");
        dirties = 0;
        return out;
    }

    /** Find, for each section with calls, the snapshot its mesh on screen was compiled from. */
    @Override
    public void frame() {
        Minecraft mc = Minecraft.getInstance();
        if (failed) {
            setMode(0);
            return;
        }
        if (mc.level != level) {
            level = mc.level;
            clear();
        }
        if (secs.isEmpty() || mc.levelRenderer == null) return;
        ViewArea area = mc.levelRenderer.viewArea();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        var it = secs.values().iterator();
        while (it.hasNext()) {
            Sec s = it.next();
            SectionMesh mesh = null;
            if (area != null) {
                p.set(SectionPos.x(s.node) << 4, SectionPos.y(s.node) << 4, SectionPos.z(s.node) << 4);
                SectionRenderDispatcher.RenderSection rs = area.getRenderSectionAt(p);
                if (rs != null && rs.getSectionNode() == s.node) mesh = rs.sectionMesh.get();
            }
            if (mesh != s.lastMesh) {
                s.lastMesh = mesh;
                Snap found = null;
                if (mesh instanceof CompiledSectionMesh m && mesh != CompiledSectionMesh.UNCOMPILED && mesh != CompiledSectionMesh.EMPTY) {
                    long t = m.getCompileTaskStartTime();
                    int at = -1;
                    for (int i = s.snaps.size() - 1; i >= 0; i--) {
                        if (s.snaps.get(i).time <= t) {
                            at = i;
                            break;
                        }
                    }
                    if (at >= 0) {
                        found = s.snaps.get(at);
                        if (at > 0) s.snaps.subList(0, at).clear();
                        if (!found.complete) found = null;
                    }
                }
                activate(s, found);
            }
            if (s.calls.isEmpty() && (s.active == null || s.active.calls.length == 0)) it.remove();
        }
    }

    private static void activate(Sec s, Snap snap) {
        if (s.active == snap) return;
        if (s.active != null) for (Bake.Call c : s.active.calls) c.shown = false;
        s.active = snap;
        if (snap != null) for (Bake.Call c : snap.calls) c.shown = true;
    }

    /** A section is about to be compiled: hand it the calls it holds now. */
    public void onGeometry(AddSectionGeometryEvent e) {
        if (Bake.mode == 0 || secs.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (e.getLevel() != mc.level || mc.level != level) return;
        BlockPos o = e.getSectionOrigin();
        long node = SectionPos.asLong(o.getX() >> 4, o.getY() >> 4, o.getZ() >> 4);
        Sec s = secs.get(node);
        if (s == null) return;
        ArrayList<Bake.Call> live = new ArrayList<>(s.calls.size());
        for (Bake.Call c : s.calls) {
            // a block entity removed since its last draw: its block change is why this compile runs
            if (c.device.owner instanceof BlockEntity be && be.isRemoved()) continue;
            live.add(c);
        }
        Bake.Call[] calls = Bake.inDrawOrder(live);
        int[] lights = new int[calls.length];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < calls.length; i++) {
            // as BlockEntityRenderState.extractBase reads it
            lights[i] = i > 0 && calls[i].device == calls[i - 1].device ? lights[i - 1] : LightCoordsUtil.getLightCoords(mc.level, p.set(calls[i].pos));
        }
        Snap snap = new Snap(Util.getNanos(), calls, lights);
        s.snaps.add(snap);
        if (s.snaps.size() > 16) s.snaps.remove(0);
        if (calls.length == 0) {
            snap.complete = true;
            return;
        }
        Map<Identifier, BakeMesh.Spr> sprites = ModelSprites.table;
        boolean nether = mc.level.dimensionType().cardinalLightType() == CardinalLighting.Type.NETHER;
        int ox = o.getX(), oy = o.getY(), oz = o.getZ();
        e.addRenderer(context -> {
            try {
                VertexConsumer[] buffers = new VertexConsumer[2];
                compiledVertices.add(BakeMesh.write((cutout, x, y, z, color, u, v, light) -> {
                    int b = cutout ? 1 : 0;
                    if (buffers[b] == null) buffers[b] = context.getOrCreateChunkBuffer(cutout ? ChunkSectionLayer.CUTOUT : ChunkSectionLayer.SOLID);
                    buffers[b].addVertex(x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, light, 0, 1, 0);
                }, calls, lights, sprites, ox, oy, oz, nether));
                compiles.increment();
                snap.complete = true;
            } catch (Throwable t) {
                // the snapshot stays incomplete, so nothing of it counts as shown; the next frame switches baking off
                if (!failed) org.slf4j.LoggerFactory.getLogger("eln").error("[eln bake] writing chunk geometry failed; static device parts go back to the block entity renderers", t);
                failed = true;
            }
        });
    }

    /** Forget the devices whose block entity is gone (client tick). */
    public void tick() {
        if (Bake.mode == 0) return;
        Bake.sweep(owner -> owner instanceof BlockEntity be && (be.isRemoved() || be.getLevel() != level));
    }
}
