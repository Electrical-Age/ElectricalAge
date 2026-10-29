package mods.eln.sound;

import mods.eln.misc.Coordonate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The turbines' overspeed whistle (AdventurAgent 2026-10-29: none heard at 896-916 rad/s): silent at rated speed, sounding
 * from 105 % (840 rad/s), full at the trip and above it (a tripped line coasting down still whistles); not faded by
 * distance, so it's heard down the line.
 */
public class TurbineSoundsTest {
    @Test
    void whistlesOverspeed() {
        double[] rads = {0};
        var loops = MachineSounds.turbine(new Coordonate(), () -> rads[0] / 800, () -> 0.5, false, 1f);
        LoopedSound whistle = loops.stream().filter(l -> l.getSample().equals("eln:overspeed_whistle")).findFirst().orElseThrow();
        rads[0] = 800;
        assertEquals(0, whistle.getVolume(), 1e-6, "rated speed: silent");
        rads[0] = 830;
        assertEquals(0, whistle.getVolume(), 1e-6, "under 105 %: silent");
        for (double r : new double[]{850, 880, 896, 916, 990}) {
            rads[0] = r;
            assertTrue(whistle.getVolume() > (r >= 880 ? 0.29 : 0.05), r + " rad/s: whistles, " + whistle.getVolume());
        }
        assertEquals(LoopedSound.Attenuation.NONE, whistle.getAttenuation(), "heard down the line");
        // and a gas turbine's too
        var gas = MachineSounds.turbine(new Coordonate(), () -> 1.12, () -> 0.0, true, 1f);
        assertTrue(gas.stream().anyMatch(l -> l.getSample().equals("eln:overspeed_whistle") && l.getVolume() > 0.29));
    }
}
