package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.Oil;
import mods.eln.oil.Reservoir;
import mods.eln.oil.ReservoirData;
import mods.eln.oil.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * Oil lakes (design v2 §2): the pooled volume and the lockstep surface. A small lake in the test box: 7 x 3 columns,
 * odd x two cells deep from y 1, even x one cell from y 2 (stone below), so the floor is uneven.
 */
public final class OilCases {
    private OilCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Lockstep());
        cases.add(new Buckets());
        cases.add(new Dam());
        cases.add(new CatchUp());
        cases.add(new LakeSizes());
        cases.add(new Separator());
        cases.add(new Pump(false));
        cases.add(new Pump(true));
        cases.add(new Boiler());
        cases.add(new ColdBoilerOtherPipes());
        cases.add(new SteamTurbine());
        cases.add(new TurbineFloods());
        cases.add(new TurbineLowPressure());
        cases.add(new TurbineCondenser());
        cases.add(new TurbineHoldsPressure());
        cases.add(new CondenserFloods());
        cases.add(new CondenserMakeupFloat());
        cases.add(new TurbineLoadDrop());
        cases.add(new TurbineRunaway());
        cases.add(new StackedPlant());
        cases.add(new CompactTurbine("compact_turbine_full_power", 3000, 4000, true, 99, 1200));
        cases.add(new CompactTurbine("compact_turbine_short_of_heat", 1000, 4000, true, 99, 1200));
        cases.add(new CompactTurbine("compact_turbine_no_load_bypass", 3000, 4000, false, 99, 1200));
        cases.add(new CompactTurbine("compact_turbine_vents_surplus", 6000, 4000, false, 99, 1200));
        cases.add(new CompactTurbine("compact_turbine_dry", 1000, 0, true, 99, 600));
        cases.add(new CompactTurbine("compact_turbine_cold_start", 1000, 4000, true, 20, 3600));
        cases.add(new CompactTurbinePipe());
        cases.add(new SteamOvenCase());
        cases.add(new SealedPlant());
        cases.add(new DryBoiler());
        cases.add(new SafetyValvePlant(4000, 1200, "fluid_safety_valve_holds_the_plant"));
        cases.add(new SafetyValvePlant(8000, 1400, "fluid_safety_valve_relieves_steadily"));
        cases.add(new SteamJetScalds());
        cases.add(new OldBoilerSave());
        cases.add(new Buckets2());
        cases.add(new Column());
        cases.add(new ColumnFold());
        cases.add(new Flare());
    }

    /** Build the lake (full) and sweep it in. */
    static Reservoir lake(TestContext ctx) {
        int x0 = ctx.at(1, 0, 0).getX(), z0 = ctx.at(0, 0, 0).getZ(), y1 = ctx.at(0, 1, 0).getY();
        int[] floor = new int[7 * 3], mask = new int[7 * 3];
        for (int dx = 0; dx < 7; dx++)
            for (int dz = 0; dz < 3; dz++) {
                boolean deep = (dx + 1) % 2 == 1;
                floor[dx * 3 + dz] = deep ? y1 : y1 + 1;
                mask[dx * 3 + dz] = deep ? 0b11 : 0b1;
                if (!deep) ctx.level().setBlock(new BlockPos(x0 + dx, y1, z0 + dz), Blocks.STONE.defaultBlockState(), 3);
            }
        Reservoir r = ReservoirData.get(ctx.level()).create(x0, z0, 7, 3, floor, mask, Long.MAX_VALUE);
        sweepAll(ctx, r);
        return r;
    }

    static void sweepAll(TestContext ctx, Reservoir r) {
        BlockPos a = ctx.at(1, 0, 0), b = ctx.at(7, 0, 2);
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++)
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++)
                Reservoirs.sweep(ctx.level(), r, cx, cz);
    }

    static int level(TestContext ctx, int x, int y, int z) {
        BlockState s = ctx.level().getBlockState(ctx.at(x, y, z));
        return s.is(Oil.RESERVOIR_BLOCK.get()) ? s.getValue(BlockStateProperties.LEVEL_FLOWING) : 0;
    }

    static void cleanup(TestContext ctx, Reservoir r) {
        ReservoirData.get(ctx.level()).remove(r);
    }

    abstract static class Base implements TestCase {
        Reservoir r;

        public int width() {
            return 9;
        }

        public int ticks() {
            return 2;
        }
    }

    /** Half way up the second cell: every column shows the same surface (level 4 at y 2; level 8 at y 1 where deep). */
    static final class Lockstep extends Base {
        public String id() {
            return "oil_lake_lockstep";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            long cap = r.capacity();
            ctx.check("oil: full capacity is 4 x 3 deep columns x 2 cells + 3 x 3 shallow x 1 = 33 cells", cap == 33_000, "cap " + cap);
            int y1 = ctx.at(0, 1, 0).getY();
            long half = r.volumeAt((y1 + 1) * 8 + 4);
            ctx.check("oil: the volume half way up the second cell", half == 12 * 12 * 125 + 9 * 4 * 125, "" + half);
            Reservoirs.take(ctx.level(), r, r.amount() - half, false);
            ctx.check("oil: surface at y2 + 4/8", r.surface() == (y1 + 1) * 8 + 4, r.surface() + " vs " + ((y1 + 1) * 8 + 4));
            sweepAll(ctx, r);
            boolean same = true;
            StringBuilder sb = new StringBuilder();
            for (int x = 1; x <= 7; x++)
                for (int z = 0; z <= 2; z++) {
                    int top = level(ctx, x, 2, z), low = level(ctx, x, 1, z);
                    boolean deep = x % 2 == 1;
                    if (top != 4 || (deep && low != 8)) {
                        same = false;
                        sb.append(x).append(',').append(z).append(':').append(low).append('/').append(top).append(' ');
                    }
                }
            ctx.check("oil: every column shows the same surface", same, sb.toString());
            cleanup(ctx, r);
        }
    }

    /** A bucket takes 1000 mB from the record; crude poured back in rejoins it; poured elsewhere it's plain crude. */
    static final class Buckets extends Base {
        public String id() {
            return "oil_lake_buckets";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            long full = r.amount();
            BlockPos cell = ctx.at(3, 2, 1);
            ItemStack got = ((mods.eln.oil.ReservoirOilBlock) Oil.RESERVOIR_BLOCK.get()).pickupBlock(null, ctx.level(), cell, ctx.level().getBlockState(cell));
            ctx.check("oil: a bucket of crude out", got.is(Oil.CRUDE_BUCKET.get()), got.toString());
            ctx.check("oil: the record lost 1000 mB", r.amount() == full - 1000, "" + r.amount());
            ctx.check("oil: the cell is still oil (the surface moves on the next sweep)", level(ctx, 3, 2, 1) > 0, "");
            // pour it back into the lake: a crude source in a lake cell
            ctx.level().setBlock(cell, Oil.CRUDE_BLOCK.get().defaultBlockState(), 3);
            sweepAll(ctx, r);
            ctx.check("oil: poured back, the record is full again", r.amount() == full, "" + r.amount());
            ctx.check("oil: and the cell is lake oil again", level(ctx, 3, 2, 1) > 0, "");
            // poured elsewhere: plain crude, nothing to do with the lake
            BlockPos out = ctx.at(8, 1, 1);
            ctx.level().setBlock(out, Oil.CRUDE_BLOCK.get().defaultBlockState(), 3);
            sweepAll(ctx, r);
            ctx.check("oil: crude poured outside stays crude", ctx.level().getBlockState(out).is(Oil.CRUDE_BLOCK.get()), "");
            ctx.check("oil: and the lake doesn't change", r.amount() == full, "" + r.amount());
            ctx.level().setBlock(out, Blocks.AIR.defaultBlockState(), 3);
            cleanup(ctx, r);
        }
    }

    /** A block placed into the lake is a dam: the cell leaves the lake; the oil stays in the record. */
    static final class Dam extends Base {
        public String id() {
            return "oil_lake_dam";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            long cap = r.capacity();
            Reservoirs.take(ctx.level(), r, 5000, false);
            long amount = r.amount();
            BlockPos cell = ctx.at(5, 1, 0);
            ctx.level().setBlock(cell, Blocks.COBBLESTONE.defaultBlockState(), 3);
            sweepAll(ctx, r);
            ctx.check("oil: the dammed cell left the lake", !r.contains(cell.getX(), cell.getY(), cell.getZ()), "");
            ctx.check("oil: capacity one cell less", r.capacity() == cap - 1000, r.capacity() + " vs " + (cap - 1000));
            ctx.check("oil: the oil it held stays in the record", r.amount() == amount, "" + r.amount());
            ctx.check("oil: the dam block stays", ctx.level().getBlockState(cell).is(Blocks.COBBLESTONE), "");
            cleanup(ctx, r);
        }
    }

    /** The tar sand separator: tar sand -> sand + 100 mB crude in its tank, drainable through the fluid capability. */
    static final class Separator implements TestCase {
        mods.eln.transparentnode.electricalmachine.OilSeparatorElement sep;

        public String id() {
            return "oil_separator";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            sep = (mods.eln.transparentnode.electricalmachine.OilSeparatorElement) l.device(0 + (80 << 6), 2, 1);
            l.feed(ctx.at(2, 1, 1), sep.front.left());
            sep.getInventory().setItem(sep.inSlotId, new ItemStack(mods.eln.oil.OilWorldgen.TAR_SAND_ITEM.get(), 1));
            sep.getInventory().setChanged();
            // 20 kJ a block at 200 W is 100 s: run it 4x faster for the test (800 W, within the LV source and cable)
            mods.eln.sim.ElectricalStackMachineProcess proc = Wp10aCases.field(sep, "slowRefreshProcess");
            proc.setSpeedUp(4);
        }

        public void measure(TestContext ctx) {
            ItemStack in = sep.getInventory().getItem(sep.inSlotId), out = sep.getInventory().getItem(sep.outSlotId);
            var tank = (mods.eln.fluid.ElementFluidHandler) sep.getFluidHandler();
            mods.eln.sim.ElectricalStackMachineProcess proc = Wp10aCases.field(sep, "slowRefreshProcess");
            ctx.check("oil: separator used the tar sand and made sand", in.isEmpty() && out.is(net.minecraft.world.item.Items.SAND) && out.getCount() == 1,
                "in " + in + ", out " + out + ", recipes " + mods.eln.Eln.separatorRecipes.getRecipes().size() + ", can " + proc.smeltCan()
                    + ", extra " + proc.extraCondition.getAsBoolean() + ", P " + proc.getPower() + ", state " + proc.processState());
            ctx.check("oil: 100 mB crude in its tank", tank.getFluidAmount() == 100, tank.getFluidAmount() + " mB");
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, sep.node.coordonate.pos(), null);
            ctx.check("oil: the tank is the block's fluid capability (pipes can drain it)", cap == tank, "" + cap);
        }
    }

    /**
     * The pump on well casing: from a 3 x 3 lake under the test floor (or water) at its 1 kW: crude flow = power / (5% of
     * crude's heat value + a tiny lift), ~7.8 mB/s; the lake's record loses what the tank gains. Water at 20 J/mB.
     */
    static final class Pump implements TestCase {
        final boolean water;
        mods.eln.oil.OilPumpElement pump;
        Reservoir r;
        long before;

        Pump(boolean water) {
            this.water = water;
        }

        public String id() {
            return water ? "oil_pump_water" : "oil_pump_lake";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext ctx) {
            BlockPos casing = ctx.at(2, 0, 1), under = casing.below();
            ctx.level().setBlock(casing, mods.eln.oil.OilWorldgen.WELL_CASING.get().defaultBlockState(), 3);
            if (water) {
                ctx.level().setBlock(under, Blocks.WATER.defaultBlockState(), 3);
            } else {
                // (below the test floor is the structure's barrier layer: clear room for the lake, or it'd be a dam)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) ctx.level().setBlock(under.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
                int[] floor = new int[9], mask = new int[9];
                java.util.Arrays.fill(floor, under.getY());
                java.util.Arrays.fill(mask, 1);
                r = ReservoirData.get(ctx.level()).create(under.getX() - 1, under.getZ() - 1, 3, 3, floor, mask, Long.MAX_VALUE);
                Reservoirs.sweep(ctx.level(), r, under.getX() >> 4, under.getZ() >> 4);
                before = r.amount();
            }
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pump = (mods.eln.oil.OilPumpElement) l.device(0 + (81 << 6), 2, 1);
            l.feed(ctx.at(2, 1, 1), pump.front.left());
        }

        public void measure(TestContext ctx) {
            var tank = (mods.eln.fluid.ElementFluidHandler) pump.getFluidHandler();
            int got = tank.getFluidAmount();
            var fluid = tank.getFluid().getFluid();
            BlockPos under = ctx.at(2, 0, 1).below();
            ctx.line("oil: pump " + (water ? "water" : "lake") + ": " + got + " mB in 5 s, " + pump.getWaila() + ", at(under) "
                + ReservoirData.get(ctx.level()).at(under) + ", block under " + ctx.level().getBlockState(under) + ", casing " + ctx.level().getBlockState(ctx.at(2, 0, 1)));
            if (water) {
                // 1000 W / (20 J/mB + lift) ~ 50 mB/s
                ctx.check("oil: the pump draws water", fluid.isSame(net.minecraft.world.level.material.Fluids.WATER) && got > 100, got + " mB of " + fluid);
            } else {
                ctx.check("oil: the pump draws crude", fluid.isSame(Oil.CRUDE.get()) && got > 15 && got < 60, got + " mB of " + fluid);
                ctx.check("oil: the lake lost what the tank gained", before - r.amount() == got, (before - r.amount()) + " vs " + got);
                ReservoirData.get(ctx.level()).remove(r);
            }
        }
    }

    /** Each tick, take all the steam a boiler offers through its fluid capability, as a pipe would (mB taken). */
    static mods.eln.sim.IProcess pipeOut(mods.eln.oil.BoilerElement boiler, int[] taken) {
        return t -> {
            int n = boiler.tanks.steam();
            if (n <= 0) return;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                taken[0] += boiler.tanks.extract(1, net.neoforged.neoforge.transfer.fluid.FluidResource.of(mods.eln.oil.Oil.STEAM.get()), n, tx);
                tx.commit();
            }
        };
    }

    /**
     * Other mods' pipes (Baughn: an Energized Power pipe drew a cold boiler below ambient and fed a turbine its water): a
     * cold, unheated boiler kept topped up by a pipe (its air squeezed above 1 atm) offers no steam through its
     * capability, and a pipe drawing at it every tick for 30 s takes none and leaves it at ambient; the steam turbine exposes no fluid capability at all (it runs off pressure).
     */
    static final class ColdBoilerOtherPipes implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        final int[] taken = new int[1];
        mods.eln.sim.IProcess pipe;

        public String id() {
            return "oil_cold_boiler_other_pipes";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 3, 1);
            boiler.fromTanks(3000, 0);
            boiler.showTanks();
            // (a pipe keeps it topped up with cold water: that squeezes the air over it above 1 atm)
            mods.eln.sim.IProcess draw = pipeOut(boiler, taken);
            mods.eln.Eln.simulator.addSlowProcess(pipe = t -> {
                boiler.tanks.fillWater(100);
                draw.process(t);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(pipe);
            double c = boiler.vessel.T() - mods.eln.sim.fluid.Water.T0;
            ctx.check("oil: a cold boiler gives other mods' pipes no steam, and stays at ambient", taken[0] == 0 && c > 19,
                String.format("%d mB taken, %.1f C", taken[0], c));
            Object cap = ctx.world().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, ctx.at(3, 1, 1), net.minecraft.core.Direction.UP);
            ctx.check("oil: the steam turbine has no fluid capability (steam only by its EA port)", cap == null && turbine.getFluidHandler() == null, String.valueOf(cap));
        }
    }

    /**
     * Electric heater (1 kW) against a boiler with water, its steam drawn off by a pipe (the fluid capability): the
     * boiler heats to 100 C, then boils: steam = heat / 2257 J per g (the water is already hot), so up to ~440 mB/s at
     * the full kilowatt (in a steady plant, fed cold water, 175 J per mB of water: ~385 mB/s, as before the vessel).
     * Since the pipes' phase 2 the boiler is a vessel: what leaves as steam is exactly what the water lost.
     */
    static final class Boiler implements TestCase {
        mods.eln.oil.ElectricHeaterElement heater;
        mods.eln.oil.BoilerElement boiler;
        final int[] taken = new int[1];
        mods.eln.sim.IProcess pipe;
        double water0;

        public String id() {
            return "oil_boiler";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 4, 1);
            heater = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 3, 1);
            // heat out of the heater's back, into the boiler (+X); power from its side
            heater.front = mods.eln.misc.Direction.XN;
            heater.reconnect();
            l.feed(ctx.at(3, 1, 1), mods.eln.misc.Direction.ZN);
            // both start at 99 C, near boiling, full of water: the test is about boiling, not the warm-up
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            heater.thermal.Tc = boiler.thermal.Tc;
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            water0 = boiler.vessel.mWater;
            mods.eln.Eln.simulator.addSlowProcess(pipe = pipeOut(boiler, taken));
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(pipe);
            boiler.syncTanks();   // (the pipe's last draw since the last fluid step)
            int steam = taken[0];
            ctx.line("oil: boiler: " + boiler.getWaila() + ", heater " + heater.getWaila() + ", piped off " + steam + " mB");
            ctx.check("oil: the heater's back heats the boiler: steam made", steam > 2000, steam + " mB steam");
            ctx.check("oil: the steam meter is within what the heat allows", boiler.steamRate > 50 && boiler.steamRate <= 961 / 2.257 * 1.05, boiler.steamRate + " mB/s");
            double lost = (water0 - boiler.vessel.mWater) / mods.eln.sim.fluid.EaUnits.KG_PER_MB_STEAM;
            ctx.check("oil: the steam piped off is the water the vessel lost (1 mB of steam = 1 mg)", Math.abs(steam - lost) < 2, steam + " vs " + lost);
            // energy: 60 s x ~1 kW = ~60 kJ at 2257 J/g: at most ~27 B of steam
            ctx.check("oil: no more steam than the heat allows", steam <= 60 * 1000 / 2.257 * 1.05, steam + " mB");
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, boiler.node.coordonate.pos(), null);
            ctx.check("oil: the boiler's tanks are its fluid capability", cap == boiler.tanks, "" + cap);
        }
    }

    /**
     * The 50V steam turbine on a steady supply (steam piped in all the time, as Pipez would): the shaft spins up, the
     * governor holds it at speed; idle it gives the open-circuit voltage (1.05 x 50) and uses little steam (losses and
     * the leak past its valve); driving a 50V heater (2.5 ohm) it gives up to its 1 kW at 40% of the steam's 2.257 J/mB.
     */
    static final class SteamTurbine implements TestCase {
        mods.eln.oil.SteamGeneratorElement idle, run;
        mods.eln.oil.ElectricHeaterElement heater;
        mods.eln.sim.IProcess feed, meter;
        double idleUsed, runUsed, runEnergy, window;

        public String id() {
            return "oil_steam_turbine";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            idle = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 1, 1);
            run = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            heater = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            // turbine: electricity out of front/back (XN/XP); heater: power on its sides (front ZN: sides XN/XP)
            run.front = mods.eln.misc.Direction.XN;
            run.reconnect();
            heater.front = mods.eln.misc.Direction.ZN;
            heater.reconnect();
            int[] tick = new int[1];
            double[] before = new double[2];
            mods.eln.Eln.simulator.addSlowProcess(feed = t -> {
                // keep each inlet at its rating, 10 bar of saturated steam
                for (var tb : new mods.eln.oil.SteamGeneratorElement[]{idle, run})
                    if (tb.inlet != null) steam(tb);
                tick[0]++;
                if (tick[0] > 300) {   // (measure the last 5 s, after the spin-up)
                    window += t;
                    idleUsed += idle.steamRate * t;
                    runUsed += run.steamRate * t;
                    runEnergy += run.source.getEffectiveP() * t;
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.line("oil: steam turbine idle " + idle.getWaila() + ", running " + run.getWaila() + ", heater " + heater.getWaila());
            ctx.check("oil: idle, the governor holds the open-circuit voltage (1.05 x 50)", Math.abs(idle.load.getU() - 52.5) < 52.5 * 0.05, idle.load.getU() + " V");
            ctx.check("oil: idle, it uses little steam (losses and the valve's leak)", idleUsed / window < 0.2 * mods.eln.oil.SteamGeneratorDescriptor.nominalSteam(1000), idleUsed / window + " mB/s");
            double p = runEnergy / window, used = runUsed / window;
            ctx.check("oil: loaded, it drives the heater (1.1 kW at 52.5 V; a little over its kilowatt is fine)", p > 500 && p <= 1000 * 1.15, String.format("%.0f W", p));
            // (Baughn 2026-10-26: by the expansion; 10 bar saturated to 1 atm: 0.7 x 409 kJ/kg)
            double perKg = mods.eln.oil.SteamGeneratorDescriptor.NOMINAL_J_PER_KG;
            ctx.check("oil: its rating, 0.7 of the expansion from 10 bar to air: 286 kJ/kg", Math.abs(perKg - 0.7 * 409e3) < 3e3, perKg + " J/kg");
            ctx.check("oil: no more electricity than the expansion gives (+5%)", p <= used * mods.eln.sim.fluid.EaUnits.KG_PER_MB_STEAM * perKg * 1.05,
                String.format("%.0f W vs %.0f mB/s", p, used));
        }
    }

    /**
     * The low-pressure tip (Baughn: tell players to use a valve): a loaded turbine on 3 bar for over 30 s is due the
     * "Under Pressure" advancement (its placer gets it, if online) and shows a low pressure efficiency; one on 10 bar
     * isn't, at ~100%.
     */
    static final class TurbineLowPressure implements TestCase {
        mods.eln.oil.SteamGeneratorElement low, rated;
        mods.eln.sim.IProcess feed;

        public String id() {
            return "oil_steam_turbine_low_pressure";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 760;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            low = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 1, 1);
            var h1 = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 2, 1);
            rated = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            var h2 = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            for (var t : new mods.eln.oil.SteamGeneratorElement[]{low, rated}) {
                t.front = mods.eln.misc.Direction.XN;
                t.reconnect();
            }
            for (var h : new mods.eln.oil.ElectricHeaterElement[]{h1, h2}) {
                h.front = mods.eln.misc.Direction.ZN;
                h.reconnect();
            }
            double t3 = mods.eln.sim.fluid.Water.tsat(3e5);
            mods.eln.Eln.simulator.addSlowProcess(feed = t -> {
                if (low.inlet != null && low.inlet.P() < 3e5) mods.eln.sim.fluid.EaUnits.addWater(low.inlet, 5e-4, t3, true);
                if (rated.inlet != null) steam(rated);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.line("oil: on 3 bar " + low.getWaila() + "; on 10 bar " + rated.getWaila());
            var adv = ctx.level().getServer().getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "guide/low_pressure"));
            ctx.check("oil: the Under Pressure advancement exists", adv != null, "");
            ctx.check("oil: loaded on 3 bar for 30 s, the tip is due", low.lowPressureTips >= 1, "" + low.lowPressureTips);
            ctx.check("oil: on 10 bar, no tip", rated.lowPressureTips == 0, "" + rated.lowPressureTips);
            ctx.check("oil: pressure efficiency ~100% at 10 bar, about half at 3 bar", rated.pressureEfficiency() > 0.9 && rated.pressureEfficiency() < 1.1
                && low.pressureEfficiency() < 0.6, String.format("%.2f / %.2f", rated.pressureEfficiency(), low.pressureEfficiency()));
        }
    }

    /**
     * The turbine's pressure regulator (Baughn 2026-10-26): two boilers on 2.5 kW of heat, each on a 50V turbine driving
     * a 1 kW heater: more than the steam allows. Without a regulator the governor opens fully and the boiler sits near
     * 2 bar; with an analog regulator set to 4 bar the turbine holds its inlet there (it slows instead), and makes more
     * of the same heat. A third turbine on external control with no signal keeps its valve shut.
     */
    static final class TurbineHoldsPressure implements TestCase {
        mods.eln.oil.SteamGeneratorElement free, held, ext;
        mods.eln.oil.BoilerElement b1, b2;
        mods.eln.sim.IProcess fire;
        double e1, e2;
        int tick;

        public String id() {
            return "oil_steam_turbine_holds_pressure";
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 2400;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            free = plant(l, 1, false)[0];
            held = plant(l, 4, false)[0];
            ext = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 7, 1);
            b1 = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), ctx.at(1, 2, 1));
            b2 = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), ctx.at(4, 2, 1));
            for (var b : new mods.eln.oil.BoilerElement[]{b1, b2}) {
                b.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
                b.fromTanks(8000, 0);
                b.showTanks();
            }
            held.inventory.setItem(mods.eln.oil.SteamGeneratorElement.REGULATOR_SLOT, mods.eln.Eln.findItemStack("Analogic Regulator"));
            held.inventoryChange(held.inventory);
            held.targetBar = 4;
            held.inventoryChange(held.inventory);
            ext.controlExternal = true;
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                b1.thermal.moveEnergyTo(2500 * t);
                b2.thermal.moveEnergyTo(2500 * t);
                if (ext.inlet != null) steam(ext);
                if (++tick > 1600) {
                    e1 += free.source.getEffectiveP() * t;
                    e2 += held.source.getEffectiveP() * t;
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            ctx.line("oil: free " + free.getWaila() + "; held " + held.getWaila() + "; external " + ext.getWaila());
            ctx.check("oil: without a regulator, an overloaded turbine drains its boiler (under 4 bar)", free.inlet.P() < 4e5,
                String.format("%.2f bar", free.inlet.P() / 1e5));
            ctx.check("oil: with one set to 4 bar it holds its inlet there (3.5-5 bar)", held.inlet.P() > 3.5e5 && held.inlet.P() < 5e5,
                String.format("%.2f bar", held.inlet.P() / 1e5));
            ctx.check("oil: and makes more of the same heat", e2 > 1.3 * e1, String.format("%.0f J vs %.0f J", e2, e1));
            ctx.check("oil: external control with no signal keeps the valve shut", ext.steamRate < 0.02 * mods.eln.oil.SteamGeneratorDescriptor.nominalSteam(1000)
                && ext.omega < 0.5, String.format("%.0f mB/s, speed %.0f%%", ext.steamRate, ext.omega * 100));
        }
    }

    /** A turbine with a condenser against its exhaust side (right of its front: +z for a front of XN), and its load. */
    static mods.eln.oil.SteamGeneratorElement[] plant(Wp10aCases.Layout l, int dx, boolean condenser) {
        var t = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), dx, 1);
        var h = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), dx + 1, 1);
        t.front = mods.eln.misc.Direction.XN;
        t.reconnect();
        h.front = mods.eln.misc.Direction.ZN;
        h.reconnect();
        if (condenser) l.device(28 + (82 << 6), dx, 2);
        return new mods.eln.oil.SteamGeneratorElement[]{t};
    }

    static mods.eln.oil.CondenserElement condenserOf(TestContext ctx, BlockPos p) {
        return ctx.level().getBlockEntity(p) instanceof mods.eln.node.transparent.TransparentNodeEntity e
            && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, ctx.level())) instanceof mods.eln.node.transparent.TransparentNode n
            && n.element instanceof mods.eln.oil.CondenserElement c ? c : null;
    }

    /**
     * The condenser (Baughn 2026-10-26): two loaded 50V turbines on 10 bar steam, one venting to air, one into a Small
     * Steam Condenser against its exhaust side. The condenser pulls a vacuum (its ejector takes the air; its shell cools
     * to the ambient), so its turbine gets more per kg (pressure efficiency over 150%) and uses less steam for the same
     * load; the turbine sees it (WAILA: Exhaust), and its condensate pump moves the water.
     */
    static final class TurbineCondenser implements TestCase {
        mods.eln.oil.SteamGeneratorElement vent, cond;
        mods.eln.oil.CondenserElement c;
        mods.eln.sim.IProcess feed;
        double ventUsed, condUsed, window;
        int tick;

        public String id() {
            return "oil_steam_turbine_condenser";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            vent = plant(l, 1, false)[0];
            cond = plant(l, 4, true)[0];
            l.device(28 + (82 << 6), 0, 0);   // (one on its own)
            mods.eln.Eln.simulator.addSlowProcess(feed = t -> {
                for (var tb : new mods.eln.oil.SteamGeneratorElement[]{vent, cond}) if (tb.inlet != null) steam(tb);
                if (++tick > 600) {
                    window += t;
                    ventUsed += vent.steamRate * t;
                    condUsed += cond.steamRate * t;
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            c = condenserOf(ctx, ctx.at(4, 1, 2));
            ctx.line("oil: venting " + vent.getWaila() + "; with condenser " + cond.getWaila() + "; condenser " + (c == null ? "none" : c.getWaila()));
            ctx.check("oil: the turbine finds the condenser against its exhaust side", c != null && cond.condenser == c && c.turbine == cond, "");
            if (c == null) return;
            ctx.check("oil: the condenser pulls a vacuum (under 0.3 bar)", c.vessel.P() < 0.3e5, String.format("%.2f bar", c.vessel.P() / 1e5));
            ctx.check("oil: its turbine gets over 1.5x the work per kg", cond.pressureEfficiency() > 1.5, String.format("%.2f", cond.pressureEfficiency()));
            double v = ventUsed / window, w = condUsed / window;
            ctx.check("oil: and uses well under the steam for the same load (under 75%)", w < 0.75 * v, String.format("%.0f vs %.0f mB/s", w, v));
            var alone = condenserOf(ctx, ctx.at(0, 1, 0));
            ctx.check("oil: a condenser on its own keeps a sensible state (AdventurAgent: it showed -193 C)", alone != null && alone.turbine == null
                && alone.vessel.T() > 273 && alone.vessel.T() < 320 && Math.abs(alone.heatW) < 50, alone == null ? "none" : alone.getWaila().toString());
            ctx.check("oil: its condensate pump runs with the shaft", c.pumped > 0 || c.floodShare() < 0.01, String.format("%.0f mB/s pumped", c.pumped));
            // Jade says whether the pump runs, and why not (Baughn: a cold start gave no hint)
            String running = c.getWaila().get(mods.eln.i18n.I18N.tr("Condensate pump")), idle = alone.getWaila().get(mods.eln.i18n.I18N.tr("Condensate pump"));
            ctx.check("oil: Jade: the running condenser's pump shows its share and flow", running != null && running.endsWith("mB/s"), "" + running);
            ctx.check("oil: Jade: a condenser without a turning turbine says its pump is stopped", idle != null && idle.startsWith("stopped"), "" + idle);
            ctx.check("oil: Jade: its fill", c.getWaila().containsKey(mods.eln.i18n.I18N.tr("Fill")), "" + c.getWaila());
        }
    }

    /** The makeup flange's float valve (Baughn: a 200V pump on it filled a condenser to the flood line): with makeup
     *  water at 10 bar behind it, a condenser 30 % full takes none, one 5 % full takes it in. */
    static final class CondenserMakeupFloat implements TestCase {
        mods.eln.oil.CondenserElement full, low;
        mods.eln.sim.IProcess feed;
        double full0, low0;
        boolean primed;

        public String id() {
            return "oil_condenser_makeup_float";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            l.device(28 + (82 << 6), 1, 1);
            l.device(28 + (82 << 6), 3, 1);
            mods.eln.Eln.simulator.addSlowProcess(feed = x -> {
                if (full == null) full = condenserOf(ctx, ctx.at(1, 1, 1));
                if (low == null) low = condenserOf(ctx, ctx.at(3, 1, 1));
                if (full == null || low == null || full.inlet == null || low.inlet == null) return;
                if (!primed) {
                    primed = true;
                    double rho = mods.eln.sim.fluid.Water.RHO_L;
                    mods.eln.sim.fluid.EaUnits.addWater(full.vessel, 0.30 * full.vessel.volume * rho, 300, false);
                    mods.eln.sim.fluid.EaUnits.addWater(low.vessel, 0.05 * low.vessel.volume * rho, 300, false);
                    full0 = full.vessel.mWater;
                    low0 = low.vessel.mWater;
                }
                // (a pump on each makeup flange: water kept at 10 bar behind the valve)
                for (var c : new mods.eln.oil.CondenserElement[]{full, low})
                    {
                        // (into the run's lump, which the fluid step works on: water put into the flange's own node was lost
                        // or not, by where this process ran in the slow list: the test failed by its place in the grid)
                        var n = mods.eln.fluid.FluidWorld.nodeFor(c.inlet);
                        for (int k = 0; k < 100 && n.P() < 10e5; k++) mods.eln.sim.fluid.EaUnits.addWater(n, 2e-5, 300, false);
                    }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.check("oil: makeup at 10 bar: a condenser 30 % full takes none (float valve shut)", full != null
                && full.vessel.mWater <= full0 + 1e-6, full == null ? "none" : String.format("%.4f -> %.4f kg; %s", full0, full.vessel.mWater, full.getWaila()));
            ctx.check("oil: makeup at 10 bar: one 5 % full takes it in", low != null
                && low.vessel.mWater > low0 + 0.001, low == null ? "none" : String.format("%.4f -> %.4f kg; %s", low0, low.vessel.mWater, low.getWaila()));
        }
    }

    /** A flooded condenser floods its turbine's exhaust: water damage (over half its volume as water, turning). */
    static final class CondenserFloods implements TestCase {
        mods.eln.oil.SteamGeneratorElement t;
        mods.eln.oil.CondenserElement c;
        mods.eln.sim.IProcess feed;
        boolean ranBefore;
        int tick;

        public String id() {
            return "oil_condenser_floods";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            t = plant(l, 0, true)[0];
            c = condenserOf(ctx, ctx.at(0, 1, 2));
            mods.eln.Eln.simulator.addSlowProcess(feed = x -> {
                if (t.inlet != null && !t.isBurnt()) steam(t);
                if (++tick == 200 && c != null && c.vessel != null) {
                    ranBefore = !t.isBurnt() && t.omega > 0.5;
                    mods.eln.sim.fluid.EaUnits.addWater(c.vessel, 0.6 * c.vessel.volume * mods.eln.sim.fluid.Water.RHO_L, 320, false);
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.check("oil: condenser placed, and the turbine ran before the flood", c != null && ranBefore, "" + t.getWaila());
            ctx.check("oil: flooded, the turbine takes water damage", t.getBurnt() == mods.eln.sim.process.destruct.FailureCause.WATER, "" + t.getBurnt());
        }
    }

    /** Saturated steam into a turbine's inlet while it is under the rating (10 bar): as a boiler at 10 bar would. */
    static void steam(mods.eln.oil.SteamGeneratorElement t) {
        double pa = mods.eln.oil.SteamGeneratorDescriptor.NOMINAL_PA;
        if (t.inlet.P() < pa) mods.eln.sim.fluid.EaUnits.addWater(t.inlet, 5e-4, mods.eln.sim.fluid.Water.tsat(pa), true);
    }

    /** Losing its load at full power, the turbine overspeeds a little (the governor lags) and settles: no burst. */
    static final class TurbineLoadDrop implements TestCase {
        mods.eln.oil.SteamGeneratorElement run;
        mods.eln.sim.IProcess feed;
        double peak, atDrop;

        public String id() {
            return "oil_steam_turbine_load_drop";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            run = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            var heaterPos = ctx.at(5, 1, 1);
            var heater = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            run.front = mods.eln.misc.Direction.XN;
            run.reconnect();
            heater.front = mods.eln.misc.Direction.ZN;
            heater.reconnect();
            int[] tick = new int[1];
            mods.eln.Eln.simulator.addSlowProcess(feed = t -> {
                if (run.inlet != null) steam(run);
                if (++tick[0] == 300) {
                    atDrop = run.source.getEffectiveP();
                    ctx.level().removeBlock(heaterPos, false);   // (the load goes)
                }
                if (tick[0] > 300) peak = Math.max(peak, run.omega);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.line(String.format("oil: load drop from %.0f W: peak speed %.0f%%, now %s", atDrop, peak * 100, run.getWaila()));
            ctx.check("oil: it was running loaded", atDrop > 500, atDrop + " W");
            ctx.check("oil: it overspeeds a little but stays under the trip", peak > 1.0 && peak < 1.15, String.format("peak %.0f%%", peak * 100));
            ctx.check("oil: and settles back to speed", Math.abs(run.omega - 1) < 0.03 && !run.node.isDestructing(), String.format("%.0f%%", run.omega * 100));
        }
    }

    /**
     * Water in a turning turbine (Baughn: a significant quantity of water damages it): two loaded turbines at speed on
     * steam; at 300 ticks one gets 5% of its inlet as water, which it survives, the other 15%, which burns it out
     * (FailureCause.WATER). A stopped turbine holds 15% unharmed. The casing drain lets water out slowly meanwhile.
     */
    static final class TurbineFloods implements TestCase {
        mods.eln.oil.SteamGeneratorElement wet, flooded, still;
        mods.eln.sim.IProcess feed;
        double wetShare;

        public String id() {
            return "oil_steam_turbine_floods";
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 500;
        }

        static void water(mods.eln.oil.SteamGeneratorElement t, double share) {
            mods.eln.sim.fluid.EaUnits.addWater(t.inlet, share * t.inlet.volume * mods.eln.sim.fluid.Water.RHO_L, t.inlet.T(), false);
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            wet = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 1, 1);
            var h1 = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 2, 1);
            flooded = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            var h2 = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            still = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 7, 1);
            for (var t : new mods.eln.oil.SteamGeneratorElement[]{wet, flooded}) {
                t.front = mods.eln.misc.Direction.XN;
                t.reconnect();
            }
            for (var h : new mods.eln.oil.ElectricHeaterElement[]{h1, h2}) {
                h.front = mods.eln.misc.Direction.ZN;
                h.reconnect();
            }
            int[] tick = new int[1];
            mods.eln.Eln.simulator.addSlowProcess(feed = t -> {
                for (var tb : new mods.eln.oil.SteamGeneratorElement[]{wet, flooded})
                    if (tb.inlet != null) steam(tb);
                if (++tick[0] == 300 && wet.inlet != null) {
                    water(wet, 0.05);
                    water(flooded, 0.15);
                    water(still, 0.15);
                }
                if (tick[0] > 300) wetShare = Math.max(wetShare, wet.waterShare());
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.line("oil: wet " + wet.getWaila() + "; flooded burnt " + flooded.getBurnt() + "; still " + still.getWaila());
            ctx.check("oil: 5% water in a turning turbine: it keeps running", !wet.isBurnt() && wet.omega > 0.8 && wetShare > 0.03,
                String.format("speed %.0f%%, water %.0f%%", wet.omega * 100, wetShare * 100));
            ctx.check("oil: 15% water in a turning turbine: water damage", flooded.getBurnt() == mods.eln.sim.process.destruct.FailureCause.WATER, "" + flooded.getBurnt());
            ctx.check("oil: 15% water in a stopped turbine: no harm", !still.isBurnt(), "" + still.getBurnt());
            // the casing drain (Baughn: water gets out slowly; 0.5% of the inlet a second): 10 s after, the wet one is dry and
            // the stopped one is down by about 5 points
            ctx.check("oil: the casing drain empties 5% in 10 s", wet.waterShare() < 0.005, String.format("%.2f%%", wet.waterShare() * 100));
            ctx.check("oil: and lowers a stopped turbine's 15%", still.waterShare() < 0.12 && still.waterShare() > 0.08,
                String.format("%.2f%%", still.waterShare() * 100));
            flooded.repair();
            ctx.check("oil: repaired, the flooded turbine is drained and works", !flooded.isBurnt() && flooded.waterShare() < 0.001,
                String.format("burnt %s, water %.1f%%", flooded.getBurnt(), flooded.waterShare() * 100));
        }
    }

    /**
     * The casing (Baughn: rated 10 bar, bursts over 25): an unloaded turbine on 20 bar steam holds its speed (the leak
     * past its shut valve doesn't run it away), one fed on towards 40 bar bursts as a pressure vessel.
     */
    static final class TurbineRunaway implements TestCase {
        mods.eln.oil.SteamGeneratorElement held, burst;
        mods.eln.sim.IProcess feed;
        double peak;

        public String id() {
            return "oil_steam_turbine_casing";
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            held = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 1, 1);
            burst = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 7, 1);   // (away from the other: it explodes)
            held.omega = 1;
            double t20 = mods.eln.sim.fluid.Water.tsat(20e5), t40 = mods.eln.sim.fluid.Water.tsat(40e5);
            mods.eln.Eln.simulator.addSlowProcess(feed = t -> {
                if (held.inlet != null && held.inlet.P() < 20e5) mods.eln.sim.fluid.EaUnits.addWater(held.inlet, 0.002, t20, true);
                if (burst.inlet != null && !burst.isBurnt() && burst.inlet.P() < 40e5) mods.eln.sim.fluid.EaUnits.addWater(burst.inlet, 0.002, t40, true);
                peak = Math.max(peak, held.omega);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.line(String.format("oil: unloaded on 20 bar: peak %.0f%%, now %s; fed to 40 bar: burnt %s", peak * 100, held.getWaila(), burst.getBurnt()));
            ctx.check("oil: unloaded on 20 bar, it holds its speed", peak < 1.15 && !held.isBurnt() && !held.node.isDestructing(), String.format("peak %.0f%%", peak * 100));
            ctx.check("oil: over 25 bar its casing bursts", burst.isBurnt() || burst.node.isDestructing(), "" + burst.getBurnt());
        }
    }

    /**
     * The steam oven (Baughn 2026-10-23): a boiler set on it (its dome into the oven's top port) on 2.5 kW of heat. With
     * its fan on 50 V it cooks beef at up to twice the electrical oven's best food speed (280 W: 3.6 s an item), taking
     * 280 W / 25% of the steam's heat (~500 mB/s); without the fan it cooks nothing; hoppers feed only food.
     */
    static final class SteamOvenCase implements TestCase {
        mods.eln.oil.SteamOvenElement oven, unfed;
        mods.eln.oil.BoilerElement b1, b2;
        mods.eln.sim.IProcess fire;
        String hoppers;
        boolean hoppersOk;

        public String id() {
            return "steam_oven_cooks_with_steam";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 600;
        }

        mods.eln.oil.BoilerElement boiler(TestContext ctx, int x) {
            var b = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), ctx.at(x, 2, 1));
            b.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            b.fromTanks(8000, 0);
            b.showTanks();
            return b;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            oven = (mods.eln.oil.SteamOvenElement) l.device(20 + (82 << 6), 1, 1);
            l.feed(ctx.at(1, 1, 1), oven.front.left());
            unfed = (mods.eln.oil.SteamOvenElement) l.device(20 + (82 << 6), 5, 1);
            for (var o : new mods.eln.oil.SteamOvenElement[]{oven, unfed}) {
                o.getInventory().setItem(mods.eln.oil.SteamOvenElement.inSlotId, new ItemStack(Items.BEEF, 32));
                o.getInventory().setChanged();
            }
            b1 = boiler(ctx, 1);
            b2 = boiler(ctx, 5);
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                b1.thermal.moveEnergyTo(2500 * t);
                b2.thermal.moveEnergyTo(2500 * t);
            });
            var inv = (mods.eln.oil.SteamOvenInventory) oven.getInventory();
            int side = net.minecraft.core.Direction.NORTH.get3DDataValue(), down = net.minecraft.core.Direction.DOWN.get3DDataValue();
            boolean beef = inv.canInsertItem(0, new ItemStack(Items.BEEF), side), ore = inv.canInsertItem(0, new ItemStack(Items.RAW_IRON), side);
            boolean out = inv.canExtractItem(1, new ItemStack(Items.COOKED_BEEF), down);
            hoppers = "side takes beef " + beef + ", raw iron " + ore + "; bottom gives out " + out;
            hoppersOk = beef && !ore && out;
        }

        static int cooked(mods.eln.oil.SteamOvenElement o) {
            return o.getInventory().getItem(mods.eln.oil.SteamOvenElement.outSlotId).getCount();
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            ctx.line("steam oven: " + cooked(oven) + " cooked, unfed " + cooked(unfed) + "; " + oven.getWaila() + "; boiler " + b1.getWaila());
            // (the boiler needs a while to raise steam: count what the last 20 s should give at most, and that it cooked)
            ctx.check("steam oven: cooks beef on a boiler's steam", cooked(oven) >= 3 && cooked(oven) <= 9, "" + cooked(oven));
            ctx.check("steam oven: no fan (no 50 V), no cooking", cooked(unfed) == 0, "" + cooked(unfed));
            ctx.check("steam oven: never faster than 280 W (3.6 s an item)", cooked(oven) <= 30 / 3.57 + 1, "" + cooked(oven));
            ctx.check("steam oven: hoppers feed only food, the bottom empties", hoppersOk, hoppers);
        }
    }

    /**
     * Pipes phase 2 (notes/pipes/DESIGN.md): a boiler set on a steam turbine feeds it through its dome port, no pipes.
     * 2.5 kW of heat into the boiler's shell; the turbine drives a 50 V heater. After a minute the turbine runs near
     * its nominal kilowatt, and below what the heat allows (40% of it at the boiling point).
     */
    static final class StackedPlant implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        mods.eln.oil.ElectricHeaterElement load;
        mods.eln.sim.IProcess fire;
        double energy;
        static final double HEAT = 9600;   // (a 1 kW turbine at its 10 bar rating, venting: 3.5 g/s of steam)

        public String id() {
            return "fluid_boiler_on_turbine";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            load = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            turbine.front = mods.eln.misc.Direction.XN;
            turbine.reconnect();
            load.front = mods.eln.misc.Direction.ZN;
            load.reconnect();
            boiler = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), ctx.at(4, 2, 1));
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                boiler.thermal.moveEnergyTo(HEAT * t);
                if (energy >= 0) energy += turbine.source.getEffectiveP() * t;
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            double p = turbine.source.getEffectiveP();
            ctx.line("fluid: boiler " + boiler.getWaila() + ", turbine " + turbine.getWaila() + "; boiler in sim " + boiler.node.isAdded()
                + " at " + boiler.node.coordonate + ", turbine in sim " + turbine.node.isAdded() + " at " + turbine.node.coordonate
                + ", boiler element now " + ctx.level().getBlockEntity(boiler.node.coordonate.pos()));
            ctx.check("fluid: the boiler's dome feeds the turbine below (one network)",
                mods.eln.fluid.FluidWorld.networkOf(boiler.vessel) != null && mods.eln.fluid.FluidWorld.networkOf(boiler.vessel) == mods.eln.fluid.FluidWorld.networkOf(turbine.inlet), "");
            ctx.check("fluid: the turbine runs near its nominal kilowatt", p > 700 && p <= 1050, String.format("%.0f W", p));
            ctx.check("fluid: no more electricity than 40% of the heat (+5%)", energy <= 0.42 * HEAT * 80, String.format("%.0f J of %.0f J heat", energy, HEAT * 80));
            ctx.check("fluid: the boiler stays below 10 bar (the turbine takes its steam)", boiler.vessel.P() < 10e5, boiler.vessel.P() + " Pa");
            ctx.check("fluid: no water collects in the turbine (it isn't damaged)", turbine.waterShare() < 0.01 && !turbine.isBurnt(),
                String.format("%.2f%% water", turbine.waterShare() * 100));
        }
    }

    /**
     * The compact steam turbine (Baughn 2026-10-27, the bootstrap option): heat put straight into its boiler, a 50 V
     * heater on its back as the load, or none. ETA 10%: 3 kW gives its 300 W, 1 kW a third of it; with no load the
     * bypass takes a stone heat furnace's 3 kW at a little over 150 C without venting; 6 kW vents; dry it stops and
     * doesn't fail; from cold on 1 kW it makes power within 3 minutes.
     */
    static final class CompactTurbine implements TestCase {
        final String id;
        final double heat, water0, startC;
        final boolean loaded;
        final int ticks;
        mods.eln.oil.CompactTurbineElement turbine;
        mods.eln.sim.IProcess fire;
        double energy, heatGiven, peakC;

        CompactTurbine(String id, double heat, double water0, boolean loaded, double startC, int ticks) {
            this.id = id;
            this.heat = heat;
            this.water0 = water0;
            this.loaded = loaded;
            this.startC = startC;
            this.ticks = ticks;
        }

        public String id() {
            return id;
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return ticks;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            turbine = (mods.eln.oil.CompactTurbineElement) l.device(36 + (82 << 6), 3, 1);
            turbine.front = mods.eln.misc.Direction.XN;
            turbine.reconnect();
            if (loaded) {
                var load = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 4, 1);
                load.front = mods.eln.misc.Direction.ZN;
                load.reconnect();
            }
            turbine.water = water0;
            turbine.boiler.Tc = startC - mods.eln.sim.PhysicalConstant.Tamb;
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                turbine.boiler.moveEnergyTo(heat * t);
                heatGiven += heat * t;
                energy += turbine.source.getEffectiveP() * t;
                peakC = Math.max(peakC, turbine.celsius());
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            double p = turbine.powerOut, used = water0 - turbine.water, c = turbine.celsius();
            ctx.line(id + ": " + turbine.getWaila() + String.format("; %.0f J out of %.0f J heat, %.1f mB used, peak %.0f C", energy, heatGiven, used, peakC));
            ctx.check(id + ": not burnt", !turbine.isBurnt(), "");
            ctx.check(id + ": never more than 10% of the heat (+1%)", energy <= 0.11 * heatGiven, String.format("%.0f of %.0f J", energy, heatGiven));
            switch (id) {
                case "compact_turbine_full_power" -> {
                    ctx.check(id + ": 3 kW of heat gives its 300 W", p > 270 && p < 310, String.format("%.0f W", p));
                    ctx.check(id + ": about 50 V at the load", turbine.load.getU() > 25 && turbine.load.getU() < 53, String.format("%.1f V", turbine.load.getU()));
                    ctx.check(id + ": the condenser keeps the water (under 1 mB/s)", used < 60, String.format("%.1f mB in 60 s", used));
                }
                case "compact_turbine_short_of_heat" -> {
                    ctx.check(id + ": 1 kW of heat gives ~100 W", p > 80 && p < 110, String.format("%.0f W", p));
                    ctx.check(id + ": it says it is short of heat", turbine.state().contains("short of heat"), turbine.state());
                }
                case "compact_turbine_no_load_bypass" -> {
                    ctx.check(id + ": no load, a stone furnace's 3 kW: it doesn't vent", turbine.ventW == 0 && peakC < 180, String.format("%.0f C peak", peakC));
                    ctx.check(id + ": the bypass takes it", turbine.bypassW > 2000, String.format("%.0f W", turbine.bypassW));
                    ctx.check(id + ": little water (the condenser returns the bypass)", used < 60, String.format("%.1f mB in 60 s", used));
                }
                case "compact_turbine_vents_surplus" -> {
                    ctx.check(id + ": 6 kW with no load vents", turbine.ventW > 1500 && c > 180, String.format("%.0f W at %.0f C", turbine.ventW, c));
                    ctx.check(id + ": venting costs water", used > 100, String.format("%.1f mB in 60 s", used));
                }
                case "compact_turbine_dry" -> {
                    ctx.check(id + ": dry, no power", p == 0 && turbine.source.getEffectiveP() < 1, String.format("%.1f W", p));
                    ctx.check(id + ": it says it is out of water", turbine.state().contains("out of water"), turbine.state());
                }
                case "compact_turbine_cold_start" ->
                    ctx.check(id + ": from cold on 1 kW, ~100 W within 3 minutes", p > 70, String.format("%.0f W at %.0f C", p, c));
                default -> {
                }
            }
        }
    }

    /** Water by pipe on its top flange: an empty tank takes it in; a nearly full one shuts its float valve. */
    static final class CompactTurbinePipe implements TestCase {
        mods.eln.oil.CompactTurbineElement empty, full;
        mods.eln.sim.IProcess feed;

        public String id() {
            return "compact_turbine_pipe_feed";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            empty = (mods.eln.oil.CompactTurbineElement) l.device(36 + (82 << 6), 1, 1);
            full = (mods.eln.oil.CompactTurbineElement) l.device(36 + (82 << 6), 3, 1);
            full.water = 3500;
            mods.eln.Eln.simulator.addSlowProcess(feed = x -> {
                // (a pump on each flange: water kept at 3 bar)
                for (var c : new mods.eln.oil.CompactTurbineElement[]{empty, full})
                    if (c.inlet != null)
                        {
                        // (into the run's lump, which the fluid step works on: water put into the flange's own node was lost
                        // or not, by where this process ran in the slow list: the test failed by its place in the grid)
                        var n = mods.eln.fluid.FluidWorld.nodeFor(c.inlet);
                        for (int k = 0; k < 100 && n.P() < 3e5; k++) mods.eln.sim.fluid.EaUnits.addWater(n, 2e-5, 300, false);
                    }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(feed);
            ctx.line("compact turbine pipe: empty " + empty.getWaila() + "; full " + full.getWaila());
            ctx.check("compact turbine: an empty one fills by pipe", empty.water > 100, String.format("%.0f mB", empty.water));
            ctx.check("compact turbine: a nearly full one takes none (float valve)", full.water <= 3500.01 && full.water > 3400,
                String.format("%.1f mB", full.water));
            ctx.check("compact turbine: the tank shows its water", empty.tank.water() == (int) Math.floor(empty.water), empty.tank.water() + "");
        }
    }

    /**
     * A sealed plant heated hard (Baughn: no caps; the pipe bursts, not the boiler): a boiler with one block of iron pipe
     * on its top port, capped. The pressure climbs past 10 bar without pause, the pipe bursts at 40 and vents, and the
     * boiler survives.
     */
    static final class SealedPlant implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.sim.IProcess fire;
        double peak;
        net.minecraft.core.BlockPos pipeAt;

        public String id() {
            return "fluid_sealed_plant_bursts_its_pipe";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            boiler.thermal.Tc = 225 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);   // (225 C: it bursts at ~40 bar, ~250 C; the start was 175 C when numeric superheat in the pipe derated it early)
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            pipeAt = ctx.at(1, 2, 1);
            mods.eln.node.voxel.PipeKitItem.place(ctx.level(), pipeAt, new mods.eln.node.voxel.PipePart(0, new int[]{8, 0, 8, 8, 16, 8}), null);
            mods.eln.node.voxel.PipeKitItem.place(ctx.level(), pipeAt.above(), new mods.eln.node.voxel.PipePart(0, new int[]{8, 0, 8, 8, 12, 8}), null);
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                boiler.thermal.moveEnergyTo(6000 * t);
                peak = Math.max(peak, boiler.vessel.P());
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            int bursts = 0;
            for (var at : new net.minecraft.core.BlockPos[]{pipeAt, pipeAt.above()})
                if (ctx.level().getBlockEntity(at) instanceof mods.eln.node.voxel.VoxelNodeEntity e && e.getNode() instanceof mods.eln.node.voxel.VoxelNode v
                    && v.parts.get(0) instanceof mods.eln.node.voxel.PipePart p && p.burst) bursts++;
            boolean burst = bursts == 1;
            ctx.line("fluid: sealed plant " + boiler.getWaila() + String.format(", peak %.1f bar", peak / 1e5));
            ctx.check("fluid: the pressure rose past 10 bar (no cap)", peak > 20e5, String.format("peak %.1f bar", peak / 1e5));
            ctx.check("fluid: one piece of the iron run burst (not the whole run)", burst, bursts + " burst");
            ctx.check("fluid: the crack lets the pressure down (the heat keeps coming)", boiler.vessel.P() < 0.9 * peak, String.format("%.1f bar now", boiler.vessel.P() / 1e5));
            ctx.check("fluid: the boiler survived", !boiler.node.isDestructing() && !boiler.isBurnt(), "");
        }
    }

    /**
     * A boiler boiled dry on a hot shell (420 C), a capped iron pipe on top (AdventurAgent: a dry boiler filled its pipes
     * with "air" at up to 1800 C, the pressure swinging 0.5-3.6 bar second to second): its gas stays at or under the
     * shell's temperature, the pipe's too, and the pressure steady.
     */
    static final class DryBoiler implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.sim.IProcess fire;
        net.minecraft.core.BlockPos pipeAt;
        double hotGas, hotPipe, time, lo = 1e9, hi;

        public String id() {
            return "fluid_dry_boiler_stays_sane";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            boiler.thermal.Tc = 420 - mods.eln.sim.PhysicalConstant.Tamb;
            boiler.fromTanks(0, 0);
            boiler.showTanks();
            pipeAt = ctx.at(1, 2, 1);
            mods.eln.node.voxel.PipeKitItem.place(ctx.level(), pipeAt, new mods.eln.node.voxel.PipePart(0, new int[]{8, 0, 8, 8, 12, 8}), null);
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                boiler.thermal.Tc = 420 - mods.eln.sim.PhysicalConstant.Tamb;   // (a furnace holding the shell hot)
                time += t;
                if (boiler.vessel == null) return;
                hotGas = Math.max(hotGas, boiler.vessel.T() - 273.15);
                var n = (mods.eln.node.voxel.VoxelNode) ((mods.eln.node.voxel.VoxelNodeEntity) ctx.level().getBlockEntity(pipeAt)).getNode();
                var pipe = mods.eln.fluid.FluidWorld.nodeFor(((mods.eln.node.voxel.PipePart) n.parts.get(0)).fluid);
                hotPipe = Math.max(hotPipe, pipe.T() - 273.15);
                if (time > 20) {
                    lo = Math.min(lo, pipe.P());
                    hi = Math.max(hi, pipe.P());
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            ctx.line(String.format("dry boiler: gas up to %.0f C, pipe up to %.0f C, pipe %.2f-%.2f bar after 20 s; %s", hotGas, hotPipe, lo / 1e5, hi / 1e5, boiler.getWaila()));
            ctx.check("dry boiler: its gas stays at or under the shell's 420 C", hotGas < 425, String.format("%.0f C", hotGas));
            ctx.check("dry boiler: its pipe too", hotPipe < 425, String.format("%.0f C", hotPipe));
            ctx.check("dry boiler: the pressure holds steady", hi - lo < 0.1e5, String.format("%.2f-%.2f bar", lo / 1e5, hi / 1e5));
        }
    }

    /** The same sealed plant with a safety valve on its pipe: it lifts at 10 bar and whistles; the pipe holds. */
    static final class SafetyValvePlant implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.sim.IProcess fire;
        double peak, time, lo = 1e9, hi, lastLift, liftSum;
        int flips;
        net.minecraft.core.BlockPos pipeAt;
        String err;
        mods.eln.node.voxel.SafetyValvePart valve;
        final double heat;
        final int ticks;
        final String id;

        SafetyValvePlant(double heat, int ticks, String id) {
            this.heat = heat;
            this.ticks = ticks;
            this.id = id;
        }

        public String id() {
            return id;
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return ticks;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            boiler.thermal.Tc = 175 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            pipeAt = ctx.at(1, 2, 1);
            mods.eln.node.voxel.PipeKitItem.place(ctx.level(), pipeAt, new mods.eln.node.voxel.PipePart(0, new int[]{8, 0, 8, 8, 12, 8}), null);
            var n = (mods.eln.node.voxel.VoxelNode) ((mods.eln.node.voxel.VoxelNodeEntity) ctx.level().getBlockEntity(pipeAt)).getNode();
            err = n.addSafetyValveNear(0.5, 8 / 16.0, 0.5);
            for (var p : n.parts) if (p instanceof mods.eln.node.voxel.SafetyValvePart sv) valve = sv;
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> {
                boiler.thermal.moveEnergyTo(heat * t);
                peak = Math.max(peak, boiler.vessel.P());
                time += t;
                // the last 20 s (it lifts after ~25 s): the boiler's swing, and how often the valve goes from shut to over half open
                if (time > ticks / 20.0 - 20 && valve != null) {
                    lo = Math.min(lo, boiler.vessel.P());
                    hi = Math.max(hi, boiler.vessel.P());
                    if (lastLift < 0.02 && valve.lift >= 0.02) flips++;
                    lastLift = valve.lift;
                    liftSum += valve.lift * t;
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            var n = (mods.eln.node.voxel.VoxelNode) ((mods.eln.node.voxel.VoxelNodeEntity) ctx.level().getBlockEntity(pipeAt)).getNode();
            boolean burst = false;
            for (var p : n.parts) if (p instanceof mods.eln.node.voxel.PipePart pp && pp.burst) burst = true;
            ctx.line("fluid: plant with a safety valve " + boiler.getWaila() + String.format(", peak %.1f bar; last 20 s %.2f-%.2f bar, %d openings, mean lift %.3f, lift %.2f",
                peak / 1e5, lo / 1e5, hi / 1e5, flips, liftSum / 20, valve == null ? -1 : valve.lift));
            ctx.check("fluid: a safety valve fits on the pipe", err == null, "" + err);
            ctx.check("fluid: it holds the plant near its 10 bar setting", peak > 9e5 && peak < 12e5, String.format("peak %.1f bar", peak / 1e5));
            ctx.check("fluid: the pipe doesn't burst", !burst, "");
            // (Baughn 2026-10-27) its screen's slider sets the pressure (bar), within its range
            if (heat < 5000 && valve != null) {
                double[] got = new double[2];
                float[] send = {12.5f, 500f};
                for (int k = 0; k < 2; k++)
                    try {
                        var bos = new java.io.ByteArrayOutputStream();
                        var out = new java.io.DataOutputStream(bos);
                        out.writeByte(mods.eln.node.voxel.VoxelNode.PART_PACKET);
                        out.writeByte(n.parts.indexOf(valve));
                        out.writeFloat(send[k]);
                        n.networkUnserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bos.toByteArray())),
                            (net.minecraft.server.level.ServerPlayer) net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level()));
                        got[k] = valve.setPa;
                    } catch (java.io.IOException e) {
                        got[k] = -1;
                    }
                ctx.check("fluid: the safety valve's slider sets 12.5 bar, and 500 is held to its 150 bar top",
                    Math.abs(got[0] - 12.5e5) < 1 && Math.abs(got[1] - 150e5) < 1, got[0] + " / " + got[1]);
            }
            // (Baughn 2026-10-27: it pulsed; it acts on the averaged pressure now) relieving 8 kW, it holds steady
            if (heat > 5000) ctx.check("fluid: relieving, it holds the boiler steady (no pulsing)", flips <= 1 && hi - lo < 0.5e5,
                String.format("%d openings, %.2f-%.2f bar in the last 20 s", flips, lo / 1e5, hi / 1e5));
        }
    }

    /** A burst pipe's jet scalds what stands in it (Baughn: walking through it should kill you). */
    static final class SteamJetScalds implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.sim.IProcess fire;
        net.minecraft.world.entity.LivingEntity pig;
        float start;

        public String id() {
            return "fluid_steam_jet_scalds";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 1400;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 2, 1);
            boiler.thermal.Tc = 240 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            var at = ctx.at(2, 2, 1);
            mods.eln.node.voxel.PipeKitItem.place(ctx.level(), at, new mods.eln.node.voxel.PipePart(0, new int[]{8, 0, 8, 8, 12, 8}), null);
            // walls around the pipe but on one side: the jet goes there, where the pig stands
            for (var d : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST})
                ctx.level().setBlock(at.relative(d), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            pig = (net.minecraft.world.entity.LivingEntity) net.minecraft.world.entity.EntityTypes.PIG.create(ctx.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            pig.setPos(at.getX() + 1.5, at.getY(), at.getZ() + 0.5);
            if (pig instanceof net.minecraft.world.entity.Mob m) m.setNoAi(true);
            ctx.level().addFreshEntity(pig);
            start = pig.getHealth();
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(8000 * t));
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            ctx.line("fluid: jet test boiler " + boiler.getWaila() + ", pig " + pig.getHealth() + "/" + start + (pig.isRemoved() ? " (dead)" : ""));
            ctx.check("fluid: the pig in the jet was scalded (dead or hurt)", pig.isDeadOrDying() || pig.isRemoved() || pig.getHealth() < start - 4, pig.getHealth() + "");
            if (!pig.isRemoved()) pig.discard();
        }
    }

    /** An old save (tanks only, no vessel): the vessel starts from the tanks' water and steam. */
    static final class OldBoilerSave implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        double w, l;

        public String id() {
            return "fluid_boiler_old_save";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l0 = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l0.device(8 + (82 << 6), 1, 1);
            var tag = new net.minecraft.nbt.CompoundTag();
            boiler.writeToNBT(tag);
            tag.remove("vesselW");
            tag.remove("vesselA");
            tag.remove("vesselU");
            tag.putInt("boilerwater", 4000);
            tag.putInt("boilersteam", 1000);
            boiler.tanks.readFromNBT(tag, "boiler");
            boiler.fromTanks(boiler.tanks.water(), boiler.tanks.steam());
            w = boiler.vessel.mWater;
            l = boiler.vessel.mLiquid();
        }

        public void measure(TestContext ctx) {
            double k = mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
            double extra = w - (4000 * k + 1000e-6);   // (the fill also saturates the air space with vapour: < 0.2 g)
            ctx.check("fluid: an old boiler's water and steam are in its vessel", extra >= -1e-9 && extra < 2e-4, w + " kg, " + extra + " over");
            ctx.check("fluid: most of it liquid", l > 0.9 * 4000 * k, l + " kg");
        }
    }

    /** Buckets on EA fluid devices (Baughn: manual bootstrap): water into the boiler; crude out of the pump. */
    static final class Buckets2 implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.OilPumpElement pump;
        ItemStack afterWater, afterEmpty, afterSteamTry;
        int waterAfter, crudeAfter;

        public String id() {
            return "oil_device_buckets";
        }

        public int width() {
            return 6;
        }

        static ItemStack use(TestContext ctx, mods.eln.node.transparent.TransparentNodeElement e, ItemStack held) {
            var p = Wp10bCases.player(ctx);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
            try {
                e.node.onBlockActivated(p, mods.eln.misc.Direction.YP, 0.5F, 1F, 0.5F);
                return p.getMainHandItem().copy();
            } finally {
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            pump = (mods.eln.oil.OilPumpElement) l.device(0 + (81 << 6), 4, 1);
            afterWater = use(ctx, boiler, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            waterAfter = boiler.tanks.water();
            afterSteamTry = use(ctx, boiler, new ItemStack(net.minecraft.world.item.Items.BUCKET));   // water stays in, steam has no bucket
            pump.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.CRUDE.get(), 1500), true);
            afterEmpty = use(ctx, pump, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            crudeAfter = pump.tank.getFluidAmount();
        }

        public void measure(TestContext ctx) {
            ctx.check("oil: a water bucket fills the boiler and comes back empty", waterAfter == 1000 && afterWater.is(net.minecraft.world.item.Items.BUCKET),
                waterAfter + " mB, hand " + afterWater);
            // (a little of it evaporates into the vessel's air space: since the pipes' phase 2 the tanks show the vessel)
            ctx.check("oil: an empty bucket can't take the boiler's water", boiler.tanks.water() >= 990 && afterSteamTry.is(net.minecraft.world.item.Items.BUCKET),
                boiler.tanks.water() + " mB, hand " + afterSteamTry);
            ctx.check("oil: an empty bucket takes 1000 mB of crude from the pump", crudeAfter == 500 && afterEmpty.is(mods.eln.oil.Oil.CRUDE_BUCKET.get()),
                crudeAfter + " mB, hand " + afterEmpty);
        }
    }

    /**
     * The fractionating column with 4 segments: crude at 400 C distils with the heat above 350 C (60 J/mB) into gas,
     * gasoline, diesel, heavy oil (top to bottom) and residue in the base, at 5/30/30/25/10%.
     */
    static final class Column implements TestCase {
        mods.eln.oil.ColumnBaseElement base;
        final mods.eln.oil.ColumnSegmentElement[] seg = new mods.eln.oil.ColumnSegmentElement[4];
        static final double TREF = mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius;

        public String id() {
            return "oil_column";
        }

        public int width() {
            return 5;
        }

        public int height() {
            return 7;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            base = (mods.eln.oil.ColumnBaseElement) l.device(0 + (83 << 6), 2, 1);
            for (int i = 0; i < 4; i++) seg[i] = (mods.eln.oil.ColumnSegmentElement) ctx.placeTransparent(4 + (83 << 6), ctx.at(2, 2 + i, 1));
            base.tanks.setCrude(8000);
            base.thermal.Tc = 400 - TREF;
        }

        public void measure(TestContext ctx) {
            int used = 8000 - base.tanks.crude();
            int[] got = new int[5];
            for (int i = 0; i < 4; i++) got[3 - i] = seg[i].tank.getFluidAmount();   // top segment = gas
            got[4] = base.tanks.residue();
            ctx.line("oil: column " + base.getWaila() + ", segments (top..bottom) " + java.util.Arrays.toString(got));
            // 50 K x 0.4 kJ/K over the distilling point = 20 kJ = ~333 mB of crude (less the losses while it waited for its scan)
            ctx.check("oil: the heat above 350 C distils ~330 mB of crude", used > 260 && used <= 335, used + " mB");
            ctx.check("oil: the base cools to the distilling point", Math.abs(base.thermal.Tc + TREF - 350) < 3, (base.thermal.Tc + TREF) + " C");
            String[] names = {"gas", "gasoline", "diesel", "heavy oil", "residue"};
            double[] y = {0.05, 0.30, 0.30, 0.25, 0.10};
            for (int c = 0; c < 5; c++)
                ctx.check("oil: column " + names[c] + " is " + (int) (y[c] * 100) + "% of the crude", Math.abs(got[c] - used * y[c]) <= 2, got[c] + " of " + used);
            ctx.check("oil: the top segment holds natural gas", seg[3].tank.getFluid().getFluid().isSame(mods.eln.oil.Oil.Cut.NATURALGAS.source.get()), "" + seg[3].tank.getFluid().getFluid());
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, seg[1].node.coordonate.pos(), null);
            ctx.check("oil: a segment's tank is its fluid capability", cap == seg[1].tank, "" + cap);
        }
    }

    /** Fewer segments: the missing cuts fold into the next heavier one (MACHINES-PLAN table). */
    static final class ColumnFold implements TestCase {
        public String id() {
            return "oil_column_fold";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            double[][] want = {
                {0, 0, 0, 0, 0},
                {0, 0, 0, 0.90, 0.10},
                {0, 0.35, 0, 0.55, 0.10},
                {0.05, 0.30, 0.30, 0, 0.35},
                {0.05, 0.30, 0.30, 0.25, 0.10},
            };
            for (int n = 0; n <= 4; n++) {
                double[] f = mods.eln.oil.ColumnBaseElement.fractions(n);
                boolean ok = true;
                for (int c = 0; c < 5; c++) ok &= Math.abs(f[c] - want[n][c]) < 1e-9;
                ctx.check("oil: column with " + n + " segments: cuts", ok, java.util.Arrays.toString(f));
            }
        }
    }

    /** The flare stack burns what it's given at 250 mB/s. */
    static final class Flare implements TestCase {
        mods.eln.oil.FlareStackElement flare;

        public String id() {
            return "oil_flare";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            flare = (mods.eln.oil.FlareStackElement) l.device(8 + (83 << 6), 2, 1);
            flare.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.Cut.RESIDUE.source.get(), 1000), true);
        }

        public void measure(TestContext ctx) {
            int burnt = 1000 - flare.tank.getFluidAmount();
            ctx.check("oil: the flare stack burns ~250 mB/s (2 s)", burnt >= 400 && burnt <= 550, burnt + " mB");
        }
    }

    /** Generated lakes (LakeShape): mostly 5-20k buckets (design: a tower-fortnight or a few), every one a real lake. */
    static final class LakeSizes extends Base {
        public String id() {
            return "oil_lake_sizes";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var rnd = new java.util.Random(12345);
            int inRange = 0, n = 12;
            StringBuilder sb = new StringBuilder(), outcrops = new StringBuilder();
            for (int i = 0; i < n; i++) {
                int r0 = 28 + rnd.nextInt(29);
                float aspect = 0.7f + 0.6f * rnd.nextFloat();
                var shape = new mods.eln.oil.LakeShape(i, 0, 0, 0, Math.round(r0 * aspect), Math.round(r0 / aspect), rnd.nextInt());
                int[][] cols = shape.columns();
                long cells = 0;
                for (int k = 0; k < cols[1].length; k++) cells += Integer.bitCount(cols[1][k]);
                int out = 0, tarCols = 0, area = 0;
                for (int x = shape.minX(); x < shape.minX() + shape.sizeX(); x++)
                    for (int z = shape.minZ(); z < shape.minZ() + shape.sizeZ(); z++) {
                        if (shape.dist(x, z) <= 1) area++;
                        if (mods.eln.oil.OilLakePiece.tarColumn(shape, x, z)) tarCols++;
                        if (mods.eln.oil.OilLakePiece.outcrop(shape, x, z)) out++;
                    }
                outcrops.append(out).append('/').append(tarCols).append(' ');
                // Baughn: the lake must show at the surface; a few small outcrops, not a slab
                ctx.check("oil: lake " + i + " has tar sand outcrops at the surface", out >= 8 && out <= area * 0.04, out + " columns of " + area);
                sb.append(cells).append(' ');
                if (cells >= 4000 && cells <= 25000) inRange++;
                ctx.check("oil: lake " + i + " has oil", cells > 500, "" + cells);
            }
            ctx.line("oil: lake sizes (buckets): " + sb);
            ctx.line("oil: tar sand outcrop / buried columns per lake: " + outcrops);
            ctx.check("oil: most lakes hold 4-25k buckets", inRange >= n * 2 / 3, inRange + " of " + n + ": " + sb);
            // a lake below y 0 (negative heights): full means the surface at its top
            var deep = ReservoirData.get(ctx.level()).create(0, 0, 2, 1, new int[]{-21, -22}, new int[]{0b111, 0b1111}, Long.MAX_VALUE);
            ctx.check("oil: below y 0, a full lake's surface is its top (y -18)", deep.surface() == -18 * 8, "" + deep.surface());
            Reservoirs.take(ctx.level(), deep, 2000, false);
            ctx.check("oil: below y 0, draining 2 of 7 cells lowers it to y -19", deep.surface() == -19 * 8, "" + deep.surface());
            ReservoirData.get(ctx.level()).remove(deep);
            ctx.check("oil: the lake structure is registered",
                ctx.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "oil_lake")).isPresent(), "");
        }
    }

    /** A chunk swept after the lake moved (as when it loads) shows the current level. */
    static final class CatchUp extends Base {
        public String id() {
            return "oil_lake_catch_up";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            int y1 = ctx.at(0, 1, 0).getY();
            Reservoirs.take(ctx.level(), r, r.amount() - r.volumeAt((y1 + 1) * 8 + 2), false);
            // stale blocks: still full (as if the chunk was unloaded while the lake drained)
            ctx.check("oil: before the sweep, stale", level(ctx, 1, 2, 0) == 8, "");
            sweepAll(ctx, r);
            ctx.check("oil: after the sweep, at the record's level", level(ctx, 1, 2, 0) == 2 && level(ctx, 7, 2, 2) == 2,
                level(ctx, 1, 2, 0) + " / " + level(ctx, 7, 2, 2));
            cleanup(ctx, r);
        }
    }
}
