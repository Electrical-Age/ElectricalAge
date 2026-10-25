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
    }

    static final class FloorRemoved implements TestCase {
        BlockPos p;
        boolean gone;
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
            ctx.check("floor gone: the device drops", devices == 1, devices + " devices");
            ctx.check("floor gone: its eggs drop", eggs == 3, eggs + " eggs");
        }
    }
}
