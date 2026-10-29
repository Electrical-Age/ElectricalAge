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
        cases.add(new HighQualityPlates());
        cases.add(new Sensors());
        cases.add(new DeviceGoneMidStep());
        cases.add(new JadeAimedPipe());
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
        cases.add(new ThrottleAndControlValves());
        cases.add(new Burst());
        cases.add(new Valve());
        cases.add(new Adapter());
        cases.add(new ValveByPlayer());
        cases.add(new KitPickupMerges());
        cases.add(new PipeRoutePreview());
        cases.add(new KitLengthNotDamage());
        cases.add(new ValveMeters());
        cases.add(new LiquidStaysIncompressible());
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
            ctx.check("pump dead-headed: its outlet stops at the shutoff pressure", p > 0.9 * ((mods.eln.oil.SubmersiblePumpDescriptor) pump.transparentNodeDescriptor).shutoffPa && p < 1.02 * ((mods.eln.oil.SubmersiblePumpDescriptor) pump.transparentNodeDescriptor).shutoffPa, String.format("%.3f bar, %s", p / 1e5, pump.getWaila()));
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

    /**
     * Two pipes in one block, 5 bar in one and 2 in the other (Baughn 2026-10-28: Jade showed one of them, not the one
     * looked at): each part's Jade lines are its own run's.
     */
    static final class JadeAimedPipe implements TestCase {
        BlockPos b;

        public String id() {
            return "pipe_jade_shows_the_aimed_pipe";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext ctx) {
            b = ctx.at(1, 1, 1);
            lay(ctx, b, 0, 0, 4, 8, 16, 4, 8);
            lay(ctx, b, 0, 8, 12, 0, 8, 12, 16);
            var n = node(ctx, b);
            ((PipePart) n.parts.get(0)).fluid.fill(0, 5e5, 293.15);
            ((PipePart) n.parts.get(1)).fluid.fill(0, 2e5, 293.15);
            mods.eln.fluid.FluidWorld.changed(n);
        }

        public void measure(TestContext ctx) {
            var n = node(ctx, b);
            String a = String.valueOf(n.getWaila(false, 0).get("Pressure")), c = String.valueOf(n.getWaila(false, 1).get("Pressure"));
            ctx.line("jade aimed pipe: part 0 " + a + "; part 1 " + c);
            ctx.check("Jade on each of two pipes in a block shows that pipe's run", a.startsWith("5.0") && c.startsWith("2.0"), a + " / " + c);
            // the meters read the part clicked, too (they read the block's first)
            String m0 = n.multiMeterString(0), m1 = n.multiMeterString(1);
            ctx.check("the multimeter on each pipe reads that pipe's run", m0.startsWith("5.0") && m1.startsWith("2.0"), m0 + " / " + m1);
            var a0 = n.aimed(0.5, 4.5 / 16, 8.5 / 16);
            ctx.check("aimed: the crosshair on the low pipe is part 0", a0.part() == 0 && a0.element() < 0, "" + a0);
            // two cables in a block, one signal, one LV (not touching): each shows only its own
            var cb = ctx.at(1, 1, 0);
            PipeKitItem.place(ctx.level(), cb, new mods.eln.node.voxel.CablePart(0, 0, new int[]{0, 0, 3, 16, 0, 3}), null);
            PipeKitItem.place(ctx.level(), cb, new mods.eln.node.voxel.CablePart(1, 0, new int[]{0, 0, 12, 16, 0, 12}), null);
            var cn = node(ctx, cb);
            var w0 = cn.getWaila(false, 0);
            var w1 = cn.getWaila(false, 1);
            ctx.line("jade aimed cable: part 0 " + w0 + "; part 1 " + w1 + "; none " + cn.getWaila(false, -1));
            ctx.check("the multimeter on each cable reads that cable", !cn.multiMeterString(0).equals(cn.multiMeterString(1)), cn.multiMeterString(0) + " / " + cn.multiMeterString(1));
            ctx.check("Jade on each of two cables in a block shows only that cable", w0.size() == 1 && w1.size() == 1
                && !String.valueOf(w0.get("Cable")).equals(String.valueOf(w1.get("Cable"))) && cn.getWaila(false, -1).size() == 2, w0 + " / " + w1);
        }
    }

    /**
     * A device that takes another out of the fluid world during its step (a steam turbine's casing burst and its
     * explosion broke a neighbour: the lab server crashed on a ConcurrentModificationException in FluidWorld.process):
     * the step carries on, and the one taken out isn't stepped after it.
     */
    static final class DeviceGoneMidStep implements TestCase {
        final int[] stepped = new int[2];
        mods.eln.sim.fluid.FluidDevice a, b;
        boolean crashed;
        mods.eln.sim.IProcess guard;

        public String id() {
            return "fluid_device_gone_mid_step";
        }

        public int ticks() {
            return 20;
        }

        mods.eln.sim.fluid.FluidDevice device(int i, java.util.function.Supplier<mods.eln.sim.fluid.FluidDevice> other) {
            return new mods.eln.sim.fluid.FluidDevice() {
                public Object fluidLevel() {
                    return "test";
                }

                public java.util.List<FluidPort> fluidPorts() {
                    return java.util.List.of();
                }

                public void beforeFluidStep(double dt) {
                    stepped[i]++;
                    if (i == 0 && other.get() != null) {
                        mods.eln.fluid.FluidWorld.remove(other.get());   // (as an explosion breaking the neighbour would)
                        mods.eln.fluid.FluidWorld.remove(this);
                    }
                }

                public void afterFluidStep(double dt) {
                }
            };
        }

        public void build(TestContext ctx) {
            a = device(0, () -> b);
            b = device(1, () -> null);
            mods.eln.fluid.FluidWorld.add(a);
            mods.eln.fluid.FluidWorld.add(b);
        }

        public void measure(TestContext ctx) {
            mods.eln.fluid.FluidWorld.remove(a);
            mods.eln.fluid.FluidWorld.remove(b);
            ctx.check("fluid world: a device taken out mid-step doesn't crash the step, and isn't stepped after",
                stepped[0] == 1 && stepped[1] == 0, stepped[0] + " / " + stepped[1]);
        }
    }

    /**
     * Pipe sensors (Baughn 2026-10-27): a run of three blocks holding hot steam, a pressure sensor on the first block and a
     * temperature sensor on the last, each with a signal cable in its block; a second sensor in a block is refused. Their signals are the reading over full scale (25 bar, 200 C), and an
     * empty-hand right-click steps the full scale.
     */
    static final class Sensors implements TestCase {
        BlockPos a, b, c;
        String errP, errT, second;

        public String id() {
            return "pipe_pressure_and_temperature_sensors";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 100;
        }

        static mods.eln.node.voxel.PipeSensorPart sensor(TestContext ctx, BlockPos p, int sensing) {
            for (VoxelPart q : node(ctx, p).parts) if (q instanceof mods.eln.node.voxel.PipeSensorPart s && s.sensing == sensing) return s;
            return null;
        }

        public void build(TestContext ctx) {
            a = ctx.at(1, 1, 1);
            b = ctx.at(2, 1, 1);
            c = ctx.at(3, 1, 1);
            for (BlockPos p : new BlockPos[]{a, b, c}) lay(ctx, p, 0, 0, 8, 8, 16, 8, 8);
            errP = node(ctx, a).addSensorNear(0.5, 0.5, 0.5, mods.eln.node.voxel.PipeSensorPart.PRESSURE);
            errT = node(ctx, c).addSensorNear(0.5, 0.5, 0.5, mods.eln.node.voxel.PipeSensorPart.TEMPERATURE);
            second = node(ctx, a).addSensorNear(0.25, 0.5, 0.5, mods.eln.node.voxel.PipeSensorPart.TEMPERATURE);
            // a signal cable along the floor of each block: the sensor's output joins it
            for (BlockPos p : new BlockPos[]{a, c}) {
                String ce = PipeKitItem.place(ctx.level(), p, new mods.eln.node.voxel.CablePart(0, 0, new int[]{0, 0, 3, 16, 0, 3}), null);
                if (ce != null) errP = "cable: " + ce;
            }
            for (BlockPos p : new BlockPos[]{a, b, c}) {
                var f = ((PipePart) node(ctx, p).parts.get(0)).fluid;
                f.fill(0, 1, 423.15);
                mods.eln.sim.fluid.EaUnits.addWater(f, 6e5 * f.volume / (461.5 * 423.15), 423.15, true);   // (~6 bar of vapour at 150 C)
                mods.eln.fluid.FluidWorld.changed(node(ctx, p));
            }
        }

        public void measure(TestContext ctx) {
            ctx.check("pipe sensors fit on a straight pipe", errP == null && errT == null, errP + " / " + errT);
            if (errP != null || errT != null) return;
            ctx.check("a second sensor in a block is refused", second != null, "" + second);
            var ps = sensor(ctx, a, mods.eln.node.voxel.PipeSensorPart.PRESSURE);
            var ts = sensor(ctx, c, mods.eln.node.voxel.PipeSensorPart.TEMPERATURE);
            var n = mods.eln.fluid.FluidWorld.nodeFor(((PipePart) node(ctx, b).parts.get(0)).fluid);
            ctx.line(String.format("pipe sensors: run %.2f bar %.0f C; pressure sensor %.2f bar -> %.2f V; temperature sensor %.0f C -> %.2f V; %s",
                n.P() / 1e5, n.T() - 273.15, ps.reading, ps.out.getU(), ts.reading, ts.out.getU(), node(ctx, a).getWaila(false)));
            ctx.check("pressure sensor reads the run (absolute)", Math.abs(ps.reading - n.P() / 1e5) < 0.05 && ps.reading > 2, String.format("%.2f", ps.reading));
            ctx.check("pressure sensor: its signal is the reading over 25 bar", Math.abs(ps.out.getU() - ps.reading / 25 * 50) < 0.5, String.format("%.2f V", ps.out.getU()));
            ctx.check("temperature sensor reads the run", Math.abs(ts.reading - (n.T() - 273.15)) < 1 && ts.reading > 50, String.format("%.0f", ts.reading));
            ctx.check("temperature sensor: its signal is the reading over 200 C (50 V over it)", Math.abs(ts.out.getU() - Math.min(1, ts.reading / 200) * 50) < 0.5, String.format("%.2f V", ts.out.getU()));
            // a cable kit clicked on the sensor lays to the floor under it (AdventurAgent: "not a place for a cable")
            int[] db = ps.boxes().get(0);
            var end = mods.eln.node.voxel.CablePlacer.endAtForTests(ctx.level(), a, net.minecraft.core.Direction.UP,
                new net.minecraft.world.phys.Vec3(a.getX() + (db[0] + db[3]) / 32.0, a.getY() + (db[1] + db[4]) / 32.0, a.getZ() + (db[2] + db[5]) / 32.0), true, new long[1][]);
            ctx.check("a cable ends on the floor under a pipe sensor", end != null && end.y() == a.getY() * 16L && end.attach() == 0, "" + end);
            double before = ps.scale;
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
            int idx = node(ctx, a).parts.indexOf(ps);
            int[] bx = ps.boxes().get(0);
            node(ctx, a).onBlockActivated(player, mods.eln.misc.Direction.YP, (bx[0] + bx[3]) / 32f, (bx[1] + bx[4]) / 32f, (bx[2] + bx[5]) / 32f);
            ctx.check("a right-click steps the full scale (25 -> 50 bar)", before == 25 && ps.scale == 50, ps.scale + " (part " + idx + ")");
        }
    }

    /**
     * EA's recipes take plates by eln:high_quality_plates/<metal> (Baughn 2026-10-27: hammered plates don't fit; a tag so
     * a modpack can add some): EA's plates are in it, the casing recipe takes it, an iron ingot isn't a plate, and pipe kits
     * are made of what the tag holds.
     */
    static final class HighQualityPlates implements TestCase {
        public String id() {
            return "recipes_take_high_quality_plates";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var iron = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("eln:iron_plate")));
            ctx.check("plates: EA's iron plate is a high-quality iron plate", iron.is(mods.eln.misc.OreDictTags.highQualityPlates("iron")), "");
            ItemStack[] grid = new ItemStack[9];
            java.util.Arrays.fill(grid, iron.copy());
            grid[4] = ItemStack.EMPTY;
            var casing = KitCrafting.craft(ctx, 3, 3, grid);
            ctx.line("plates: a ring of 8 EA iron plates makes " + casing);
            ctx.check("plates: a recipe that takes the tag accepts EA's plates", !casing.isEmpty(), "" + casing);
            var level = ctx.level();
            var recipe = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.parse("eln:casing")));
            ctx.check("plates: the casing recipe exists", recipe.isPresent(), "");
            ctx.check("plates: pipe kits take what the tag holds", mods.eln.node.voxel.PipeKitRecipes.plateTier(iron) == 0
                && mods.eln.node.voxel.PipeKitRecipes.plateTier(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)) == -1, "");
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
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(9600 * t));
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
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(9600 * t));
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

    /**
     * The two valves Baughn asked for (2026-10-26): three runs of three blocks, 5 bar on one end, a valve in the middle.
     * A throttle valve set to 0 holds; one set to 100% by its screen's packet (VoxelNode.PART_PACKET) passes; a control
     * valve with 50 V on its signal passes, and one with none holds.
     */
    static final class ThrottleAndControlValves implements TestCase {
        BlockPos[][] runs = new BlockPos[4][];
        String err = "";
        mods.eln.sim.mna.component.VoltageSource source;
        mods.eln.node.voxel.ValvePart control;

        public String id() {
            return "pipe_throttle_and_control_valves";
        }

        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        public int ticks() {
            return 300;
        }

        static mods.eln.node.voxel.ValvePart valve(TestContext ctx, BlockPos p) {
            for (VoxelPart q : node(ctx, p).parts) if (q instanceof mods.eln.node.voxel.ValvePart v) return v;
            return null;
        }

        public void build(TestContext ctx) {
            int[] modes = {mods.eln.node.voxel.ValvePart.THROTTLE, mods.eln.node.voxel.ValvePart.THROTTLE,
                mods.eln.node.voxel.ValvePart.CONTROL, mods.eln.node.voxel.ValvePart.CONTROL};
            for (int r = 0; r < 4; r++) {
                runs[r] = new BlockPos[]{ctx.at(1, 1, r), ctx.at(2, 1, r), ctx.at(3, 1, r)};
                for (BlockPos p : runs[r]) lay(ctx, p, 0, 0, 8, 8, 16, 8, 8);
                String e = node(ctx, runs[r][1]).addValve(8, 8, 8, modes[r]);
                if (e != null) err += "run " + r + ": " + e + "; ";
            }
            if (!err.isEmpty()) return;
            valve(ctx, runs[0][1]).setting = 0;
            // run 1: set open from its screen (the packet a client's slider sends)
            try {
                var bos = new java.io.ByteArrayOutputStream();
                var out = new java.io.DataOutputStream(bos);
                out.writeByte(mods.eln.node.voxel.VoxelNode.PART_PACKET);
                int idx = node(ctx, runs[1][1]).parts.indexOf(valve(ctx, runs[1][1]));
                valve(ctx, runs[1][1]).setting = 0;
                out.writeByte(idx);
                out.writeFloat(1f);
                node(ctx, runs[1][1]).networkUnserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bos.toByteArray())),
                    null);
            } catch (java.io.IOException e) {
                err += e;
            }
            // run 2: 50 V on the control valve's signal
            control = valve(ctx, runs[2][1]);
            source = new mods.eln.sim.mna.component.VoltageSource("test", control.signal, null).setU(mods.eln.Eln.SVU);
            mods.eln.Eln.simulator.addElectricalComponent(source);
            for (int r = 0; r < 4; r++) {
                ((PipePart) node(ctx, runs[r][0]).parts.get(0)).fluid.fill(0, 5e5, 293.15);
                mods.eln.fluid.FluidWorld.changed(node(ctx, runs[r][0]));
            }
        }

        double[] p(TestContext ctx, int r) {
            return new double[]{((PipePart) node(ctx, runs[r][0]).parts.get(0)).fluid.P(), ((PipePart) node(ctx, runs[r][2]).parts.get(0)).fluid.P()};
        }

        public void measure(TestContext ctx) {
            if (source != null) mods.eln.Eln.simulator.removeElectricalComponent(source);
            ctx.check("pipe: throttle and control valves fit", err.isEmpty(), err);
            if (!err.isEmpty()) return;
            double[] a = p(ctx, 0), b = p(ctx, 1), c = p(ctx, 2), d = p(ctx, 3);
            ctx.line(String.format("valves: throttle 0%% %.2f/%.2f, throttle 100%% %.2f/%.2f, control 50 V %.2f/%.2f (%.0f%%), control 0 V %.2f/%.2f bar",
                a[0] / 1e5, a[1] / 1e5, b[0] / 1e5, b[1] / 1e5, c[0] / 1e5, c[1] / 1e5, control.opening() * 100, d[0] / 1e5, d[1] / 1e5));
            ctx.check("pipe: a throttle valve at 0% holds", a[0] > 3e5 && a[1] < 1.5e5, "");
            ctx.check("pipe: a throttle valve set to 100% by its screen passes", valve(ctx, runs[1][1]).setting == 1 && Math.abs(b[0] - b[1]) < 0.3e5, "");
            ctx.check("pipe: a control valve on 50 V is open and passes", control.opening() > 0.95 && Math.abs(c[0] - c[1]) < 0.3e5, "");
            ctx.check("pipe: a control valve without a signal holds", d[0] > 3e5 && d[1] < 1.5e5, "");
            // a cable kit clicked on a control valve lays to the floor under it (AdventurAgent: "not a place for a cable")
            var pos = runs[3][1];
            var e = mods.eln.node.voxel.CablePlacer.endAtForTests(ctx.level(), pos, net.minecraft.core.Direction.UP,
                new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 14.5 / 16, pos.getZ() + 0.5), true, new long[1][]);
            ctx.check("pipe: a cable ends on the floor under a control valve", e != null && e.y() == pos.getY() * 16L && e.attach() == 0, "" + e);
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
            ItemStack alloy = mods.eln.Eln.findItemStack("Alloy Plate");
            ItemStack k3 = craft(ctx, 3, 1, alloy.copy(), alloy.copy(), alloy.copy());
            ctx.check("pipe: 3 alloy plates make an alloy kit of three blocks (Baughn: 3 m per craft)", k3.getItem() == mods.eln.registry.ElnRegistry.PIPE_KIT_ALLOY.get()
                && PipeKitItem.length(k3) == 48, k3 + " " + PipeKitItem.length(k3));
            ItemStack m = craft(ctx, 2, 1, PipeKitItem.kit(0, 16), PipeKitItem.kit(0, 40));
            ctx.check("pipe: two iron kits merge (16 + 40)", PipeKitItem.length(m) == 56, "" + PipeKitItem.length(m));
            ItemStack mix = craft(ctx, 2, 1, PipeKitItem.kit(0, 16), PipeKitItem.kit(1, 16));
            ctx.check("pipe: iron and alloy kits don't merge", mix.isEmpty() || !(mix.getItem() instanceof PipeKitItem), "" + mix);
            // a lone kit gives a block's worth of plates back, as a cable kit gives a coil (Baughn: not halves)
            ItemStack back = craft(ctx, 1, 1, PipeKitItem.kit(1, 33));
            var rest = remaining(ctx, 1, 1, PipeKitItem.kit(1, 33));
            ctx.check("pipe: an alloy kit gives an alloy plate back and keeps the rest (33: 17 stays)", mods.eln.node.voxel.PipeKitRecipes.plateTier(back) == 1 && back.getCount() == 1
                && rest.size() == 1 && PipeKitItem.length(rest.get(0)) == 17, back + " / " + rest);
            ItemStack back16 = craft(ctx, 3, 3, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, PipeKitItem.kit(0, 16),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            var rest16 = remaining(ctx, 3, 3, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, PipeKitItem.kit(0, 16),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            ctx.check("pipe: a one-block iron kit gives 2 plates and is used up (in a 3x3 grid)", mods.eln.node.voxel.PipeKitRecipes.plateTier(back16) == 0 && back16.getCount() == 2
                && rest16.stream().allMatch(ItemStack::isEmpty), back16 + " / " + rest16);
            ItemStack short8 = craft(ctx, 1, 1, PipeKitItem.kit(0, 8));
            ctx.check("pipe: a kit under a block gives nothing back", short8.isEmpty() || !(mods.eln.node.voxel.PipeKitRecipes.plateTier(short8) >= 0), "" + short8);
            // kits top up (Baughn: pipe kits as cable kits, by the same code): 63 + 2 blocks give a full kit, 1 block stays
            ItemStack full = craft(ctx, 2, 1, PipeKitItem.kit(0, 1008), PipeKitItem.kit(0, 32));
            var left = remaining(ctx, 2, 1, PipeKitItem.kit(0, 1008), PipeKitItem.kit(0, 32));
            ctx.check("pipe: 63 + 2 blocks top up to a full kit, 1 block stays", PipeKitItem.length(full) == 1024
                && left.size() == 2 && left.get(0).isEmpty() && PipeKitItem.length(left.get(1)) == 16, PipeKitItem.length(full) + " / " + left);
            ItemStack cfull = craft(ctx, 2, 1, mods.eln.node.voxel.CableKitItem.kit(0, 1000), mods.eln.node.voxel.CableKitItem.kit(0, 100));
            var cleft = remaining(ctx, 2, 1, mods.eln.node.voxel.CableKitItem.kit(0, 1000), mods.eln.node.voxel.CableKitItem.kit(0, 100));
            ctx.check("cable: 1000 + 100 voxels top up to a full kit, 76 stay", mods.eln.node.voxel.KitItem.length(cfull) == 1024
                && cfull.getItem() instanceof mods.eln.node.voxel.CableKitItem && cleft.size() == 2 && mods.eln.node.voxel.KitItem.length(cleft.get(1)) == 76,
                cfull + " / " + cleft);
            ItemStack none = craft(ctx, 2, 1, PipeKitItem.kit(0, 1024), PipeKitItem.kit(0, 32));
            ctx.check("pipe: a full kit and another don't merge (nothing to top up)", none.isEmpty() || !(none.getItem() instanceof PipeKitItem), "" + none);
            // shift-click in a crafting table: the full kit (the rest stays: its plates are another item, so the loop stops)
            var shift = shiftCraft(ctx, PipeKitItem.kit(0, 1008), PipeKitItem.kit(0, 32));
            ctx.check("pipe: shift-click 63 + 2 blocks: a full kit, the block left over stays in the grid", shift.equals("kits [1024] plates 0 grid [16]"), shift);
            var unroll = shiftCraft(ctx, PipeKitItem.kit(0, 40));
            ctx.check("pipe: shift-click a 2.5-block kit: 4 plates, half a block stays", unroll.equals("kits [] plates 4 grid [8]"), unroll);
        }

        /** Shift-click the result of a crafting table holding items (from slot 1): "kits [lengths] plates n grid [kit lengths]". */
        static String shiftCraft(TestContext ctx, ItemStack... items) {
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.get(ctx.level(),
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("e1a0c0de-0000-4000-8000-00000000c0de"), "eln_kit_shift"));
            fp.getInventory().clearContent();
            var cm = new net.minecraft.world.inventory.CraftingMenu(8, fp.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.level(), BlockPos.ZERO));
            for (int i = 0; i < items.length; i++) cm.getSlot(1 + i).set(items[i]);
            cm.slotsChanged(cm.getSlot(1).container);
            cm.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, fp);
            var inv = new java.util.ArrayList<Integer>();
            int plates = 0;
            for (int i = 0; i < fp.getInventory().getContainerSize(); i++) {
                var s = fp.getInventory().getItem(i);
                if (s.getItem() instanceof mods.eln.node.voxel.KitItem) inv.add(mods.eln.node.voxel.KitItem.length(s));
                else if (mods.eln.node.voxel.PipeKitRecipes.plateTier(s) >= 0) plates += s.getCount();
            }
            var grid = new java.util.ArrayList<Integer>();
            for (int i = 1; i <= 9; i++)
                if (cm.getSlot(i).getItem().getItem() instanceof mods.eln.node.voxel.KitItem) grid.add(mods.eln.node.voxel.KitItem.length(cm.getSlot(i).getItem()));
            fp.getInventory().clearContent();
            return "kits " + inv + " plates " + plates + " grid " + grid;
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

    /**
     * A physics bug (Baughn: fix when spotted): a throttle valve 2 % open passed water as 2 % of a lossless pipe, ~3 kg/s
     * at 5 bar, so no feed could be metered. By the orifice law through an alloy seat it passes ~25 g/s.
     */
    static final class ValveMeters implements TestCase {
        mods.eln.sim.fluid.FluidDevice fx;
        mods.eln.sim.fluid.FluidNode sink;
        double m0, flow;
        mods.eln.sim.IProcess watch;
        int tick;

        public String id() {
            return "valve_meters_water_by_orifice_law";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            BlockPos pv = ctx.at(4, 1, 1);
            lay(ctx, pv, 1, 0, 8, 8, 8, 8, 8, 16, 8, 8);
            VoxelNode n = node(ctx, pv);
            n.addValve(8, 8, 8, mods.eln.node.voxel.ValvePart.THROTTLE);
            for (VoxelPart part : n.parts) if (part instanceof mods.eln.node.voxel.ValvePart v) v.setting = 0.02;
            // (a big free vessel of water under 6 bar of air, and an empty 10 m^3 one at 1 atm, behind test ports)
            var src = new mods.eln.sim.fluid.FluidNode(1.0, pv.getY(), 1.0, false).fill(0.7 * mods.eln.sim.fluid.Water.RHO_L, 6e5, 293.15);
            sink = new mods.eln.sim.fluid.FluidNode(10.0, pv.getY(), 1.0, false).fill(0, mods.eln.sim.fluid.Water.P_ATM, 293.15);
            double k = 2 * PipePart.K_PER_BLOCK[1] * 16;
            List<mods.eln.sim.fluid.FluidDevice.FluidPort> ports = List.of(
                new mods.eln.sim.fluid.FluidDevice.FluidPort(src, pv.west().asLong(), 5, 0.5, k),
                new mods.eln.sim.fluid.FluidDevice.FluidPort(sink, pv.east().asLong(), 4, 0.5, k));
            Object level = n.fluidLevel();
            fx = new mods.eln.sim.fluid.FluidDevice() {
                public Object fluidLevel() {
                    return level;
                }

                public List<FluidPort> fluidPorts() {
                    return ports;
                }
            };
            mods.eln.fluid.FluidWorld.add(fx);
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 100) m0 = sink.mWater;
                if (tick > 100) flow = (sink.mWater - m0) / ((tick - 100) * dt);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            mods.eln.fluid.FluidWorld.remove(fx);
            ctx.line(String.format("valve 2 %% open, water 6 bar -> 1 atm: %.1f g/s", flow * 1000));
            ctx.check("a throttle valve 2 % open meters water (10-60 g/s through an alloy seat at ~5 bar)", flow > 0.010 && flow < 0.060,
                String.format("%.1f g/s", flow * 1000));
        }
    }

    /**
     * A physics bug: water was soft (bulk modulus 2e7 Pa): a liquid-full pipe took 1.4 x its volume at ~95 bar. At water's
     * own ~2 GPa a full pipe pushed by a 50 bar reservoir holds under 1 % more than it should.
     */
    static final class LiquidStaysIncompressible implements TestCase {
        mods.eln.sim.fluid.FluidDevice fx;
        mods.eln.sim.fluid.FluidNode pipe;

        public String id() {
            return "liquid_full_pipe_incompressible";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(4, 1, 1);
            lay(ctx, p, 1, 0, 8, 8, 16, 8, 8);   // (one alloy block, its east end shut)
            VoxelNode n = node(ctx, p);
            for (VoxelPart part : n.parts) if (part instanceof PipePart q) pipe = q.fluid;
            pipe.fill(0.97 * pipe.volume * mods.eln.sim.fluid.Water.RHO_L, mods.eln.sim.fluid.Water.P_ATM, 293.15);
            var src = new mods.eln.sim.fluid.FluidNode(1.0, p.getY(), 1.0, false).fill(0.7 * mods.eln.sim.fluid.Water.RHO_L, 50e5, 293.15);
            List<mods.eln.sim.fluid.FluidDevice.FluidPort> ports = List.of(
                new mods.eln.sim.fluid.FluidDevice.FluidPort(src, p.west().asLong(), 5, 0.5, 2 * PipePart.K_PER_BLOCK[1] * 16));
            Object level = n.fluidLevel();
            fx = new mods.eln.sim.fluid.FluidDevice() {
                public Object fluidLevel() {
                    return level;
                }

                public List<FluidPort> fluidPorts() {
                    return ports;
                }
            };
            mods.eln.fluid.FluidWorld.add(fx);
        }

        public void measure(TestContext ctx) {
            mods.eln.fluid.FluidWorld.remove(fx);
            double full = pipe.mWater / (pipe.volume * mods.eln.sim.fluid.Water.RHO_L);
            ctx.line(String.format("liquid-full pipe on a 50 bar reservoir: %.4f of its volume, %.1f bar", full, pipe.P() / 1e5));
            ctx.check("liquid-full: under 1 % over its volume", full < 1.01, String.format("%.4f", full));
            ctx.check("liquid-full: at about the reservoir's pressure (40-60 bar)", pipe.P() > 40e5 && pipe.P() < 60e5, String.format("%.1f bar", pipe.P() / 1e5));
        }
    }
}
