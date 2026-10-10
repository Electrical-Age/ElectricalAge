package mods.eln.generic;

import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Descriptor of a plain EA item (1.12: a sub-item of SharedItem, item + damage). On 26.x each descriptor has its own
 * Item ({@link #parentItem}, an {@link IDescriptorItem}); damage values are gone. Item renders (1.12 IItemRenderer)
 * move to item models in P3.
 */
public class GenericItemUsingDamageDescriptor {

    public String IconName;
    public String name;
    public VoltageLevelColor voltageLevelColor = VoltageLevelColor.None;

    public Item parentItem;
    /** The 1.12 damage value (for eln-ids.json lookups and golden data); no meaning in game. */
    public int parentItemDamage;

    public GenericItemUsingDamageDescriptor(String name) {
        this(name, name);
    }

    public GenericItemUsingDamageDescriptor(String name, String iconName) {
        setDefaultIcon(iconName);
        this.name = name;
    }

    public void setDefaultIcon(String name) {
        this.IconName = "eln:" + name.replaceAll(" ", "").toLowerCase();
    }

    public CompoundTag getDefaultNBT() {
        return null;
    }

    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
    }

    public ItemStack onItemRightClick(ItemStack s, Level w, Player p) {
        return s;
    }

    public void getSubItems(List list) {
        list.add(newItemStack(1));
    }

    /** What 1.7.10 getIconName() returned. */
    public String getIconName() {
        return IconName;
    }

    public String getName(ItemStack stack) {
        return name;
    }

    public static GenericItemUsingDamageDescriptor getDescriptor(ItemStack stack) {
        if (Utils.isEmpty(stack)) return null;
        if (stack.getItem() instanceof IDescriptorItem) {
            Object d = ((IDescriptorItem) stack.getItem()).getEaDescriptor();
            if (d instanceof GenericItemUsingDamageDescriptor) return (GenericItemUsingDamageDescriptor) d;
        }
        return null;
    }

    public static GenericItemUsingDamageDescriptor getDescriptor(ItemStack stack, Class extendClass) {
        GenericItemUsingDamageDescriptor desc = getDescriptor(stack);
        if (desc == null) return null;
        if (!extendClass.isAssignableFrom(desc.getClass())) return null;
        return desc;
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

    public boolean checkSameItemStack(ItemStack stack) {
        return !Utils.isEmpty(stack) && stack.getItem() == parentItem;
    }

    /** Right-click on a block with the item; true if something happened (1.12 onItemUse). */
    public boolean onItemUse(ItemStack stack, Player player, Level world, int x, int y, int z, int side, float vx, float vy, float vz) {
        return false;
    }

    public void onUpdate(ItemStack stack, Level world, Entity entity, int par4, boolean par5) {
    }

    /** The stack's EA tag, created from getDefaultNBT() when missing (1.12 semantics: the stack gets the tag). */
    protected CompoundTag getNbt(ItemStack stack) {
        CompoundTag nbt = ItemNbt.get(stack);
        if (nbt == null) {
            nbt = getDefaultNBT();
            ItemNbt.set(stack, nbt);
        }
        return nbt;
    }

    /** Change the stack's EA tag (created from getDefaultNBT() when missing) and store it back (see ItemNbt.edit). */
    protected void editNbt(ItemStack stack, java.util.function.Consumer<CompoundTag> edit) {
        CompoundTag nbt = getNbt(stack);
        edit.accept(nbt);
        ItemNbt.set(stack, nbt);
    }

    public float getStrVsBlock(ItemStack stack, Block block) {
        return 0.2f;
    }

    public boolean onBlockDestroyed(ItemStack stack, Level w, Block block, int x, int y, int z, LivingEntity entity) {
        return false;
    }

    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return true;
    }

    public boolean onEntitySwing(LivingEntity entityLiving, ItemStack stack) {
        return false;
    }

    public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, Player player) {
        return false;
    }
}
