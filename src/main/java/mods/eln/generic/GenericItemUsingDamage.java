package mods.eln.generic;

import mods.eln.i18n.I18N;

import mods.eln.compat.GameRegistryCompat;


import mods.eln.misc.Utils;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.misc.UtilsClient;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import javax.annotation.Nullable;
import net.minecraft.world.World;

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

    public void setDefaultElement(Descriptor descriptor) {
        defaultElement = descriptor;
    }

    public void addWithoutRegistry(int damage, Descriptor descriptor) {
        subItemList.put(damage, descriptor);
        // TODO(1.12 WP13): LanguageRegistry.addName(stack, descriptor.name) is gone; display names come from
        // lang keys "<getTranslationKey(stack)>.name".
        descriptor.setParent(this, damage);
    }

    public void addElement(int damage, Descriptor descriptor) {
        subItemList.put(damage, descriptor);
        // TODO(1.12 WP13): LanguageRegistry.addName(stack, descriptor.name), see addWithoutRegistry
        orderList.add(damage);
        descriptor.setParent(this, damage);
        GameRegistryCompat.registerCustomItemStack(descriptor.name, descriptor.newItemStack(1));
    }

    public Descriptor getDescriptor(int damage) {
        return subItemList.get(damage);
    }

    public Descriptor getDescriptor(ItemStack itemStack) {
        if (Utils.isEmpty(itemStack))
            return defaultElement;
        if (itemStack.getItem() != this)
            return defaultElement;
        return getDescriptor(itemStack.getMetadata());
    }

    /** 1.7.10 onItemRightClick(stack, world, player) returned the new held stack; PASS when unchanged. */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        ItemStack s = p.getHeldItem(hand);
        Descriptor desc = getDescriptor(s);
        if (desc == null)
            return new ActionResult<ItemStack>(EnumActionResult.PASS, s);
        ItemStack result = desc.onItemRightClick(s, w, p);
        if (result == null) result = ItemStack.EMPTY;
        return new ActionResult<ItemStack>(result == s ? EnumActionResult.PASS : EnumActionResult.SUCCESS, result);
    }

	/*//caca1.5.1
    @Override
	@SideOnly(Side.CLIENT)
	public int getIconFromDamage(int damage) {
	return getDescriptor(damage).getIconId();
	
	}
	@Override
	public String getTextureFile () {
	return CommonProxy.ITEMS_PNG;
	}
	@Override
	public String getItemNameIS(ItemStack itemstack) {
	return getItemName() + "." + getDescriptor(itemstack).name;
	}

	/*
	@Override
	public String getUnlocalizedNameInefficiently(ItemStack par1ItemStack) {
		return "trololol";
	}
	*/

    @Override
    public String getTranslationKey(ItemStack par1ItemStack) {
        Descriptor desc = getDescriptor(par1ItemStack);
        if (desc != null && desc.name != null) {
            return I18N.langKey(desc.name);
        } else {
            return null;
        }
    }

	/*
	@Override
	public String getItemStackDisplayName(ItemStack par1ItemStack) {
		Descriptor desc = getDescriptor(par1ItemStack);
		if (desc == null)
			return "NullItem";
		return desc.getName(par1ItemStack);
	}
	*/

    // TODO(1.12 WP6 icon): getIconFromDamage/registerIcons removed (item models instead).

    @Override
    public void getSubItems(CreativeTabs tabs, NonNullList<ItemStack> list) {
        // 1.12 asks every item for every tab; 1.7.10 only asked for the item's own tab (and search).
        if (!isInCreativeTab(tabs)) return;
        // You can also take a more direct approach and do each one individual but I prefer the lazy / right way
        for (int id : orderList) {
            subItemList.get(id).getSubItems(list);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
        boolean par4 = flag.isAdvanced();
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
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float vx, float vy, float vz) {
        ItemStack stack = player.getHeldItem(hand);
        GenericItemUsingDamageDescriptor d = getDescriptor(stack);
        if (d == null)
            return EnumActionResult.PASS;
        return d.onItemUse(stack, player, world, pos.getX(), pos.getY(), pos.getZ(), facing.getIndex(), vx, vy, vz)
            ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        GenericItemUsingDamageDescriptor d = getDescriptor(stack);
        if (d == null)
            return super.onEntitySwing(entityLiving, stack);
        return d.onEntitySwing(entityLiving, stack);
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {
        GenericItemUsingDamageDescriptor d = getDescriptor(itemstack);
        if (d == null)
            return super.onBlockStartBreak(itemstack, pos, player);
        return d.onBlockStartBreak(itemstack, pos.getX(), pos.getY(), pos.getZ(), player);
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
    public float getDestroySpeed(ItemStack stack, IBlockState state) { //getStrVsBlock
        GenericItemUsingDamageDescriptor d = getDescriptor(stack);
        if (d == null)
            return 0.2f;
        return d.getStrVsBlock(stack, state.getBlock());
    }

    @Override
    public boolean canHarvestBlock(IBlockState state, ItemStack item) {
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World w, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        Block block = state.getBlock();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (w.isRemote) {
            return false;
        }

        GenericItemUsingDamageDescriptor d = getDescriptor(stack);

        if (d == null)
            return true;
        return d.onBlockDestroyed(stack, w, block, x, y, z, entity);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
        GenericItemUsingDamageDescriptor d = getDescriptor(item);
        if (d == null)
            return super.onDroppedByPlayer(item, player);
        return d.onDroppedByPlayer(item, player);
    }
}
