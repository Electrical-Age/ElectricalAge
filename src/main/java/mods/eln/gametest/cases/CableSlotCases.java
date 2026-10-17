package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.electricalsensor.ElectricalSensorElement;
import mods.eln.sixnode.resistor.ResistorElement;
import mods.eln.sixnode.thermalsensor.ThermalSensorElement;
import net.minecraft.world.Container;

import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.f;
import static mods.eln.gametest.cases.Wp9aCases.orient;

/**
 * Devices that needed "a piece of cable in the GUI slot" before they worked (Baughn 2026-10-17:
 * notes/port26/cable-slots.md) work with an empty inventory. Power socket, energy meter, voltage probe, lamp sockets
 * and the lamp supply are covered by their own cases (wp9a, wp9c, wp9b probes, wp10a, lamp supply, lamp light), which
 * no longer put a cable in. Here: the electrical probe measuring current in series, the thermal probe between two
 * thermal cables and the temperature probe on an LV cable, each with nothing in its inventory.
 */
public final class CableSlotCases {
    private CableSlotCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ElectricalProbe());
        cases.add(new ThermalProbes());
    }

    static boolean empty(SixNodeElement e) {
        Container c = e.getInventory();
        if (c == null) return true;
        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) return false;
        return true;
    }

    /**
     * Source 50 V | Electrical Probe (current, 0..2 A) | power resistor (48 coal) | ground, in a row. The probe's two
     * pins connect with no cable; its series resistor is 2 x the LV cable Rs (1.12: 2 x Rs of the cable in the slot);
     * it carries the resistor's current and its output is I / 2 A * SVU.
     */
    static final class ElectricalProbe implements TestCase {
        ElectricalSensorElement probe;
        ResistorElement r;

        public String name() {
            return "cable slots electrical probe";
        }

        public void build(TestContext ctx) {
            Wp9aCases.source(ctx, Wp9aCases.SOURCE, 0, 1, 50);
            probe = (ElectricalSensorElement) ctx.placeSix(Ids.six("Electrical Probe"), ctx.at(1, 1, 1));
            orient(probe, LRDU::left, Direction.XN);
            probe.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalSensorElement.setTypeOfSensorId);
                o.writeByte(1); // currantType
            }));
            probe.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalSensorElement.setValueId);
                o.writeFloat(0f);
                o.writeFloat(2f);
            }));
            r = Wp9aCases.resistor(ctx, Wp9aCases.POWER_RESISTOR, 2, 1, 48);
            ctx.placeSix(Wp9aCases.GROUND, ctx.at(3, 1, 1));
        }

        public void measure(TestContext ctx) {
            ctx.check("electrical probe: inventory empty", empty(probe), "");
            ctx.check("electrical probe: both pins connect without a cable",
                probe.getConnectionMask(probe.front.left()) != 0 && probe.getConnectionMask(probe.front.right()) != 0, "");
            ctx.checkValue("electrical probe: series resistor = 2 x LV cable Rs [ohm]", probe.resistor.getR(),
                2 * ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs);
            double i = Batch1Cases.current(r);
            ctx.check("electrical probe: current flows (50 V through the resistor)", i > 0.1, f("I = %.4f A", i));
            ctx.checkValue("electrical probe: its current [A] = the resistor's", Math.abs(probe.resistor.getCurrent()), i);
            ctx.checkValue("electrical probe: output [V] = I / 2 A * SVU", probe.outputGate.getU(), Math.min(1, i / 2) * Eln.SVU);
        }
    }

    /**
     * Thermal Probe between two Copper Thermal Cables (x 0..2) and a Temperature Probe on an LV cable (x 4..5), all
     * with empty inventories. The cables start at 100 C; after the run heat has reached each probe through its
     * built-in cable (thermal probe: Copper Thermal Cable; temperature probe, empty slot: an LV cable's thermal load).
     */
    static final class ThermalProbes implements TestCase {
        ThermalSensorElement thermal, temp;
        SixNodeElement hot, cold, lv;

        public String name() {
            return "cable slots thermal probes";
        }

        public void build(TestContext ctx) {
            hot = ctx.placeSix(Wp9bCases.THERMAL_CABLE, ctx.at(0, 1, 1));
            thermal = (ThermalSensorElement) ctx.placeSix(Ids.six("Thermal Probe"), ctx.at(1, 1, 1));
            orient(thermal, LRDU::left, Direction.XN);
            cold = ctx.placeSix(Wp9bCases.THERMAL_CABLE, ctx.at(2, 1, 1));
            lv = ctx.placeSix(Wp9bCases.LV_CABLE, ctx.at(4, 1, 1));
            temp = (ThermalSensorElement) ctx.placeSix(Wp9bCases.TEMPERATURE_PROBE, ctx.at(5, 1, 1));
            orient(temp, LRDU::inverse, Direction.XN);
            hot.getThermalLoad(LRDU.Up).Tc = 100;
            lv.getThermalLoad(LRDU.Up).Tc = 100;
        }

        public void measure(TestContext ctx) {
            ctx.check("thermal probes: inventories empty", empty(thermal) && empty(temp), "");
            ThermalLoad c = cold.getThermalLoad(LRDU.Up);
            ThermalLoad copper = new ThermalLoad();
            ElnDeviceRegistry.copperThermalCableDescriptor.setThermalLoad(copper);
            ctx.check("thermal probe: Copper Thermal Cable built in", thermal.thermalLoad.C == copper.C && thermal.thermalLoad.Rs == copper.Rs,
                f("C %.3g, Rs %.3g (copper %.3g, %.3g)", thermal.thermalLoad.C, thermal.thermalLoad.Rs, copper.C, copper.Rs));
            ctx.check("thermal probe: heat passes through it (hot cable -> probe -> cold cable)",
                thermal.thermalLoad.Tc > 0.01 && c.Tc > 0.001, f("probe %.4f C, far cable %.4f C", thermal.thermalLoad.Tc, c.Tc));
            ctx.check("temperature probe: empty slot joins the LV cable", temp.getConnectionMask(temp.front.inverse()) != 0
                && temp.thermalLoad.isSlow(), "mask " + temp.getConnectionMask(temp.front.inverse()));
            ctx.check("temperature probe: reads the LV cable's heat", temp.thermalLoad.Tc > 0.01,
                f("probe %.4f C, cable %.4f C", temp.thermalLoad.Tc, lv.getThermalLoad(LRDU.Up).Tc));
        }
    }
}
