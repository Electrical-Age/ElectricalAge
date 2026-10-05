package mods.eln.compat;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import net.minecraft.item.ItemStack;

/**
 * The portable battery item class (mods.eln.item.electricalitem.BatteryItem, ported by batch wp12), looked up by name
 * so that batch wp9b (six-node watch, fire buzzer: battery slot) builds without wp12. Behaviour once wp12 is merged
 * is the 1.7.10 one: slot filter = BatteryItem.class, battery = (BatteryItem) getDescriptor(stack).
 * Without wp12 no item matches (slots accept nothing, devices see no battery).
 * TODO(1.12 wp12): after the merge, device code may use BatteryItem.class directly and this class can go.
 */
public final class BatteryItemRef {
    private BatteryItemRef() {
    }

    /** BatteryItem.class, or a class no descriptor extends (Void) if BatteryItem is not in the build. */
    public static final Class<?> CLASS = lookup();

    private static Class<?> lookup() {
        try {
            return Class.forName("mods.eln.item.electricalitem.BatteryItem", false, BatteryItemRef.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Void.class;
        }
    }

    /** 1.7.10 (BatteryItem) BatteryItem.getDescriptor(stack): null for an empty stack or a non-battery item. */
    public static IItemEnergyBattery get(ItemStack stack) {
        GenericItemUsingDamageDescriptor d = GenericItemUsingDamageDescriptor.getDescriptor(stack, CLASS);
        return d instanceof IItemEnergyBattery ? (IItemEnergyBattery) d : null;
    }
}
