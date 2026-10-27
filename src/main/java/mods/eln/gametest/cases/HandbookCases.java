package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.handbook.Handbook;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Electrical Engineer's Ledger (Baughn 2026-10-27): a book ringed by 4 copper cables crafts it, and every player is
 * given one, once, when their inventory has a free slot. (The guide itself is client-only, checked in game.)
 */
public final class HandbookCases {
    private HandbookCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TestCase() {
            public String id() {
                return "handbook_recipe";
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                var cable = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("eln:copper_cable")));
                var grid = new ArrayList<ItemStack>();
                for (int i = 0; i < 9; i++)
                    grid.add(i == 4 ? new ItemStack(Items.BOOK) : i % 2 == 1 ? cable.copy() : ItemStack.EMPTY);
                var in = CraftingInput.of(3, 3, grid);
                var got = ctx.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, ctx.level());
                var out = got.map(h -> h.value().assemble(in)).orElse(ItemStack.EMPTY);
                ctx.check("a book ringed by 4 copper cables makes the Ledger", out.is(ElnRegistry.HANDBOOK.get()), out.toString());
            }
        });
        cases.add(new TestCase() {
            public String id() {
                return "handbook_given_once";
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                var p = player(ctx, "eln_handbook_once");
                boolean first = Handbook.tryGive(p), second = Handbook.tryGive(p);
                int n = p.getInventory().countItem(ElnRegistry.HANDBOOK.get());
                ctx.check("a new player gets the Ledger", first && n >= 1, "gave " + first + ", has " + n);
                p.getInventory().clearContent();
                boolean afterLoss = Handbook.tryGive(p);
                ctx.check("only once, even after losing it", !second && !afterLoss, "second " + second + ", after loss " + afterLoss);
            }
        });
        cases.add(new TestCase() {
            public String id() {
                return "handbook_waits_for_room";
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                var p = player(ctx, "eln_handbook_full");
                var inv = p.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) inv.setItem(i, new ItemStack(Items.STONE, 64));
                boolean full = Handbook.tryGive(p);
                inv.setItem(5, ItemStack.EMPTY);
                boolean room = Handbook.tryGive(p);
                ctx.check("a full inventory waits; a free slot gets the Ledger",
                    !full && room && inv.getItem(5).is(ElnRegistry.HANDBOOK.get()), "full " + full + ", room " + room + ", slot 5 " + inv.getItem(5));
                inv.clearContent();
            }
        });
    }

    /** A fake player that has never had the Ledger. */
    static net.minecraft.server.level.ServerPlayer player(TestContext ctx, String name) {
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(ctx.level(),
            new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        p.getInventory().clearContent();
        CompoundTag persisted = p.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        persisted.remove(Handbook.GIVEN_TAG);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return p;
    }
}
