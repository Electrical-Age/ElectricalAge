package mods.eln.transparentnode.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Converter windings as drawn (Coil; Baughn 2026-11-07): sized by their share of the window, turns in layers. */
class CoilTest {
    @Test
    void fortyEightToTwoThirtyOnACheapCore() {
        double total = 12 * 2.5 + 58 * 1.5;   // 117 of 150
        Coil p = Coil.of(12, 2.5, 150, total), s = Coil.of(58, 1.5, 150, total);
        assertEquals(12 * 2.5 / 150 * Coil.WINDOW, p.build(), 1e-9);
        assertTrue(p.build() + s.build() < Coil.WINDOW, "they don't meet: the core has room");
        assertFalse(p.over() || s.over());
        assertTrue(p.turnsPerLayer() <= 12 && p.turnsPerLayer() * p.layers() >= 12);
        assertTrue(s.layers() >= p.layers(), "more, thinner turns: at least as many layers");
    }

    @Test
    void fullCoresMeetAndOverfullOnesShow() {
        Coil a = Coil.of(30, 2.5, 150, 150), b = Coil.of(50, 1.5, 150, 150);
        assertEquals(Coil.WINDOW, a.build() + b.build(), 1e-9);
        assertTrue(Coil.of(64, 2.5, 150, 320).over());
    }

    @Test
    void manyThinTurnsMakeAFineWeave() {
        Coil c = Coil.of(688, 0.5, 800, 688 * 0.5 + 25 * 10);
        assertTrue(c.turnsPerLayer() > 20 && c.layers() > 10, c.toString());
        assertNull(Coil.of(0, 2.5, 150, 0));
        assertNull(Coil.of(10, 2.5, 0, 25));
    }
}
