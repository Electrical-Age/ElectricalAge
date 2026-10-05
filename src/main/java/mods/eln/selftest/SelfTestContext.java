package mods.eln.selftest;

import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.io.DataInputStream;

/** What a {@link SelfTestCase} may use. Positions are relative to the case's own platform origin. */
public interface SelfTestContext {
    WorldServer world();

    /** Case platform origin + (dx, dy, dz); the platform is y = 0, devices go at y = 1. */
    BlockPos at(int dx, int dy, int dz);

    /** Place a six-node device (damage = subId + (id &lt;&lt; 6)) on the floor of p (as a player clicking the block below). */
    SixNodeElement placeSix(int damage, BlockPos p);

    /**
     * Place a six-node device in p on its {@code side} face (YN = floor, YP = ceiling, XN/XP/ZN/ZP = walls), as a
     * player clicking the face of the neighbour block at p + side that points back at p. The neighbour must be solid.
     * The node block is removed by the cleanup like the floor ones.
     */
    SixNodeElement placeSix(int damage, BlockPos p, mods.eln.misc.Direction side);

    /** Place a transparent-node device standing on the block below p. */
    TransparentNodeElement placeTransparent(int damage, BlockPos p);

    /** Bytes for an element's networkUnserialize (the GUI packet path), e.g. stream(out -> { out.writeByte(id); ... }). */
    DataInputStream stream(SelfTest.Writer writer);

    void line(String text);

    void check(String what, boolean ok, String detail);

    /** PASS when |measured - expected| <= 1% of |expected|. */
    void checkValue(String what, double measured, double expected);
}
