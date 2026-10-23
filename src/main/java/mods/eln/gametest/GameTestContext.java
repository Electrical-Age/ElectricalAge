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



}
