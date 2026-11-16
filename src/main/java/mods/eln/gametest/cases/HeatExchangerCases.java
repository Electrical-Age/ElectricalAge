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
 * mistakes (both streams in at the same end; the cooling side dead-ended); an exchanger saved at 8 L a side loaded at 1 B.
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
        double j0, window;

        abstract int measureFrom();

        void watch() {
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == measureFrom()) {
                    j0 = h.totalJ;
                    if (a != null) a.start();
                    b.start();
                }
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
            // (the feed pump's flow, set by a throttled port: ~5 g/s through one block, ~10 through the stack; the feed side's
            // own drop is small, and with only a few Pa across it its water would circulate by its own warmth instead)
            FluidNode steamIn = steam(y, 2.4e5), drain = heldWater(y, Water.P_ATM, 293.15), feedIn = heldWater(y, 16e5, 293.15),
                feedOut = heldWater(y, 15e5, 293.15);
            PlumbingCases.fill(h.b, 0.999 * h.b.volume * Water.RHO_L, 16e5, 293.15);
            b = new Stream(feedIn, feedOut);
            a = new Stream(steamIn, drain);
            double feedK = big ? FEED_K * 2 : FEED_K;
            fx = fixtures(h.fluidLevel(),
                new Object[]{ctx.at(west, 1, 3), 5, steamIn}, new Object[]{ctx.at(east, 1, 3), 4, drain, FluidDevice.FluidPort.CENTRE, TRAP_K},
                new Object[]{ctx.at(east, 1, 3), 4, feedIn, B_AT, feedK}, new Object[]{ctx.at(west, 1, 3), 5, feedOut, B_AT});
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
            ctx.check("feed heater: the feed's heat gain is the exchanger's duty (within 10 %)", Math.abs(qb - q) < 0.1 * Math.abs(q) + 20,
                String.format("%.2f vs %.2f kW", qb / 1000, q / 1000));
            ctx.check("feed heater: steam condenses in side A and its trap drains it", h.a.mLiquid() > 1e-4 && a.arrived(window) > 0.3 * q / 2.2e6,
                String.format("%.2f g held, %.2f g/s drained", h.a.mLiquid() * 1000, a.arrived(window) * 1000));
            if (big) {
                ctx.check("feed heater 2x2x2: duty 3-6 kW", q > 3000 && q < 6000, String.format("%.2f kW", q / 1000));
                ctx.check("feed heater 2x2x2: the feed takes on 100 K or more", tOut - tIn > 100, String.format("%.0f K", tOut - tIn));
            } else {
                ctx.check("feed heater, one block: duty 0.3-1.5 kW", q > 300 && q < 1500, String.format("%.2f kW", q / 1000));
                ctx.check("feed heater, one block: the feed warms by 20 K or more", tOut - tIn > 20, String.format("%.0f K", tOut - tIn));
            }
        }
    }

    /**
     * A cooler: hot water (90 C, 3 bar) through side A, west to east; cooling water (20 C, 2 bar) through side B, both
     * 2 x 2 x 4 (200 W/K), ~0.6 bar across each. COUNTER: B east to west, against A (right). PARALLEL: B in at the west
     * end with A (both ends' ports are alike: piping it the wrong way round does this). DEAD_END: B's outlet left off,
     * its water stands.
     */
    static final class Cooler extends Run {
        static final int COUNTER = 0, PARALLEL = 1, DEAD_END = 2;
        static final double DROP = 30;
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
            FluidNode hotIn = heldWater(y, 3e5, 363.15), hotOut = heldWater(y, 3e5 - DROP, 363.15), coldIn = heldWater(y, 2e5, 293.15),
                coldOut = heldWater(y, 2e5 - DROP, 293.15);
            // (start full of the streams' water: a cold start otherwise counts the first fill)
            PlumbingCases.fill(h.a, 0.999 * h.a.volume * Water.RHO_L, 3e5, 363.15);
            PlumbingCases.fill(h.b, 0.999 * h.b.volume * Water.RHO_L, 2e5, 293.15);
            List<Object[]> ports = new ArrayList<>();
            ports.add(new Object[]{ctx.at(0, 1, 3), 5, hotIn});
            ports.add(new Object[]{ctx.at(5, 1, 3), 4, hotOut});
            if (mode == PARALLEL) {
                ports.add(new Object[]{ctx.at(0, 1, 4), 5, coldIn, B_AT});
                ports.add(new Object[]{ctx.at(5, 1, 4), 4, coldOut, B_AT});
            } else {
                ports.add(new Object[]{ctx.at(5, 1, 4), 4, coldIn, B_AT});
                if (mode == COUNTER) ports.add(new Object[]{ctx.at(0, 1, 4), 5, coldOut, B_AT});
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
            ctx.check("cooler: the hot stream's heat loss is the duty (within 10 %)", Math.abs(qa - q) < 0.1 * Math.abs(q) + 20,
                String.format("%.2f vs %.2f kW", qa / 1000, q / 1000));
            switch (mode) {
                case COUNTER -> {
                    ctx.check("cooler counterflow: 62-75 % of the most (NTU ~1.9: 65 %)", q > 0.62 * most && q < 0.75 * most, String.format("%.0f %%", q / most * 100));
                    ctx.check("cooler counterflow: the cooling water leaves hotter than the hot water does", tBout > tAout,
                        String.format("%.0f vs %.0f C", tBout, tAout));
                }
                case PARALLEL -> {
                    // (textbook parallel flow at NTU 1.9 gives 49 %; the side's slices mix along it, so it reads ~61 %, still
                    // under counterflow's ~68 %)
                    ctx.check("cooler parallel: 50-65 % of the most, under counterflow", q > 0.5 * most && q < 0.65 * most, String.format("%.0f %%", q / most * 100));
                }
                default -> {
                    ctx.check("cooler dead-ended: no cooling water flows", Math.abs(mb) < 2e-4, String.format("%.2f g/s", mb * 1000));
                    // (its standing water only warms: the heat goes into it, not out with a stream)
                    ctx.check("cooler dead-ended: under half what counterflow gives (5 kW)", q < 2500, String.format("%.2f kW", q / 1000));
                }
            }
        }
    }

    /**
     * Saves from before the resize (8 L a side, no "sideV" key), full of water at 11 bar and 60 C: one block and one of
     * a 2 x 2 x 1 stack. Loaded, each side is 1 B at the state it had (11 bar, 60 C), what it held over that vented and
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
}
