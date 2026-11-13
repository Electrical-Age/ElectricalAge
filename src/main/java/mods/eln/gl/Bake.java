package mods.eln.gl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Which of a block entity's display-list geometry the chunk mesh draws in place of the block entity renderer.
 * <p>
 * The renderer runs the device's draw code every frame as before. Each display-list primitive it replays (opaque,
 * texture coordinates within the texture) is described by a {@link Call}: the primitive, its matrix, colour, texture,
 * state and light. A call that has been the same for a while is <i>settled</i>: the {@link Host} puts it into the
 * chunk sections its quads lie in and has them re-meshed. Once the mesh on screen contains it, {@link Device#skip}
 * answers true and the renderer leaves it out. A call that changes or stops being drawn leaves the sections at once
 * and is drawn by the renderer again, so nothing is ever assumed to be static: the comparison every frame is the proof.
 * A primitive that keeps changing after it settled (a blinking part) waits longer each time before it settles again.
 * <p>
 * Render thread only, except that the meshing workers read a settled call's geometry and state (everything but
 * {@link Call#order}; those fields are not written after it settled).
 */
public final class Bake {
    private Bake() {
    }

    /**
     * 0 = off. 1 = on. 2 = debug: the renderer leaves out every settled call without asking whether the chunk mesh
     * shows it (what is on screen is then what the chunk draws).
     */
    public static int mode = initialMode();

    private static int initialMode() {
        String e = System.getenv("ELN_BAKE");
        if ("force".equals(e)) return 2;
        if ("1".equals(e) || Boolean.getBoolean("eln.bake")) return 1;
        return 0;
    }

    /** The client side: sprites, chunk sections and their meshes. */
    public interface Host {
        /** Whether the texture (null = untextured) is in the block atlas. */
        boolean hasSprite(Identifier texture);

        /** The call joins the sections of {@link Call#sections}. */
        void settled(Call c);

        /** The call leaves its sections. */
        void unsettled(Call c);

        /** Whether the section was drawn last frame (a call in a section other than its device's is skipped only then). */
        boolean visible(long section);

        /** Once per rendered frame, before any device is drawn. */
        void frame();

        String stats();
    }

    public static Host host;

    /**
     * Why {@link Device#skip} last answered false, and why a frame has no device at all (statistics). CHANGED = not
     * what it was last frame; WAITING = the same, but not for long enough yet; WAITING_LONG = the same, but the part
     * changed soon after it last settled and waits longer; NOT_SHOWN = settled, the mesh on screen does not contain
     * it yet; HIDDEN = some of it lies in another section, which is not drawn.
     */
    public static final int NONE = 0, NOT_ATTACHED = 1, BURNT = 2, POSE = 3, NO_BLOCK_ENTITY = 4, CHANGED = 5, WAITING = 6,
        WAITING_LONG = 7, NO_SPRITE = 8, NO_QUADS = 9, NOT_SHOWN = 10, HIDDEN = 11, REASONS = 12;
    static int why;
    /** What made a call differ from the device's last draw of the same primitive (statistics). */
    public static final int NEW = 0, BY_MATRIX = 1, BY_LIGHT = 2, BY_STATE = 3, CHANGES = 4;

    /** One replay of a display-list primitive. */
    public static final class Call {
        final GlFrame.Prim prim;
        public final Device device;
        /** The primitive's quads (list space, {@link GlFrame#STRIDE} floats a vertex); colored = they carry their own colour. */
        public final float[] quads;
        public final int floats;
        public final boolean colored;
        /** List space to block space (the device's block corner is the origin). */
        public float m00, m01, m02, m10, m11, m12, m20, m21, m22, m30, m31, m32;
        public int color, light;
        public Identifier texture;
        public boolean cull, fullBright, lit;
        /** The device's block (BlockPos.asLong). */
        public final long pos;

        long since, settledAt;
        boolean settled;
        int stamp;
        /** Its place in the device's last draw (render thread only; rewritten every frame). */
        int order;
        /** The sections holding this call's quads (SectionPos.asLong); set when it settles. */
        public long[] sections;
        /** In how many of them the mesh on screen contains this call (kept by the host). */
        public int activeIn;
        /** Some of its quads lie in a section other than its device's. */
        public boolean foreign;

        /** Whether it is in its sections now (false again once it changed or was no longer drawn). */
        public boolean isSettled() {
            return settled;
        }

        Call(Device device, GlFrame.Prim p) {
            this.device = device;
            this.pos = device.pos;
            prim = p;
            quads = p.quads;
            floats = p.floats;
            colored = p.colored;
        }

        void set(Matrix4f m, int color, Identifier texture, boolean cull, boolean fullBright, boolean lit, int light) {
            m00 = m.m00();
            m01 = m.m01();
            m02 = m.m02();
            m10 = m.m10();
            m11 = m.m11();
            m12 = m.m12();
            m20 = m.m20();
            m21 = m.m21();
            m22 = m.m22();
            m30 = m.m30();
            m31 = m.m31();
            m32 = m.m32();
            this.color = color;
            this.texture = texture;
            this.cull = cull;
            this.fullBright = fullBright;
            this.lit = lit;
            this.light = light;
        }

        boolean at(Matrix4f m) {
            return m30 == m.m30() && m31 == m.m31() && m32 == m.m32() && m00 == m.m00() && m11 == m.m11() && m22 == m.m22()
                && m01 == m.m01() && m02 == m.m02() && m10 == m.m10() && m12 == m.m12() && m20 == m.m20() && m21 == m.m21();
        }

        boolean same(Matrix4f m, int color, Identifier texture, boolean cull, boolean fullBright, boolean lit, int light) {
            return at(m) && this.color == color && this.light == light && this.cull == cull && this.fullBright == fullBright && this.lit == lit
                && (this.texture == texture || (texture != null && texture.equals(this.texture)));
        }
    }

    /** The section holding the quad that starts at float index i of the call's quads: the one its centre is in. */
    public static long sectionOf(Call c, int i) {
        float[] q = c.quads;
        int s = GlFrame.STRIDE;
        float x = (q[i] + q[i + s] + q[i + 2 * s] + q[i + 3 * s]) * 0.25f;
        float y = (q[i + 1] + q[i + s + 1] + q[i + 2 * s + 1] + q[i + 3 * s + 1]) * 0.25f;
        float z = (q[i + 2] + q[i + s + 2] + q[i + 2 * s + 2] + q[i + 3 * s + 2]) * 0.25f;
        int bx = BlockPos.getX(c.pos) + Mth.floor(c.m00 * x + c.m10 * y + c.m20 * z + c.m30);
        int by = BlockPos.getY(c.pos) + Mth.floor(c.m01 * x + c.m11 * y + c.m21 * z + c.m31);
        int bz = BlockPos.getZ(c.pos) + Mth.floor(c.m02 * x + c.m12 * y + c.m22 * z + c.m32);
        return SectionPos.asLong(bx >> 4, by >> 4, bz >> 4);
    }

    private static long[] sectionsOf(Call c) {
        long first = 0;
        long[] more = null;
        int n = 0;
        for (int i = 0; i + 4 * GlFrame.STRIDE <= c.floats; i += 4 * GlFrame.STRIDE) {
            long s = sectionOf(c, i);
            if (n == 0) {
                first = s;
                n = 1;
                continue;
            }
            if (s == first) continue;
            boolean known = false;
            for (int k = 0; k < n - 1; k++) {
                if (more[k] == s) {
                    known = true;
                    break;
                }
            }
            if (known) continue;
            if (more == null) more = new long[4];
            else if (n - 1 == more.length) more = java.util.Arrays.copyOf(more, more.length * 2);
            more[n - 1] = s;
            n++;
        }
        long[] out = new long[n];
        if (n > 0) out[0] = first;
        if (n > 1) System.arraycopy(more, 0, out, 1, n - 1);
        return out;
    }

    /**
     * The calls in the order the chunk writes them: each device's in the order of its draw. Where a device draws two
     * things in the same place the later one covers the earlier, in the chunk as in 1.12, whichever settled first.
     * Render thread only.
     */
    public static Call[] inDrawOrder(java.util.Collection<Call> calls) {
        Call[] out = calls.toArray(new Call[0]);
        java.util.Arrays.sort(out, (a, b) -> a.device != b.device ? Integer.compare(a.device.id, b.device.id) : Integer.compare(a.order, b.order));
        return out;
    }

    /** How long a call must stay the same before it settles, and the most a primitive that keeps changing waits. */
    static final long SETTLE_NS = 400_000_000L, SETTLE_MAX_NS = 60_000_000_000L;

    private static long now;
    private static long frame;
    private static long settles, drops;
    private static int settledCalls;
    private static long settledVertices;
    private static int devices;

    private static final Map<Object, Device> DEVICES = new IdentityHashMap<>();

    /** A new rendered frame at this time (nanoseconds). */
    static void frame(long nanos) {
        now = nanos;
        frame++;
        if (mode != 0 && host != null) host.frame();
    }

    /** The device of a block entity (owner), about to be drawn; null when baking is off. */
    static Device begin(Object owner, long pos, int light) {
        if (host == null) return null;
        Device d = DEVICES.get(owner);
        if (d == null) {
            d = new Device(owner, pos);
            DEVICES.put(owner, d);
        }
        d.begin(light);
        return d;
    }

    /** Forget the devices whose owner the test accepts (block entities that are gone); their calls leave the chunk. */
    public static void sweep(Predicate<Object> gone) {
        for (Iterator<Device> it = DEVICES.values().iterator(); it.hasNext(); ) {
            Device d = it.next();
            if (!gone.test(d.owner)) continue;
            for (int i = 0; i < d.prevN; i++) {
                Call c = d.prev[i];
                if (c.settled) d.unsettle(c, false);
            }
            it.remove();
        }
    }

    /** Forget everything without telling the host (the level changed, or the atlas was rebuilt). */
    public static void clear() {
        DEVICES.clear();
        settledCalls = 0;
        settledVertices = 0;
    }

    static String statsLine(int frames) {
        String s = "bake mode " + mode + ": " + DEVICES.size() + " devices, " + settledCalls + " settled calls (" + settledVertices
            + " vertices), " + settles + " settled and " + drops + " dropped in " + frames + " frames";
        settles = drops = 0;
        return host == null ? s : s + "; " + host.stats();
    }

    /** A block entity's calls: last frame's, matched against this frame's as they come. */
    public static final class Device {
        public final Object owner;
        final long pos, home;
        final int id = devices++;
        private int light;
        private Call[] prev = new Call[8], cur = new Call[8];
        private int prevN, curN, cursor, pass;
        private long lastFrame;
        /** Settle times of the primitives that changed soon after settling. */
        private IdentityHashMap<GlFrame.Prim, Long> slow;

        Device(Object owner, long pos) {
            this.owner = owner;
            this.pos = pos;
            home = SectionPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getY(pos) >> 4, BlockPos.getZ(pos) >> 4);
        }

        void begin(int light) {
            this.light = light;
            cursor = 0;
            curN = 0;
            pass++;
            if (frame - lastFrame > 1) {
                // not drawn for a while: what it did meanwhile is unknown, so the wait starts again
                for (int i = 0; i < prevN; i++) if (!prev[i].settled) prev[i].since = now;
            }
            lastFrame = frame;
        }

        private long settleTime(GlFrame.Prim p) {
            if (slow == null) return SETTLE_NS;
            Long t = slow.get(p);
            return t == null ? SETTLE_NS : t;
        }

        /**
         * Account for one replay of a primitive with the given matrix and state. True = the chunk mesh on screen
         * draws exactly this, so the renderer must not.
         */
        boolean skip(GlFrame.Prim p, Matrix4f m, int color, Identifier texture, boolean cull, boolean fullBright, boolean lit) {
            why = NONE;
            if (!p.uvInUnitSquare()) return false;
            Call c = null, spare = null, near = null;
            int n = prevN;
            for (int k = 0; k < n; k++) {
                int i = cursor + k;
                if (i >= n) i -= n;
                Call o = prev[i];
                if (o.stamp == pass || o.prim != p) continue;
                if (o.same(m, color, texture, cull, fullBright, lit, light)) {
                    c = o;
                    cursor = i + 1 == n ? 0 : i + 1;
                    break;
                }
                if (!o.settled && spare == null) spare = o;
                if (near == null) near = o;
            }
            boolean changed = c == null;
            if (changed) {
                if (!host.hasSprite(texture)) {
                    why = NO_SPRITE;
                    if (BakeStats.ON) BakeStats.noSprite(texture);
                    return false;
                }
                if (BakeStats.ON) BakeStats.changed(near == null ? NEW : !near.at(m) ? BY_MATRIX
                    : near.same(m, color, texture, cull, fullBright, lit, near.light) ? BY_LIGHT : BY_STATE, near != null && near.settled);
                // a part in motion reuses its object from last frame
                c = spare != null ? spare : new Call(this, p);
                c.set(m, color, texture, cull, fullBright, lit, light);
                c.since = now;
            }
            if (BakeStats.ON) {
                for (int i = 0; i < curN; i++) {
                    if (cur[i].prim == p && cur[i].at(m)) {
                        BakeStats.twiceInPlace();
                        break;
                    }
                }
            }
            c.stamp = pass;
            c.order = curN;
            if (curN == cur.length) cur = java.util.Arrays.copyOf(cur, curN * 2);
            cur[curN++] = c;
            if (!c.settled) {
                long wait = settleTime(p);
                if (now - c.since < wait) {
                    why = changed ? CHANGED : wait == SETTLE_NS ? WAITING : WAITING_LONG;
                    return false;
                }
                c.sections = sectionsOf(c);
                if (c.sections.length == 0) {
                    why = NO_QUADS;
                    return false;
                }
                c.foreign = c.sections.length > 1 || c.sections[0] != home;
                c.settled = true;
                c.settledAt = now;
                settles++;
                settledCalls++;
                settledVertices += c.floats / GlFrame.STRIDE;
                host.settled(c);
            }
            if (mode == 2) return true;
            if (c.activeIn != c.sections.length) {
                why = NOT_SHOWN;
                return false;
            }
            if (c.foreign) {
                for (long s : c.sections) {
                    if (s != home && !host.visible(s)) {
                        why = HIDDEN;
                        return false;
                    }
                }
            }
            return true;
        }

        private void unsettle(Call c, boolean penalty) {
            c.settled = false;
            drops++;
            settledCalls--;
            settledVertices -= c.floats / GlFrame.STRIDE;
            host.unsettled(c);
            if (!penalty) return;
            long t = settleTime(c.prim);
            if (now - c.settledAt < 16 * t) {
                if (slow == null) slow = new IdentityHashMap<>();
                slow.put(c.prim, Math.min(t * 2, SETTLE_MAX_NS));
            } else if (slow != null) slow.remove(c.prim);
        }

        /** The draw is over: what was not drawn again leaves the chunk. */
        void end() {
            for (int i = 0; i < prevN; i++) {
                Call c = prev[i];
                if (c.stamp != pass && c.settled) unsettle(c, true);
                prev[i] = null;
            }
            Call[] t = prev;
            prev = cur;
            cur = t;
            prevN = curN;
            curN = 0;
        }

        /** The calls of the last draw (tests). */
        public ArrayList<Call> calls() {
            ArrayList<Call> out = new ArrayList<>();
            for (int i = 0; i < prevN; i++) out.add(prev[i]);
            return out;
        }
    }
}
