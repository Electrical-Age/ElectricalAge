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
 * Counterflow Heat Exchanger (economizer, condensing, combined cycle).
 */
public final class GasPlantCases {
    private GasPlantCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new MotorStart());
        cases.add(new NoSelfStart());
        cases.add(new Economizer());
        cases.add(new CondensingSteamHeatsWater());
        cases.add(new CombinedCycle());
    }

    static final int GAS_TURBINE = 11 + (4 << 6), HX = 56 + (82 << 6);

    /** Fixed nodes behind ports on the faces of blocks (a lake, a drain, a steam main): {pos, face, node} each. */
    static FluidDevice fixtures(Object level, Object[]... ports) {
        List<FluidDevice.FluidPort> list = new ArrayList<>();
        for (Object[] p : ports)
            // (an iron pipe's end: a lake or a main through a modest pipe, not a firehose)
            list.add(new FluidDevice.FluidPort((FluidNode) p[2], ((BlockPos) p[0]).asLong(), (Integer) p[1], 0.5, 2 * mods.eln.node.voxel.PipePart.K_PER_BLOCK[0] * 16));
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
        n.energy = mods.eln.sim.fluid.Flash.energyAt(m, 0, 1.0, T);
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
                load.setR(3200.0 * 3200.0 / loadW);
                loaded = true;
            }
        }

        GasLine(TestContext ctx, boolean withMotor, double loadW) {
            gt = (GasTurbineElement) orient(ctx.placeTransparent(GAS_TURBINE, ctx.at(2, 1, 5)));
            var g = (GeneratorElement) orient(ctx.placeTransparent(GENERATOR_8KW, ctx.at(2, 1, 4)));
            load = load(g, loadW);
            load.setR(1e12);   // (no load while the motor starts it: switched on once it burns, by the test)
            this.loadW = loadW;
            if (withMotor) {
                motor = (ShaftMotorElement) orient(ctx.placeTransparent(MOTOR, ctx.at(2, 1, 6)));
                ctx.placeSix(Ids.six("Medium Voltage Cable"), ctx.at(1, 1, 6));
                var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(SOURCE, ctx.at(0, 1, 6));
                s.networkUnserialize(ctx.stream(out -> {
                    out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                    out.writeFloat(200f);
                }));
            }
            var fuels = FuelRegistry.INSTANCE.fluidListToFluids(FuelRegistry.INSTANCE.getGasolineList());
            if (fuels.length > 0) gt.tank.fill(new FluidStack(fuels[0], GasTurbineElement.TANK_MB), true);
        }
    }

    /** A heat exchanger at (3,1,5), facing ZP: its left (A in) against the gas turbine's exhaust. */
    static HeatExchangerElement hx(TestContext ctx, BlockPos at, Direction front) {
        var h = (HeatExchangerElement) ctx.placeTransparent(HX, at);
        h.front = front;
        h.reconnect();
        return h;
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
            double eta = (l.gt.totalWork - w0) / Math.max(1, l.gt.totalFuelJ - f0);
            ctx.line(String.format("gas turbine: lit at %.1f s, carrying its load at %.1f s (%.1f s after lighting); %.0f-%.0f rad/s; %.1f kW fuel -> %.1f kW: %.1f %%; %s",
                lit / 20.0, full / 20.0, (full - lit) / 20.0, minR, maxR, l.gt.fuelW / 1000, l.gt.shaftW / 1000, eta * 100, l.gt.getWaila()));
            ctx.check("gas turbine: the motor brings it to light", lit > 0, "");
            ctx.check("gas turbine: at its load within 10 s of lighting", full > 0 && full - lit < 200, String.format("%.1f s", (full - lit) / 20.0));
            ctx.check("gas turbine: holds its speed (780-845 rad/s)", minR > 780 && maxR < 845, String.format("%.0f-%.0f", minR, maxR));
            ctx.check("gas turbine: work / fuel heat ~28 % (25-30)", eta > 0.25 && eta < 0.30, String.format("%.1f %%", eta * 100));
        }
    }

    /** Without a starter it can't light: it stays still and uses no fuel. */
    static final class NoSelfStart extends PlumbingCases.Big {
        GasLine l;
        int fuel0;

        public String id() {
            return "gas_turbine_cannot_self_start";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, false, 1000);
            fuel0 = l.gt.tank.getFluidAmount();
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeElectricalComponent(l.load);
            ctx.check("gas turbine without a starter: still, no fuel used", l.gt.getShaft().getRads() < 1 && l.gt.tank.getFluidAmount() == fuel0 && fuel0 > 0,
                String.format("%.1f rad/s, %d of %d mB", l.gt.getShaft().getRads(), l.gt.tank.getFluidAmount(), fuel0));
        }
    }

    /** An economizer: the gas turbine's exhaust through side A, cold water (3 bar, 20 C) through side B: the water warms. */
    static final class Economizer extends PlumbingCases.Big {
        GasLine l;
        HeatExchangerElement h;
        FluidDevice fx;
        IProcess watch;
        int tick;
        double j0, window;

        public String id() {
            return "heat_exchanger_economizer";
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000);
            BlockPos hp = ctx.at(3, 1, 5);
            h = hx(ctx, hp, Direction.ZP);
            fx = fixtures(h.fluidLevel(), new Object[]{hp.east(), 4, air(hp.getY())}, new Object[]{hp.north(), 3, water(hp.getY(), 1.3e5, 293.15)},
                new Object[]{hp.south(), 2, air(hp.getY())});
            Eln.simulator.addSlowProcess(watch = dt -> {
                l.tick();
                if (++tick == 500) j0 = h.totalJ;
                if (tick > 500) window += dt;
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            mods.eln.fluid.FluidWorld.remove(fx);
            double kw = (h.totalJ - j0) / Math.max(1e-9, window) / 1000;
            ctx.line(String.format("economizer: %.1f kW from the exhaust (gas turbine %.1f kW fuel, %.1f kW on the shaft); %s", kw, l.gt.fuelW / 1000, l.gt.shaftW / 1000, h.getWaila()));
            ctx.check("economizer: the exhaust heats the water (over 3 kW)", kw > 3, String.format("%.1f kW", kw));
            ctx.check("economizer: the water warms (B over 30 C)", h.b.T() > 303, String.format("%.0f C", h.b.T() - Water.T0));
        }
    }

    /** Condensing steam (5 bar) in side A heats cold water in side B; A holds condensate. */
    static final class CondensingSteamHeatsWater extends PlumbingCases.Big {
        HeatExchangerElement h;
        FluidDevice fx;

        public String id() {
            return "heat_exchanger_condensing_steam";
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            BlockPos hp = ctx.at(5, 1, 5);
            h = hx(ctx, hp, Direction.ZP);
            fx = fixtures(h.fluidLevel(), new Object[]{hp.west(), 5, steam(hp.getY(), 5e5)}, new Object[]{hp.east(), 4, steam(hp.getY(), 4.5e5)},
                new Object[]{hp.north(), 3, water(hp.getY(), 1.3e5, 293.15)}, new Object[]{hp.south(), 2, air(hp.getY())});
        }



        public void measure(TestContext ctx) {
            mods.eln.fluid.FluidWorld.remove(fx);
            ctx.line("condensing steam: " + h.getWaila());
            ctx.check("condensing: the water is heated (B over 40 C)", h.b.T() > 313, String.format("%.0f C", h.b.T() - Water.T0));
            ctx.check("condensing: the steam side holds condensate", h.a.mLiquid() > 1e-4, String.format("%.4f kg", h.a.mLiquid()));
            ctx.check("condensing: kilowatts across (over 2)", h.q > 2000, String.format("%.1f kW", h.q / 1000));
        }
    }

    /**
     * A combined cycle (Argon's pinnacle): the gas turbine's exhaust raises steam in the heat exchanger's side B (a drum
     * fed from an 18 bar reservoir), which drives an HP turbine. Numbers logged: fuel, both shafts, combined efficiency.
     */
    static final class CombinedCycle extends PlumbingCases.Big {
        /** (liquid passes a valve freely: a thousandth of a percent open lets ~4 g/s through at a few bar) */
        static final double FEED_OPEN = 1e-5;
        GasLine l;
        HeatExchangerElement h;
        ShaftSteamTurbineElement hp;
        Resistor hpLoad;
        FluidDevice fx;
        IProcess watch;
        int tick;
        double f0, g0, s0, k0, j0, window;

        public String id() {
            return "gas_plant_combined_cycle";
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000);
            BlockPos hxAt = ctx.at(3, 1, 5);
            h = hx(ctx, hxAt, Direction.ZP);
            // B out (front, south) -> alloy pipe into the high side port of a separator tank at (3,1,7) -> its top -> pipe
            // south and down into an HP turbine at (3,1,8); its generator at z 9 (Argon: a drum / separator keeps the
            // water out of the turbine)
            PipeCases.lay(ctx, ctx.at(3, 1, 6), 1, 8, 8, 0, 8, 8, 8, 8, 12, 8, 8, 12, 16);
            PlumbingCases.tank(ctx, PlumbingCases.ALLOY_TANK, ctx.at(3, 1, 7), Direction.ZP);   // (its back, north, to the pipe)
            PipeCases.lay(ctx, ctx.at(3, 2, 7), 1, 8, 0, 8, 8, 8, 8, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(3, 2, 8), 1, 8, 8, 0, 8, 8, 8, 8, 0, 8);
            hp = (ShaftSteamTurbineElement) orient(ctx.placeTransparent(STEAM_TURBINE, ctx.at(3, 1, 8)));
            var g = (GeneratorElement) orient(ctx.placeTransparent(GENERATOR, ctx.at(3, 1, 9)));
            hpLoad = load(g, 2000);
            // (its nozzle is made for 39 g/s at 20 bar: on a few g/s it would let the drum down to 1 bar; a throttle holds
            // the pressure up, as a regulator on its screen would)
            hp.controls.throttle = 0.08;
            // side B: half full of water at 20 bar, saturated; fed from a 25 bar reservoir through a throttle valve barely
            // open (a feed that floods it fills the line and the turbine: the water must come no faster than it boils)
            h.b.fill(0.5 * h.b.volume * Water.RHO_L, 20e5, Water.tsat(20e5));
            PipeCases.lay(ctx, hxAt.north(), 1, 8, 8, 0, 8, 8, 8, 8, 8, 16);
            var feedNode = PipeCases.node(ctx, hxAt.north());
            feedNode.addValve(8, 8, 8, mods.eln.node.voxel.ValvePart.THROTTLE);
            for (var part : feedNode.parts) if (part instanceof mods.eln.node.voxel.ValvePart v) v.setting = FEED_OPEN;
            fx = fixtures(h.fluidLevel(), new Object[]{hxAt.east(), 4, air(hxAt.getY())}, new Object[]{hxAt.north().north(), 3, water(hxAt.getY(), 25e5, 293.15)});
            Eln.simulator.addSlowProcess(watch = dt -> {
                l.tick();
                if (++tick == 1000) {
                    f0 = l.gt.totalFuelJ;
                    g0 = l.gt.totalWork;
                    s0 = hp.totalWork;
                    k0 = hp.totalKg;
                    j0 = h.totalJ;
                }
                if (tick > 1000) window += dt;
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            Eln.simulator.removeElectricalComponent(l.load);
            Eln.simulator.removeElectricalComponent(hpLoad);
            mods.eln.fluid.FluidWorld.remove(fx);
            double fuel = (l.gt.totalFuelJ - f0) / window, gas = (l.gt.totalWork - g0) / window, st = (hp.totalWork - s0) / window,
                kg = (hp.totalKg - k0) / window, q = (h.totalJ - j0) / window;
            ctx.line(String.format("combined cycle: fuel %.1f kW; gas turbine %.1f kW (%.1f %%); exhaust -> %.1f kW raising %.1f g/s of steam at %.1f bar; "
                    + "HP %.1f kW; together %.1f kW = %.1f %% of the fuel (+%.0f %% over the gas turbine alone)",
                fuel / 1000, gas / 1000, gas / fuel * 100, q / 1000, kg * 1000, h.b.P() / 1e5, st / 1000, (gas + st) / 1000, (gas + st) / fuel * 100, st / Math.max(1, gas) * 100));
            ctx.line("combined cycle: HX " + h.getWaila() + "; HP " + hp.getWaila());
            ctx.check("combined cycle: the exhaust raises steam that drives the HP (over 0.5 kW)", st > 500, String.format("%.1f kW", st / 1000));
            ctx.check("combined cycle: together they beat the gas turbine alone", gas + st > 1.05 * gas, "");
        }
    }
}
