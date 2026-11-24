package mods.eln.transparentnode.groundradar;

/**
 * The Ground Radar's picture of what is under it, without Minecraft: a cone of cells from just under the device down to
 * the world's floor, {@link #N} cells across at every depth, each cell a cube whose side grows with its depth (2 blocks
 * at the top; Baughn: no hard depth limit, the picture coarsens instead, and a 50-wide feature still shows, barely, at
 * bedrock). A cell holds its blocks' mean density, read from up to 27 stratified samples (a deep cell is far bigger), and
 * what most of it is (liquid, oil, lava, a structure's, unknown).
 * <p>
 * The radar's signal fades on the way down, faster through wet ground (water, clay, a swamp's mud) than dry rock: a
 * cell's signal is its parent's (the cell above its centre) times exp(-attenuation x thickness) of the parent. The picture
 * mixes grey static over a cell as its signal fades, so under the sea it turns to static sooner, as real ground-penetrating
 * radar does. The static comes from a hash of the position: the same ground scans the same.
 * <p>
 * The work is done a few samples at a time ({@link #step}): the device spreads a scan over its seconds.
 */
public final class GroundScan {
    /** Cells across at the default range (Settings). */
    public static final int N = 24;
    /** The top cells' side (blocks), and how fast it grows by default: side = TOP_CELL + depth / growth, rounded. */
    public static final int TOP_CELL = 2, GROWTH = 8;
    /** The most cells across a scan may have. */
    public static final int MAX_N = 96;
    /** The most layers a picture may have (a 4064-high world at the finest detail has ~140). */
    public static final int MAX_LAYERS = 256;
    /** Samples a cell reads at most (3 x 3 x 3 strata; a cell of 3 or fewer blocks a side is read whole). */
    public static final int MAX_SAMPLES = 27;
    /** Densities are kept as bytes over 0..MAX_DENSITY g/cm3 (8.5: ores apart, galena and uraninite near the top; pictures
     *  from before were over 0..4.5, LEGACY_MAX, and read back so). */
    public static final double MAX_DENSITY = 8.5, LEGACY_MAX = 4.5;

    /** Cell flags: most of it liquid (water), an eighth of it oil, most of it lava; an eighth of it in a structure; most of it unknown (unloaded). */
    public static final int LIQUID = 1, OIL = 2, LAVA = 4, BUILT = 8, UNKNOWN = 16, SKY = 32;

    /** What a block is to the radar: its density (g/cm3), its attenuation (per block) and its kind. */
    public record Material(double density, double attenuation, int kind) {
        public static final int SOLID = 0, AIR = 1, WATER = 2, OIL = 3, LAVA = 4, SKY = 5;
        public static final Material AIR_ = new Material(0, 0, AIR);
        /** Air open to the sky (over the ground's surface): not a cave. */
        public static final Material SKY_ = new Material(0, 0, SKY);
    }

    /** The world as the scan reads it. material: null where it can't be read (an unloaded chunk). */
    public interface World {
        Material material(int x, int y, int z);

        /** Whether the block is inside a structure's piece. */
        boolean built(int x, int y, int z);
    }

    /** One layer of cells: its top (blocks below the device's floor, 0 = the block under it) and its cells' side. */
    public record Layer(int depth, int cell) {
    }

    /** The world x (or z) of cell i's low edge in layer l, centred on c. */
    public int edge(Layer l, int c, int i) {
        return c - n * l.cell() / 2 + i * l.cell();
    }

    public final int cx, cy, cz;
    /** The densities' scale (g/cm3 at byte 255). */
    private double max = MAX_DENSITY;
    /** Cells across at every depth, and the growth of the cells' side with depth (side = TOP_CELL + depth / growth). */
    public final int n, growth;
    public final Layer[] layers;
    /** Per layer, N x N cells (index (k * N + i) * N + j, i along x, j along z): density bytes and flags. */
    public final byte[] density, flags, signal;
    /** Per cell: its densest sample (the goggles' band can find an ore a cell's mean hides; legacy pictures: the mean). */
    public final byte[] peak;
    /** Per cell: the share of its samples at (within 0.3 g/cm3 of) its peak: one ore block in a big cell is a small share. */
    public final byte[] peakShare;
    private final double[] sampled = new double[MAX_SAMPLES];

