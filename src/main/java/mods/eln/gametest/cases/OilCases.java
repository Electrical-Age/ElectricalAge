package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.Oil;
import mods.eln.oil.Reservoir;
import mods.eln.oil.ReservoirData;
import mods.eln.oil.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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
        cases.add(new SteamTurbine());
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

    /**
     * Electric heater (1 kW) against a boiler with water: the boiler heats to 100 C, then boils: steam = heat / 175 J per mB
     * of water x 67.5. Over 30 s of boiling at ~1 kW: ~10 kJ/s... ~380 mB/s of steam at the full kilowatt.
     */
    static final class Boiler implements TestCase {
        mods.eln.oil.ElectricHeaterElement heater;
        mods.eln.oil.BoilerElement boiler;

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
            boiler.tanks.fillWater(8000);
            // both start at 99 C, near boiling: the test is about boiling, not the warm-up
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            heater.thermal.Tc = boiler.thermal.Tc;
        }

        public void measure(TestContext ctx) {
            int steam = boiler.tanks.steam(), water = boiler.tanks.water();
            ctx.line("oil: boiler: " + boiler.getWaila() + ", heater " + heater.getWaila());
            ctx.check("oil: the heater's back heats the boiler: steam made", steam > 2000, steam + " mB steam");
            // 961 W at 175 J per mB of water: at most ~370 mB/s of steam once the heater is warm
            ctx.check("oil: the steam meter is within what the heat allows", boiler.steamRate > 50 && boiler.steamRate <= 961 / 175.0 * 67.5 * 1.05, boiler.steamRate + " mB/s");
            ctx.check("oil: 67.5 mB of steam per mB of water", Math.abs(steam - (8000 - water) * 67.5) < 70, steam + " vs " + (8000 - water) * 67.5);
            // energy: 60 s x ~1 kW = ~60 kJ, -> ~340 mB of water -> ~23 B of steam (less the heater's warm-up and losses)
            ctx.check("oil: no more steam than the heat allows", steam <= 60 * 1000 / 175.0 * 67.5 * 1.05, steam + " mB");
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, boiler.node.coordonate.pos(), null);
            ctx.check("oil: the boiler's tanks are its fluid capability", cap == boiler.tanks, "" + cap);
        }
    }

    /**
     * The 50V steam turbine: idle it keeps its steam at the open-circuit voltage; driving a 50V heater (2.5 ohm) it
     * turns steam into electricity at 40% of 2.257 J/mB, up to 1 kW (1108 mB/s).
     */
    static final class SteamTurbine implements TestCase {
        mods.eln.oil.SteamGeneratorElement idle, run;
        mods.eln.oil.ElectricHeaterElement heater;
        static final int FILL = 2000;

        public String id() {
            return "oil_steam_turbine";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 20;
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
            idle.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.STEAM.get(), FILL), true);
            run.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.STEAM.get(), FILL), true);
        }

        public void measure(TestContext ctx) {
            ctx.line("oil: steam turbine idle " + idle.getWaila() + ", running " + run.getWaila() + ", heater " + heater.getWaila());
            ctx.check("oil: an idle steam turbine keeps its steam", idle.tank.getFluidAmount() >= FILL - 1, idle.tank.getFluidAmount() + " mB");
            ctx.checkValue("oil: idle open-circuit voltage [V] (1.05 x 50)", idle.load.getU(), 52.5);
            int used = FILL - run.tank.getFluidAmount();
            // 1 s at up to 1 kW: at most 1108 mB; the output follows the tank (1 s), so a good part of the fill
            ctx.check("oil: the loaded turbine uses steam, at most its nominal 1108 mB/s", used > 500 && used <= 1108 * 1.1, used + " mB in 1 s");
            double p = run.source.getEffectiveP();
            ctx.check("oil: the heater gets the turbine's power, up to 1 kW", p > 100 && p <= 1000 * 1.05, String.format("%.0f W", p));
            // energy: what it delivered over the second can't beat 40% of the steam it used (allow the last step's lag)
            ctx.check("oil: no more electricity than 40% of the steam", p * 0.9 <= used * mods.eln.oil.SteamGeneratorDescriptor.EFFICIENCY * 2.257,
                String.format("%.0f W vs %d mB/s x 0.9 J", p, used));
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
            ctx.check("oil: an empty bucket can't take the boiler's water", boiler.tanks.water() == 1000 && afterSteamTry.is(net.minecraft.world.item.Items.BUCKET),
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
            // 50 K x 1 kJ/K over the distilling point = 50 kJ = ~833 mB of crude (less the losses while it waited for its scan)
            ctx.check("oil: the heat above 350 C distils ~800 mB of crude", used > 650 && used <= 840, used + " mB");
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
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                int r0 = 28 + rnd.nextInt(29);
                float aspect = 0.7f + 0.6f * rnd.nextFloat();
                var shape = new mods.eln.oil.LakeShape(i, 0, 0, 0, Math.round(r0 * aspect), Math.round(r0 / aspect), rnd.nextInt());
                int[][] cols = shape.columns();
                long cells = 0;
                for (int k = 0; k < cols[1].length; k++) cells += Integer.bitCount(cols[1][k]);
                sb.append(cells).append(' ');
                if (cells >= 4000 && cells <= 25000) inRange++;
                ctx.check("oil: lake " + i + " has oil", cells > 500, "" + cells);
            }
            ctx.line("oil: lake sizes (buckets): " + sb);
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
