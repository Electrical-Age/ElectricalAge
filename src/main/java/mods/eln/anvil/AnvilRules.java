package mods.eln.anvil;

/**
 * The electrical anvil's arithmetic, apart from Minecraft (unit-tested in AnvilRulesTest). Baughn 2026-10-28: no XP,
 * repairs never raise the anvil penalty and step an old one down ("we shouldn't penalise players for not knowing about
 * the block"), vanilla's 25% of the tool per unit of material, but the desk keeps fractions of a unit as credit.
 */
public final class AnvilRules {
    private AnvilRules() {
    }

    /**
     * The least anvil penalty (vanilla REPAIR_COST, 2^n - 1 after n anvil operations) a tool with this many enchantments
     * could have, had they gone on in the cheapest order: a balanced tree of combines, ceil(log2(n + 1)) deep. Annealing
     * stops there, so the desk can't make room for more books than a fresh tool would take.
     */
    public static int penaltyFloor(int enchantments) {
        if (enchantments <= 0) return 0;
        int depth = 32 - Integer.numberOfLeadingZeros(enchantments);   // ceil(log2(n + 1))
        return depth >= 31 ? Integer.MAX_VALUE : (1 << depth) - 1;
    }

    /** One annealing step: down a notch of vanilla's ladder (31 -> 15 -> 7 -> 3 -> 1 -> 0), never below the floor, never up. */
    public static int anneal(int penalty, int enchantments) {
        int floor = penaltyFloor(enchantments);
        if (penalty <= floor) return penalty;
        return Math.max(floor, (penalty - 1) >> 1);
    }

    /** A unit's worth of durability: a quarter of the maximum, rounded down as vanilla's anvil does (at least 1). */
    public static int unit(int maxDamage) {
        return Math.max(1, maxDamage / 4);
    }

    /** Durability one repair step restores: a unit's worth or what's missing, if less. */
    public static int stepRestore(int damage, int maxDamage) {
        return Math.max(0, Math.min(damage, unit(maxDamage)));
    }

    /** Units of material (fractions too) that restoring this much durability takes. */
    public static double unitsFor(int restore, int maxDamage) {
        return maxDamage <= 0 ? 0 : restore / (double) unit(maxDamage);
    }

    /** Whole units to take from the material slot so the credit covers the step (0 if it does already). */
    public static int unitsToConsume(double credit, double units) {
        double short_ = units - credit;
        return short_ <= 1e-9 ? 0 : (int) Math.ceil(short_ - 1e-9);
    }
}
