package mods.eln.gridnode;

import mods.eln.node.voxel.CableSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Overhead cable (Baughn 2026-11-09): bare copper for the spans between utility poles, made on a wire-drawing machine.
 * Air insulates a line in the open, so it takes no rubber; the pole's insulators set the line's voltage, and the
 * cable's cross-section only its resistance. It is good for nothing else, and poles take nothing else (cable kits are
 * refused there). One holds up to MAX_BLOCKS blocks of one cross-section; two of the same cross-section combine by
 * clicking one onto the other.
 */
public class OverheadCableItem extends Item {
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
        return Component.translatable(getDescriptionId()).append(" (" + blocks(stack) + " blocks)");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        CableSpec spec = new CableSpec(mm2(stack), 0);
        out.accept(Component.literal(mods.eln.transparentnode.wiredrawing.WireDrawingDescriptor.fmt(CableSpec.MM2[spec.mm2()])
            + String.format(" mm² bare copper, %.0f A", spec.amps())).withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("For the spans between utility poles: right-click one pole, then the next")
            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
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

    /** Another overhead cable of the same cross-section clicked onto this one: as much goes in as fits. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.entity.SlotAccess carried) {
        if (action != net.minecraft.world.inventory.ClickAction.PRIMARY || !is(other) || mm2(other) != mm2(self)) return false;
        int room = MAX_BLOCKS - blocks(self);
        if (room <= 0) return false;
        int n = Math.min(room, blocks(other));
        int now = blocks(self) + n;
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, self, t -> t.putInt(BLOCKS, now));
        take(other, n);
        return true;
    }
}
