package mods.eln.sim;

import net.minecraft.nbt.CompoundTag;

/**
 * A fire's real burn rate. Its burn level scales the heat it gives; a fire asked to burn below minFire goes out once it
 * gets there, and a fire that's out takes `kindle` seconds to light (then starts at minFire).
 * <p>
 * An oil flame (no air valve in its kind) follows the setting with a first-order lag. A coal fire (Baughn 1573753560456499223:
 * real furnaces have air valves) has two states: its <b>air</b>, the damper the setting drives, which follows within
 * {@link Kind#air} seconds either way, and its <b>bed</b>, how much of the fuel bed is hot, which grows towards the air
 * over {@link Kind#lag} seconds (coal plants change burn rate slowly: a fire spreads through a cold bed slowly) and,
 * with the air cut, cools towards it over {@link Kind#bank} seconds (banked embers stay hot). It burns at the lesser of
 * the two, plus {@link #LEAK} of its bed past a shut damper: cutting the air cuts the heat in seconds, opening it on a
 * hot bed gives full heat in seconds, on a cold one only what the bed can burn.
 */
public final class Fire {
    /** Kind of fire: its lag and lighting time (seconds, from ElnConfig), and for a coal bed its air valve and banking. */
    public static final class Kind {
        public volatile double lag, kindle;
        /** Seconds the air valve takes to follow its setting (0: no valve, the level follows the setting with lag). */
        public final double air;
        /** Seconds a hot bed takes to cool towards a cut air setting. */
        public final double bank;

        Kind(double lag, double kindle, double air, double bank) {
            this.lag = lag;
            this.kindle = kindle;
            this.air = air;
            this.bank = bank;
        }
    }

    /** Coal: 5 s for the air (Baughn: shutting the air is like 5 s on a fireplace), 180 s to spread, 300 s to cool. */
    public static final Kind COAL = new Kind(180, 30, 5, 300), OIL = new Kind(20, 3, 0, 0);
    public static volatile double minFire = 0.15;
    /** The share of its bed a fire burns past a shut damper (it smoulders, so a banked bed stays lit). */
    public static final double LEAK = 0.03;
    /** A coal bed with its air shut goes out only once it has cooled to embers this small (a regulator holding a low
     *  heat keeps it alight, rather than letting it go out and wait to be lit again). */
    public static final double EMBERS = 0.05;

    final Kind kind;
    /** No lag once lit: the flame follows the setting at once (the fuel heat furnace's inertia is its burner's mass). */
    public boolean instant;
    /** Lighting time of a fire still hot from its last flame (seconds). */
    public static final double HOT_KINDLE = 0.25;
    /** Still hot from its last flame (its owner says so before a step): lit again, it catches in HOT_KINDLE seconds. */
    public boolean hot;
    /** The real burn level (0 = out .. 1). */
    public double level;
    /** A coal fire's air valve and hot bed (0..1; see the class comment). */
    public double air, bed;
    double lighting;

    public Fire(Kind kind) {
        this.kind = kind;
    }

    /** Burning at this level, bed and air alike (tests, and saves from before the air valve). */
    public void set(double level) {
        this.level = air = bed = level;
    }

    /** One step towards the setting (0..1) over time seconds; fuel: is there anything to burn. */
    public void step(double setting, boolean fuel, double time) {
        step(setting, 1, fuel, time);
    }

    /**
     * ... with no more of its bed alight than bedMax (a coal bed short of fuel: Baughn, with nothing to burn it banks
     * until it's out, rather than flaring to full power on embers when fuel comes back; the air stays the setting's).
     */
    public void step(double setting, double bedMax, boolean fuel, double time) {
        double min = minFire;
        if (!fuel) {
            set(0);
            lighting = 0;
            return;
        }
        if (level <= 0) {
            if (Math.min(setting, bedMax) >= min && setting > 0) {
                lighting += time;
                if (lighting >= (hot ? Math.min(HOT_KINDLE, kind.kindle) : kind.kindle)) {
                    set(Math.max(min, 1e-3));
                    lighting = 0;
                }
            } else lighting = 0;
            return;
        }
        if (kind.air <= 0 || instant) {
            setting = Math.min(setting, bedMax);
            level += (setting - level) * (instant || kind.lag <= 0 ? 1 : Math.min(1, time / kind.lag));
            air = bed = level;
            if (setting < min && level <= min) set(0);   // (asked lower than it can burn: it goes out)
            return;
        }
        air += (setting - air) * Math.min(1, time / kind.air);
        double toward = Math.min(air, bedMax);
        bed += (toward - bed) * Math.min(1, time / (toward > bed ? kind.lag : kind.bank));
        level = Math.max(Math.min(air, bed), LEAK * bed);
        // (the air shut, or nothing left to burn, and the bed down to embers: it goes out)
        if (bed <= EMBERS && (setting < min && air <= min || bedMax < min)) set(0);
    }

    /** Is it being lit (setting up, not burning yet)? */
    public boolean lighting() {
        return level <= 0 && lighting > 0;
    }

    public void write(CompoundTag t, String key) {
        t.putDouble(key + "fire", level);
        t.putDouble(key + "fireLighting", lighting);
        t.putDouble(key + "fireAir", air);
        t.putDouble(key + "fireBed", bed);
    }

    public void read(CompoundTag t, String key) {
        level = t.getDoubleOr(key + "fire", 0);
        lighting = t.getDoubleOr(key + "fireLighting", 0);
        // (a save from before the air valve: burning at its level, bed and air alike)
        air = t.getDoubleOr(key + "fireAir", level);
        bed = t.getDoubleOr(key + "fireBed", level);
    }
}
