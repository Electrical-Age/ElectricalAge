package mods.eln.client.render.bake;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import mods.eln.gl.Bake;
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
 * The chunk side of {@link Bake}: keeps the settled calls of each chunk section, writes them into the section's mesh
 * when it is compiled, and knows which calls the mesh on screen contains.
 * <p>
 * Every compile of a section that has calls gets a snapshot of them, taken when the section's extra renderers are
 * gathered (on the render thread, just before the compile task is created). A compiled mesh carries its task's start
 * time, so the mesh on screen contains exactly the last snapshot taken before that time, provided the snapshot's
 * renderer ran to its end. A mesh that maps to no snapshot contains nothing as far as the block entity renderers are
 * told, and they draw everything themselves.
 */
public final class ChunkBake implements Bake.Host {
    public static final ChunkBake INSTANCE = new ChunkBake();

    private ChunkBake() {
    }

    private static final class Snap {
        final long time;
        final Bake.Call[] calls;
        volatile boolean complete;

        Snap(long time, Bake.Call[] calls) {
            this.time = time;
            this.calls = calls;
        }
    }

    private static final class Sec {
        final long node;
        final ReferenceLinkedOpenHashSet<Bake.Call> calls = new ReferenceLinkedOpenHashSet<>();
        final ArrayList<Snap> snaps = new ArrayList<>();
        SectionMesh lastMesh;
        Snap active;
        long seen;

        Sec(long node) {
            this.node = node;
        }
    }

    private final Long2ObjectOpenHashMap<Sec> secs = new Long2ObjectOpenHashMap<>();
    private Level level;
    private long frame;
    private int foreign;
    private long dirties;
    private final java.util.concurrent.atomic.LongAdder compiles = new java.util.concurrent.atomic.LongAdder(),
        compiledVertices = new java.util.concurrent.atomic.LongAdder();
    private volatile boolean failed;

    /** Forget everything (the atlas was rebuilt: the sprites moved, and every section is compiled again anyway). */
    public void reset() {
        Minecraft.getInstance().execute(this::clear);
    }

    /** Let the chunk draw what it can of this block entity's frame (call right after opening the frame). */
    public static void attach(GlFrame frame, BlockEntity be, int light) {
        if (Bake.mode == 0) return;
        if (be == null || be.isRemoved() || be.getLevel() != INSTANCE.level) {
            frame.noBake(Bake.NO_BLOCK_ENTITY);
            return;
        }
        frame.bake(be, be.getBlockPos().asLong(), light);
    }

    private void clear() {
        Bake.clear();
        secs.clear();
        foreign = 0;
    }

    // ---- Bake.Host

    @Override
    public boolean hasSprite(Identifier texture) {
        return ModelSprites.table.containsKey(texture == null ? UtilsClient.WHITE : texture);
    }

    @Override
    public void settled(Bake.Call c) {
        for (long s : c.sections) {
            Sec sec = secs.get(s);
            if (sec == null) {
                sec = new Sec(s);
                secs.put(s, sec);
            }
            sec.calls.add(c);
            dirty(s);
        }
        if (c.foreign) foreign++;
    }

    @Override
    public void unsettled(Bake.Call c) {
        for (long s : c.sections) {
            Sec sec = secs.get(s);
            if (sec != null && sec.calls.remove(c)) dirty(s);
        }
        if (c.foreign) foreign--;
    }

    private void dirty(long section) {
        dirties++;
        Minecraft.getInstance().levelExtractor.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
    }

    @Override
    public boolean visible(long section) {
        Sec s = secs.get(section);
        return s != null && s.seen == frame;
    }

