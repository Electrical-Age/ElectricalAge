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
        cases.add(new TankBoxSettlesAfterBreak());
        cases.add(new TankBoxHitbox());
        cases.add(new TankBoxCornerPipeClick());
        cases.add(new TankFlanges());
        cases.add(new TankSightGlass());
        cases.add(new HotWaterIntoShortPipe());
        cases.add(new ValveMeters());
        cases.add(new LiquidStaysIncompressible());
        cases.add(new TankTakesOil());
        cases.add(new TankStandsWithoutFloor());
    }

    static final int IRON_TANK = 40 + (82 << 6), ALLOY_TANK = 44 + (82 << 6), PUMP = 48 + (82 << 6);

    /**
     * A tank takes any liquid a species is through its fluid capability (Baughn 2026-11-03: data-driven fluids):
     * gasoline goes in as gasoline, water put in after it settles under it (denser), the capability then shows the top
     * layer (gasoline) and gives it back out; lava (no species) is refused.
     */
    static final class TankTakesOil implements TestCase {
        BlockPos at;
        TankElement t;
        int putGas, putWater, lava, took = -1;
        String shownAfter = "";

        public String id() {
            return "tank_takes_oil";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 30;
        }

        net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> cap(TestContext ctx) {
            return ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.UP);
        }

        public void build(TestContext ctx) {
            at = ctx.at(1, 1, 1);
            t = tank(ctx, IRON_TANK, at, Direction.XN);
            var cap = cap(ctx);
            if (cap == null) return;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                putGas = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(mods.eln.oil.Oil.Cut.GASOLINE.source.get()), 2000, tx);
                lava = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA), 1000, tx);
                tx.commit();
            }
        }

        public void measure(TestContext ctx) {
            var cap = cap(ctx);
            ctx.check("tank: it has a fluid capability", cap != null, "");
            if (cap == null || t.vessel == null) return;
            int gas = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:gasoline");
            double inTank = t.vessel.mass(gas) / mods.eln.fluid.FluidSpecies.kgPerMb(gas, false);
            ctx.check("tank: 2000 mB of gasoline went in", putGas == 2000, putGas + " mB");
            ctx.check("tank: it is gasoline in the vessel", inTank > 1990 && inTank < 2001, inTank + " mB");
            ctx.check("tank: lava (no species) is refused", lava == 0, lava + " mB");
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                putWater = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 1000, tx);
                tx.commit();
            }
            t.syncWater();
            t.vessel.update();
            ctx.check("tank: water goes in beside it", putWater == 1000, putWater + " mB");
            ctx.check("tank: the water settles under the gasoline", t.vessel.layerOf(mods.eln.sim.fluid.SpeciesRegistry.WATER) == 0 && t.vessel.layerOf(gas) == 1,
                "water layer " + t.vessel.layerOf(mods.eln.sim.fluid.SpeciesRegistry.WATER) + ", gasoline " + t.vessel.layerOf(gas));
            shownAfter = String.valueOf(cap.getResource(0).getFluid());
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                took = cap.extract(net.neoforged.neoforge.transfer.fluid.FluidResource.of(mods.eln.oil.Oil.Cut.GASOLINE.source.get()), 500, tx);
                tx.commit();
            }
            ctx.check("tank: it shows the top layer, gasoline, and gives it back", took == 500, took + " mB, shown " + shownAfter);
            var info = t.getWaila();
            ctx.check("tank: Jade lists the gasoline", String.valueOf(info.get("Contents")).contains(" mB of gasoline"), "" + info.get("Contents"));
        }
    }

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

    /**
     * A tank needs no floor (Baughn 2026-11-10): tank A stands on a stone block, the block is taken away (it used to
     * break the tank), and a pipe in its place runs from A's underside port down into tank B's top port. A stays, and
     * its water runs down into B (slowly: both are sealed, so B's air has to climb the same pipe, ~2 g/s).
     */
    static final class TankStandsWithoutFloor extends Big {
        TankElement a, b;
        BlockPos pa;
        double aWater0;

        public String id() {
            return "tank_stands_without_a_floor";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            pa = ctx.at(2, 3, 1);
            BlockPos under = ctx.at(2, 2, 1);
            ctx.level().setBlock(under, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            a = tank(ctx, IRON_TANK, pa, Direction.XN);
            ctx.level().removeBlock(under, false);
            PipeCases.lay(ctx, under, 0, 8, 16, 8, 8, 0, 8);
            b = tank(ctx, IRON_TANK, ctx.at(2, 1, 1), Direction.XN);
            fill(a.vessel, 0.5 * a.vessel.volume * Water.RHO_L, Water.P_ATM, 300);
            aWater0 = a.vessel.mLiquid();
        }

        public void measure(TestContext ctx) {
            boolean stands = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pa, ctx.level()))
                instanceof mods.eln.node.transparent.TransparentNode n && n.element == a;
            ctx.line("tank without a floor: A " + a.getWaila() + "; B " + b.getWaila());
            ctx.check("tank: it stays when the block under it goes", stands, "");
            ctx.check("tank: its water runs out of its underside port into B below", b.vessel.mLiquid() > 0.05 * aWater0,
                String.format("%.3f of %.3f kg", b.vessel.mLiquid(), aWater0));
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
     * The inline pump on MVU: tank A's low port -> pipe -> pump -> pipe -> tank B's low port. With cold water in A it
     * moves it to B and draws power; with A full of water at its boiling point (1 atm, 100 C) it cavitates and moves
     * next to nothing.
     */
    static final class PumpMovesWater extends Big {
        final boolean boiling;
        TankElement a, b;
        InlinePumpElement p;
        IProcess watch;
        double bWater0, maxW;
        boolean sawCavitate, sawSteamBound;
        String steamJade = "", airJade = "";

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
                out.writeFloat((float) mods.eln.Eln.MVU);
            }));
            double T = boiling ? Water.TB : Water.T_AMBIENT;
            fill(a.vessel, 0.8 * a.vessel.volume * Water.RHO_L, Water.P_ATM, T);
            bWater0 = b.vessel.mLiquid();
            Eln.simulator.addSlowProcess(watch = dt -> {
                sawCavitate |= boiling ? p.cavitating : p.cavitating && !p.gasBound;
                maxW = Math.max(maxW, p.wantW);
                // (Jade: gas at its inlet that is its own vapour reads cavitating, air reads priming: AdventurAgent)
                if (p.gasBound && p.steamBound) {
                    sawSteamBound = true;
                    steamJade = p.getWaila().get("Speed");
                }
                if (p.gasBound && !p.steamBound) airJade = p.getWaila().get("Speed");
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            double moved = b.vessel.mLiquid() - bWater0;
            ctx.line("inline pump" + (boiling ? " (boiling feed)" : "") + ": " + p.getWaila() + "; A " + a.getWaila() + "; B " + b.getWaila());
            if (!boiling) {
                // (both tanks are sealed: A's air thins and B's compresses until the pump's 0.5 bar head and the fall
                // balance them; with gas able to come back through a pipe's opening, ~0.04 kg reaches B, the rest fills
                // the lines. One stream per edge let 0.2 kg over against 1.1 bar, more than the pump can make.)
                ctx.check("pump: cold water goes from A to B", moved > 0.025, String.format("%.3f kg", moved));
                // (more than its idling: the pump puts its real hydraulic power into the water, over its efficiency, a few W
                // against these low heads; the old law charged the full shut-off head on whatever moved)
                ctx.check("pump: it draws power doing it", maxW > mods.eln.oil.InlinePumpDescriptor.IDLE_W + 1, String.format("%.0f W", maxW));
                ctx.check("pump: it primed itself (passed the line's air on) and then pumped water", p.totalKg > 0.05, String.format("%.3f kg", p.totalKg));
                ctx.check("pump: Jade calls air at its inlet priming", airJade.contains("priming"), airJade);
            } else {
                ctx.check("pump: water at its boiling point vapour-locks it (cavitating, or its line full of steam)", sawCavitate, "");
                ctx.check("pump: and it moves next to nothing (under 0.05 kg)", moved < 0.05, String.format("%.3f kg", moved));
                ctx.check("pump: Jade calls steam at its inlet cavitating, not priming", !sawSteamBound || steamJade.contains("CAVITATING") && !steamJade.contains("priming"),
                    sawSteamBound + ": " + steamJade);
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

    /**
     * Baughn 2026-11-03: a lone tank showed all seven flanges, and a flange's nozzle stopped short of the curved shell (one
     * could look into it). A tank draws a flange only where a pipe meets the port: lone tank A with no pipe has none;
     * tanks B and C piped low port to low port have one each, on the facing sides, as their renders read it. And every
     * side nozzle, in any box up to 3 x 3 x 3, reaches inside the shell all round its bore (unless its port is so near
     * the box's corner that the bore passes beside the shell).
     */
    static final class TankFlanges extends Big {
        TankElement a, b, c;

        public String id() {
            return "tank_flanges_where_piped";
        }

        public void build(TestContext ctx) {
            a = tank(ctx, IRON_TANK, ctx.at(1, 1, 6), Direction.XN);
            b = tank(ctx, ALLOY_TANK, ctx.at(2, 1, 1), Direction.XN);   // (its back, +x, faces the pipe)
            c = tank(ctx, ALLOY_TANK, ctx.at(4, 1, 1), Direction.XP);   // (its back, -x, faces the pipe)
            PipeCases.lay(ctx, ctx.at(3, 1, 1), 1, 0, 4, 8, 16, 4, 8);
        }

        static mods.eln.oil.TankRender render(TankElement t) {
            var bos = new java.io.ByteArrayOutputStream();
            t.networkSerialize(new java.io.DataOutputStream(bos));
            var r = new mods.eln.oil.TankRender(null, t.getDescriptor());
            r.networkUnserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bos.toByteArray())));
            return r;
        }

        /** Whether (u, v) (px, box coordinates across the box's floor) is inside the shell of a box sx x sz (its flats' ellipse). */
        static boolean inShell(double u, double v, int sx, int sz) {
            double r = mods.eln.oil.TankRender.SHELL_R * Math.cos(Math.PI / 14);
            double du = (u - sx * 8.0) / (r * sx), dv = (v - sz * 8.0) / (r * sz);
            return du * du + dv * dv < 1;
        }

        public void measure(TestContext ctx) {
            var ra = render(a);
            var rb = render(b);
            var rc = render(c);
            ctx.check("tank flanges: a lone tank with no pipe draws none", a.coupled == 0 && ra.coupled == 0, Integer.toHexString(ra.coupled));
            ctx.check("tank flanges: B draws one, its east low port (the pipe's)", rb.coupled == 1 << 10, Integer.toHexString(rb.coupled));
            ctx.check("tank flanges: C draws one, its west low port (the pipe's)", rc.coupled == 1 << 8, Integer.toHexString(rc.coupled));
            ctx.check("tank flanges: the ports still work (kits end at them): seven on a lone tank", b.fluidPorts().size() == 8 && a.getWaila().containsKey(mods.eln.i18n.I18N.tr("Ports")),
                b.fluidPorts().size() + " ports");
            // every side nozzle of a box up to 3 x 3 x 3, on every outer face of its blocks: its bore's end inside the shell
            var r = rb;
            String bad = "";
            int checked = 0, turned = 0;
            for (int sx = 1; sx <= 3; sx++)
                for (int sz = 1; sz <= 3; sz++)
                    for (int ox = 0; ox < sx; ox++)
                        for (int oz = 0; oz < sz; oz++)
                            for (int face = 2; face <= 5; face++) {
                                boolean alongX = face >= 4, high = face == 3 || face == 5;
                                int o = alongX ? ox : oz, n = alongX ? sx : sz;
                                if (o != (high ? n - 1 : 0)) continue;   // (an outer face only)
                                r.sx = sx;
                                r.sz = sz;
                                r.ox = ox;
                                r.oz = oz;
                                int nAcross = alongX ? sz : sx, oAcross = alongX ? oz : ox;
                                double across = oAcross * 16 + 8, half = mods.eln.oil.TankRender.SHELL_R * Math.cos(Math.PI / 14) * nAcross;
                                if (Math.abs(across - nAcross * 8.0) + mods.eln.oil.TankRender.NOZZLE_R > 0.95 * half) {
                                    // beside the shell (Baughn 2026-11-09, a corner flange): it turns toward the shell, and its bore's end is in it
                                    float[] nz = r.sideNozzle(face);
                                    double th = Math.toRadians(nz[1]), sign = face == 2 || face == 5 ? -1 : 1;
                                    double du = high ? -Math.cos(th) : Math.cos(th), dv = sign * -Math.sin(th);
                                    double l = nz[0] - mods.eln.oil.TankRender.DISC, pu = (high ? (o + 1) * 16 - mods.eln.oil.TankRender.DISC : o * 16 + mods.eln.oil.TankRender.DISC);
                                    double eu = pu + du * l, ev = across + dv * l;
                                    boolean ok = nz[1] != 0 && l < 24;
                                    for (int k = -1; k <= 1; k += 2) {
                                        double wu = eu - k * dv * mods.eln.oil.TankRender.NOZZLE_R, wv = ev + k * du * mods.eln.oil.TankRender.NOZZLE_R;
                                        ok &= alongX ? inShell(wu, wv, sx, sz) : inShell(wv, wu, sx, sz);
                                    }
                                    if (!ok) bad += String.format(" %dx%d (%d,%d) face %d turned %.0f len %.1f;", sx, sz, ox, oz, face, nz[1], nz[0]);
                                    turned++;
                                    continue;
                                }
                                double faceAt = high ? (o + 1) * 16 : o * 16, end = high ? faceAt - r.nozzle(face) : faceAt + r.nozzle(face);
                                for (int k = 0; k < 24; k++) {
                                    double w = across + mods.eln.oil.TankRender.NOZZLE_R * Math.cos(2 * Math.PI * k / 24);
                                    boolean in = alongX ? inShell(end, w, sx, sz) : inShell(w, end, sx, sz);
                                    if (!in) {
                                        bad += String.format(" %dx%d (%d,%d) face %d;", sx, sz, ox, oz, face);
                                        break;
                                    }
                                }
                                checked++;
                            }
            ctx.check("tank flanges: every side nozzle reaches inside the shell all round its bore", bad.isEmpty() && checked > 20 && turned > 10, checked + " straight, " + turned + " turned;" + bad);
            r.sx = r.sz = 1;
            r.ox = r.oz = 0;
            ctx.check("tank flanges: a lone tank's top nozzle reaches below its flat top (15 px)", 16 - r.topNozzle() < 15, String.format("%.2f px", 16 - r.topNozzle()));
        }
    }

    /**
     * Baughn 2026-11-03: a viewglass. What a tank's render reads for its sight glass: the water's share and whether its
     * gas is steam. A hot tank half full at 10 bar shows half and steam; a cold one a third full shows a third and air; a
     * box of two stacked tanks shows its one level from its lowest block, which draws it the box's height (the other
     * block draws none).
     */
    static final class TankSightGlass extends Big {
        TankElement hot, cold, low, high;

        public String id() {
            return "tank_sight_glass";
        }

        public void build(TestContext ctx) {
            hot = tank(ctx, ALLOY_TANK, ctx.at(1, 1, 1), Direction.XN);
            cold = tank(ctx, IRON_TANK, ctx.at(3, 1, 1), Direction.XN);
            low = (TankElement) ctx.placeTransparent(IRON_TANK, ctx.at(5, 1, 1));
            high = (TankElement) ctx.placeTransparent(IRON_TANK, ctx.at(5, 2, 1));
            fill(hot.vessel, 0.5 * hot.vessel.volume * Water.RHO_L, 10e5, Water.tsat(10e5));
            fill(cold.vessel, cold.vessel.volume * Water.RHO_L / 3, Water.P_ATM, 293.15);
            if (low.group != null) fill(low.group.node, 0.75 * low.group.node.volume * Water.RHO_L, Water.P_ATM, 293.15);
        }

        public void measure(TestContext ctx) {
            var rh = TankFlanges.render(hot);
            var rc = TankFlanges.render(cold);
            var rl = TankFlanges.render(low);
            var ru = TankFlanges.render(high);
            ctx.check("sight glass: a hot tank half full shows half, and steam over it", Math.abs(rh.fill - 0.5) < 0.05 && rh.steam,
                String.format("%.3f, steam %b", rh.fill, rh.steam));
            ctx.check("sight glass: a cold tank a third full shows a third, and air over it", Math.abs(rc.fill - 1 / 3f) < 0.03 && !rc.steam,
                String.format("%.3f, steam %b", rc.fill, rc.steam));
            ctx.check("sight glass: a box of two shows its level once, from its lowest block, two blocks high",
                low.group != null && rl.draws && !ru.draws && rl.sy == 2 && Math.abs(rl.fill - 0.75) < 0.03,
                String.format("group %s, draws %b/%b, sy %d, %.3f", low.group != null, rl.draws, ru.draws, rl.sy, rl.fill));
        }
    }

    /** Breaking a block of a 2 x 2 x 3 box: what's left isn't a box, so 11 one-block tanks, each with its share by layer. */
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
            // (the liquid lies in the lower layers, so the top corner held only gas and vapour)
            ctx.check("tank box split: they keep the water (the broken top corner held none)", Math.abs(after - before) < 0.01 * before,
                String.format("%.3f of %.3f kg", after, before));
        }
    }

    /**
     * Baughn 2026-11-09: breaking a corner of a box tank set the oil in the rest oscillating. A 3 x 2 x 3 box 15 % full of
     * crude loses a bottom corner; after it has had 5 s to settle, a bottom tank's water stays put (within 1 % over the
     * last 5 s).
     */
    /**
     * A box's outline and collision are its shell's, cut to each block (Baughn 2026-11-09: a 2 x 2 x 3 box kept its lone
     * tanks' small cylinders): a block reaches the faces it shares with the rest of the box, the middle layer runs floor
     * to ceiling, the top stops at the shell's top, and the box's inside corner is solid to walk on.
     */
    static final class TankBoxHitbox extends Big {
        String bad = "", line = "";

        public String id() {
            return "tank_box_hitbox_from_its_shell";
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            box(ctx);
        }

        public void measure(TestContext ctx) {
            var level = ctx.level();
            for (int y = 1; y <= 3; y++) {
                var pos = ctx.at(2, y, 2);
                var st = level.getBlockState(pos);
                var outline = st.getShape(level, pos);
                var coll = st.getCollisionShape(level, pos);
                if (outline.isEmpty() || coll.isEmpty()) {
                    bad += "layer " + y + " empty; ";
                    continue;
                }
                var b = outline.bounds();
                line += String.format("layer %d: x %.2f..%.2f y %.2f..%.2f z %.2f..%.2f (%d boxes); ", y, b.minX, b.maxX, b.minY, b.maxY, b.minZ, b.maxZ, outline.toAabbs().size());
                if (b.maxX < 0.999 || b.maxZ < 0.999) bad += "layer " + y + " short of the box's inside faces; ";
                if (b.minX < 0.01 || b.minZ < 0.01) bad += "layer " + y + " out to the box's outer faces; ";
                if (y < 3 && b.maxY < 0.999) bad += "layer " + y + " short of the next layer; ";
                if (y > 1 && b.minY > 0.001) bad += "layer " + y + " short of the layer below; ";
                if (y == 3 && (b.maxY > 0.99 || b.maxY < 0.75)) bad += "top layer's top at " + b.maxY + "; ";
                boolean corner = false;
                for (var a : coll.toAabbs()) corner |= a.contains(0.95, y == 3 ? 0.5 : 0.95, 0.95);
                if (!corner) bad += "layer " + y + " collision misses the inside corner; ";
            }
            ctx.line("tank box hitbox: " + line);
            ctx.check("a tank box's hitbox is its shell, cut to each block", bad.isEmpty(), bad);
        }
    }

    /**
     * A click on a box's shell at a corner block (Baughn 2026-11-09; AdventurAgent's pipes didn't seat): the shell's steps
     * face any way, so a click on one whose face has no port (the box's inside) takes the nearest side port; a click on
     * the block's own face with no port takes none, as before.
     */
    static final class TankBoxCornerPipeClick extends Big {
        java.util.List<BlockPos> at = new java.util.ArrayList<>();

        public String id() {
            return "tank_box_corner_click_finds_its_port";
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            for (int y = 1; y <= 2; y++)
                for (int x = 2; x <= 3; x++)
                    for (int z = 2; z <= 3; z++) ctx.placeTransparent(IRON_TANK, ctx.at(x, y, z));
        }

        public void measure(TestContext ctx) {
            var pos = ctx.at(2, 1, 2);   // (the box's low corner: its outer faces are west and north)
            var level = ctx.level();
            var hit = new net.minecraft.world.phys.Vec3(pos.getX() + 0.7, pos.getY() + 0.3, pos.getZ() + 0.75);
            var e = mods.eln.node.voxel.PipePlacer.portAtForTests(level, pos, net.minecraft.core.Direction.EAST, hit);
            ctx.check("a click inside a corner block on an inner-facing step takes its nearest side port (west, low)",
                e != null && e.dir() == 0 && e.x() == pos.getX() * 16L && e.y() == pos.getY() * 16L + 4, String.valueOf(e));
            var hit2 = new net.minecraft.world.phys.Vec3(pos.getX() + 0.3, pos.getY() + 0.8, pos.getZ() + 0.6);
            var e2 = mods.eln.node.voxel.PipePlacer.portAtForTests(level, pos, net.minecraft.core.Direction.SOUTH, hit2);
            ctx.check("... and high up, its west high port", e2 != null && e2.dir() == 0 && e2.y() == pos.getY() * 16L + 12, String.valueOf(e2));
            var onFace = new net.minecraft.world.phys.Vec3(pos.getX() + 1, pos.getY() + 0.3, pos.getZ() + 0.5);
            ctx.check("a click on a block's own face with no port takes none",
                mods.eln.node.voxel.PipePlacer.portAtForTests(level, pos, net.minecraft.core.Direction.EAST, onFace) == null, "");
        }
    }

    static final class TankBoxSettlesAfterBreak extends Big {
        java.util.List<TankElement> ts;
        IProcess watch;
        int tick;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        final java.util.List<Double> samples = new java.util.ArrayList<>();
        String trace = "";

        public String id() {
            return "tank_box_settles_after_break";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            // (Baughn's: 3 x 2 x 3 iron tanks, x 2..4, y 1..2, z 2..4)
            ts = new java.util.ArrayList<>();
            for (int y = 1; y <= 2; y++)
                for (int x = 2; x <= 4; x++)
                    for (int z = 2; z <= 4; z++) ts.add((TankElement) ctx.placeTransparent(IRON_TANK, ctx.at(x, y, z)));
            var g = ts.get(0).group;
            // (crude, as Baughn's: 15 % of the box)
            int crude = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:crude_oil");
            if (g != null) {
                fill(g.node, 0, Water.P_ATM, 293.15);
                mods.eln.sim.fluid.EaUnits.add(g.node, crude, 0.15 * g.node.volume * 850, 293.15, false);
            }
            TankElement gone = ts.remove(0);   // (a bottom corner)
            ctx.level().removeBlock(gone.node.coordonate.pos(), false);
            TankElement watched = ts.get(1);
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                double m = watched.vessel.liquidVolume();
                if (tick % 40 == 0) trace += String.format(" %.7f", m);
                samples.add(m);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            for (int i = samples.size() / 2; i < samples.size(); i++) {   // (the last half: 5 s)
                lo = Math.min(lo, samples.get(i));
                hi = Math.max(hi, samples.get(i));
            }
            ctx.line("tank box after a break: a bottom tank's crude (m3), 10 samples:" + trace);
            ctx.check("tank box after a break: the crude settles (within 1 % over the last 5 s)", hi - lo <= 0.01 * Math.max(hi, 1e-9),
                String.format("%.7f .. %.7f m3", lo, hi));
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

    /**
     * A physics bug: water was soft (bulk modulus 2e7 Pa): a liquid-full vessel took 1.4 x its volume at ~95 bar. At water's
     * own ~2 GPa a full tank pushed by a 50 bar reservoir holds under 1 % more than it should.
     */
    static final class LiquidStaysIncompressible extends Big {
        TankElement t;
        mods.eln.sim.fluid.FluidDevice fx;

        public String id() {
            return "liquid_full_tank_incompressible";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(4, 1, 4);
            t = tank(ctx, ALLOY_TANK, p, Direction.XN);
            fill(t.vessel, 0.97 * t.vessel.volume * Water.RHO_L, Water.P_ATM, 293.15);
            fx = vessels(t.fluidLevel(), new Object[]{p.above(), 0, reservoir(p.getY() + 1, 50e5)});   // (on its top port)
        }

        public void measure(TestContext ctx) {
            mods.eln.fluid.FluidWorld.remove(fx);
            double full = t.vessel.mWater / (t.vessel.volume * Water.RHO_L);
            ctx.line(String.format("liquid-full tank on a 50 bar reservoir: %.4f of its volume, %.1f bar", full, t.vessel.P() / 1e5));
            ctx.check("liquid-full: under 1 % over its volume", full < 1.01, String.format("%.4f", full));
            ctx.check("liquid-full: at about the reservoir's pressure (40-60 bar)", t.vessel.P() > 40e5 && t.vessel.P() < 60e5, String.format("%.1f bar", t.vessel.P() / 1e5));
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
            dry.energy = dry.energyAt(T);
            dry.update();
            wet.mWater = 1.5 * mv;   // (plus half as much again as liquid: a third of the water)
            wet.energy = dry.energyAt(T) + 0.5 * mv * Water.uLiquid(T);
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
