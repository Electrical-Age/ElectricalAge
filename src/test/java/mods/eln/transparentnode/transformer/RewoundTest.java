package mods.eln.transparentnode.transformer;

import mods.eln.Eln;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Converters saved for 1.12's tiers, rewound once for the real-world ones by their windings' tiers (Baughn 2026-11-06). */
class RewoundTest {
    @Test
    void byTheTiersOfTheirWindings() {
        assertArrayEquals(new int[]{5, 24}, TransformerElement.rewound(1, 4, Eln.LVU, Eln.MVU));   // 50 -> 200: 48 -> 230.4
        assertArrayEquals(new int[]{24, 5}, TransformerElement.rewound(4, 1, Eln.MVU, Eln.LVU));
        assertArrayEquals(new int[]{26, 15}, TransformerElement.rewound(4, 1, Eln.HVU, Eln.MVU));  // 800 -> 200: 400 -> 230.8
        assertArrayEquals(new int[]{2, 55}, TransformerElement.rewound(1, 4, Eln.HVU, Eln.VVU));   // 800 -> 3200: 400 -> 11 kV
        assertArrayEquals(new int[]{5, 24}, TransformerElement.rewound(2, 8, Eln.LVU, Eln.MVU));
    }

    @Test
    void oneTierTakenFor48To230() {
        assertArrayEquals(new int[]{24, 5}, TransformerElement.rewound(4, 1, Eln.MVU, Eln.MVU));
        assertArrayEquals(new int[]{5, 24}, TransformerElement.rewound(1, 4, Eln.LVU, Eln.LVU));
    }

    @Test
    void otherRatiosAndCoilsOnlyAdded() {
        assertNull(TransformerElement.rewound(1, 2, Eln.LVU, Eln.MVU));
        assertNull(TransformerElement.rewound(3, 3, Eln.LVU, Eln.LVU));
        int[] t = TransformerElement.rewound(4, 1, Eln.HVU, Eln.MVU);
        assertTrue(t[0] >= 4 && t[1] >= 1);
    }
}
