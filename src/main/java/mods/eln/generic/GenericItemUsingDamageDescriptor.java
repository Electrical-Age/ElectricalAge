package mods.eln.generic;

import mods.eln.misc.VoltageLevelColor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class GenericItemUsingDamageDescriptor {

    public String IconName;
    public String name;
    public VoltageLevelColor voltageLevelColor = VoltageLevelColor.None;

    public Item parentItem;
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

    public NBTTagCompound getDefaultNBT() {
        return null;
    }

    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {

    }

    public ActionResult<ItemStack> onItemRightClick(ItemStack s, World w, EntityPlayer p) {
        return new ActionResult(EnumActionResult.PASS, s);
    }

    public void getSubItems(List list) {
        ItemStack stack = newItemStack(1);
        list.add(stack);
    }

    // TODO(1.10): These are all implicit now.
//    @SideOnly(value = Side.CLIENT)
//    public void updateIcons(IIconRegister iconRegister) {
//        this.iconIndex = iconRegister.registerIcon(IconName);
//    }
//
//    public IIcon getIcon() {
//        return iconIndex;
//    }

    public String getName(ItemStack stack) {
        return name;
    }

    public static GenericItemUsingDamageDescriptor getDescriptor(ItemStack stack) {
        if (stack == null)
            return null;
        if (!(stack.getItem() instanceof GenericItemUsingDamage))
            return null;
        return ((GenericItemUsingDamage<GenericItemUsingDamageDescriptor>) stack.getItem()).getDescriptor(stack);
    }

    public static GenericItemUsingDamageDescriptor getDescriptor(ItemStack stack, Class extendClass) {
        GenericItemUsingDamageDescriptor desc = getDescriptor(stack);
        if (desc == null)
            return null;
        if (extendClass.isAssignableFrom(desc.getClass()) == false)
            return null;
        return desc;
    }

    public void setParent(Item item, int damage) {
        this.parentItem = item;
        this.parentItemDamage = damage;
    }

    public ItemStack newItemStack(int size) {
        ItemStack stack = new ItemStack(parentItem, size, parentItemDamage);
        stack.setTagCompound(getDefaultNBT());
        return stack;
    }

    public ItemStack newItemStack() {
        return newItemStack(1);
    }

    public boolean checkSameItemStack(@Nonnull ItemStack stack) {
        return stack.getItem() == parentItem && stack.getItemDamage() == parentItemDamage;
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float vx, float vy, float vz) {
        return EnumActionResult.PASS;
    }

    // TODO(1.10): Fix item render.
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return voltageLevelColor != VoltageLevelColor.None;
//    }
//
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return false;
//    }
//
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (getIcon() == null)
//            return;
//
//        voltageLevelColor.drawIconBackground(type);
//
//        // remove "eln:" to add the full path replace("eln:", "textures/blocks/") + ".png";
//        String icon = getIcon().getIconName().substring(4);
//        UtilsClient.drawIcon(type, new ResourceLocation("eln", "textures/items/" + icon + ".png"));
//    }

    public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) {
    }

    protected NBTTagCompound getNbt(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            stack.setTagCompound(nbt = getDefaultNBT());
        }
        return nbt;
    }

    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        return 0.2f;
    }

    public boolean onBlockDestroyed(ItemStack stack, World w, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        return false;
    }

    public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
        return true;
    }

    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        return false;
    }

    public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer player) {
        return false;
    }
}
