package mods.eln;

import net.neoforged.neoforge.common.ModConfigSpec;

/** EA's server-side balancing knobs (config/eln-synced.toml). The first: battery lifespan (Baughn 2026-10-17). */
public final class ElnConfig {
    private ElnConfig() {
    }

    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.DoubleValue BATTERY_LIFE_FACTOR;
    static final ModConfigSpec.BooleanValue PROBE_DETAILS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("balancing");
        BATTERY_LIFE_FACTOR = b.comment("Battery lifespan multiplier: wear is divided by it (1 = 1.12's wear; Baughn: 25). Wear also",
                "doubles every 10 C above ambient and rises up to 4x as the charge runs low.")
            .defineInRange("batteryLifeFactor", 25.0, 0.01, 10000.0);
        b.pop();
        b.push("probe");
        PROBE_DETAILS = b.comment("Jade (WAILA) shows every device's full details, not only its headline (sneak to see them anyway).",
                "Also 1.12's wailaEasyMode: more detail on some machines.")
            .define("details", false);
        b.pop();
        SPEC = b.build();
    }

    /** Cached (the battery processes read it every second). */
    public static volatile double batteryLifeFactor = 25.0;

    static void refresh() {
        batteryLifeFactor = BATTERY_LIFE_FACTOR.get();
        Eln.wailaEasyMode = PROBE_DETAILS.get();
    }
}
