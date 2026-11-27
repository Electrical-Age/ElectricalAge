package mods.eln.transparentnode.groundradar;

/**
 * What the goggles' view shows, without Minecraft (Baughn 1575645294446518445): one of the named bands, which a key
 * cycles through, or a custom band-pass on density, which the four edge keys make from any of them. Water and oil show
 * by their flags (oil at 0.85 g/cm3 sits right by water's 1.0, so no density band separates them). Everything shows
 * caves, liquids, structures and ore veins, over the ground drawn faintly. The radar keeps the band (and its item does).
 */
public record RadarBand(int kind, double lo, double hi) {
    public static final int EVERYTHING = 0, CAVES = 1, WATER = 2, OIL = 3, SOIL = 4, ROCK = 5, ORES = 6, CUSTOM = 7;
    /** The band edges' step per key press (g/cm3), and their range. */
    public static final double STEP = 0.1, MIN = -0.5, MAX = GroundScan.MAX_DENSITY + 0.5;
    /** The named bands' edges (for the readout, and where the edge keys start a custom band from). */
    static final double[] LO = {MIN, -0.3, 0.7, 0.7, 1.3, 2.2, 4.0}, HI = {MAX, 0.6, 1.3, 1.3, 2.2, 3.6, GroundScan.MAX_DENSITY};
    /** The named bands shown by a flag instead of density (0: by density). */
    static final int[] FLAG = {0, 0, GroundScan.LIQUID, GroundScan.OIL, 0, 0, 0};
    /** In Everything, the weight of plain ground (soil, rock), so the features stand out in it. */
    public static final double FAINT = 0.05;
    /** In Everything, the densest sample from which a cell's peak counts as ore. */
    public static final double ORE = 4.0;
    /** In Everything, how much stronger an ore vein shows than in a density band (through faint ground it was a pale
     *  speck, AdventurAgent 1575661910995763301). */
    public static final double ORE_GAIN = 2;

    public static final RadarBand DEFAULT = of(EVERYTHING);

    /** A named band. */
    public static RadarBand of(int kind) {
        int k = Math.max(0, Math.min(ORES, kind));
        return new RadarBand(k, LO[k], HI[k]);
    }

    /** The band as saved or sent: a named one by its kind alone, a custom one by its edges, kept in order and range. */
    public static RadarBand read(int kind, double lo, double hi) {
        if (kind < EVERYTHING || kind > CUSTOM) return DEFAULT;
        if (kind != CUSTOM) return of(kind);
        if (Double.isNaN(lo) || Double.isNaN(hi)) return DEFAULT;
        lo = Math.round(lo * 10) / 10.0;   // (edges move in tenths; sent and saved as floats)
        hi = Math.round(hi * 10) / 10.0;
        if (!(lo >= MIN) || !(hi <= MAX) || !(hi - lo >= STEP - 1e-9)) return DEFAULT;
        return new RadarBand(CUSTOM, lo, hi);
    }

    /** The next named band (from a custom one: Everything). */
    public RadarBand next() {
        return of(kind >= ORES ? EVERYTHING : kind + 1);
    }

    /** The edges moved by dLo and dHi (whole steps), as a custom band; they stay in range and a step apart. */
    public RadarBand edges(int dLo, int dHi) {
        double l = lo, h = hi;
        if (dLo != 0) l = Math.max(MIN, Math.min(h - STEP, l + dLo * STEP));
        if (dHi != 0) h = Math.min(MAX, Math.max(l + STEP, h + dHi * STEP));
        return new RadarBand(CUSTOM, Math.round(l * 10) / 10.0, Math.round(h * 10) / 10.0);
    }

    /** The flag a cell needs to show (0: it shows by density). */
    public int flag() {
        return kind < FLAG.length ? FLAG[kind] : 0;
    }

    /**
     * How much a cell shows, 0..1: by its flags f, its mean density, and its densest sample peak, which counts by
     * peakShare (the weight RadarView gives a cell's densest samples for the share of the cell they are).
     */
    public double weight(int f, double mean, double peak, double peakShare) {
        if (flag() != 0) return (f & flag()) != 0 ? 1 : 0;
        if (kind == EVERYTHING) {
            if ((f & (GroundScan.LIQUID | GroundScan.OIL | GroundScan.LAVA | GroundScan.BUILT)) != 0 || mean < 0.5) return 1;
            return Math.max(FAINT, peak >= ORE ? Math.min(1, ORE_GAIN * peakShare) : 0);
        }
        return Math.max(RadarView.band(mean, lo, hi), RadarView.band(peak, lo, hi) * peakShare);
    }

    /** True if a cell's densest sample, not its mean, is what shows (it is drawn in the peak's colour). */
    public boolean byPeak(int f, double mean, double peak, double peakShare) {
        if (flag() != 0) return false;
        if (kind == EVERYTHING)
            return (f & (GroundScan.LIQUID | GroundScan.OIL | GroundScan.LAVA | GroundScan.BUILT)) == 0 && mean >= 0.5 && peak >= ORE && peakShare > FAINT;
        return RadarView.band(peak, lo, hi) * peakShare > RadarView.band(mean, lo, hi);
    }
}
