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
        cases.add(new NoSelfStart());
        cases.add(new SteamRaiser());
        cases.add(new StackMergesAndSplits());
        cases.add(new Reheat());
        cases.add(new CombinedCycle());
    }

    static final int GAS_TURBINE = 11 + (4 << 6), HX = 56 + (82 << 6);

    /** Fixed nodes behind ports on the faces of blocks (a lake, a drain, a steam main): {pos, face, node[, spot]} each. */
    static FluidDevice fixtures(Object level, Object[]... ports) {
        List<FluidDevice.FluidPort> list = new ArrayList<>();
        for (Object[] p : ports)
            // (an iron pipe's end: a lake or a main through a modest pipe, not a firehose)
            list.add(new FluidDevice.FluidPort((FluidNode) p[2], ((BlockPos) p[0]).asLong(), (Integer) p[1], 0.5, 2 * mods.eln.node.voxel.PipePart.K_PER_BLOCK[0] * 16,
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
            ctx.check("gas turbine without a starter: Jade says it needs one", l.gt.status().startsWith("Needs a starter"), l.gt.status());
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
            wasStacked = stacked(s, 100) && Math.abs(s.get(0).b.volume - 8 * 0.008) < 1e-9;
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
            ctx.check("stack: 2 x 2 x 2 blocks are one exchanger, 100 W/K, 64 L a side", wasStacked, "");
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
                new Object[]{ctx.at(5, 1, 2), 4, steam(y, 2.4e5), B_AT}, new Object[]{ctx.at(0, 2, 2), 5, steam(y, 2.4e5 - 900), B_AT});
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 300) j0 = head(s).totalJ;
                if (tick > 300) {
                    window += dt;
                    flow += HeatExchangerElement.inflow(head(s).b)[0];
                    n++;
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            mods.eln.fluid.FluidWorld.remove(fx);
            HeatExchangerElement h = head(s);
            double q = (h.totalJ - j0) / window, m = flow / Math.max(1, n);
            ctx.line(String.format("reheat 2x2x4 (%.0f W/K): %.1f kW into %.1f g/s of crossover steam; %s", h.ua(), q / 1000, m * 1000, h.getWaila()));
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
        GasLine l;
        List<HeatExchangerElement> raiser, econ;
        ShaftSteamTurbineElement hp;
        Resistor hpLoad;
        FluidDevice fx;
        IProcess watch;
        AirMeter drain;
        FluidNode feed;
        int tick;
        double f0, g0, s0, k0, j0, e0, w0, x0, window;

        public String id() {
            return "gas_plant_combined_cycle";
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            l = new GasLine(ctx, true, 7000);
            raiser = stack(ctx, ctx.at(3, 1, 5), 2);
            econ = stack(ctx, ctx.at(6, 1, 5), 2);
            // exhaust: raiser east end -> alloy pipe -> economizer west end; water: economizer west end -> valve -> raiser
            // east end (B's spot, up high)
            PipeCases.lay(ctx, ctx.at(5, 1, 5), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(5, 1, 6), 1, 0, 12, 8, 8, 12, 8, 16, 12, 8);
            var valveNode = PipeCases.node(ctx, ctx.at(5, 1, 6));
            valveNode.addValve(8, 12, 8, mods.eln.node.voxel.ValvePart.THROTTLE);
            for (var part : valveNode.parts) if (part instanceof mods.eln.node.voxel.ValvePart v) v.setting = FEED_OPEN;
            // steam: the raiser's west end, top row -> pipe west and south into a separator tank's high side port at (2,2,7)
            // -> its top -> up, south, down into an HP turbine at (2,1,8); its generator at z 9
            PipeCases.lay(ctx, ctx.at(2, 2, 6), 1, 16, 12, 8, 8, 12, 8, 8, 12, 16);
            ctx.level().setBlockAndUpdate(ctx.at(2, 1, 7), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());   // (under it)
            PlumbingCases.tank(ctx, PlumbingCases.ALLOY_TANK, ctx.at(2, 2, 7), Direction.ZP);   // (its back, north, to the pipe)
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
            int y = ctx.at(8, 1, 5).getY();
            drain = new AirMeter(air(y));
            fx = fixtures(head(raiser).fluidLevel(), new Object[]{ctx.at(8, 1, 5), 4, drain.node},
                new Object[]{ctx.at(8, 1, 6), 4, feed = water(y, 16e5, 293.15), B_AT});
            Eln.simulator.addSlowProcess(watch = dt -> {
                l.tick();
                if (++tick == 1000) {
                    f0 = l.gt.totalFuelJ;
                    g0 = l.gt.totalWork;
                    x0 = l.gt.totalExhaustJ;
                    s0 = hp.totalWork;
                    k0 = hp.totalKg;
                    j0 = head(raiser).totalJ;
                    e0 = head(econ).totalJ;
                    w0 = feed.mWater;
                    drain.start();
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
                kg = (hp.totalKg - k0) / window, qr = (head(raiser).totalJ - j0) / window, qe = (head(econ).totalJ - e0) / window,
                ex = (l.gt.totalExhaustJ - x0) / window, left = drain.heat(window), fed = (w0 - feed.mWater) / window;
            ctx.line(String.format("combined cycle: fuel %.1f kW; gas turbine %.1f kW (%.1f %%); exhaust %.1f kW -> raiser %.1f + economizer %.1f kW, %.1f left in air at %.0f C; "
                    + "feed %.1f g/s, HP %.1f g/s at %.1f bar, %.1f kW; together %.1f kW = %.1f %% of the fuel (+%.0f %% over the gas turbine alone)",
                fuel / 1000, gas / 1000, gas / fuel * 100, ex / 1000, qr / 1000, qe / 1000, left / 1000, drain.tOut(window) - Water.T0,
                fed * 1000, kg * 1000, head(raiser).b.P() / 1e5, st / 1000, (gas + st) / 1000, (gas + st) / fuel * 100, st / Math.max(1, gas) * 100));
            ctx.line("combined cycle: raiser " + head(raiser).getWaila() + "; economizer " + head(econ).getWaila() + "; HP " + hp.getWaila());
            ctx.check("combined cycle: two exchangers of 100 W/K", stacked(raiser, 100) && stacked(econ, 100), "");
            ctx.check("combined cycle: heat is conserved (within 5 %)", Math.abs(qr + qe + left - ex) < 0.05 * ex,
                String.format("%.1f + %.1f + %.1f vs %.1f kW", qr / 1000, qe / 1000, left / 1000, ex / 1000));
            ctx.check("combined cycle: the exchangers take most of the exhaust's heat (over 70 %)", qr + qe > 0.7 * ex, String.format("%.0f %%", (qr + qe) / ex * 100));
            ctx.check("combined cycle: the exhaust raises steam that drives the HP (over 1.5 kW)", st > 1500, String.format("%.1f kW", st / 1000));
            ctx.check("combined cycle: together they beat the gas turbine alone by over 20 %", gas + st > 1.2 * gas, "");
        }
    }
}
