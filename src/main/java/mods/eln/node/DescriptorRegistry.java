package mods.eln.node;

import mods.eln.generic.IDescriptorItem;
import mods.eln.misc.Utils;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Device descriptors by their 1.12 id (the damage value of the old sub-item, kept as the element id in node NBT and
 * in the client sync packets, as in 1.12) and by item. One instance for six-node devices, one for transparent ones
 * (1.12: Eln.sixNodeItem / Eln.transparentNodeItem, GenericItemBlockUsingDamage.subItemList).
 */
public final class DescriptorRegistry<D> {
    private final Map<Integer, D> byId = new TreeMap<>();
    private final Class<D> type;

    public DescriptorRegistry(Class<D> type) {
        this.type = type;
    }

    public void add(int legacyDamage, D descriptor) {
        byId.put(legacyDamage, descriptor);
    }

    /** 1.12 GenericItemBlockUsingDamage.addDescriptor: registered, gets an item. */
    public void addDescriptor(int legacyDamage, D descriptor) {
        add(legacyDamage, descriptor);
    }

    /** 1.12 addWithoutRegistry: a hidden descriptor (no creative tab entry); still an item on 26.x. */
    public void addWithoutRegistry(int legacyDamage, D descriptor) {
        add(legacyDamage, descriptor);
    }

    @Nullable
    public D getDescriptor(int legacyDamage) {
        return byId.get(legacyDamage);
    }

    @Nullable
    public D getDescriptor(ItemStack stack) {
        if (Utils.isEmpty(stack) || !(stack.getItem() instanceof IDescriptorItem)) return null;
        Object d = ((IDescriptorItem) stack.getItem()).getEaDescriptor();
        return type.isInstance(d) ? type.cast(d) : null;
    }

    /** All registered descriptors by 1.12 id (1.12 subItemList). */
    public Map<Integer, D> all() {
        return Collections.unmodifiableMap(byId);
    }
}