    private final World world;
    /** The world's floor (the lowest y read; the last layer is cut off there). */
    private final int minY;
    private final float[] strength;
    /** Every cell at full signal (slab). */
    private final boolean flat;
    private int next;

    /**
     * A scan of the ground under (cx, cy, cz) (the device's block; the first layer is the one under it) down to minY,
     * the world's floor (inclusive).
     */
    public GroundScan(World world, int cx, int cy, int cz, int minY) {
        this(world, cx, cy, cz, minY, N, GROWTH);
    }

    /** As above, n cells across, cells growing by one block every growth blocks down. */
    public GroundScan(World world, int cx, int cy, int cz, int minY, int n, int growth) {
        this(world, cx, cy, cz, minY, n, growth, layers(cy - 1 - minY + 1, growth), false);
    }

    private GroundScan(World world, int cx, int cy, int cz, int minY, int n, int growth, Layer[] layers, boolean flat) {
        this.world = world;
        this.n = n;
        this.growth = growth;
        this.minY = minY;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.layers = layers;
        this.flat = flat;
        density = new byte[layers.length * n * n];
        flags = new byte[layers.length * n * n];
        signal = new byte[layers.length * n * n];
        peak = new byte[layers.length * n * n];
        peakShare = new byte[layers.length * n * n];
        strength = new float[layers.length * n * n];
    }

    /**
     * A few chosen layers under (cx, cy, cz) at full signal (no fading: the auto-miner's own view around its drill head,
     * where it already is), n cells across; minY the world's floor.
     */
    public static GroundScan slab(World world, int cx, int cy, int cz, int minY, int n, Layer... layers) {
        return new GroundScan(world, cx, cy, cz, minY, n, 1, layers, true);
    }

    /**
     * The cone under (cx, cy, cz) down to minY at full signal, but only its two cuts through the centre, along x and along
     * z (cells (k, i, n/2) and (k, n/2, j)); the rest is left unknown (the auto-miner's side views: a few hundred cells).
     */
    public static GroundScan cross(World world, int cx, int cy, int cz, int minY, int n, int growth) {
        GroundScan s = new GroundScan(world, cx, cy, cz, minY, n, growth, layers(cy - 1 - minY + 1, growth), true);
        s.crossOnly = true;
        return s;
    }

    private boolean crossOnly;

    /** A finished scan read back (the client's copy). */
    public GroundScan(int cx, int cy, int cz, int n, int growth, Layer[] layers, byte[] density, byte[] flags, byte[] signal, byte[] peak, byte[] peakShare) {
        this.world = null;
        this.n = n;
        this.growth = growth;
        this.minY = Integer.MIN_VALUE;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.layers = layers;
        this.density = density;
        this.flags = flags;
        this.signal = signal;
        this.peak = peak;
        this.peakShare = peakShare;
        this.strength = null;
        this.flat = false;
        next = layers.length * n * n;
    }

    /** The layers that fill depth blocks under the device: each as thick as its cells are wide; the last one cut short. */
    public static Layer[] layers(int depth, int growth) {
        java.util.List<Layer> l = new java.util.ArrayList<>();
        for (int d = 0; d < depth; ) {
            int c = cellAt(d, growth);
            l.add(new Layer(d, c));
            d += c;
        }
        return l.toArray(new Layer[0]);
    }

    /** A cell's side at depth d (blocks under the device). */
    public static int cellAt(int d, int growth) {
        return (int) Math.round(TOP_CELL + d / (double) growth);
    }

    public int cells() {
        return layers.length * n * n;
    }

    public boolean done() {
        return next >= cells();
    }

