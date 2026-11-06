package mods.eln.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A tooltip's voltage line is left out when the name already says it (Baughn: pointless). */
class NameShowsVoltageTest {
    @Test
    void namesWithTheirVoltage() {
        assertTrue(Utils.nameShowsVoltage("48V Macerator", 48));
        assertTrue(Utils.nameShowsVoltage("Small 230V Iron Heating Corp", 230));
        assertTrue(Utils.nameShowsVoltage("400V Defence Turret", 400.0));
        assertFalse(Utils.nameShowsVoltage("148V Turbine", 48));   // (not a different number ending in 48)
        assertFalse(Utils.nameShowsVoltage("Auto Miner", 200));
        assertFalse(Utils.nameShowsVoltage("48V Turbine", 230));
        assertFalse(Utils.nameShowsVoltage(null, 50));
    }
}
