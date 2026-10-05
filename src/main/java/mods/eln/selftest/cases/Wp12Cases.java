package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.Other;
import mods.eln.compat.GameRegistryCompat;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.item.electricalitem.ElectricalTool;
import mods.eln.item.electricalitem.TreeCapitation;
import mods.eln.misc.Utils;
import mods.eln.registry.batch.Wp12Content;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Selftest cases of device batch Wp12 (registry/batch/Wp12Content): items, FE, ore, tree capitation, replicator. */
public final class Wp12Cases {
    private Wp12Cases() {
    }

    // sub-UIDs (id << 6) + subId, as registered in Wp12Content
    static final int PORTABLE_BATTERY = (122 << 6) + 0; // 40000 J, charge 500 W, discharge 100 W
    static final int MINING_DRILL = (121 << 6) + 8;     // ElectricalPickaxe 120000 J, charge 2400 W
    static final int ELECTRICAL_AXE = (121 << 6) + 12;  // ElectricalAxe 40000 J, 200 J per block

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new Registration());
        cases.add(new ForgeEnergy());
        cases.add(new OreVein());
        cases.add(new TreeCapitationCase());
        cases.add(new Replicator());
    }

    /** Ids, names, oredict entries and registry names of the batch (no world needed). */
    static final class Registration implements SelfTestCase {
        @Override
        public String name() {
            return "wp12 registration";
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public void build(SelfTestContext ctx) {
        }

        private static void stack(SelfTestContext ctx, String name, Item item, int damage) {
            ItemStack s = GameRegistryCompat.findItemStack("Eln", name, 1);
            ctx.check("wp12 item " + name, !s.isEmpty() && s.getItem() == item && s.getItemDamage() == damage,
                s.isEmpty() ? "not found" : s.getItem().getRegistryName() + "@" + s.getItemDamage() + ", expected @" + damage);
        }

        private static boolean oreHas(String ore, ItemStack s) {
            for (ItemStack o : OreDictionary.getOres(ore)) if (OreDictionary.itemMatches(o, s, false)) return true;
            return false;
        }

        @Override
        public void measure(SelfTestContext ctx) {
            stack(ctx, "Copper Ingot", Eln.sharedItem, 1 + (8 << 6));
            stack(ctx, "Iron Dust", Eln.sharedItem, 2 + (9 << 6));
            stack(ctx, "Lead Dust", Eln.sharedItem, 5); // 1.7.10 registerDust used the bare id for these (not id << 6)
            stack(ctx, "Portable Battery", Eln.sharedItemStackOne, PORTABLE_BATTERY);
            stack(ctx, "Portable Electrical Axe", Eln.sharedItemStackOne, ELECTRICAL_AXE);
            stack(ctx, "Copper Ore", Wp12Content.oreItem, 1);
            stack(ctx, "Cinnabar Ore", Wp12Content.oreItem, 6);
            ctx.check("wp12 oredict oreCopper has EA copper ore", oreHas("oreCopper", new ItemStack(Wp12Content.oreItem, 1, 1)), "");
            ctx.check("wp12 oredict ingotCopper has EA copper ingot", oreHas("ingotCopper", GameRegistryCompat.findItemStack("Eln", "Copper Ingot", 1)), "");
            String[] names = {"copper_sword", "copper_axe", "copper_helmet", "e_coal_helmet", "e_coal_boots", "ore"};
            List<String> missing = new ArrayList<>();
            for (String n : names) if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("eln", n))) missing.add(n);
            ctx.check("wp12 item registry names", missing.isEmpty(), missing.isEmpty() ? "eln:copper_sword ... eln:ore" : "missing " + missing);
            int oreId = net.minecraft.block.Block.getIdFromBlock(Wp12Content.oreBlock);
            // X-ray scanner keys are block id + meta << 12 in a 65536 table (1.7.10 design): ids >= 4096 are not scanned
            ctx.line((oreId < 4096 ? "INFO" : "WARN") + " wp12 ore block id " + oreId + " (X-ray scanner sees EA ores only if < 4096)");
            ResourceLocation rep = EntityList.getKey(ReplicatorEntity.class);
            ctx.check("wp12 replicator entity id", rep != null && rep.toString().equals("eln:replicator"), String.valueOf(rep));
        }
    }

    /**
     * Forge Energy on items. Expected (ratio r = balancing.ElnToThermalExpansionConversionRatio, default 4/3 FE per J;
     * one call = at most one tick, 0.05 s, of the item's charge/discharge power):
     * battery receive(1e6) = floor(500 W * 0.05 s * r) FE (33 at r = 4/3), stored J = that / r; simulate changes nothing;
     * extract(1e6) = floor(100 W * 0.05 s * r) FE (6); capacity = floor(40000 J * r) (53333);
     * mining drill: receive = floor(2400 * 0.05 * r) (160), canExtract false, extract 0.
     */
    static final class ForgeEnergy implements SelfTestCase {
        @Override
        public String name() {
            return "wp12 forge energy";
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public void build(SelfTestContext ctx) {
        }

        @Override
        public void measure(SelfTestContext ctx) {
            double r = Other.getElnToTeConversionRatio();
            ItemStack bat = Eln.sharedItemStackOne.getDescriptor(PORTABLE_BATTERY).newItemStack();
            IItemEnergyBattery b = (IItemEnergyBattery) Utils.getItemObject(bat);
            IEnergyStorage fe = bat.getCapability(CapabilityEnergy.ENERGY, null);
            ctx.check("wp12 FE capability on Portable Battery", fe != null, "");
            if (fe == null) return;
            ctx.checkValue("wp12 FE battery capacity (FE)", fe.getMaxEnergyStored(), Math.floor(40000 * r));
            int sim = fe.receiveEnergy(1000000, true);
            ctx.check("wp12 FE simulate leaves energy", b.getEnergy(bat) == 0 && sim == (int) Math.floor(500 * 0.05 * r),
                "simulated " + sim + " FE, stored " + b.getEnergy(bat) + " J");
            int in = fe.receiveEnergy(1000000, false);
            ctx.checkValue("wp12 FE battery receive per call (FE)", in, Math.floor(500 * 0.05 * r));
            ctx.checkValue("wp12 FE battery stored after receive (J)", b.getEnergy(bat), in / r);
            double before = b.getEnergy(bat);
            int out = fe.extractEnergy(1000000, false);
            ctx.checkValue("wp12 FE battery extract per call (FE)", out, Math.floor(100 * 0.05 * r));
            ctx.checkValue("wp12 FE battery stored after extract (J)", b.getEnergy(bat), before - out / r);

            ItemStack drill = Eln.sharedItemStackOne.getDescriptor(MINING_DRILL).newItemStack();
            IEnergyStorage dfe = drill.getCapability(CapabilityEnergy.ENERGY, null);
            ctx.check("wp12 FE capability on mining drill", dfe != null, "");
            if (dfe == null) return;
            int din = dfe.receiveEnergy(1000000, false);
            ctx.checkValue("wp12 FE drill receive per call (FE)", din, Math.floor(2400 * 0.05 * r));
            ctx.check("wp12 FE drill is receive-only", !dfe.canExtract() && dfe.extractEnergy(1000, false) == 0, "");
            ItemStack ingot = GameRegistryCompat.findItemStack("Eln", "Copper Ingot", 1);
            ctx.check("wp12 no FE on plain items", ingot.getCapability(CapabilityEnergy.ENERGY, null) == null, "Copper Ingot");
        }
    }

    /**
     * Ore worldgen: fills x 0..7, y 1..3, z 0..2 of the row with stone, runs Copper Ore's vein generator (the code path of
     * OreDescriptor.generateSurface, size = spawnSizeMax 10, seeded) centred on x 4, z 1. Expected: copper ore blocks
     * (meta 1) inside the stone only (> 0 inside, 0 in the air around: the target predicate is stone), and a copper
     * ore block drops one Copper Ore item (damage 1).
     */
    static final class OreVein implements SelfTestCase {
        @Override
        public String name() {
            return "wp12 ore vein";
        }

        @Override
        public void build(SelfTestContext ctx) {
            WorldServer w = ctx.world();
            for (BlockPos p : BlockPos.getAllInBox(ctx.at(0, 1, 0), ctx.at(7, 3, 2)))
                if (!w.isAirBlock(p)) throw new IllegalStateException("not empty: " + p);
            for (BlockPos p : BlockPos.getAllInBox(ctx.at(0, 1, 0), ctx.at(7, 3, 2)))
                w.setBlockState(p, Blocks.STONE.getDefaultState(), 2);
            BlockPos c = ctx.at(4, 2, 1);
            // WorldGenMinable centres the vein on pos + (8, 0..-2, 8)
            Wp12Content.oreCopper.generateVein(w, new Random(1234567), c.getX() - 8, c.getY() + 1, c.getZ() - 8, 10);
        }

        @Override
        public void measure(SelfTestContext ctx) {
            WorldServer w = ctx.world();
            int inside = 0, outside = 0, wrongMeta = 0;
            IBlockState sample = null;
            BlockPos samplePos = null;
            try {
                for (BlockPos p : BlockPos.getAllInBox(ctx.at(-2, 0, -2), ctx.at(9, 5, 4))) {
                    IBlockState s = w.getBlockState(p);
                    if (s.getBlock() != Wp12Content.oreBlock) continue;
                    boolean in = p.getX() >= ctx.at(0, 0, 0).getX() && p.getX() <= ctx.at(7, 0, 0).getX()
                        && p.getY() >= ctx.at(0, 0, 0).getY() && p.getY() <= ctx.at(0, 3, 0).getY()
                        && p.getZ() >= ctx.at(0, 0, 0).getZ() && p.getZ() <= ctx.at(0, 0, 2).getZ();
                    if (in) inside++;
                    else outside++;
                    if (Wp12Content.oreBlock.getMetaFromState(s) != 1) wrongMeta++;
                    if (sample == null) {
                        sample = s;
                        samplePos = p.toImmutable();
                    }
                }
                ctx.check("wp12 ore vein in stone", inside > 0 && outside == 0 && wrongMeta == 0,
                    inside + " copper ore block(s) in the stone (expected > 0), " + outside + " outside (expected 0), "
                        + wrongMeta + " with meta != 1");
                if (sample != null) {
                    NonNullList<ItemStack> drops = NonNullList.create();
                    Wp12Content.oreBlock.getDrops(drops, w, samplePos, sample, 0);
                    ctx.check("wp12 copper ore drop", drops.size() == 1 && drops.get(0).getItem() == Wp12Content.oreItem
                        && drops.get(0).getItemDamage() == 1, drops.toString() + " (expected 1x ore@1)");
                }
            } finally {
                for (BlockPos p : BlockPos.getAllInBox(ctx.at(0, 1, 0), ctx.at(7, 3, 2))) w.setBlockToAir(p);
                // platform (y 0) is removed by the framework
            }
        }
    }

    /**
     * Tree capitation (Portable Electrical Axe): a 4-log column on the row, a fully charged axe (40000 J), a fake
     * survival player, TreeCapitation.addBlockSwapper at the bottom log (what breaking it with the axe does).
     * TreeCapitation runs from Wp12Content.serverTick. Expected: all 4 logs gone, axe energy 40000 - 4 * 200 J = 39200 J.
     */
    static final class TreeCapitationCase implements SelfTestCase {
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
        public void build(SelfTestContext ctx) {
            WorldServer w = ctx.world();
            for (int y = 1; y <= 4; y++) {
                if (!w.isAirBlock(ctx.at(1, y, 1))) throw new IllegalStateException("not empty: " + ctx.at(1, y, 1));
            }
            for (int y = 1; y <= 4; y++) w.setBlockState(ctx.at(1, y, 1), Blocks.LOG.getDefaultState(), 3);
            axe = Eln.sharedItemStackOne.getDescriptor(ELECTRICAL_AXE).newItemStack();
            ElectricalTool tool = (ElectricalTool) Utils.getItemObject(axe);
            tool.setEnergy(axe, ENERGY);
            TreeCapitation.INSTANCE.addBlockSwapper(w, FakePlayerFactory.getMinecraft(w), tool, ctx.at(1, 1, 1), true, axe);
        }

        @Override
        public void measure(SelfTestContext ctx) {
            WorldServer w = ctx.world();
            int logs = 0;
            for (int y = 1; y <= 4; y++) if (w.getBlockState(ctx.at(1, y, 1)).getBlock() == Blocks.LOG) logs++;
            ctx.check("wp12 tree capitation cut the logs", logs == 0, logs + " of 4 logs left (expected 0)");
            ElectricalTool tool = (ElectricalTool) Utils.getItemObject(axe);
            ctx.checkValue("wp12 tree capitation axe energy used (J)", ENERGY - tool.getEnergy(axe), 4 * 200);
            for (int y = 1; y <= 4; y++) if (w.getBlockState(ctx.at(1, y, 1)).getBlock() == Blocks.LOG) w.setBlockToAir(ctx.at(1, y, 1));
            // log drops (item entities) are removed by the framework cleanup
        }
    }

    /**
     * Replicator entity: spawned on its own 3x3 stone pad 24 blocks west of the row (out of its cable-seeking range of
     * 15 blocks, so it cannot load the other cases' cables). Expected after the ticks: still alive (persistent), max
     * health 8, attack damage 3, follow range 8 (1.7.10 attributes). Peaceful difficulty despawns mobs: skipped then.
     */
    static final class Replicator implements SelfTestCase {
        private ReplicatorEntity entity;
        private final List<BlockPos> pad = new ArrayList<>();

        @Override
        public String name() {
            return "wp12 replicator";
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public void build(SelfTestContext ctx) {
            WorldServer w = ctx.world();
            if (w.getDifficulty() == EnumDifficulty.PEACEFUL) return;
            for (BlockPos p : BlockPos.getAllInBox(ctx.at(-25, 0, 0), ctx.at(-23, 3, 2)))
                if (!w.isAirBlock(p)) throw new IllegalStateException("not empty: " + p);
            for (BlockPos p : BlockPos.getAllInBox(ctx.at(-25, 0, 0), ctx.at(-23, 0, 2))) {
                w.setBlockState(p, Blocks.STONE.getDefaultState(), 2);
                pad.add(p.toImmutable());
            }
            entity = new ReplicatorEntity(w);
            BlockPos s = ctx.at(-24, 1, 1);
            entity.setLocationAndAngles(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 0f, 0f);
            w.spawnEntity(entity);
        }

        @Override
        public void measure(SelfTestContext ctx) {
            WorldServer w = ctx.world();
            if (w.getDifficulty() == EnumDifficulty.PEACEFUL) {
                ctx.line("SKIP wp12 replicator: peaceful difficulty");
                return;
            }
            try {
                ctx.check("wp12 replicator alive", entity != null && entity.isEntityAlive() && w.loadedEntityList.contains(entity),
                    entity == null ? "not spawned" : "at " + entity.getPosition());
                if (entity != null) {
                    ctx.checkValue("wp12 replicator max health", entity.getMaxHealth(), 8);
                    ctx.checkValue("wp12 replicator attack damage", entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue(), 3);
                    ctx.checkValue("wp12 replicator follow range", entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue(), 8);
                }
            } finally {
                if (entity != null) entity.setDead();
                for (BlockPos p : pad) w.setBlockToAir(p);
            }
        }
    }
}
