package mods.eln.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a {@link TestCase} may use: the 26.x form of the 1.12 selftest's TestContext (legacy/src-1.12, selftest/),
 * so the 1.12 cases port with type changes only. Positions are relative to the case's structure (y = 0 is its stone
 * floor, devices go at y = 1); {@link #at} returns absolute positions.
 */
public interface TestContext {
    ServerLevel level();

    /** 1.12 name of level() (the ported selftest cases use it). */
    default ServerLevel world() {
        return level();
    }

    BlockPos at(int dx, int dy, int dz);

    /** Put a stone block at the absolute position p (support for a device outside the floor). */
    void floor(BlockPos p);

    /** Place a six-node device (1.12 id) at p on its floor face (as a player clicking the block below). */
    default mods.eln.node.six.SixNodeElement placeSix(int damage, BlockPos p) {
        return placeSix(damage, p, mods.eln.misc.Direction.YN);
    }

    /** Place a six-node device at p on face `side`; the block at p + side must be solid. */
    default mods.eln.node.six.SixNodeElement placeSix(int damage, BlockPos p, mods.eln.misc.Direction side) {
        mods.eln.node.six.SixNodeDescriptor d = mods.eln.Eln.sixNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no six node descriptor " + damage);
        net.minecraft.server.level.ServerLevel level = level();
        net.minecraft.world.entity.player.Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        boolean ok = ((mods.eln.node.six.SixNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, side);
        if (!ok) throw new IllegalStateException("placing " + d.name + " at " + p + " on " + side + " failed: here "
            + level.getBlockState(p) + ", support " + level.getBlockState(p.relative(side.toEnumFacing())) + ", checkCanPlace "
            + d.checkCanPlace(new mods.eln.misc.Coordonate(p, level), side, mods.eln.misc.LRDU.Up));
        mods.eln.node.NodeBase node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level));
        if (!(node instanceof mods.eln.node.six.SixNode)) throw new IllegalStateException("no SixNode at " + p);
        return ((mods.eln.node.six.SixNode) node).getElement(side);
    }

    /**
     * Place a transparent-node device as a player clicking the top of the block below p (front from the fake player's
     * view, like 1.12). The node lands at p + the descriptor's spawn delta; use element.node.coordonate.
     */
    default mods.eln.node.transparent.TransparentNodeElement placeTransparent(int damage, BlockPos p) {
        mods.eln.node.transparent.TransparentNodeDescriptor d = mods.eln.Eln.transparentNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no transparent node descriptor " + damage);
        net.minecraft.server.level.ServerLevel level = level();
        net.minecraft.world.entity.player.Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        mods.eln.node.transparent.TransparentNode node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem)
            .placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
        if (node == null) {
            mods.eln.misc.Direction front = d.getFrontFromPlace(mods.eln.misc.Direction.YN, player);
            throw new IllegalStateException("placing " + d.name + " at " + p + " failed: here " + level.getBlockState(p)
                + ", checkCanPlace " + d.checkCanPlace(new mods.eln.misc.Coordonate(p, level), front));
        }
        return node.element;
    }

    /** Bytes for an element's networkUnserialize (the GUI packet path). */
    default java.io.DataInputStream stream(Writer writer) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            writer.write(new java.io.DataOutputStream(bos));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return new java.io.DataInputStream(new java.io.ByteArrayInputStream(bos.toByteArray()));
    }

    interface Writer {
        void write(java.io.DataOutputStream out) throws java.io.IOException;
    }

    void line(String text);

    void check(String what, boolean ok, String detail);

    /** PASS when |measured - expected| <= 1% of |expected|. */
    void checkValue(String what, double measured, double expected);
}
