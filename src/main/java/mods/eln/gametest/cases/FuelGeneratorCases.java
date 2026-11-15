package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FuelQuality;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.transparentnode.FuelGeneratorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Fuel Generator on a fuel vessel, with fuel quality (refinery M1, Baughn 2026-11-15). A 50 V generator on open
 * circuit (its minimum load, 0.1 P_nom at efficiency factor 1.375):
 * mode 0, gasoline: full power, the old tank's fuel use (1.375 x 0.1 P_nom / tank energy x 2000 mB a second), clean;
 * mode 1, gasoline with 10 % residue by mass: 80 % power (10 % too heavy for its 330-400 K window), and it fouls by
 * the engines' law (4x slower than a burner);
 * mode 2, a half-fouled one on gasoline: half power, it clears by the law; wool cleans it, one used;
 * mode 3, an old save (a tank of 0.75 of gasoline, and one of 0.5 of "lava", 1.7.10's default name): its fuel is in
 * the vessel, 1500 mB and 1000 mB of gasoline;
 * mode 4, an empty one under an open tank of gasoline (1 atm, a pipe end on its top flange): the head fills its vessel
 * past a bucket, and it runs on it;
 * mode 5, gasoline with 10 % diesel by mass: 80 % power (diesel, 530 K, is over its 400 K window; the gas turbine's
 * 720 K window would take it), and it burns clean;
 * mode 6, 40 % gasoline, 30 % diesel, 30 % heavy oil (60 % off its window: no power): it won't run; topped up with
 * gasoline (23 % off: 54 % power), a click starts it.
 */
public final class FuelGeneratorCases {
    private FuelGeneratorCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Quality("fuel_generator_gasoline", 0));
        cases.add(new Quality("fuel_generator_off_spec_derates", 1));
        cases.add(new Quality("fuel_generator_clean_fuel_clears", 2));
        cases.add(new Quality("fuel_generator_old_save", 3));
        cases.add(new Quality("fuel_generator_piped", 4));
        cases.add(new Quality("fuel_generator_diesel_derates", 5));
        cases.add(new Quality("fuel_generator_restarts_after_dilution", 6));
    }

    static final class Quality implements TestCase {
        final String id;
        final int mode;
        FuelGeneratorElement gen;
        long t0;
        double mb0, foul0, burnt0;
        String legacy = "not run", legacyLava = "not run";
        mods.eln.sim.fluid.FluidDevice fx;
        mods.eln.sim.fluid.FluidNode main;
        boolean refused, restarted;

        Quality(String id, int mode) {
            this.id = id;
            this.mode = mode;
        }

        public String id() {
            return id;
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(2, 1, 1);
            gen = (FuelGeneratorElement) ctx.placeTransparent(Wp10bCases.FUEL_GENERATOR_50V, p);
            var reg = SpeciesRegistry.get();
            var v = gen.inlet.vessel;
            if (mode == 3) {
                BlockPos q = ctx.at(1, 1, 1);
                ctx.placeTransparent(Wp10bCases.FUEL_GENERATOR_50V, q);
                ctx.after(2, () -> {
                    legacy = oldSave(ctx, p, 0.75, "gasoline", 1500);
                    legacyLava = oldSave(ctx, q, 0.5, "lava", 1000);
                });
                return;
            }
            // (1500 mB of gasoline; mode 1 with residue of a ninth of its mass: 10 % of the mix)
            double kg = 1500 * mods.eln.fluid.FluidSpecies.kgPerMb(reg.id("eln:gasoline"), false);
            EaUnits.add(v, reg.id("eln:gasoline"), kg, 293.15, false);
            if (mode == 1) EaUnits.add(v, reg.id("eln:residue"), kg / 9, 293.15, false);
            if (mode == 5) EaUnits.add(v, reg.id("eln:diesel"), kg / 9, 293.15, false);
            if (mode == 6) {
                // (200 mB of gasoline, by mass 40 %, diesel and heavy oil 30 % each)
                EaUnits.drainLiquid(v, reg.id("eln:gasoline"), v.liquid(reg.id("eln:gasoline")));
                double k = 200 * mods.eln.fluid.FluidSpecies.kgPerMb(reg.id("eln:gasoline"), false);
                EaUnits.add(v, reg.id("eln:gasoline"), k, 293.15, false);
                EaUnits.add(v, reg.id("eln:diesel"), k * 0.75, 293.15, false);
                EaUnits.add(v, reg.id("eln:heavy_oil"), k * 0.75, 293.15, false);
                v.update();
                Wp10bCases.click(ctx, gen, ItemStack.EMPTY);   // (it can't: no power on this)
                ctx.after(20, () -> {
                    refused = !gen.running() && gen.fuel.power() == 0;
                    // (4x as much gasoline again: 1.5 of 6.5 parts off its window, 23 %: 54 % power)
                    EaUnits.add(gen.inlet.vessel, reg.id("eln:gasoline"), 4 * k, 293.15, false);
                    gen.inlet.vessel.update();
                    Wp10bCases.click(ctx, gen, ItemStack.EMPTY);
                    restarted = gen.running();
                });
                return;
            }
            if (mode == 4) {
                // (an open tank of gasoline over its top flange instead: 1 m^3, 80 % full, 1 atm over the liquid)
                EaUnits.drainLiquid(v, reg.id("eln:gasoline"), v.liquid(reg.id("eln:gasoline")));
                main = new mods.eln.sim.fluid.FluidNode(1.0, ctx.at(2, 2, 1).getY(), 1.0, false).fill(0, mods.eln.sim.fluid.Water.P_ATM, 293.15);
                EaUnits.add(main, reg.id("eln:gasoline"), 0.8 * 740, 293.15, false);
                main.mAir *= 0.2;
                main.energy = main.energyAt(293.15);
                main.update();
                fx = GasPlantCases.fixtures(gen.fluidLevel(), new Object[]{ctx.at(2, 2, 1), 0, main});
                v.update();
                ctx.after(60, () -> Wp10bCases.click(ctx, gen, ItemStack.EMPTY));
                ctx.after(100, () -> {
                    t0 = ctx.world().getGameTime();
                    mb0 = gen.inlet.feedMb();
                    burnt0 = gen.burntMb;
                });
                return;
            }
            v.update();
            if (mode == 2) gen.foul = 0.5;
            Wp10bCases.click(ctx, gen, ItemStack.EMPTY);   // (switch on)
            ctx.after(100, () -> {
                t0 = ctx.world().getGameTime();
                mb0 = gen.inlet.feedMb();
                foul0 = gen.foul;
                burnt0 = gen.burntMb;
            });
        }

        /** Reload the generator at p from a save as the old one wrote it (a tank level and fluid name, no vessel). */
        static String oldSave(TestContext ctx, BlockPos p, double level, String fluid, double wantMb) {
            try {
                var r = mods.eln.gametest.Reload.node(ctx.level(), p, t -> {
                    var el = OilCases.FuelBySpecies.holding(t, "fuelInVesselW");
                    for (String k : new String[]{"W", "A", "U", "S", "Fv"}) {
                        el.remove("fuelInVessel" + k);
                        el.remove("fuelIn" + k);
                    }
                    for (String k : new String[]{"foul", "fuelLight", "fuelHeavy", "fuelCoking", "burntMb"}) el.remove(k);
                    el.putDouble("tankLevel", level);
                    el.putString("tankFluid", fluid);
                    return t;
                });
                var g = (FuelGeneratorElement) ((mods.eln.node.transparent.TransparentNode) r.fresh).element;
                double mb = g.inlet.feedMb();
                return Math.abs(mb - wantMb) < 0.01 * wantMb && g.foul == 0 ? "" : String.format("%.1f mB (want %.0f); %s", mb, wantMb, g.getWaila());
            } catch (ReflectiveOperationException e) {
                return e.toString();
            }
        }

        public void measure(TestContext ctx) {
            if (mode == 3) {
                ctx.check(id + ": an old save's 0.75 tank of gasoline is 1500 mB in its vessel", legacy.isEmpty(), legacy);
                ctx.check(id + ": an old save's 0.5 tank of \"lava\" (1.7.10's name) is 1000 mB of gasoline, not lost", legacyLava.isEmpty(), legacyLava);
                return;
            }
            if (mode == 6) {
                String what = String.format("refused %b, restarted %b, now on %b, rating %s, source %.1f W; %s", refused, restarted, gen.running(), gen.fuel, gen.sourceP(), gen.getWaila());
                ctx.line(id + ": " + what);
                ctx.check(id + ": 60 % off its window: no power, it won't run", refused, what);
                ctx.check(id + ": topped up with gasoline, a click starts it and it runs at 54 %", restarted && gen.running()
                    && Math.abs(gen.fuel.power() - (1 - 2 * 1.5 / 6.5)) < 0.01, what);
                return;
            }
            if (mode == 4) {
                mods.eln.fluid.FluidWorld.remove(fx);
                String what = String.format("vessel %.0f mB at %.2f bar, burnt %.2f mB; %s", gen.inlet.feedMb(), gen.inlet.vessel.P() / 1e5, gen.burntMb - burnt0, gen.getWaila());
                ctx.line(id + ": " + what);
                ctx.check(id + ": an open tank's head fills its vessel past a bucket (over 1000 mB)", gen.inlet.feedMb() > 1000, what);
                ctx.check(id + ": it runs on piped fuel", gen.running() && gen.burntMb - burnt0 > 1, what);
                return;
            }
            double t = (ctx.world().getGameTime() - t0) / 20.0, used = mb0 - gen.inlet.feedMb(), burnt = gen.burntMb - burnt0;
            double pNom = 300 * Eln.fuelGeneratorPowerFactor;
            double rate = 1.375 * 0.1 * pNom / (Eln.fuelGeneratorTankCapacity * pNom) * FuelGeneratorElement.TANK_MB;   // (mB/s)
            double power = mode == 1 || mode == 5 ? 0.8 : 1;
            double index = mode == 1 ? 0.1 : 0;
            double dFoul = gen.foul - foul0;
            // (load 0.1 of nominal: the minimum load)
            double expect = FuelQuality.foulRate(index, FuelQuality.ENGINE_BALANCE) * 0.1 * t / (FuelQuality.FOUL_SECONDS * FuelQuality.ENGINE_SLOWER);
            double q = power * (1 - gen.foul);
            ctx.line(String.format("%s: on %b, %.1f s, used %.3f mB (burnt %.3f; old tank %.3f), rating %s, fouling %.6f -> %.6f (d %.7f, law %.7f), source %.1f W of %.1f; %s",
                id, gen.running(), t, used, burnt, rate * t, gen.fuel, foul0, gen.foul, dFoul, expect, gen.sourceP(), pNom, gen.getWaila()));
            ctx.check(id + ": running", gen.running(), "");
            ctx.check(id + ": burns the old tank's fuel for its load (1.375 x 0.1 P_nom), within 3 %", Math.abs(burnt / (rate * t) - 1) < 0.03,
                String.format("%.3f against %.3f mB", burnt, rate * t));
            ctx.check(id + ": its fuel comes out of the vessel", Math.abs(used - burnt) < 0.01 + 0.01 * burnt, String.format("%.3f against %.3f", used, burnt));
            ctx.check(id + ": its power source gives " + String.format("%.0f %%", q * 100) + " of nominal",
                Math.abs(gen.sourceP() - pNom * q) < 0.01 * pNom, String.format("%.1f W", gen.sourceP()));
            if (mode == 0) ctx.check(id + ": gasoline is in its window and clean", gen.fuel.off() == 0 && gen.foul == 0, gen.fuel + ", " + gen.foul);
            if (mode == 5) ctx.check(id + ": diesel is 10 % too heavy for it, and burns clean", Math.abs(gen.fuel.heavy() - 0.1) < 0.002
                && gen.fuel.light() == 0 && gen.foul == 0, gen.fuel + ", " + gen.foul);
            if (mode == 1) {
                ctx.check(id + ": rated 10 % too heavy", Math.abs(gen.fuel.heavy() - 0.1) < 0.002 && gen.fuel.light() == 0, gen.fuel.toString());
                ctx.check(id + ": it fouls slowly, by the engines' law", dFoul > 0 && Math.abs(dFoul / expect - 1) < 0.05, String.format("%.7f against %.7f", dFoul, expect));
            }
            if (mode == 2) {
                ctx.check(id + ": clean fuel clears it, by the engines' law", dFoul < 0 && Math.abs(dFoul / expect - 1) < 0.05, String.format("%.7f against %.7f", dFoul, expect));
                boolean jade = gen.getWaila().containsKey(mods.eln.i18n.I18N.tr("Fouling"));
                var p = Wp10bCases.player(ctx);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.level.block.Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 3));
                gen.onBlockActivated(p, mods.eln.misc.Direction.YP, 0.5f, 1f, 0.5f);
                int left = p.getMainHandItem().getCount();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                ctx.check(id + ": Jade shows the fouling; wool cleans it, one used", jade && gen.foul == 0 && left == 2, jade + ", " + gen.foul + ", " + left);
            }
            ctx.check(id + ": Jade shows the fuel's quality", gen.getWaila().containsKey(mods.eln.i18n.I18N.tr("Fuel quality")), "");
        }
    }
}
