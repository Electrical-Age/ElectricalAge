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

    /** Place a six-node device (1.12 id) at p on its floor face (as a player clicking the block below). */
    default mods.eln.node.six.SixNodeElement placeSix(int damage, BlockPos p) {
        return placeSix(damage, p, mods.eln.misc.Direction.YN);
    }

    /** Place a six-node device at p on face `side`; the block at p + side must be solid. */
    mods.eln.node.six.SixNodeElement placeSix(int damage, BlockPos p, mods.eln.misc.Direction side);

    /** Bytes for an element's networkUnserialize (the GUI packet path). */
    java.io.DataInputStream stream(Writer writer);

    interface Writer {
        void write(java.io.DataOutputStream out) throws java.io.IOException;
    }

    void line(String text);

    void check(String what, boolean ok, String detail);

    /** PASS when |measured - expected| <= 1% of |expected|. */
    void checkValue(String what, double measured, double expected);
}
