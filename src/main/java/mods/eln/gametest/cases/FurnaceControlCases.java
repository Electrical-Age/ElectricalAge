package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.CompactTurbineElement;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceContainer;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceElement;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * The Stone Heat Furnace's air valve and hot bed, and its regulators (Baughn 1573753560456499223; AdventurAgent's
 * handbook replay saw two furnaces with On/Off regulators at 250 C burn out once their boiler went dry). Before, its fire
 * followed the regulator over 180 s either way, and its ~13 J/K heated in 10 s: with nothing taking its heat, every
 * regulator let a 4-chamber furnace reach 1100-1150 C.
 * <ul>
 *   <li>no_sink: at 250 C, its fire full and its bed hot, a regulator set to 250 C, nothing taking its heat: it never
 *   burns out; the Analogic Regulator settles within 10 K of its set point;</li>
 *   <li>cold: a cold furnace, lit, the Analogic Regulator set to 250 C: the bed takes minutes to come up, it doesn't
 *   overshoot by more than 10 K and ends within 5 K;</li>
 *   <li>turbine_dry: against a wet compact turbine, set to 300 C (more than the turbine lets it reach), then the turbine
 *   runs dry: it holds its set point instead of burning out (on/off within its band; analogue at most 20 K over, as
 *   the dry turbine's heavy boiler heats with it, then within 5 K).</li>
 * </ul>
 */
public final class FurnaceControlCases {
    private FurnaceControlCases() {
    }

    /** Regulator sub-ids (Wp12aContent: 0 On/Off 1 %, 1 On/Off 10 %, 8 Analogic). */
    static final int ONOFF1 = 0, ONOFF10 = 1, ANALOG = 8;

    public static void addTo(List<TestCase> cases) {
        for (int r : new int[]{ONOFF1, ONOFF10, ANALOG}) cases.add(new Run("no_sink", r));
        cases.add(new Run("cold", ANALOG));
        for (int r : new int[]{ONOFF10, ANALOG}) cases.add(new Run("turbine_dry", r));
    }

    /** A 4-chamber furnace with coal and a regulator (sub-id) set to setC (degrees C). */
    static HeatFurnaceElement furnace(TestContext c, BlockPos p, mods.eln.misc.Direction front, int regulator, float setC) {
        var f = (HeatFurnaceElement) Wp10bCases.placeFacing(c, Ids.transparent("Stone Heat Furnace"), p, front);
        var inv = f.getInventory();
        inv.setItem(HeatFurnaceContainer.combustrionChamberId, mods.eln.Eln.findItemStack("Combustion Chamber", 4));
        inv.setItem(HeatFurnaceContainer.regulatorId, mods.eln.Eln.sharedItem.getDescriptor(regulator + (3 << 6)).newItemStack());
        inv.setItem(HeatFurnaceContainer.combustibleId, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 64));
        f.inventoryChange(inv);
        f.networkUnserialize(c.stream(o -> {
            o.writeByte(HeatFurnaceElement.unserializeTemperatureTarget);
            o.writeFloat(setC - 20);   // (Tc is above the 20 C ambient)
        }));
        f.furnaceProcess.combustibleEnergy = f.furnaceProcess.nominalCombustibleEnergy;
        return f;
    }

    static final class Run implements TestCase {
        final String kind;
        final int regulator;
        HeatFurnaceElement furnace;
        CompactTurbineElement turbine;
        mods.eln.sim.IProcess watch;
        double t, peakC, peakAfterDry = -1e9, setC, minLast30 = 1e9, maxLast30 = -1e9;
        boolean dried;

        Run(String kind, int regulator) {
            this.kind = kind;
            this.regulator = regulator;
        }

        public String id() {
            return "stone_furnace_" + kind + "_" + regulator;
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            // (30 minutes: long enough that a fire held low would have gone out and been lit again, had it)
            return 20 * 1800;
        }

        public void build(TestContext c) {
            setC = kind.equals("turbine_dry") ? 300 : 250;
            if (kind.equals("turbine_dry")) {
                turbine = (CompactTurbineElement) Wp10bCases.placeFacing(c, Ids.transparent("48V Compact Steam Turbine"), c.at(2, 1, 1),
                    mods.eln.misc.Direction.ZN);
                var pad = turbine.front.left();   // (its heat pad: on the left as you face it)
                BlockPos fp = c.at(2, 1, 1).relative(pad.toEnumFacing());
                furnace = furnace(c, fp, pad, regulator, (float) setC);   // (its back on the pad)
                turbine.water = 4000;
                furnace.thermalLoad.Tc = 130;
                turbine.boiler.Tc = 130;
            } else {
                furnace = furnace(c, c.at(1, 1, 1), mods.eln.misc.Direction.XN, regulator, (float) setC);
                if (kind.equals("no_sink")) {
                    furnace.thermalLoad.Tc = 230;
                    furnace.furnaceProcess.fire.set(1);
                }
            }
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                t += dt;
                double cNow = furnace.thermalLoad.Tc + 20;
                peakC = Math.max(peakC, cNow);
                if (turbine != null && !dried && t >= 300) {
                    turbine.water = 0;   // (it runs dry)
                    dried = true;
                }
                if (dried) peakAfterDry = Math.max(peakAfterDry, cNow);
                if (t >= ticks() / 20.0 - 30) {
                    minLast30 = Math.min(minLast30, cNow);
                    maxLast30 = Math.max(maxLast30, cNow);
                }
            });
        }

        public void measure(TestContext c) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            var fire = furnace.furnaceProcess.fire;
            c.line(String.format("%s: peak %.0f C, after dry %.0f C, last 30 s %.0f-%.0f C, set %.0f C; bed %.2f air %.2f; turbine %s; %s", id(), peakC,
                peakAfterDry, minLast30, maxLast30, setC, fire.bed, fire.air, turbine == null ? "-" : turbine.getWaila(), furnace.getWaila()) + " " + furnace.regulator.pidState());
            c.check(id() + ": not burnt out", !furnace.isBurnt(), String.format("peak %.0f C", peakC));
            // (on/off: half its hysteresis of 500 K, plus what ~600 J/K gains at full fire while the air shuts, ~5 s)
            double band = regulator == ONOFF10 ? 25 + 25 : regulator == ONOFF1 ? 2.5 + 25 : 10;
            switch (kind) {
                case "no_sink" -> {
                    c.check(id() + ": it stays near its set point", maxLast30 < setC + band && minLast30 > setC - band,
                        String.format("%.0f-%.0f C", minLast30, maxLast30));
                    // (fitted to a fire burning full at its set point: the air shuts within seconds)
                    c.check(id() + ": it doesn't run far over", peakC <= setC + (regulator == ANALOG ? 15 : band + 15), String.format("peak %.0f C", peakC));
                    c.check(id() + ": its fire stays alight (held low, it doesn't go out and wait to be lit)", fire.level > 0 && !fire.lighting(),
                        String.format("level %.2f", fire.level));
                }
                case "cold" -> {
                    c.check(id() + ": no more than 10 K over its set point", peakC <= setC + 10, String.format("peak %.0f C", peakC));
                    c.check(id() + ": within 5 K at the end", minLast30 >= setC - 5 && maxLast30 <= setC + 5, String.format("%.0f-%.0f C", minLast30, maxLast30));
                }
                case "turbine_dry" -> {
                    // (the analogue one: the dry turbine's boiler is heavy, and heats with it: a little over, then it settles)
                    c.check(id() + ": dry, it stays near its set point", peakAfterDry <= setC + (regulator == ANALOG ? 20 : band),
                        String.format("peak after dry %.0f C", peakAfterDry));
                    if (regulator == ANALOG) c.check(id() + ": and settles within 5 K", minLast30 >= setC - 5 && maxLast30 <= setC + 5,
                        String.format("%.0f-%.0f C", minLast30, maxLast30));
                }
            }
        }
    }
}
