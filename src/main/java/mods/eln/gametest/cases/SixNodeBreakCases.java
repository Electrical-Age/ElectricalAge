package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Breaking a six-node block takes one element at a time and drops it (1.12 removedByPlayer; the port had lost it: a
 * pickaxe removed every face and dropped nothing). Whatever is left drops when the block goes another way.
 */
public final class SixNodeBreakCases {
    private SixNodeBreakCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new BreakOneFace());
    }

    static final class BreakOneFace implements TestCase {
        BlockPos p;
        int afterPick, afterRemove;
        boolean stayed;

        public String name() {
            return "six node breaks one face at a time";
        }

        public void build(TestContext ctx) {
            p = ctx.at(1, 1, 1);
            ctx.level().setBlockAndUpdate(ctx.at(0, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            int cable = Ids.six("Low Voltage Cable");
            ctx.placeSix(cable, p, Direction.YN);
            ctx.placeSix(cable, p, Direction.XN);
            var level = ctx.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var state = level.getBlockState(p);
            boolean destroyed = state.onDestroyedByPlayer(level, p, player, ItemStack.EMPTY, true, level.getFluidState(p));
            stayed = !destroyed && level.getBlockState(p).is(mods.eln.node.six.SixNodeBlock.get());
            afterPick = drops(ctx);
            level.removeBlock(p, false);
            afterRemove = drops(ctx);
        }

        int drops(TestContext ctx) {
            return ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2)).stream().mapToInt(e -> e.getItem().getCount()).sum();
        }

        public int ticks() {
            return 2;
        }

        public void measure(TestContext ctx) {
            ctx.check("breaking takes one face; the block stays for the other", stayed, "stayed " + stayed);
            ctx.check("that face drops its item", afterPick == 1, afterPick + " items");
            ctx.check("the rest drops when the block goes another way", afterRemove == 2, afterRemove + " items");
        }
    }
}
