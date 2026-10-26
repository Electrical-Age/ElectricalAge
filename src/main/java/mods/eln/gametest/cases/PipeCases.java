package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.AdapterPart;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.node.voxel.VoxelNodeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Pipes phase 3 (notes/pipes/DESIGN.md): voxel nodes holding pipe parts, coupled into fluid networks. */
public final class PipeCases {
    private PipeCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new BoilerPipeTurbine());
        cases.add(new BoilerBucket());
        cases.add(new BoilerFeed());
        cases.add(new BoilerPhase());
        cases.add(new PumpFeedsBoiler());
        cases.add(new PumpWaterAndSignal());
        cases.add(new PumpUnderBoiler());
        cases.add(new PumpDeadHeaded());
        cases.add(new GuideAdvancements());
        cases.add(new TwoPipesOneBlock());
        cases.add(new RoutedOverAWall());
        cases.add(new JoinMidRun());
        cases.add(new KitCrafting());
        cases.add(new Burst());
        cases.add(new Valve());
        cases.add(new Adapter());
        cases.add(new ValveByPlayer());
        cases.add(new KitPickupMerges());
        cases.add(new PipeRoutePreview());
        cases.add(new KitLengthNotDamage());
    }

    static VoxelNode node(TestContext ctx, BlockPos p) {
        return ctx.level().getBlockEntity(p) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
    }

    static void lay(TestContext ctx, BlockPos p, int tier, int... pts) {
        String err = PipeKitItem.place(ctx.level(), p, new PipePart(tier, pts), null);
        if (err != null) throw new IllegalStateException("pipe at " + p + ": " + err);
    }

    /**
     * Boiler and turbine side by side, an iron pipe from the boiler's top port up, across three blocks and down into the
     * turbine's inlet: the turbine runs off the boiler's steam (as the stacked plant, a little behind for the pipe).
     */
    /**
     * The feedwater flange on the boiler's right flank (Baughn: a water input): water under pressure in a pipe there
     * goes into a cold, empty boiler; a hot boiler's pressure doesn't push water back out into an empty feed pipe (the
     * check valve).
     */
    static final class BoilerFeed implements TestCase {
        mods.eln.oil.BoilerElement cold, hot;
        PipePart coldPipe, hotPipe;
        mods.eln.sim.IProcess fire;
        double m0;

        public String id() {
            return "boiler_feedwater_flange";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            cold = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            hot = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 3, 1);
            for (var b : new mods.eln.oil.BoilerElement[]{cold, hot}) {
                b.front = mods.eln.misc.Direction.XN;   // (its right: ZP, south)
                b.reconnect();
            }
            cold.fromTanks(0, 0);
            hot.fromTanks(4000, 0);
            hot.thermal.Tc = 170;
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> hot.thermal.moveEnergyTo(3000 * t));
            lay(ctx, ctx.at(1, 1, 2), 0, 8, 8, 0, 8, 8, 8);
            lay(ctx, ctx.at(3, 1, 2), 0, 8, 8, 0, 8, 8, 8);
            coldPipe = (PipePart) node(ctx, ctx.at(1, 1, 2)).parts.get(0);
            hotPipe = (PipePart) node(ctx, ctx.at(3, 1, 2)).parts.get(0);
            // the cold boiler's pipe: full of water at 10 bar (a feed pump's pressure)
            m0 = coldPipe.fluid.volume * mods.eln.sim.fluid.Water.RHO_L * 0.9;
            coldPipe.fluid.fill(m0, 10e5, mods.eln.sim.fluid.Water.T_AMBIENT);
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            cold.syncTanks();
            cold.showTanks();
            double back = hotPipe.fluid.mLiquid() / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
            ctx.line("feed: cold " + cold.getWaila() + ", hot " + hot.getWaila() + String.format(", hot's pipe %.1f mB water, %.2f bar", back, hotPipe.fluid.P() / 1e5));
            ctx.check("feed: the pipe's water went into the cold boiler", cold.vessel.mLiquid() > 0.1 * m0
                && mods.eln.fluid.FluidWorld.networkOf(cold.vessel) == mods.eln.fluid.FluidWorld.networkOf(coldPipe.fluid), String.format("%.4f kg", cold.vessel.mLiquid()));
            ctx.check("feed: the hot boiler is under pressure", hot.vessel.P() > 1.5e5, String.format("%.2f bar", hot.vessel.P() / 1e5));
            ctx.check("feed: nothing came back out through the check valve", back < 1, String.format("%.1f mB", back));
        }
    }

    /**
     * The phase signal on the boiler's front, under its gauges (Baughn: liquid or gas): high with water above half the gauge glass, low
     * below it; a signal can connect on the front, heat only on the other faces.
     */
    static final class BoilerPhase implements TestCase {
        mods.eln.oil.BoilerElement full, low;

        public String id() {
            return "boiler_phase_signal";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            full = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            low = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 3, 1);
            full.fromTanks(6000, 0);
            low.fromTanks(2000, 0);
        }

        public void measure(TestContext ctx) {
            double uFull = full.phaseOut.getU(), uLow = low.phaseOut.getU();
            var front = full.front;
            ctx.check("phase: high with water at half height", uFull > 0.9 * mods.eln.Eln.SVU, String.format("%.2f V", uFull));
            ctx.check("phase: low without", uLow < 0.1 * mods.eln.Eln.SVU, String.format("%.2f V", uLow));
            ctx.check("phase: the front takes a signal, not heat; the back takes heat", full.getConnectionMask(front, mods.eln.misc.LRDU.Down) == mods.eln.node.NodeBase.maskElectricalOutputGate
                && full.getThermalLoad(front, mods.eln.misc.LRDU.Down) == null
                && full.getConnectionMask(front.back(), mods.eln.misc.LRDU.Down) == mods.eln.node.NodeBase.maskThermal, "");
        }
    }

    static mods.eln.oil.SubmersiblePumpElement pumpInWater(TestContext ctx, Wp10aCases.Layout l, int dx, int dz) {
        BlockPos p = ctx.at(dx, 1, dz);
        ctx.level().setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2);
        var pump = (mods.eln.oil.SubmersiblePumpElement) l.device(8 + (81 << 6), dx, dz);
        pump.front = mods.eln.misc.Direction.XN;   // (signal on the west, power north and south)
        pump.reconnect();
        return pump;
    }

    /**
     * A 50V submersible pump (Baughn: a small waterlogged feed pump) standing in water, powered, piped from its top
     * flange up, over and down into a cold empty boiler's feedwater flange: it fills the boiler.
     */
    static final class PumpFeedsBoiler implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SubmersiblePumpElement pump;

        public String id() {
            return "submersible_pump_feeds_a_boiler";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            boiler.front = mods.eln.misc.Direction.XN;   // (feed flange south)
            boiler.reconnect();
            boiler.fromTanks(0, 0);
            lay(ctx, ctx.at(2, 2, 2), 0, 8, 0, 8, 8, 8, 8, 0, 8, 8);    // up out of the pump, then west
            lay(ctx, ctx.at(1, 2, 2), 0, 16, 8, 8, 8, 8, 8, 8, 0, 8);   // east in, then down
            lay(ctx, ctx.at(1, 1, 2), 0, 8, 16, 8, 8, 8, 8, 8, 8, 0);   // down in, then north into the flange
            l.used.add(ctx.at(1, 1, 2));
            ctx.level().setBlockAndUpdate(ctx.at(3, 1, 2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            pump = pumpInWater(ctx, l, 2, 2);
            l.feed(ctx.at(2, 1, 2), pump.front.left());
        }

        public void measure(TestContext ctx) {
            boiler.syncTanks();
            boiler.showTanks();
            ctx.line("pump: " + pump.getWaila() + ", boiler " + boiler.getWaila());
            ctx.check("pump: in water, running", pump.submerged && pump.speed == 1 && pump.motor.getP() > 50, pump.getWaila().toString());
            // (57 mB/s for a minute, less what the pipe run holds)
            ctx.check("pump: the boiler filled through its feed flange", boiler.tanks.water() > 1000, boiler.tanks.water() + " mB");
        }
    }

    /**
     * Dysnomiaw's rig: the pump under the pipe block that turns into the boiler's feed flange, the boiler one block
     * up and over (a single pipe block, in from below, out north). The riser's top port must spill once it is full: it
     * drew only gas, and the pump pushed it to 9 bar with the boiler dry.
     */
    static final class PumpUnderBoiler implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SubmersiblePumpElement pump;
        PipePart pipe;

        public String id() {
            return "submersible_pump_under_a_boiler";
        }

        public int width() {
            return 5;
        }

        public int height() {
            return 7;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            ctx.level().setBlockAndUpdate(ctx.at(2, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            boiler = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), ctx.at(2, 2, 1));
            boiler.front = mods.eln.misc.Direction.XN;   // (feed flange south)
            boiler.reconnect();
            boiler.fromTanks(0, 0);
            lay(ctx, ctx.at(2, 2, 2), 0, 8, 0, 8, 8, 8, 8, 8, 8, 0);   // up from the pump, then north into the flange
            pipe = (PipePart) node(ctx, ctx.at(2, 2, 2)).parts.get(0);
            pump = pumpInWater(ctx, l, 2, 2);
            pump.front = mods.eln.misc.Direction.ZP;   // (power on the flanks: east and west)
            pump.reconnect();
            l.feed(ctx.at(2, 1, 2), mods.eln.misc.Direction.XN);
        }

        public void measure(TestContext ctx) {
            boiler.syncTanks();
            boiler.showTanks();
            var net = mods.eln.fluid.FluidWorld.networkOf(boiler.vessel);
            ctx.line("pump under boiler: pump " + pump.getWaila() + ", boiler " + boiler.getWaila() + String.format(", pipe %.2f bar, feed %.2f bar, one network %s",
                pipe.fluid.P() / 1e5, boiler.feed.P() / 1e5, net != null && net == mods.eln.fluid.FluidWorld.networkOf(pipe.fluid)));
            ctx.check("pump under boiler: the boiler fills", boiler.tanks.water() > 200, boiler.tanks.water() + " mB");
        }
    }

    /** A pump with nowhere to push: its outlet rises to the shutoff pressure (3 bar) and no further, and the flow stops
     *  (the pump is a stamped edge with that head, Baughn: not water injected per tick). */
    static final class PumpDeadHeaded implements TestCase {
        mods.eln.oil.SubmersiblePumpElement pump;

        public String id() {
            return "submersible_pump_dead_headed";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pump = pumpInWater(ctx, l, 2, 1);
            l.feed(ctx.at(2, 1, 1), pump.front.left());
        }

        public void measure(TestContext ctx) {
            double p = pump.outlet.P();
            ctx.check("pump dead-headed: its outlet stops at the shutoff pressure", p > 2.7e5 && p < 3.05e5, String.format("%.3f bar, %s", p / 1e5, pump.getWaila()));
            ctx.check("pump dead-headed: no flow", pump.rate < 2, String.format("%.1f mB/s", pump.rate));
        }
    }

    /** Out of water a pump doesn't run; a signal on its front sets its speed (0 V: stopped, half: half power). */
    static final class PumpWaterAndSignal implements TestCase {
        mods.eln.oil.SubmersiblePumpElement dry, stopped, half;

        public String id() {
            return "submersible_pump_water_and_signal";
        }

        public int width() {
            return 10;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            dry = pumpInWater(ctx, l, 1, 1);
            stopped = pumpInWater(ctx, l, 4, 1);
            half = pumpInWater(ctx, l, 7, 1);
            // (the signals first: the power sources are then put where they don't touch them)
            signal(ctx, l, stopped, 0);
            signal(ctx, l, half, (float) (mods.eln.Eln.SVU / 2));
            for (var p : new mods.eln.oil.SubmersiblePumpElement[]{dry, stopped, half}) {
                l.feed(p.node.coordonate.pos(), p.front.left());
            }
            // the dry one: its water gone (as if drained)
            BlockPos d = dry.node.coordonate.pos();
            ctx.level().setBlock(d, ctx.level().getBlockState(d).setValue(mods.eln.node.transparent.TransparentNodeBlock.WATERLOGGED, false), 2);
        }

        static void signal(TestContext ctx, Wp10aCases.Layout l, mods.eln.oil.SubmersiblePumpElement p, float u) {
            BlockPos cp = p.node.coordonate.pos().west();
            var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) l.six(Wp10bCases.SIGNAL_SOURCE, cp);
            s.networkUnserialize(ctx.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(u);
            }));
        }

        public void measure(TestContext ctx) {
            ctx.line("pumps: dry " + dry.getWaila() + "; stopped " + stopped.getWaila() + "; half " + half.getWaila());
            ctx.check("pump: out of water it doesn't run", !dry.submerged && dry.outlet.mLiquid() == 0 && dry.motor.getP() < 1, dry.getWaila().toString());
            ctx.check("pump: a 0 V signal stops it", stopped.outlet.mLiquid() == 0 && stopped.motor.getP() < 1, stopped.getWaila().toString());
            ctx.check("pump: half the signal, half the power, and it pumps", Math.abs(half.speed - 0.5) < 0.05
                && Math.abs(half.motor.getP() - 50) < 10 && half.outlet.mLiquid() > 0, half.getWaila().toString());
        }
    }

    /** A water bucket fills a boiler by hand (Baughn: buckets work, pipes only automate it); the bucket comes back empty. */
    static final class BoilerBucket implements TestCase {
        int before, after;
        boolean took, emptied;

        public String id() {
            return "boiler_takes_a_bucket";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            var boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            boiler.fromTanks(2000, 0);
            boiler.showTanks();
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
            fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            before = boiler.tanks.water();
            took = boiler.onBlockActivated(fp, mods.eln.misc.Direction.XP, 0.5f, 0.5f, 0.5f);
            after = boiler.tanks.water();
            emptied = fp.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }

        public void measure(TestContext ctx) {
            ctx.check("boiler bucket: taken, 1000 mB more water", took && after - before == 1000, before + " -> " + after);
            ctx.check("boiler bucket: the bucket comes back empty", emptied, "");
        }
    }

    /** The guide advancements (Baughn's hints) all load, under the rubber root. */
    static final class GuideAdvancements implements TestCase {
        public String id() {
            return "guide_advancements_load";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var adv = ctx.level().getServer().getAdvancements();
            String[] keys = {"root", "copper_cable", "heat_furnace", "boiler", "safety_valve", "steam_turbine", "big_iron", "machine_block",
                "macerator", "electrical_furnace", "auto_miner", "cable_kit", "chisel", "lead", "battery", "solar", "multimeter", "converter",
                "fuse", "lamp"};
            java.util.List<String> missing = new java.util.ArrayList<>();
            for (String k : keys)
                if (adv.get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "guide/" + k)) == null) missing.add(k);
            ctx.check("guide advancements: all " + keys.length + " load", missing.isEmpty(), missing.toString());
            var root = adv.get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "guide/copper_cable"));
            ctx.check("guide advancements: under the rubber root", root != null && root.value().parent().map(Object::toString).orElse("").equals("eln:guide/root"),
                root == null ? "-" : String.valueOf(root.value().parent()));
        }
    }

    static final class BoilerPipeTurbine implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        mods.eln.oil.ElectricHeaterElement load;
        mods.eln.sim.IProcess fire;
        BlockPos first;

        public String id() {
            return "pipe_boiler_to_turbine";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            load = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            turbine.front = mods.eln.misc.Direction.XN;
            turbine.reconnect();
            load.front = mods.eln.misc.Direction.ZN;
            load.reconnect();
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            first = ctx.at(1, 2, 1);
            lay(ctx, first, 0, 8, 0, 8, 8, 8, 8, 16, 8, 8);           // up out of the boiler, then east
            lay(ctx, ctx.at(2, 2, 1), 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, ctx.at(3, 2, 1), 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, ctx.at(4, 2, 1), 0, 0, 8, 8, 8, 8, 8, 8, 0, 8);   // east in, then down into the turbine
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(2500 * t));
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            double p = turbine.source.getEffectiveP();
            VoxelNode n = node(ctx, first);
            var net = mods.eln.fluid.FluidWorld.networkOf(boiler.vessel);
            ctx.line("pipe: boiler " + boiler.getWaila() + ", turbine " + turbine.getWaila() + ", network " + (net == null ? 0 : net.nodes.size()) + " nodes");
            // (the 4 pipe blocks are one run: one lumped node; with the boiler, its feed inlet and the turbine: 4 nodes)
            ctx.check("pipe: boiler, the pipe run and turbine are one network", net != null && n != null && net == mods.eln.fluid.FluidWorld.networkOf(turbine.inlet)
                && net == mods.eln.fluid.FluidWorld.networkOf(((PipePart) n.parts.get(0)).fluid) && net.nodes.size() == 4, net == null ? "none" : net.nodes.size() + " nodes");
            ctx.check("pipe: the turbine runs off the piped steam", p > 500 && p <= 1050, String.format("%.0f W", p));
            ctx.check("pipe: steam is in the pipe", n != null && ((PipePart) n.parts.get(0)).fluid.mVapour() > 0, "");
        }
    }

    /**
     * The router: boiler and turbine with a stone wall between them; a route from the boiler's top port to the
     * turbine's inlet finds its way over the wall and is laid; the turbine runs.
     */
    static final class RoutedOverAWall implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        mods.eln.sim.IProcess fire;
        String err;
        int length;

        public String id() {
            return "pipe_routed_over_a_wall";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            var load = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            turbine.front = mods.eln.misc.Direction.XN;
            turbine.reconnect();
            load.front = mods.eln.misc.Direction.ZN;
            load.reconnect();
            for (int y = 1; y <= 2; y++) for (int z = 0; z <= 2; z++) ctx.level().setBlock(ctx.at(2, y, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            var level = ctx.level();
            var a = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(1, 1, 1), net.minecraft.core.Direction.UP);
            var b = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(4, 1, 1), net.minecraft.core.Direction.UP);
            if (a == null || b == null) {
                err = "ports not found: " + a + " " + b;
                return;
            }
            var r = mods.eln.node.voxel.PipeRouter.route(mods.eln.node.voxel.PipePlacer.space(level), 4, a, List.of(), b);
            if (r.error != null) {
                err = r.error;
                return;
            }
            length = r.length();
            err = mods.eln.node.voxel.PipePlacer.lay(level, 0, r.points, null);
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(2500 * t));
        }

        public void measure(TestContext ctx) {
            if (fire != null) mods.eln.Eln.simulator.removeSlowProcess(fire);
            double p = turbine.source.getEffectiveP();
            ctx.line("pipe: routed " + length / 16.0 + " blocks; boiler " + boiler.getWaila() + ", turbine " + turbine.getWaila());
            ctx.check("pipe: a route over the wall is found and laid", err == null, "" + err);
            ctx.check("pipe: it goes over the wall (longer than straight across)", length > 3 * 16, length + " voxels");
            ctx.check("pipe: the turbine runs off the routed pipe", p > 400 && p <= 1050, String.format("%.0f W", p));
        }
    }

    /**
     * Joining a run mid-way (Baughn): a straight run lies across the turbine's top (into its inlet from above, through
     * a pipe that then turns along x); a route from the boiler's top port joins that run in the middle of a block. The
     * run is cut there into two pieces, the three meet in a junction, and boiler, run and turbine are one network.
     */
    static final class JoinMidRun implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        String err;
        BlockPos joinBlock;
        int partsAtJoin;

        public String id() {
            return "pipe_join_mid_run";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 5, 1);
            // the run: down into the turbine at x 5, coming along x from x 3 (open end inside block 3)
            lay(ctx, ctx.at(5, 2, 1), 0, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            lay(ctx, ctx.at(4, 2, 1), 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, ctx.at(3, 2, 1), 0, 4, 8, 8, 16, 8, 8);
            joinBlock = ctx.at(4, 2, 1);
            var level = ctx.level();
            var a = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(1, 1, 1), net.minecraft.core.Direction.UP);
            long[] join = {joinBlock.getX() * 16L + 8, joinBlock.getY() * 16L + 8, joinBlock.getZ() * 16L + 8};
            err = mods.eln.node.voxel.PipePlacer.routeAndJoinForTests(level, 0, a, join);
            VoxelNode n = node(ctx, joinBlock);
            partsAtJoin = n == null ? 0 : n.parts.size();
        }

        public void measure(TestContext ctx) {
            var net = mods.eln.fluid.FluidWorld.networkOf(boiler.vessel);
            ctx.check("pipe: the route joins the run", err == null, "" + err);
            ctx.check("pipe: the run is cut in two at the junction, plus the new pipe: 3 pieces", partsAtJoin == 3, partsAtJoin + " pieces");
            ctx.check("pipe: boiler, run and turbine are one network", net != null && net == mods.eln.fluid.FluidWorld.networkOf(turbine.inlet), "");
        }
    }

    /** Picking up a dropped kit tops up the kit already in the inventory (Baughn: stacking, reimplemented). */
    static final class KitPickupMerges implements TestCase {
        int held, inv;
        boolean gone;

        public String id() {
            return "pipe_kit_pickup_merges";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext ctx) {
            var p = Wp10bCases.player(ctx);
            p.getInventory().clearContent();
            p.getInventory().add(PipeKitItem.kit(0, 16));
            var ent = new net.minecraft.world.entity.item.ItemEntity(ctx.level(), p.getX(), p.getY(), p.getZ(), PipeKitItem.kit(0, 40));
            ent.setNoPickUpDelay();
            ctx.level().addFreshEntity(ent);
            ent.playerTouch(p);
            gone = ent.isRemoved();
            int kits = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                var s = p.getInventory().getItem(i);
                if (s.getItem() instanceof PipeKitItem) {
                    kits++;
                    held = PipeKitItem.length(s);
                }
            }
            inv = kits;
            p.getInventory().clearContent();
        }

        public void measure(TestContext ctx) {
            ctx.check("pipe: the dropped kit merged into the one held (16 + 40)", gone && inv == 1 && held == 56, "gone " + gone + ", kits " + inv + ", length " + held);
        }
    }

    /** A player fits a valve the real way (the server's use-item-on-block path, through the pipe block's use). */
    static final class ValveByPlayer implements TestCase {
        BlockPos at;
        String result;

        public String id() {
            return "pipe_valve_by_player";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext ctx) {
            at = ctx.at(1, 1, 1);
            lay(ctx, at, 0, 0, 8, 8, 16, 8, 8);
            var p = Wp10bCases.player(ctx);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var stack = new ItemStack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get());
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.62),
                net.minecraft.core.Direction.SOUTH, at, false);
            var r = p.gameMode.useItemOn(p, ctx.level(), stack, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            result = r.toString();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        public void measure(TestContext ctx) {
            VoxelNode n = node(ctx, at);
            boolean valve = false;
            if (n != null) for (VoxelPart q : n.parts) if (q instanceof mods.eln.node.voxel.ValvePart) valve = true;
            ctx.check("pipe: a player's right-click with a valve fits it", valve, result);
        }
    }

    /** A pipe adapter: water put in through the fluid capability on its face goes into the run behind it, and comes
     *  back out the same way. */
    static final class Adapter implements TestCase {
        BlockPos at;
        int put, took;
        double inRunAfterPut = -1;

        public String id() {
            return "pipe_adapter_capability";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            at = ctx.at(2, 1, 1);
            ctx.level().setBlock(ctx.at(1, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            var item = new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get());
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ctx.at(1, 1, 1)),
                net.minecraft.core.Direction.EAST, ctx.at(1, 1, 1), false);
            item.useOn(new net.minecraft.world.item.context.UseOnContext(ctx.level(), null, net.minecraft.world.InteractionHand.MAIN_HAND, item, hit));
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.WEST);
            if (cap == null) return;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                put = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 150, tx);
                tx.commit();
            }
        }

        public void measure(TestContext ctx) {
            VoxelNode n = node(ctx, at);
            ctx.check("pipe: an adapter goes on the stone's face, with its stub", n != null && n.adapterOn(net.minecraft.core.Direction.WEST.get3DDataValue()) != null, "");
            if (n == null) return;
            PipePart stub = null;
            for (VoxelPart p : n.parts) if (p instanceof PipePart pp) stub = pp;
            double run = stub == null ? 0 : mods.eln.fluid.FluidWorld.nodeFor(stub.fluid).mWater / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
            ctx.check("pipe: 150 mB of water went in through the capability (the stub holds ~190)", put == 150, put + " mB");
            ctx.check("pipe: it is in the run (some evaporates into its air)", run > 145 && run < 155, run + " mB");
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.WEST);
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                took = cap.extract(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 100, tx);
                tx.commit();
            }
            ctx.check("pipe: water comes back out through it", took == 100, took + " mB");
            var other = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.UP);
            ctx.check("pipe: only on the adapter's face", other == null, "" + other);

            // a cache held by another mod's pipe sees the adapter go and come back on the same block
            int west = net.minecraft.core.Direction.WEST.get3DDataValue();
            var cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, ctx.level(), at, net.minecraft.core.Direction.WEST);
            boolean held = cache.getCapability() != null;
            int i = n.parts.indexOf(n.adapterOn(west));
            n.removePart(i, false);
            boolean gone = cache.getCapability() == null;
            String err = n.addPart(new AdapterPart(west));
            boolean back = cache.getCapability() != null;
            ctx.check("pipe: a held capability cache follows the adapter out and back in", held && gone && back && err == null,
                "held " + held + ", gone " + gone + ", back " + back + (err == null ? "" : ", " + err));
        }
    }

    /** A valve between a pressurised run and an empty one: shut, it holds; open, the pressures even out. */
    static final class Valve implements TestCase {
        BlockPos a, b, c;
        String err;
        double heldHigh, heldLow;

        public String id() {
            return "pipe_valve";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            a = ctx.at(1, 1, 1);
            b = ctx.at(2, 1, 1);
            c = ctx.at(3, 1, 1);
            for (BlockPos p : new BlockPos[]{a, b, c}) lay(ctx, p, 0, 0, 8, 8, 16, 8, 8);
            err = node(ctx, b).addValve();
            if (err != null) return;
            for (VoxelPart p : node(ctx, b).parts) if (p instanceof mods.eln.node.voxel.ValvePart v) v.open = false;
            ((PipePart) node(ctx, a).parts.get(0)).fluid.fill(0, 5e5, 293.15);
            mods.eln.fluid.FluidWorld.changed(node(ctx, a));
        }

        public void measure(TestContext ctx) {
            ctx.check("pipe: a valve fits in a straight pipe", err == null, "" + err);
            if (err != null) return;
            double pa = ((PipePart) node(ctx, a).parts.get(0)).fluid.P(), pc = ((PipePart) node(ctx, c).parts.get(0)).fluid.P();
            ctx.check("pipe: shut, it holds (5 bar on one side, 1 on the other)", pa > 3e5 && pc < 1.5e5, String.format("%.2f / %.2f bar", pa / 1e5, pc / 1e5));
            var parts = node(ctx, b).parts;
            ctx.check("pipe: the pipe was cut in two around the valve", parts.size() == 3, parts.size() + " parts");
        }
    }

    /** An iron run pressed past its burst rating (40 bar) bursts: the weak piece goes, scorched, and the run vents to
     *  the air (Baughn: a burst vents; a player's break seals). Breaking the burst piece drops nothing. */
    static final class Burst implements TestCase {
        BlockPos a, b;

        public String id() {
            return "pipe_burst_vents";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            a = ctx.at(1, 1, 1);
            b = ctx.at(2, 1, 1);
            lay(ctx, a, 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, b, 0, 0, 8, 8, 16, 8, 8);
            for (BlockPos p : new BlockPos[]{a, b}) {
                VoxelNode n = node(ctx, p);
                ((PipePart) n.parts.get(0)).fluid.fill(0, 50e5, 293.15);
                mods.eln.fluid.FluidWorld.changed(n);
            }
        }

        public void measure(TestContext ctx) {
            PipePart pa = (PipePart) node(ctx, a).parts.get(0), pb = (PipePart) node(ctx, b).parts.get(0);
            ctx.check("pipe: one piece of the run burst", pa.burst ^ pb.burst, pa.burst + " " + pb.burst);
            ctx.check("pipe: the run vented to the air", pa.fluid.P() < 2e5, String.format("%.2f bar", pa.fluid.P() / 1e5));
            VoxelNode n = node(ctx, pa.burst ? a : b);
            n.removePart(0, true);
            var items = ctx.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(a).inflate(3));
            ctx.check("pipe: a burst piece drops nothing", items.isEmpty(), items.size() + " items");
        }
    }

    /** Pipe kits in the crafting grid: plates make a block of pipe; kits merge; a kit splits in halves. */
    static final class KitCrafting implements TestCase {
        public String id() {
            return "pipe_kit_crafting";
        }

        public int width() {
            return 2;
        }

        public int ticks() {
            return 1;
        }

        static ItemStack craft(TestContext ctx, int w, int h, ItemStack... items) {
            var in = net.minecraft.world.item.crafting.CraftingInput.of(w, h, java.util.Arrays.asList(items));
            var level = ctx.level();
            var r = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, level);
            return r.map(x -> x.value().assemble(in)).orElse(ItemStack.EMPTY);
        }

        static java.util.List<ItemStack> remaining(TestContext ctx, int w, int h, ItemStack... items) {
            var in = net.minecraft.world.item.crafting.CraftingInput.of(w, h, java.util.Arrays.asList(items));
            var level = ctx.level();
            var r = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, level);
            return r.map(x -> (java.util.List<ItemStack>) x.value().getRemainingItems(in)).orElse(java.util.List.of());
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            ItemStack plate = mods.eln.Eln.findItemStack("Iron Plate");
            ItemStack k = craft(ctx, 2, 1, plate.copy(), plate.copy());
            ctx.check("pipe: 2 iron plates make an iron kit of one block (16 voxels)", k.getItem() == mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()
                && PipeKitItem.length(k) == 16, k + " " + PipeKitItem.length(k));
            ItemStack m = craft(ctx, 2, 1, PipeKitItem.kit(0, 16), PipeKitItem.kit(0, 40));
            ctx.check("pipe: two iron kits merge (16 + 40)", PipeKitItem.length(m) == 56, "" + PipeKitItem.length(m));
            ItemStack mix = craft(ctx, 2, 1, PipeKitItem.kit(0, 16), PipeKitItem.kit(1, 16));
            ctx.check("pipe: iron and alloy kits don't merge", mix.isEmpty() || !(mix.getItem() instanceof PipeKitItem), "" + mix);
            ItemStack half = craft(ctx, 1, 1, PipeKitItem.kit(1, 33));
            var rest = remaining(ctx, 1, 1, PipeKitItem.kit(1, 33));
            ItemStack half16 = craft(ctx, 3, 3, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, PipeKitItem.kit(0, 16),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            ctx.check("pipe: a one-block kit splits too (in a 3x3 grid: 8)", PipeKitItem.length(half16) == 8, "" + PipeKitItem.length(half16));
            ctx.check("pipe: a kit splits in halves (33: 16 out, 17 stays)", PipeKitItem.length(half) == 16 && rest.size() == 1 && PipeKitItem.length(rest.get(0)) == 17,
                PipeKitItem.length(half) + " / " + rest);
        }
    }

    /** Two pipes crossing one block at different heights; a third that would cut through one is refused; taking one out
     *  leaves the other and drops its length as a kit. */
    static final class TwoPipesOneBlock implements TestCase {
        BlockPos at;
        String refused;
        boolean emptyAfterFirst, emptyAfterSecond;
        int partsLeft, dropped;

        public String id() {
            return "pipe_two_in_one_block";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext ctx) {
            at = ctx.at(1, 1, 1);
            lay(ctx, at, 0, 0, 4, 8, 16, 4, 8);                       // along x, low
            lay(ctx, at, 1, 8, 12, 0, 8, 12, 16);                     // along z, high (alloy)
            refused = PipeKitItem.place(ctx.level(), at, new PipePart(0, new int[]{8, 4, 0, 8, 4, 16}), null);
            VoxelNode n = node(ctx, at);
            int low = n.partAt(0.5, 4 / 16.0, 0.5);
            emptyAfterFirst = n.removePart(low, true);
            partsLeft = n.parts.size();
        }

        public void measure(TestContext ctx) {
            var items = ctx.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(2));
            for (var it : items) dropped += PipeKitItem.length(it.getItem());
            ctx.check("pipe: a crossing pipe at another height fits; one through the first is refused", refused != null, "" + refused);
            ctx.check("pipe: taking the low pipe out leaves the high one", !emptyAfterFirst && partsLeft == 1, partsLeft + " left");
            ctx.check("pipe: it drops as a kit of its length (16 voxels)", dropped == 16, dropped + " voxels dropped");
            for (var it : items) it.discard();
        }
    }

    /**
     * The live route preview of a pipe kit (Baughn: pipes didn't show while picking waypoints; AdventurAgent saw none
     * drawn): from the top of an upright pipe to an air cell three blocks on, the preview has the pipe's boxes.
     */
    static final class PipeRoutePreview implements TestCase {
        String result = "not run";
        boolean ok;

        public String name() {
            return "pipe kit: the live route preview has the pipe";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext ctx) {
            var level = ctx.level();
            String err = mods.eln.node.voxel.PipeKitItem.place(level, ctx.at(1, 1, 1), new mods.eln.node.voxel.PipePart(0, new int[]{8, 0, 8, 8, 16, 8}), null);
            var a = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(1, 1, 1), net.minecraft.core.Direction.UP);
            if (err != null || a == null) {
                result = "setup: " + err + " port " + a;
                return;
            }
            BlockPos at = ctx.at(4, 2, 1);
            var end = new mods.eln.node.voxel.PipeRouter.End(at.getX() * 16L + 8, at.getY() * 16L + 8, at.getZ() * 16L + 8, -1);
            try {
                var ans = mods.eln.node.voxel.KitPreview.pipeAnswerForTests(level, 0, a, end);
                result = ans.ok() + " " + ans.boxes().size() + " boxes: " + ans.message();
                ok = ans.ok() && ans.boxes().size() >= 2;
            } catch (RuntimeException e) {
                result = "threw " + e;
            }
        }

        public void measure(TestContext ctx) {
            ctx.check("pipe preview: a route with boxes", ok, result);
        }
    }

    /**
     * A kit's length isn't damage (a player's report via Baughn: an Occultism blacksmith familiar repaired cable kits
     * full): kits can't be damaged or repaired, a kit from before (its length as damage) still reads right, and changing
     * it moves the length into the kit's data.
     */
    static final class KitLengthNotDamage implements TestCase {
        String result = "";
        boolean ok;

        public String name() {
            return "kits keep their length in data, not damage";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
            ItemStack pipe = PipeKitItem.kit(0, 100);
            ItemStack cable = mods.eln.node.voxel.CableKitItem.kit(1, 40);
            // a kit saved before: damage 1000 of 1024 = 24 voxels
            ItemStack old = new ItemStack(mods.eln.registry.ElnRegistry.CABLE_KITS.get(1).get());
            old.set(net.minecraft.core.component.DataComponents.DAMAGE, 1000);
            int oldLen = mods.eln.node.voxel.KitItem.length(old);
            mods.eln.node.voxel.KitItem.setLength(old, oldLen + 16);
            result = "pipe " + PipeKitItem.length(pipe) + " damageable " + pipe.isDamageableItem() + ", cable " + mods.eln.node.voxel.KitItem.length(cable)
                + " damageable " + cable.isDamageableItem() + ", old " + oldLen + " -> " + mods.eln.node.voxel.KitItem.length(old)
                + " damage left " + old.has(net.minecraft.core.component.DataComponents.DAMAGE);
            ok = PipeKitItem.length(pipe) == 100 && !pipe.isDamageableItem() && mods.eln.node.voxel.KitItem.length(cable) == 40 && !cable.isDamageableItem()
                && oldLen == 24 && mods.eln.node.voxel.KitItem.length(old) == 40 && !old.has(net.minecraft.core.component.DataComponents.DAMAGE);
        }

        public void measure(TestContext ctx) {
            ctx.check("kits: length in data, not damage; old kits read right", ok, result);
        }
    }
}
