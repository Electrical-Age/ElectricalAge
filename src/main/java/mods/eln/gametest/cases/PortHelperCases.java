package mods.eln.gametest.cases;

import mods.eln.compat.BlockMeta;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Checks of the 26.x replacements for 1.12 world APIs that no device case exercises directly (26.x port, no 1.12
 * counterpart): fuel burn times from the COOKING_FUEL component, smelting through the recipe manager, and the eln:light
 * block (META level, timeouts, self-removal, cave air).
 */
public final class PortHelperCases {
    private PortHelperCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FuelSmeltCase());
        cases.add(new LightCase());
    }

    /** 1.12 burn times: coal 1600 ticks (80 kJ), lava bucket 20000; cobblestone smelts to stone, dirt to nothing. */
    static final class FuelSmeltCase implements TestCase {
        public String name() {
            return "port26 fuel and smelting";
        }

        public int width() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            int coal = Utils.burnTime(new ItemStack(Items.COAL));
            int lava = Utils.burnTime(new ItemStack(Items.LAVA_BUCKET));
            int dirt = Utils.burnTime(new ItemStack(Items.DIRT));
            ctx.check("burn time coal = 1600, lava bucket = 20000, dirt = 0", coal == 1600 && lava == 20000 && dirt == 0,
                "coal " + coal + ", lava " + lava + ", dirt " + dirt);
            ctx.checkValue("fuel energy of coal [J] (80 kJ)", Utils.getItemEnergie(new ItemStack(Items.COAL)), 80000.0);
            ItemStack stone = Utils.smeltResult(ctx.world(), new ItemStack(Items.COBBLESTONE));
            ItemStack none = Utils.smeltResult(ctx.world(), new ItemStack(Items.DIRT));
            ctx.check("smelting: cobblestone -> stone, dirt -> nothing", stone.getItem() == Items.STONE && none.isEmpty(),
                "cobblestone -> " + stone + ", dirt -> " + none);
        }
    }

    /**
     * Three lights at build: level 10 for 1000 ticks in air, level 7 for 5 ticks in air, level 12 for 1000 ticks in
     * cave air. After the run: the first is an eln:light with META 10 and block light 10, the second removed itself,
     * the third lit the cave air (1.12 only knew Blocks.AIR).
     */
    static final class LightCase implements TestCase {
        BlockPos lasting, shortLived, cave;

        public String name() {
            return "port26 light block";
        }

        public int width() {
            return 5;
        }

        public void build(TestContext ctx) {
            lasting = ctx.at(0, 2, 1);
            shortLived = ctx.at(2, 2, 1);
            cave = ctx.at(4, 2, 1);
            ctx.world().setBlock(cave, Blocks.CAVE_AIR.defaultBlockState(), 3);
            LightBlockEntity.addLight(new Coordonate(lasting, ctx.world()), 10, 1000);
            LightBlockEntity.addLight(new Coordonate(shortLived, ctx.world()), 7, 5);
            LightBlockEntity.addLight(new Coordonate(cave, ctx.world()), 12, 1000);
        }

        public void measure(TestContext ctx) {
            BlockState a = ctx.world().getBlockState(lasting);
            int meta = a.hasProperty(BlockMeta.META) ? a.getValue(BlockMeta.META) : -1;
            int light = ctx.world().getBrightness(LightLayer.BLOCK, lasting);
            ctx.check("light block: eln:light, META 10, block light 10", a.getBlock() == ElnDeviceRegistry.lightBlock && meta == 10 && light == 10,
                a + ", block light " + light);
            BlockState b = ctx.world().getBlockState(shortLived);
            ctx.check("light block: removed itself after its timeout", b.isAir(), String.valueOf(b));
            BlockState c = ctx.world().getBlockState(cave);
            ctx.check("light block: placed in cave air too", c.getBlock() == ElnDeviceRegistry.lightBlock, String.valueOf(c));
            // leave the box clean
            ctx.world().removeBlock(lasting, false);
            ctx.world().removeBlock(cave, false);
        }
    }
}
