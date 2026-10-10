package mods.eln.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** {@link TestContext} on a GameTestHelper: checks are collected and the test fails at the end if any failed. */
public final class GameTestContext implements TestContext {
    static final double TOLERANCE = 0.01;

    private final GameTestHelper helper;
    private final String id;
    final List<String> failures = new ArrayList<>();
    int passes;

    public GameTestContext(GameTestHelper helper, String id) {
        this.helper = helper;
        this.id = id;
    }

    @Override
    public ServerLevel level() {
        return helper.getLevel();
    }

    @Override
    public BlockPos at(int dx, int dy, int dz) {
        return helper.absolutePos(new BlockPos(dx, dy, dz));
    }

    @Override
    public void floor(BlockPos p) {
        helper.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
    }

    @Override
    public void line(String text) {
        mods.eln.Eln.LOG.info("[{}] {}", id, text);
    }

    @Override
    public void check(String what, boolean ok, String detail) {
        String l = (ok ? "PASS " : "FAIL ") + what + (detail.isEmpty() ? "" : ": " + detail);
        line(l);
        if (ok) passes++;
        else failures.add(l);
    }

    @Override
    public void checkValue(String what, double measured, double expected) {
        double err = Math.abs(measured - expected) / Math.max(Math.abs(expected), 1e-9);
        check(what, !Double.isNaN(measured) && err <= TOLERANCE,
            String.format("measured %.4f, expected %.4f (%.2f%%)", measured, expected, err * 100));
    }

    /** End the test: succeed, or fail listing the failed checks. */
    void finish() {
        if (failures.isEmpty()) helper.succeed();
        else helper.fail(String.join("\n", failures));
    }

    @Override
    public mods.eln.node.six.SixNodeElement placeSix(int damage, BlockPos p, mods.eln.misc.Direction side) {
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

    @Override
    public java.io.DataInputStream stream(Writer writer) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            writer.write(new java.io.DataOutputStream(bos));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return new java.io.DataInputStream(new java.io.ByteArrayInputStream(bos.toByteArray()));
    }

    @Override
    public mods.eln.node.transparent.TransparentNodeElement placeTransparent(int damage, BlockPos p) {
        mods.eln.node.transparent.TransparentNodeDescriptor d = mods.eln.Eln.transparentNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no transparent node descriptor " + damage);
        net.minecraft.server.level.ServerLevel level = level();
        net.minecraft.world.entity.player.Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        mods.eln.node.transparent.TransparentNode node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem)
            .placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
        if (node == null) throw new IllegalStateException("placing " + d.name + " at " + p + " failed");
        return node.element;
    }
}
