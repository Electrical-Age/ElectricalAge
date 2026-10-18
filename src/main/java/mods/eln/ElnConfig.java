package mods.eln;

import net.neoforged.neoforge.common.ModConfigSpec;

/** EA's server-side balancing knobs (config/eln-synced.toml). The first: battery lifespan (Baughn 2026-10-17). */
public final class ElnConfig {
    private ElnConfig() {
    }

    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.DoubleValue BATTERY_LIFE_FACTOR;
    static final ModConfigSpec.BooleanValue PROBE_DETAILS;
    static final ModConfigSpec.BooleanValue CONVERT_CABLES;
    static final ModConfigSpec.DoubleValue COAL_LAG, OIL_LAG, MIN_FIRE, COAL_KINDLE, OIL_LIGHT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("balancing");
        BATTERY_LIFE_FACTOR = b.comment("Battery lifespan multiplier: wear is divided by it (1 = 1.12's wear; Baughn: 25). Wear also",
                "doubles every 10 C above ambient and rises up to 4x as the charge runs low.")
            .defineInRange("batteryLifeFactor", 25.0, 0.01, 10000.0);
        b.pop();
        b.push("fire");
        COAL_LAG = b.comment("Seconds a coal fire (the stone heat furnace) takes to follow its setting: a first-order lag (Baughn: coal",
                "plants change burn rate slowly).").defineInRange("coalLagSeconds", 180.0, 0.0, 3600.0);
        OIL_LAG = b.comment("The same for an oil burner (the fuel heat furnace).").defineInRange("oilLagSeconds", 20.0, 0.0, 3600.0);
        MIN_FIRE = b.comment("Below this share of full power a fire asked to burn lower goes out.").defineInRange("minFire", 0.15, 0.0, 1.0);
        COAL_KINDLE = b.comment("Seconds to relight a coal fire that went out.").defineInRange("coalKindleSeconds", 30.0, 0.0, 3600.0);
        OIL_LIGHT = b.comment("Seconds to light an oil burner.").defineInRange("oilLightSeconds", 3.0, 0.0, 3600.0);
        b.pop();
        b.push("cables");
        CONVERT_CABLES = b.comment("Convert six-node cables to voxel cables as their chunks load (blocks holding only cables; the",
                "six-node block is kept inside for /elnlab voxelcables revert). Off until the conversion is approved.")
            .define("convertOnLoad", false);
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
        mods.eln.node.voxel.CableConverter.convertOnLoad = CONVERT_CABLES.get();
        mods.eln.sim.Fire.COAL.lag = COAL_LAG.get();
        mods.eln.sim.Fire.COAL.kindle = COAL_KINDLE.get();
        mods.eln.sim.Fire.OIL.lag = OIL_LAG.get();
        mods.eln.sim.Fire.OIL.kindle = OIL_LIGHT.get();
        mods.eln.sim.Fire.minFire = MIN_FIRE.get();
    }
}
