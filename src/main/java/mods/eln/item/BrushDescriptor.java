package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class BrushDescriptor extends GenericItemUsingDamageDescriptor {

	private final ResourceLocation icon;
	private static ResourceLocation dryOverlay = new ResourceLocation("eln", "textures/items/brushdryoverlay.png");

	public BrushDescriptor(String name) {
		super( name);
		icon = new ResourceLocation("eln", "textures/items/" + name.toLowerCase().replace(" ", "") + ".png");
	}

	@Override
	public String getName(ItemStack stack) {
		int color = getColor(stack),life = getLife(stack);
		if(color == 15 && life == 0)
			return "Empty " + super.getName(stack);
		return super.getName(stack);
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}

	public int getColor(ItemStack stack) {
		return stack.getItemDamage() & 0xF;
	}

	public int getLife(ItemStack stack) {
		return stack.getTagCompound().getInteger("life");
	}

	public void setColor(ItemStack stack,int color) {
		stack.setItemDamage((stack.getItemDamage() & ~0xF) | color);
	}

	public void setLife(ItemStack stack,int life) {
		stack.getTagCompound().setInteger("life", life);
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("life", 32);
		return nbt;
	}
	
	@Override
	public ItemStack newItemStack(int size) {
		return super.newItemStack(size);
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		if (itemStack != null)
			list.add(tr("Can paint %1$ blocks", itemStack.getTagCompound().getInteger("life")));
	}
	
	public boolean use(ItemStack stack, EntityPlayer entityPlayer) {
		int life = stack.getTagCompound().getInteger("life");
		if(life != 0) {
			--life;
			stack.getTagCompound().setInteger("life", life);
			return true;
		}
		else
			Utils.addChatMessage(entityPlayer, tr("Brush is dry"));
		return false;
	}

	@Override
	public boolean use2DIcon() {
		return true;
	}

	@Override
	public boolean handleRenderType(ItemStack item, IItemRenderer.ItemRenderType type) {
		return type == IItemRenderer.ItemRenderType.INVENTORY;
	}

	@Override
	public boolean shouldUseRenderHelper(IItemRenderer.ItemRenderType type, ItemStack item, IItemRenderer.ItemRendererHelper helper) {
		return type != IItemRenderer.ItemRenderType.INVENTORY;
	}

	@Override
	public void renderItem(IItemRenderer.ItemRenderType type, ItemStack item, Object... data) {
		if (type == IItemRenderer.ItemRenderType.INVENTORY) {
			UtilsClient.drawIcon(type, icon);
			GL11.glColor4f(1, 1, 1, 0.75f - 0.75f * getLife(item) / 32f);
			UtilsClient.drawIcon(type, dryOverlay);
			GL11.glColor3f(1, 1, 1);
		} else {
			super.renderItem(type, item, data);
		}
	}
}
