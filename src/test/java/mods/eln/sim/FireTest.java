package mods.eln.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A fire's burn level lags its setting (Baughn: coal plants change burn rate slowly). */
public class FireTest {
    static final double DT = 0.05;

    static void run(Fire f, double setting, boolean fuel, double seconds) {
        for (double t = 0; t < seconds; t += DT) f.step(setting, fuel, DT);
    }

    @Test
    void coalFollowsItsSettingOverMinutes() {
        Fire f = new Fire(Fire.COAL);
        f.level = 0.2;
        run(f, 1.0, true, 180);   // one time constant
        assertEquals(0.2 + 0.8 * (1 - Math.exp(-1)), f.level, 0.02);
        run(f, 1.0, true, 900);
        assertEquals(1.0, f.level, 0.01);
    }

    @Test
    void askedBelowItsMinimumItGoesOutAndRelightingTakesTime() {
        Fire f = new Fire(Fire.COAL);
        f.level = 0.5;
        run(f, 0.0, true, 1200);
        assertEquals(0.0, f.level, 0.0, "out");
        run(f, 1.0, true, Fire.COAL.kindle - 1);
        assertTrue(f.lighting() && f.level == 0, "still being lit");
        run(f, 1.0, true, 2);
        assertEquals(Fire.minFire, f.level, 0.01, "lit, at the minimum");
    }

    @Test
    void noFuelNoFireAndOilIsQuick() {
        Fire f = new Fire(Fire.COAL);
        f.level = 0.8;
        f.step(0.8, false, DT);
        assertEquals(0, f.level);
        Fire o = new Fire(Fire.OIL);
        o.level = 0.2;
        run(o, 1.0, true, 60);
        assertTrue(o.level > 0.9, "oil: " + o.level);
    }

    /**
     * Baughn: a furnace at full throttle with no fuel coming climbed to 100% on embers (the bed's energy only decays
     * towards zero), then flared to full power the moment fuel came back. The fire now can't outburn its bed.
     */
    @Test
    void aFurnaceWithNoFuelComingBanksUntilItsOut() {
        FurnaceProcess p = new FurnaceProcess(new ThermalLoad());
        p.nominalCombustibleEnergy = 40000;
        p.nominalPower = 3000;
        p.setGain(1);
        p.fire.level = 0.5;
        p.combustibleEnergy = 20000;   // (a half-spent bed, nothing coming)
        double maxLevelLate = 0;
        for (double t = 0; t < 3600; t += DT) {
            p.process(DT);
            assertTrue(p.fire.level <= 0.5 + 1e-9, "it never climbs towards the setting: " + p.fire.level);
            if (t > 1800) maxLevelLate = Math.max(maxLevelLate, p.fire.level);
        }
        assertEquals(0.0, p.fire.level, 0.0, "out once the bed is spent");
        assertEquals(0.0, maxLevelLate, 0.0, "and stays out");
        // fuel back: it relights and climbs slowly, no instant full power
        p.combustibleEnergy = 40000;
        for (double t = 0; t < 31; t += DT) p.process(DT);
        p.combustibleEnergy = 40000;
        assertTrue(p.getP() < 0.25 * p.nominalPower, "no flare: " + p.getP() + " W");
    }
}
