package mods.eln.sim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Thermal network (Simulator.thermalStep). Each ThermalLoad is a lumped heat capacity C (J/K) at temperature Tc
 * (K above ambient), leaking to ambient through Rp (K/W); a ThermalConnection conducts (T2-T1)/(Rs1+Rs2).
 * Integration is EXPLICIT (forward) EULER: every flow is computed from the current temperatures, then
 * Tc += P dt / C. Fast loads step at the thermal period (1/400 s, 20 steps per tick), slow loads once per
 * tick (0.05 s). Exact discrete expectations are asserted tightly, continuous physics loosely.
 */
public class ThermalTest {
    static final double DT_FAST = SimHarness.THERMAL_PERIOD;

    SimHarness h = new SimHarness();

    /** Constant heater process, like ResistorHeatThermalLoad with a fixed resistor power. */
    static IProcess heater(final ThermalLoad load, final double watts) {
        return new IProcess() {
            @Override
            public void process(double time) {
                load.movePowerTo(watts);
            }
        };
    }

    /** No losses: dT/dt = P/C, so after t seconds T = P t / C (Euler is exact for a ramp). */
    @Test
    public void heatCapacity() {
        ThermalLoad fast = new ThermalLoad(0, 1e30, 1, 50);
        ThermalLoad slow = new ThermalLoad(0, 1e30, 1, 50);
        slow.setAsSlow();
        h.sim.addThermalLoad(fast);
        h.sim.addThermalLoad(slow);
        h.sim.addThermalFastProcess(heater(fast, 100));
        h.sim.addThermalSlowProcess(heater(slow, 100));
        h.seconds(10);
        assertEquals(100 * 10 / 50.0, fast.Tc, 1e-9);
        assertEquals(100 * 10 / 50.0, slow.Tc, 1e-9);
    }

    /** Heater P into C with loss Rp: T -> P Rp with tau = Rp C. Forward Euler: T_n = P Rp (1 - (1 - dt/tau)^n). */
    @Test
    public void approachToSteadyState() {
        double p = 40, rp = 2.5, c = 4; // tau = 10 s, Tss = 100 K
        double tau = rp * c;
        ThermalLoad t = new ThermalLoad(0, rp, 1, c);
        h.sim.addThermalLoad(t);
        h.sim.addThermalFastProcess(heater(t, p));
        double maxContErr = 0;
        for (int tick = 1; tick <= 3000; tick++) { // 150 s = 15 tau
            h.tick();
            int n = tick * 20;
            assertEquals("tick " + tick, p * rp * (1 - Math.pow(1 - DT_FAST / tau, n)), t.Tc, 1e-9 * p * rp);
            maxContErr = Math.max(maxContErr, Math.abs(t.Tc - p * rp * (1 - Math.exp(-n * DT_FAST / tau))));
        }
        assertTrue(maxContErr < 1e-3 * p * rp);
        assertEquals(p * rp, t.Tc, 1e-3);
        assertEquals(p * rp / rp, t.getPower(), 1e-2); // at steady state all heater power leaves through Rp
    }

    /**
     * Two loads (no losses) joined by conduction R = Rs1 + Rs2: energy C1T1 + C2T2 is conserved, both go to
     * the capacity-weighted mean, and the difference decays with tau = R C1 C2 / (C1 + C2).
     */
    @Test
    public void conductionBetweenTwoLoads() {
        double c1 = 1, c2 = 3, rs1 = 0.25, rs2 = 0.75; // R = 1 K/W, tau = 0.75 s
        ThermalLoad a = new ThermalLoad(100, 1e30, rs1, c1), b = new ThermalLoad(0, 1e30, rs2, c2);
        h.sim.addThermalLoad(a);
        h.sim.addThermalLoad(b);
        h.sim.addThermalConnection(new ThermalConnection(a, b));
        double e0 = c1 * a.Tc + c2 * b.Tc;
        double k = DT_FAST / (rs1 + rs2) * (1 / c1 + 1 / c2);
        for (int tick = 1; tick <= 100; tick++) {
            h.tick();
            assertEquals("energy, tick " + tick, e0, c1 * a.Tc + c2 * b.Tc, 1e-9 * e0);
            assertEquals("difference, tick " + tick, 100 * Math.pow(1 - k, tick * 20), a.Tc - b.Tc, 1e-9 * 100);
        }
        double tau = (rs1 + rs2) * c1 * c2 / (c1 + c2);
        // continuous solution: forward Euler error after 6.7 tau is ~ n k^2 / 2 = 1% of the (tiny) residual
        assertEquals(100 * Math.exp(-5.0 / tau), a.Tc - b.Tc, 0.02 * 100 * Math.exp(-5.0 / tau));
        assertEquals(25 - 0.25 * (a.Tc - b.Tc), b.Tc, 1e-9); // mean 25 K, remaining difference shared by capacity
        assertEquals(25, b.Tc, 0.05);
    }

