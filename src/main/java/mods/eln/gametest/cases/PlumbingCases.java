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
                sawCavitate |= p.cavitating && !p.gasBound;
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
                ctx.check("pump: water at its boiling point makes it cavitate", sawCavitate, "");
                ctx.check("pump: and it moves next to nothing", moved < 0.02, String.format("%.3f kg", moved));
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
                ctx.check("relief valve: A let down to about 9 bar (8.6-9.6) and held there", pa > 8.6 && pa < 9.6, String.format("%.2f bar", pa));
                ctx.check("relief valve: B took what A let go, and no more (1.2 bar to under A's)", pb > 1.2 && pb < pa - 1, String.format("%.2f bar", pb));
            }
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