    /** The fraction of cells read. */
    public double progress() {
        return next / (double) Math.max(1, cells());
    }

    /** Read at most maxCells cells, stopping once maxSamples samples are spent; returns the cells read. */
    public int stepCells(int maxCells, int maxSamples) {
        int c = 0;
        while (!done() && c < maxCells && maxSamples > 0) {
            maxSamples -= cell(next++);
            c++;
        }
        return c;
    }

    /** Read cells, top layer first, until about budget samples are spent or the scan is done; true if it is done. */
    public boolean step(int budget) {
        while (!done() && budget > 0) budget -= cell(next++);
        return done();
    }

    /** Read one cell; returns the samples it took. */
    private int cell(int index) {
        int k = index / (n * n), i = index / n % n, j = index % n;
        if (crossOnly && i != n / 2 && j != n / 2) {
            flags[index] = (byte) UNKNOWN;
            return 0;
        }
        Layer layer = layers[k];
        int side = layer.cell;
        int top = cy - 1 - layer.depth;
        int x0 = edge(layer, cx, i), z0 = edge(layer, cz, j);
        int per = side <= 3 ? side : 3;   // samples per axis
        // (the last layer is cut off at the world's floor: only what is above it is read; under it is nothing)
        int tall = Math.max(1, Math.min(side, top - minY + 1)), perY = Math.min(per, tall);
        int samples = 0, known = 0, liquid = 0, oil = 0, lava = 0, built = 0, sky = 0;
        double sum = 0, att = 0, most = 0;
        for (int a = 0; a < per; a++)
            for (int b = 0; b < perY; b++)
                for (int c = 0; c < per; c++) {
                    int x, y, z;
                    if (per == side && perY == tall) {
                        x = x0 + a;
                        y = top - b;
                        z = z0 + c;
                    } else {
                        int h = hash(x0 + a * 7919, top - b * 104729, z0 + c * 1299709);
                        x = x0 + stratum3(a, per, side, h);
                        y = perY == tall ? top - b : top - stratum3(b, perY, tall, h >>> 10);
                        z = z0 + stratum3(c, per, side, h >>> 20);
                    }
                    samples++;
                    Material m = world.material(x, y, z);
                    if (m == null) continue;
                    known++;
                    sum += m.density();
                    most = Math.max(most, m.density());
                    if (known <= MAX_SAMPLES) sampled[known - 1] = m.density();
                    att += m.attenuation();
                    switch (m.kind()) {
                        case Material.WATER -> liquid++;
                        case Material.OIL -> oil++;
                        case Material.LAVA -> lava++;
                        case Material.SKY -> sky++;
                        default -> {
                        }
                    }
                    if (world.built(x, y, z)) built++;
                }
        int f = 0;
        if (known * 2 < samples) f |= UNKNOWN;
        if (liquid * 2 > known && known > 0) f |= LIQUID;
        if (oil * 8 >= known && oil > 0) f |= OIL;   // (an eighth: oil is what a prospector is after)
        if (lava * 2 > known && known > 0) f |= LAVA;
        if (sky * 2 > known && known > 0) f |= SKY;
        if (built * 8 >= known && built > 0) f |= BUILT;
        // (the signal reaching this cell: its parent's, faded through the parent's thickness)
        double s = 1;
        if (k > 0 && !flat) {
            int pi = parent(k, i, cx), pj = parent(k, j, cz);
            int p = ((k - 1) * n + pi) * n + pj;
            s = strength[p];
        }
        double meanAtt = known > 0 ? att / known : 0.02;
        strength[index] = (float) (s * Math.exp(-meanAtt * side));
        double mean = known > 0 ? sum / known : 0;
        // grain: some at every depth, more as the signal fades; unknown cells are all grain
        density[index] = (byte) Math.max(0, Math.min(255, Math.round(mean / max * 255)));
        flags[index] = (byte) f;
        peak[index] = (byte) Math.max(0, Math.min(255, Math.round(most / max * 255)));
        int near = 0;
        for (int q = 0; q < Math.min(known, MAX_SAMPLES); q++) if (sampled[q] >= most - 0.3) near++;
        peakShare[index] = (byte) (known == 0 ? 0 : Math.round(255.0 * near / Math.min(known, MAX_SAMPLES)));
        signal[index] = (byte) ((f & UNKNOWN) != 0 ? 0 : Math.round(Math.max(0, Math.min(1, s)) * 255));
        return samples;
    }

