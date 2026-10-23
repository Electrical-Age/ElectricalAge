package mods.eln.gui;

import mods.eln.generic.ElnItem;
import mods.eln.item.LampDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeItem;
import mods.eln.sixnode.lampsocket.LampSocketType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * One device slot of an EA GUI (P4): the 1.12 Slot classes as data, built the same way on the server (with the
 * element's inventory) and on the client (from the descriptor alone). x/y, skin and comment are for the screen (P4b).
 * <ul>
 * <li>{@link #any}: SlotWithSkin, accepts everything (1.12 Slot.isItemValid default), the inventory's limit.</li>
 * <li>{@link #descriptor}: GenericItemUsingDamageSlot, an EA shared item whose descriptor is (a subclass of) one of
 * the classes.</li>
 * <li>{@link #six}: SixNodeItemSlot, a six-node device item whose descriptor class is exactly one of the classes.</li>
 * <li>{@link #filter}: SlotFilter, any of the given tests.</li>
 * <li>{@link #battery}: BatteryChargerContainer.BatterySlot, any IItemEnergyBattery, 1 item.</li>
 * <li>{@link #lamp}: LampSlot, a lamp of the socket's type.</li>
 * <li>{@link #regulator}: RegulatorSlot, a regulator of one of the types.</li>
 * </ul>
 */
public record SlotSpec(String kind, int index, int x, int y, int limit, Predicate<ItemStack> filter, String skin, String[] comment) {
    public static final String MEDIUM = "medium", BIG = "big", SMALL = "small";

    public static SlotSpec any(int index, int x, int y, String skin, String... comment) {
        return new SlotSpec("SlotWithSkin", index, x, y, 64, s -> true, skin, comment);
    }

    /** A machine's output: take only, nothing goes in (Baughn: shift-click put items into the plate machine's and the
     *  electric furnace's output slots). */
    public static SlotSpec output(int index, int x, int y, String skin) {
        return new SlotSpec("Output", index, x, y, 64, s -> false, skin, new String[0]);
    }

    /** A furnace's input: what vanilla's furnace would smelt (the recipe property set, known on both sides; ElnSlot). */
    public static SlotSpec smeltable(int index, int x, int y, String skin, String... comment) {
        return new SlotSpec("Smeltable", index, x, y, 64, s -> true, skin, comment);
    }

    /** 1.12 GenericItemUsingDamageSlot: shared items only, descriptor class or a subclass. */
    public static SlotSpec descriptor(int index, int x, int y, int limit, Class<?>[] classes, String skin, String... comment) {
        return new SlotSpec("GenericItemUsingDamageSlot", index, x, y, limit, s -> descriptorIs(s, classes), skin, comment);
    }

    public static SlotSpec descriptor(int index, int x, int y, int limit, Class<?> cls, String skin, String... comment) {
        return descriptor(index, x, y, limit, new Class<?>[]{cls}, skin, comment);
    }

    static boolean descriptorIs(ItemStack s, Class<?>[] classes) {
        if (!(s.getItem() instanceof ElnItem e)) return false;
        Object d = e.getEaDescriptor();
        if (d == null) return false;
        for (Class<?> c : classes) if (c.isInstance(d)) return true;
        return false;
    }

    /** 1.12 SixNodeItemSlot: six-node device items, exact descriptor class. */
    public static SlotSpec six(int index, int x, int y, int limit, Class<?>[] classes, String skin, String... comment) {
        return new SlotSpec("SixNodeItemSlot", index, x, y, limit, s -> {
            if (!(s.getItem() instanceof SixNodeItem i)) return false;
            Object d = i.getEaDescriptor();
            for (Class<?> c : classes) if (d != null && d.getClass() == c) return true;
            return false;
        }, skin, comment);
    }

    public static SlotSpec filter(int index, int x, int y, int limit, Predicate<ItemStack> filter, String skin, String... comment) {
        return new SlotSpec("SlotFilter", index, x, y, limit, filter, skin, comment);
    }

    /** 1.12 BatteryChargerContainer.BatterySlot. */
    public static SlotSpec battery(int index, int x, int y) {
        return new SlotSpec("BatterySlot", index, x, y, 1, s -> Utils.getItemObject(s) instanceof IItemEnergyBattery, MEDIUM,
            new String[]{mods.eln.i18n.I18N.tr("Battery slot")});
    }

    /** 1.12 LampSlot: a lamp (shared item) of the socket's type. */
    public static SlotSpec lamp(int index, int x, int y, int limit, LampSocketType socket) {
        return new SlotSpec("LampSlot", index, x, y, limit,
            s -> descriptorIs(s, new Class<?>[]{LampDescriptor.class}) && ((LampDescriptor) Utils.getItemObject(s)).socket == socket,
            MEDIUM, new String[]{mods.eln.i18n.I18N.tr("Lamp slot")});
    }

    /** 1.12 RegulatorSlot. */
    public static SlotSpec regulator(int index, int x, int y, int limit, IRegulatorDescriptor.RegulatorType[] types, String skin, String comment) {
        return new SlotSpec("RegulatorSlot", index, x, y, limit, s -> {
            if (!descriptorIs(s, new Class<?>[]{IRegulatorDescriptor.class})) return false;
            IRegulatorDescriptor.RegulatorType t = ((IRegulatorDescriptor) Utils.getItemObject(s)).getType();
            for (IRegulatorDescriptor.RegulatorType x2 : types) if (x2 == t) return true;
            return false;
        }, skin, new String[]{comment});
    }

    /** 1.12 ItemStackFilter: an exact item. */
    public static Predicate<ItemStack> item(net.minecraft.world.level.ItemLike item) {
        return s -> s.getItem() == item.asItem();
    }

    /** 1.12 ItemStackFilter.OreDict(name): the items of the ore name, now its tag. */
    public static Predicate<ItemStack> ore(String oreName) {
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = mods.eln.misc.OreDictTags.tag(oreName);
        return s -> s.is(tag);
    }
}
