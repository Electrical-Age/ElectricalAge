package mods.eln.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** RegulatorProcess.setPid (Baughn 1573753560456499223) on a first-order plant: T' = (K u - T) / tau. */
public class RegulatorPidTest {
    static final double DT = 0.05;

    static final class Plant extends RegulatorProcess {
        double T, u, K = 1000, tau = 100, cap = 1;

        Plant() {
            super("pid");
        }

        @Override
        protected double getHit() {
            return T;
        }

        @Override
        protected void setCmd(double cmd) {
            u = cmd;
        }

        @Override
        protected boolean limitedUp() {
            return u > cap;
        }

        void run(double seconds) {
            for (double t = 0; t < seconds; t += DT) {
                process(DT);
                T += (K * Math.min(u, cap) - T) / tau * DT;
            }
        }
    }

    static Plant plant() {
        Plant p = new Plant();
        p.setPid(0.01, 1e-4, 0.02, 2);
        p.setTarget(300);
        return p;
    }

    @Test
    void itSettlesOnItsSetPointWithoutOffset() {
        Plant p = plant();
        double peak = 0;
        for (int i = 0; i < 600; i++) {
            p.run(1);
            peak = Math.max(peak, p.T);
        }
        assertEquals(300, p.T, 1, "settled");
        assertTrue(peak < 310, "overshoot: " + peak);
    }

    @Test
    void theCommandStaysWithinZeroAndOne() {
        Plant p = plant();
        p.T = 900;
        p.run(1);
        assertEquals(0, p.u, 0, "far over: shut");
        p.T = 0;
        p.setTarget(950);
        p.run(5);
        assertTrue(p.u <= 1, "" + p.u);
    }

    @Test
    void noWindupWhileThePlantCantGiveMore() {
        Plant p = plant();
        p.cap = 0.2;   // (a bed still catching: it gives no more than 200 K whatever the air)
        p.run(600);
        double held = p.errorIntegrated;
        assertTrue(held < 0.5, "the integral didn't wind up: " + held);
        p.cap = 1;
        double peak = 0;
        for (int i = 0; i < 600; i++) {
            p.run(1);
            peak = Math.max(peak, p.T);
        }
        assertTrue(peak < 315, "no overshoot once the plant catches up: " + peak);
    }

    @Test
    void aLoadKeepsItsIntegral() {
        Plant p = plant();
        p.run(600);
        var t = new net.minecraft.nbt.CompoundTag();
        p.writeToNBT(t, "");
        Plant q = new Plant();
        q.readFromNBT(t, "");
        q.setPid(0.01, 1e-4, 0.02, 2);
        q.T = p.T;
        q.process(DT);
        assertEquals(p.errorIntegrated, q.errorIntegrated, 1e-3, "it carries on where it was");
    }
}
