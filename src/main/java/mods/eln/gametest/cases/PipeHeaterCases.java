package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.oil.PipeHeaterElement;
import mods.eln.oil.TankElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.fluid.SpeciesRegistry;
import net.minecraft.core.BlockPos;

import java.util.List;

import static mods.eln.gametest.cases.Wp11Cases.SOURCE;

/**
 * The Pipe Heater (Baughn 2026-11-26, todo #623): an electric heater in a line, for oil too viscous to flow cold.
 */
public final class PipeHeaterCases {
    private PipeHeaterCases() {
    }

    static final int HEATER = 24 + (84 << 6);
    static final double COLD = 293.15;

    public static void addTo(List<TestCase> cases) {
        cases.add(new ThinsHeavyOil());
        cases.add(new HoldsItsSetPoint());
        cases.add(new BurnsDry());
    }

    /**
     * One line along z at x: tank A (9 B of heavy oil at 20 C under air at 8 bar, unless empty) -> a block of pipe ->
     * heater -> three blocks of pipe -> tank B (air at 1 atm), all low (the heater's line is the inline pump's), and the heater powered from the west if
     * powered.
     */
    static final class Line {
        TankElement a, b;
        PipeHeaterElement h;
        final int oil = SpeciesRegistry.get().id("eln:heavy_oil");

