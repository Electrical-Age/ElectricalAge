package mods.eln.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a {@link TestCase} may use: the 26.x form of the 1.12 selftest's SelfTestContext (legacy/src-1.12, selftest/),
 * so the 1.12 cases port with type changes only. Positions are relative to the case's structure (y = 0 is its stone
 * floor, devices go at y = 1); {@link #at} returns absolute positions.
 */
public interface TestContext {
    ServerLevel level();

    BlockPos at(int dx, int dy, int dz);

    /** Put a stone block at the absolute position p (support for a device outside the floor). */
    void floor(BlockPos p);

    void line(String text);

    void check(String what, boolean ok, String detail);

    /** PASS when |measured - expected| <= 1% of |expected|. */
    void checkValue(String what, double measured, double expected);
}
