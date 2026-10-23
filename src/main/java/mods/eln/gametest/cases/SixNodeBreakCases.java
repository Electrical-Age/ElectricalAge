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
        cases.add(new OrphanBlock());
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

    /**
     * A six-node block whose node this version could not load (an orphan, kept as saved data): placing a device on it
     * is refused and leaves the block and its data; removing the block for real drops the data. Placing used to remove
     * the block and leave the data behind, to come back in whatever was built there next.
     */
    static final class OrphanBlock implements TestCase {
        BlockPos p;
        String result = "";

        public String name() {
            return "orphan six-node block: kept on placement, dropped with the block";
        }

        public void build(TestContext ctx) {
            p = ctx.at(1, 1, 1);
            var level = ctx.level();
            ctx.level().setBlockAndUpdate(ctx.at(0, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            int dim = mods.eln.misc.Dimensions.id(ctx.world());
            net.minecraft.nbt.CompoundTag pos = new net.minecraft.nbt.CompoundTag();
            pos.putInt("cx", p.getX());
            pos.putInt("cy", p.getY());
            pos.putInt("cz", p.getZ());
            pos.putInt("cd", dim);
            net.minecraft.nbt.CompoundTag o = new net.minecraft.nbt.CompoundTag();
            o.putString("tag", "eln:test_no_such_node_type");
            o.put("node", pos);
            net.minecraft.nbt.CompoundTag file = new net.minecraft.nbt.CompoundTag();
            file.put("n0", o);
            var nm = mods.eln.node.NodeManager.instance;
            var at = new mods.eln.misc.Coordonate(p, level);
            nm.loadFromNbt(file, dim);
            level.setBlock(p, mods.eln.node.six.SixNodeBlock.get().defaultBlockState(), 3);
            boolean kept = nm.hasOrphanAt(at);
            ItemStack cable = mods.eln.Eln.sixNodeItem.getDescriptor(Ids.six("Low Voltage Cable")).newItemStack();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            boolean placed = ((mods.eln.node.six.SixNodeItem) cable.getItem()).placeAt(cable, player, level, p, Direction.XN);
            boolean blockStays = level.getBlockState(p).is(mods.eln.node.six.SixNodeBlock.get());
            boolean keptAfter = nm.hasOrphanAt(at);
            level.removeBlock(p, false);
            boolean dropped = !nm.hasOrphanAt(at);
            result = "kept " + kept + ", placed " + placed + ", block stays " + blockStays + ", kept after " + keptAfter + ", dropped " + dropped;
        }

        public int ticks() {
            return 1;
        }

        public void measure(TestContext ctx) {
            ctx.check("orphan block: placement refused, block and data kept; removal drops the data",
                result.equals("kept true, placed false, block stays true, kept after true, dropped true"), result);
        }
    }
}
