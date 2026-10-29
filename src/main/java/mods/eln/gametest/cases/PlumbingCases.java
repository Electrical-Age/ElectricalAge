package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.PipeSensorPart;
import mods.eln.node.voxel.ValvePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.oil.InlinePumpElement;
import mods.eln.oil.TankElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;

import java.util.List;

import static mods.eln.gametest.cases.Wp11Cases.SOURCE;

/**
 * The shaft redo's plumbing (Baughn 2026-10-29): tanks, the inline pump, the pressure valve, the steam quality sensor.
 */
public final class PlumbingCases {
    private PlumbingCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TankSeparates());
        cases.add(new PumpMovesWater(false));
        cases.add(new PumpMovesWater(true));
        cases.add(new PressureValve(ValvePart.REDUCING));
        cases.add(new PressureValve(ValvePart.RELIEF));
        cases.add(new QualitySensor());
        cases.add(new TankBox());
        cases.add(new TankBoxSplits());
        cases.add(new HotWaterIntoShortPipe());
        cases.add(new ValveMeters());
    }

    static final int IRON_TANK = 40 + (82 << 6), ALLOY_TANK = 44 + (82 << 6), PUMP = 48 + (82 << 6);

    /** A big test box (10 x 5 x 10, floored). */
    abstract static class Big implements TestCase {
        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }
    }

    static TankElement tank(TestContext ctx, int damage, BlockPos p, Direction front) {
        TankElement t = (TankElement) ctx.placeTransparent(damage, p);
        t.front = front;
        t.reconnect();
        return t;
    }

    /** Set a node's contents: liquid water (kg) at pressure p, temperature T, the rest steam/air as fill() makes it. */
    static void fill(FluidNode n, double liquidKg, double p, double T) {
        n.fill(liquidKg, p, T);
        n.update();
    }

    /**
     * A tank as a separator: tank A half full of water at 10 bar (saturated), its top port piped to an empty tank B.
     * Steam goes over off the top; A keeps its water (all but what flashes as its pressure falls).
     */
    static final class TankSeparates extends Big {
        TankElement a, b;
        double aWater0;

        public String id() {
            return "tank_top_port_gives_steam";
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            BlockPos pa = ctx.at(2, 1, 1), pb = ctx.at(4, 1, 1);
            a = tank(ctx, ALLOY_TANK, pa, Direction.XN);
            b = tank(ctx, ALLOY_TANK, pb, Direction.XN);
            PipeCases.lay(ctx, pa.above(), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, pa.above().east(), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, pb.above(), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            double T = Water.tsat(10e5);
            fill(a.vessel, 0.5 * a.vessel.volume * Water.RHO_L, 10e5, T);
            aWater0 = a.vessel.mLiquid();
        }

        public void measure(TestContext ctx) {
            ctx.line("tank separator: A " + a.getWaila() + "; B " + b.getWaila());
            ctx.check("tank: steam went over the top to B", b.vessel.mWater > 0.001, String.format("%.4f kg", b.vessel.mWater));
            ctx.check("tank: A kept its water (over 80%)", a.vessel.mLiquid() > 0.8 * aWater0, String.format("%.3f of %.3f kg", a.vessel.mLiquid(), aWater0));
            ctx.check("tank: B got far less water than A had", b.vessel.mWater < 0.2 * aWater0, String.format("%.4f kg", b.vessel.mWater));
        }
    }

    /**
     * The inline pump on 200 V: tank A's low port -> pipe -> pump -> pipe -> tank B's low port. With cold water in A it
     * moves it to B and draws power; with A full of water at its boiling point (1 atm, 100 C) it cavitates and moves
     * next to nothing.
     */
    static final class PumpMovesWater extends Big {
        final boolean boiling;
        TankElement a, b;
        InlinePumpElement p;
        IProcess watch;
        double bWater0, maxW;
        boolean sawCavitate;

        PumpMovesWater(boolean boiling) {
            this.boiling = boiling;
        }

        public String id() {
            return boiling ? "inline_pump_cavitates_on_boiling_water" : "inline_pump_moves_water";
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            BlockPos pa = ctx.at(5, 1, 1), pp = ctx.at(5, 1, 3), pb = ctx.at(5, 1, 5);
            a = tank(ctx, IRON_TANK, pa, Direction.XN);   // (its right, +z, faces the pump)
            p = (InlinePumpElement) ctx.placeTransparent(PUMP, pp);
            p.front = Direction.XN;   // (its left, -z: in; its right, +z: out)
            p.reconnect();
            b = tank(ctx, IRON_TANK, pb, Direction.XN);   // (its left, -z, faces the pump)
            PipeCases.lay(ctx, pa.south(), 0, 8, 4, 0, 8, 4, 16);   // (low: the pump's line is low, so it stays flooded)
            PipeCases.lay(ctx, pp.south(), 0, 8, 4, 0, 8, 4, 16);
            BlockPos cable = pp.west(), src = cable.west();
            ctx.placeSix(Ids.six("Medium Voltage Cable"), cable);
            var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(SOURCE, src);
            s.networkUnserialize(ctx.stream(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat(200f);
            }));
            double T = boiling ? Water.TB : Water.T_AMBIENT;
            fill(a.vessel, 0.8 * a.vessel.volume * Water.RHO_L, Water.P_ATM, T);
            bWater0 = b.vessel.mLiquid();
            Eln.simulator.addSlowProcess(watch = dt -> {
                sawCavitate |= boiling ? p.cavitating : p.cavitating && !p.gasBound;
                maxW = Math.max(maxW, p.wantW);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            double moved = b.vessel.mLiquid() - bWater0;
            ctx.line("inline pump" + (boiling ? " (boiling feed)" : "") + ": " + p.getWaila() + "; A " + a.getWaila() + "; B " + b.getWaila());
            if (!boiling) {
                ctx.check("pump: cold water goes from A to B", moved > 0.05, String.format("%.3f kg", moved));
                ctx.check("pump: it draws power doing it", maxW > 50, String.format("%.0f W", maxW));
                ctx.check("pump: it primed itself (passed the line's air on) and then pumped water", p.totalKg > 0.05, String.format("%.3f kg", p.totalKg));
            } else {
                ctx.check("pump: water at its boiling point vapour-locks it (cavitating, or its line full of steam)", sawCavitate, "");
                ctx.check("pump: and it moves next to nothing (under 0.05 kg)", moved < 0.05, String.format("%.3f kg", moved));
            }
        }
    }

    /**
     * The pressure valve between two alloy tanks: A at 10 bar (steam over hot water), B air at 1 atm. Reducing at 3 bar:
     * B settles near 3 bar while A stays higher; relief at 9 bar: A falls to about 9 bar and stops there (B, as big
     * as A, would otherwise have taken it down to ~8.4).
     */
    static final class PressureValve extends Big {
        final int pmode;
        TankElement a, b;
        ValvePart v;

        PressureValve(int pmode) {
            this.pmode = pmode;
        }

        public String id() {
            return pmode == ValvePart.REDUCING ? "pressure_valve_reducing" : "pressure_valve_relief";
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            BlockPos pa = ctx.at(2, 1, 1), pv = ctx.at(3, 1, 1), pb = ctx.at(4, 1, 1);
            a = tank(ctx, ALLOY_TANK, pa, Direction.XN);   // (its back, +x, faces the valve)
            b = tank(ctx, ALLOY_TANK, pb, Direction.XP);   // (its back, -x, faces the valve)
            PipeCases.lay(ctx, pv, 1, 0, 12, 8, 8, 12, 8, 16, 12, 8);   // (between the tanks' high, vapour ports)
            VoxelNode n = PipeCases.node(ctx, pv);
            String err = n == null ? "no pipe node" : n.addValve(8, 12, 8, ValvePart.PRESSURE);
            if (err != null) throw new IllegalStateException("valve: " + err);
            for (var part : n.parts) if (part instanceof ValvePart vp) v = vp;
            v.pmode = pmode;
            v.setPa = (pmode == ValvePart.REDUCING ? 3 : 9) * 1e5;
            v.flow = 1;   // (toward +x: A to B)
            double T = Water.tsat(10e5);
            fill(a.vessel, 0.3 * a.vessel.volume * Water.RHO_L, 10e5, T);
        }

        public void measure(TestContext ctx) {
            double pa = a.vessel.P() / 1e5, pb = b.vessel.P() / 1e5;
            ctx.line("pressure valve: A " + a.getWaila() + "; B " + b.getWaila() + "; pipe " + PipeCases.node(ctx, ctx.at(3, 1, 1)).getWaila(true));
            ctx.line(String.format("pressure valve (%s): A %.2f bar, B %.2f bar, %.0f%% open", pmode == ValvePart.REDUCING ? "reducing 3" : "relief 9", pa, pb, v.lift * 100));
            if (pmode == ValvePart.REDUCING) {
                ctx.check("reducing valve: B held near 3 bar (2.5-3.6)", pb > 2.5 && pb < 3.6, String.format("%.2f bar", pb));
                ctx.check("reducing valve: A stays well above it", pa > 5, String.format("%.2f bar", pa));
            } else {
                // (A, a one-block tank of hot water, cools as it stands: its saturated pressure falls below 9 on its own)
                ctx.check("relief valve: A let down to about 9 bar and held (7.8-9.6)", pa > 7.8 && pa < 9.6, String.format("%.2f bar", pa));
                ctx.check("relief valve: B took what A let go, and no more (1.2 bar to 3 under A's)", pb > 1.2 && pb < pa - 3, String.format("%.2f bar", pb));
            }
        }
    }

    /** A 2 x 2 x 3 box of iron tanks at (2..3, 1..3, 2..3), bottom layer first. */
    static java.util.List<TankElement> box(TestContext ctx) {
        java.util.List<TankElement> out = new java.util.ArrayList<>();
        for (int y = 1; y <= 3; y++)
            for (int x = 2; x <= 3; x++)
                for (int z = 2; z <= 3; z++) out.add((TankElement) ctx.placeTransparent(IRON_TANK, ctx.at(x, y, z)));
        return out;
    }

    static double water(java.util.List<TankElement> ts) {
        java.util.Set<FluidNode> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        double w = 0;
        for (TankElement t : ts) if (seen.add(t.vessel)) w += t.vessel.mLiquid();
        return w;
    }

    /**
     * Baughn 2026-10-29: tank blocks of a tier filling a box are one vessel. A 2 x 2 x 3 box of iron tanks: one node
     * holding 12 x 12 buckets; its ports on the box's outer faces (top: steam to a tank over a pipe; a low side port of
     * the bottom layer: water to another).
     */
    static final class TankBox extends Big {
        java.util.List<TankElement> ts;
        TankElement b, c;
        double cWater0;

        public String id() {
            return "tank_box_merges";
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            ts = box(ctx);
            // a tank B west of it, its top piped to the box's top; a tank C east of it, piped to a low port of its bottom layer
            b = tank(ctx, ALLOY_TANK, ctx.at(0, 1, 2), Direction.XN);
            c = tank(ctx, ALLOY_TANK, ctx.at(5, 1, 3), Direction.XP);   // (its back, -x, faces the pipe)
            PipeCases.lay(ctx, ctx.at(2, 4, 2), 1, 8, 0, 8, 8, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(1, 4, 2), 1, 16, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(0, 4, 2), 1, 16, 8, 8, 8, 8, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(0, 3, 2), 1, 8, 16, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(0, 2, 2), 1, 8, 16, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(4, 1, 3), 1, 0, 4, 8, 16, 4, 8);
            // half full of water at 10 bar, saturated
            var g = ts.get(0).group;
            if (g != null) fill(g.node, 0.5 * g.node.volume * Water.RHO_L, 10e5, Water.tsat(10e5));
            cWater0 = c.vessel.mLiquid();
        }



        public void measure(TestContext ctx) {
            var g = ts.get(0).group;
            boolean one = g != null;
            for (TankElement t : ts) one &= t.group == g && t.vessel == g.node;
            ctx.check("tank box: all 12 blocks are one vessel, 2 x 3 x 2", one && g.sx == 2 && g.sy == 3 && g.sz == 2,
                g == null ? "no group" : g.sx + " x " + g.sy + " x " + g.sz + ", " + g.members.size() + " blocks");
            if (g == null) return;
            double buckets = g.node.volume * Water.RHO_L / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / 1000;
            ctx.check("tank box: it holds 12 x 12 buckets", Math.abs(buckets - 144) < 0.5, String.format("%.1f buckets", buckets));
            ctx.line("tank box: " + ts.get(0).getWaila() + "; B " + b.getWaila() + "; C " + c.getWaila());
            ctx.check("tank box: its top port gives steam (B got some; far less than C's water)", b.vessel.mWater > 0.0003 && b.vessel.mWater < 0.2 * (c.vessel.mLiquid() - cWater0),
                String.format("%.4f kg", b.vessel.mWater));
            ctx.check("tank box: a low port of its bottom layer gives water (C got water)", c.vessel.mLiquid() > cWater0 + 0.01,
                String.format("%.3f kg", c.vessel.mLiquid() - cWater0));
            // (Baughn: a box draws a flange only where a pipe meets it)
            int withPipes = 0, top = 0, east = 0;
            for (TankElement t : ts) {
                if (t.coupled != 0) withPipes++;
                var cc = t.node.coordonate;
                if (cc.equals(new mods.eln.misc.Coordonate(ctx.at(2, 3, 2), ctx.level()))) top = t.coupled;
                if (cc.equals(new mods.eln.misc.Coordonate(ctx.at(3, 1, 3), ctx.level()))) east = t.coupled;
            }
            ctx.check("tank box: flanges only where pipes meet it (two blocks: the top one, the east low one)",
                withPipes == 2 && top == 1 << 12 && east == 1 << 10, String.format("%d blocks; top %x, east %x", withPipes, top, east));
        }
    }

    /** Breaking a block of a 2 x 2 x 3 box: what's left isn't a box, so 11 one-block tanks, each with its share. */
    static final class TankBoxSplits extends Big {
        java.util.List<TankElement> ts;
        double before;

        public String id() {
            return "tank_box_splits_on_break";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            ts = box(ctx);
            var g = ts.get(0).group;
            if (g != null) fill(g.node, 0.6 * g.node.volume * Water.RHO_L, Water.P_ATM, 293.15);
            before = water(ts);
            TankElement gone = ts.remove(ts.size() - 1);   // (the top layer's last corner)
            ctx.level().removeBlock(gone.node.coordonate.pos(), false);
        }

        public void measure(TestContext ctx) {
            boolean alone = true;
            for (TankElement t : ts) alone &= t.group == null && t.vessel == t.own;
            double after = water(ts);
            ctx.check("tank box split: 11 one-block tanks (not a box)", alone && ts.size() == 11, "");
            ctx.check("tank box split: they keep 11/12 of the water (by volume)", Math.abs(after - before * 11 / 12) < 0.02 * before,
                String.format("%.3f of %.3f kg", after, before));
        }
    }

    /**
     * A physics bug (Baughn: fix when spotted): hot water at 10 bar from a big vessel through a one-block pipe into a
     * small tank rammed the pipe full in a step and on to 1446 bar, past its burst (the tank's inflow limit cut what the
     * pipe passed on after the pipe's own limit had been checked). Now the pipe never goes far past the pressures
     * around it, nothing bursts, and the water gets through.
     */
    static final class HotWaterIntoShortPipe extends Big {
        TankElement a, b;
        mods.eln.node.voxel.PipePart pipe;
        IProcess watch;
        double maxP, mass0;
        int tick;
        String trace = "";

        public String id() {
            return "pipe_hot_water_short_pipe_no_overfill";
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            // the source: a 2 x 2 x 2 box of alloy tanks (x 1..2, z 1..2); the pipe from its east face's low port to a
            // lone iron tank B (a small vessel whose own inflow limit cut what the pipe passed on)
            for (int y = 1; y <= 2; y++)
                for (int x = 1; x <= 2; x++)
                    for (int z = 1; z <= 2; z++) {
                        TankElement t = (TankElement) ctx.placeTransparent(ALLOY_TANK, ctx.at(x, y, z));
                        if (x == 1 && y == 1 && z == 1) a = t;
                    }
            BlockPos pv = ctx.at(3, 1, 1), pb = ctx.at(4, 1, 1);
            b = tank(ctx, IRON_TANK, pb, Direction.XP);   // (its back, -x, faces the pipe)
            PipeCases.lay(ctx, pv, 1, 0, 4, 8, 16, 4, 8);   // (between their low, liquid ports)
            VoxelNode n = PipeCases.node(ctx, pv);
            for (var part : n.parts) if (part instanceof mods.eln.node.voxel.PipePart pp) pipe = pp;
            fill(a.vessel, 0.6 * a.vessel.volume * Water.RHO_L, 10e5, Water.tsat(10e5));
            mass0 = a.vessel.mWater + b.vessel.mWater + pipe.fluid.mWater;
            Eln.simulator.addSlowProcess(watch = dt -> {
                var run = mods.eln.fluid.FluidWorld.nodeFor(pipe.fluid);
                maxP = Math.max(maxP, run.P());
                if (++tick <= 6) trace += String.format(" t%d pipe %.1f bar %.4f kg; A %.2f; B %.2f |", tick, run.P() / 1e5, run.mWater, a.vessel.P() / 1e5, b.vessel.P() / 1e5);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            double mass = a.vessel.mWater + b.vessel.mWater + mods.eln.fluid.FluidWorld.nodeFor(pipe.fluid).mWater;
            ctx.line("hot water into a short pipe:" + trace + " max " + String.format("%.1f bar", maxP / 1e5) + "; A " + a.getWaila() + "; B " + b.getWaila());
            ctx.check("short pipe: never far past the 10 bar around it (under 20 bar)", maxP < 20e5, String.format("%.1f bar", maxP / 1e5));
            ctx.check("short pipe: it doesn't burst", !pipe.burst, "");
            ctx.check("short pipe: the water gets through to B", b.vessel.mLiquid() > 0.02, String.format("%.3f kg", b.vessel.mLiquid()));
            ctx.check("short pipe: no water lost", Math.abs(mass - mass0) < 1e-3 * mass0, String.format("%.4f of %.4f kg", mass, mass0));
        }
    }

    /** A big free vessel of water under an air cushion at pa (1 m^3, 70 % water). */
    static FluidNode reservoir(int y, double pa) {
        return new FluidNode(1.0, y, 1.0, false).fill(0.7 * Water.RHO_L, pa, 293.15);
    }

    /** Fixed-free test vessels behind ports: {pos, face, node}; returns the device (remove it at the end). */
    static mods.eln.sim.fluid.FluidDevice vessels(Object level, Object[]... ports) {
        java.util.List<mods.eln.sim.fluid.FluidDevice.FluidPort> list = new java.util.ArrayList<>();
        for (Object[] p : ports)
            list.add(new mods.eln.sim.fluid.FluidDevice.FluidPort((FluidNode) p[2], ((BlockPos) p[0]).asLong(), (Integer) p[1], 0.5,
                2 * mods.eln.node.voxel.PipePart.K_PER_BLOCK[1] * 16));
        var d = new mods.eln.sim.fluid.FluidDevice() {
            public Object fluidLevel() {
                return level;
            }

            public java.util.List<FluidPort> fluidPorts() {
                return list;
            }
        };
        mods.eln.fluid.FluidWorld.add(d);
        return d;
    }

    /**
     * A physics bug (Baughn: fix when spotted): a throttle valve 2 % open passed water as 2 % of a lossless pipe, ~10 kg/s
     * at 5 bar, so no feed could be metered. By the orifice law through an alloy seat it passes ~25 g/s.
     */
    static final class ValveMeters extends Big {
        mods.eln.sim.fluid.FluidDevice fx;
        FluidNode src, sink;
        double m0, flow;
        IProcess watch;
        int tick;

        public String id() {
            return "valve_meters_water_by_orifice_law";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            BlockPos pv = ctx.at(4, 1, 4);
            PipeCases.lay(ctx, pv, 1, 0, 8, 8, 8, 8, 8, 16, 8, 8);
            VoxelNode n = PipeCases.node(ctx, pv);
            n.addValve(8, 8, 8, ValvePart.THROTTLE);
            for (var part : n.parts) if (part instanceof ValvePart v) v.setting = 0.02;
            src = reservoir(pv.getY(), 6e5);
            sink = new FluidNode(10.0, pv.getY(), 1.0, false).fill(0, Water.P_ATM, 293.15);
            fx = vessels(n.fluidLevel(), new Object[]{pv.west(), 5, src}, new Object[]{pv.east(), 4, sink});
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 100) m0 = sink.mWater;
                if (tick > 100) flow = (sink.mWater - m0) / ((tick - 100) * dt);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            mods.eln.fluid.FluidWorld.remove(fx);
            ctx.line(String.format("valve 2 %% open, water 6 bar -> 1 atm: %.1f g/s", flow * 1000));
            ctx.check("a throttle valve 2 % open meters water (10-60 g/s through an alloy seat at ~5 bar)", flow > 0.010 && flow < 0.060,
                String.format("%.1f g/s", flow * 1000));
        }
    }

    /** The steam quality sensor reads a run's wetness: a third of its water liquid reads ~33 % wet; dry steam 0. */
    static final class QualitySensor implements TestCase {
        public String id() {
            return "pipe_steam_quality_sensor";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var s = new PipeSensorPart(8, 8, 8, 1, 4, PipeSensorPart.QUALITY, PipeSensorPart.DEFAULT_PCT);
            FluidNode wet = new FluidNode(0.001, 0, 1, false), dry = new FluidNode(0.001, 0, 1, false);
            double T = Water.tsat(5e5), mv = 5e5 * 0.001 / (Water.R_V * T);   // (saturated vapour filling 1 L)
            dry.mWater = mv;
            dry.energy = mods.eln.sim.fluid.Flash.energyAt(mv, 0, 0.001, T);
            dry.update();
            wet.mWater = 1.5 * mv;   // (plus half as much again as liquid: a third of the water)
            wet.energy = mods.eln.sim.fluid.Flash.energyAt(mv, 0, 0.001, T) + 0.5 * mv * Water.uLiquid(T);
            wet.update();
            s.read(wet);
            double w = s.reading;
            s.read(dry);
            double d = s.reading;
            ctx.check("quality sensor: a third of the water liquid reads about 33 % wet", Math.abs(w - 100.0 / 3) < 5, String.format("%.1f %%", w));
            ctx.check("quality sensor: dry steam reads ~0", d < 1, String.format("%.1f %%", d));
            ctx.check("quality sensor: its name and unit", s.name().equals("Steam quality sensor") && s.unit().equals("% wet"), s.name() + " " + s.unit());
        }
    }
}
