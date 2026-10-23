package mods.eln.gametest;

import mods.eln.Eln;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * {@link TestContext} on a live world at an origin, for the lab commands (gallery, stress scenes; P3 R2): checks are
 * logged and counted, nothing fails.
 */
public final class WorldTestContext implements TestContext {
    final ServerLevel level;
    final BlockPos origin;
    public int passes, failures;

    public WorldTestContext(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public BlockPos at(int dx, int dy, int dz) {
        return origin.offset(dx, dy, dz);
    }

    @Override
    public void floor(BlockPos p) {
        level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
    }

    @Override
    public void line(String text) {
        Eln.LOG.info("[lab] {}", text);
    }

    @Override
    public void check(String what, boolean ok, String detail) {
        if (ok) passes++;
        else failures++;
        line((ok ? "PASS " : "FAIL ") + what + (detail.isEmpty() ? "" : ": " + detail));
    }

    @Override
    public void checkValue(String what, double measured, double expected) {
        check(what, Math.abs(measured - expected) <= 0.01 * Math.max(Math.abs(expected), 1e-9), measured + " vs " + expected);
    }
}
