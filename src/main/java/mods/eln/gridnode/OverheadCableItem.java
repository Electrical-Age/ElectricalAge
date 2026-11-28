package mods.eln.gridnode;

import mods.eln.node.voxel.CableSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Overhead cable (Baughn 2026-11-09): bare copper for the spans between utility poles, made on a wire-drawing machine.
 * Air insulates a line in the open, so it takes no rubber; the pole's insulators set the line's voltage, and the
 * cable's cross-section only its resistance. It is good for nothing else, and poles take nothing else (cable kits are
 * refused there). One holds up to MAX_BLOCKS blocks of one cross-section; two of the same cross-section combine
 * wherever they meet, as kits do (Pourable).
 */
public class OverheadCableItem extends Item implements mods.eln.node.voxel.Pourable {
    /** Most blocks one holds (Baughn: up to 500 block lengths, for convenience). */
    public static final int MAX_BLOCKS = 500;

    static final String BLOCKS = "blocks", MM2 = "mm2";

    public OverheadCableItem(Properties p) {
        super(p.stacksTo(1));
    }

    /** blocks of overhead cable of this cross-section (CableSpec.MM2 index); empty for none. */
    public static ItemStack make(int mm2, int blocks) {
        if (blocks <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.OVERHEAD_CABLE.get());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, t -> {
            t.putInt(BLOCKS, Math.min(MAX_BLOCKS, blocks));
            t.putInt(MM2, new CableSpec(mm2, 0).mm2());
        });
        return s;
    }

    private static net.minecraft.nbt.CompoundTag tag(ItemStack s) {
        var d = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null ? new net.minecraft.nbt.CompoundTag() : d.copyTag();
    }

    public static boolean is(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof OverheadCableItem;
    }

    public static int blocks(ItemStack s) {
        return is(s) ? Math.max(0, Math.min(MAX_BLOCKS, tag(s).getIntOr(BLOCKS, 0))) : 0;
    }

    /** Its cross-section (CableSpec.MM2 index; 1.5 mm² if it carries none). */
    public static int mm2(ItemStack s) {
        return new CableSpec(tag(s).getIntOr(MM2, 3), 0).mm2();
    }

    /** A span's resistance a block, ohms: a cable's of this cross-section (as the kits' spans counted it). */
    public static double rsPerBlock(ItemStack s) {
        return new CableSpec(mm2(s), 0).descriptor(4).electricalRs;
    }

    /** Takes blocks off s and returns them as a piece of the same cable; s keeps the rest (emptied when none is left). */
    public static ItemStack take(ItemStack s, int blocks) {
        int have = blocks(s);
        if (blocks <= 0 || have <= 0) return ItemStack.EMPTY;
        int n = Math.min(have, blocks);
        ItemStack piece = make(mm2(s), n);
        if (n >= have) s.setCount(0);
        else net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s,
            t -> t.putInt(BLOCKS, have - n));
        return piece;
    }

    @Override
    public Component getName(ItemStack stack) {
        int b = blocks(stack);
        return Component.translatable(getDescriptionId()).append(" (" + b + (b == 1 ? " block)" : " blocks)"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        CableSpec spec = new CableSpec(mm2(stack), 0);
        out.accept(Component.literal(CableSpec.mm2Number(CableSpec.MM2[spec.mm2()])
            + String.format(" mm² bare copper, %.0f A", spec.amps())).withStyle(net.minecraft.ChatFormatting.GRAY));
        mods.eln.client.ItemTooltips.addDetails(out,
            mods.eln.client.ItemTooltips.hintLine(mods.eln.i18n.I18N.tr("Right-click two utility poles to span between them")));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * blocks(stack) / MAX_BLOCKS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xd07040;
    }

    @Override
    public int held(ItemStack s) {
        return blocks(s);
    }

    @Override
    public int capacity() {
        return MAX_BLOCKS;
    }

    @Override
    public void setHeld(ItemStack s, int n) {
        int now = Math.max(0, Math.min(MAX_BLOCKS, n));
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, t -> t.putInt(BLOCKS, now));
    }

    @Override
    public boolean sameKind(ItemStack a, ItemStack b) {
        return mm2(a) == mm2(b);
    }

    /** Another overhead cable of the same cross-section clicked onto this one, or this onto it: as much goes in as
     *  fits. They also merge wherever they meet, as kits do (Baughn 2026-11-28; mods.eln.node.voxel.Pourable). */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.entity.SlotAccess carried) {
        return mods.eln.node.voxel.KitItem.clickedKit(other, self, slot, action, player);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                                          net.minecraft.world.entity.player.Player player) {
        return mods.eln.node.voxel.KitItem.clickedKit(self, slot.getItem(), slot, action, player);
    }
}