    /** ThermalResistor (an explicit process, used by e.g. heat sinks / furnaces) conducts (Ta - Tb)/R. */
    @Test
    public void thermalResistorConducts() {
        ThermalLoad a = new ThermalLoad(50, 1e30, 1, 2), b = new ThermalLoad(10, 1e30, 1, 2);
        h.sim.addThermalLoad(a);
        h.sim.addThermalLoad(b);
        ThermalResistor r = new ThermalResistor(a, b);
        r.setR(4);
        assertEquals(10, r.getP(), 1e-12);
        h.sim.addThermalFastProcess(r);
        h.seconds(60); // tau = R C/2 = 4 s
        assertEquals(30, a.Tc, 1e-3);
        assertEquals(30, b.Tc, 1e-3);
    }

    /**
     * "Power drop" initializer (machines): setMaximalPower(P) picks C = P tau / Twarm and Rp = Twarm / P so that
     * running at exactly the rated power settles at the warm limit with time constant heatingTao.
     */
    @Test
    public void powerDropInitializerHitsWarmLimitAtRatedPower() {
        double warm = 100, tauH = 10, pRated = 50;
        ThermalLoadInitializerByPowerDrop init = new ThermalLoadInitializerByPowerDrop(warm, -40, tauH, 5);
        init.setMaximalPower(pRated);
        ThermalLoad t = new ThermalLoad();
        init.applyTo(t);
        h.sim.addThermalLoad(t);
        h.sim.addThermalFastProcess(heater(t, pRated));
        h.seconds(tauH);
        assertEquals(warm * (1 - Math.exp(-1)), t.Tc, 0.05);
        h.seconds(10 * tauH);
        assertEquals(warm, t.Tc, 0.01);
    }

    /** Cable/six-node initializer: at rated power T -> warm limit with tau = heatingTao; conduction R*C = conductionTao. */
    @Test
    public void cableInitializerTimeConstants() {
        double warm = 130, tauH = 30, tauC = 0.5, p = 20;
        ThermalLoadInitializer init = new ThermalLoadInitializer(warm, -100, tauH, tauC);
        init.setMaximalPower(p);
        ThermalLoad a = new ThermalLoad(), b = new ThermalLoad();
        init.applyTo(a);
        init.applyTo(b);
        assertEquals(tauH, a.Rp * a.C, 1e-9);
        assertEquals(warm, p * a.Rp, 1e-9);
        assertEquals(tauC, (a.Rs + b.Rs) * a.C, 1e-9);
    }

    /** Simulator.checkThermalLoad rejects loads whose capacity is too small for the explicit thermal step. */
    @Test
    public void stabilityGuardRejectsTooSmallCapacity() {
        double rs = 0.01, rp = 1;
        double cMin = h.sim.getMinimalThermalC(rs, rp);
        assertEquals(3 * DT_FAST / (1 / (1 / rs + 1 / rp)), cMin, 1e-12);
        assertTrue(h.sim.checkThermalLoad(rs, rp, cMin * 1.01));
        try {
            h.sim.checkThermalLoad(rs, rp, cMin * 0.99);
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    /**
     * A ThermalConnection between a fast and a slow load is dropped (logged, not thrown): no heat flows.
     * Documents current behaviour, which is a trap for the port: devices must put both ends on the same list.
     */
    @Test
    public void fastSlowConnectionIsSilentlyDropped() {
        ThermalLoad fast = new ThermalLoad(100, 1e30, 0.5, 1), slow = new ThermalLoad(0, 1e30, 0.5, 1);
        slow.setAsSlow();
        h.sim.addThermalLoad(fast);
        h.sim.addThermalLoad(slow);
        h.sim.addThermalConnection(new ThermalConnection(fast, slow));
        h.seconds(5);
        assertEquals(100, fast.Tc, 1e-9);
        assertEquals(0, slow.Tc, 1e-9);
    }
}
