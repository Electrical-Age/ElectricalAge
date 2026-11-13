package mods.eln.node.voxel;

import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A transformer winding (Baughn 2026-11-06, Wires stage 4): a coil of N turns of one wire (cross-section, insulation,
 * colour), made by a wire-drawing machine. One goes in a DC-DC converter's winding slot instead of a stack of cables:
 * its turns are its count, so a converter can have hundreds (the stack limit of 64 cables can't reach 11 kV).
 */
public class WindingItem extends Item {
    public static final int MAX_TURNS = 1000;
    /** Wire a turn takes, blocks (for its resistance and the machine's cost). */
    public static final double BLOCKS_PER_TURN = 1.0 / 8;

    static final String TURNS = "turns", TIER = "tier", MM2 = "mm2", COLOUR = "col";

    public WindingItem(Properties p) {
        super(p.stacksTo(1));
    }

    /** A winding of turns of a wire of this insulation tier (1..4) and spec. */
    public static ItemStack make(int tier, CableSpec spec, int turns) {
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.WINDING.get());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, t -> {
            t.putInt(TURNS, Math.max(1, Math.min(MAX_TURNS, turns)));
            t.putInt(TIER, tier);
            t.putInt(MM2, spec.mm2());
            t.putInt(COLOUR, spec.colour());
        });
        return s;
    }

    private static net.minecraft.nbt.CompoundTag tag(ItemStack s) {
        var d = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null ? new net.minecraft.nbt.CompoundTag() : d.copyTag();
    }

    public static boolean is(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof WindingItem;
    }

    public static int turns(ItemStack s) {
        return Math.max(1, Math.min(MAX_TURNS, tag(s).getIntOr(TURNS, 1)));
    }

    /** Its insulation tier, 1..4 (48 V, 230 V, 400 V, 11 kV). */
    public static int tier(ItemStack s) {
        int t = tag(s).getIntOr(TIER, 1);
        return CableSpec.power(t) ? t : 1;
    }

    public static CableSpec spec(ItemStack s) {
        var t = tag(s);
        return new CableSpec(t.getIntOr(MM2, 4), t.getIntOr(COLOUR, CableSpec.BROWN));
    }

    /** Its wire's constants (rating, insulation). */
    public static ElectricalCableDescriptor descriptor(ItemStack s) {
        return spec(s).descriptor(tier(s));
    }

    /** Its resistance, ohms (as a cable load's Rs counts it: half the wire's). */
    public static double rs(ItemStack s) {
        return turns(s) * BLOCKS_PER_TURN * spec(s).ohmsPerBlock() / 2;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(" (" + turns(stack) + " turns)");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        CableSpec spec = spec(stack);
        out.accept(Component.literal(turns(stack) + " turns of " + spec.label(tier(stack)) + String.format(", %.0f A", spec.amps()))
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("For a Transformer (DC-DC)'s winding slot").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        // (Baughn 2026-11-13: say that they merge; HandWindingRecipe's rule)
        out.accept(Component.literal("Windings of the same wire merge in a crafting grid, up to " + HandWindingRecipe.MAX_TURNS + " turns")
            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
