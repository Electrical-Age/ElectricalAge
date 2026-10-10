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
 * Still in legacy/ (with their Wp12 steps): Registration (ore dictionary, ore item, replicator id), OreVein, Replicator.
 */
public final class Wp12Cases {
    private Wp12Cases() {
    }

    static final int ELECTRICAL_AXE = 12 + (121 << 6);
    static final int MINING_DRILL = 8 + (121 << 6);
    static final int PORTABLE_BATTERY = 0 + (122 << 6); // 40000 J, charge 500 W, discharge 100 W

    public static void addTo(List<TestCase> cases) {
        cases.add(new TreeCapitationCase());
        cases.add(new ForgeEnergy());
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

    /**
     * Forge Energy on items (Capabilities.Energy.ITEM over an ItemAccess for the stack). Expected (ratio r =
     * balancing.ElnToThermalExpansionConversionRatio, default 4/3 FE per J; one call = at most one tick, 0.05 s, of the
     * item's charge/discharge power): battery insert(1e6) = floor(500 W * 0.05 s * r) FE (33), stored J = that / r;
     * an aborted transaction changes nothing; extract(1e6) = floor(100 W * 0.05 s * r) FE (6); capacity =
     * floor(40000 J * r) (53333); mining drill: insert = floor(2400 * 0.05 * r) (160), extract 0; no FE on plain items.
     */
    static final class ForgeEnergy implements TestCase {
        @Override
        public String name() {
            return "wp12 forge energy";
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public void build(TestContext ctx) {
        }

        static net.neoforged.neoforge.transfer.energy.EnergyHandler fe(ItemStack[] holder) {
            return net.neoforged.neoforge.transfer.access.ItemAccess.forStack(holder[0]).getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM);
        }

        @Override
        public void measure(TestContext ctx) {
            double r = Eln.ElnToTeConversionRatio;
            ItemStack bat = Eln.sharedItemStackOne.getDescriptor(PORTABLE_BATTERY).newItemStack();
            mods.eln.item.electricalinterface.IItemEnergyBattery b = (mods.eln.item.electricalinterface.IItemEnergyBattery) Utils.getItemObject(bat);
            var fe = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(bat).getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM);
            ctx.check("wp12 FE capability on Portable Battery", fe != null, "");
            if (fe == null) return;
            ctx.checkValue("wp12 FE battery capacity (FE)", fe.getCapacityAsLong(), Math.floor(40000 * r));
            int sim;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                sim = fe.insert(1000000, tx);
            }
            ctx.check("wp12 FE aborted insert leaves energy", b.getEnergy(bat) == 0 && sim == (int) Math.floor(500 * 0.05 * r),
                "inserted " + sim + " FE (aborted), stored " + b.getEnergy(bat) + " J");
            int in;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                in = fe.insert(1000000, tx);
                tx.commit();
            }
            ctx.checkValue("wp12 FE battery receive per call (FE)", in, Math.floor(500 * 0.05 * r));
            ctx.checkValue("wp12 FE battery stored after receive (J)", b.getEnergy(bat), in / r);
            double before = b.getEnergy(bat);
            int out;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                out = fe.extract(1000000, tx);
                tx.commit();
            }
            ctx.checkValue("wp12 FE battery extract per call (FE)", out, Math.floor(100 * 0.05 * r));
            ctx.checkValue("wp12 FE battery stored after extract (J)", b.getEnergy(bat), before - out / r);

            ItemStack drill = Eln.sharedItemStackOne.getDescriptor(MINING_DRILL).newItemStack();
            var dfe = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(drill).getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM);
            ctx.check("wp12 FE capability on mining drill", dfe != null, "");
            if (dfe == null) return;
            int din, dout;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                din = dfe.insert(1000000, tx);
                dout = dfe.extract(1000, tx);
                tx.commit();
            }
            ctx.checkValue("wp12 FE drill receive per call (FE)", din, Math.floor(2400 * 0.05 * r));
            ctx.check("wp12 FE drill is receive-only", dout == 0, dout + " FE extracted");
            ItemStack ingot = Eln.findItemStack("Copper Ingot", 1);
            ctx.check("wp12 no FE on plain items", net.neoforged.neoforge.transfer.access.ItemAccess.forStack(ingot)
                .getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM) == null, "Copper Ingot");
        }
    }
}
