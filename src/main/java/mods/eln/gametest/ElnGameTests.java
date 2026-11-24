package mods.eln.gametest;

import mods.eln.Eln;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/**
 * Registers every {@link TestCase} as a GameTest eln:&lt;id&gt; in structure eln:floor_WxHxD. Run them headless with
 * `gradlew runGameTestServer` (exit code = number of failed tests), or in game with `/test runall`.
 */
public final class ElnGameTests {
    private ElnGameTests() {
    }

    static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, Eln.MODID);

    /** The cases; ported 1.12 selftest cases are added here batch by batch. */
    static List<TestCase> cases() {
        // the cable benchmark runs alone (-PelnBench=FILE; notes/cables/PERF.md)
        if (mods.eln.gametest.bench.CableBench.enabled()) return List.of(new mods.eln.gametest.bench.CableBench());
        // so does the render benchmark's CPU tier (-PelnRenderBench=FILE)
        if (mods.eln.gametest.bench.RenderBench.enabled()) return List.of(new mods.eln.gametest.bench.RenderBench());
        java.util.List<TestCase> l = new java.util.ArrayList<>(List.of(new SimTickCase(), new KotlinRuntimeCase(), new GoldenCase(), new GoldenRecipesCase(), new GoldenSlotsCase(), new PublishRenderCase(), new GuiPacketCase(), new MachineMenuCase(), new MenusOpenCase(), new AutoMinerSpinCase(), new LampRestCase(), new ItemDrawCase(), new XRayCase()));
        l.addAll(mods.eln.gametest.cases.Batch1Cases.cases());
        l.addAll(mods.eln.gametest.cases.SolarCases.cases());
        mods.eln.gametest.cases.Wp9aCases.addTo(l);
        mods.eln.gametest.cases.RouteLeakCases.addTo(l);
        mods.eln.gametest.cases.LinksCases.addTo(l);
        mods.eln.gametest.cases.Wp9bCases.addTo(l);
        mods.eln.gametest.cases.Wp9cCases.addTo(l);
        mods.eln.gametest.cases.LogicTruthCases.addTo(l);
        mods.eln.gametest.cases.ChipMoreCases.addTo(l);
        mods.eln.gametest.cases.WirelessMoreCases.addTo(l);
        mods.eln.gametest.cases.Wp10aCases.addTo(l);
        mods.eln.gametest.cases.Wp10bCases.addTo(l);
        mods.eln.gametest.cases.WorldSensorCases.addTo(l);
        mods.eln.gametest.cases.Wp11Cases.addTo(l);
        mods.eln.gametest.cases.MachineApiCases.addTo(l);
        mods.eln.gametest.cases.ShaftMoreCases.addTo(l);
        mods.eln.gametest.cases.ShaftSteamCases.addTo(l);
        mods.eln.gametest.cases.PlumbingCases.addTo(l);
        mods.eln.gametest.cases.ShaftPlantCases.addTo(l);
        mods.eln.gametest.cases.HpPlantCases.addTo(l);
        mods.eln.gametest.cases.PipeSizeCases.addTo(l);
        mods.eln.gametest.cases.GasPlantCases.addTo(l);
        mods.eln.gametest.cases.HeatExchangerCases.addTo(l);
        mods.eln.gametest.cases.MixerCases.addTo(l);
        mods.eln.gametest.cases.ShaftArtCases.addTo(l);
        mods.eln.gametest.cases.WrenchTurnCases.addTo(l);
        l.addAll(mods.eln.gametest.cases.HitboxCases.cases());
        mods.eln.gametest.cases.PortHelperCases.addTo(l);
        mods.eln.gametest.cases.LampSupplyCases.addTo(l);
        mods.eln.gametest.cases.LampLightCases.addTo(l);
        mods.eln.gametest.cases.CableSlotCases.addTo(l);
        mods.eln.gametest.cases.SixNodeBreakCases.addTo(l);
        mods.eln.gametest.cases.BreakToolCases.addTo(l);
        mods.eln.gametest.cases.TransparentSupportCases.addTo(l);
        mods.eln.gametest.cases.AgeFixCases.addTo(l);
        mods.eln.gametest.cases.OilCases.addTo(l);
        mods.eln.gametest.cases.CombustionCases.addTo(l);
        mods.eln.gametest.cases.FluidViewCases.addTo(l);
        mods.eln.gametest.cases.GuiPacketFuzzCases.addTo(l);
        mods.eln.gametest.cases.OilLakeCases.addTo(l);
        mods.eln.gametest.cases.ReservoirCases.addTo(l);
        mods.eln.gametest.cases.MinerCableCases.addTo(l);
        mods.eln.gametest.cases.HandbookFixCases.addTo(l);
        mods.eln.gametest.cases.FurnaceControlCases.addTo(l);
        mods.eln.gametest.cases.BatteryCurveCases.addTo(l);
        mods.eln.gametest.cases.PoleCableCases.addTo(l);
        mods.eln.gametest.cases.BusCases.addTo(l);
        mods.eln.gametest.cases.BusModuleCases.addTo(l);
        mods.eln.gametest.cases.BusDeviceCases.addTo(l);
        mods.eln.gametest.cases.BusSpCases.addTo(l);
        mods.eln.gametest.cases.AmbientCases.addTo(l);
        mods.eln.gametest.cases.PipeCases.addTo(l);
        mods.eln.gametest.cases.JoinRouteCases.addTo(l);
        mods.eln.gametest.cases.PipeTransmitterCases.addTo(l);
        mods.eln.gametest.cases.FluidSpeciesCases.addTo(l);
        mods.eln.gametest.cases.CableCases.addTo(l);
        mods.eln.gametest.cases.CableAirCases.addTo(l);
        mods.eln.gametest.cases.ToolbagCases.addTo(l);
        mods.eln.gametest.cases.JunctionCases.addTo(l);
        mods.eln.gametest.cases.SpecCases.addTo(l);
        mods.eln.gametest.cases.VoxelDeviceCases.addTo(l);
        mods.eln.gametest.cases.Wp12Cases.addTo(l);
        mods.eln.gametest.cases.HeadlampCases.addTo(l);
        mods.eln.gametest.cases.BurnChainCases.addTo(l);
        mods.eln.gametest.cases.BurntCases.addTo(l);
        mods.eln.gametest.cases.TooltipCases.addTo(l);
        mods.eln.gametest.cases.TurretCases.addTo(l);
        mods.eln.gametest.cases.DimensionCases.addTo(l);
        mods.eln.gametest.cases.AutoMinerCases.addTo(l);
        mods.eln.gametest.cases.OreDropCases.addTo(l);
        mods.eln.gametest.cases.RubberCases.addTo(l);
        mods.eln.gametest.cases.GroundRadarCases.addTo(l);
        mods.eln.gametest.cases.ItemCapabilityCases.addTo(l);
        mods.eln.gametest.cases.LampMonsterCases.addTo(l);
        mods.eln.gametest.cases.BeltCases.addTo(l);
        mods.eln.gametest.cases.ChargerWearCases.addTo(l);
        mods.eln.gametest.cases.BikeCases.addTo(l);
        mods.eln.gametest.cases.AnvilCases.addTo(l);
        mods.eln.gametest.cases.WireDrawingCases.addTo(l);
        mods.eln.gametest.cases.CapacitorBankCases.addTo(l);
        mods.eln.gametest.cases.IncandescenceCases.addTo(l);
        mods.eln.gametest.cases.HandbookCases.addTo(l);
        mods.eln.gametest.cases.JeiAuditCases.addTo(l);
        mods.eln.gametest.cases.RecipeClashCases.addTo(l);
        mods.eln.gametest.cases.KitDeviceCases.addTo(l);
        mods.eln.gametest.cases.HubRenderCases.addTo(l);
        mods.eln.gametest.cases.ArmourEnchantCases.addTo(l);
        mods.eln.gametest.cases.OilPondCases.addTo(l);
        mods.eln.gametest.cases.ServoCases.addTo(l);
        mods.eln.gametest.cases.ComputerCases.addTo(l);
        mods.eln.gametest.cases.HostedMachineCases.addTo(l);
        mods.eln.gametest.cases.LowLevelCases.addTo(l);
        mods.eln.gametest.cases.HubSlotCases.addTo(l);
        return l;
    }

    public static void register(IEventBus modBus) {
        for (TestCase c : cases()) FUNCTIONS.register(c.id(), () -> helper -> run(helper, c));
        FUNCTIONS.register(modBus);
        TestEnergySinks.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, ElnGameTests::registerTests);
    }

    /**
     * Empty blocks round every test structure. The test server lays every batch's structures out in one grid and
     * keeps the old ones, 5 blocks apart: a neighbour's torch lit this test's blocks, its biome cells and its mobs
     * reached in, and the outcome hung on which tests ran beside it. 2 x 8 + 5 = 21 blocks is past a light's reach (15)
     * and a 4-block biome cell. Baughn: the order of the tests must not matter.
     */
    public static final int PADDING = 8;

    /** The chunks under the structure and its padding stay loaded for the test (the server forces only the structure's). */
    public static void forceLoadPadding(GameTestHelper helper) {
        net.minecraft.world.phys.AABB b = helper.getBoundsWithPadding();
        for (int cx = (int) Math.floor(b.minX) >> 4; cx <= (int) Math.floor(b.maxX) >> 4; cx++)
            for (int cz = (int) Math.floor(b.minZ) >> 4; cz <= (int) Math.floor(b.maxZ) >> 4; cz++)
                helper.getLevel().setChunkForced(cx, cz, true);   // (the runner releases every forced chunk after the batch)
    }

    static void run(GameTestHelper helper, TestCase c) {
        forceLoadPadding(helper);
        // the test world's biomes vary from test to test: 20 C everywhere, as the cases' numbers assume (AmbientCases
        // lift it for their own devices)
        mods.eln.sim.Ambient.fixedC = 20.0;
        // (the game's own fluid step budget, so a budget that cuts healthy plants shows up in stalls(): the 10 ms one
        // broke more or less every plant on Incognito and no test saw it, run without it; Baughn 1573837175051522166.
        // At 4x it lets the test plants' start-up transients through (a fuel furnace's first steps took over 400 ms; at
        // 40 ms 27 tests stalled), and fails them if the game's budget is ever as tight as 10 ms was)
        mods.eln.sim.fluid.CoreStep.budgetNanos = 4 * mods.eln.sim.fluid.CoreStep.GAME_BUDGET;
        GameTestContext ctx = new GameTestContext(helper, c.id());
        // tests share one level: a node left by an earlier test here would join this test's circuits
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.coordonate.dimention == mods.eln.misc.Dimensions.id(helper.getLevel())
                && bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) {
                ctx.check(c.id() + ": no leaked node from a previous test", false, n.getClass().getSimpleName() + " at " + n.coordonate);
                cleanUp(helper, ctx);
                ctx.finish();
                return;
            }
        }
        // the stone floor (y = 0) of the 1.12 selftest rows; laid here, the structure only gives the empty box
        for (int x = 0; x < c.width(); x++)
            for (int z = 0; z < c.floorDepth(); z++) ctx.floor(ctx.at(x, 0, z));
        try {
            c.build(ctx);
        } catch (RuntimeException e) {
            ctx.check(c.id() + " build", false, e.toString());
            cleanUp(helper, ctx);
            ctx.finish();
            return;
        }
        helper.runAfterDelay(c.ticks(), () -> {
            try {
                c.measure(ctx);
            } catch (RuntimeException e) {
                ctx.check(c.id() + " measure", false, e.toString());
            }
            mods.eln.introspect.DescribeCheck.run(helper.getLevel(), helper.getBounds(), ctx::check);
            if (c.checkStalls()) stalls(helper, ctx);
            if (c.checkLimitCycles()) limitCycles(helper, ctx);   // (before the round trip: reloaded nodes start a fresh record)
            links(helper, ctx);
            linksNeutral(helper, ctx);
            fluidTopology(helper, ctx);
            persistence(helper, ctx);
            cleanUp(helper, ctx);
            ctx.finish();
        });
    }

    /**
     * Clears the test's structure and padding when it is done: every block (but the test instance block) to air, its
     * entities discarded. The test server keeps every earlier batch's structures, and EA simulates nodes in unloaded
     * chunks, so without this every device a test built kept running to the end of the suite (by the oil batches the
     * server ticked at ~18 tps; the suite took 70 min). Removing the blocks removes their nodes as a player's breaking
     * them would; any node still in the bounds after that is a leak and fails the test.
     */
    static void cleanUp(GameTestHelper helper, GameTestContext ctx) {
        var level = helper.getLevel();
        net.minecraft.world.phys.AABB b = helper.getBoundsWithPadding();
        net.minecraft.core.BlockPos lo = net.minecraft.core.BlockPos.containing(b.minX, b.minY, b.minZ);
        net.minecraft.core.BlockPos hi = net.minecraft.core.BlockPos.containing(b.maxX - 1e-6, b.maxY - 1e-6, b.maxZ - 1e-6);
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(lo, hi)) {
            var st = level.getBlockState(p);
            if (st.isAir() || st.is(net.minecraft.world.level.block.Blocks.TEST_INSTANCE_BLOCK)) continue;
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, b, e -> !(e instanceof net.minecraft.world.entity.player.Player)))
            e.discard();
        mods.eln.api.impl.HostedMachines.INSTANCE.retireGone();   // (the API's nodes go before the next step, by design: now)
        int dim = mods.eln.misc.Dimensions.id(level);
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.coordonate.dimention != dim || !b.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) continue;
            ctx.check("clean-up: no node left once the test's blocks are gone", false, n.getClass().getSimpleName() + " at " + n.coordonate
                + " (block there: " + level.getBlockState(new net.minecraft.core.BlockPos(n.coordonate.x, n.coordonate.y, n.coordonate.z)) + ")");
            n.onBreakBlock();
        }
    }

    /**
     * No fluid network of a device in the test's bounds went a quarter second without progress, nor lost a quarter second
     * of time to the cap on owed time (Baughn 2026-11-10).
     */
    static void stalls(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        double worst = 0, lost = 0;
        String where = "";
        for (var d : mods.eln.fluid.FluidWorld.devices())
            for (var p : d.fluidPorts()) {
                var b = net.minecraft.core.BlockPos.of(p.pos());
                if (!bounds.contains(b.getX() + .5, b.getY() + .5, b.getZ() + .5)) continue;
                var n = p.node();
                if (n.longestStall > worst || n.droppedTime > lost) where = d.getClass().getSimpleName() + " at " + b.toShortString();
                worst = Math.max(worst, n.longestStall);
                lost = Math.max(lost, n.droppedTime);
            }
        if (worst > 0.25 || lost > 0.25) ctx.check("fluid: no network goes a quarter second without progress", false,
            where + String.format(": %.2f s in a row, %.2f s dropped", worst, lost));
    }

    /**
     * No fluid node of a device in the test's bounds flips its pressure back and forth every step at the end (a solver
     * limit cycle, as the boiler feed valve's of 2026-11-04). It needs the case to have run 20 fluid steps.
     */
    static void limitCycles(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        java.util.Set<mods.eln.sim.fluid.FluidNode> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var d : mods.eln.fluid.FluidWorld.devices())
            for (var p : d.fluidPorts()) {
                var b = net.minecraft.core.BlockPos.of(p.pos());
                if (!bounds.contains(b.getX() + .5, b.getY() + .5, b.getZ() + .5)) continue;
                var n = mods.eln.fluid.FluidWorld.nodeFor(p.node());
                if (!seen.add(n)) continue;
                int f = mods.eln.fluid.FluidWorld.recentFlips(n);
                if (f >= 15) ctx.check("fluid: no limit cycle (a node flipping every step)", false,
                    d.getClass().getSimpleName() + " at " + b.toShortString() + ": " + f + " of the last 20 steps, P " + String.format("%.3f bar", n.P() / 1e5));
            }
    }

    static void registerTests(RegisterGameTestsEvent event) {
        mods.eln.robotics.computer.hw.Runner.SYNC = true;   // (computers keep pace with a test server ticking flat out)
        mods.eln.fluid.FluidWorld.watchAll = true;
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(Eln.MODID, "default"), new TestEnvironmentDefinition.AllOf(List.of()));
        // (-Deln.gametests=a,b,...: only the cases whose ids are listed; for re-running a handful while working)
        String only = System.getProperty("eln.gametests");
        java.util.Set<String> wanted = only == null || only.isBlank() ? null : java.util.Set.of(only.split(","));
        for (TestCase c : cases()) {
            if (wanted != null && !wanted.contains(c.id())) continue;
            Identifier id = Identifier.fromNamespaceAndPath(Eln.MODID, c.id());
            Identifier structure = Identifier.fromNamespaceAndPath(Eln.MODID,
                "floor_" + c.width() + "x" + c.height() + "x" + c.depth());
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, id);
            event.registerTest(id, new FunctionGameTestInstance(fn, new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD,
                structure, c.ticks() + 40, 0, true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, PADDING)));
        }
    }

    /**
     * Routing phase 3b: the voxel blocks in the test's bounds, reconnected with `[cables] linksBuild` off (the rules make
     * the links) and then on (the joints do), have the same links and connection flags (LinksDiff).
     */
    static void linksNeutral(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        java.util.List<mods.eln.node.NodeBase> in = new java.util.ArrayList<>();
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.coordonate.dimention != mods.eln.misc.Dimensions.id(helper.getLevel())) continue;
            if (bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) in.add(n);
        }
        var scope = LinksDiff.added(in);
        if (scope.isEmpty()) return;
        var off = LinksDiff.connectedWith(scope, false);
        var on = LinksDiff.connectedWith(scope, true);
        ctx.check("links: the joints build the same network as the rules", off.equals(on),
            off.size() + " vs " + on.size() + " lines; " + LinksDiff.difference(off, on, 6));
        // one block at a time (second opinion, 11-13: a block reconnecting alone builds its links against neighbours
        // whose joints were confirmed in their own passes): after each single reconnect, still the rules' network
        String single = "";
        for (var v : scope) {
            var now = LinksDiff.connectedWith(java.util.List.of(v), true);
            var all = LinksDiff.canonical(scope);
            if (!all.equals(off) && single.isEmpty())
                single = v.coordonate + ": " + LinksDiff.difference(off, all, 6);
        }
        ctx.check("links: each block reconnected alone keeps the rules' network", single.isEmpty(), single);
    }

    /**
     * The fluid world's topology around the test: the pure plan (FluidWorld.Plan.dump) is the same with the devices in
     * their order, reversed and shuffled; the networks as built (FluidWorld.liveTopology, without a rebuild) have the
     * plan's runs and networks, unless a rebuild is pending; and their hash is recorded, so two builds of the mod can be
     * compared test by test.
     */
    static void fluidTopology(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        java.util.function.LongPredicate in = p -> {
            var b = net.minecraft.core.BlockPos.of(p);
            return bounds.contains(b.getX() + .5, b.getY() + .5, b.getZ() + .5);
        };
        var devices = mods.eln.fluid.FluidWorld.devices();
        var plan = relative(mods.eln.fluid.FluidWorld.plan(devices).dump(in), bounds);
        var live = relative(mods.eln.fluid.FluidWorld.liveTopology(in), bounds);
        if (plan.isEmpty() && live.isEmpty()) return;
        var reversed = new java.util.ArrayList<>(devices);
        java.util.Collections.reverse(reversed);
        var shuffled = new java.util.ArrayList<>(devices);
        java.util.Collections.shuffle(shuffled, new java.util.Random(devices.size()));
        var r = relative(mods.eln.fluid.FluidWorld.plan(reversed).dump(in), bounds);
        var sh = relative(mods.eln.fluid.FluidWorld.plan(shuffled).dump(in), bounds);
        ctx.check("fluid: the topology is the same whatever order the devices join in", plan.equals(r) && plan.equals(sh),
            plan.size() + " lines" + (plan.equals(r) ? "" : "; reversed: " + LinksDiff.difference(plan, r, 6))
                + (plan.equals(sh) ? "" : "; shuffled: " + LinksDiff.difference(plan, sh, 6)));
        if (!mods.eln.fluid.FluidWorld.dirty()) {
            var want = plan.stream().filter(l -> l.startsWith("run ") || l.startsWith("net ")).toList();
            var got = live.stream().filter(l -> l.startsWith("run ") || l.startsWith("net ")).toList();
            ctx.check("fluid: the networks built have the plan's runs and networks", want.equals(got), LinksDiff.difference(want, got, 6));
        }
        ctx.check("fluid: the networks as built", true, live.size() + " lines, " + (mods.eln.fluid.FluidWorld.dirty() ? "rebuild pending, " : "")
            + "hash " + Integer.toHexString(String.join("\n", live).hashCode()));
    }

    /**
     * Topology lines with their blocks ("x,y,z") relative to the test's corner, a run's or network's names sorted again,
     * and sorted (the suite places tests anywhere).
     */
    static java.util.List<String> relative(java.util.List<String> lines, net.minecraft.world.phys.AABB bounds) {
        var xyz = java.util.regex.Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+)");
        int x0 = (int) Math.floor(bounds.minX), y0 = (int) Math.floor(bounds.minY), z0 = (int) Math.floor(bounds.minZ);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String l : lines) {
            l = xyz.matcher(l).replaceAll(m -> (Integer.parseInt(m.group(1)) - x0) + "," + (Integer.parseInt(m.group(2)) - y0) + "," + (Integer.parseInt(m.group(3)) - z0));
            if (l.startsWith("run ") || l.startsWith("net ")) {
                var names = new java.util.ArrayList<>(java.util.List.of(l.substring(4).split(", ")));
                java.util.Collections.sort(names);
                l = l.substring(0, 4) + String.join(", ", names);
            }
            out.add(l);
        }
        java.util.Collections.sort(out);
        return out;
    }

    /** The cable pieces' stored joints say what the live network has (CableVerify), over the voxel blocks in the test's bounds. */
    static void links(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        java.util.List<mods.eln.node.voxel.VoxelNode> scope = new java.util.ArrayList<>();
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (!(n instanceof mods.eln.node.voxel.VoxelNode v) || n.isDestructing() || n.coordonate.dimention != mods.eln.misc.Dimensions.id(helper.getLevel())) continue;
            if (bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) scope.add(v);
        }
        if (scope.isEmpty()) return;
        var rep = mods.eln.node.voxel.CableVerify.verify(scope);
        ctx.check("links: the cables' joints match the network", rep.ok(),
            rep.summary() + (rep.ok() ? "" : "; " + String.join("; ", rep.problems().subList(0, Math.min(6, rep.problems().size())))));
    }

    /**
     * Save/load round trip of every saved node (mustBeSaved) in the test's bounds, in the state the case left it (1.12 selftest
     * persistence pass): only live sim state (voltages, switch-resistor states) may differ.
     */
    static void persistence(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        int ok = 0, total = 0;
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.isDestructing() || n.coordonate.dimention != mods.eln.misc.Dimensions.id(helper.getLevel())) continue;
            if (!bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) continue;
            if (!n.mustBeSaved()) continue;   // (ghost and hosted-machine nodes: never in a save, nothing to round-trip)
            total++;
            try {
                Reload r = Reload.node(helper.getLevel(), n.coordonate.pos());
                java.util.List<String> hard = new java.util.ArrayList<>();
                for (String d : r.differences()) if (!Reload.isTransient(d)) hard.add(d);
                if (hard.isEmpty()) ok++;
                else ctx.check("persistence " + Reload.describe(r.fresh) + " at " + n.coordonate, false, "NBT: " + String.join("; ", hard));
            } catch (ReflectiveOperationException | RuntimeException e) {
                ctx.check("persistence at " + n.coordonate, false, e.toString());
            }
        }
        if (total > 0) ctx.check("persistence: every node keeps its saved state", ok == total, ok + "/" + total);
    }
}
