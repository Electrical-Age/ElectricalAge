package mods.eln.sim;

import net.minecraft.nbt.CompoundTag;

/**
 * A fire's real burn rate (Baughn: coal plants change burn rate slowly): it follows the player's or regulator's
 * setting with a first-order lag; a fire asked to burn below minFire goes out once it gets there, and a fire that's
 * out takes `kindle` seconds to light (then starts at minFire). Its burn level scales the heat it gives.
 */
public final class Fire {
    /** Kind of fire: its lag and lighting time (seconds), from ElnConfig. */
    public static final class Kind {
        public volatile double lag, kindle;

        Kind(double lag, double kindle) {
            this.lag = lag;
            this.kindle = kindle;
        }
    }

    public static final Kind COAL = new Kind(180, 30), OIL = new Kind(20, 3);
    public static volatile double minFire = 0.15;

    final Kind kind;
    /** The real burn level (0 = out .. 1). */
    public double level;
    double lighting;

    public Fire(Kind kind) {
        this.kind = kind;
    }

    /** One step towards the setting (0..1) over time seconds; fuel: is there anything to burn. */
    public void step(double setting, boolean fuel, double time) {
        double min = minFire;
        if (!fuel) {
            level = 0;
            lighting = 0;
            return;
        }
        if (level <= 0) {
            if (setting >= min && setting > 0) {
                lighting += time;
                if (lighting >= kind.kindle) {
                    level = Math.max(min, 1e-3);
                    lighting = 0;
                }
            } else lighting = 0;
            return;
        }
        level += (setting - level) * (kind.lag <= 0 ? 1 : Math.min(1, time / kind.lag));
        if (setting < min && level <= min) level = 0;   // (asked lower than it can burn: it goes out)
    }

    /** Is it being lit (setting up, not burning yet)? */
    public boolean lighting() {
        return level <= 0 && lighting > 0;
    }

    public void write(CompoundTag t, String key) {
        t.putDouble(key + "fire", level);
        t.putDouble(key + "fireLighting", lighting);
    }

    public void read(CompoundTag t, String key) {
        level = t.getDoubleOr(key + "fire", 0);
        lighting = t.getDoubleOr(key + "fireLighting", 0);
    }
}
