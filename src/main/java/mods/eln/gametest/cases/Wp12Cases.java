package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.electricalitem.ElectricalTool;
import mods.eln.item.electricalitem.TreeCapitation;
import mods.eln.misc.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Selftest cases of device batch Wp12 (1.12 Wp12Cases), as its parts are ported: Wp12b's electrical axe so far.
 * Still in legacy/ (with their Wp12 steps): Registration (ore dictionary, ore item, replicator id), ForgeEnergy on
 * items, OreVein, Replicator.
 */
public final class Wp12Cases {
    private Wp12Cases() {
    }

    static final int ELECTRICAL_AXE = 12 + (121 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new TreeCapitationCase());
    }

    /**
     * Portable electrical axe with 40 kJ, tree capitation on a 4-log oak trunk from its bottom log (the block swapper
     * runs from the server tick). Expected: all 4 logs cut, 200 J per log.
     */
    static final class TreeCapitationCase implements TestCase {
        private ItemStack axe;
        private static final double ENERGY = 40000;

        @Override
        public String name() {
            return "wp12 tree capitation";
        }

        @Override
        public int width() {
            return 3;
        }

        @Override
        public int height() {
            return 6;
        }

        @Override
        public void build(TestContext ctx) {
            ServerLevel w = ctx.world();
            for (int y = 1; y <= 4; y++) {
                if (!w.getBlockState(ctx.at(1, y, 1)).isAir()) throw new IllegalStateException("not empty: " + ctx.at(1, y, 1));
            }
            for (int y = 1; y <= 4; y++) w.setBlock(ctx.at(1, y, 1), Blocks.OAK_LOG.defaultBlockState(), 3);
            axe = Eln.sharedItemStackOne.getDescriptor(ELECTRICAL_AXE).newItemStack();
            ElectricalTool tool = (ElectricalTool) Utils.getItemObject(axe);
            tool.setEnergy(axe, ENERGY);
            TreeCapitation.INSTANCE.addBlockSwapper(w, FakePlayerFactory.getMinecraft(w), tool, ctx.at(1, 1, 1), true, axe);
        }

        @Override
        public void measure(TestContext ctx) {
            ServerLevel w = ctx.world();
            int logs = 0;
            for (int y = 1; y <= 4; y++) if (w.getBlockState(ctx.at(1, y, 1)).getBlock() == Blocks.OAK_LOG) logs++;
            ctx.check("wp12 tree capitation cut the logs", logs == 0, logs + " of 4 logs left (expected 0)");
            ElectricalTool tool = (ElectricalTool) Utils.getItemObject(axe);
            ctx.checkValue("wp12 tree capitation axe energy used (J)", ENERGY - tool.getEnergy(axe), 4 * 200);
            for (int y = 1; y <= 4; y++) if (w.getBlockState(ctx.at(1, y, 1)).getBlock() == Blocks.OAK_LOG) w.removeBlock(ctx.at(1, y, 1), false);
        }
    }
}
