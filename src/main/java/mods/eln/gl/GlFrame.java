package mods.eln.gl;

import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The state machine behind {@link GL11}: one frame per drawn node (opened by the client renderer, which then submits
 * {@link #batches()}). Vertices are transformed by the frame's matrix stack, which starts at the renderer's pose, and
 * grouped by {@link Key} (texture, blending, culling, full-bright); every polygon mode becomes quads (a triangle repeats
 * its last vertex). Outside a frame every call goes to a frame that discards, so stray draw calls do no harm.
 * <p>
 * Display lists (EA compiles every OBJ face group into one) are compiled into quads in list space plus the few state
 * calls between them; calling a list transforms those quads straight into the batches. Batches come from a pool reset
 * once per rendered frame ({@link #beginRenderFrame}), because the submitted geometry is read later in the frame.
 * Render thread only.
 */
public final class GlFrame {
    /**
     * What selects the RenderType of a vertex batch. texture null = untextured (drawn with a white texture);
     * fullBright = the 1.12 lightmap override (drawn at full light); lit = GL_LIGHTING (directional shading).
     */
    public record Key(Identifier texture, Blend blend, boolean cull, boolean fullBright, boolean lit) {
    }

    /** LINES: a line batch (GL_LINES and strips); u holds the line width, the normal the line's direction. */
    public enum Blend {NONE, ALPHA, ADDITIVE, MULTIPLY, LINES}

    /** Vertex layout in a batch: x y z u v argb(bits) nx ny nz. */
    public static final int STRIDE = 9;

    public static final class Batch {
        public float[] data = new float[STRIDE * 256];
        public int floats;

        void ensure(int more) {
            if (floats + more > data.length) data = Arrays.copyOf(data, Math.max(data.length * 2, floats + more));
        }

        public int vertices() {
            return floats / STRIDE;
        }
    }

    /**
     * Something a renderer submits itself at the matrix of the moment (pose space): in-world text, an item. Payload:
     * {@link Text} or an ItemStack (as 1.12's entity-item drawing: spinning on machines).
     */
    public record Extra(Matrix4f matrix, Object payload) {
    }

    public record Text(String text, int x, int y, int color, boolean fullBright) {
    }

    /** An item lying flat, as in an item frame (FIXED): one block across, in the current matrix's x-y plane. */
    public record FlatItem(net.minecraft.world.item.ItemStack stack) {
    }

    private final ArrayList<Extra> extras = new ArrayList<>();

    /** Burnt overlay (1.12 BurntRender): everything drawn while set is multiplied into the frame in a soot colour. */
    public boolean soot;
    /** Placement preview: everything drawn while set (0 = off) is see-through in this ARGB (alpha and tint). */
    public int ghost;
    static final float SOOT = Float.intBitsToFloat(0xFF4D4238); // 0.30, 0.26, 0.22

    public List<Extra> extras() {
        return extras;
    }

    /** Record an extra at the current matrix (dropped when compiling a display list or in the discarding frame). */
    public void extra(Object payload) {
        if (discard || rec != null) return;
        Matrix4f m = new Matrix4f(stack[depth]);
        if (split) m.setTranslation(m.m30() + ox, m.m31() + oy, m.m32() + oz);
        extras.add(new Extra(m, payload));
    }

    /** In-world text at the current matrix (1.12 FontRenderer.drawString in a TESR). */
    public void text(String text, int x, int y, int color) {
        extra(new Text(text, x, y, color, fullBright || !lighting));
    }

    private static final GlFrame DISCARD = new GlFrame(true);
    private static final GlFrame FRAME = new GlFrame(false);
    private static GlFrame current = DISCARD;

    /** The open frame, or the discarding one. */
    public static GlFrame current() {
        return current;
    }

    /** Open the frame with an identity base (tests). */
    public static GlFrame open() {
        return open(null, null);
    }

    /**
     * Open the frame for one node's draw, with the renderer's pose as the base matrix (vertices come out in the pose's
     * space). 1.12 TESR defaults: texture, lighting and culling on, white.
     */
    public static GlFrame open(Matrix4f pose, Matrix3f normal) {
        FRAME.reset(pose, normal);
        current = FRAME;
        return FRAME;
    }

    /**
     * Open the frame for a block entity's draw. A pose that only translates (block position minus camera, the usual
     * case) is kept out of the matrix stack and added to every vertex instead, so the matrices of the draw are the
     * same from frame to frame while the camera moves: that is what lets {@link Bake} compare them.
     */
    public static GlFrame openBlock(Matrix4f pose, Matrix3f normal) {
        boolean pure = pose.m00() == 1 && pose.m11() == 1 && pose.m22() == 1 && pose.m01() == 0 && pose.m02() == 0
            && pose.m10() == 0 && pose.m12() == 0 && pose.m20() == 0 && pose.m21() == 0 && pose.m33() == 1;
        if (!pure) return open(pose, normal);
        FRAME.reset(null, normal);
        FRAME.ox = pose.m30();
        FRAME.oy = pose.m31();
        FRAME.oz = pose.m32();
        FRAME.split = true;
        current = FRAME;
        return FRAME;
    }

    /**
     * Let the chunk mesh stand in for this frame's unchanging display-list geometry (see {@link Bake}); owner is the
     * block entity. Does nothing unless the frame was opened by {@link #openBlock} with a translating pose.
     */
    public void bake(Object owner, long pos, int light) {
        if (split && Bake.mode != 0) bake = Bake.begin(owner, pos, light);
    }

    /** Close the frame; its batches stay readable until the pool is reset by the next render frame. */
    public static void close() {
        if (FRAME.bake != null) {
            FRAME.bake.end();
            FRAME.bake = null;
        }
        current = DISCARD;
    }

    // ---- batch pool, reset per rendered frame
    private static final ArrayList<Batch> POOL = new ArrayList<>();
    private static int poolUsed;

    /** A new render frame: every batch handed out before has been drawn. */
    public static void beginRenderFrame() {
        poolUsed = 0;
        BakeStats.frame();
        Bake.frame(System.nanoTime());
    }

    private static Batch acquire() {
        if (poolUsed == POOL.size()) POOL.add(new Batch());
        Batch b = POOL.get(poolUsed++);
        b.floats = 0;
        return b;
    }

    private final boolean discard;

    private GlFrame(boolean discard) {
        this.discard = discard;
    }

    // ---- state
    /** The base translation of a frame opened by {@link #openBlock} (0 otherwise): added to every vertex. */
    private float ox, oy, oz;
    private boolean split;
    private Bake.Device bake;
    /** While set, nothing drawn is left to the chunk mesh (a burnt element: its soot overlay needs the same depth). */
    public boolean bakePause;
    private final Matrix4f[] stack = new Matrix4f[64];
    private final Matrix3f baseNormal = new Matrix3f();
    private int depth;
    private float r = 1, g = 1, b = 1, a = 1;
    private float nx, ny = 1, nz, u, v;
    private Identifier texture;
    private boolean texture2d = true, lighting = true, blend, cull = true, fullBright;
    private Blend blendMode = Blend.ALPHA;

    private record Attrib(float r, float g, float b, float a, Identifier texture, boolean texture2d, boolean lighting,
                          boolean blend, boolean cull, boolean fullBright, Blend blendMode) {
    }

    private final ArrayList<Attrib> attribs = new ArrayList<>();

    private final Map<Key, Batch> batches = new LinkedHashMap<>();
    /** Lines and points dropped this frame (counted for the render check). */
    public int droppedVertices;

    {
        for (int i = 0; i < stack.length; i++) stack[i] = new Matrix4f();
    }

    /** A copy of the current matrix (model to the frame's base space: world-aligned for a block's draw). */
    public Matrix4f currentMatrix() {
        return new Matrix4f(stack[depth]);
    }

    void reset(Matrix4f pose, Matrix3f normal) {
        depth = 0;
        if (pose != null) stack[0].set(pose);
        else stack[0].identity();
        if (normal != null) baseNormal.set(normal);
        else baseNormal.identity();
        r = g = b = a = 1;
        nx = 0;
        ny = 1;
        nz = 0;
        u = v = 0;
        texture = null;
        texture2d = lighting = cull = true;
        blend = fullBright = false;
        blendMode = Blend.ALPHA;
        attribs.clear();
        batches.clear();
        extras.clear();
        soot = false;
        ghost = 0;
        droppedVertices = 0;
        rec = null;
        ox = oy = oz = 0;
        split = false;
        bake = null;
        bakePause = false;
        mode = -1;
        lastKey = null;
        lastBatch = null;
    }

    public Map<Key, Batch> batches() {
        return batches;
    }

    // ---- display lists: state calls as ops, primitives as quads in list space
    static final class Prim {
        final float[] quads;
        final int floats;
        final boolean colored, lines;
        /** Texture coordinates all within [0,1] (so the texture can be a sprite of an atlas): 0 unknown, 1 yes, 2 no. */
        private byte uv;

        Prim(float[] quads, int floats, boolean colored, boolean lines) {
            this.quads = quads;
            this.floats = floats;
            this.colored = colored;
            this.lines = lines;
        }

        boolean uvInUnitSquare() {
            if (uv == 0) {
                uv = 1;
                for (int i = 0; i < floats; i += STRIDE) {
                    float u = quads[i + 3], v = quads[i + 4];
                    if (!(u >= -1e-4f && u <= 1.0001f && v >= -1e-4f && v <= 1.0001f)) {
                        uv = 2;
                        break;
                    }
                }
            }
            return uv == 1;
        }
    }

    private static final Map<Integer, List<Object>> LISTS = new HashMap<>();
    private static int nextList = 1;
    private List<Object> rec;
    private boolean recExecute;
    private int recList;
    // compile-time current values (GL_COMPILE doesn't execute, so the frame's own state stays as it was)
    private float cu, cv, cnx, cny = 1, cnz;
    private int ccolor;
    private boolean ccolored;

    static synchronized int genLists(int range) {
        int first = nextList;
        nextList += range;
        return first;
    }

    static synchronized void deleteLists(int list, int range) {
        for (int i = 0; i < range; i++) LISTS.remove(list + i);
    }

    void newList(int list, boolean execute) {
        rec = new ArrayList<>();
        recExecute = execute;
        recList = list;
        cu = cv = cnx = cnz = 0;
        cny = 1;
        ccolored = false;
    }

    void endList() {
        if (rec == null) return;
        synchronized (GlFrame.class) {
            LISTS.put(recList, rec);
        }
        rec = null;
        if (recExecute) callList(recList);
    }

    @SuppressWarnings("unchecked")
    void callList(int list) {
        if (record(f -> f.callList(list))) return;
        List<Object> ops;
        synchronized (GlFrame.class) {
            ops = LISTS.get(list);
        }
        if (ops == null) return;
        for (Object op : ops) {
            if (op instanceof Prim p) {
                if (p.lines) {
                    if (BakeStats.ON) BakeStats.add(BakeStats.LINES, p.floats / STRIDE);
                    emitLines(p.quads, p.floats, p.colored);
                } else replay(p);
            }
            else ((Consumer<GlFrame>) op).accept(this);
        }
    }

    /** While compiling a list: record op and return true (it must not run now). */
    private boolean record(Consumer<GlFrame> op) {
        if (rec == null) return false;
        rec.add(op);
        return true;
    }

    // ---- matrix stack
    void pushMatrix() {
        if (record(GlFrame::pushMatrix)) return;
        if (depth + 1 >= stack.length) return;
        stack[depth + 1].set(stack[depth]);
        depth++;
    }

    void popMatrix() {
        if (record(GlFrame::popMatrix)) return;
        if (depth > 0) depth--;
    }

    void loadIdentity() {
        if (record(GlFrame::loadIdentity)) return;
        stack[depth].set(stack[0]);
    }

    void translate(float x, float y, float z) {
        if (record(f -> f.translate(x, y, z))) return;
        stack[depth].translate(x, y, z);
    }

    void rotate(float angle, float x, float y, float z) {
        if (record(f -> f.rotate(angle, x, y, z))) return;
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return;
        stack[depth].rotate((float) Math.toRadians(angle), x / len, y / len, z / len);
    }

    /** Keep the current origin but turn the axes to the camera's (a camera-facing sprite: Incandescence halos). */
    void faceCamera(org.joml.Quaternionf camera) {
        if (record(f -> f.faceCamera(camera))) return;
        var t = stack[depth].getTranslation(new org.joml.Vector3f());
        stack[depth].identity().translate(t).rotate(camera);
    }

    void scale(float x, float y, float z) {
        if (record(f -> f.scale(x, y, z))) return;
        stack[depth].scale(x, y, z);
    }

    // ---- attributes
    private static int argb(float r, float g, float b, float a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    void color(float r, float g, float b, float a) {
        if (rec != null) {
            ccolor = argb(r, g, b, a);
            ccolored = true;
        }
        if (record(f -> f.color(r, g, b, a))) return;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    void normal(float x, float y, float z) {
        if (rec != null) {
            cnx = x;
            cny = y;
            cnz = z;
            return;
        }
        nx = x;
        ny = y;
        nz = z;
    }

    void texCoord(float u, float v) {
        if (rec != null) {
            cu = u;
            cv = v;
            return;
        }
        this.u = u;
        this.v = v;
    }

    void texture(Identifier t) {
        if (record(f -> f.texture(t))) return;
        texture = t;
    }

    private float lineWidth = 1;

    void lineWidth(float w) {
        if (record(f -> f.lineWidth(w))) return;
        lineWidth = w;
    }

    void fullBright(boolean on) {
        if (record(f -> f.fullBright(on))) return;
        fullBright = on;
    }

    void setCap(int cap, boolean on) {
        if (record(f -> f.setCap(cap, on))) return;
        switch (cap) {
            case GL11.GL_TEXTURE_2D -> texture2d = on;
            case GL11.GL_LIGHTING -> lighting = on;
            case GL11.GL_BLEND -> blend = on;
            case GL11.GL_CULL_FACE -> cull = on;
            default -> {
            }
        }
    }

    boolean isCap(int cap) {
        return switch (cap) {
            case GL11.GL_TEXTURE_2D -> texture2d;
            case GL11.GL_LIGHTING -> lighting;
            case GL11.GL_BLEND -> blend;
            case GL11.GL_CULL_FACE -> cull;
            default -> false;
        };
    }

    void blendFunc(int src, int dst) {
        if (record(f -> f.blendFunc(src, dst))) return;
        if (dst == GL11.GL_ONE) blendMode = Blend.ADDITIVE;
        else if (src == GL11.GL_DST_COLOR || dst == GL11.GL_SRC_COLOR) blendMode = Blend.MULTIPLY;
        else blendMode = Blend.ALPHA;
    }

    void pushAttrib() {
        if (record(GlFrame::pushAttrib)) return;
        attribs.add(new Attrib(r, g, b, a, texture, texture2d, lighting, blend, cull, fullBright, blendMode));
    }

    void popAttrib() {
        if (record(GlFrame::popAttrib)) return;
        if (attribs.isEmpty()) return;
        Attrib s = attribs.remove(attribs.size() - 1);
        r = s.r;
        g = s.g;
        b = s.b;
        a = s.a;
        texture = s.texture;
        texture2d = s.texture2d;
        lighting = s.lighting;
        blend = s.blend;
        cull = s.cull;
        fullBright = s.fullBright;
        blendMode = s.blendMode;
    }

    // ---- primitives
    private int mode = -1;
    private float[] prim = new float[STRIDE * 64];
    private int primFloats;
    private final Matrix3f normalMatrix = new Matrix3f();
    private Key lastKey;
    private Batch lastBatch;

    /** The batch for the current state (cached while the state doesn't change). */
    private Batch batch() {
        Identifier t = texture2d ? texture : null;
        Blend bl = soot ? Blend.MULTIPLY : ghost != 0 ? Blend.ALPHA : blend ? blendMode : Blend.NONE;
        Key k = lastKey;
        if (k == null || k.texture != t || k.blend != bl || k.cull != cull || k.fullBright != fullBright || k.lit != lighting) {
            k = new Key(t, bl, cull, fullBright, lighting);
            Batch b = batches.get(k);
            if (b == null) {
                b = acquire();
                batches.put(k, b);
            }
            lastKey = k;
            lastBatch = b;
        }
        return lastBatch;
    }

    void begin(int mode) {
        this.mode = mode;
        primFloats = 0;
    }

    void vertex(float x, float y, float z) {
        if (mode < 0) return;
        if (primFloats + STRIDE > prim.length) prim = Arrays.copyOf(prim, prim.length * 2);
        float[] p = prim;
        int i = primFloats;
        p[i] = x;
        p[i + 1] = y;
        p[i + 2] = z;
        if (rec != null) {
            p[i + 3] = cu;
            p[i + 4] = cv;
            p[i + 5] = Float.intBitsToFloat(ccolor);
            p[i + 6] = cnx;
            p[i + 7] = cny;
            p[i + 8] = cnz;
        } else {
            p[i + 3] = u;
            p[i + 4] = v;
            p[i + 5] = Float.intBitsToFloat(argb(r, g, b, a));
            p[i + 6] = nx;
            p[i + 7] = ny;
            p[i + 8] = nz;
        }
        primFloats += STRIDE;
    }

    private static int clamp(float c) {
        return Math.max(0, Math.min(255, Math.round(c * 255f)));
    }

    void end() {
        int m = mode;
        mode = -1;
        if (m < 0) return;
        int n = primFloats / STRIDE;
        if (m == GL11.GL_LINES || m == GL11.GL_LINE_STRIP || m == GL11.GL_LINE_LOOP) {
            toLines(m, n);
            if (quadFloats == 0) return;
            if (rec != null) rec.add(new Prim(Arrays.copyOf(quadBuf, quadFloats), quadFloats, ccolored, true));
            else if (!discard) {
                if (BakeStats.ON) BakeStats.add(BakeStats.LINES, quadFloats / STRIDE);
                emitLines(quadBuf, quadFloats, true);
            }
            return;
        }
        float[] quads = toQuads(m, n);
        int qf = quadFloats;
        if (quads == null) {
            if (rec == null) droppedVertices += n;
            return;
        }
        if (rec != null) {
            rec.add(new Prim(Arrays.copyOf(quads, qf), qf, ccolored, false));
            return;
        }
        if (discard) return;
        if (BakeStats.ON) BakeStats.add(BakeStats.IMMEDIATE, qf / STRIDE);
        emit(quads, qf, true);
    }

    private float[] quadBuf = new float[STRIDE * 64];
    private int quadFloats;

    /** The current primitive as quads (list space), or null for lines/points. */
    private float[] toQuads(int m, int n) {
        quadFloats = 0;
        switch (m) {
            case GL11.GL_QUADS -> {
                for (int i = 0; i + 3 < n; i += 4) quad(i, i + 1, i + 2, i + 3);
            }
            case GL11.GL_TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) quad(i, i + 1, i + 2, i + 2);
            }
            case GL11.GL_QUAD_STRIP -> {
                for (int i = 0; i + 3 < n; i += 2) quad(i, i + 1, i + 3, i + 2);
            }
            case GL11.GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < n; i++) {
                    if ((i & 1) == 0) quad(i, i + 1, i + 2, i + 2);
                    else quad(i + 1, i, i + 2, i + 2);
                }
            }
            case GL11.GL_TRIANGLE_FAN, GL11.GL_POLYGON -> {
                for (int i = 1; i + 1 < n; i++) quad(0, i, i + 1, i + 1);
            }
            default -> {
                return null;
            }
        }
        return quadBuf;
    }

    private void quad(int i0, int i1, int i2, int i3) {
        if (quadFloats + 4 * STRIDE > quadBuf.length) quadBuf = Arrays.copyOf(quadBuf, quadBuf.length * 2);
        System.arraycopy(prim, i0 * STRIDE, quadBuf, quadFloats, STRIDE);
        System.arraycopy(prim, i1 * STRIDE, quadBuf, quadFloats + STRIDE, STRIDE);
        System.arraycopy(prim, i2 * STRIDE, quadBuf, quadFloats + 2 * STRIDE, STRIDE);
        System.arraycopy(prim, i3 * STRIDE, quadBuf, quadFloats + 3 * STRIDE, STRIDE);
        quadFloats += 4 * STRIDE;
    }

    /** The current line primitive as vertex pairs. */
    private void toLines(int m, int n) {
        quadFloats = 0;
        if (m == GL11.GL_LINES) {
            for (int i = 0; i + 1 < n; i += 2) pair(i, i + 1);
        } else {
            for (int i = 0; i + 1 < n; i++) pair(i, i + 1);
            if (m == GL11.GL_LINE_LOOP && n > 2) pair(n - 1, 0);
        }
    }

    private void pair(int i0, int i1) {
        if (quadFloats + 2 * STRIDE > quadBuf.length) quadBuf = Arrays.copyOf(quadBuf, quadBuf.length * 2);
        System.arraycopy(prim, i0 * STRIDE, quadBuf, quadFloats, STRIDE);
        System.arraycopy(prim, i1 * STRIDE, quadBuf, quadFloats + STRIDE, STRIDE);
        quadFloats += 2 * STRIDE;
    }

    /** Lines: transformed pairs into the line batch; u = width, normal = direction. */
    private void emitLines(float[] q, int floats, boolean colored) {
        if (soot || ghost != 0) return;
        Key k = new Key(null, Blend.LINES, false, fullBright, false);
        Batch out = batches.get(k);
        if (out == null) {
            out = acquire();
            batches.put(k, out);
        }
        out.ensure(floats);
        float[] d = out.data;
        int o = out.floats;
        Matrix4f m = stack[depth];
        float col = Float.intBitsToFloat(argb(r, g, b, a));
        for (int i = 0; i + 2 * STRIDE <= floats; i += 2 * STRIDE) {
            float x0 = q[i], y0 = q[i + 1], z0 = q[i + 2], x1 = q[i + STRIDE], y1 = q[i + STRIDE + 1], z1 = q[i + STRIDE + 2];
            float ax = m.m00() * x0 + m.m10() * y0 + m.m20() * z0 + m.m30() + ox, ay = m.m01() * x0 + m.m11() * y0 + m.m21() * z0 + m.m31() + oy,
                az = m.m02() * x0 + m.m12() * y0 + m.m22() * z0 + m.m32() + oz;
            float bx = m.m00() * x1 + m.m10() * y1 + m.m20() * z1 + m.m30() + ox, by = m.m01() * x1 + m.m11() * y1 + m.m21() * z1 + m.m31() + oy,
                bz = m.m02() * x1 + m.m12() * y1 + m.m22() * z1 + m.m32() + oz;
            float dx = bx - ax, dy = by - ay, dz = bz - az, len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0) {
                dx /= len;
                dy /= len;
                dz /= len;
            }
            for (int v = 0; v < 2; v++) {
                int src = i + v * STRIDE;
                d[o] = v == 0 ? ax : bx;
                d[o + 1] = v == 0 ? ay : by;
                d[o + 2] = v == 0 ? az : bz;
                d[o + 3] = lineWidth;
                d[o + 4] = 0;
                d[o + 5] = colored ? q[src + 5] : col;
                d[o + 6] = dx;
                d[o + 7] = dy;
                d[o + 8] = dz;
                o += STRIDE;
            }
        }
        out.floats = o;
    }

    private void replay(Prim p) {
        if (discard) return;
        if (bake != null && !blend && !soot && ghost == 0 && !bakePause
            && bake.skip(p, stack[depth], p.colored ? 0 : argb(r, g, b, a), texture2d ? texture : null, cull, fullBright, lighting)) {
            if (BakeStats.ON) BakeStats.add(BakeStats.SKIPPED, p.floats / STRIDE);
            return;
        }
        if (BakeStats.ON) {
            Matrix4f m = stack[depth], b0 = stack[0];
            long h = 17;
            h = h * 31 + Math.round((m.m30() - b0.m30()) * 1024f);
            h = h * 31 + Math.round((m.m31() - b0.m31()) * 1024f);
            h = h * 31 + Math.round((m.m32() - b0.m32()) * 1024f);
            h = h * 31 + Math.round(m.m00() * 4096f);
            h = h * 31 + Math.round(m.m01() * 4096f);
            h = h * 31 + Math.round(m.m02() * 4096f);
            h = h * 31 + Math.round(m.m10() * 4096f);
            h = h * 31 + Math.round(m.m11() * 4096f);
            h = h * 31 + Math.round(m.m12() * 4096f);
            h = h * 31 + Math.round(m.m20() * 4096f);
            h = h * 31 + Math.round(m.m21() * 4096f);
            h = h * 31 + Math.round(m.m22() * 4096f);
            if (!p.colored) h = h * 31 + argb(r, g, b, a);
            h = h * 31 + (texture2d && texture != null ? texture.hashCode() : 0);
            h = h * 31 + (cull ? 1 : 0) + (fullBright ? 2 : 0) + (lighting ? 4 : 0);
            BakeStats.list(p.quads, p.floats, p.floats / STRIDE, h, blend, soot || ghost != 0);
        }
        emit(p.quads, p.floats, p.colored);
    }

    /** Transform quads (list or primitive space) by the current matrix into the current state's batch. */
    private void emit(float[] q, int floats, boolean colored) {
        // soot over glow (additive halos, faint alpha light planes) would darken empty space: 1.12's overlay kept their
        // blending, where a soot colour adds next to nothing or is nearly transparent
        if (soot && blend && (blendMode == Blend.ADDITIVE || a < 0.5f)) return;
        if (ghost != 0 && blend && blendMode == Blend.ADDITIVE) return;   // (no glow on a preview)
        Batch out = batch();
        out.ensure(floats);
        float[] d = out.data;
        int o = out.floats;
        Matrix4f m = stack[depth];
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m10 = m.m10(), m11 = m.m11(), m12 = m.m12(),
            m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m30 = m.m30() + ox, m31 = m.m31() + oy, m32 = m.m32() + oz;
        boolean flat = !lighting;
        float fx = 0, fy = 1, fz = 0;
        if (flat) {
            // "up" in the base (pose) space
            baseNormal.transform(0, 1, 0, tmpN);
            tmpN.normalize();
            fx = tmpN.x;
            fy = tmpN.y;
            fz = tmpN.z;
        } else {
            m.normal(normalMatrix);
        }
        float n00 = normalMatrix.m00, n01 = normalMatrix.m01, n02 = normalMatrix.m02, n10 = normalMatrix.m10,
            n11 = normalMatrix.m11, n12 = normalMatrix.m12, n20 = normalMatrix.m20, n21 = normalMatrix.m21, n22 = normalMatrix.m22;
        float col = soot ? SOOT : Float.intBitsToFloat(argb(r, g, b, a));
        if (soot) colored = false;
        if (ghost != 0) {
            col = Float.intBitsToFloat(argb(r * ((ghost >> 16) & 255) / 255f, g * ((ghost >> 8) & 255) / 255f, b * (ghost & 255) / 255f,
                ((ghost >>> 24) & 255) / 255f));
            colored = false;
        }
        for (int i = 0; i < floats; i += STRIDE) {
            float x = q[i], y = q[i + 1], z = q[i + 2];
            d[o] = m00 * x + m10 * y + m20 * z + m30;
            d[o + 1] = m01 * x + m11 * y + m21 * z + m31;
            d[o + 2] = m02 * x + m12 * y + m22 * z + m32;
            d[o + 3] = q[i + 3];
            d[o + 4] = q[i + 4];
            d[o + 5] = colored ? q[i + 5] : col;
            if (flat) {
                d[o + 6] = fx;
                d[o + 7] = fy;
                d[o + 8] = fz;
            } else {
                float a0 = q[i + 6], a1 = q[i + 7], a2 = q[i + 8];
                float bx = n00 * a0 + n10 * a1 + n20 * a2, by = n01 * a0 + n11 * a1 + n21 * a2, bz = n02 * a0 + n12 * a1 + n22 * a2;
                float len = (float) Math.sqrt(bx * bx + by * by + bz * bz);
                if (len > 0) {
                    float inv = 1f / len;
                    bx *= inv;
                    by *= inv;
                    bz *= inv;
                }
                d[o + 6] = bx;
                d[o + 7] = by;
                d[o + 8] = bz;
            }
            o += STRIDE;
        }
        out.floats = o;
    }

    private final org.joml.Vector3f tmpN = new org.joml.Vector3f();
}
