package mods.eln.transparentnode.transformer;

/**
 * A converter winding as drawn (Baughn 2026-11-07: see the configuration at a glance, stylized, no geometry per turn).
 * The coil wraps its leg of the core above the board's parts, up to the yoke; its build into the window between the legs is its
 * share of the core's window (turns x mm²), so the two coils meet in the middle when the core is full. Its turns are laid
 * in layers of square turns: few thick turns make few fat stripes, many thin ones a fine weave in many layers. Sizes in
 * px, the primary's leg at z = -5 (the secondary's is the same turned half round).
 */
public record Coil(double build, int turnsPerLayer, int layers, boolean over) {
    /** The leg's visible height (from the board to the yoke) and the window's width between the legs' coils' holes. */
    public static final double HEIGHT = 5.65, WINDOW = 7.7, LEAST = 0.4;

    /** The coil of turns of mm² wire on a core of window (mm² turns) whose two windings fill total. Null for no turns. */
    public static Coil of(int turns, double mm2, double window, double total) {
        if (turns <= 0 || window <= 0) return null;
        // (an overfull core's coils share the window as the full ones would: they meet in the middle and show red, and never
        // reach out of the block)
        double build = Math.max(LEAST, turns * mm2 / Math.max(window, total) * WINDOW);
        double pitch = Math.sqrt(build * HEIGHT / turns);
        int tpl = (int) Math.max(1, Math.min(256, Math.round(HEIGHT / pitch)));
        int layers = (turns + tpl - 1) / tpl;
        return new Coil(build, tpl, layers, total > window + 1e-9);
    }
}
