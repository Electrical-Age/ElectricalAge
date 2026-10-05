package mods.eln.selftest;

/**
 * One extra `/eln selftest` case (device batches add theirs in mods.eln.selftest.cases.&lt;Batch&gt;Cases). Each case gets
 * its own stone platform row of {@link #width()} x 3 blocks (ctx.at(0..width-1, 0, 0..2)); place devices at y = 1
 * (on the floor) with ctx.placeSix / ctx.placeTransparent. Cases run in plain `/eln selftest` (not keep/verify),
 * are measured after the same number of ticks, and are cleaned up with everything else.
 */
public interface SelfTestCase {
    String name();

    /** Platform length along +X (1..16). */
    default int width() {
        return 8;
    }

    /** Place and configure the devices. Throwing = FAIL "&lt;name&gt; build". */
    void build(SelfTestContext ctx);

    /** After the ticks: ctx.check / ctx.checkValue against physics. Throwing = FAIL "&lt;name&gt; measure". */
    void measure(SelfTestContext ctx);
}
