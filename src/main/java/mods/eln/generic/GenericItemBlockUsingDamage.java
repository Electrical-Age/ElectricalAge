package mods.eln.generic;

import mods.eln.i18n.I18N;

import mods.eln.compat.GameRegistryCompat;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
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

public class GenericItemBlockUsingDamage<Descriptor extends GenericItemBlockUsingDamageDescriptor> extends ItemBlock {

    public Hashtable<Integer, Descriptor> subItemList = new Hashtable<Integer, Descriptor>();
    public ArrayList<Integer> orderList = new ArrayList<Integer>();
    public ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>();

    public Descriptor defaultElement = null;

    public GenericItemBlockUsingDamage(Block b) {
        super(b);
        setHasSubtypes(true);
    }

    public void setDefaultElement(Descriptor descriptor) {
        defaultElement = descriptor;
    }

    public void doubleEntry(int src, int dst) {
        subItemList.put(dst, subItemList.get(src));
    }

    public void addDescriptor(int damage, Descriptor descriptor) {
        subItemList.put(damage, descriptor);
        ItemStack stack = new ItemStack(this, 1, damage);
        stack.setTagCompound(descriptor.getDefaultNBT());
        //LanguageRegistry.addName(stack, descriptor.name);
        orderList.add(damage);
        descriptors.add(descriptor);
        descriptor.setParent(this, damage);
        GameRegistryCompat.registerCustomItemStack(descriptor.name, descriptor.newItemStack(1));
    }

    public void addWithoutRegistry(int damage, Descriptor descriptor) {
        subItemList.put(damage, descriptor);
        ItemStack stack = new ItemStack(this, 1, damage);
        stack.setTagCompound(descriptor.getDefaultNBT());
        descriptor.setParent(this, damage);
    }

    public Descriptor getDescriptor(int damage) {
        return subItemList.get(damage);
    }

    public Descriptor getDescriptor(ItemStack itemStack) {
        if (Utils.isEmpty(itemStack)) return defaultElement;
        if (itemStack.getItem() != this) return defaultElement;
        return getDescriptor(itemStack.getMetadata());
    }

	/*
    @Override
	@SideOnly(Side.CLIENT)
	public int getIconFromDamage(int damage) {
		return getDescriptor(damage).getIconId();
		
	}
	//caca1.5.1
	@Override
	public String getTextureFile () {
		return CommonProxy.ITEMS_PNG;
	}
	@Override
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + "." + getDescriptor(itemstack).name;
	}
	*/

	/*@Override
    public String getItemStackDisplayName(ItemStack par1ItemStack) {
		Descriptor desc = getDescriptor(par1ItemStack);
		if(desc == null) return "Unknown";
        return desc.getName(par1ItemStack);
    }*/

    @Override
    public String getTranslationKey(ItemStack par1ItemStack) {
        Descriptor desc = getDescriptor(par1ItemStack);
        if (desc == null) {
            return this.getClass().getName();
        } else {
            return I18N.langKey(desc.name);
        }
    }

    // TODO(1.12 WP6 icon): getIconFromDamage/registerIcons removed (item models instead).

    @Override
    public void getSubItems(CreativeTabs tabs, NonNullList<ItemStack> list) {
        // 1.12 asks every item for every tab; 1.7.10 only asked for the item's own tab (and search).
        if (!isInCreativeTab(tabs)) return;
        // You can also take a more direct approach and do each one individual but I prefer the lazy / right way
        //for(Entry<Integer, Descriptor> entry : subItemList.entrySet())
        for (int id : orderList) {
            ItemStack stack = Utils.newItemStack(this, 1, id);
            stack.setTagCompound(subItemList.get(id).getDefaultNBT());
            list.add(stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        Descriptor desc = getDescriptor(itemStack);
        if (desc == null) return;
        EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
        boolean par4 = flag.isAdvanced();
        List listFromDescriptor = new ArrayList();
        desc.addInformation(itemStack, entityPlayer, listFromDescriptor, par4);
        UtilsClient.showItemTooltip(listFromDescriptor, list);
    }

    public boolean onEntityItemUpdate(EntityItem entityItem) {
        Descriptor desc = getDescriptor(entityItem.getItem());
        if (desc != null) return desc.onEntityItemUpdate(entityItem);
        return false;
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        Descriptor desc = getDescriptor(stack);
        if (desc != null && desc.onItemUseFirst(stack, player)) return EnumActionResult.SUCCESS;
        return EnumActionResult.PASS;
    }
}
