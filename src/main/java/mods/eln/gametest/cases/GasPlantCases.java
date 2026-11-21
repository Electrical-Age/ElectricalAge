package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.fluid.FuelRegistry;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GasTurbineElement;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftMotorElement;
import mods.eln.mechanical.ShaftSteamTurbineElement;
import mods.eln.misc.Direction;
import mods.eln.oil.HeatExchangerElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.ShaftPlantCases.*;
import static mods.eln.gametest.cases.ShaftSteamCases.*;
import static mods.eln.gametest.cases.Wp11Cases.*;

/**
 * The shaft redo, part 4 (Baughn 2026-10-29): the Gas Turbine (motor-started peaker, hot exhaust) and the
 * Counterflow Heat Exchanger, stacked 2 x 2 x N (steam raiser, reheat, combined cycle with an economizer).
 */
public final class GasPlantCases {
    private GasPlantCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new MotorStart());
        cases.add(new FuelQualityCase("gas_turbine_off_spec_derates", 0));
        cases.add(new FuelQualityCase("gas_turbine_heavy_oil_fouls", 1));
        cases.add(new FuelQualityCase("gas_turbine_clean_fuel_clears", 2));
        cases.add(new FuelQualityCase("gas_turbine_crude_derated", 3));
        FuelGeneratorCases.addTo(cases);
        cases.add(new NoSelfStart());
        cases.add(new RealGas());
        cases.add(new LowHead());
        cases.add(new LegacyTank());
        cases.add(new ExhaustRescale());
        cases.add(new SteamRaiser());
        cases.add(new StackMergesAndSplits());
        cases.add(new Reheat());
        cases.add(new CombinedCycle());
    }

    static final int GAS_TURBINE = 11 + (4 << 6), HX = 56 + (82 << 6);

    /** Fixed nodes behind ports on the faces of blocks (a lake, a drain, a steam main): {pos, face, node[, spot[, k]]} each
     *  (k: the port's conductance, m^3, for a throttled one; else an iron pipe's end). */
    static FluidDevice fixtures(Object level, Object[]... ports) {
        List<FluidDevice.FluidPort> list = new ArrayList<>();
        for (Object[] p : ports)
            // (an iron pipe's end: a lake or a main through a modest pipe, not a firehose)
            list.add(new FluidDevice.FluidPort((FluidNode) p[2], ((BlockPos) p[0]).asLong(), (Integer) p[1], 0.5,
                p.length > 4 ? (Double) p[4] : 2 * mods.eln.node.voxel.PipePart.K_PER_BLOCK[0] * 16,
                p.length > 3 ? (Integer) p[3] : FluidDevice.FluidPort.CENTRE));
        var d = new FluidDevice() {
            public Object fluidLevel() {
                return level;
            }

            public List<FluidPort> fluidPorts() {
                return list;
            }
        };
        mods.eln.fluid.FluidWorld.add(d);
        return d;
    }

    /** The stats of n's fluid network (FluidNetwork.stats: ms/step, sleep), or "none". */
    static String netStats(FluidNode n) {
        var net = n == null ? null : mods.eln.fluid.FluidWorld.networkOf(n);
        return net == null ? "none" : net.stats();
    }

    /** A big reservoir of water under an air cushion at pa (1 m^3, 70 % water: free, not fixed, so it gives as a real
     *  vessel does; fixed nodes as stiff sources set liquid-full nodes between them swinging by tens of bar). */
    static FluidNode water(int y, double pa, double T) {
        return new FluidNode(1.0, y, 1.0, false).fill(0.7 * Water.RHO_L, pa, T);
    }

    /** A big vessel of air at 1 atm: a drain (10 m^3, free). */
    static FluidNode air(int y) {
        return new FluidNode(10.0, y, 1.0, false).fill(0, Water.P_ATM, 293.15);
    }

    static FluidNode steam(int y, double pa) {
        double T = Water.tsat(pa), m = pa * 1.0 / (Water.R_V * T);
        FluidNode n = new FluidNode(1.0, y, 1.0, true);
        n.mWater = m;
        n.energy = n.energyAt(T);
        n.update();
        return n;
    }

    /** A gas turbine at (2,1,5) facing XN with a bucket-filled tank, a loaded 8 kW generator (z 4) and, if asked, a powered
     *  shaft motor (z 6) to start it. */
    static final class GasLine {
        GasTurbineElement gt;
        Resistor load;
        ShaftMotorElement motor;
        double loadW;
        boolean loaded;

        /** Switch the load on once it burns (a peaker's start: spin, light, then take the load). */
        void tick() {
            if (!loaded && gt.burning && gt.getShaft().getRads() > 760) {
                load.setR(mods.eln.Eln.HVU * mods.eln.Eln.HVU / loadW);   // (the generator's nominal volts)
                loaded = true;
            }
        }

        GasLine(TestContext ctx, boolean withMotor, double loadW) {
            this(ctx, withMotor, loadW, true);
        }

        GasLine(TestContext ctx, boolean withMotor, double loadW, boolean bucketFuel) {
            gt = (GasTurbineElement) orient(ctx.placeTransparent(GAS_TURBINE, ctx.at(2, 1, 5)));
            var g = (GeneratorElement) orient(ctx.placeTransparent(GENERATOR_8KW, ctx.at(2, 1, 4)));
            load = load(g, loadW);
            load.setR(1e12);   // (no load while the motor starts it: switched on once it burns, by the test)
            this.loadW = loadW;
            if (withMotor) {
                motor = (ShaftMotorElement) orient(ctx.placeTransparent(MOTOR, ctx.at(2, 1, 6)));
                ctx.placeSix(Ids.six("Medium Voltage Cable"), ctx.at(1, 1, 6));
                var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(SOURCE, ctx.at(0, 1, 6));
                ctx.setVolts(s, 200f);
            }
            var fuels = FuelRegistry.INSTANCE.fluidListToFluids(FuelRegistry.INSTANCE.getGasolineList());
            if (fuels.length > 0 && bucketFuel) gt.putFuel(fuels[0], GasTurbineElement.TANK_MB / 2);
        }
    }

    /**
     * A motor-started gas turbine (Baughn: a peaker; Argon: seconds to full power): the motor spins it over its cutoff,
     * it lights, carries its 7 kW load at speed within seconds and holds it; its fuel's heat to work ~28 %.
     */
    static final class MotorStart extends PlumbingCases.Big {
        GasLine l;
        IProcess watch;
        int tick, lit = -1, full = -1;
        double f0, w0, minR = 1e9, maxR;

        public String id() {
            return "gas_turbine_motor_start";
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000);
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                l.tick();
                if (lit < 0 && l.gt.burning) lit = tick;
                if (lit >= 0 && full < 0 && l.load.getP() > 0.9 * 7000 * Math.pow(l.gt.getShaft().getRads() / 800, 2) && l.gt.getShaft().getRads() > 760) full = tick;
                if (tick == 400) {
                    f0 = l.gt.totalFuelJ;
                    w0 = l.gt.totalWork;
                }
                if (tick > 400) {
                    minR = Math.min(minR, l.gt.getShaft().getRads());
                    maxR = Math.max(maxR, l.gt.getShaft().getRads());
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            // (its exhaust to the air: the casing's own network, which should sleep)
            ctx.line("gas turbine: exhaust network " + netStats(l.gt.exhaust));
            double eta = (l.gt.totalWork - w0) / Math.max(1, l.gt.totalFuelJ - f0);
            ctx.line(String.format("gas turbine: lit at %.1f s, carrying its load at %.1f s (%.1f s after lighting); %.0f-%.0f rad/s; %.1f kW fuel -> %.1f kW: %.1f %%; %s",
                lit / 20.0, full / 20.0, (full - lit) / 20.0, minR, maxR, l.gt.fuelW / 1000, l.gt.shaftW / 1000, eta * 100, l.gt.getWaila()));
            ctx.check("gas turbine: the motor brings it to light", lit > 0, "");
            ctx.check("gas turbine: at its load within 10 s of lighting", full > 0 && full - lit < 200, String.format("%.1f s", (full - lit) / 20.0));
            ctx.check("gas turbine: holds its speed (780-845 rad/s)", minR > 780 && maxR < 845, String.format("%.0f-%.0f", minR, maxR));
            ctx.check("gas turbine: work / fuel heat ~28 % (25-30)", eta > 0.25 && eta < 0.30, String.format("%.1f %%", eta * 100));
        }
    }

    /**
     * Fuel quality on the gas turbine (refinery M1, Baughn 2026-11-15), a motor-started turbine on a 4 kW load:
     * mode 0, diesel with 10 % residue by mass: 20 % less work for its fuel (22.4 % against 28 %), and it fouls;
     * mode 1, heavy oil: full power for its fuel at first, fouling at the furnace burner's law over the engines' balance, 4x slower;
     * mode 2, a half-fouled turbine on gasoline: half the work for its fuel, and it clears; wool cleans it;
     * mode 3, dead crude (it counts as its main cut, heavy oil, so the turbine takes it, as before): 15.8 % residue, so
     * 68 % of the work for its fuel, and it fouls faster than on heavy oil.
     */
    static final class FuelQualityCase extends PlumbingCases.Big {
        final String id;
        final int mode;
        GasLine l;
        IProcess watch;
        int tick;
        double f0, w0, foul0, foulStart;

        FuelQualityCase(String id, int mode) {
            this.id = id;
            this.mode = mode;
        }

        public String id() {
            return id;
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 4000, mode == 2);
            var reg = mods.eln.sim.fluid.SpeciesRegistry.get();
            var v = l.gt.fuelIn.vessel;
            if (mode == 0) {
                mods.eln.sim.fluid.EaUnits.add(v, reg.id("eln:diesel"), 0.45, 293.15, false);
                mods.eln.sim.fluid.EaUnits.add(v, reg.id("eln:residue"), 0.05, 293.15, false);
            } else if (mode == 1) mods.eln.sim.fluid.EaUnits.add(v, reg.id("eln:heavy_oil"), 0.5, 293.15, false);
            else if (mode == 3) mods.eln.sim.fluid.EaUnits.add(v, reg.id("eln:crude_oil"), 0.5, 293.15, false, false);
            else l.gt.foul = foulStart = 0.5;
            v.update();
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                l.tick();
                if (tick == 400) {
                    f0 = l.gt.totalFuelJ;
                    w0 = l.gt.totalWork;
                    foul0 = l.gt.foul;
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            var gt = l.gt;
            double fuelJ = gt.totalFuelJ - f0, eta = (gt.totalWork - w0) / Math.max(1, fuelJ), dFoul = gt.foul - foul0;
            // (the fouling law: FOUL_PER_INDEX x (index - balance) per ENGINE_SLOWER x FOUL_SECONDS at full power = per that much fuel heat)
            double full = GasTurbineElement.NOMINAL_P / GasTurbineElement.ETA * mods.eln.sim.fluid.FuelQuality.FOUL_SECONDS * mods.eln.sim.fluid.FuelQuality.ENGINE_SLOWER;
            double index = mode == 0 ? 0.9 * 0.01 + 0.1 : mode == 1 ? 0.15 : mode == 3 ? (0.35 * 0.15 + 0.25 * 0.01 + 0.15) / 0.95 : 0;
            double expect = mods.eln.sim.fluid.FuelQuality.foulRate(index, mods.eln.sim.fluid.FuelQuality.ENGINE_BALANCE) * fuelJ / full;
            double etaWant = GasTurbineElement.ETA * (mode == 0 ? 0.8 : mode == 3 ? 1 - 2 * 0.15 / 0.95 : 1) * (1 - (foul0 + gt.foul) / 2);
            ctx.line(String.format("%s: burning %b at %.0f rad/s, %.1f kW fuel -> %.1f kW, %.1f %% (want %.1f); rating %s; fouling %.5f -> %.5f (d %.6f, law %.6f); %s",
                id, gt.burning, gt.getShaft().getRads(), gt.fuelW / 1000, gt.shaftW / 1000, eta * 100, etaWant * 100, gt.fuel, foul0, gt.foul, dFoul, expect, gt.getWaila()));
            ctx.check(id + ": it runs and carries its load at speed", gt.burning && l.loaded && gt.getShaft().getRads() > 760, String.format("%.0f rad/s", gt.getShaft().getRads()));
            ctx.check(id + ": work / fuel heat within 6 % of " + String.format("%.1f %%", etaWant * 100), Math.abs(eta / etaWant - 1) < 0.06, String.format("%.2f %%", eta * 100));
            if (mode == 0) ctx.check(id + ": rated 10 % too heavy", Math.abs(gt.fuel.heavy() - 0.1) < 0.005 && gt.fuel.light() == 0, gt.fuel.toString());
            if (mode == 3) ctx.check(id + ": rated 15.8 % too heavy", Math.abs(gt.fuel.heavy() - 0.15 / 0.95) < 0.005 && gt.fuel.light() == 0, gt.fuel.toString());
            if (mode == 2) ctx.check(id + ": clean fuel clears it, by the law", dFoul < 0 && Math.abs(dFoul / expect - 1) < 0.02 && gt.foul < foulStart, String.format("%.6f against %.6f", dFoul, expect));
            else ctx.check(id + ": it fouls slowly, by the law", dFoul > 0 && Math.abs(dFoul / expect - 1) < 0.02 && gt.foul < 0.02, String.format("%.6f against %.6f", dFoul, expect));
            if (mode == 2) {
                // (wool cleans a fouled turbine, one used, as the furnace burner; on a clean one it does nothing)
                var player = new net.minecraft.world.entity.player.Player(ctx.level(), new com.mojang.authlib.GameProfile(
                    java.util.UUID.nameUUIDFromBytes("eln-foul".getBytes()), "foul")) {
                    @Override
                    public net.minecraft.world.level.GameType gameMode() {
                        return net.minecraft.world.level.GameType.SURVIVAL;
                    }

                    @Override
                    public boolean isClientAuthoritative() {
                        return false;
                    }
                };
                boolean jade = gt.getWaila().containsKey(mods.eln.i18n.I18N.tr("Fouling"));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 3));
                boolean used = gt.onBlockActivated(player, mods.eln.misc.Direction.YP, 0.5f, 0.5f, 0.5f);
                boolean again = gt.onBlockActivated(player, mods.eln.misc.Direction.YP, 0.5f, 0.5f, 0.5f);
                ctx.check(id + ": Jade shows the fouling; wool cleans it, one used; on a clean one it does nothing",
                    jade && used && !again && gt.foul == 0 && player.getMainHandItem().getCount() == 2, jade + ", " + used + ", " + again + ", " + gt.foul + ", " + player.getMainHandItem());
            }
            ctx.check(id + ": Jade shows the fuel's quality",
                gt.getWaila().containsKey(mods.eln.i18n.I18N.tr("Fuel quality")), "");
        }
    }

    /**
     * Natural gas at its real heating value (Baughn 2026-11-11, 1570091283270336655): a gas main at 3 bar on its fuel
     * flange lights it and carries its 7 kW load at speed; the
     * fuel heat it reports is the gas it burnt x 50 MJ/kg (within 5 %), ~0.5 g/s, ~560 mB/s.
     */
    static final class RealGas extends PlumbingCases.Big {
        GasLine l;
        IProcess watch;
        FluidDevice fx;
        FluidNode main;
        int tick, lit = -1, full = -1;
        double kg0, f0, tank0;

        public String id() {
            return "gas_turbine_real_gas";
        }

        public int ticks() {
            return 900;
        }

        static int ng() {
            return mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:natural_gas");
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000, false);
            // (a gas main: 1 m^3 of natural gas at ~3 bar, ~2 kg, on its top fuel flange)
            main = new FluidNode(1.0, ctx.at(2, 2, 5).getY(), 1.0, false).fill(0, 100, 293.15);
            mods.eln.sim.fluid.EaUnits.add(main, ng(), 3e5 * 0.01604 / (8.314 * 293.15), 293.15, true);
            main.update();
            fx = fixtures(l.gt.fluidLevel(), new Object[]{ctx.at(2, 2, 5), 0, main});
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                l.tick();
                if (lit < 0 && l.gt.burning) lit = tick;
                if (lit >= 0 && full < 0 && l.load.getP() > 0.9 * 7000 * Math.pow(l.gt.getShaft().getRads() / 800, 2) && l.gt.getShaft().getRads() > 760) full = tick;
                if (tick == 500) {
                    kg0 = main.vapour(ng());
                    f0 = l.gt.totalFuelJ;
                    tank0 = l.gt.fuelGasKg();
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            mods.eln.fluid.FluidWorld.remove(fx);
            double burnt = (kg0 - main.vapour(ng())) - (l.gt.fuelGasKg() - tank0), fuel = l.gt.totalFuelJ - f0, secs = (tick - 500) / 20.0;
            String what = String.format("lit at %.1f s, load at %.1f s; %.3f g/s of gas, %.1f kW fuel heat (%.1f MJ/kg), vessel %.2f g, main %.2f bar; %s",
                lit / 20.0, full / 20.0, burnt / secs * 1000, fuel / secs / 1000, fuel / Math.max(1e-12, burnt) / 1e6, l.gt.fuelGasKg() * 1000, main.P() / 1e5, l.gt.getWaila());
            ctx.line("gas turbine on real gas: " + what);
            ctx.check("gas turbine on natural gas: lights and carries its 7 kW load", lit > 0 && full > 0, what);
            ctx.check("gas turbine on natural gas: 50 MJ/kg (within 5 %)", Math.abs(fuel / Math.max(1e-12, burnt) - 50e6) < 2.5e6, what);
            ctx.check("gas turbine on natural gas: ~0.5 g/s (0.4-0.65)", burnt / secs > 0.4e-3 && burnt / secs < 0.65e-3, what);
        }
    }

    /** Without a starter it can't light: it stays still and uses no fuel. */
    static final class NoSelfStart extends PlumbingCases.Big {
        GasLine l;
        double fuel0;

        public String id() {
            return "gas_turbine_cannot_self_start";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, false, 1000);
            fuel0 = l.gt.fuelMb();
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeElectricalComponent(l.load);
            ctx.check("gas turbine without a starter: still, no fuel used", l.gt.getShaft().getRads() < 1 && Math.abs(l.gt.fuelMb() - fuel0) < 0.01 * fuel0 && fuel0 > 0 && l.gt.totalFuelJ == 0,
                String.format("%.1f rad/s, %.0f of %.0f mB", l.gt.getShaft().getRads(), l.gt.fuelMb(), fuel0));
            ctx.check("gas turbine without a starter: Jade says it needs one", l.gt.status().startsWith("Needs a starter"), l.gt.status());
        }
    }

    /**
     * Gasoline under a low head (an open tank, 0.3 m of it over the fuel flange) and a motor-started turbine: the
     * sealed fuel vessel takes only what squeezes its air cap at first, enough to light; running, the turbine lets the
     * cap out through its burner, so the vessel fills (well past what the head alone would push in).
     */
    static final class LowHead extends PlumbingCases.Big {
        GasLine l;
        IProcess watch;
        FluidDevice fx;
        FluidNode main;
        int tick, lit = -1;
        double atLight;

        public String id() {
            return "gas_turbine_low_head_fuel";
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000, false);
            int gasoline = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:gasoline");
            main = new FluidNode(1.0, ctx.at(2, 2, 5).getY(), 1.0, false).fill(0, mods.eln.sim.fluid.Water.P_ATM, 293.15);
            mods.eln.sim.fluid.EaUnits.add(main, gasoline, 0.8 * 740, 293.15, false);
            main.mAir *= 0.2;   // (open to the air: 1 atm over the liquid)
            main.energy = main.energyAt(293.15);
            main.update();
            fx = fixtures(l.gt.fluidLevel(), new Object[]{ctx.at(2, 2, 5), 0, main});
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                l.tick();
                if (lit < 0 && l.gt.burning) {
                    lit = tick;
                    atLight = l.gt.fuelMb();
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            mods.eln.fluid.FluidWorld.remove(fx);
            String what = String.format("lit at %.1f s with %.0f mB; now %.0f mB at %.2f bar, tank over it %.2f bar; %s", lit / 20.0, atLight, l.gt.fuelMb(),
                l.gt.fuelIn.vessel.P() / 1e5, main.P() / 1e5, l.gt.getWaila());
            ctx.line("gas turbine on a low head: " + what);
            ctx.check("gas turbine on a low head of gasoline: it lights", lit > 0 && l.gt.totalFuelJ > 0, what);
            ctx.check("gas turbine on a low head of gasoline: running, its vessel fills (over 3 B; the head alone squeezes in under 1 B)",
                l.gt.fuelMb() > 3000, what);
        }
    }

    /** An old save's fuel tank ("fuel", 8 buckets of gasoline) loads into the fuel vessel, at 1 atm. */
    static final class LegacyTank extends PlumbingCases.Big {
        String legacy = "not run";

        public String id() {
            return "gas_turbine_legacy_tank";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(2, 1, 2);
            ctx.placeTransparent(GAS_TURBINE, p);
            var fuels = FuelRegistry.INSTANCE.fluidListToFluids(new String[]{"gasoline"});
            ctx.after(2, () -> {
                try {
                    var r = mods.eln.gametest.Reload.node(ctx.level(), p, t -> {
                        var el = OilCases.FuelBySpecies.holding(t, "fuelInVesselW");
                        for (String k : new String[]{"W", "A", "U", "S", "Fv"}) el.remove("fuelInVessel" + k);
                        var old = new mods.eln.fluid.ElementFluidHandler(GasTurbineElement.TANK_MB);
                        old.fill(new FluidStack(fuels[0], 8000), true);
                        old.writeToNBT(el, "fuel");
                        return t;
                    });
                    var gt = (GasTurbineElement) ((mods.eln.node.transparent.TransparentNode) r.fresh).element;
                    double mb = gt.fuelMb(), bar = gt.fuelIn.vessel.P() / 1e5;
                    legacy = Math.abs(mb - 8000) < 80 && bar > 0.95 && bar < 1.3 ? "" : String.format("%.0f mB at %.2f bar; %s", mb, bar, gt.getWaila());
                } catch (ReflectiveOperationException e) {
                    legacy = e.toString();
                }
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("gas turbine: an old save's 8 B of gasoline is in its fuel vessel at about 1 atm", legacy.isEmpty(), legacy);
        }
    }

    /**
     * The exhaust casing's saved volume (GasTurbineElement.EXHAUST_V, "exhaustV"): a save from the 40 L casing loads into
     * the 4 L one at the pressure and temperature it was saved at (its contents rescaled), and a save without the tag (a 4 L
     * casing's) loads as it is.
     */
    static final class ExhaustRescale extends PlumbingCases.Big {
        String big = "not run", old = "not run";

        public String id() {
            return "gas_turbine_exhaust_volume_rescale";
        }

        public int ticks() {
            return 20;
        }

        /** Reload the turbine at p with its casing saved as volume v (m^3; tagged if asked) holding air at pa and T. */
        static String load(TestContext ctx, BlockPos p, double v, boolean tagged, double pa, double T) {
            // (what the saved casing held, as a node of its own volume: the loaded one should read its pressure and temperature)
            FluidNode saved = new FluidNode(v, p.getY(), 1.0, false).fill(0, pa, T);
            try {
                var r = mods.eln.gametest.Reload.node(ctx.level(), p, t -> {
                    var el = OilCases.FuelBySpecies.holding(t, "exhaustW");
                    mods.eln.fluid.FluidSaves.put(el, "exhaust", saved);
                    if (tagged) el.putDouble("exhaustV", v);
                    else el.remove("exhaustV");
                    return t;
                });
                var gt = (GasTurbineElement) ((mods.eln.node.transparent.TransparentNode) r.fresh).element;
                FluidNode ex = gt.exhaust;
                String got = String.format("%.4f m^3 at %.4f bar, %.2f K, %.4g kg air", ex.volume, ex.P() / 1e5, ex.T(), ex.mAir);
                ctx.line(String.format("gas turbine casing saved as %.3f m^3 %s at %.2f bar, %.0f K: loaded %s", v, tagged ? "(tagged)" : "(no tag)", pa / 1e5, T, got));
                return Math.abs(ex.volume - GasTurbineElement.EXHAUST_V) < 1e-12 && Math.abs(ex.P() - saved.P()) < 1e-6 * saved.P()
                    && Math.abs(ex.T() - saved.T()) < 1e-3 ? "" : got + String.format(" (saved: %.6f bar, %.4f K)", saved.P() / 1e5, saved.T());
            } catch (ReflectiveOperationException e) {
                return e.toString();
            }
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(2, 1, 2), q = ctx.at(5, 1, 2);
            ctx.placeTransparent(GAS_TURBINE, p);
            ctx.placeTransparent(GAS_TURBINE, q);
            ctx.after(2, () -> {
                big = load(ctx, p, 0.04, true, 1.6e5, 600);
                old = load(ctx, q, GasTurbineElement.EXHAUST_V, false, 1.4e5, 500);
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("gas turbine: a 40 L casing's save loads into the 4 L one at its saved pressure and temperature", big.isEmpty(), big);
            ctx.check("gas turbine: a save without the casing's volume (4 L) loads as it is", old.isEmpty(), old);
        }
    }

    /** B's port spot on an exchanger's east or west end face (up high: TankElement.sideSpot). */
    static final int B_AT = mods.eln.oil.TankElement.sideSpot(4, mods.eln.oil.TankElement.HIGH);

    /**
     * A stack of heat exchangers 2 x 2 across and `len` long along X from its lowest corner, fronts XP: A in at its west
     * end, out at its east; B in at the east, out at the west.
     */
    static List<HeatExchangerElement> stack(TestContext ctx, BlockPos corner, int len) {
        List<HeatExchangerElement> out = new ArrayList<>();
        for (int i = 0; i < len; i++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) out.add((HeatExchangerElement) ctx.placeTransparent(HX, corner.offset(i, y, z)));
        for (HeatExchangerElement h : out) {
            h.front = Direction.XP;
            h.reconnect();
        }
        return out;
    }

    /** The stack's corner (it steps the stack and keeps its readings). */
    static HeatExchangerElement head(List<HeatExchangerElement> s) {
        return s.get(0).group == null ? s.get(0) : s.get(0).group.master();
    }

    static boolean stacked(List<HeatExchangerElement> s, double ua) {
        return s.get(0).group != null && s.get(0).group.members.size() == s.size() && Math.abs(s.get(0).ua() - ua) < 1e-9;
    }

    /** Air drained at 1 atm off an exchanger's A outlet: the heat it still carries over ambient (W), from a window's start. */
    static final class AirMeter {
        final FluidNode node;
        double e0, m0;

        AirMeter(FluidNode node) {
            this.node = node;
        }

        void start() {
            e0 = node.energy;
            m0 = node.mAir;
        }

        double heat(double window) {
            return ((node.energy - e0) - (node.mAir - m0) * Water.hAir(293.15)) / window;
        }

        /** The temperature it left at (K). */
        double tOut(double window) {
            return 293.15 + heat(window) / Math.max(1e-9, (node.mAir - m0) / window * 1005);
        }
    }

    /**
     * A 2 x 2 x 2 steam raiser on the gas turbine's exhaust (Baughn 2026-10-29: 50 W/K a slice, 100 here, NTU 2.5 on the
     * exhaust's 40 W/K): its side B half full of water boiling at 15 bar, steam off to a 15 bar main. It takes ~92 % of
     * what the exhaust can give down to the water's 198 C; the exhaust's heat goes into the water or out with the air.
     */
    static final class SteamRaiser extends PlumbingCases.Big {
        GasLine l;
        List<HeatExchangerElement> s;
        FluidDevice fx;
        IProcess watch;
        AirMeter drain;
        int tick;
        double j0, x0, window;

        public String id() {
            return "heat_exchanger_steam_raiser";
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000);
            s = stack(ctx, ctx.at(3, 1, 5), 2);
            var b = head(s).b;
            PlumbingCases.fill(b, 0.5 * b.volume * Water.RHO_L, 15e5, Water.tsat(15e5));
            drain = new AirMeter(air(ctx.at(5, 1, 5).getY()));
            fx = fixtures(head(s).fluidLevel(), new Object[]{ctx.at(5, 1, 5), 4, drain.node},
                new Object[]{ctx.at(2, 2, 6), 5, steam(ctx.at(2, 2, 6).getY(), 15e5), B_AT});   // (steam off its west end's top row)
            Eln.simulator.addSlowProcess(watch = dt -> {
                l.tick();
                // (makeup at its own temperature to half full: at 1 B a side a block it holds 8 B, under a minute of steaming)
                var w = head(s).b;
                double lack = 0.5 * w.volume * Water.RHO_L - w.mLiquid();
                if (lack > 0) {
                    double T = w.T();
                    w.mWater += lack;
                    w.energy = w.energyAt(T);
                    w.update();
                }
                if (++tick == 500) {
                    j0 = head(s).totalJ;
                    x0 = l.gt.totalExhaustJ;
                    drain.start();
                }
                if (tick > 500) window += dt;
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            // (the exhaust's network's cost and sleep over the test, before taking the fixtures out rebuilds it)
            ctx.line("steam raiser: exhaust network " + netStats(l.gt.exhaust));
            mods.eln.fluid.FluidWorld.remove(fx);
            HeatExchangerElement h = head(s);
            double q = (h.totalJ - j0) / window, ex = (l.gt.totalExhaustJ - x0) / window, left = drain.heat(window);
            double cEx = ex / (GasTurbineElement.EXHAUST_T - 293.15), most = cEx * (GasTurbineElement.EXHAUST_T - h.b.T());
            ctx.line(String.format("steam raiser 2x2x2 (%.0f W/K): %.1f of %.1f kW the exhaust can give down to %.0f C (%.0f %%); exhaust %.1f kW = %.1f + %.1f left in air leaving at %.0f C; %s",
                h.ua(), q / 1000, most / 1000, h.b.T() - Water.T0, q / most * 100, ex / 1000, q / 1000, left / 1000, drain.tOut(window) - Water.T0, h.getWaila()));
            ctx.check("steam raiser: one 2 x 2 x 2 exchanger (100 W/K)", stacked(s, 100), "");
            ctx.check("steam raiser: takes 85-97 % of what the exhaust can give (NTU 2.5)", q > 0.85 * most && q < 0.97 * most, String.format("%.0f %%", q / most * 100));
            ctx.check("steam raiser: heat is conserved (within 5 %)", Math.abs(q + left - ex) < 0.05 * ex, String.format("%.1f + %.1f vs %.1f kW", q / 1000, left / 1000, ex / 1000));
            ctx.check("steam raiser: the air leaves no colder than the water", drain.tOut(window) > h.b.T() - 2, String.format("%.0f C", drain.tOut(window) - Water.T0));
        }
    }

    /**
     * Stacking (Baughn 2026-10-29): 2 x 2 x 2 exchangers are one, 100 W/K, its sides the blocks' summed; a block broken
     * out leaves seven quarter slices (12.5 W/K each) keeping 7/8 of the water; put back, one again with all of it. Nine
     * long is past the most (8): nine-and-thirty quarter slices.
     */
    static final class StackMergesAndSplits extends PlumbingCases.Big {
        List<HeatExchangerElement> s, longOne;
        IProcess watch;
        int tick;
        double before, afterSplit;
        boolean wasStacked, split;

        public String id() {
            return "heat_exchanger_stack_merges_and_splits";
        }

        public int ticks() {
            return 30;
        }

        static double water(List<HeatExchangerElement> s) {
            double m = 0;
            for (HeatExchangerElement h : s) m += h.group == null ? h.b.mWater : 0;
            return s.get(0).group != null ? s.get(0).group.b.mWater : m;
        }

        public void build(TestContext ctx) {
            s = stack(ctx, ctx.at(1, 1, 1), 2);
            longOne = stack(ctx, ctx.at(0, 1, 5), 9);
            wasStacked = stacked(s, 100) && Math.abs(s.get(0).b.volume - 8 * mods.eln.oil.HeatExchangerDescriptor.SIDE_VOLUME) < 1e-12;
            var b = head(s).b;
            PlumbingCases.fill(b, 0.5 * b.volume * Water.RHO_L, Water.P_ATM, 293.15);
            before = water(s);
            HeatExchangerElement gone = s.get(s.size() - 1);
            ctx.level().removeBlock(gone.node.coordonate.pos(), false);
            List<HeatExchangerElement> rest = new ArrayList<>(s.subList(0, s.size() - 1));
            split = true;
            for (HeatExchangerElement h : rest) split &= h.group == null && h.b == h.ownB && Math.abs(h.ua() - 12.5) < 1e-9;
            afterSplit = water(rest);
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 5) s.set(s.size() - 1, (HeatExchangerElement) placeBack(ctx, gone.node.coordonate.pos()));
            });
        }

        static HeatExchangerElement placeBack(TestContext ctx, BlockPos abs) {
            var h = (HeatExchangerElement) ctx.placeTransparent(HX, abs);
            h.front = Direction.XP;
            h.reconnect();
            return h;
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            ctx.check("stack: 2 x 2 x 2 blocks are one exchanger, 100 W/K, 8 B a side", wasStacked, "");
            ctx.check("stack: one broken out leaves seven quarter slices of 12.5 W/K", split, "");
            ctx.check("stack: they keep 7/8 of the water", Math.abs(afterSplit - before * 7 / 8) < 0.02 * before, String.format("%.3f of %.3f kg", afterSplit, before));
            ctx.check("stack: put back, one exchanger again with all they had", stacked(s, 100) && Math.abs(water(s) - afterSplit) < 0.02 * before,
                String.format("%.3f kg", water(s)));
            boolean apart = true;
            for (HeatExchangerElement h : longOne) apart &= h.group == null;
            ctx.check("stack: nine long is past the most (8): quarter slices", apart, "");
        }
    }

    /**
     * Reheat between the HP and LP turbines (Baughn 2026-10-29): a 2 x 2 x 4 exchanger (200 W/K), live steam at 20 bar
     * condensing in side A, the crossover's ~40 g/s of 2.4 bar steam through side B: it gets ~6 kW, the steam ~75 K hotter.
     */
    static final class Reheat extends PlumbingCases.Big {
        /** The crossover's drop over the reheater and its fixtures (Pa): 900 at 8 L a side; at 1 B its joints and ports are
         *  narrower (HeatExchangerElement.endK) and it took ~1.5 kPa for the same ~40 g/s; with its joints a tenth of its
         *  ports (HeatExchangerElement.JOINT_SHARE) it takes ~5 kPa. */
        static final double REHEAT_DROP = 5000;
        List<HeatExchangerElement> s;
        FluidDevice fx;
        IProcess watch;
        int tick, n;
        double j0, window, flow;

        public String id() {
            return "heat_exchanger_reheat";
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            s = stack(ctx, ctx.at(1, 1, 1), 4);
            int y = ctx.at(0, 1, 1).getY();
            // (A: live steam into its west end; B: crossover steam in at its east end, out at its west end's top row to a
            // main a little lower)
            fx = fixtures(head(s).fluidLevel(), new Object[]{ctx.at(0, 1, 1), 5, steam(y, 20e5)},
                new Object[]{ctx.at(5, 1, 2), 4, steam(y, 2.4e5), B_AT}, new Object[]{ctx.at(0, 2, 2), 5, steam(y, 2.4e5 - REHEAT_DROP), B_AT});
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 300) j0 = head(s).totalJ;
                if (tick > 300) {
                    window += dt;
                    flow += head(s).sideInflow(false)[0];
                    n++;
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            mods.eln.fluid.FluidWorld.remove(fx);
            HeatExchangerElement h = head(s);
            double q = (h.totalJ - j0) / window, m = flow / Math.max(1, n);
            ctx.line(String.format("reheat 2x2x4 (%.0f W/K): %.1f kW into %.1f g/s of crossover steam, its joints dropping %.0f Pa of the %.0f; %s", h.ua(), q / 1000, m * 1000,
                Math.abs(h.jointDrop(false)), REHEAT_DROP, h.getWaila()));
            ctx.check("reheat: one 2 x 2 x 4 exchanger (200 W/K)", stacked(s, 200), "");
            ctx.check("reheat: ~40 g/s of crossover steam (30-55)", m > 0.030 && m < 0.055, String.format("%.1f g/s", m * 1000));
            ctx.check("reheat: 5-8 kW into it", q > 5000 && q < 8000, String.format("%.1f kW", q / 1000));
            ctx.check("reheat: the live steam condenses in side A", h.a.mLiquid() > 1e-3, String.format("%.4f kg", h.a.mLiquid()));
        }
    }

    /**
     * A combined cycle (Argon's pinnacle): the gas turbine's exhaust through a 2 x 2 x 2 steam raiser, a pipe, then a 2 x 2 x
     * 2 economizer; feedwater (16 bar) the other way: through the economizer, a throttle valve, the raiser's water side; its
     * steam to a separator tank and an HP turbine. Separate jobs, separate exchangers in series. Numbers logged: fuel, both
     * shafts, combined efficiency.
     */
    static final class CombinedCycle extends PlumbingCases.Big {
        /** The feed valve (alloy) between the economizer and the raiser, against ~2 bar: ~6 g/s (the orifice law; the
         *  level is the player's circuit to hold, Baughn). */
        static final double FEED_OPEN = 0.008;
        /** The feed port's throttle (m^3): ~6 g/s on a bar across it. */
        static final double FEED_K = 6e-14;
        GasLine l;
        List<HeatExchangerElement> raiser, econ;
        mods.eln.node.voxel.ValvePart feedValve;
        ShaftSteamTurbineElement hp;
        Resistor hpLoad;
        FluidDevice fx;
        IProcess watch;
        AirMeter drain;
        FluidNode feed;
        int tick;
        double f0, g0, s0, k0, j0, e0, w0, x0, window;
        mods.eln.oil.TankElement sep;
        /** The energy (J) in the raiser's water side, the economizer's and the separator at the window's start. */
        double store0;

        double store() {
            return head(raiser).b.energy + head(econ).b.energy + sep.vessel.energy;
        }

        public String id() {
            return "gas_plant_combined_cycle";
        }

        public int ticks() {
            return WARM + 800;
        }

        /** Ticks run before the measured window, and the raiser's pressure the HP's throttle holds (Pa). */
        static final int WARM = 1600;
        static final double HOLD_P = 14e5;

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000);
            raiser = stack(ctx, ctx.at(3, 1, 5), 2);
            econ = stack(ctx, ctx.at(6, 1, 5), 2);
            // exhaust: raiser east end -> alloy pipe -> economizer west end; water: economizer west end -> valve -> raiser
            // east end (B's spot, up high)
            PipeCases.lay(ctx, ctx.at(5, 1, 5), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(5, 1, 6), 1, 0, 12, 8, 8, 12, 8, 16, 12, 8);
            var valveNode = ctx.voxelByEntity(ctx.at(5, 1, 6));
            valveNode.addValve(8, 12, 8, mods.eln.node.voxel.ValvePart.THROTTLE);
            for (var part : valveNode.parts) if (part instanceof mods.eln.node.voxel.ValvePart v) feedValve = v;
            feedValve.setting = FEED_OPEN;
            // steam: the raiser's west end, top row -> pipe west and south into a separator tank's high side port at (2,2,7)
            // -> its top -> up, south, down into an HP turbine at (2,1,8); its generator at z 9
            PipeCases.lay(ctx, ctx.at(2, 2, 6), 1, 16, 12, 8, 8, 12, 8, 8, 12, 16);
            ctx.level().setBlockAndUpdate(ctx.at(2, 1, 7), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());   // (under it)
            sep = PlumbingCases.tank(ctx, PlumbingCases.ALLOY_TANK, ctx.at(2, 2, 7), Direction.ZP);   // (its back, north, to the pipe)
            PipeCases.lay(ctx, ctx.at(2, 3, 7), 1, 8, 0, 8, 8, 8, 8, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(2, 3, 8), 1, 8, 8, 0, 8, 8, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(2, 2, 8), 1, 8, 16, 8, 8, 0, 8);
            hp = (ShaftSteamTurbineElement) orient(ctx.placeTransparent(STEAM_TURBINE, ctx.at(2, 1, 8)));
            var g = (GeneratorElement) orient(ctx.placeTransparent(GENERATOR, ctx.at(2, 1, 9)));
            hpLoad = load(g, 3000);
            // (its nozzle is made for 39 g/s at 20 bar: on a few g/s it would let the drum down to 1 bar; a throttle holds
            // the pressure up, as a regulator on its screen would)
            hp.controls.throttle = 0.15;
            // the raiser's water side half full at 15 bar, saturated; the economizer's full of feed at about its running
            // temperature (64 L of water takes hours to warm from cold on 5 kW: the test measures the plant running)
            var rb = head(raiser).b;
            PlumbingCases.fill(rb, 0.5 * rb.volume * Water.RHO_L, 15e5, Water.tsat(15e5));
            var eb = head(econ).b;
            PlumbingCases.fill(eb, 0.97 * eb.volume * Water.RHO_L, 16e5, 453.15);
            // (as it runs: the feed coming in at its east end at 20 C, leaving at the west end at ~180 C; one temperature
            // through it, the exchanger's slices hold 180 C water against the gas for hours, and it gives 2 kW of 4.4)
            head(econ).profile(false, Direction.XN, 453.15, 293.15);
            int y = ctx.at(8, 1, 5).getY();
            drain = new AirMeter(air(y));
            fx = fixtures(head(raiser).fluidLevel(), new Object[]{ctx.at(8, 1, 5), 4, drain.node},
                // (the feed port throttled, as a pump's or a valve's outlet: open, the economizer's warm water ran back into the
                // feed vessel by its own warmth, ~1 kW of its heat)
                new Object[]{ctx.at(8, 1, 6), 4, feed = water(y, 17e5, 293.15), B_AT, FEED_K});
            Eln.simulator.addSlowProcess(watch = dt -> {
                l.tick();
                // (the player's level circuit: at 1 B a side a block the raiser holds 8 B, about a minute of steaming,
                // so the feed valve follows its level, half full, as a regulator on the valve would)
                var w = head(raiser).b;
                double level = w.mLiquid() / (Water.RHO_L * w.volume);
                feedValve.setting = FEED_OPEN * Math.max(0, Math.min(3, 1 + 8 * (0.5 - level)));
                // (the player's pressure regulator on the HP's throttle: it holds the raiser at HOLD_P, so the HP takes what the
                // heat raises and no more)
                hp.controls.throttle = Math.max(0.02, Math.min(1, hp.controls.throttle + dt * 0.02 * (head(raiser).b.P() - HOLD_P) / 1e5));
                if (++tick == WARM) {
                    f0 = l.gt.totalFuelJ;
                    g0 = l.gt.totalWork;
                    x0 = l.gt.totalExhaustJ;
                    s0 = hp.totalWork;
                    k0 = hp.totalKg;
                    j0 = head(raiser).totalJ;
                    e0 = head(econ).totalJ;
                    w0 = feed.mWater;
                    drain.start();
                    store0 = store();
                }
                if (tick > WARM) window += dt;
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            Eln.simulator.removeElectricalComponent(hpLoad);
            ctx.line("combined cycle: exhaust network " + netStats(l.gt.exhaust) + "; water network " + netStats(sep.vessel));
            mods.eln.fluid.FluidWorld.remove(fx);
            double fuel = (l.gt.totalFuelJ - f0) / window, gas = (l.gt.totalWork - g0) / window, st = (hp.totalWork - s0) / window,
                kg = (hp.totalKg - k0) / window, qr = (head(raiser).totalJ - j0) / window, qe = (head(econ).totalJ - e0) / window,
                ex = (l.gt.totalExhaustJ - x0) / window, left = drain.heat(window), fed = (w0 - feed.mWater) / window;
            ctx.line(String.format("combined cycle: fuel %.1f kW; gas turbine %.1f kW (%.1f %%); exhaust %.1f kW -> raiser %.1f + economizer %.1f kW, %.1f left in air at %.0f C; "
                    + "feed %.1f g/s, HP %.1f g/s at %.1f bar, %.1f kW; together %.1f kW = %.1f %% of the fuel (+%.0f %% over the gas turbine alone)",
                fuel / 1000, gas / 1000, gas / fuel * 100, ex / 1000, qr / 1000, qe / 1000, left / 1000, drain.tOut(window) - Water.T0,
                fed * 1000, kg * 1000, head(raiser).b.P() / 1e5, st / 1000, (gas + st) / 1000, (gas + st) / fuel * 100, st / Math.max(1, gas) * 100));
            double stored = (store() - store0) / window;
            ctx.line(String.format("combined cycle: the raiser, economizer and separator stored %.2f kW over the window; raiser at %.2f bar, HP throttle %.3f",
                stored / 1000, head(raiser).b.P() / 1e5, hp.controls.throttle));
            ctx.line("combined cycle: raiser " + head(raiser).getWaila() + "; economizer " + head(econ).getWaila() + "; HP " + hp.getWaila());
            ctx.check("combined cycle: two exchangers of 100 W/K", stacked(raiser, 100) && stacked(econ, 100), "");
            ctx.check("combined cycle: heat is conserved (within 5 %)", Math.abs(qr + qe + left - ex) < 0.05 * ex,
                String.format("%.1f + %.1f + %.1f vs %.1f kW", qr / 1000, qe / 1000, left / 1000, ex / 1000));
            ctx.check("combined cycle: the exchangers take most of the exhaust's heat (over 70 %)", qr + qe > 0.7 * ex, String.format("%.0f %%", (qr + qe) / ex * 100));
            // (steady: what the vessels stored over the window is small against what the exchangers moved. At 8 L a side the
            // raiser held a 30 kg drum, and the window caught it giving up 5.2 kW as its pressure fell: the HP's 2.3 kW then,
            // +31 %, was never sustainable. Held at 14 bar it gave ~1.6 kW, +22 %, its storage taken out)
            ctx.check("combined cycle: steady over the window (the vessels store under 5 % of the exchangers' heat)", Math.abs(stored) < 0.05 * (qr + qe),
                String.format("%.2f of %.1f kW", stored / 1000, (qr + qe) / 1000));
            ctx.check("combined cycle: the exhaust raises steam that drives the HP (over 1.5 kW)", st > 1500, String.format("%.1f kW", st / 1000));
            ctx.check("combined cycle: together they beat the gas turbine alone by over 20 %", gas + st > 1.2 * gas, "");
        }
    }
}
