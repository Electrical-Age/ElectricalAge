package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A cable kit (notes/cables/PLAN.md; Baughn: 1024 voxel cross-sections, 64 blocks, for every tier): a length of one
 * tier's cable, kept as the item's damage (KitItem). Right-click faces to route a cable (CablePlacer). Made from the
 * old cable items ("coils", 16 voxels each) and cut back into them (CableKitRecipes).
 */
public class CableKitItem extends Item implements PipeTool, KitItem {
    public final int tier;

    public CableKitItem(int tier, Properties p) {
        super(p.durability(MAX));
        this.tier = tier;
    }

    /** A kit holding len voxels of a tier's cable (empty if none). */
    public static ItemStack kit(int tier, int len) {
        if (len <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.CABLE_KITS.get(tier).get());
        KitItem.setLength(s, len);
        return s;
    }

    @Override
    public ItemStack make(int len) {
        return kit(tier, len);
    }

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(String.format(" (%.2f blocks)", KitItem.length(stack) / 16.0));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (ctx.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp && ctx.getLevel() instanceof net.minecraft.server.level.ServerLevel sl
            && CablePlacer.use(sp, sl, ctx.getClickedPos(), ctx.getClickedFace(), ctx.getItemInHand(), tier, ctx.getClickLocation()))
            return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }
}
