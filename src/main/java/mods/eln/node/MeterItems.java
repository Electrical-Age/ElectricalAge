package mods.eln.node;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** The measuring tools NodeBase reacts to; set when their items are registered (1.12: Eln.multiMeterElement etc.). */
public final class MeterItems {
    private MeterItems() {
    }

    public static Predicate<ItemStack> multiMeter = s -> false, thermometer = s -> false, allMeter = s -> false;

    public static boolean isMultiMeter(ItemStack s) {
        return multiMeter.test(s);
    }

    public static boolean isThermometer(ItemStack s) {
        return thermometer.test(s);
    }

    public static boolean isAllMeter(ItemStack s) {
        return allMeter.test(s);
    }
}