    /** The index, along one axis, of the cell in layer k - 1 over the centre of cell i of layer k (clamped). */
    private int parent(int k, int i, int c) {
        Layer l = layers[k], up = layers[k - 1];
        int centre = edge(l, c, i) + l.cell / 2;
        int pi = Math.floorDiv(centre - edge(up, c, 0), up.cell);
        return Math.max(0, Math.min(n - 1, pi));
    }

    /** A jittered offset inside stratum a of n across a side. */
    private static int stratum3(int a, int n, int side, int h) {
        int lo = a * side / n, hi = (a + 1) * side / n;
        return lo + Math.floorMod(h, Math.max(1, hi - lo));
    }

    static int hash(int x, int y, int z) {
        int h = x * 0x27d4eb2d ^ y * 0x165667b1 ^ z * 0x9e3779b9;
        h ^= h >>> 15;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        return h ^ (h >>> 16);
    }

    /** Cell (k, i, j)'s density in g/cm3. */
    public double densityAt(int k, int i, int j) {
        return (density[(k * n + i) * n + j] & 0xFF) * max / 255;
    }

    /** Cell (k, i, j)'s signal, 0..1: how much of the radar's pulse reached it (0: none, or unknown ground). */
    /** The density bytes on the MAX_DENSITY scale (a picture from before, on LEGACY_MAX, rescaled): the bus sends these. */
    public byte[] densityBytes() {
        if (max == MAX_DENSITY) return density;
        byte[] b = new byte[density.length];
        for (int q = 0; q < b.length; q++) b[q] = (byte) Math.min(255, Math.round((density[q] & 0xFF) * max / MAX_DENSITY));
        return b;
    }

    /** Cell (k, i, j)'s densest sample (g/cm3). */
    public double peakAt(int k, int i, int j) {
        return (peak[(k * n + i) * n + j] & 0xFF) * max / 255;
    }

    /** The share of cell (k, i, j)'s samples at its peak, 0..1. */
    public double peakShareAt(int k, int i, int j) {
        return (peakShare[(k * n + i) * n + j] & 0xFF) / 255.0;
    }

    public double signalAt(int k, int i, int j) {
        return (signal[(k * n + i) * n + j] & 0xFF) / 255.0;
    }

    public int flagsAt(int k, int i, int j) {
        return flags[(k * n + i) * n + j] & 0xFF;
    }

    /** The layer holding depth d (blocks under the device), or -1. */
    public int layerAt(int d) {
        for (int k = 0; k < layers.length; k++) if (d < layers[k].depth + layers[k].cell) return d >= layers[k].depth ? k : -1;
        return -1;
    }

    // ---- wire form (to the client, and to computers)

