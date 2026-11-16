package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.oil.HeatExchangerDescriptor;
import mods.eln.oil.HeatExchangerElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.GasPlantCases.*;

/**
 * The Counterflow Heat Exchanger used as a player would (Baughn 2026-11-15: "a scenario test that demonstrates correct use
 * of the heat exchanger"; the Ledger's page quotes these numbers): a feed-water heater on bleed steam, one block and a 2 x
 * 2 x 2 stack; a cooler, hot water against cooling water through a 2 x 2 x 4 stack, run counterflow, then the two
 * mistakes (both streams in at the same end; the cooling side dead-ended); an exchanger saved at 8 L a side loaded at 1 B;
 * condensate reheat on a furnace's flue in a small plant (a 230V Steam Turbine), with and without.
 * The streams come from and go to big vessels (GasPlantCases.fixtures) on the exchanger's ports, so each stream's flow
 * and the heat it carries are read off its two vessels.
 */
public final class HeatExchangerCases {
    private HeatExchangerCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FeedHeater(false));
        cases.add(new FeedHeater(true));
        cases.add(new Cooler(Cooler.COUNTER));
        cases.add(new Cooler(Cooler.PARALLEL));
        cases.add(new Cooler(Cooler.DEAD_END));
        cases.add(new OldSave());
        cases.add(new CondensateReheat());
    }

    /** A source or sink of water: a lake-sized vessel (1000 m^3, 70 % full) whose pressure barely moves, so its mass and
     *  energy give the stream's flow and the heat it carries. */
    static FluidNode heldWater(int y, double pa, double T) {
        FluidNode n = new FluidNode(1000.0, y, 1.0, false).fill(700 * Water.RHO_L, pa, T);
        n.update();
        return n;
    }

    /** A stream's two vessels: what leaves the source against what reaches the sink, and the heat it took on between. */
    static final class Stream {
        final FluidNode from, to;
        double m0, mTo0, eFrom, eTo;

        Stream(FluidNode from, FluidNode to) {
            this.from = from;
            this.to = to;
        }

        void start() {
            m0 = from.mass();
            mTo0 = to == null ? 0 : to.mass();
            eFrom = from.energy;
            eTo = to == null ? 0 : to.energy;
        }

        /** Its flow (kg/s) over the window. */
        double flow(double window) {
            return (m0 - from.mass()) / window;
        }

        /** What reached its sink (kg/s) over the window. */
        double arrived(double window) {
            return to == null ? 0 : (to.mass() - mTo0) / window;
        }

        /** The heat it took on between its vessels (W; negative: it gave heat up). */
        double gain(double window) {
            return ((to == null ? 0 : to.energy - eTo) + (from.energy - eFrom)) / window;
        }
    }

    /** One exchanger block at p, its front east (XP): A in at its west face, B in at its east face. */
    static HeatExchangerElement lone(TestContext ctx, BlockPos p) {
        var h = (HeatExchangerElement) ctx.placeTransparent(HX, p);
        h.front = Direction.XP;
        h.reconnect();
        return h;
    }

    /** The measuring frame: a run of `ticks`, the last `window` ticks measured. */
    abstract static class Run extends PlumbingCases.Big {
        HeatExchangerElement h;
        FluidDevice fx;
        IProcess watch;
        Stream a, b;
        int tick;
        double j0, jMid, window;

        abstract int measureFrom();

        void watch() {
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == measureFrom()) {
                    j0 = h.totalJ;
                    if (a != null) a.start();
                    b.start();
                }
                if (tick == (measureFrom() + ticks()) / 2) jMid = h.totalJ;
                if (tick > measureFrom()) window += dt;
            });
        }

        void stop() {
            Eln.simulator.removeSlowProcess(watch);
            mods.eln.fluid.FluidWorld.remove(fx);
        }

        double duty() {
            return (h.totalJ - j0) / window;
        }
    }

    /**
     * A feed-water heater: feed water (20 C, 16 bar) through side B, in at the east end and out at the west; bleed steam
     * (2.4 bar, 126 C) through side A the other way, in at the west end, its condensate and what steam is left out at the
     * east into a 1.4 bar drain. One block (12.5 W/K) heats ~6 g/s part way; a 2 x 2 x 2 stack (100 W/K) heats more feed
     * to within a few K of the steam.
     */
    static final class FeedHeater extends Run {
        /** The feed's throttle (m^3): ~5 g/s of water on 1 bar. And the steam side's drain: a trap's orifice, which passes
         *  condensate to 1 atm but little steam. */
        static final double FEED_K = 1.25e-14, TRAP_K = 1e-14;
        final boolean big;

        FeedHeater(boolean big) {
            this.big = big;
        }

        public String id() {
            return big ? "heat_exchanger_feed_heater_stack" : "heat_exchanger_feed_heater_one_block";
        }

        public int ticks() {
            return 1400;
        }

        int measureFrom() {
            return 1000;
        }

        public void build(TestContext ctx) {
            BlockPos at = ctx.at(3, 1, 3);
            int west = 2, east = big ? 5 : 4, y = at.getY();
            h = big ? head(stack(ctx, at, 2)) : lone(ctx, at);
            // (the feed's flow, set by throttled ports a bar each side: ~5 g/s through one block, ~10 through the stack; the
            // exchanger's own drop is a few Pa, and with only that across it, water ran back into the source by its own warmth)
            FluidNode steamIn = steam(y, 2.4e5), drain = heldWater(y, Water.P_ATM, 293.15), feedIn = heldWater(y, 16e5, 293.15),
                feedOut = heldWater(y, 14e5, 293.15);
            PlumbingCases.fill(h.b, 0.999 * h.b.volume * Water.RHO_L, 16e5, 293.15);
            b = new Stream(feedIn, feedOut);
            a = new Stream(steamIn, drain);
            double feedK = big ? FEED_K * 2 : FEED_K;
            fx = fixtures(h.fluidLevel(),
                new Object[]{ctx.at(west, 1, 3), 5, steamIn}, new Object[]{ctx.at(east, 1, 3), 4, drain, FluidDevice.FluidPort.CENTRE, TRAP_K},
                new Object[]{ctx.at(east, 1, 3), 4, feedIn, B_AT, feedK}, new Object[]{ctx.at(west, 1, 3), 5, feedOut, B_AT, feedK});
            watch();
        }

        public void measure(TestContext ctx) {
            stop();
            double q = duty(), mb = b.flow(window), qb = b.gain(window), ma = h.sideInflow(true)[0], tIn = 20, tOut = 20 + qb / Math.max(1e-9, mb * 4186),
                tSteam = Water.tsat(h.a.P()) - Water.T0;
            ctx.line(String.format("feed heater %s (%.1f W/K): %.2f kW into %.1f g/s of feed, %.0f -> %.0f C by the heat it carried (steam side at %.2f bar, %.0f C: approach %.0f K); "
                    + "steam side %.2f g/s in; feed arrived %.2f g/s; %.2f g/s of condensate drained; %s",
                big ? "2x2x2" : "one block", h.ua(), q / 1000, mb * 1000, tIn, tOut, h.a.P() / 1e5, tSteam, tSteam - tOut, ma * 1000, b.arrived(window) * 1000, a.arrived(window) * 1000, h.getWaila()));
            ctx.check("feed heater: the feed flows through side B", mb > 0.002, String.format("%.2f g/s", mb * 1000));
            ctx.check("feed heater: the feed's heat gain is the exchanger's duty (within 5 %)", Math.abs(qb - q) < 0.05 * Math.abs(q) + 20,
                String.format("%.2f vs %.2f kW", qb / 1000, q / 1000));
            ctx.check("feed heater: no more than the feed can take up to the steam's temperature", q < 1.02 * mb * 4186 * (tSteam - tIn),
                String.format("%.2f vs %.2f kW", q / 1000, mb * 4186 * (tSteam - tIn) / 1000));
            ctx.check("feed heater: Jade's feed outlet agrees with the heat it carried (within 5 K)", Math.abs(h.tBout - Water.T0 - tOut) < 5,
                String.format("%.0f vs %.0f C", h.tBout - Water.T0, tOut));
            ctx.check("feed heater: steam condenses in side A and its trap drains it", h.a.mLiquid() > 1e-4 && a.arrived(window) > 0,
                String.format("%.2f g held, %.2f g/s drained", h.a.mLiquid() * 1000, a.arrived(window) * 1000));
            if (big) {
                ctx.check("feed heater 2x2x2: duty 3-6 kW", q > 3000 && q < 6000, String.format("%.2f kW", q / 1000));
                ctx.check("feed heater 2x2x2: feed out within 25 K of the steam", tSteam - tOut < 25, String.format("%.0f K", tSteam - tOut));
            } else {
                ctx.check("feed heater, one block: duty 0.3-1.5 kW", q > 300 && q < 1500, String.format("%.2f kW", q / 1000));
                ctx.check("feed heater, one block: the feed warms by 20 K or more", tOut - tIn > 20, String.format("%.0f K", tOut - tIn));
            }
        }
    }

    /**
     * A cooler: hot water (90 C) through side A, west to east; cooling water (20 C) through side B; 2 x 2 x 4 (200 W/K),
     * each stream ~23 g/s through throttled ports, a bar across each. COUNTER: B east to west, against A (right). PARALLEL: B in at the west
     * end with A (both ends' ports are alike: piping it the wrong way round does this). DEAD_END: B's outlet left off,
     * its water stands.
     */
    static final class Cooler extends Run {
        static final int COUNTER = 0, PARALLEL = 1, DEAD_END = 2;
        /** Each port's throttle (m^3): ~25 g/s on a bar across each, inlet and outlet. */
        static final double PORT_K = 6e-14;
        final int mode;

        Cooler(int mode) {
            this.mode = mode;
        }

        public String id() {
            return "heat_exchanger_cooler_" + new String[]{"counterflow", "parallel_flow", "dead_ended"}[mode];
        }

        public int ticks() {
            return 3000;
        }

        int measureFrom() {
            return 2400;
        }

        public void build(TestContext ctx) {
            BlockPos at = ctx.at(1, 1, 3);
            int y = at.getY();
            h = head(stack(ctx, at, 4));
            FluidNode hotIn = heldWater(y, 4e5, 363.15), hotOut = heldWater(y, 2e5, 363.15), coldIn = heldWater(y, 4e5, 293.15),
                coldOut = heldWater(y, 2e5, 293.15);
            // (start full of the streams' water: a cold start otherwise counts the first fill)
            PlumbingCases.fill(h.a, 0.999 * h.a.volume * Water.RHO_L, 3e5, 363.15);
            PlumbingCases.fill(h.b, 0.999 * h.b.volume * Water.RHO_L, 2e5, 293.15);
            List<Object[]> ports = new ArrayList<>();
            int c = FluidDevice.FluidPort.CENTRE;
            ports.add(new Object[]{ctx.at(0, 1, 3), 5, hotIn, c, PORT_K});
            ports.add(new Object[]{ctx.at(5, 1, 3), 4, hotOut, c, PORT_K});
            if (mode == PARALLEL) {
                ports.add(new Object[]{ctx.at(0, 1, 4), 5, coldIn, B_AT, PORT_K});
                ports.add(new Object[]{ctx.at(5, 1, 4), 4, coldOut, B_AT, PORT_K});
            } else {
                ports.add(new Object[]{ctx.at(5, 1, 4), 4, coldIn, B_AT, PORT_K});
                if (mode == COUNTER) ports.add(new Object[]{ctx.at(0, 1, 4), 5, coldOut, B_AT, PORT_K});
            }
            a = new Stream(hotIn, hotOut);
            b = new Stream(coldIn, mode == DEAD_END ? null : coldOut);
            fx = fixtures(h.fluidLevel(), ports.toArray(new Object[0][]));
            watch();
        }

        public void measure(TestContext ctx) {
            stop();
            double q = duty(), ma = a.flow(window), mb = b.flow(window), qa = -a.gain(window), qb = b.gain(window);
            // (the streams' outlet temperatures from the heat they carried: the end slices' are mixed nodes)
            double tAout = 90 - qa / Math.max(1e-9, ma * 4186), tBout = mode == DEAD_END ? h.b.T() - Water.T0 : 20 + qb / Math.max(1e-9, mb * 4186);
            double cMin = (mode == DEAD_END ? ma : Math.min(ma, mb)) * 4186, most = cMin * 70;
            ctx.line(String.format("cooler 2x2x4 %s (%.0f W/K): %.2f kW (%.0f %% of the most the smaller stream could take, %.2f kW); hot %.1f g/s %.0f -> %.0f C, "
                    + "cooling %.1f g/s %.0f -> %.0f C; hot stream gives %.2f kW; %s",
                new String[]{"counterflow", "parallel flow", "dead-ended"}[mode], h.ua(), q / 1000, q / most * 100, most / 1000, ma * 1000,
                90.0, tAout, mb * 1000, 20.0, tBout, qa / 1000, h.getWaila()));
            ctx.check("cooler: the hot stream flows (10-40 g/s)", ma > 0.010 && ma < 0.040, String.format("%.1f g/s", ma * 1000));
            // (dead-ended: under 1 kW, the hot side's own warming shows; 0.5 kW slack)
            ctx.check("cooler: the hot stream's heat loss is the duty (within 5 %)", Math.abs(qa - q) < 0.05 * Math.abs(q) + (mode == DEAD_END ? 500 : 20),
                String.format("%.2f vs %.2f kW", qa / 1000, q / 1000));
            ctx.check("cooler: Jade's hot outlet agrees with the heat it carried (within 5 K)", Math.abs(h.tAout - Water.T0 - tAout) < 5,
                String.format("%.0f vs %.0f C", h.tAout - Water.T0, tAout));
            switch (mode) {
                case COUNTER -> {
                    ctx.check("cooler counterflow: 55-72 % of the most (NTU ~2.1: 67 % ideal)", q > 0.55 * most && q < 0.72 * most, String.format("%.0f %%", q / most * 100));
                    ctx.check("cooler counterflow: the cooling water leaves hotter than the hot water does", tBout > tAout,
                        String.format("%.0f vs %.0f C", tBout, tAout));
                }
                case PARALLEL -> {
                    // (textbook parallel flow at NTU 1.9 gives 49 %; the side's slices mix along it, so it reads ~61 %, still
                    // under counterflow's ~68 %)
                    ctx.check("cooler parallel: 42-58 % of the most (NTU ~1.9: 49 %)", q > 0.42 * most && q < 0.58 * most, String.format("%.0f %%", q / most * 100));
                    ctx.check("cooler parallel: the cooling water leaves colder than the hot water", tBout < tAout + 1, String.format("%.0f vs %.0f C", tBout, tAout));
                }
                default -> {
                    ctx.check("cooler dead-ended: no cooling water flows", Math.abs(mb) < 2e-4, String.format("%.2f g/s", mb * 1000));
                    // (its standing water only warms: the heat goes into it, not out with a stream)
                    double first = (jMid - j0) / (window / 2), second = (h.totalJ - jMid) / (window / 2);
                    ctx.check("cooler dead-ended: falling (second half of the window under the first)", second < first,
                        String.format("%.2f then %.2f kW", first / 1000, second / 1000));
                    ctx.check("cooler dead-ended: under half what counterflow gives", q < 0.5 * 0.65 * most, String.format("%.2f kW", q / 1000));
                }
            }
        }
    }

    /**
     * Saves from before the resize (8 L a side, no "sideV" key), full of water at 60 C (33 bar, liquid-full): one block and one of
     * a 2 x 2 x 1 stack. Loaded, each side is 1 B at the state it had (33 bar, 60 C), what it held over that vented and
     * counted (113.5 B less 1 B of water a side); a second save and load vents nothing more.
     */
    static final class OldSave extends PlumbingCases.Big {
        String fail = "";
        String detail = "";

        public String id() {
            return "heat_exchanger_old_save_vents_to_1_bucket";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            lone(ctx, ctx.at(2, 1, 2));
            stack(ctx, ctx.at(5, 1, 2), 1);
            ctx.after(2, () -> {
                for (BlockPos p : new BlockPos[]{ctx.at(2, 1, 2), ctx.at(5, 1, 2)}) {
                    try {
                        double old = HeatExchangerDescriptor.OLD_SIDE_VOLUME, v = HeatExchangerDescriptor.SIDE_VOLUME;
                        FluidNode full = new FluidNode(old, p.getY(), 1.0, false).fill(0.995 * old * Water.RHO_L, 11e5, 333.15);
                        full.update();
                        double p0 = full.P(), t0 = full.T(), w0 = full.mWater;
                        var r = mods.eln.gametest.Reload.node(ctx.level(), p, t -> {
                            var el = OilCases.FuelBySpecies.holding(t, "sideAW");
                            el.remove("sideV");
                            for (String side : new String[]{"sideA", "sideB"}) {
                                el.putDouble(side + "W", full.mWater);
                                el.putDouble(side + "A", full.mAir);
                                el.putDouble(side + "U", full.energy);
                            }
                            return t;
                        });
                        var h = (HeatExchangerElement) ((mods.eln.node.transparent.TransparentNode) r.fresh).element;
                        FluidNode a = h.ownA;
                        double vented = h.ventedA[0], kept = a.mWater;
                        String what = String.format("%s: own side %.3g m^3, %.2f bar %.0f C (saved %.2f bar %.0f C), kept %.1f g + vented %.1f g of %.1f g; Jade %s",
                            p.toShortString(), a.volume, a.P() / 1e5, a.T() - Water.T0, p0 / 1e5, t0 - Water.T0, kept * 1000, vented * 1000, w0 * 1000, h.getWaila());
                        detail += what + "; ";
                        if (Math.abs(a.volume - v) > 1e-12) fail += "volume; ";
                        if (Math.abs(vented - w0 * (1 - v / old)) > 1e-6 * w0) fail += "vented " + String.format("%.1f g", vented * 1000) + "; ";
                        // (a stack member's own share is the stack's once it re-forms: its state is the stack's, so only a lone
                        // block shows the saved state as it is)
                        if (h.group == null) {
                            if (Math.abs(a.P() - p0) > 0.03 * p0) fail += "pressure " + String.format("%.2f", a.P() / 1e5) + " bar; ";
                            if (Math.abs(a.T() - t0) > 0.5) fail += "temperature; ";
                            if (Math.abs(kept + vented - w0) > 1e-6 * w0) fail += "mass not kept + vented; ";
                        } else if (h.group.a.mWater < 0.99 * w0 * v / old) fail += "the stack lost the kept water; ";
                        if (Math.abs(h.ventedB[0] - vented) > 1e-6 * w0) fail += "side B vented differently; ";
                        if (!h.getWaila().containsKey(mods.eln.i18n.I18N.tr("Resized"))) fail += "no Jade line; ";
                        // (saved again with its volume key: loads as it is)
                        var r2 = mods.eln.gametest.Reload.node(ctx.level(), p);
                        var h2 = (HeatExchangerElement) ((mods.eln.node.transparent.TransparentNode) r2.fresh).element;
                        // (a stack member's own share is the stack's by volume once saved again: only a lone block keeps exactly its own)
                        boolean alone = h2.group == null;
                        if (Math.abs(h2.ventedA[0] - vented) > 1e-9 || alone && Math.abs(h2.ownA.mWater - kept) > 1e-4 * kept) fail += "second load changed it ("
                            + String.format("%.1f g vented, %.1f g kept", h2.ventedA[0] * 1000, h2.ownA.mWater * 1000) + "); ";
                    } catch (ReflectiveOperationException e) {
                        fail += e + "; ";
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            ctx.line("old save: " + detail);
            ctx.check("old save: each side 1 B at the saved pressure and temperature, the rest vented and shown, once", fail.isEmpty(), fail);
        }
    }

    /**
     * Condensate reheat in a small plant (Baughn 2026-11-16: "reheating of the condenser output in a smaller plant with
     * just a 230V steam turbine"). Two plants side by side, the same but for the flue: a boiler fired at a fixed FIRE W (a
     * furnace's 80 % share of its fire, Flue.HEAT_SHARE), an iron pipe from its top to a loaded 230V Steam Turbine, a Steam
     * Condenser on its exhaust, and its pump's condensate back into the boiler's feed flange through a 2 x 2 x 1 exchanger
     * (50 W/K; the water on A, west out of its front). In the second plant the exchanger's B side takes the furnace's
     * flue: air at 300 C (Flue.SMOKE_K), FIRE x 35/80 over the ambient (Flue.SMOKE_SHARE), put into a 2 L node a tick at a
 * time as a furnace's Flue does, in at its west end and out at
     * its east end, against the water. The first plant's exchanger is sealed: only a pipe. Same fire, so what the flue
     * plant's turbine makes over the other's is what the reheat buys.
     */
    static final class CondensateReheat extends PlumbingCases.Big {
        static final double FIRE = 12000;
        /** The flue's smoke over the ambient (W): what a fire whose 80 % is FIRE sends up it (Flue.SMOKE_SHARE). It goes into
         *  a 2 L node (Flue.VOLUME) a tick at a time, before each fluid step, as Flue.step does, through a flange's port
         *  (PortBridge.PORT_K); out through FLUE_K (m^3) to the air. */
        static final double FLUE_W = FIRE * mods.eln.fluid.Flue.SMOKE_SHARE / mods.eln.fluid.Flue.HEAT_SHARE, FLUE_K = 1.15e-9;
        /** The boiler pressure each turbine's throttle holds (Pa): the player's regulator, as on a real plant. */
        static final double HOLD_P = 8e5;
        static final int WARM = 3200;
        final mods.eln.oil.BoilerElement[] boiler = new mods.eln.oil.BoilerElement[2];
        final mods.eln.oil.SteamGeneratorElement[] turbine = new mods.eln.oil.SteamGeneratorElement[2];
        final mods.eln.oil.CondenserElement[] cond = new mods.eln.oil.CondenserElement[2];
        @SuppressWarnings("unchecked")
        final List<HeatExchangerElement>[] hx = new List[2];
        FluidDevice fx;
        FluidNode flueIn;
        IProcess fire;
        int tick;
        final double[] power = new double[2], j0 = new double[2], pumped0 = new double[2], pBoiler = new double[2], tFeed = new double[2];
        final double[] water0 = new double[2], firstHalf = new double[2];
        double flue0, window;

        public String id() {
            return "heat_exchanger_condensate_reheat";
        }

        public int ticks() {
            return WARM + 800;
        }

        /** One plant at z offset oz: boiler (1,1,1), turbine (4,1,1) and its load (5,1,1), condenser (4,1,2), exchanger
         *  (2,1..2,2..3), the condensate (3,1,2) -> exchanger -> (1,1,2) -> the boiler's feed flange. */
        void plant(TestContext ctx, int i, int oz) {
            var b = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), ctx.at(1, 1, 1 + oz));
            b.front = Direction.XN;   // (feed flange south)
            b.reconnect();
            b.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            b.fromTanks(5600, 0);
            b.showTanks();
            boiler[i] = b;
            var t = (mods.eln.oil.SteamGeneratorElement) ctx.placeTransparent(16 + (82 << 6), ctx.at(4, 1, 1 + oz));
            t.front = Direction.XN;
            t.reconnect();
            turbine[i] = t;
            var h = (mods.eln.oil.ElectricHeaterElement) ctx.placeTransparent(4 + (82 << 6), ctx.at(5, 1, 1 + oz));
            h.front = Direction.ZN;
            h.reconnect();
            ctx.placeTransparent(32 + (82 << 6), ctx.at(4, 1, 2 + oz));
            cond[i] = OilCases.condenserOf(ctx, ctx.at(4, 1, 2 + oz));
            for (int k = 0; k < 4 && cond[i].front != Direction.XN; k++) mods.eln.fluid.FluidPlacement.turn(cond[i]);
            List<HeatExchangerElement> s = new ArrayList<>();
            for (int y = 1; y <= 2; y++)
                for (int z = 2; z <= 3; z++) s.add((HeatExchangerElement) ctx.placeTransparent(HX, ctx.at(2, y, z + oz)));
            for (HeatExchangerElement e : s) {
                e.front = Direction.XN;   // (A in at its back, east, out at its front, west; B in at the west, out east)
                e.reconnect();
            }
            hx[i] = s;
            // steam: up out of the boiler, east, down into the turbine
            PipeCases.lay(ctx, ctx.at(1, 2, 1 + oz), 0, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(2, 2, 1 + oz), 0, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(3, 2, 1 + oz), 0, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(4, 2, 1 + oz), 0, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            // condensate: west out of the condenser's front into the exchanger's east end (A), out its west end, north
            // into the boiler's feed flange
            PipeCases.lay(ctx, ctx.at(3, 1, 2 + oz), 0, 16, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(1, 1, 2 + oz), 0, 16, 8, 8, 8, 8, 8, 8, 8, 0);
        }

        public void build(TestContext ctx) {
            plant(ctx, 0, 0);
            plant(ctx, 1, 5);
            int y = ctx.at(1, 1, 8).getY();
            flueIn = new FluidNode(0.002, y, 0.25, false).fill(0, Water.P_ATM, mods.eln.fluid.Flue.SMOKE_K);
            FluidNode flueOut = new FluidNode(1.0, y, 1.0, true).fill(0, Water.P_ATM, 293.15);
            int hi = mods.eln.oil.TankElement.HIGH;
            fx = fixtures(head(hx[1]).fluidLevel(),
                new Object[]{ctx.at(1, 1, 8), 5, flueIn, mods.eln.oil.TankElement.sideSpot(5, hi), mods.eln.fluid.PortBridge.PORT_K},
                new Object[]{ctx.at(3, 1, 8), 4, flueOut, mods.eln.oil.TankElement.sideSpot(4, hi), FLUE_K});
            Eln.simulator.addSlowProcess(fire = dt -> {
                for (int i = 0; i < 2; i++) {
                    if (cond[i].vessel == null || boiler[i].vessel == null) return;
                }
                if (tick == 0) for (int i = 0; i < 2; i++) {
                    // (as fluid_closed_plant_feed_loop: the condenser in its pump's level band, the feed run full)
                    mods.eln.sim.fluid.EaUnits.addWater(cond[i].vessel, 0.28 * cond[i].vessel.volume * Water.RHO_L, 320, false);
                    var run = mods.eln.fluid.FluidWorld.nodeFor(boiler[i].feed);
                    run.fill(0.995 * run.volume * Water.RHO_L, Water.P_ATM, 300);
                    run.update();
                    // (the exchangers' water sides full of condensate)
                    var a = head(hx[i]).a;
                    PlumbingCases.fill(a, 0.97 * a.volume * Water.RHO_L, Water.P_ATM, 320);
                }
                // (the furnace's smoke, as Flue.step: this tick's, at 300 C, in one go)
                double smoke = FLUE_W * dt / (Water.hAir(mods.eln.fluid.Flue.SMOKE_K) - Water.hAir(293.15));
                flueIn.mAir += smoke;
                flueIn.energy += smoke * Water.hAir(mods.eln.fluid.Flue.SMOKE_K);
                flueIn.update();
                for (int i = 0; i < 2; i++) {
                    boiler[i].thermal.moveEnergyTo(FIRE * dt);
                    var c = turbine[i].controls;
                    c.throttle = Math.max(0.02, Math.min(1, c.throttle + dt * 0.05 * (boiler[i].vessel.P() - HOLD_P) / 1e5));
                }
                if (++tick == WARM) {
                    for (int i = 0; i < 2; i++) {
                        j0[i] = head(hx[i]).totalJ;
                        pumped0[i] = cond[i].v.totalPumpedKg;
                        water0[i] = boiler[i].vessel.mWater + cond[i].vessel.mWater;
                    }
                }
                if (tick > WARM) {
                    window += dt;
                    for (int i = 0; i < 2; i++) {
                        power[i] += turbine[i].source.getEffectiveP() * dt;
                        if (tick == WARM + 400) firstHalf[i] = power[i];
                        pBoiler[i] += boiler[i].vessel.P() * dt;
                        tFeed[i] += head(hx[i]).tAout * dt;
                    }
                    flue0 += head(hx[1]).sideInflow(false)[0] * dt;
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(fire);
            mods.eln.fluid.FluidWorld.remove(fx);
            double[] p = new double[2], q = new double[2], fed = new double[2];
            for (int i = 0; i < 2; i++) {
                p[i] = power[i] / window;
                q[i] = -(head(hx[i]).totalJ - j0[i]) / window;   // (B, the flue, to A, the water)
                fed[i] = (cond[i].v.totalPumpedKg - pumped0[i]) / window;
                String name = i == 0 ? "no reheat" : "flue reheat";
                ctx.line(String.format("condensate reheat, %s: fire %.1f kW; turbine %.0f W; exchanger %.2f kW into %.2f g/s of condensate, out at %.0f C; boiler %.2f bar; "
                        + "turbine throttle %.2f; boiler %s; turbine %s; condenser %s; exchanger %s", name, FIRE / 1000, p[i], q[i] / 1000, fed[i] * 1000, tFeed[i] / window - Water.T0,
                    pBoiler[i] / window / 1e5, turbine[i].controls.throttle, boiler[i].getWaila(), turbine[i].getWaila(), cond[i].getWaila(), head(hx[i]).getWaila()));
            }
            double[] dw = new double[2];
            for (int i = 0; i < 2; i++) dw[i] = (boiler[i].vessel.mWater + cond[i].vessel.mWater - water0[i]) / window;
            ctx.line(String.format("condensate reheat: boiler and condenser water %+.2f and %+.2f g/s over the window", dw[0] * 1000, dw[1] * 1000));
            // (steady: each turbine's power over the window's second half within 3 % of its first; the boilers lose a little
            // water, as a plant does, the condensers' ejectors and makeup floats left to themselves, the same in both)
            double[] drift = new double[2];
            for (int i = 0; i < 2; i++) drift[i] = (power[i] - 2 * firstHalf[i]) / Math.max(1, firstHalf[i]);
            ctx.check("condensate reheat: steady (each turbine's power over the window's halves within 3 %)", Math.abs(drift[0]) < 0.03 && Math.abs(drift[1]) < 0.03,
                String.format("%+.1f %%, %+.1f %%", drift[0] * 100, drift[1] * 100));
            double flue = flue0 / window, flueMax = flue * 1005 * (mods.eln.fluid.Flue.SMOKE_K - 293.15);
            ctx.line(String.format("condensate reheat: flue %.1f g/s at 300 C (%.2f kW over 20 C); the reheat %.2f kW (%.0f %% of it), the turbine +%.0f W (+%.0f %%)",
                flue * 1000, flueMax / 1000, q[1] / 1000, q[1] / Math.max(1, flueMax) * 100, p[1] - p[0], (p[1] / Math.max(1, p[0]) - 1) * 100));
            ctx.check("condensate reheat: two exchangers of 50 W/K", stacked(hx[0], 50) && stacked(hx[1], 50), "");
            ctx.check("condensate reheat: both turbines make power", p[0] > 200 && p[1] > 200, String.format("%.0f, %.0f W", p[0], p[1]));
            ctx.check("condensate reheat: the sealed exchanger moves next to nothing (under 50 W)", Math.abs(q[0]) < 50, String.format("%.0f W", q[0]));
            ctx.check("condensate reheat: the flue warms the condensate (over 1 kW)", q[1] > 1000, String.format("%.2f kW", q[1] / 1000));
            ctx.check("condensate reheat: the same fire makes more power with it (over 10 %)", p[1] > 1.1 * p[0], String.format("%.0f vs %.0f W", p[1], p[0]));
        }
    }
}
