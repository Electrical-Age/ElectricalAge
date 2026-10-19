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
        cases.add(new BreakKeptOnlyVoxel());
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

    /**
     * A voxel block whose only content is a face this version could not load (kept as saved data) can still be broken
     * by a player: nothing in it can be aimed at, and it used to refuse every break.
     */
    static final class BreakKeptOnlyVoxel implements TestCase {
        BlockPos p;
        boolean destroyed, air, nodeGone;

        public String name() {
            return "voxel block with only a kept face can be broken";
        }

        public void build(TestContext ctx) {
            p = ctx.at(1, 1, 1);
            var level = ctx.level();
            int dim = mods.eln.misc.Dimensions.id(ctx.world());
            net.minecraft.nbt.CompoundTag pos = new net.minecraft.nbt.CompoundTag();
            pos.putInt("cx", p.getX());
            pos.putInt("cy", p.getY());
            pos.putInt("cz", p.getZ());
            pos.putInt("cd", dim);
            net.minecraft.nbt.CompoundTag v = new net.minecraft.nbt.CompoundTag();
            v.putString("tag", mods.eln.node.voxel.VoxelNode.UUID);
            v.put("node", pos);
            for (int i = 0; i < 6; i++) v.putShort("EID" + i, (short) 0);
            v.putShort("EID" + Direction.YN.getInt(), (short) 32000);   // no such device
            v.put("ED" + Direction.YN.getInt(), new net.minecraft.nbt.CompoundTag());
            net.minecraft.nbt.CompoundTag file = new net.minecraft.nbt.CompoundTag();
            file.put("n0", v);
            mods.eln.node.NodeManager.instance.loadFromNbt(file, dim);
            if (!(mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level)) instanceof mods.eln.node.voxel.VoxelNode n)
                || !n.isSideKept(Direction.YN.getInt()))
                throw new IllegalStateException("the voxel tag with a kept face did not load at " + p);
            level.setBlock(p, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState(), 3);
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            destroyed = level.getBlockState(p).onDestroyedByPlayer(level, p, player, ItemStack.EMPTY, true, level.getFluidState(p));
            air = level.getBlockState(p).isAir();
            nodeGone = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level)) == null;
        }

        public int ticks() {
            return 2;
        }

        public void measure(TestContext ctx) {
            ctx.check("kept-only voxel block: the player's break goes through", destroyed && air, "destroyed " + destroyed + ", air " + air);
            ctx.check("kept-only voxel block: its node is gone", nodeGone, "node gone " + nodeGone);
        }
    }
}
