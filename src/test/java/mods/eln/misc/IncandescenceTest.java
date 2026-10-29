package mods.eln.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hot things glow (Baughn 2026-10-28): the light levels by temperature, their hysteresis, the look and its steps. */
public class IncandescenceTest {
    @Test
    void lightByTemperature() {
        assertEquals(0, Incandescence.lightLevel(500));
        assertEquals(1, Incandescence.lightLevel(525));   // the Draper point
        assertEquals(2, Incandescence.lightLevel(600));   // dull red
        assertEquals(8, Incandescence.lightLevel(900));   // orange
        assertEquals(14, Incandescence.lightLevel(1300)); // white
        assertEquals(15, Incandescence.lightLevel(1500));
        for (double c = 0; c < 1500; c += 7) assertTrue(Incandescence.lightLevel(c) <= Incandescence.lightLevel(c + 7));
    }

    @Test
    void upAtOnceDownOnlyWellBelow() {
        var l = new Incandescence.Light();
        assertTrue(l.update(null, 905));
        assertEquals(8, l.level);
        assertTrue(!l.update(null, 890));   // just under the 900 step: stays (hysteresis)
        assertEquals(8, l.level);
        assertTrue(l.update(null, 870));    // 25 K under it: one down
        assertEquals(7, l.level);
        assertTrue(l.update(null, 400));
        assertEquals(0, l.level);
        assertTrue(l.update(null, 1300));   // straight up again
        assertEquals(14, l.level);
        var cold = new Incandescence.Light();
        assertTrue(cold.update(null, 20));   // the first check reports even at 0: a loaded node's saved light goes out
        assertEquals(0, cold.level);
        assertTrue(!cold.update(null, 20));
    }

    @Test
    void lookAndSteps() {
        assertEquals(0, Incandescence.intensity(500));
        assertEquals(0, Incandescence.intensity(525));
        assertTrue(Incandescence.intensity(600) > 0.1 && Incandescence.intensity(600) < 0.35);
        assertEquals(1, Incandescence.intensity(1100), 1e-6);
        for (double c = 530; c < 1400; c += 10) assertTrue(Incandescence.intensity(c) <= Incandescence.intensity(c + 10));
        float[] red = Incandescence.color(600), white = Incandescence.color(1300);
        assertTrue(red[0] > red[1] && red[1] < 0.1f);
        assertTrue(white[1] > 0.9f);
        assertEquals(0, Incandescence.step(400));
        for (double c = 510; c < 1400; c += 3) {
            int s = Incandescence.step(c);
            assertTrue(s > 0);
            assertEquals(c, Incandescence.stepCelsius(s), 12.5 + 1e-9);   // the client sees it within half a step
        }
    }

    @Test
    void resistorWarningScale() {
        // a 130 C resistor: dark to 60% of its limit, orange (900 C on the glow's scale) at its limit
        assertEquals(0, Incandescence.warningCelsius(0.5 * 130, 130));
        assertEquals(Incandescence.DRAPER_C, Incandescence.warningCelsius(0.6 * 130 + 1e-9, 130), 1e-3);
        assertEquals(900, Incandescence.warningCelsius(130, 130), 1e-9);
    }
}
