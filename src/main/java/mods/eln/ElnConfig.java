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
    static final ModConfigSpec.DoubleValue DEPTH_C, DEPTH_PER_BLOCK, NETHER_C, HEAT_FURNACE_FUEL;
    static final ModConfigSpec.DoubleValue BIKE_POWERED, BIKE_PEDALLING, BIKE_WINDOW, BIKE_MOTOR, BIKE_TIP_DELAY, BIKE_TIP_CHANCE,
        BIKE_CRASH, BIKE_JUMP, BIKE_STEP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("balancing");
        BATTERY_LIFE_FACTOR = b.comment("Battery lifespan multiplier: wear is divided by it (1 = 1.12's wear; Baughn: 25). Wear also",
                "doubles every 10 C above 30 C and rises up to 4x as the charge runs low.")
            .defineInRange("batteryLifeFactor", 25.0, 0.01, 10000.0);
        HEAT_FURNACE_FUEL = b.comment("Heat a fuel gives in the heat furnaces (stone and fuel), times its 1.12 value. Baughn 2026-10-26: with",
                "steam turbines working by the expansion, coal to electricity straddles the old 33%: ~24% venting, ~44% with a condenser.")
            .defineInRange("heatFurnaceFuelFactor", 2.5, 0.01, 100.0);
        b.pop();
        b.push("fire");
        COAL_LAG = b.comment("Seconds a coal fire (the stone heat furnace) takes to follow its setting: a first-order lag (Baughn: coal",
                "plants change burn rate slowly).").defineInRange("coalLagSeconds", 180.0, 0.0, 3600.0);
        OIL_LAG = b.comment("The same for an oil burner (the fuel heat furnace).").defineInRange("oilLagSeconds", 20.0, 0.0, 3600.0);
        MIN_FIRE = b.comment("Below this share of full power a fire asked to burn lower goes out.").defineInRange("minFire", 0.15, 0.0, 1.0);
        COAL_KINDLE = b.comment("Seconds to relight a coal fire that went out.").defineInRange("coalKindleSeconds", 30.0, 0.0, 3600.0);
        OIL_LIGHT = b.comment("Seconds to light an oil burner.").defineInRange("oilLightSeconds", 3.0, 0.0, 3600.0);
        b.pop();
        b.push("ambient");
        DEPTH_C = b.comment("Ambient temperature (C) deep underground, before the gradient: surface temperatures (25 C per unit of",
                "the biome's temperature) blend into it from 4 to 32 blocks under the surface (Baughn 2026-10-26).")
            .defineInRange("depthC", 15.0, -50.0, 200.0);
        DEPTH_PER_BLOCK = b.comment("Geothermal gradient: C warmer per block of depth under the surface.")
            .defineInRange("depthPerBlock", 0.1, 0.0, 10.0);
        NETHER_C = b.comment("Ambient temperature (C) where water evaporates (the Nether).").defineInRange("netherC", 90.0, -50.0, 1000.0);
        b.pop();
        b.push("cables");
        CONVERT_CABLES = b.comment("Convert six-node cables to voxel cables as their chunks load (blocks holding only cables; the",
                "six-node block is kept inside for /elnlab voxelcables revert). Off until the conversion is approved.")
            .define("convertOnLoad", false);
        b.pop();
        // the electric bike (Baughn 2026-10-28: "might adjust values in playtesting"): synced, as the rider's client moves it
        b.push("bike");
        BIKE_POWERED = b.comment("The electric bike's top speed on battery, blocks per second (an average horse: 9.5).")
            .defineInRange("poweredSpeed", 9.5, 0.5, 40.0);
        BIKE_PEDALLING = b.comment("Its top speed without charge, pedalling, blocks per second (sprinting is 5.6).")
            .defineInRange("pedallingSpeed", 6.6, 0.5, 40.0);
        BIKE_WINDOW = b.comment("Seconds of the rider's input averaged into its speed (Baughn: 2-3, so it feels like a bicycle).")
            .defineInRange("inputAverageSeconds", 2.5, 0.05, 10.0);
        BIKE_MOTOR = b.comment("Its motor's draw at full speed, W (falls with the square of speed; capped by the battery's discharge power).")
            .defineInRange("motorWatts", 100.0, 0.0, 10000.0);
        BIKE_TIP_DELAY = b.comment("Seconds it stays up when stopped with a rider, before it may fall over.")
            .defineInRange("tipOverDelaySeconds", 5.0, 0.0, 3600.0);
        BIKE_TIP_CHANCE = b.comment("Chance per second, after that, that it falls over with its rider (0 never).")
            .defineInRange("tipOverChancePerSecond", 0.1, 0.0, 1.0);
        BIKE_CRASH = b.comment("Running into a full block or a wall faster than this (blocks per second) throws the rider off.")
            .defineInRange("crashSpeed", 3.0, 0.0, 100.0);
        BIKE_JUMP = b.comment("Height of a full-charge jump, blocks (1.31: one block with room, never 1.5).")
            .defineInRange("jumpHeight", 1.31, 0.0, 5.0);
        BIKE_STEP = b.comment("Height it rides up without jumping, blocks (0.6: slabs and stairs; 1 or more climbs full blocks).")
            .defineInRange("stepHeight", 0.6, 0.0, 2.0);
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
    public static volatile double heatFurnaceFuelFactor = 2.5;

    static void refresh() {
        batteryLifeFactor = BATTERY_LIFE_FACTOR.get();
        heatFurnaceFuelFactor = HEAT_FURNACE_FUEL.get();
        Eln.wailaEasyMode = PROBE_DETAILS.get();
        mods.eln.node.voxel.CableConverter.convertOnLoad = CONVERT_CABLES.get();
        mods.eln.sim.Fire.COAL.lag = COAL_LAG.get();
        mods.eln.sim.Fire.COAL.kindle = COAL_KINDLE.get();
        mods.eln.sim.Fire.OIL.lag = OIL_LAG.get();
        mods.eln.sim.Fire.OIL.kindle = OIL_LIGHT.get();
        mods.eln.sim.Fire.minFire = MIN_FIRE.get();
        mods.eln.sim.Ambient.depthC = DEPTH_C.get();
        mods.eln.sim.Ambient.depthPerBlock = DEPTH_PER_BLOCK.get();
        mods.eln.sim.Ambient.netherC = NETHER_C.get();
        mods.eln.bike.BikePhysics.VMAX_POWERED = BIKE_POWERED.get() / 20;
        mods.eln.bike.BikePhysics.VMAX_PEDALLING = BIKE_PEDALLING.get() / 20;
        mods.eln.bike.BikePhysics.AVERAGE_TICKS = Math.max(1, (int) Math.round(BIKE_WINDOW.get() * 20));
        mods.eln.bike.BikePhysics.MOTOR_WATTS = BIKE_MOTOR.get();
        mods.eln.bike.ElectricBike.STILL_TICKS = (int) Math.round(BIKE_TIP_DELAY.get() * 20);
        mods.eln.bike.ElectricBike.TIP_CHANCE = (float) (double) BIKE_TIP_CHANCE.get();
        mods.eln.bike.BikePhysics.CRASH_SPEED = BIKE_CRASH.get() / 20;
        mods.eln.bike.BikePhysics.JUMP_VY = mods.eln.bike.BikePhysics.velocityForPeak(BIKE_JUMP.get());
        mods.eln.bike.BikePhysics.STEP_HEIGHT = (float) (double) BIKE_STEP.get();
    }
}
