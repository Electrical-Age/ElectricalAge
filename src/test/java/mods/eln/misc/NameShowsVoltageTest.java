package mods.eln.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A tooltip's voltage line is left out when the name already says it (Baughn: pointless). */
class NameShowsVoltageTest {
    @Test
    void namesWithTheirVoltage() {
        assertTrue(Utils.nameShowsVoltage("50V Macerator", 50));
        assertTrue(Utils.nameShowsVoltage("Small 200V Iron Heating Corp", 200));
        assertTrue(Utils.nameShowsVoltage("800V Defence Turret", 800.0));
        assertFalse(Utils.nameShowsVoltage("150V Turbine", 50));   // (not a different number ending in 50)
        assertFalse(Utils.nameShowsVoltage("Auto Miner", 200));
        assertFalse(Utils.nameShowsVoltage("50V Turbine", 200));
        assertFalse(Utils.nameShowsVoltage(null, 50));
    }
}
