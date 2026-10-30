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
}
