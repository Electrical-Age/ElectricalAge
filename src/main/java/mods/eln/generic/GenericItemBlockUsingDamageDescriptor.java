package mods.eln.generic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

import java.util.List;

public class GenericItemBlockUsingDamageDescriptor {

    String iconName;
    IIcon iconIndex;
    public String name;

    public Item parentItem;
    public int parentItemDamage;

    public GenericItemBlockUsingDamageDescriptor(String name) {
        this(name, name);
    }

    public GenericItemBlockUsingDamageDescriptor(String name, String iconName) {
        changeDefaultIcon(iconName);
        this.name = name;
    }

    public void changeDefaultIcon(String name) {
        String iconName = name.replaceAll(" ", "").toLowerCase();

        if (Eln.avoidSymbols &&
            getClass().getClassLoader().getResource("assets/eln/textures/blocks/" + iconName + "-ni.png") != null) {
            this.iconName = iconName + "-ni";
        } else {
            this.iconName = iconName;
        }
    }

    public NBTTagCompound getDefaultNBT() {
        return null;
    }

    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List<String> list, boolean par4) {
    }

    @SideOnly(value = Side.CLIENT)
    public void updateIcons(IIconRegister iconRegister) {
        if (use2DIcon())
            this.iconIndex = iconRegister.registerIcon("eln:" + iconName);
    }

    public boolean use2DIcon() {
        return true;
    }

    public IIcon getIcon() {
        return iconIndex;
    }

    public String getName(ItemStack stack) {
        return name;
    }

    public void setParent(Item item, int damage) {
        this.parentItem = item;
        this.parentItemDamage = damage;
    }

    public ItemStack newItemStack(int size) {
        return new ItemStack(parentItem, size, parentItemDamage);
    }

    public ItemStack newItemStack() {
        return new ItemStack(parentItem, 1, parentItemDamage);
    }

    public static GenericItemBlockUsingDamageDescriptor getDescriptor(ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        if (item instanceof GenericItemBlockUsingDamage == false) return null;
        GenericItemBlockUsingDamage genItem = (GenericItemBlockUsingDamage) item;
        return genItem.getDescriptor(stack);
    }

    public static GenericItemBlockUsingDamageDescriptor getDescriptor(ItemStack stack, Class extendClass) {
        GenericItemBlockUsingDamageDescriptor desc = getDescriptor(stack);
        if (desc == null) return null;
        if (extendClass.isAssignableFrom(desc.getClass()) == false) return null;
        return desc;
    }

    public boolean onEntityItemUpdate(EntityItem entityItem) {
        return false;
    }

    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player) {
        return false;
    }
}
