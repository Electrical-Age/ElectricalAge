package mods.eln.anvil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The electrical anvil's rules (Baughn 2026-10-28): penalty never up, annealed down to a floor; 25% a unit; credit. */
public class AnvilRulesTest {
    @Test
    void floorIsTheCheapestTreeOfCombines() {
        assertEquals(0, AnvilRules.penaltyFloor(0));
        assertEquals(1, AnvilRules.penaltyFloor(1));
        assertEquals(3, AnvilRules.penaltyFloor(2));
        assertEquals(3, AnvilRules.penaltyFloor(3));
        assertEquals(7, AnvilRules.penaltyFloor(4));
        assertEquals(15, AnvilRules.penaltyFloor(8));
    }

    @Test
    void annealingStepsDownToTheFloorAndNeverUp() {
        assertEquals(15, AnvilRules.anneal(31, 0));
        assertEquals(0, AnvilRules.anneal(1, 0));
        assertEquals(15, AnvilRules.anneal(31, 5));  // one notch a repair; floor 7 for 5 enchantments
        assertEquals(7, AnvilRules.anneal(15, 5));
        assertEquals(7, AnvilRules.anneal(7, 5));
        assertEquals(0, AnvilRules.anneal(0, 3));    // an enchanting table's tool (penalty 0) stays at 0
        assertEquals(3, AnvilRules.anneal(3, 8));    // under the floor already: untouched
    }

    @Test
    void aUnitIsAQuarterAndCreditCoversToppingUp() {
        // a diamond pickaxe (1561): a step restores 390 (a quarter, rounded down as vanilla), exactly one unit
        assertEquals(390, AnvilRules.stepRestore(1000, 1561));
        assertEquals(1.0, AnvilRules.unitsFor(390, 1561), 1e-12);
        assertEquals(1, AnvilRules.unitsToConsume(0, AnvilRules.unitsFor(390, 1561)));
        assertEquals(100, AnvilRules.stepRestore(100, 1561));
        assertEquals(100 / 390.0, AnvilRules.unitsFor(100, 1561), 1e-9);
        // topping up 0.26 of a unit with no credit takes one diamond (0.74 stays as credit)
        assertEquals(1, AnvilRules.unitsToConsume(0, 0.26));
        assertEquals(0, AnvilRules.unitsToConsume(0.74, 0.26));
        assertEquals(1, AnvilRules.unitsToConsume(0.5, 1.0));
        assertEquals(0, AnvilRules.unitsToConsume(1.0, 1.0));
    }
}
