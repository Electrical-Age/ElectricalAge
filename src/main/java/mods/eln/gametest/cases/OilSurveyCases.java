package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.OilSurvey;
import mods.eln.oil.OilSurveyMapFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import java.util.List;

/** Oil survey maps (todo #470): the trade and loot function are loaded, a map is made of a lake in the ring, and no
 *  chunk is loaded for it. */
public final class OilSurveyCases {
    private OilSurveyCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new SurveyMap());
        cases.add(new ChestAndTrader());
    }

    /** The map in chest loot (a NeoForge global loot modifier adding eln:chests/oil_survey_map) and the wandering
     *  trader's offer (Baughn 2026-11-15: both approved). */
    static final class ChestAndTrader implements TestCase {
        public String id() {
            return "oil_survey_chest";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            ServerLevel level = ctx.level();
            if (OilSurvey.lakes(level) == null) {
                ctx.line("oil survey chest: the test world has no oil lakes; skipped");
                return;
            }
            // Real chests: a cartographer house chest (30 %) opened 60 times; each map is made around the chest.
            BlockPos chestAt = ctx.at(1, 1, 1);
            int maps = 0, other = 0;
            boolean good = true;
            String bad = "";
            for (int i = 0; i < 60; i++) {
                level.setBlock(chestAt, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
                if (!(level.getBlockEntity(chestAt) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                    good = false;
                    bad = "no chest";
                    break;
                }
                chest.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.VILLAGE_CARTOGRAPHER, 1000L + i);
                chest.unpackLootTable(null);
                for (int s = 0; s < chest.getContainerSize(); s++) {
                    ItemStack st = chest.getItem(s);
                    if (!st.is(Items.FILLED_MAP)) continue;
                    var name = st.get(DataComponents.ITEM_NAME);
                    boolean ours = name != null && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                        && t.getKey().equals("item.eln.oil_survey_map");
                    if (!ours) {
                        other++;
                        continue;
                    }
                    maps++;
                    var m = st.get(DataComponents.MAP_DECORATIONS);
                    var mark = m == null ? null : m.decorations().get("+");
                    double d = mark == null ? -1 : Math.hypot(mark.x() - chestAt.getX(), mark.z() - chestAt.getZ());
                    if (st.get(DataComponents.MAP_ID) == null || mark == null || d < 600 || d > 3000 || st.get(DataComponents.LORE) == null) {
                        good = false;
                        bad = "map id " + st.get(DataComponents.MAP_ID) + ", mark " + mark + ", " + Math.round(d) + " blocks from the chest";
                    }
                }
                chest.clearContent();
            }
            level.removeBlock(chestAt, false);
            ctx.check("oil survey chest: cartographer chests hold oil survey maps (30 %: 60 chests -> 6..33)", maps >= 6 && maps <= 33,
                maps + " maps, " + other + " other maps");
            ctx.check("oil survey chest: every chest map has an id, a mark 600-3000 blocks from the chest and lore", good, bad);

            // The extra table rolled without an origin (no place to measure from): nothing, and no error.
            var table = level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("eln", "chests/oil_survey_map")));
            var noOrigin = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY)).create(java.util.Optional.empty());
            java.util.List<ItemStack> got = new java.util.ArrayList<>();
            table.getRandomItemsRaw(noOrigin, st -> { if (!st.isEmpty()) got.add(st); });
            ctx.check("oil survey chest: without an origin the table gives nothing (the map is discarded)", got.isEmpty(), got.toString());

            // The wandering trader's offer.
            var trades = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
            var trade = trades.get(Identifier.fromNamespaceAndPath("eln", "wandering_trader/emerald_oil_survey_map"));
            ctx.check("oil survey trader: the wandering trader trade is loaded", trade.isPresent(), "");
            ctx.check("oil survey trader: it is in the wandering trader's uncommon set", trade.isPresent()
                && trade.get().is(TagKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("wandering_trader/uncommon"))), "");
            if (trade.isEmpty()) return;
            var trader = net.minecraft.world.entity.EntityTypes.WANDERING_TRADER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            BlockPos origin = ctx.at(0, 0, 0);
            trader.setPos(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5);
            var lc = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, trader.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, trader)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, net.minecraft.util.Unit.INSTANCE)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.VILLAGER_TRADE))
                .create(java.util.Optional.empty());
            var offer = trade.get().value().getOffer(lc);
            ItemStack r = offer == null ? ItemStack.EMPTY : offer.getResult();
            var m = r.get(DataComponents.MAP_DECORATIONS);
            ctx.check("oil survey trader: the offer is a marked map for 14 emeralds, 1 use", offer != null && r.is(Items.FILLED_MAP)
                && r.get(DataComponents.MAP_ID) != null && m != null && m.decorations().containsKey("+")
                && offer.getCostA().is(Items.EMERALD) && offer.getCostA().getCount() == 14 && offer.getCostB().isEmpty() && offer.getMaxUses() == 1,
                offer == null ? "no offer" : r.getHoverName().getString() + " for " + offer.getCostA().getCount() + " emeralds + " + offer.getCostB());
        }
    }

    static final class SurveyMap implements TestCase {
        public String id() {
            return "oil_survey_map";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            ServerLevel level = ctx.level();
            var trades = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
            var trade = trades.get(Identifier.fromNamespaceAndPath("eln", "cartographer/3/emerald_and_compass_oil_survey_map"));
            ctx.check("oil survey: the cartographer trade is loaded (its eln:oil_survey_map function parsed)", trade.isPresent(), "");
            ctx.check("oil survey: the trade is in the cartographer's level 3 set", trade.isPresent()
                && trade.get().is(TagKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("cartographer/level_3"))), "");
            var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).get(OilSurvey.LAKES);
            ctx.check("oil survey: the lake structure set is random-spread", set.isPresent()
                && set.get().value().placement() instanceof RandomSpreadStructurePlacement, "");
            if (set.isEmpty() || !(set.get().value().placement() instanceof RandomSpreadStructurePlacement pl)) return;
            ctx.line("oil survey: the test world's generator has oil lakes: " + (OilSurvey.lakes(level) != null));

            BlockPos origin = ctx.at(0, 0, 0);
            int loadedBefore = level.getChunkSource().getLoadedChunksCount();
            java.util.Set<BlockPos> seen = new java.util.HashSet<>();
            boolean inRing = true, unloaded = true;
            for (int i = 0; i < 40; i++) {
                BlockPos lake = OilSurvey.pick(level, set.get().value(), origin, 600, 3000, level.getRandom(), q -> OilSurvey.worth(level, q));
                if (lake == null) {
                    inRing = false;
                    continue;
                }
                seen.add(lake);
                double d = Math.hypot(lake.getX() - origin.getX(), lake.getZ() - origin.getZ());
                inRing &= d >= 600 && d <= 3000;
                unloaded &= level.getChunkSource().getChunkNow(lake.getX() >> 4, lake.getZ() >> 4) == null;
            }
            ctx.check("oil survey: 40 picks are all lakes 600-3000 blocks away", inRing, "");
            ctx.check("oil survey: 40 picks find several different lakes", seen.size() >= 6, seen.size() + " lakes");
            ctx.check("oil survey: a picked lake's chunk is not loaded", unloaded, "");

            BlockPos lake = seen.iterator().next();
            ItemStack map = new ItemStack(Items.FILLED_MAP);
            OilSurveyMapFunction.fill(level, map, lake, (byte) 2, MapDecorationTypes.RED_X);
            ctx.check("oil survey: the map has an id", map.get(DataComponents.MAP_ID) != null, "");
            var data = net.minecraft.world.item.MapItem.getSavedData(map, level);
            ctx.check("oil survey: the lake is on the map (512 blocks across)", data != null
                && Math.abs(data.centerX - lake.getX()) <= 256 && Math.abs(data.centerZ - lake.getZ()) <= 256,
                data == null ? "no map data" : "centre " + (data.centerX - lake.getX()) + ", " + (data.centerZ - lake.getZ()) + " from the lake");
            var marks = map.get(DataComponents.MAP_DECORATIONS);
            var mark = marks == null ? null : marks.decorations().get("+");
            ctx.check("oil survey: the map marks the lake", mark != null && Math.abs(mark.x() - lake.getX()) < 1 && Math.abs(mark.z() - lake.getZ()) < 1
                && mark.type().equals(MapDecorationTypes.RED_X), "" + mark);
            ctx.check("oil survey: the map's lore gives the position", map.get(DataComponents.LORE) != null
                && map.get(DataComponents.LORE).lines().get(0).getString().contains("" + lake.getX()), "");
            // the existence check (vanilla's, as /locate): a region file header read or a generation-point query
            long start = OilSurvey.pack(lake.getX() >> 4, lake.getZ() >> 4);
            ctx.line("oil survey: the existence check says of a lake in the test world: " + OilSurvey.present(level, set.get().value(), start));
            BlockPos full = OilSurvey.pick(level, origin, 600, 3000, level.getRandom());
            ctx.line("oil survey: the full pick (with the existence check) in the test world: " + (full == null ? "none" : "a lake"));
            // the trade itself, as a cartographer makes its offers (AbstractVillager.addOffersFromTradeSet)
            if (trade.isPresent() && full != null) {
                var villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                villager.setPos(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5);
                var lc = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                    new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, villager.position())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, villager)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, net.minecraft.util.Unit.INSTANCE)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.VILLAGER_TRADE))
                    .create(java.util.Optional.empty());
                java.util.Set<String> sold = new java.util.HashSet<>();
                boolean good = true;
                String what = "";
                for (int i = 0; i < 12; i++) {
                    var offer = trade.get().value().getOffer(lc);
                    if (offer == null) {
                        good = false;
                        what = "no offer";
                        continue;
                    }
                    ItemStack r = offer.getResult();
                    var m = r.get(DataComponents.MAP_DECORATIONS);
                    good &= r.is(Items.FILLED_MAP) && r.get(DataComponents.MAP_ID) != null && m != null && m.decorations().containsKey("+")
                        && offer.getCostA().is(Items.EMERALD) && offer.getCostB().is(Items.COMPASS);
                    if (m != null && m.decorations().containsKey("+")) sold.add(m.decorations().get("+").x() + "," + m.decorations().get("+").z());
                    what = r.getHoverName().getString() + " for " + offer.getCostA().getCount() + " emeralds";
                }
                ctx.check("oil survey: the trade gives a marked map for emeralds and a compass", good, what);
                ctx.check("oil survey: 12 offers point at several different lakes", sold.size() >= 4, sold.size() + " lakes");
            }
            ctx.check("oil survey: making the map loaded no chunk", level.getChunkSource().getChunkNow(lake.getX() >> 4, lake.getZ() >> 4) == null
                && level.getChunkSource().getLoadedChunksCount() <= loadedBefore, loadedBefore + " -> " + level.getChunkSource().getLoadedChunksCount());
        }
    }
}
