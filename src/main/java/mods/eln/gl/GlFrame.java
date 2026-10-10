package mods.eln.gl;

import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The state machine behind {@link GL11}: one frame per drawn node (opened by the client renderer, which then submits
 * {@link #batches()}). Vertices are transformed by the frame's own matrix stack into the node's frame and grouped by
 * {@link Key} (texture, blending, culling, lighting); every polygon mode becomes quads (a triangle repeats its last
 * vertex). Outside a frame every call goes to a frame that discards, so stray draw calls do no harm.
 * Render thread only.
 */
public final class GlFrame {
    /**
     * What selects the RenderType of a vertex batch. texture null = untextured (drawn with a white texture);
     * fullBright = the 1.12 lightmap override (drawn at full light). GL_LIGHTING off only flattens the shading (normals
     * point up).
     */
    public record Key(Identifier texture, Blend blend, boolean cull, boolean fullBright) {
    }

    public enum Blend {NONE, ALPHA, ADDITIVE, MULTIPLY}

    /** Vertex layout in a batch: x y z u v argb(bits) nx ny nz. */
    public static final int STRIDE = 9;

    public static final class Batch {
        public float[] data = new float[STRIDE * 64];
        public int floats;

        void ensure(int more) {
            if (floats + more > data.length) data = java.util.Arrays.copyOf(data, Math.max(data.length * 2, floats + more));
        }

        public int vertices() {
            return floats / STRIDE;
        }
    }

    private static final GlFrame DISCARD = new GlFrame(true);
    private static final GlFrame FRAME = new GlFrame(false);
    private static GlFrame current = DISCARD;

    /** The open frame, or the discarding one. */
    public static GlFrame current() {
        return current;
    }

    /** Open the frame for one node's draw: identity matrix, 1.12 TESR defaults (texture, lighting and culling on). */
    public static GlFrame open() {
        FRAME.reset();
        current = FRAME;
        return FRAME;
    }

    /** Close the frame; its batches stay readable until the next open. */
    public static void close() {
        current = DISCARD;
    }

    private final boolean discard;

    private GlFrame(boolean discard) {
        this.discard = discard;
    }

    // ---- state
    private final Matrix4f[] stack = new Matrix4f[64];
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

    void reset() {
        depth = 0;
        stack[0].identity();
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
        droppedVertices = 0;
        rec = null;
        mode = -1;
    }

    public Map<Key, Batch> batches() {
        return batches;
    }

    // ---- display-list recording
    private static final Map<Integer, List<Consumer<GlFrame>>> LISTS = new HashMap<>();
    private static int nextList = 1;
    private List<Consumer<GlFrame>> rec;
    private boolean recExecute;
    private int recList;

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
    }

    void endList() {
        if (rec == null) return;
        synchronized (GlFrame.class) {
            LISTS.put(recList, rec);
        }
        rec = null;
    }

    void callList(int list) {
        if (record(f -> f.callList(list))) return;
        List<Consumer<GlFrame>> ops;
        synchronized (GlFrame.class) {
            ops = LISTS.get(list);
        }
        if (ops != null) for (Consumer<GlFrame> op : ops) op.accept(this);
    }

    /** While compiling a list: record op; returns true if it must not run now (GL_COMPILE). */
    private boolean record(Consumer<GlFrame> op) {
        if (rec == null) return false;
        rec.add(op);
        return !recExecute;
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
        stack[depth].identity();
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

    void scale(float x, float y, float z) {
        if (record(f -> f.scale(x, y, z))) return;
        stack[depth].scale(x, y, z);
    }

    // ---- attributes
    void color(float r, float g, float b, float a) {
        if (record(f -> f.color(r, g, b, a))) return;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    void normal(float x, float y, float z) {
        if (record(f -> f.normal(x, y, z))) return;
        nx = x;
        ny = y;
        nz = z;
    }

    void texCoord(float u, float v) {
        if (record(f -> f.texCoord(u, v))) return;
        this.u = u;
        this.v = v;
    }

    void texture(Identifier t) {
        if (record(f -> f.texture(t))) return;
        texture = t;
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
    private Batch primBatch;
    private final Matrix3f normalMatrix = new Matrix3f();
    private float[] prim = new float[STRIDE * 16];
    private int primFloats;
    private final Vector3f tmp = new Vector3f();
    private boolean flat;

    void begin(int mode) {
        if (record(f -> f.begin(mode))) return;
        this.mode = mode;
        primFloats = 0;
        if (discard) return;
        stack[depth].normal(normalMatrix);
        Key key = new Key(texture2d ? texture : null, blend ? blendMode : Blend.NONE, cull, fullBright);
        flat = !lighting;
        primBatch = batches.computeIfAbsent(key, k -> new Batch());
    }

    void vertex(float x, float y, float z) {
        if (record(f -> f.vertex(x, y, z))) return;
        if (discard || mode < 0) return;
        if (primFloats + STRIDE > prim.length) prim = java.util.Arrays.copyOf(prim, prim.length * 2);
        stack[depth].transformPosition(x, y, z, tmp);
        prim[primFloats] = tmp.x;
        prim[primFloats + 1] = tmp.y;
        prim[primFloats + 2] = tmp.z;
        prim[primFloats + 3] = u;
        prim[primFloats + 4] = v;
        int argb = (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        prim[primFloats + 5] = Float.intBitsToFloat(argb);
        if (flat) tmp.set(0, 1, 0);
        else {
            normalMatrix.transform(nx, ny, nz, tmp);
            float len = tmp.length();
            if (len > 0) tmp.div(len);
        }
        prim[primFloats + 6] = tmp.x;
        prim[primFloats + 7] = tmp.y;
        prim[primFloats + 8] = tmp.z;
        primFloats += STRIDE;
    }

    private static int clamp(float c) {
        return Math.max(0, Math.min(255, Math.round(c * 255f)));
    }

    void end() {
        if (record(GlFrame::end)) return;
        int m = mode;
        mode = -1;
        if (discard || primBatch == null) return;
        int n = primFloats / STRIDE;
        Batch out = primBatch;
        primBatch = null;
        switch (m) {
            case GL11.GL_QUADS -> {
                for (int i = 0; i + 3 < n; i += 4) quad(out, i, i + 1, i + 2, i + 3);
            }
            case GL11.GL_TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) quad(out, i, i + 1, i + 2, i + 2);
            }
            case GL11.GL_QUAD_STRIP -> {
                for (int i = 0; i + 3 < n; i += 2) quad(out, i, i + 1, i + 3, i + 2);
            }
            case GL11.GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < n; i++) {
                    if ((i & 1) == 0) quad(out, i, i + 1, i + 2, i + 2);
                    else quad(out, i + 1, i, i + 2, i + 2);
                }
            }
            case GL11.GL_TRIANGLE_FAN, GL11.GL_POLYGON -> {
                for (int i = 1; i + 1 < n; i++) quad(out, 0, i, i + 1, i + 1);
            }
            default -> droppedVertices += n; // lines, points
        }
    }

    private void quad(Batch out, int i0, int i1, int i2, int i3) {
        out.ensure(4 * STRIDE);
        System.arraycopy(prim, i0 * STRIDE, out.data, out.floats, STRIDE);
        System.arraycopy(prim, i1 * STRIDE, out.data, out.floats + STRIDE, STRIDE);
        System.arraycopy(prim, i2 * STRIDE, out.data, out.floats + 2 * STRIDE, STRIDE);
        System.arraycopy(prim, i3 * STRIDE, out.data, out.floats + 3 * STRIDE, STRIDE);
        out.floats += 4 * STRIDE;
    }
}
