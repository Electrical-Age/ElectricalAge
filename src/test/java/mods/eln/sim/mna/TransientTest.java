package mods.eln.sim.mna;

import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Capacitors and inductors. EA integrates them with BACKWARD (implicit) EULER at the electrical period:
 * Capacitor stamps C/dt into A and injects (Va-Vb)*C/dt from the previous step; Inductor adds a current
 * unknown with row Va - Vb - (L/dt) I = -(L/dt) I_prev. So each test checks two things:
 * the exact backward-Euler recurrence (tight), and the continuous physics exp(-t/tau) (loose, error O(dt/tau)).
 */
public class TransientTest {
    static final double DT = 0.05;

    RootSystem root = new RootSystem(DT, 1);

    VoltageState node() {
        VoltageState s = new VoltageState();
        root.addState(s);
        return s;
    }

    /** RC charging from 0 V: V_n = E (1 - (1 + dt/tau)^-n) for backward Euler; E (1 - e^(-t/tau)) physically. */
    @Test
    public void rcCharging() {
        double e = 10, r = 100, c = 0.01; // tau = 1 s = 20 steps
        double tau = r * c;
        VoltageState a = node(), b = node();
        root.addComponent(new VoltageSource("vs", a, null).setU(e));
        Resistor res = new Resistor(a, b).setR(r);
        root.addComponent(res);
        Capacitor cap = new Capacitor(b, null);
        cap.setC(c);
        root.addComponent(cap);

        double maxContErr = 0;
        for (int n = 1; n <= 200; n++) {
            root.step();
            double be = e * (1 - Math.pow(1 + DT / tau, -n));
            assertEquals("backward Euler, step " + n, be, b.state, 1e-9 * e);
            maxContErr = Math.max(maxContErr, Math.abs(b.state - e * (1 - Math.exp(-n * DT / tau))));
        }
        // global error of backward Euler on this problem peaks around t = tau at roughly (dt/2tau) * E/e
        assertTrue("continuous error " + maxContErr, maxContErr < 0.01 * e);
        assertEquals(e, b.state, 1e-3); // 10 tau later: settled (BE residual (1+dt/tau)^-200 = 6e-5)
        assertEquals(0.5 * c * b.state * b.state, cap.getE(), 1e-12);
    }

    /** Discharge: capacitor preset to V0 (as after NBT load), R to ground. Energy is lost only in R (and to BE damping). */
    @Test
    public void rcDischargeAndEnergy() {
        double v0 = 100, r = 10, c = 0.05; // tau = 0.5 s = 10 steps
        double tau = r * c;
        VoltageState b = node();
        Capacitor cap = new Capacitor(b, null);
        cap.setC(c);
        root.addComponent(cap);
        Resistor res = new Resistor(b, null).setR(r);
        root.addComponent(res);
        b.state = v0;

        double e0 = 0.5 * c * v0 * v0, dissipated = 0;
        for (int n = 1; n <= 100; n++) {
            root.step();
            assertEquals(v0 * Math.pow(1 + DT / tau, -n), b.state, 1e-9 * v0);
            dissipated += res.getP() * DT;
        }
        // backward Euler: sum of R-losses at the end-of-step currents equals the stored energy minus the
        // numerical damping term; it must never exceed the initial energy (method is dissipative, not generative)
        assertTrue(dissipated <= e0);
        assertEquals(e0, dissipated, 0.1 * e0); // within the O(dt/tau) = 10% scheme error
    }

    /** RL: I_n = (E/R)(1 - (1 + dt R/L)^-n) for backward Euler. Inductor current is positive aPin -> bPin. */
    @Test
    public void rlCurrentRise() {
        double e = 12, r = 6, l = 3; // tau = 0.5 s
        double tau = l / r;
        VoltageState a = node(), b = node();
        root.addComponent(new VoltageSource("vs", a, null).setU(e));
        root.addComponent(new Resistor(a, b).setR(r));
        Inductor ind = new Inductor("l", b, null);
        ind.setL(l);
        root.addComponent(ind);

        double maxContErr = 0;
        for (int n = 1; n <= 100; n++) {
            root.step();
            assertEquals(e / r * (1 - Math.pow(1 + DT / tau, -n)), ind.getCurrent(), 1e-9);
            maxContErr = Math.max(maxContErr, Math.abs(ind.getCurrent() - e / r * (1 - Math.exp(-n * DT / tau))));
        }
        assertTrue(maxContErr < 0.02 * e / r);
        assertEquals(e / r, ind.getCurrent(), 1e-3); // 10 tau: settled (BE residual 1.1^-100 = 7e-5)
        assertEquals(0, b.state, 1e-2); // ideal inductor: no DC voltage (BE residual E*1.1^-100 = 9e-4)
    }

