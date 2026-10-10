package mods.eln.gametest;

/**
 * One Electrical Age game test: build, wait {@link #ticks()} server ticks, measure. The 26.x form of the 1.12
 * selftest's SelfTestCase. Each case runs in its own GameTest structure eln:floor_WxHxD (an empty box with a stone
 * floor, made by script/gen_test_structures.py), so the size must name a generated structure.
 */
public interface TestCase {
    /** Lower-case id, [a-z0-9_]: the test is eln:&lt;id&gt;. */
    String id();

    default int width() {
        return 8;
    }

    default int height() {
        return 4;
    }

    default int depth() {
        return 3;
    }

    default int ticks() {
        return 40;
    }

    void build(TestContext ctx);

    void measure(TestContext ctx);
}
