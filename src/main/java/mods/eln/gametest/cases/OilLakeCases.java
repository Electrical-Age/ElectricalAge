package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.Oil;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Oil lakes, todo #138 (Baughn 2026-10-30, Argon's a3): deeper lakes, a seepage tail, enhanced recovery by steam
 * injection, the heavy cuts burnable.
 */
public final class OilLakeCases {
    private OilLakeCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new HeavyCutsBurn());
        cases.add(new Recovery(Recovery.Mode.SEEPAGE));
        cases.add(new Recovery(Recovery.Mode.STEAM));
        cases.add(new Recovery(Recovery.Mode.WEAK_STEAM));
        cases.add(new Recovery(Recovery.Mode.TWO_PUMPS));
    }

    /**
     * The heavy cuts are fuel (Baughn: burn or crack them): the fuel heat furnace takes heavy oil and residue and gets
     * their heat from them; the gas turbine takes heavy oil but not residue (too tarry for an engine).
     */
    static final class HeavyCutsBurn implements TestCase {
        String bad = "";

        public String id() {
            return "oil_heavy_cuts_burn";
        }

        public int width() {
            return 5;
        }

        public void build(TestContext ctx) {
            var furnace = ctx.placeTransparent(Wp10bCases.FUEL_HEAT_FURNACE, ctx.at(1, 1, 1));
            var gt = (mods.eln.mechanical.GasTurbineElement) ctx.placeTransparent(GasPlantCases.GAS_TURBINE, ctx.at(3, 1, 1));
            for (Oil.Cut c : new Oil.Cut[]{Oil.Cut.HEAVYOIL, Oil.Cut.RESIDUE}) {
                var tank = (mods.eln.fluid.PreciseElementFluidHandler) furnace.getFluidHandler();
                tank.drain(tank.getFluidAmount(), true);
                int in = tank.fill(new FluidStack(c.source.get(), 10), true);
                double heat = in > 0 ? tank.drainEnergy(1e9) : 0;
                double perMb = mods.eln.fluid.FuelRegistry.INSTANCE.heatEnergyPerMilliBucket(c.source.get());
                if (in != 10) bad += "the furnace refuses " + c + "; ";
                else if (Math.abs(heat - 10 * perMb) > 1e-6 * heat || perMb <= 0) bad += String.format("%s gives %.0f J for 10 mB (%.0f J/mB); ", c, heat, perMb);
                int toGt = gt.tank.fill(new FluidStack(c.source.get(), 10), false);
                if (c == Oil.Cut.HEAVYOIL && toGt != 10) bad += "the gas turbine refuses heavy oil; ";
                if (c == Oil.Cut.RESIDUE && toGt != 0) bad += "the gas turbine takes residue; ";
            }
        }

        public void measure(TestContext ctx) {
            ctx.check("heavy oil and residue fire the fuel heat furnace; residue isn't an engine's fuel", bad.isEmpty(), bad);
        }
    }

    /**
     * A depleted lake (15 % of its capacity, under the 20 % tail) under the test floor, a 50V oil pump on well casing
     * down to it:
     * <ul>
     * <li>SEEPAGE: it never runs dry: it seeps 18 % of a 200V pump's nominal rate (OilRecovery.seepRate), the lake stays
 * where it is, Jade says so;</li>
 * <li>TWO_PUMPS: two pumps on it share that one budget (Baughn 2026-10-30);</li>
     * <li>STEAM: steam at 3 bar down a second casing through a wellhead (more than the well's head) pays for crude: the
     * pump runs at its full rate again, Jade says boosted, the steam taken matches the crude at steamKgPerBucket;</li>
     * <li>WEAK_STEAM: steam under the well's head doesn't go down: nothing injected, seepage as before.</li>
     * </ul>
     */
    static final class Recovery implements TestCase {
        enum Mode { SEEPAGE, STEAM, WEAK_STEAM, TWO_PUMPS }

        final Mode mode;
        mods.eln.oil.OilPumpElement pump, pump2;
        mods.eln.oil.SteamInjectionElement well;
        mods.eln.reservoir.Reservoir r;
        mods.eln.sim.fluid.FluidDevice fx;
        mods.eln.sim.IProcess watch;
        long amount0;
        int tick, tank0, tank20;
        double window, steam0;

        Recovery(Mode mode) {
            this.mode = mode;
        }

        public String id() {
            return "oil_lake_" + mode.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 500;
        }

        public void build(TestContext ctx) {
            var level = ctx.level();
            net.minecraft.core.BlockPos under = ctx.at(0, 0, 0).below();
            // a lake 6 x 3 columns, 4 cells deep, under the test floor (cleared: the floor's barrier layer would be a dam)
            int[] floor = new int[18], mask = new int[18];
            for (int x = 0; x < 6; x++)
                for (int z = 0; z < 3; z++) {
                    for (int y = 0; y < 4; y++) level.setBlock(under.offset(x, -y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    floor[x * 3 + z] = under.getY() - 3;
                    mask[x * 3 + z] = 0b1111;
                }
            r = mods.eln.oil.OilReservoirs.create(level, null, under.getX(), under.getZ(), 6, 3, floor, mask, Long.MAX_VALUE);
            mods.eln.reservoir.Reservoirs.setAmount(level, r, (long) (0.15 * r.capacity()));
            mods.eln.reservoir.Reservoirs.sweep(level, r, under.getX() >> 4, under.getZ() >> 4);
            amount0 = r.amount();
            for (int x : new int[]{1, 4}) level.setBlock(ctx.at(x, 0, 1), mods.eln.oil.OilWorldgen.WELL_CASING.get().defaultBlockState(), 3);
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pump = (mods.eln.oil.OilPumpElement) l.device(0 + (81 << 6), 1, 1);
            l.feed(ctx.at(1, 1, 1), pump.front.left());
            if (mode == Mode.TWO_PUMPS) {
                pump2 = (mods.eln.oil.OilPumpElement) l.device(0 + (81 << 6), 4, 1);
                l.feed(ctx.at(4, 1, 1), pump2.front.left());
            } else if (mode != Mode.SEEPAGE) {
                well = (mods.eln.oil.SteamInjectionElement) ctx.placeTransparent(16 + (81 << 6), ctx.at(4, 1, 1));
                net.minecraft.core.BlockPos above = ctx.at(4, 2, 1);
                fx = GasPlantCases.fixtures(well.fluidLevel(), new Object[]{above, 0, GasPlantCases.steam(above.getY(), mode == Mode.STEAM ? 3e5 : 0.9e5)});
            }
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 200) {
                    tank0 = ((mods.eln.fluid.ElementFluidHandler) pump.getFluidHandler()).getFluidAmount();
                    tank20 = pump2 == null ? 0 : ((mods.eln.fluid.ElementFluidHandler) pump2.getFluidHandler()).getFluidAmount();
                    steam0 = well == null ? 0 : field(well, "totalKg");
                }
                if (tick > 200) window += dt;
            });
        }

        static double field(Object o, String name) {
            try {
                var f = o.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.getDouble(o);
            } catch (ReflectiveOperationException e) {
                return Double.NaN;
            }
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            if (fx != null) mods.eln.fluid.FluidWorld.remove(fx);
            int got = ((mods.eln.fluid.ElementFluidHandler) pump.getFluidHandler()).getFluidAmount() - tank0;
            int got2 = pump2 == null ? 0 : ((mods.eln.fluid.ElementFluidHandler) pump2.getFluidHandler()).getFluidAmount() - tank20;
            double rate = got / window, nominal = 1000 / mods.eln.oil.OilPumpDescriptor.energyPerMb(false, 1);
            double steam = well == null ? 0 : field(well, "totalKg") - steam0;
            var info = pump.getWaila();
            ctx.line(String.format("%s: pump %.2f mB/s (nominal %.2f, %.0f%%); lake %d -> %d mB of %d; steam %.4f kg; pump %s%s", mode, rate, nominal,
                100 * rate / nominal, amount0, r.amount(), r.capacity(), steam, info, well == null ? "" : "; wellhead " + well.getWaila()));
            ctx.check(mode + ": the lake stays at its tail level", r.amount() == amount0, r.amount() + " vs " + amount0);
            String lake = String.valueOf(info.get("Lake"));
            if (mode == Mode.STEAM) {
                ctx.check("steam: the pump runs at its full rate again (over 80 % of nominal)", rate > 0.8 * nominal, String.format("%.2f of %.2f mB/s", rate, nominal));
                ctx.check("steam: Jade says boosted", lake.contains("boosted"), lake);
                ctx.check("steam: steam went down the well", steam > 0, String.format("%.4f kg", steam));
            } else {
                // (one budget for the lake, shared by its pumps: Baughn 2026-10-30)
                double seep = mods.eln.oil.OilRecovery.seepRate(), total = (got + got2) / window;
                ctx.check(mode + ": it seeps: its pumps get the lake's seepage between them (+-20 %)", Math.abs(total - seep) < 0.2 * seep,
                    String.format("%.2f + %.2f vs %.2f mB/s", rate, got2 / window, seep));
                if (mode == Mode.TWO_PUMPS) ctx.check("two pumps: each gets some", got > 0 && got2 > 0, got + " / " + got2 + " mB");
                ctx.check(mode + ": Jade says seeping", lake.contains("seeping"), lake);
                if (mode == Mode.WEAK_STEAM) ctx.check("weak steam: under the well's head nothing goes down", steam == 0, String.format("%.4f kg", steam));
            }
            mods.eln.reservoir.ReservoirData.get(ctx.level()).remove(r);
        }
    }
}
