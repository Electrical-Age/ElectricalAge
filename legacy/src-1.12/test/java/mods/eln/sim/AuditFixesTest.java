package mods.eln.sim;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sim.nbt.NbtResistor;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** WP16 sim audit: regression tests for the 1.7.10 bugs fixed outside the four originally @Ignore'd tests. */
public class AuditFixesTest {
    SimHarness h = new SimHarness();

    /** ThermalLoad.externalLoad is ambient (an infinite reservoir): a ThermalResistor to it is a plain loss. */
    @Test
    public void externalLoadIsAnInfiniteReservoir() {
        ThermalLoad t = new ThermalLoad(0, 1e30, 1, 10);
        h.sim.addThermalLoad(t);
        ThermalResistor r = new ThermalResistor(t, ThermalLoad.externalLoad);
        r.setR(2);
        h.sim.addThermalFastProcess(r);
        h.sim.addThermalFastProcess(ThermalTest.heater(t, 50));
        h.seconds(200); // tau = 20 s
        assertEquals(100, t.Tc, 1e-2);           // P * R (10 tau)
        assertEquals(0, ThermalLoad.externalLoad.Tc, 0);
        assertTrue(!Double.isNaN(ThermalLoad.externalLoad.getPower()));

        // even on a load list it stays at ambient (1.7.10: Tc/Rp = 0/0 = NaN)
        h.sim.addThermalLoad(ThermalLoad.externalLoad);
        try {
            h.ticks(20);
            assertEquals(0, ThermalLoad.externalLoad.Tc, 0);
            assertEquals(100, t.Tc, 1e-2);
        } finally {
            h.sim.removeThermalLoad(ThermalLoad.externalLoad);
            ThermalLoad.externalLoad.PcTemp = 0;
        }
    }

    /** NbtResistor: name-qualified key (two resistors under one prefix don't collide), no name growth. */
    @Test
    public void nbtResistorRoundTrip() {
        NbtResistor a = new NbtResistor("a", null, null), b = new NbtResistor("b", null, null);
        a.setR(3);
        b.setR(7);
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < 3; i++) { // repeated saves must write the same keys
            a.writeToNBT(nbt, "dev");
            b.writeToNBT(nbt, "dev");
        }
        NbtResistor a2 = new NbtResistor("a", null, null), b2 = new NbtResistor("b", null, null);
        a2.readFromNBT(nbt, "dev");
        b2.readFromNBT(nbt, "dev");
        assertEquals(3, a2.getR(), 0);
        assertEquals(7, b2.getR(), 0);
        NbtResistor missing = new NbtResistor("c", null, null);
        missing.readFromNBT(nbt, "dev"); // key absent (world saved before the fix): no R = 0 (infinite conductance)
        assertTrue(missing.getR() > 1e8);
    }

    /**
     * A component left straddling two SubSystems (here: a private/non-private boundary) that is not a resistor
     * used to ClassCastException in RootSystem.generateInterSystems; now it is skipped (logged). A resistor
     * across the same boundary is split into an InterSystem and still obeys Ohm's law.
     */
    @Test
    public void componentsAcrossAPrivateBoundary() {
        RootSystem root = new RootSystem(0.05, 50);
        VoltageState pub = new VoltageState(), priv = new VoltageState();
        priv.setAsPrivate();
        root.addState(pub);
        root.addState(priv);
        root.addComponent(new VoltageSource("v", pub, null).setU(10));
        Resistor across = new Resistor(pub, priv).setR(10);
        root.addComponent(across);
        root.addComponent(new Resistor(priv, null).setR(30));
        Capacitor c = new Capacitor(pub, priv);
        c.setC(1);
        root.addComponent(c);
        for (int i = 0; i < 10; i++) root.step();
        assertEquals(7.5, priv.state, 1e-6);
        assertEquals(10, pub.state, 1e-9);
    }

    /**
     * Slow thermal loads step once per tick (0.05 s, explicit Euler) although Simulator.checkThermalLoad is
     * calibrated for the fast period. With EA's cable constants (heat time 30 s, conduction tau 0.5 s) a junction
     * with 4 neighbours has dt * lambda_max ~ 2 dt (d / tau) <= 0.8 < 1: stable and non-oscillating.
     */
    @Test
    public void slowCableJunctionIsStable() {
        double pMax = 100, warm = 130, heatTime = 30, tao = 0.5;
        double c = pMax * heatTime / warm, rp = warm / pMax, rs = tao / c / 2;
        ThermalLoad hub = new ThermalLoad(100, rp, rs, c);
        hub.setAsSlow();
        h.sim.addThermalLoad(hub);
        ThermalLoad[] arm = new ThermalLoad[4];
        for (int i = 0; i < 4; i++) {
            arm[i] = new ThermalLoad(0, rp, rs, c);
            arm[i].setAsSlow();
            h.sim.addThermalLoad(arm[i]);
            h.sim.addThermalConnection(new ThermalConnection(hub, arm[i]));
        }
        double last = hub.Tc;
        for (int t = 0; t < 200; t++) {
            h.tick();
            assertTrue("tick " + t, hub.Tc <= last + 1e-12 && hub.Tc >= 0);
            for (ThermalLoad a : arm) assertTrue(a.Tc >= 0 && a.Tc <= 100);
            last = hub.Tc;
        }
    }
}
