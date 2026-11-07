package mods.eln.node.voxel;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import net.minecraft.world.item.ItemStack;

/**
 * A cable as devices take it (Baughn 2026-11-07: cable kits replace the 1.12 cable items in poles, breakers, hubs,
 * lamp supplies, probes and converters): a 1.12 cable item (still valid where it already is, and still crafted into
 * kits) or a cable kit, whose spec sets its electrical and thermal constants. A kit's length counts in blocks (16
 * voxels), as the items counted one a block.
 */
public final class CableItems {
    private CableItems() {
    }

    /** A cable kit's tier, or -1. */
    public static int kitTier(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof CableKitItem k ? k.tier : -1;
    }

    /** An electrical tier (signal, 48 V ... 11 kV): what the electrical cable slots took. */
    public static boolean electricalTier(int tier) {
        return tier >= 0 && tier < CablePart.TIERS.length && CablePart.TIERS[tier].kind() == CablePart.Tier.Kind.ELECTRICAL;
    }

    /** The cable's tier (CablePart.TIERS): a kit's, or a 1.12 item's; -1 if it is neither. */
    public static int tier(ItemStack s) {
        int t = kitTier(s);
        return t >= 0 ? t : CableKitRecipes.coilTier(s);
    }

    /** An electrical cable: a 1.12 item or a kit of an electrical tier. */
    public static boolean isElectrical(ItemStack s) {
        return descriptor(s) != null;
    }

    /** A thermal cable: the 1.12 item or the thermal kit. */
    public static boolean isThermal(ItemStack s) {
        return thermal(s) != null;
    }

    /** Its electrical constants: the 1.12 item's descriptor, or the kit's spec (its tier's preset if it has none). */
    public static ElectricalCableDescriptor descriptor(ItemStack s) {
        if (s.isEmpty()) return null;
        int t = kitTier(s);
        if (t >= 0) {
            if (!electricalTier(t)) return null;
            CableSpec spec = CableKitItem.specOf(s);
            return spec != null ? spec.descriptor(t) : CablePart.TIERS[t].descriptor();
        }
        return GenericItemBlockUsingDamageDescriptor.getDescriptor(s) instanceof ElectricalCableDescriptor d ? d : null;
    }

    /** Its thermal constants: the 1.12 thermal cable's, or the thermal kit's. */
    public static ThermalCableDescriptor thermal(ItemStack s) {
        if (s.isEmpty()) return null;
        int t = kitTier(s);
        if (t >= 0) return CablePart.TIERS[t].kind() == CablePart.Tier.Kind.THERMAL ? CablePart.TIERS[t].thermalDescriptor() : null;
        return GenericItemBlockUsingDamageDescriptor.getDescriptor(s) instanceof ThermalCableDescriptor d ? d : null;
    }

    /** How many blocks of cable: a 1.12 stack's count, a kit's length in whole blocks (at least 1). */
    public static int blocks(ItemStack s) {
        if (s.isEmpty()) return 0;
        if (kitTier(s) >= 0) return Math.max(1, KitItem.length(s) / 16);
        return s.getCount();
    }

    /** A kit's voxels, or a 1.12 stack's (16 a cable). */
    public static int voxels(ItemStack s) {
        if (s.isEmpty()) return 0;
        return kitTier(s) >= 0 ? KitItem.length(s) : 16 * s.getCount();
    }

    /** Adds voxels to a kit (up to a kit's most). */
    public static void add(ItemStack kit, int voxels) {
        if (kitTier(kit) >= 0) KitItem.setLength(kit, KitItem.length(kit) + voxels);
    }

    /**
     * Takes voxels off a cable stack and returns them as a stack of the same cable: a kit piece with the kit's spec,
     * or whole 1.12 items (rounded up to a cable). The given stack keeps the rest (emptied when none is left).
     */
    public static ItemStack take(ItemStack s, int voxels) {
        if (s.isEmpty() || voxels <= 0) return ItemStack.EMPTY;
        if (kitTier(s) < 0) return s.split((voxels + 15) / 16);
        int have = KitItem.length(s);
        if (voxels >= have) {
            ItemStack all = s.copy();
            s.setCount(0);
            return all;
        }
        ItemStack piece = s.copyWithCount(1);
        KitItem.setLength(piece, voxels);
        KitItem.setLength(s, have - voxels);
        return piece;
    }
}
