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

    /** A fake player that records its chat and action-bar messages (where a device's message goes). */
    default ListeningPlayer listener() {
        return new ListeningPlayer(level());
    }

    /** The level's shared fake player (the same one each call: clear what you put in its hand). */
    default net.neoforged.neoforge.common.util.FakePlayer player() {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level());
    }

    /** Runs r with held in the fake player's main hand, then empties the hand (also when r throws). */
    default <T> T holding(net.minecraft.world.item.ItemStack held,
                          java.util.function.Function<net.neoforged.neoforge.common.util.FakePlayer, T> r) {
        var p = player();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
        try {
            return r.apply(p);
        } finally {
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    /** The node at the absolute position p, as the node manager has it (null when there is none). */
    default mods.eln.node.NodeBase nodeAt(BlockPos p) {
        return mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level()));
    }

    /** The voxel node at p, as the node manager has it; null when there is none or it isn't a voxel node. */
    default mods.eln.node.voxel.VoxelNode voxelAt(BlockPos p) {
        return nodeAt(p) instanceof mods.eln.node.voxel.VoxelNode v ? v : null;
    }

    /**
     * The voxel node at p through its block entity, which caches the node it found (so after a reload it can differ
     * from {@link #voxelAt}); null when there is no voxel block entity or node.
     */
    default mods.eln.node.voxel.VoxelNode voxelByEntity(BlockPos p) {
        return level().getBlockEntity(p) instanceof mods.eln.node.voxel.VoxelNodeEntity e
            && e.getNode() instanceof mods.eln.node.voxel.VoxelNode v ? v : null;
    }

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
    /**
     * A transparent node placed at p as a player looking along yaw places it (degrees: 270 faces it west, XN; a multiblock
     * grows from there), or null if it doesn't fit.
     */
    default mods.eln.node.transparent.TransparentNodeElement placeByPlayer(int damage, BlockPos p, float yaw) {
        mods.eln.node.transparent.TransparentNodeDescriptor d = mods.eln.Eln.transparentNodeItem.getDescriptor(damage);
        net.minecraft.server.level.ServerLevel level = level();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setYRot(yaw);
        try {
            var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
            return node == null ? null : node.element;
        } finally {
            player.setYRot(0f);
        }
    }

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

    /** Set an Electrical (or Signal) Source to u volts through its GUI packet. */
    default void setVolts(mods.eln.node.six.SixNodeElement source, double u) {
        source.networkUnserialize(stream(out -> {
            out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) u);
        }));
    }

    /** Place an Electrical (or Signal) Source (six-node id damage) at p on its floor, at u volts. */
    default mods.eln.sixnode.electricalsource.ElectricalSourceElement source(int damage, BlockPos p, double u) {
        var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) placeSix(damage, p);
        setVolts(s, u);
        return s;
    }

    /** A field read by reflection, searched up the class chain (for state a device keeps private). */
    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(o);
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("no field " + name + " in " + o.getClass());
    }

    /** Runs r ticks from now, as the test's own measure runs (outside the simulator's tick: a node may be reloaded). */
    default void after(int ticks, Runnable r) {
        throw new UnsupportedOperationException("this context can't schedule (only game tests can)");
    }

    void line(String text);

    void check(String what, boolean ok, String detail);

    /** PASS when |measured - expected| <= 1% of |expected|. */
    void checkValue(String what, double measured, double expected);
}