    /** The picture as bytes (deflated: a wide, detailed scan is a few hundred thousand cells). */
    public byte[] encode() {
        var bos = new java.io.ByteArrayOutputStream(16 + layers.length * (4 + n * n));
        try (var o = new java.io.DataOutputStream(new java.util.zip.DeflaterOutputStream(bos))) {
            o.writeInt(cx);
            o.writeInt(cy);
            o.writeInt(cz);
            o.writeShort(-n);   // (negative: the densities' scale follows)
            o.writeShort((int) Math.round(max * 100));
            o.writeShort(growth);
            o.writeShort(layers.length);
            for (Layer l : layers) {
                o.writeShort(l.depth);
                o.writeShort(l.cell);
            }
            o.write(density);
            o.write(flags);
            o.write(signal);
            o.write(peak);
            o.write(peakShare);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return bos.toByteArray();
    }

    /** A scan from encode()'s bytes, or null if they don't make one. */
    public static GroundScan decode(byte[] b) {
        try (var in = new java.io.DataInputStream(new java.util.zip.InflaterInputStream(new java.io.ByteArrayInputStream(b)))) {
            int x = in.readInt(), y = in.readInt(), z = in.readInt();
            int n = in.readShort();
            double scale = LEGACY_MAX;
            boolean scaled = n < 0;
            if (scaled) {
                n = -n;
                scale = in.readShort() / 100.0;
                if (!(scale > 0)) return null;
            }
            int growth = in.readShort(), count = in.readShort();
            if (n <= 0 || n > MAX_N || growth <= 0 || count <= 0 || count > MAX_LAYERS) return null;
            Layer[] ls = new Layer[count];
            for (int k = 0; k < count; k++) {
                ls[k] = new Layer(in.readShort(), in.readShort());
                if (ls[k].depth() < 0 || ls[k].cell() <= 0) return null;
            }
            int cells = count * n * n;
            byte[] d = new byte[cells], f = new byte[cells], g = new byte[cells], pk = d, ps = new byte[cells];
            in.readFully(d);
            in.readFully(f);
            in.readFully(g);
            if (scaled) {   // (the scaled format carries the peaks too)
                pk = new byte[cells];
                in.readFully(pk);
                in.readFully(ps);
            }
            if (!scaled) java.util.Arrays.fill(ps, (byte) 255);   // (legacy: the peak is the mean, all of it)
            GroundScan s = new GroundScan(x, y, z, n, growth, ls, d, f, g, pk, ps);
            s.max = scale;
            return s;
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    // ---- the picture's palette (the screen, the lab's PNG)

    /** A cell's colour: voids dark, earth brown, rock grey, dense bright; water blue, oil amber, lava red; structures tinted violet; faded cells grey. */
    public static int colour(GroundScan p, int k, int i, int j) {
        int f = p.flagsAt(k, i, j);
        double d = p.densityAt(k, i, j);
        int rgb;
        if ((f & UNKNOWN) != 0) rgb = 0x505050;
        else if ((f & SKY) != 0) return 0xFF000000;   // (open air over the ground: nothing to see)
        else if ((f & LAVA) != 0) rgb = 0xE05018;
        else if ((f & OIL) != 0) rgb = 0xC88C1E;
        else if ((f & LIQUID) != 0) rgb = 0x2860C8;
        else rgb = ramp(d);
        if ((f & BUILT) != 0) rgb = mix(rgb, 0xA060D0, 0.5);
        // a faded cell turns flat grey (with a faint grain): weak signal reads as "can't tell", not as texture
        int h = hash(i * 31 + k * 977, j * 17 + k, k * 7919);
        int grey = 0x6A + (h & 0x0F) - 8;
        double fade = 1 - p.signalAt(k, i, j);
        if ((f & OIL) != 0) fade *= 0.5;   // (oil answers the pulse strongly: it stands out through faded ground)
        rgb = mix(rgb, grey << 16 | grey << 8 | grey, 0.85 * Math.pow(fade, 1.5));
        return 0xFF000000 | rgb;
    }

    private static final double[] STOPS = {0, 0.9, 1.7, 2.7, 3.4, 5.0, 8.5};
    private static final int[] COLOURS = {0x0A0A10, 0x463224, 0x7A5A3C, 0xA8A090, 0xE8E0C0, 0xF0C060, 0xFFFFF0};

    static int ramp(double d) {
        for (int s = 1; s < STOPS.length; s++)
            if (d <= STOPS[s]) return mix(COLOURS[s - 1], COLOURS[s], (d - STOPS[s - 1]) / (STOPS[s] - STOPS[s - 1]));
        return COLOURS[COLOURS.length - 1];
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | gr << 8 | bl;
    }
}
