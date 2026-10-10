package mods.eln.gametest;

/**
 * One Electrical Age game test: build, wait {@link #ticks()} server ticks, measure. The 26.x form of the 1.12
 * selftest's TestCase. Each case runs in its own GameTest structure eln:floor_WxHxD (an empty box with a stone
 * floor, made by script/gen_test_structures.py), so the size must name a generated structure.
 */
public interface TestCase {
    /** Lower-case id, [a-z0-9_]: the test is eln:&lt;id&gt;. Default: name() made into an id. */
    default String id() {
        return name().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /** Human name (the 1.12 selftest cases have one); default: the id. Override one of the two. */
    default String name() {
        return id();
    }

    default int width() {
        return 8;
    }

    default int height() {
        return 4;
    }

    default int depth() {
        return 3;
    }

    /** Rows of stone floor laid at y = 0 (the 1.12 selftest row was 3 deep, whatever the structure size). */
    default int floorDepth() {
        return Math.min(3, depth());
    }

    default int ticks() {
        return 40;
    }

    void build(TestContext ctx);

    void measure(TestContext ctx);
}
