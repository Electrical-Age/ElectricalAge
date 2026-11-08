package mods.eln.gridnode.electricalpole;

import mods.eln.node.voxel.CableSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A pole transformer (Baughn 2026-11-08: made by the wire machine, fitted to a utility pole by right-clicking it). Its
 * high side is the pole's line, whatever that line runs at; its low side is the cable socket at the pole's foot, at
 * the voltage it was wound for (48, 230 or 400 V), with the current rating of its low-side wire.
 */
public class PoleTransformerItem extends Item {
    static final String TIER = "tier", MM2 = "mm2", CORE = "core";

    public PoleTransformerItem(Properties p) {
        super(p.stacksTo(1));
    }

    /**
     * A transformer whose low side is this power tier (1 48 V, 2 230 V, 3 400 V) wound with this cross-section, on a
     * ferromagnetic core of this quality (0 cheap, 1 average, 2 optimal).
     */
    public static ItemStack make(int tier, int mm2, int core) {
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.POLE_TRANSFORMER.get());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, t -> {
            t.putInt(TIER, clampTier(tier));
            t.putInt(MM2, Math.max(0, Math.min(CableSpec.MM2.length - 1, mm2)));
            t.putInt(CORE, Math.max(0, Math.min(2, core)));
        });
        return s;
    }

    static int clampTier(int t) {
        return Math.max(1, Math.min(3, t));
    }

    private static net.minecraft.nbt.CompoundTag tag(ItemStack s) {
        var d = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null ? new net.minecraft.nbt.CompoundTag() : d.copyTag();
    }

    public static boolean is(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof PoleTransformerItem;
    }

    /** The low side's power tier: 1 48 V, 2 230 V, 3 400 V (a bare item: 400 V, the old pole's). */
    public static int tier(ItemStack s) {
        return clampTier(tag(s).getIntOr(TIER, 3));
    }

    /** The low-side wire's cross-section (CableSpec.MM2 index; a bare item: 16 mm²). */
    public static int mm2(ItemStack s) {
        return Math.max(0, Math.min(CableSpec.MM2.length - 1, tag(s).getIntOr(MM2, 8)));
    }

    /** Its core's quality: 0 cheap, 1 average, 2 optimal (a bare item: optimal, as the old pole's recipe). */
    public static int core(ItemStack s) {
        return Math.max(0, Math.min(2, tag(s).getIntOr(CORE, 2)));
    }

    /** How much its core adds to the windings' resistance (a better core loses less): 2, 1.4, 1. */
    public static double lossFactor(ItemStack s) {
        return new double[]{2.0, 1.4, 1.0}[core(s)];
    }

    static final String[] CORE_NAMES = {"cheap", "average", "optimal"};

    public static double volts(ItemStack s) {
        return CableSpec.volts(tier(s));
    }

    /** The low side's current rating, amps (its wire's). */
    public static double amps(ItemStack s) {
        return CableSpec.AMPS[mm2(s)];
    }

    static String voltsLabel(double v) {
        return v >= 1000 ? (int) (v / 1000) + " kV" : (int) v + " V";
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(" (" + voltsLabel(volts(stack)) + ")");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        double v = volts(stack), a = amps(stack);
        out.accept(Component.literal(String.format("Line to %s, %.0f A (%s)", voltsLabel(v), a, mods.eln.misc.Utils.plotValue(v * a, "W")))
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("On a " + CORE_NAMES[core(stack)] + " core").withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("Right-click a utility pole to fit it").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
