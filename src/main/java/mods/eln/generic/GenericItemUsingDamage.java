package mods.eln.generic;

import mods.eln.misc.UtilsClient;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class GenericItemUsingDamage<Descriptor extends GenericItemUsingDamageDescriptor> extends Item implements IGenericItemUsingDamage {
    public Hashtable<Integer, Descriptor> subItemList = new Hashtable<Integer, Descriptor>();
    ArrayList<Integer> orderList = new ArrayList<Integer>();

    Descriptor defaultElement = null;

    public GenericItemUsingDamage() {
        super();
        setHasSubtypes(true);
    }

    public void addWithoutRegistry(int damage, Descriptor descriptor) {
        subItemList.put(damage, descriptor);
        setUnlocalizedName(descriptor.name);
        descriptor.setParent(this, damage);
    }

    public void addElement(int damage, Descriptor descriptor) {
        subItemList.put(damage, descriptor);
        setUnlocalizedName(descriptor.name);
        orderList.add(damage);
        descriptor.setParent(this, damage);
    }

    public Descriptor getDescriptor(int damage) {
        return subItemList.get(damage);
    }

    public Descriptor getDescriptor(ItemStack itemStack) {
        if (itemStack == null)
            return defaultElement;
        if (itemStack.getItem() != this)
            return defaultElement;
        return getDescriptor(itemStack.getItemDamage());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack s, World w, EntityPlayer p, EnumHand hand) {
        Descriptor desc = getDescriptor(s);
        if (desc == null)
            return new ActionResult(EnumActionResult.PASS, s);
        return desc.onItemRightClick(s, w, p);
    }

    @Override
    public String getUnlocalizedName(ItemStack par1ItemStack) {
        Descriptor desc = getDescriptor(par1ItemStack);
        if (desc != null && desc.name != null) {
            return desc.name.replaceAll("\\s+", "_");
        } else {
            return null;
        }
    }

    // TODO(1.10): Fix item rendering.
//    public IIcon getIconFromDamage(int damage) {
//        GenericItemUsingDamageDescriptor desc = getDescriptor(damage);
//        if (desc != null) {
//            return getDescriptor(damage).getIcon();
//        }
//        return null;
//    }
//
//    @Override
//    @SideOnly(value = Side.CLIENT)
//    public void registerIcons(IIconRegister iconRegister) {
//        for (GenericItemUsingDamageDescriptor descriptor : subItemList.values()) {
//            descriptor.updateIcons(iconRegister);
//        }
//    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item itemID, CreativeTabs tabs, List list) {
        // You can also take a more direct approach and do each one individual but I prefer the lazy / right way
        for (int id : orderList) {
            subItemList.get(id).getSubItems(list);
        }
    }

    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		/*Descriptor desc = getDescriptor(itemStack);
		if (desc == null)
			return;
		desc.addInformation(itemStack, entityPlayer, list, par4);
		*/
        Descriptor desc = getDescriptor(itemStack);
        if (desc == null) return;
        List listFromDescriptor = new ArrayList();
        desc.addInformation(itemStack, entityPlayer, listFromDescriptor, par4);
        UtilsClient.showItemTooltip(listFromDescriptor, list);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float vx, float vy, float vz) {
        GenericItemUsingDamageDescriptor d = getDescriptor(stack);
        if (d == null)
            return EnumActionResult.PASS;
        return d.onItemUse(stack, player, world, pos, hand, facing, vx, vy, vz);
    }

    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        GenericItemUsingDamageDescriptor d = getDescriptor(stack);
        if (d == null)
            return super.onEntitySwing(entityLiving, stack);
        return d.onEntitySwing(entityLiving, stack);
    }

    public boolean onBlockStartBreak(ItemStack itemstack, int X, int Y, int Z, EntityPlayer player) {
        GenericItemUsingDamageDescriptor d = getDescriptor(itemstack);
        if (d == null)
            return super.onBlockStartBreak(itemstack, new BlockPos(X, Y, Z), player);
        return d.onBlockStartBreak(itemstack, X, Y, Z, player);
    }

    public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) {
        if (world.isRemote) {
            return;
        }

        GenericItemUsingDamageDescriptor d = getDescriptor(stack);

        if (d == null)
            return;
        d.onUpdate(stack, world, entity, par4, par5);
    }

    @Override
    public float getStrVsBlock(ItemStack stack, IBlockState state) {
        GenericItemUsingDamageDescriptor d = getDescriptor(stack);
        if (d == null)
            return 0.2f;
        return d.getStrVsBlock(stack, state);
    }

    @Override
    public boolean canHarvestBlock(IBlockState state) {
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World w, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if (w.isRemote) {
            return false;
        }

        GenericItemUsingDamageDescriptor d = getDescriptor(stack);

        if (d == null)
            return true;
        return d.onBlockDestroyed(stack, w, state, pos, entity);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
        GenericItemUsingDamageDescriptor d = getDescriptor(item);
        if (d == null)
            return super.onDroppedByPlayer(item, player);
        return d.onDroppedByPlayer(item, player);
    }
}
