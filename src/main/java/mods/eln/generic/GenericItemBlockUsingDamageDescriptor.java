package mods.eln.generic;

import mods.eln.misc.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Descriptor of an EA device item (six-node and transparent-node devices; 1.12: a sub-item of SixNodeItem /
 * TransparentNodeItem). On 26.x each has its own Item ({@link #parentItem}, an {@link IDescriptorItem}).
 */
public class GenericItemBlockUsingDamageDescriptor {

    String iconName;
    public String name;

    public Item parentItem;
    /** The 1.12 damage value (eln-ids.json, golden data); no meaning in game. */
    public int parentItemDamage;

    /**
     * R5: the item is drawn in 3D (the device's model) when held, dropped, framed or worn; the GUI keeps the flat
     * icon. 1.12: renderItem for every type but INVENTORY.
     */
    public boolean hasItemDraw() {
        return false;
    }

    /** The 3D item draw through the GL11 shim, centred on the origin. entity: dropped/framed/worn (1.12 ENTITY), else held. */
    public void drawItem(boolean entity) {
    }

    public GenericItemBlockUsingDamageDescriptor(String name) {
        this(name, name);
    }

    public GenericItemBlockUsingDamageDescriptor(String name, String iconName) {
        setDefaultIcon(iconName);
        this.name = name;
    }

    public void setDefaultIcon(String name) {
        this.iconName = name.replaceAll(" ", "").toLowerCase();
    }

    public CompoundTag getDefaultNBT() {
        return null;
    }

    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
    }

    /** What 1.7.10 getIconName() returned. */
    public String getIconName() {
        return "eln:" + iconName;
    }

    public String getName(ItemStack stack) {
        return name;
    }

    public void setParent(Item item, int damage) {
        this.parentItem = item;
        this.parentItemDamage = damage;
    }

    public ItemStack newItemStack(int size) {
        ItemStack stack = new ItemStack(parentItem, size);
        ItemNbt.set(stack, getDefaultNBT());
        return stack;
    }

    public ItemStack newItemStack() {
        return newItemStack(1);
    }

    public static GenericItemBlockUsingDamageDescriptor getDescriptor(ItemStack stack) {
        if (Utils.isEmpty(stack)) return null;
        if (stack.getItem() instanceof IDescriptorItem) {
            Object d = ((IDescriptorItem) stack.getItem()).getEaDescriptor();
            if (d instanceof GenericItemBlockUsingDamageDescriptor) return (GenericItemBlockUsingDamageDescriptor) d;
        }
        return null;
    }

    public static GenericItemBlockUsingDamageDescriptor getDescriptor(ItemStack stack, Class extendClass) {
        GenericItemBlockUsingDamageDescriptor desc = getDescriptor(stack);
        if (desc == null) return null;
        if (!extendClass.isAssignableFrom(desc.getClass())) return null;
        return desc;
    }

    public boolean checkSameItemStack(ItemStack stack) {
        return !Utils.isEmpty(stack) && stack.getItem() == parentItem;
    }

    public boolean onEntityItemUpdate(ItemEntity entityItem) {
        return false;
    }

    public boolean onItemUseFirst(ItemStack stack, Player player) {
        return false;
    }
}