    @Override
    public String stats() {
        int active = 0;
        for (Sec s : secs.values()) if (s.active != null) active++;
        // audit: every call's count of meshes on screen that contain it, against the sections' own records
        java.util.IdentityHashMap<Bake.Call, int[]> shown = new java.util.IdentityHashMap<>();
        for (Sec s : secs.values()) {
            for (Bake.Call c : s.calls) shown.computeIfAbsent(c, k -> new int[1]);
            if (s.active != null) for (Bake.Call c : s.active.calls) shown.computeIfAbsent(c, k -> new int[1])[0]++;
        }
        int wrong = 0, stale = 0, skippable = 0;
        for (var e : shown.entrySet()) {
            Bake.Call c = e.getKey();
            if (c.activeIn != e.getValue()[0]) wrong++;
            if (!c.isSettled()) stale++;
            else if (c.activeIn == c.sections.length) skippable++;
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
        if (mc.level != level) {
            level = mc.level;
            clear();
        }
        frame++;
        if (secs.isEmpty() || mc.levelRenderer == null) return;
        if (foreign > 0) {
            for (SectionRenderDispatcher.RenderSection rs : mc.levelRenderer.visibleSections()) {
                Sec s = secs.get(rs.getSectionNode());
                if (s != null) s.seen = frame;
            }
        }
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
        if (s.active != null) for (Bake.Call c : s.active.calls) c.activeIn--;
        s.active = snap;
        if (snap != null) for (Bake.Call c : snap.calls) c.activeIn++;
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
        Snap snap = new Snap(Util.getNanos(), calls);
        s.snaps.add(snap);
        if (s.snaps.size() > 16) s.snaps.remove(0);
        if (calls.length == 0) {
            snap.complete = true;
            return;
        }
        Map<Identifier, ModelSprites.Spr> sprites = ModelSprites.table;
        boolean nether = mc.level.dimensionType().cardinalLightType() == CardinalLighting.Type.NETHER;
        int ox = o.getX(), oy = o.getY(), oz = o.getZ();
        e.addRenderer(context -> {
            try {
                VertexConsumer[] buffers = new VertexConsumer[2];
                compiledVertices.add(write((cutout, x, y, z, color, u, v, light) -> {
                    int b = cutout ? 1 : 0;
                    if (buffers[b] == null) buffers[b] = context.getOrCreateChunkBuffer(cutout ? ChunkSectionLayer.CUTOUT : ChunkSectionLayer.SOLID);
                    buffers[b].addVertex(x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, light, 0, 1, 0);
                }, calls, sprites, node, ox, oy, oz, nether));
                compiles.increment();
                snap.complete = true;
            } catch (Throwable t) {
                if (!failed) org.slf4j.LoggerFactory.getLogger("eln").error("[eln bake] writing chunk geometry failed", t);
                failed = true;
            }
        });
    }

    /** Forget the devices whose block entity is gone (client tick). */
    public void tick() {
        if (Bake.mode == 0) return;
        Bake.sweep(owner -> owner instanceof BlockEntity be && (be.isRemoved() || be.getLevel() != level));
    }

    /** Where {@link #write} puts a vertex: the cutout layer (the texture has holes) or the solid one. */
    public interface Sink {
        void vertex(boolean cutout, float x, float y, float z, int color, float u, float v, int light);
    }

    private static final float L0X, L0Y, L0Z, L1X, L1Y, L1Z;

    static {
        float n = (float) (1 / Math.sqrt(0.2 * 0.2 + 1 + 0.7 * 0.7));
        L0X = 0.2f * n;
        L0Y = n;
        L0Z = -0.7f * n;
        L1X = -0.2f * n;
        L1Y = n;
        L1Z = 0.7f * n;
    }

    /**
     * Write the calls' quads that lie in this section into its buffers (meshing worker; reads only the arguments).
     * Vertices are relative to the section's origin. The colour carries the entity shader's directional shading for
     * the vertex normal (two lights, 0.6 each, over 0.4 ambient), so the result equals the block entity renderer's.
     */
    public static int write(Sink out, Bake.Call[] calls, Map<Identifier, ModelSprites.Spr> sprites, long node, int ox, int oy, int oz, boolean nether) {
        int written = 0;
        final int S = GlFrame.STRIDE;
        float l1y = nether ? -L1Y : L1Y;
        float[] vx = new float[4], vy = new float[4], vz = new float[4], vu = new float[4], vv = new float[4];
        int[] vc = new int[4];
        for (Bake.Call c : calls) {
            ModelSprites.Spr sp = sprites.get(c.texture == null ? UtilsClient.WHITE : c.texture);
            if (sp == null) throw new IllegalStateException("no sprite for " + c.texture);
            boolean cutout = sp.holes();
            float m00 = c.m00, m01 = c.m01, m02 = c.m02, m10 = c.m10, m11 = c.m11, m12 = c.m12, m20 = c.m20, m21 = c.m21, m22 = c.m22;
            float tx = c.m30 + (BlockPos.getX(c.pos) - ox), ty = c.m31 + (BlockPos.getY(c.pos) - oy), tz = c.m32 + (BlockPos.getZ(c.pos) - oz);
            // normals transform by the cofactor matrix (the inverse transpose up to scale; they are normalised below)
            float n00 = m11 * m22 - m12 * m21, n01 = m12 * m20 - m10 * m22, n02 = m10 * m21 - m11 * m20;
            float n10 = m02 * m21 - m01 * m22, n11 = m00 * m22 - m02 * m20, n12 = m01 * m20 - m00 * m21;
            float n20 = m01 * m12 - m02 * m11, n21 = m02 * m10 - m00 * m12, n22 = m00 * m11 - m01 * m10;
            float det = m00 * n00 + m01 * n01 + m02 * n02;
            float sign = det < 0 ? -1 : 1;
            boolean all = c.sections.length == 1;
            int light = c.fullBright ? LightCoordsUtil.FULL_BRIGHT : c.light;
            float[] q = c.quads;
            int base = c.color;
            for (int i = 0; i + 4 * S <= c.floats; i += 4 * S) {
                if (!all && Bake.sectionOf(c, i) != node) continue;
                for (int k = 0; k < 4; k++) {
                    int j = i + k * S;
                    float x = q[j], y = q[j + 1], z = q[j + 2];
                    vx[k] = m00 * x + m10 * y + m20 * z + tx;
                    vy[k] = m01 * x + m11 * y + m21 * z + ty;
                    vz[k] = m02 * x + m12 * y + m22 * z + tz;
                    vu[k] = sp.u0() + q[j + 3] * sp.du();
                    vv[k] = sp.v0() + q[j + 4] * sp.dv();
                    int col = c.colored ? Float.floatToRawIntBits(q[j + 5]) : base;
                    float shade = 1;
                    if (c.lit) {
                        float a = q[j + 6], b = q[j + 7], d = q[j + 8];
                        float nx = (n00 * a + n10 * b + n20 * d) * sign, ny = (n01 * a + n11 * b + n21 * d) * sign, nz = (n02 * a + n12 * b + n22 * d) * sign;
                        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (len > 0) {
                            nx /= len;
                            ny /= len;
                            nz /= len;
                        }
                        // as the renderer's vertex format stores a normal: a signed byte per axis
                        nx = (int) (nx * 127f) / 127f;
                        ny = (int) (ny * 127f) / 127f;
                        nz = (int) (nz * 127f) / 127f;
                        shade = Math.min(1f, (Math.max(0f, L0X * nx + L0Y * ny + L0Z * nz) + Math.max(0f, L1X * nx + l1y * ny + L1Z * nz)) * 0.6f + 0.4f);
                    }
                    int r = Math.round(((col >> 16) & 255) * shade), g = Math.round(((col >> 8) & 255) * shade), bl = Math.round((col & 255) * shade);
                    vc[k] = 0xFF000000 | (r << 16) | (g << 8) | bl;
                }
                for (int k = 0; k < 4; k++) out.vertex(cutout, vx[k], vy[k], vz[k], vc[k], vu[k], vv[k], light);
                written += 4;
                if (!c.cull) {
                    for (int k = 3; k >= 0; k--) out.vertex(cutout, vx[k], vy[k], vz[k], vc[k], vu[k], vv[k], light);
                    written += 4;
                }
            }
        }
        return written;
    }
}
