package mods.eln.gl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
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
 * texture coordinates within the texture) is described by a {@link Call}: the primitive, its matrix, colour, texture
 * and state. A call that has been the same for a while is <i>settled</i>: the {@link Host} puts it into the
 * chunk section of the device's block and has it re-meshed. Once the mesh on screen contains it, {@link Device#skip}
 * answers true and the renderer leaves it out. A call that changes or stops being drawn leaves the section at once
 * and is drawn by the renderer again, so nothing is ever assumed to be static: the comparison every frame is the proof.
 * A primitive that keeps changing after it settled (a blinking part) waits longer each time before it settles again.
 * <p>
 * The game draws a block entity only within its renderer's range, and the chunk much farther. Beyond that range a
 * device is drawn now and then into a frame that puts nothing on screen ({@link #farDue}), so that the chunk shows
 * it without the player having been near, and follows what it does; see {@link Device}.
 * <p>
 * All of a device's geometry goes into the section of its block, however far the model reaches: the game draws a
 * block entity only while that section is drawn, so the chunk shows the device exactly when the renderer would. (A
 * section holding nothing but air is never compiled, so a part could not be put where it lies.)
 * <p>
 * The light a device is drawn with is not part of a call. The host reads it where the device stands whenever the
 * section is meshed, and the game meshes a section again when light in it changes, as for the blocks around.
 * <p>
 * Render thread only, except that the meshing workers read a settled call's geometry and state (everything but
 * {@link Call#order}; those fields are not written after it settled).
 */
public final class Bake {
    private Bake() {
    }

    /**
     * 0 = off. 1 = on. 2 = debug: the renderer leaves out every settled call without asking whether the chunk mesh
     * shows it (what is on screen is then what the chunk draws). Set by the host from the client's config.
     */
    public static int mode;

    /** The mode that -Deln.bake or ELN_BAKE (off, on, force) asks for in place of the config's; -1 = neither is set. */
    public static final int OVERRIDE = override(System.getProperty("eln.bake", System.getenv("ELN_BAKE")));

    static int override(String v) {
        if (v == null) return -1;
        return switch (v) {
            case "off", "0", "false" -> 0;
            case "on", "1", "true" -> 1;
            case "force" -> 2;
            default -> -1;
        };
    }

    /** The client side: sprites, chunk sections and their meshes. */
    public interface Host {
        /** Whether the texture (null = untextured) is in the block atlas. */
        boolean hasSprite(Identifier texture);

        /** The call joins the section of its device ({@link Device#home}). */
        void settled(Call c);

        /** The call leaves its section. */
        void unsettled(Call c);

        /** Once per rendered frame, before any device is drawn. */
        void frame();

        String stats();
    }

    public static Host host;

    /**
     * Why {@link Device#skip} last answered false, and why a frame has no device at all (statistics). CHANGED = not
     * what it was last frame; WAITING = the same, but not for long enough yet; WAITING_LONG = the same, but the part
     * changed soon after it last settled and waits longer; NOT_SHOWN = settled, the mesh on screen does not contain
     * it yet.
     */
    public static final int NONE = 0, NOT_ATTACHED = 1, BURNT = 2, POSE = 3, NO_BLOCK_ENTITY = 4, CHANGED = 5, WAITING = 6,
        WAITING_LONG = 7, NO_SPRITE = 8, NO_QUADS = 9, NOT_SHOWN = 10, REASONS = 11;
    static int why;
    /** What made a call differ from the device's last draw of the same primitive (statistics). */
    public static final int NEW = 0, BY_MATRIX = 1, BY_STATE = 2, CHANGES = 3;

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
        public int color;
        public Identifier texture;
        public boolean cull, fullBright, lit;
        /** The device's block (BlockPos.asLong). */
        public final long pos;

        long since, settledAt;
        boolean settled;
        int stamp;
        /** Its place in the device's last draw (render thread only; rewritten every frame). */
        int order;
        /** Whether the mesh on screen contains this call (kept by the host). */
        public boolean shown;

        /** Whether it is in its section now (false again once it changed or was no longer drawn). */
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

        void set(Matrix4f m, int color, Identifier texture, boolean cull, boolean fullBright, boolean lit) {
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
        }

        boolean at(Matrix4f m) {
            return m30 == m.m30() && m31 == m.m31() && m32 == m.m32() && m00 == m.m00() && m11 == m.m11() && m22 == m.m22()
                && m01 == m.m01() && m02 == m.m02() && m10 == m.m10() && m12 == m.m12() && m20 == m.m20() && m21 == m.m21();
        }

        boolean same(Matrix4f m, int color, Identifier texture, boolean cull, boolean fullBright, boolean lit) {
            return at(m) && this.color == color && this.cull == cull && this.fullBright == fullBright && this.lit == lit
                && (this.texture == texture || (texture != null && texture.equals(this.texture)));
        }
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
    /**
     * Far frames. A run lasts RUN_NS at most. A device is looked at again RETRY_NS after a run that changed what the
     * chunk holds, and twice as long after each run that did not, up to RETRY_MAX_NS (and up to a quarter longer, so
     * that the devices of a base do not all come due in one frame). A packet brings the next run forward, to at
     * least RETRY_NS after a run that changed nothing.
     */
    static final long RUN_NS = 3_000_000_000L, RETRY_NS = 10_000_000_000L, RETRY_MAX_NS = 160_000_000_000L;
    /** The most far frames in one rendered frame; a device over it stays due. */
    static final int FAR_PER_FRAME = 64;

    private static long now;
    private static long frame;
    private static long settles, drops;
    private static int settledCalls;
    private static long settledVertices;
    private static int devices;
    private static int farGranted, heldCalls;
    private static long farFrames, runsSettled, runsGivenUp;

    private static final Map<Object, Device> DEVICES = new IdentityHashMap<>();

    /** A new rendered frame at this time (nanoseconds). */
    static void frame(long nanos) {
        now = nanos;
        frame++;
        farGranted = 0;
        if (mode != 0 && host != null) host.frame();
    }

    /**
     * Whether a block entity the game would not draw (it is beyond the renderer's range) is to be drawn into a far
     * frame now: a frame that puts nothing on screen and only keeps the chunk's copy of the device current. To be
     * asked in every rendered frame in which the device is in view; true = the caller draws it far in this frame.
     */
    public static boolean farDue(Object owner) {
        if (mode == 0 || host == null) return false;
        Device d = DEVICES.get(owner);
        if (d != null && !d.due()) return false;
        if (farGranted >= FAR_PER_FRAME) return false;
        farGranted++;
        if (d != null) d.granted = frame;
        return true;
    }

    /** The server sent the block entity new state: if it is drawn far, it is looked at again soon. */
    public static void published(Object owner) {
        Device d = DEVICES.get(owner);
        if (d != null) d.published = true;
    }

    /** The device of a block entity (owner), about to be drawn (far = into a far frame); null when baking is off. */
    static Device begin(Object owner, long pos, boolean far) {
        if (host == null) return null;
        Device d = DEVICES.get(owner);
        if (d == null) {
            d = new Device(owner, pos);
            DEVICES.put(owner, d);
        }
        d.begin(far);
        return d;
    }

    /** Forget the devices whose owner the test accepts (block entities that are gone); their calls leave the chunk. */
    public static void sweep(Predicate<Object> gone) {
        for (Iterator<Device> it = DEVICES.values().iterator(); it.hasNext(); ) {
            Device d = it.next();
            if (!gone.test(d.owner)) continue;
            for (int i = 0; i < d.prevN; i++) {
                Call c = d.prev[i];
                if (c.settled) d.unsettle(c);
            }
            d.release();
            it.remove();
        }
    }

    /** Forget everything without telling the host (the level changed, or the atlas was rebuilt). */
    public static void clear() {
        DEVICES.clear();
        settledCalls = 0;
        settledVertices = 0;
        heldCalls = 0;
    }

    static String statsLine(int frames) {
        String s = "bake mode " + mode + ": " + DEVICES.size() + " devices, " + settledCalls + " settled calls (" + settledVertices
            + " vertices), " + settles + " settled and " + drops + " dropped in " + frames + " frames; far: " + farFrames + " frames, "
            + runsSettled + " runs settled, " + runsGivenUp + " given up, " + heldCalls + " calls held";
        settles = drops = farFrames = runsSettled = runsGivenUp = 0;
        return host == null ? s : s + "; " + host.stats();
    }

    /**
     * A block entity's calls: last frame's, matched against this frame's as they come.
     * <p>
     * Within the renderer's range the device is drawn every frame (a <i>near</i> frame) and the above holds as said.
     * Beyond it the device is only looked at now and then, in <i>far</i> frames that draw nothing ({@link #farDue}):
     * <ul>
     * <li>It is looked at in a <i>run</i> of samples {@link #SETTLE_NS} apart. A run begins with the device's first
     * far frame, after a near frame, after a packet ({@link #published}) and when its time to be looked at again has
     * come. A call has to be the same in every sample of the run over its wait: a weaker proof than every frame, good
     * for as long as nothing draws the device. The first near frame compares again.</li>
     * <li>A settled call that a sample does not find again is <i>held</i>: it stays in its section, so the part does
     * not vanish while what replaced it waits. While calls are held nothing joins the section. A held call that is
     * drawn again is taken back as it is.</li>
     * <li>The run ends <i>settled</i> with a sample, not its first, that finds the device as the sample before did
     * and nothing waiting: the held calls leave and the calls that waited long enough join, in one frame, so one
     * re-mesh swaps old for new.</li>
     * <li>It is <i>given up</i> with the second sample in a row that finds a change while nothing is held and nothing
     * waits (a part that moves: left out far away), and in any case {@link #RUN_NS} after it began, with the same
     * swap.</li>
     * <li>Every run is followed by another some time later, sooner after a packet: for the part that stopped
     * moving, and for what changes without a packet. Devices that send packets all the time, or keep moving, cost two
     * samples every {@link #RETRY_NS} at most.</li>
     * <li>A near frame ends the run; held calls leave before it compares anything.</li>
     * </ul>
     */
    public static final class Device {
        public final Object owner;
        final long pos;
        /** The chunk section of the device's block (SectionPos.asLong): where its settled calls go. */
        public final long home;
        final int id = devices++;
        private Call[] prev = new Call[8], cur = new Call[8];
        private int prevN, curN, cursor, pass;
        /** The last rendered frame that drew the device, and the last that granted it a far frame. */
        private long seen, granted;
        /** Settle times of the primitives that changed soon after settling. */
        private IdentityHashMap<GlFrame.Prim, Long> slow;

        /** Whether the last draw (or the one going on) was a far frame. */
        private boolean far;
        /** In a run; first = its first sample is still to come; moved = it changed what the section holds. */
        private boolean run, first, moved;
        /** This sample differs from the one before (changedBefore: so did that one); a call that is as it was still waits. */
        private boolean changed, changedBefore, waiting;
        /** A packet came since the last run began. */
        private boolean published;
        /** When the run began, its next sample, the next run without a packet, and the earliest run a packet starts. */
        private long runStart, nextSample, retryAt, packetAt;
        private long retryDelay = RETRY_NS;
        /** Settled calls the last samples did not draw; still in the section until the run's swap. */
        private ArrayList<Call> held;

        Device(Object owner, long pos) {
            this.owner = owner;
            this.pos = pos;
            home = SectionPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getY(pos) >> 4, BlockPos.getZ(pos) >> 4);
        }

        /** Whether a far frame is wanted now ({@link Bake#farDue}). */
        private boolean due() {
            if (granted == frame) return false;
            if (!far) return true;
            if (run) return now >= nextSample;
            return now >= retryAt || (published && now >= packetAt);
        }

        void begin(boolean far) {
            cursor = 0;
            curN = 0;
            pass++;
            // not drawn in the last frame: what it did meanwhile is unknown, so the waits start again
            boolean again = frame - seen > 1;
            seen = frame;
            if (!far) {
                // frames prove more than samples do: the waits start again after far frames too
                if (this.far) again = true;
                release();
                run = false;
            } else {
                farFrames++;
                if (run) again = false;   // the samples of a run carry the waits on
                else {
                    if (this.far) again = true;
                    run = first = true;
                    moved = published = changedBefore = false;
                    runStart = now;
                }
            }
            if (again) for (int i = 0; i < prevN; i++) if (!prev[i].settled) prev[i].since = now;
            this.far = far;
            changed = waiting = false;
        }

        private long settleTime(GlFrame.Prim p) {
            if (slow == null) return SETTLE_NS;
            Long t = slow.get(p);
            return t == null ? SETTLE_NS : t;
        }

        /**
         * Account for one replay of a primitive with the given matrix and state. True = the chunk mesh on screen
         * draws exactly this, so the renderer must not. (A far frame draws nothing, whatever the answer.)
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
                if (o.same(m, color, texture, cull, fullBright, lit)) {
                    c = o;
                    cursor = i + 1 == n ? 0 : i + 1;
                    break;
                }
                if (!o.settled && spare == null) spare = o;
                if (near == null) near = o;
            }
            boolean differs = c == null;
            if (differs && held != null) c = takeBack(p, m, color, texture, cull, fullBright, lit);
            if (c == null) {
                if (!host.hasSprite(texture)) {
                    why = NO_SPRITE;
                    if (BakeStats.ON) BakeStats.noSprite(texture);
                    return false;
                }
                if (BakeStats.ON) BakeStats.changed(near == null ? NEW : !near.at(m) ? BY_MATRIX : BY_STATE, near != null && near.settled);
                // a part in motion reuses its object from last frame
                c = spare != null ? spare : new Call(this, p);
                c.set(m, color, texture, cull, fullBright, lit);
                c.since = now;
            }
            if (differs) changed = true;
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
                int w = ready(c);
                if (w != NONE) {
                    why = w == NO_QUADS ? w : differs ? CHANGED : w;
                    if (w != NO_QUADS && !differs) waiting = true;
                    return false;
                }
                if (far) {
                    // joins when the sample is over, unless calls are held
                    why = NOT_SHOWN;
                    return false;
                }
                settle(c);
            }
            if (mode == 2) return true;
            if (!c.shown) {
                why = NOT_SHOWN;
                return false;
            }
            return true;
        }

        /** NONE when an unsettled call has been the same for long enough to settle, else why not. */
        private int ready(Call c) {
            long wait = settleTime(c.prim);
            if (now - c.since < wait) return wait == SETTLE_NS ? WAITING : WAITING_LONG;
            return c.floats < 4 * GlFrame.STRIDE ? NO_QUADS : NONE;
        }

        private void settle(Call c) {
            c.settled = true;
            c.settledAt = now;
            settles++;
            settledCalls++;
            settledVertices += c.floats / GlFrame.STRIDE;
            host.settled(c);
        }

        /** The call leaves its section. */
        private void unsettle(Call c) {
            c.settled = false;
            drops++;
            settledCalls--;
            settledVertices -= c.floats / GlFrame.STRIDE;
            host.unsettled(c);
        }

        /** A settled call was not drawn again: if that is soon after it settled, its primitive waits longer from now on. */
        private void changedAfterSettling(Call c) {
            long t = settleTime(c.prim);
            if (now - c.settledAt < 16 * t) {
                if (slow == null) slow = new IdentityHashMap<>();
                slow.put(c.prim, Math.min(t * 2, SETTLE_MAX_NS));
            } else if (slow != null) slow.remove(c.prim);
        }

        /** The held call that is exactly this replay, taken out of the held ones; null if there is none. */
        private Call takeBack(GlFrame.Prim p, Matrix4f m, int color, Identifier texture, boolean cull, boolean fullBright, boolean lit) {
            for (int i = 0; i < held.size(); i++) {
                Call h = held.get(i);
                if (h.prim != p || !h.same(m, color, texture, cull, fullBright, lit)) continue;
                held.remove(i);
                heldCalls--;
                return h;
            }
            return null;
        }

        /** The held calls leave the section. */
        private void release() {
            if (held == null) return;
            for (Call h : held) unsettle(h);
            heldCalls -= held.size();
            held = null;
            moved = true;
        }

        /** The draw is over: what was not drawn again leaves the chunk (near), or is held (far). */
        void end() {
            for (int i = 0; i < prevN; i++) {
                Call c = prev[i];
                if (c.stamp != pass) {
                    changed = true;
                    if (c.settled) {
                        changedAfterSettling(c);
                        if (far) {
                            if (held == null) held = new ArrayList<>();
                            held.add(c);
                            heldCalls++;
                        } else unsettle(c);
                    }
                }
                prev[i] = null;
            }
            Call[] t = prev;
            prev = cur;
            cur = t;
            prevN = curN;
            curN = 0;
            if (far) sampled();
        }

        /** A far frame is over: swap held calls for those that waited long enough, and see whether the run goes on. */
        private void sampled() {
            boolean quiet = !changed && !waiting && !first;
            boolean late = now - runStart >= RUN_NS;
            // two samples in a row differ from the one before, and nothing else is worth another: a part in motion
            boolean moving = changed && changedBefore && !waiting && held == null;
            first = false;
            changedBefore = changed;
            if (quiet || late) release();
            if (held == null) {
                for (int i = 0; i < prevN; i++) {
                    Call c = prev[i];
                    if (c.settled || ready(c) != NONE) continue;
                    settle(c);
                    moved = true;
                }
            }
            if (!quiet && !late && !moving) {
                nextSample = now + SETTLE_NS;
                return;
            }
            run = false;
            if (quiet) runsSettled++;
            else runsGivenUp++;
            if (moved) retryDelay = RETRY_NS;
            // up to a quarter more, by where the device stands
            retryAt = now + retryDelay + retryDelay / 128 * ((pos * 0x9E3779B97F4A7C15L) >>> 59);
            retryDelay = Math.min(retryDelay * 2, RETRY_MAX_NS);
            packetAt = moved ? now : now + RETRY_NS;
        }

        /** The calls of the last draw (tests). */
        public ArrayList<Call> calls() {
            ArrayList<Call> out = new ArrayList<>();
            for (int i = 0; i < prevN; i++) out.add(prev[i]);
            return out;
        }

        /** Whether the device is in a run of far frames, and how many of its calls are held (tests). */
        public boolean inRun() {
            return run;
        }

        public int held() {
            return held == null ? 0 : held.size();
        }
    }
}
