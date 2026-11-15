package mods.eln.gl;

import java.util.concurrent.atomic.LongAdder;

/**
 * What EA's rendering costs in a client's frame, counted for the render benchmark's client tier
 * (mods.eln.client.RenderBenchClient; todo #424). Off, a hook costs one test of {@link #on}; on, two nanoTime calls
 * per device frame and per vertex batch. No client classes.
 * <p>
 * Times are of the render thread but for the meshing of baked sections, which runs on the chunk workers.
 */
public final class RenderProbe {
    private RenderProbe() {
    }

    /** Set by the benchmark (render thread); read by the hooks, also on chunk workers. */
    public static volatile boolean on;

    /** The counters, in the order of {@link #NAMES}. */
    public static final int RECORD_NS = 0, FAR_NS = 1, SUBMIT_NS = 2, WRITE_NS = 3, REFRESH_NS = 4, SNAPSHOT_NS = 5, DEVICES = 6, FAR_FRAMES = 7,
        BATCHES = 8, WRITE_VERTICES = 9, REFRESHED = 10, SETTLES = 11, DROPS = 12, MESH_NS = 13, MESH_VERTICES = 14, MESH_SECTIONS = 15, N = 16;
    /**
     * recordNs: the block entity renderers' draw code into a GlFrame, near frames (devices of them); farNs, farFrames:
     * the same for far frames; submitNs: handing the batches to the game; writeNs, writeVertices, batches: the vertex
     * write loop when the game calls it; refreshNs, refreshed: clientRefresh of every loaded device; snapshotNs: the
     * render thread's part of a section compile with baked parts; settles, drops: calls that joined and left the
     * chunk; meshNs, meshVertices, meshSections: writing baked parts into section meshes (chunk workers).
     */
    public static final String[] NAMES = {"recordNs", "farNs", "submitNs", "writeNs", "refreshNs", "snapshotNs", "devices", "farFrames",
        "batches", "writeVertices", "refreshed", "settles", "drops", "meshNs", "meshVertices", "meshSections"};
    /** The counters that are render-thread time of a frame: their sum is EA's share of it. */
    public static final int[] FRAME_NS = {RECORD_NS, FAR_NS, SUBMIT_NS, WRITE_NS, REFRESH_NS, SNAPSHOT_NS};

    private static final long[] C = new long[N];
    private static final LongAdder meshNs = new LongAdder(), meshVertices = new LongAdder(), meshSections = new LongAdder();

    /** The time to hand back to the hook's end, or 0 while nothing is measured (then the end is not called). */
    public static long start() {
        return on ? System.nanoTime() : 0;
    }

    /** A block entity renderer drew a device into its frame (start from {@link #start}, not 0). */
    public static void device(long start, boolean far) {
        long d = System.nanoTime() - start;
        if (far) {
            C[FAR_NS] += d;
            C[FAR_FRAMES]++;
        } else {
            C[RECORD_NS] += d;
            C[DEVICES]++;
        }
    }

    /** A frame's batches were handed to the game. */
    public static void submitted(long start) {
        C[SUBMIT_NS] += System.nanoTime() - start;
    }

    /** The game had one batch written into its buffer. */
    public static void wrote(long start, int vertices) {
        C[WRITE_NS] += System.nanoTime() - start;
        C[WRITE_VERTICES] += vertices;
        C[BATCHES]++;
    }

    /** The frame's clientRefresh round over this many devices. */
    public static void refreshed(long start, int devices) {
        C[REFRESH_NS] += System.nanoTime() - start;
        C[REFRESHED] += devices;
    }

    /** The render thread prepared a section compile that has baked parts. */
    public static void snapshot(long start) {
        C[SNAPSHOT_NS] += System.nanoTime() - start;
    }

    /** One more of a plain counter ({@link #SETTLES}, {@link #DROPS}); render thread. */
    public static void count(int counter) {
        C[counter]++;
    }

    /** A chunk worker wrote baked parts into a section's mesh (any thread). */
    public static void meshed(long start, int vertices) {
        meshNs.add(System.nanoTime() - start);
        meshVertices.add(vertices);
        meshSections.increment();
    }

    /** Add what was counted since the last call to into[0..N) and start again from zero (render thread). */
    public static void take(long[] into) {
        for (int i = 0; i < MESH_NS; i++) {
            into[i] += C[i];
            C[i] = 0;
        }
        into[MESH_NS] += meshNs.sumThenReset();
        into[MESH_VERTICES] += meshVertices.sumThenReset();
        into[MESH_SECTIONS] += meshSections.sumThenReset();
    }
}
