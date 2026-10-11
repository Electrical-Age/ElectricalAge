package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.electricalitem.ElectricalTool;
import mods.eln.item.electricalitem.TreeCapitation;
import mods.eln.misc.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Selftest cases of device batch Wp12 (1.12 Wp12Cases), as its parts are ported: Wp12b's electrical axe so far.
 * Not ported: 1.12 Registration (ore dictionary: item tags come with P5).
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
        cases.add(new OreVein());
        cases.add(new OreWorldgen());
        cases.add(new ECoalArmor());
        cases.add(new Replicator());
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
            ItemStack ingot = Eln.findItemStack("Lead Ingot", 1);
            ctx.check("wp12 no FE on plain items", net.neoforged.neoforge.transfer.access.ItemAccess.forStack(ingot)
                .getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM) == null, "Lead Ingot");
        }
    }

    /**
     * Ore worldgen code path: fills x 0..7, y 1..3, z 0..2 with stone, places a Lead Ore vein (the ore feature on
     * plain stone, size 10, seeded) centred on x 4, y 2, z 1. Expected: lead ore inside the stone only, floor included
     * (> 0 inside, 0 in the air around) and the block drops one Lead Ore item (loot table).
     */
    static final class OreVein implements TestCase {
        @Override
        public String name() {
            return "wp12 ore vein";
        }

        @Override
        public int height() {
            return 6;
        }

        @Override
        public void build(TestContext ctx) {
            ServerLevel w = ctx.world();
            for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(ctx.at(0, 1, 0), ctx.at(7, 3, 2)))
                if (!w.getBlockState(p).isAir()) throw new IllegalStateException("not empty: " + p);
            for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(ctx.at(0, 1, 0), ctx.at(7, 3, 2)))
                w.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
            net.minecraft.core.BlockPos c = ctx.at(4, 2, 1);
            // 26.x ore features centre the vein on the origin (1.12 WorldGenMinable: pos + 8)
            mods.eln.registry.batch.Wp12cContent.oreLead.generateVein(w, net.minecraft.util.RandomSource.create(1234567), c.getX(), c.getY(), c.getZ(), 10);
        }

        @Override
        public void measure(TestContext ctx) {
            ServerLevel w = ctx.world();
            net.minecraft.world.level.block.Block ore = mods.eln.registry.batch.Wp12cContent.oreLead.block;
            int inside = 0, outside = 0;
            net.minecraft.core.BlockPos samplePos = null;
            try {
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(ctx.at(-1, 0, 0), ctx.at(8, 5, 2))) {
                    if (w.getBlockState(p).getBlock() != ore) continue;
                    boolean in = p.getX() >= ctx.at(0, 0, 0).getX() && p.getX() <= ctx.at(7, 0, 0).getX()
                        && p.getY() >= ctx.at(0, 0, 0).getY() && p.getY() <= ctx.at(0, 3, 0).getY() // the floor is stone too (1.12 same)
                        && p.getZ() >= ctx.at(0, 0, 0).getZ() && p.getZ() <= ctx.at(0, 0, 2).getZ();
                    if (in) inside++;
                    else outside++;
                    if (samplePos == null) samplePos = p.immutable();
                }
                ctx.check("wp12 ore vein in stone", inside > 0 && outside == 0,
                    inside + " lead ore block(s) in the stone (expected > 0), " + outside + " outside (expected 0)");
                if (samplePos != null) {
                    List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(w.getBlockState(samplePos), w, samplePos, null);
                    ctx.check("wp12 lead ore drop", drops.size() == 1 && drops.get(0).getItem() == ore.asItem() && drops.get(0).getCount() == 1,
                        drops + " (expected 1x lead ore)");
                }
            } finally {
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(ctx.at(0, 1, 0), ctx.at(7, 3, 2))) w.removeBlock(p, false);
            }
        }
    }

    /**
     * World generation data (26.x, no 1.12 counterpart): eln:tungsten_ore and eln:lead_ore are placed features added to
     * overworld biomes by the biome modifier (1.12 defaults: only tungsten; 26.x adds lead, Baughn 2026-10-11), cinnabar
     * has none (EA's copper ore is gone), and the ores are pickaxe-mineable and need a tool.
     */
    static final class OreWorldgen implements TestCase {
        @Override
        public String name() {
            return "wp12 ore worldgen data";
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public void build(TestContext ctx) {
        }

        @Override
        public void measure(TestContext ctx) {
            ServerLevel w = ctx.world();
            var placed = w.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
            var tungsten = placed.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "tungsten_ore")));
            ctx.check("worldgen: placed feature eln:tungsten_ore", tungsten.isPresent(), "");
            boolean others = placed.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "cinnabar_ore"))).isPresent();
            ctx.check("worldgen: placed feature eln:lead_ore (26.x)", placed.get(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.PLACED_FEATURE, net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "lead_ore"))).isPresent(), "");
            ctx.check("worldgen: no cinnabar feature (1.12 defaults)", !others, "");
            if (tungsten.isPresent()) {
                var plains = w.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).value();
                boolean found = false;
                for (var set : plains.getGenerationSettings().features())
                    for (var h : set) if (h.value() == tungsten.get().value()) found = true;
                ctx.check("worldgen: plains generate tungsten ore (biome modifier)", found, "");
            }
            net.minecraft.world.level.block.state.BlockState ore = mods.eln.registry.batch.Wp12cContent.oreLead.block.defaultBlockState();
            ctx.check("ores: pickaxe-mineable, need a tool", ore.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) && ore.requiresCorrectToolForDrops(), "");
        }
    }

    /**
     * E-Coal chestplate on a pig, charged to its full 4000 J ((6 + 2) * 500), then 10 cactus damage (armour applies). 1.12 ISpecialArmor
     * maths: ratio = min(1, 4000 / 3000) * 6 / 20 = 0.3; absorbed = min(250 * 0.3, AbsorbMax 4000 / 500 * 25 = 200)
     * = 75 (1/25 units) -> 3 damage points -> 1500 J used; the pig takes 7 (health 10 -> 3). Also: the copper tools and
     * armour exist with the iron stats.
     */
    static final class ECoalArmor implements TestCase {
        net.minecraft.world.entity.animal.pig.Pig pig;
        ItemStack plate;
        float healthAfter;

        @Override
        public String name() {
            return "wp12 e-coal armour";
        }

        @Override
        public int width() {
            return 3;
        }

        @Override
        public void build(TestContext ctx) {
            ServerLevel w = ctx.world();
            pig = net.minecraft.world.entity.EntityTypes.PIG.create(w, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            pig.snapTo(ctx.at(1, 1, 1).getX() + 0.5, ctx.at(1, 1, 1).getY(), ctx.at(1, 1, 1).getZ() + 0.5, 0, 0);
            pig.setNoAi(true);
            w.addFreshEntity(pig);
            plate = new ItemStack(mods.eln.registry.batch.Wp12dContent.plateECoal.get());
            ((mods.eln.item.electricalitem.ElectricalArmor) plate.getItem()).setEnergy(plate, 4000);
            pig.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, plate);
            pig.hurtServer(w, w.damageSources().cactus(), 10f); // generic damage bypasses armour on 26.x
            healthAfter = pig.getHealth();
        }

        @Override
        public void measure(TestContext ctx) {
            try {
                ItemStack worn = pig.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                double e = ((mods.eln.item.electricalitem.ElectricalArmor) worn.getItem()).getEnergy(worn);
                ctx.checkValue("e-coal armour: pig health after 10 damage (10 - 7)", healthAfter, 3.0);
                ctx.checkValue("e-coal armour: energy left [J] (4000 - 3 * 500)", e, 2500.0);
                ctx.check("e-coal armour: no armour points", pig.getArmorValue() == 0, "armor " + pig.getArmorValue());
            } finally {
                pig.discard();
            }
        }
    }

    /**
     * Replicator entity (1.12 case): spawned in its box with its AI off (1.12 put it 24 blocks from the row so it could
     * not reach the other cases' cables; GameTest boxes are closer). Expected after the run: alive (persistent), max
     * health 8, attack damage 3, follow range 8, movement speed 0.23 (1.7.10 attributes); the entity id is
     * eln:replicator and it has a spawn egg. Peaceful difficulty would remove it: skipped then.
     */
    static final class Replicator implements TestCase {
        private mods.eln.entity.ReplicatorEntity entity;

        @Override
        public String name() {
            return "wp12 replicator";
        }

        @Override
        public int width() {
            return 3;
        }

        @Override
        public void build(TestContext ctx) {
            ServerLevel w = ctx.world();
            if (w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
            entity = mods.eln.registry.ElnRegistry.REPLICATOR.get().create(w, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            net.minecraft.core.BlockPos s = ctx.at(1, 1, 1);
            entity.snapTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 0f, 0f);
            entity.setNoAi(true);
            w.addFreshEntity(entity);
        }

        @Override
        public void measure(TestContext ctx) {
            ServerLevel w = ctx.world();
            if (w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                ctx.line("SKIP wp12 replicator: peaceful difficulty");
                return;
            }
            try {
                ctx.check("wp12 replicator alive", entity != null && entity.isAlive() && !entity.isRemoved(),
                    entity == null ? "not spawned" : "at " + entity.blockPosition());
                if (entity != null) {
                    ctx.checkValue("wp12 replicator max health", entity.getMaxHealth(), 8);
                    ctx.checkValue("wp12 replicator attack damage", entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), 3);
                    ctx.checkValue("wp12 replicator follow range", entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE), 8);
                    ctx.checkValue("wp12 replicator movement speed", entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), 0.23000000417232513);
                    ctx.check("wp12 replicator id and spawn egg", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("eln:replicator")
                        && mods.eln.registry.ElnRegistry.REPLICATOR_SPAWN_EGG.get() != null, String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
                }
            } finally {
                if (entity != null) entity.discard();
            }
        }
    }
}
