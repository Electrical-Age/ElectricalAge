package mods.eln.node.voxel;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

/**
 * Filling kits, shared by cable kits (from coils, one to a block) and pipe kits (from plates, PipeKitRecipes.PER_BLOCK
 * to a block) (Baughn: they should top up the same way, by the same code): in the crafting grid, by clicking a stack
 * onto a kit, and kit onto kit (PipeKitRecipes.Merge).
 */
public final class KitRecipes {
    private KitRecipes() {
    }

    /**
     * Units of one tier and at most one kit of that tier in a grid: how many units each slot gives (the slots in order,
     * first come first served), then tier, length and the units used; or null. The kit takes whole stacks, as many
     * blocks as fit (Baughn: a grid of cable stacks gave one cable's worth per slot), and at least one unit from every
     * slot (the crafting grid takes one from each; the rest goes by {@link #crafted}).
     *
     * @param kitTier  the tier of a kit of this kind, or -1 if the stack isn't one
     * @param unitTier the tier of a unit (a coil, a plate), or -1
     * @param perBlock units to a block (16 voxels) of a tier
     */
    public static int[] use(List<ItemStack> items, ToIntFunction<ItemStack> kitTier, ToIntFunction<ItemStack> unitTier, IntUnaryOperator perBlock) {
        int tier = -1, kitLen = 0, kits = 0, slots = 0, total = 0;
        for (ItemStack s : items) {
            if (s.isEmpty()) continue;
            int t = kitTier.applyAsInt(s);
            if (t >= 0) {
                if (++kits > 1 || s.getCount() != 1) return null;
                kitLen += KitItem.length(s);
            } else {
                t = unitTier.applyAsInt(s);
                if (t < 0) return null;
                slots++;
                total += s.getCount();
            }
            if (tier >= 0 && t != tier) return null;
            tier = t;
        }
        if (slots < 1) return null;
        int per = perBlock.applyAsInt(tier);
        int blocks = Math.min((KitItem.MAX - kitLen) / 16, total / per);
        int used = blocks * per;
        if (blocks < 1 || used < slots) return null;   // (every slot gives one: the grid takes it)
        int[] out = new int[items.size() + 3];
        int taken = 0;
        // one from each slot first (the grid takes that anyway), then the rest in order while it fits
        for (int i = 0; i < items.size(); i++)
            if (!items.get(i).isEmpty() && kitTier.applyAsInt(items.get(i)) < 0) {
                out[i] = 1;
                taken++;
            }
        for (int i = 0; i < items.size() && taken < used; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty() || kitTier.applyAsInt(s) >= 0) continue;
            int more = Math.min(s.getCount() - 1, used - taken);
            out[i] += more;
            taken += more;
        }
        out[items.size()] = tier;
        out[items.size() + 1] = kitLen + blocks * 16;
        out[items.size() + 2] = used;
        return out;
    }

    /**
     * Units of the kit's tier into the kit, like filling a bundle (Baughn: a crafted kit couldn't be topped up past nine
     * cables): as many blocks as fit go in; take gets the units used. The blocks added, or 0.
     */
    public static int absorb(ItemStack kit, int kitTier, ItemStack units, int unitTier, int per, IntConsumer take) {
        if (kit.getCount() != 1 || unitTier != kitTier) return 0;
        int n = Math.min(units.getCount() / per, (KitItem.MAX - KitItem.length(kit)) / 16);
        if (n <= 0) return 0;
        KitItem.setLength(kit, KitItem.length(kit) + 16 * n);
        take.accept(n * per);
        return n;
    }

    /** A stack clicked onto the kit (Item.overrideOtherStackedOnMe), or the kit onto a stack (overrideStackedOnOther). */
    static boolean clicked(ItemStack kit, int kitTier, ItemStack units, int unitTier, int per, IntConsumer take,
                           net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                           net.minecraft.world.entity.player.Player player) {
        if (action != net.minecraft.world.inventory.ClickAction.PRIMARY || !slot.allowModification(player)) return false;
        if (absorb(kit, kitTier, units, unitTier, per, take) == 0) return false;
        player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
        return true;
    }

    /**
     * A kit crafted from units takes the units it counted, when the craft was one of recipe's: the grid takes one from
     * each slot after this, so the rest goes here (server; the client follows by sync).
     */
    static void crafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e, Class<?> recipe,
                        java.util.function.Function<List<ItemStack>, int[]> use) {
        if (!PipeKitRecipes.ours(e, recipe)) return;
        var grid = e.getInventory();
        List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < grid.getContainerSize(); i++) items.add(grid.getItem(i));
        int[] u = use.apply(items);
        if (u == null) return;
        for (int i = 0; i < items.size(); i++)
            if (u[i] > 1) grid.getItem(i).shrink(u[i] - 1);
    }

    /**
     * A lone kit gives one block's worth of what it's made of back and keeps the rest in the grid (cable kits: a coil;
     * pipe kits: PipeKitRecipes.PER_BLOCK plates; Baughn: the same way for both, not halving pipe kits, whose shift-click
     * halved again and again). Shift-click unrolls the kit. One recipe per tier; -1 matches any (tests).
     */
    public abstract static class Unroll extends net.minecraft.world.item.crafting.CustomRecipe {
        public final int tier;

        protected Unroll(int tier) {
            this.tier = tier;
        }

        /** The tier of a kit of this kind, or -1. */
        protected abstract int kitTier(ItemStack s);

        /** What a block of a tier's kit gives back (empty if none). */
        protected abstract ItemStack unit(int tier);

        /** A kit of this kind and tier holding len voxels. */
        protected abstract ItemStack kit(int tier, int len);

        /** The one kit (a block or more) in the grid, or null. */
        ItemStack lone(net.minecraft.world.item.crafting.CraftingInput in) {
            ItemStack found = null;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (found != null || kitTier(s) < 0 || s.getCount() != 1) return null;
                found = s;
            }
            if (found != null && tier >= 0 && kitTier(found) != tier) return null;
            return found != null && KitItem.length(found) >= 16 ? found : null;
        }

        @Override
        public boolean matches(net.minecraft.world.item.crafting.CraftingInput input, net.minecraft.world.level.Level level) {
            return lone(input) != null;
        }

        @Override
        public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput input) {
            ItemStack s = lone(input);
            return s == null ? ItemStack.EMPTY : unit(kitTier(s));
        }

        /** The rest of the kit stays in the grid. */
        @Override
        public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(net.minecraft.world.item.crafting.CraftingInput input) {
            var out = net.minecraft.core.NonNullList.withSize(input.size(), ItemStack.EMPTY);
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                int t = kitTier(s);
                if (t >= 0) out.set(i, kit(t, KitItem.length(s) - 16));
            }
            return out;
        }

        /** In the recipe book too, per tier (the any-tier instance stays out). */
        @Override
        public boolean isSpecial() {
            return tier < 0;
        }

        @Override
        public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
            if (tier < 0) return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
            return net.minecraft.world.item.crafting.PlacementInfo.create(net.minecraft.world.item.crafting.Ingredient.of(kit(tier, 32).getItem()));
        }

        @Override
        public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
            ItemStack u = tier < 0 ? ItemStack.EMPTY : unit(tier);
            if (u.isEmpty()) return List.of();
            return List.of(new net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay(
                List.of(CableKitRecipes.slot(kit(tier, 32))), CableKitRecipes.slot(u), CableKitRecipes.table()));
        }
    }
}
