package mods.eln.api.testkit;

/**
 * Test helpers for machines built on the API, for JUnit with no Minecraft world: bench() runs machines in EA's
 * real circuit simulator, tick by tick in the game's order.
 */
public final class ElnTestKit {
    private ElnTestKit() {
    }

    /** An empty bench: no supply until supply(volts, ohms), no line resistance until line(ohms). */
    public static Bench bench() {
        return new Bench();
    }
}