        Line(TestContext ctx, int x, boolean powered, boolean empty) {
            BlockPos pa = ctx.at(x, 1, 1), ph = ctx.at(x, 1, 3), pb = ctx.at(x, 1, 7);
            a = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, pa, Direction.XN);
            h = (PipeHeaterElement) ctx.placeTransparent(HEATER, ph);
            h.front = Direction.XN;   // (its flanks along z: the line)
            h.reconnect();
            b = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, pb, Direction.XN);
            PipeCases.lay(ctx, pa.south(), 0, 8, 4, 0, 8, 4, 16);
            for (int k = 1; k <= 3; k++) PipeCases.lay(ctx, ph.south(k), 0, 8, 4, 0, 8, 4, 16);
            if (powered) {
                BlockPos cable = ph.west(), src = cable.west();
                ctx.placeSix(Ids.six("Medium Voltage Cable"), cable);
                var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(SOURCE, src);
                ctx.setVolts(s, Eln.MVU);
            }
            if (!empty) ctx.after(2, () -> {   // (9 B of heavy oil under air, ~8 bar once it is in)
                var v = a.vessel;
                v.fill(0, 2.0e5, COLD);
                mods.eln.sim.fluid.EaUnits.add(v, oil, 9000 * mods.eln.fluid.FluidSpecies.kgPerMb(oil, false), COLD, false);
                v.update();
                filled = String.format("A: %.4f m3, %.2f kg heavy oil, %.2f bar", v.volume, v.mass(oil), v.P() / 1e5);
            });
        }

        String filled = "";

        double moved() {
            return b.vessel.mass(oil);
        }

        /** Its contents' temperature (C). */
        double oilC() {
            return mods.eln.fluid.FluidWorld.nodeFor(h.fluid).T() - 273.15;
        }
    }

    abstract static class Base implements TestCase {
        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;   // (an existing floor structure)
        }

        public int floorDepth() {
            return 10;
        }
    }

    /**
     * Two lines side by side, the same heavy oil pushed by the same 8 bar: one heater powered, one not. The heated line's
     * oil (viscosity 0.2 Pa s at 20 C, 0.015 at 100 C) flows ~1.6x as fast, out of the same cold
     * tank (option A'); the heater draws its 4 kW, the cold one none.
     */
    static final class ThinsHeavyOil extends Base {
        Line hot, cold;
        IProcess watch;
        double maxW, maxC;
        int tick;
        double hotAt500 = -1, coldAt500 = -1;
        final StringBuilder series = new StringBuilder();

        public String id() {
            return "pipe_heater_thins_heavy_oil";
        }

        public int ticks() {
            return 700;
        }

        public void build(TestContext ctx) {
            hot = new Line(ctx, 3, true, false);
            cold = new Line(ctx, 7, false, false);
            Eln.simulator.addSlowProcess(watch = dt -> {
                maxW = Math.max(maxW, power(hot.h));
                maxC = Math.max(maxC, hot.oilC());
                if (tick == 499) {
                    hotAt500 = hot.moved();
                    coldAt500 = cold.moved();
                }
                if (++tick % 100 == 0) series.append(String.format(" t%d %.3f/%.3f kg %.0fC", tick, hot.moved(), cold.moved(), hot.oilC()));
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            // (by 500 ticks, while both still flow: the hot line's tank equalises with its receiver at ~600)
            double mh = hotAt500, mc = coldAt500;
            ctx.line(String.format("pipe heater: by 500 ticks the heated line moved %.3f kg, the cold one %.3f kg; hottest %.0f C; heater %s; cold %s", mh, mc, maxC,
                hot.h.getWaila(), cold.h.getWaila()) + "; " + hot.filled + "; A " + hot.a.getWaila() + "; B " + hot.b.getWaila() + ";" + series);
            // (the oil it heats thins the run after it, and the edges into and out of it go partly at its own viscosity:
            // FluidEdge.shareA, option A')
            ctx.check("pipe heater: by 500 ticks the heated line has moved far more (~1.6x)", mh > 1.4 * mc && mc > 0.03,
                String.format("%.3f kg against %.3f kg", mh, mc));
            ctx.check("pipe heater: it stays whole with oil flowing", !hot.h.isBurnt(), "" + hot.h.getBurnt());
            ctx.check("pipe heater: it draws its 4 kW", maxW > 3500 && maxW < 4500, String.format("%.0f W", maxW));
            ctx.check("pipe heater: its oil got hot", maxC > 60, String.format("%.0f C", maxC));
            ctx.check("pipe heater: the unpowered one draws nothing", power(cold.h) < 1, String.format("%.1f W", power(cold.h)));
            ctx.check("pipe heater: Jade shows its contents and their viscosity", hot.h.getWaila().containsKey(mods.eln.i18n.I18N.tr("Contents"))
                && hot.h.getWaila().containsKey(mods.eln.i18n.I18N.tr("Viscosity")), "" + hot.h.getWaila());
        }
    }

    static double power(PipeHeaterElement h) {
        return h.power();
    }

    /**
     * The heated line with an Analogic Regulator in its slot set to 60 C: the oil settles near 60 C (the regulator holds
     * the oil's temperature, not its coils') and the heater throttles below its full power.
     */
    static final class HoldsItsSetPoint extends Base {
        Line line;
        IProcess watch;
        double lastW, minLateW = Double.MAX_VALUE, maxLateC, minLateC = Double.MAX_VALUE;
        int tick;
        boolean items;

        public String id() {
            return "pipe_heater_holds_its_set_point";
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            line = new Line(ctx, 3, true, false);
            var reg = Eln.findItemStack("Analogic Regulator");
            items = !reg.isEmpty();
            if (!items) return;
            line.h.getInventory().setItem(PipeHeaterElement.REGULATOR_SLOT, reg);
            line.h.inventoryChange(line.h.getInventory());   // (as a player's click does)
            line.h.setTarget(60 - PhysicalConstant.Tamb);
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick < 800) return;
                maxLateC = Math.max(maxLateC, line.oilC());
                minLateC = Math.min(minLateC, line.oilC());
                minLateW = Math.min(minLateW, power(line.h));
            });
        }

        public void measure(TestContext ctx) {
            if (watch != null) Eln.simulator.removeSlowProcess(watch);
            ctx.check("pipe heater regulator: the Analogic Regulator item exists", items, "");
            if (!items) return;
            ctx.line(String.format("pipe heater regulator: oil %.1f..%.1f C late on, %.0f W at the least; %s", minLateC, maxLateC, minLateW, line.h.getWaila()));
            // (above 60: it holds the oil, not its coils, which run hotter; holding its coils at 60 the oil stayed under)
            ctx.check("pipe heater regulator: the oil holds near its 60 C set point", minLateC > 50 && maxLateC > 60.5 && maxLateC < 70,
                String.format("%.1f..%.1f C", minLateC, maxLateC));
            ctx.check("pipe heater regulator: it throttles below full power", minLateW < 3800, String.format("%.0f W", minLateW));
        }
    }

    /**
     * A powered heater with no regulator in an empty line (air only), its coils already at 830 C: nothing carries their
     * heat away, so they stay past 800 C until its watchdog's budget (~800 K s) runs out and it burns, as an Electric Heater with nothing to heat.
     */
    static final class BurnsDry extends Base {
        Line line;

        public String id() {
            return "pipe_heater_burns_dry";
        }

        public int ticks() {
            return 1000;
        }

        public void build(TestContext ctx) {
            line = new Line(ctx, 3, true, true);
            line.h.thermal.Tc = 830 - PhysicalConstant.Tamb;
        }

        public void measure(TestContext ctx) {
            ctx.check("pipe heater: dry, its coils overheat and it burns out", line.h.isBurnt(),
                String.format("coils %.0f C, burnt %s", line.h.thermal.Tc + PhysicalConstant.Tamb, line.h.getBurnt()));
        }
    }
}
