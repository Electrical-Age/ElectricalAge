package mods.eln;

import net.neoforged.neoforge.common.ModConfigSpec;

/** EA's server-side balancing knobs (config/eln-synced.toml). The first: battery lifespan (Baughn 2026-10-17). */
public final class ElnConfig {
    private ElnConfig() {
    }

    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.DoubleValue BATTERY_LIFE_FACTOR;
    static final ModConfigSpec.IntValue AUTOMINER_RANGE;
    static final ModConfigSpec.BooleanValue PROBE_DETAILS;
    static final ModConfigSpec.BooleanValue CONVERT_CABLES;
    static final ModConfigSpec.BooleanValue LINKS_BUILD;
    static final ModConfigSpec.BooleanValue INTROSPECT_REMOTE;
    static final ModConfigSpec.DoubleValue COAL_LAG, MIN_FIRE, COAL_KINDLE, OIL_LIGHT;
    static final ModConfigSpec.DoubleValue DEPTH_C, DEPTH_PER_BLOCK, NETHER_C, HEAT_FURNACE_FUEL;
    static final ModConfigSpec.DoubleValue BIKE_POWERED, BIKE_PEDALLING, BIKE_WINDOW, BIKE_MOTOR, BIKE_TIP_DELAY, BIKE_TIP_CHANCE,
        BIKE_CRASH, BIKE_JUMP, BIKE_STEP, BIKE_TURN, BIKE_TURN_TOP, BIKE_LAMP, TERRAIN_STEP, TERRAIN_JUMP, TERRAIN_MOTOR;
    static final ModConfigSpec.DoubleValue ANVIL_POWER, ANVIL_UNIT, ANVIL_NAME, ANVIL_RESUME, ANVIL_SAG, ANVIL_GAP, ANVIL_PLATEAU;
    static final ModConfigSpec.IntValue ANVIL_BURSTS;
    static final ModConfigSpec.DoubleValue BANK_FARADS, BANK_VOLTS, BANK_LEAK, POWER_CAP_LEAK;
    static final ModConfigSpec.DoubleValue LAKE_SIZE, FULL_AT, SEEP_SHARE, STEAM_SHARE, STEAM_PER_BUCKET;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("balancing");
        BATTERY_LIFE_FACTOR = b.comment("Battery lifespan multiplier: wear is divided by this value. Wear also doubles",
                "every 10 °C above 30 °C and rises up to 4x as charge runs low.")
            .defineInRange("batteryLifeFactor", 25.0, 0.01, 10000.0);
        HEAT_FURNACE_FUEL = b.comment("Fuel heat output multiplier in the Stone and Fuel Heat Furnaces.")
            .defineInRange("heatFurnaceFuelFactor", 2.5, 0.01, 100.0);
        AUTOMINER_RANGE = b.comment("Auto Miner ore search radius in blocks around its shaft once at depth.")
            .defineInRange("autominerRange", 24, 1, 64);
        b.pop();
        b.push("fire");
        COAL_LAG = b.comment("Seconds a coal fire in a Stone Heat Furnace takes to spread through its fuel bed towards the air setting.")
            .defineInRange("coalLagSeconds", 180.0, 0.0, 3600.0);
        MIN_FIRE = b.comment("Minimum fraction of full power for an oil burner flame; asking for less extinguishes it.",
                "A coal fire with its air shut instead smoulders down to embers.")
            .defineInRange("minFire", 0.15, 0.0, 1.0);
        COAL_KINDLE = b.comment("Seconds to relight a cold coal fire in a Stone Heat Furnace.").defineInRange("coalKindleSeconds", 30.0, 0.0, 3600.0);
        OIL_LIGHT = b.comment("Seconds to light an oil burner in a Fuel Heat Furnace.").defineInRange("oilLightSeconds", 3.0, 0.0, 3600.0);
        b.pop();
        b.push("ambient");
        DEPTH_C = b.comment("Base underground ambient temperature (°C) before geothermal gradient.",
                "Surface biome temperatures blend into this between 4 and 32 blocks below the surface.")
            .defineInRange("depthC", 15.0, -50.0, 200.0);
        DEPTH_PER_BLOCK = b.comment("Geothermal gradient: ambient temperature increase (°C) per block below the surface.")
            .defineInRange("depthPerBlock", 0.1, 0.0, 10.0);
        NETHER_C = b.comment("Ambient temperature (°C) in ultra-warm dimensions where water evaporates (e.g. the Nether).")
            .defineInRange("netherC", 90.0, -50.0, 1000.0);
        b.pop();
        b.push("cables");
        CONVERT_CABLES = b.comment("Convert legacy cable blocks to voxel cables as chunks load.")
            .define("convertOnLoad", false);
        LINKS_BUILD = b.comment("Build voxel cable electrical connections from stored joints instead of recomputing adjacency rules.")
            .define("linksBuild", true);
        b.pop();
        b.push("bike");
        BIKE_POWERED = b.comment("Electric Bike top speed under battery power, in blocks per second (an average horse is ~9.5).")
            .defineInRange("poweredSpeed", 9.5, 0.5, 40.0);
        BIKE_PEDALLING = b.comment("Electric Bike top speed without battery charge (pedalling), in blocks per second (sprinting is 5.6).")
            .defineInRange("pedallingSpeed", 6.6, 0.5, 40.0);
        BIKE_WINDOW = b.comment("Electric Bike input smoothing window in seconds (higher values feel heavier and more bicycle-like).")
            .defineInRange("inputAverageSeconds", 2.5, 0.05, 10.0);
        BIKE_MOTOR = b.comment("Electric Bike motor power draw at full speed, in watts (scales with speed squared, capped by battery rating).")
            .defineInRange("motorWatts", 100.0, 0.0, 10000.0);
        BIKE_TIP_DELAY = b.comment("Seconds a stationary Electric Bike stays upright with a rider before it may tip over.")
            .defineInRange("tipOverDelaySeconds", 5.0, 0.0, 3600.0);
        BIKE_TIP_CHANCE = b.comment("Probability per second that a stationary Electric Bike tips over after tipOverDelaySeconds (0 disables).")
            .defineInRange("tipOverChancePerSecond", 0.1, 0.0, 1.0);
        BIKE_CRASH = b.comment("Speed loss (blocks per second) against a wall on the ground that throws the rider off an Electric Bike.")
            .defineInRange("crashSpeed", 3.0, 0.0, 100.0);
        BIKE_JUMP = b.comment("Electric Bike full-charge jump height, in blocks (1.31 clears one full block).")
            .defineInRange("jumpHeight", 1.31, 0.0, 5.0);
        BIKE_STEP = b.comment("Electric Bike step-up height without jumping, in blocks (0.63 climbs slabs and stairs; 1.0+ climbs full blocks).")
            .defineInRange("stepHeight", 0.63, 0.0, 2.0);
        BIKE_TURN = b.comment("Electric Bike steering turn rate at low speed, in degrees per tick.")
            .defineInRange("turnRate", 12.0, 0.5, 180.0);
        BIKE_TURN_TOP = b.comment("Electric Bike steering turn rate at powered top speed and above, in degrees per tick (interpolated from turnRate).")
            .defineInRange("turnRateAtTopSpeed", 5.0, 0.5, 180.0);
        BIKE_LAMP = b.comment("Front headlamp power draw from the trunk battery on Electric and Terrain Bikes, in watts.")
            .defineInRange("lampWatts", 20.0, 0.0, 1000.0);
        TERRAIN_STEP = b.comment("Terrain Bike step-up height without jumping, in blocks (1.13 climbs full blocks).")
            .defineInRange("terrainStepHeight", 1.13, 0.0, 2.0);
        TERRAIN_JUMP = b.comment("Terrain Bike full-charge jump height, in blocks.")
            .defineInRange("terrainJumpHeight", 2.5, 0.0, 6.0);
        TERRAIN_MOTOR = b.comment("Terrain Bike motor power draw at full speed, in watts.")
            .defineInRange("terrainMotorWatts", 150.0, 0.0, 10000.0);
        b.pop();
        b.push("anvil");
        ANVIL_POWER = b.comment("Electrical Anvil power draw while welding at 400 V, in watts.")
            .defineInRange("power", 5000.0, 10.0, 1e6);
        ANVIL_UNIT = b.comment("Electrical Anvil energy required per unit of repair material (one quarter of tool durability), in joules.")
            .defineInRange("joulesPerUnit", 40000.0, 0.0, 1e9);
        ANVIL_NAME = b.comment("Electrical Anvil energy required to engrave a new tool name, in joules.").defineInRange("joulesPerName", 2000.0, 0.0, 1e9);
        ANVIL_RESUME = b.comment("Electrical Anvil start voltage threshold as a fraction of 400 V to begin a welding pulse.").defineInRange("resumeFraction", 0.95, 0.0, 2.0);
        ANVIL_PLATEAU = b.comment("Electrical Anvil fallback start voltage threshold (fraction of 400 V) if supply voltage levels off for 2 s below resumeFraction.")
            .defineInRange("plateauFraction", 0.93, 0.0, 2.0);
        ANVIL_SAG = b.comment("Electrical Anvil voltage sag threshold (fraction of 400 V) below which a welding pulse pauses while keeping progress.").defineInRange("sagFraction", 0.9, 0.0, 2.0);
        ANVIL_BURSTS = b.comment("Number of welding bursts the Electrical Anvil splits each unit of repair material into.")
            .defineInRange("burstsPerUnit", 2, 1, 100);
        ANVIL_GAP = b.comment("Minimum recovery pause between Electrical Anvil welding bursts, in seconds.")
            .defineInRange("burstGapSeconds", 3.0, 0.0, 600.0);
        b.pop();
        b.push("capacitorBank");
        BANK_FARADS = b.comment("Capacitor Bank capacitance, in farads.")
            .defineInRange("capacitanceFarads", 1.44, 0.001, 100.0);
        BANK_VOLTS = b.comment("Capacitor Bank rated voltage, in volts (bursts above 1.3x this rating).").defineInRange("ratingVolts", 500.0, 1.0, 100000.0);
        BANK_LEAK = b.comment("Capacitor Bank self-discharge time constant (RC), in seconds.")
            .defineInRange("leakTauSeconds", 1800.0, 1.0, 1e9);
        POWER_CAP_LEAK = b.comment("Power Capacitor self-discharge time constant (RC), in seconds.")
            .defineInRange("powerCapacitorLeakTauSeconds", 1800.0, 1.0, 1e9);
        b.pop();
        b.push("oilLakes");
        LAKE_SIZE = b.comment("Volume multiplier for newly generated underground oil lakes (increases depth while keeping the same footprint).")
            .defineInRange("sizeMultiplier", 2.0, 0.25, 5.0);
        FULL_AT = b.comment("Oil lake fill fraction at or below which natural seepage reaches maximum; above this it falls linearly to zero when full.").defineInRange("seepFullAt", 0.25, 0.0, 1.0);
        SEEP_SHARE = b.comment("Maximum natural oil lake seepage rate, as a fraction of one 230 V Oil Pump's nominal rate (shared by all pumps on the lake).").defineInRange("seepMaxShare", 0.25, 0.0, 1.0);
        STEAM_SHARE = b.comment("Maximum steam-boosted oil lake seepage rate, as a fraction of one 230 V Oil Pump's nominal rate.").defineInRange("seepSteamShare", 1.0, 0.0, 10.0);
        STEAM_PER_BUCKET = b.comment("Mass of injected steam (kg) required to boost oil lake seepage by one bucket of crude.")
            .defineInRange("steamKgPerBucket", 0.2, 0.001, 100.0);
        b.pop();
        b.push("probe");
        PROBE_DETAILS = b.comment("Show full Jade probe details by default without needing to sneak or hold Jade's details key.")
            .define("details", false);
        b.pop();
        b.push("introspect");
        INTROSPECT_REMOTE = b.comment("Allow operator clients to query live device introspection reports over the network (/eln describe works for ops either way).")
            .define("remote", false);
        b.pop();
        SPEC = b.build();
    }

    /** Cached (the battery processes read it every second). */
    public static volatile double batteryLifeFactor = 25.0;
    public static volatile double heatFurnaceFuelFactor = 2.5;
    /** Whether Jade shows detail lines without sneaking (config probe.details). */
    public static volatile boolean probeDetails = false;
    /** The six-node Power Capacitor's leak, RC (s): config capacitorBank.powerCapacitorLeakTauSeconds. */
    public static volatile double powerCapacitorLeakTauS = 1800;

    static void refresh() {
        batteryLifeFactor = BATTERY_LIFE_FACTOR.get();
        heatFurnaceFuelFactor = HEAT_FURNACE_FUEL.get();
        probeDetails = PROBE_DETAILS.get();
        mods.eln.introspect.Introspect.remote = INTROSPECT_REMOTE.get();
        mods.eln.node.voxel.CableConverter.convertOnLoad = CONVERT_CABLES.get();
        mods.eln.node.voxel.VoxelNode.linksBuild = LINKS_BUILD.get();
        mods.eln.sim.Fire.COAL.lag = COAL_LAG.get();
        mods.eln.sim.Fire.COAL.kindle = COAL_KINDLE.get();
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
        mods.eln.bike.BikePhysics.TURN_PER_TICK = (float) (double) BIKE_TURN.get();
        mods.eln.bike.BikePhysics.TURN_AT_TOP = (float) (double) BIKE_TURN_TOP.get();
        mods.eln.bike.BikePhysics.LAMP_WATTS = BIKE_LAMP.get();
        mods.eln.bike.BikeType.terrainStep = (float) (double) TERRAIN_STEP.get();
        mods.eln.bike.BikeType.terrainJumpVy = mods.eln.bike.BikePhysics.velocityForPeak(TERRAIN_JUMP.get());
        mods.eln.bike.BikeType.terrainWatts = TERRAIN_MOTOR.get();
        Eln.autominerRange = AUTOMINER_RANGE.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.powerW = ANVIL_POWER.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.joulesPerUnit = ANVIL_UNIT.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.joulesPerName = ANVIL_NAME.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.resume = ANVIL_RESUME.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.plateau = ANVIL_PLATEAU.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.sag = ANVIL_SAG.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.bursts = ANVIL_BURSTS.get();
        mods.eln.anvil.ElectricalAnvilDescriptor.burstGapS = ANVIL_GAP.get();
        mods.eln.transparentnode.capacitorbank.CapacitorBankDescriptor.farads = BANK_FARADS.get();
        mods.eln.transparentnode.capacitorbank.CapacitorBankDescriptor.ratedVolts = BANK_VOLTS.get();
        mods.eln.transparentnode.capacitorbank.CapacitorBankDescriptor.leakTauS = BANK_LEAK.get();
        powerCapacitorLeakTauS = POWER_CAP_LEAK.get();
        mods.eln.oil.OilLakeStructure.sizeMultiplier = LAKE_SIZE.get();
        mods.eln.oil.OilRecovery.fullAt = FULL_AT.get();
        mods.eln.oil.OilRecovery.steamShare = STEAM_SHARE.get();
        mods.eln.oil.OilRecovery.seepShare = SEEP_SHARE.get();
        mods.eln.oil.OilRecovery.steamKgPerBucket = STEAM_PER_BUCKET.get();
        if (mods.eln.Eln.sixNodeItem != null)   // (config can load before the devices register; they take it from the field)
            for (Object d : mods.eln.Eln.sixNodeItem.all().values())
                if (d instanceof mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor p) p.dischargeTao = powerCapacitorLeakTauS;
    }
}
