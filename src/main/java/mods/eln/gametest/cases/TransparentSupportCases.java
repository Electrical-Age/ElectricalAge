package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gametest.LabCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A transparent device whose support goes is broken like a block: it drops, with its contents (Baughn: it vanished,
 * because support loss went through the failure path, which drops nothing).
 */
public final class TransparentSupportCases {
    private TransparentSupportCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FloorRemoved());
        cases.add(new FullFaces());
    }

    static final class FloorRemoved implements TestCase {
        BlockPos p;
        boolean gone, nodeGone;
        int devices, eggs;

        public String name() {
            return "transparent device drops when its floor goes";
        }

        public void build(TestContext ctx) {
            var level = ctx.level();
            BlockPos floor = ctx.at(1, 1, 1);
            p = floor.above();
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            var incubator = ctx.placeTransparent(LabCommands.transparentId("50V Egg Incubator"), p);
            incubator.getInventory().setItem(0, new ItemStack(Items.EGG, 3));
            ItemStack item = incubator.transparentNodeDescriptor.newItemStack();
            level.setBlockAndUpdate(floor, Blocks.AIR.defaultBlockState());
            gone = level.getBlockState(p).isAir();
            nodeGone = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level)) == null;
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2))) {
                if (e.getItem().is(Items.EGG)) eggs += e.getItem().getCount();
                else if (ItemStack.isSameItemSameComponents(e.getItem(), item)) devices += e.getItem().getCount();
            }
        }

        public int ticks() {
            return 2;
        }

        public void measure(TestContext ctx) {
            ctx.check("floor gone: the device is gone", gone, "");
            ctx.check("floor gone: its node is gone too", nodeGone && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(
                new mods.eln.misc.Coordonate(p, ctx.level())) == null, "");
            ctx.check("floor gone: the device drops", devices == 1, devices + " devices");
            ctx.check("floor gone: its eggs drop", eggs == 3, eggs + " eggs");
        }
    }

    /** Any full face holds a device (Baughn): a top slab, glass, a stair's back; a bottom slab's top or a fence doesn't. */
    static final class FullFaces implements TestCase {
        String place = "", six = "";
        boolean stays;

        public String name() {
            return "devices stand on any full face";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public void build(TestContext ctx) {
            var level = ctx.level();
            var d = mods.eln.Eln.transparentNodeItem.getDescriptor(LabCommands.transparentId("50V Egg Incubator"));
            var slab = Blocks.STONE_SLAB.defaultBlockState();
            var top = slab.setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP);
            var stair = Blocks.STONE_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.EAST);
            Object[][] floors = {{"top slab", top}, {"glass", Blocks.GLASS.defaultBlockState()}, {"stone", Blocks.STONE.defaultBlockState()},
                {"bottom slab", slab}, {"fence", Blocks.OAK_FENCE.defaultBlockState()}};
            int x = 1;
            for (Object[] f : floors) {
                BlockPos below = ctx.at(x, 1, 1);
                level.setBlockAndUpdate(below, (net.minecraft.world.level.block.state.BlockState) f[1]);
                String r = d.checkCanPlace(new mods.eln.misc.Coordonate(below.above(), level), mods.eln.misc.Direction.XN);
                place += f[0] + (r == null ? " yes; " : " no; ");
                x += 2;
            }
            // placed on a top slab, it stays when a neighbour changes
            BlockPos below = ctx.at(1, 1, 3);
            level.setBlockAndUpdate(below, top);
            ctx.placeTransparent(LabCommands.transparentId("50V Egg Incubator"), below.above());
            level.setBlockAndUpdate(below.above().east(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(below.above().east(), Blocks.AIR.defaultBlockState());
            stays = !level.getBlockState(below.above()).isAir();
            // a stair facing east: its full back is its east face, its west face is the step
            BlockPos s = ctx.at(5, 2, 3);
            level.setBlockAndUpdate(s, stair);
            var sb = mods.eln.node.six.SixNodeBlock.get();
            six = "back " + sb.getIfOtherBlockIsSolid(level, s.east(), mods.eln.misc.Direction.XN)
                + ", step side " + sb.getIfOtherBlockIsSolid(level, s.west(), mods.eln.misc.Direction.XP)
                + ", under " + sb.getIfOtherBlockIsSolid(level, s.below(), mods.eln.misc.Direction.YP);
        }

        public int ticks() {
            return 2;
        }

        public void measure(TestContext ctx) {
            ctx.check("placement: full faces hold, partial ones don't",
                place.equals("top slab yes; glass yes; stone yes; bottom slab no; fence no; "), place);
            ctx.check("on a top slab: stays on a neighbour update", stays, "");
            ctx.check("six node: a stair's back and underside hold", six.equals("back true, step side false, under true"), six);
        }
    }
}