    /**
     * LC tank: period 2 pi sqrt(LC). Backward Euler keeps the frequency to O((w dt)^2) but damps the
     * amplitude by 1/sqrt(1 + (w dt)^2) per step, so stored energy must decrease monotonically.
     */
    @Test
    public void lcOscillationPeriodAndNumericalDamping() {
        double l = 1, c = 1; // w = 1 rad/s, period 6.283 s = 125.7 steps
        VoltageState a = node();
        Capacitor cap = new Capacitor(a, null);
        cap.setC(c);
        root.addComponent(cap);
        Inductor ind = new Inductor("l", a, null);
        ind.setL(l);
        root.addComponent(ind);
        a.state = 1;

        double lastE = Double.MAX_VALUE, prev = a.state;
        int crossings = 0;
        double firstCross = 0, lastCross = 0;
        int steps = 1000;
        for (int n = 1; n <= steps; n++) {
            root.step();
            double en = cap.getE() + ind.getE();
            assertTrue("energy grew at step " + n, en <= lastE + 1e-15);
            lastE = en;
            if ((prev > 0) != (a.state > 0)) {
                double t = (n - 1 + prev / (prev - a.state)) * DT;
                if (crossings == 0) firstCross = t;
                lastCross = t;
                crossings++;
            }
            prev = a.state;
        }
        double halfPeriod = (lastCross - firstCross) / (crossings - 1);
        assertEquals(Math.PI * Math.sqrt(l * c), halfPeriod, 0.01 * Math.PI);
        double expectedEnergy = 0.5 * Math.pow(1 + DT * DT / (l * c), -steps); // E0 = 0.5
        assertEquals(expectedEnergy, lastE, 0.05 * expectedEnergy);
    }

    /** A capacitor charged through R carries the same current as R: i = C dV/dt. */
    @Ignore("EA bug: Capacitor.getCurrent() always returns 0, so ElectricalLoad.getI()/multimeters ignore capacitor current")
    @Test
    public void capacitorReportsItsCurrent() {
        VoltageState a = node(), b = node();
        root.addComponent(new VoltageSource("vs", a, null).setU(10));
        Resistor res = new Resistor(a, b).setR(100);
        root.addComponent(res);
        Capacitor cap = new Capacitor(b, null);
        cap.setC(0.01);
        root.addComponent(cap);
        root.step();
        root.step();
        assertEquals(res.getCurrent(), cap.getCurrent(), 1e-9);
    }

    /**
     * Two capacitors in parallel behave as one of C1 + C2.
     * Was an EA bug (fixed in WP16): both write their history current into the same node through SubSystem.addToI, which assigns
     * instead of accumulating; the matrix has (C1+C2)/dt but the RHS only C2's history (C2/dt * V_prev),
     * so the node voltage sags as if C1 leaked away every step (wrong time constant, energy not conserved).
     */
    @Test
    public void parallelCapacitorsAddUp() {
        RootSystem single = new RootSystem(DT, 1);
        VoltageState sa = new VoltageState(), sb = new VoltageState();
        single.addState(sa);
        single.addState(sb);
        single.addComponent(new VoltageSource("vs", sa, null).setU(10));
        single.addComponent(new Resistor(sa, sb).setR(100));
        Capacitor c = new Capacitor(sb, null);
        c.setC(0.03);
        single.addComponent(c);

        VoltageState a = node(), b = node();
        root.addComponent(new VoltageSource("vs", a, null).setU(10));
        root.addComponent(new Resistor(a, b).setR(100));
        Capacitor c1 = new Capacitor(b, null), c2 = new Capacitor(b, null);
        c1.setC(0.01);
        c2.setC(0.02);
        root.addComponent(c1);
        root.addComponent(c2);

        for (int n = 0; n < 40; n++) {
            single.step();
            root.step();
            assertEquals("step " + n, sb.state, b.state, 1e-9);
        }
    }

    /** Inductor current survives an NBT save/load (no energy lost or created by reloading a chunk). */
    @Test
    public void inductorStateRoundTripsThroughNbt() {
        VoltageState a = node(), b = node();
        root.addComponent(new VoltageSource("vs", a, null).setU(12));
        root.addComponent(new Resistor(a, b).setR(6));
        Inductor ind = new Inductor("l", b, null);
        ind.setL(3);
        root.addComponent(ind);
        for (int n = 0; n < 5; n++) root.step();
        double i = ind.getCurrent();
        NBTTagCompound nbt = new NBTTagCompound();
        ind.writeToNBT(nbt, "x");

        Inductor fresh = new Inductor("l");
        fresh.readFromNBT(nbt, "x");
        assertEquals(i, fresh.getCurrent(), 0);
    }
}
