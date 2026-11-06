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
        super(p.stacksTo(1));
        this.tier = tier;
    }

    /** A kit holding len voxels of a tier's cable (empty if none), its preset spec. */
    public static ItemStack kit(int tier, int len) {
        return kit(tier, null, len);
    }

    /**
     * A kit of len voxels of a tier's cable of this spec (null: the tier's preset). A preset kit carries no spec, so it
     * stacks and merges with crafted ones; another spec is kept in its custom data (mm2, col).
     */
    public static ItemStack kit(int tier, CableSpec spec, int len) {
        if (len <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.CABLE_KITS.get(tier).get());
        KitItem.setLength(s, len);
        if (spec != null && CableSpec.power(tier) && !spec.equals(CableSpec.preset(tier)))
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, t -> {
                t.putInt(MM2_KEY, spec.mm2());
                t.putInt(COLOUR_KEY, spec.colour());
            });
        return s;
    }

    static final String MM2_KEY = "mm2", COLOUR_KEY = "col";

    /** A cable kit's spec: its custom data's, or its tier's preset; null for a signal, thermal or bus kit (or not a kit). */
    public static CableSpec specOf(ItemStack s) {
        if (!(s.getItem() instanceof CableKitItem k) || !CableSpec.power(k.tier)) return null;
        var data = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) {
            var t = data.copyTag();
            if (t.contains(MM2_KEY)) return new CableSpec(t.getIntOr(MM2_KEY, 0), t.getIntOr(COLOUR_KEY, 0));
        }
        return CableSpec.preset(k.tier);
    }

    /** A kit of its tier's preset (made from coils, cut back into them). */
    public static boolean isPreset(ItemStack s) {
        return s.getItem() instanceof CableKitItem k && (!CableSpec.power(k.tier) || CableSpec.preset(k.tier).equals(specOf(s)));
    }

    @Override
    public ItemStack make(int len) {
        return kit(tier, len);
    }

    @Override
    public ItemStack make(ItemStack like, int len) {
        return kit(tier, specOf(like), len);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        CableSpec spec = specOf(stack);
        if (spec != null) out.accept(Component.literal(spec.label(tier) + String.format(", %.0f A, ", spec.amps())
            + net.minecraft.world.item.DyeColor.byId(spec.colour()).getName().replace('_', ' ')).withStyle(net.minecraft.ChatFormatting.GRAY));
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
    public int getBarWidth(ItemStack stack) {
        return KitItem.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return KitItem.barColor(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(String.format(" (%.2f blocks)", KitItem.length(stack) / 16.0));
    }

    /**
     * Cables of this tier into the kit, like filling a bundle (Baughn: a crafted kit couldn't be topped up past nine
     * cables): click a stack of them onto the kit, or the kit onto them. As many go in as fit (16 voxels each).
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.entity.SlotAccess carried) {
        // (coils are preset cable: they only go into a preset kit)
        return KitRecipes.clicked(self, isPreset(self) ? tier : -2, other, CableKitRecipes.coilTier(other), 1, other::shrink, slot, action, player);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                                          net.minecraft.world.entity.player.Player player) {
        ItemStack coils = slot.getItem();
        return KitRecipes.clicked(self, isPreset(self) ? tier : -2, coils, CableKitRecipes.coilTier(coils), 1, n -> slot.safeTake(n, n, player), slot, action, player);
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
