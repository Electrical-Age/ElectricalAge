package mods.eln.bike;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Bags whose contents live in world storage under an id they get the first time they're opened: until then their item
 * handler is empty, and the trunk's panel had nothing to show (AdventurAgent: a fresh Sophisticated Backpack's tab, 0
 * slots). Opening one in the trunk makes that storage the way the bag itself does, through its own API, found by
 * reflection (no compile dependency): BackpackWrapper.fromStack(stack).getInventoryHandler(), which gives the stack its
 * contents id. Server only; anything that fails leaves the bag as it was.
 */
final class BagStorage {
    private BagStorage() {
    }

    static final String BACKPACKS = "sophisticatedbackpacks";
    static Method fromStack, inventoryHandler;
    static boolean unavailable;

    /** Trunk slot k: a backpack with no storage yet gets it (and the slot is marked changed). */
    static void prepare(Container trunk, int k) {
        ItemStack s = trunk.getItem(k);
        if (unavailable || s.getCount() != 1 || !BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(BACKPACKS)) return;
        if (BikeTrunkMenu.opens(s)) return;   // (it has slots already)
        try {
            if (fromStack == null) {
                Class<?> c = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper");
                fromStack = c.getMethod("fromStack", ItemStack.class);
                inventoryHandler = c.getMethod("getInventoryHandler");
            }
            Object w = fromStack.invoke(null, s);
            if (w != null) inventoryHandler.invoke(w);
            trunk.setChanged();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            unavailable = true;
            mods.eln.Eln.LOG.warn("Bike trunk: can't prepare a Sophisticated Backpack's storage ({}); it won't open there", e.toString());
        }
    }
}
