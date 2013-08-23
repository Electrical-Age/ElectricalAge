package mods.eln.generic;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import net.minecraftforge.common.ISpecialArmor;

import org.lwjgl.opengl.GL11;

public class SharedItem extends GenericItemUsingDamage<GenericItemUsingDamageDescriptor> implements IItemRenderer , ISpecialArmor{

	public SharedItem(int par1) {
		super(par1);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return getDescriptor(item).handleRenderType(item, type);
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
	}
	/*
    public boolean isValidArmor(ItemStack stack, int armorType, Entity entity)
    {
    	return true;
    }
    
	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, int slot, int layer)
	{
		return "eln:textures/armor/copper_layer_1.png";
	}
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, int armorSlot)
    {
        return new ModelBiped();
    }*/
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		Minecraft.getMinecraft().mcProfiler.startSection("SharedItem");

		switch(type)
		{
		case ENTITY:

	    //    GL11.glScalef(0.5f, 0.5f, 0.5f);
	    //    GL11.glTranslatef(0.f,-0.5f,0.5f); 	
		//	GL11.glRotatef(90,0f,1f,0f);  
	        	
		//	GL11.glTranslatef(0.00f, 0.3f, 0.0f);
			break;
		case EQUIPPED:
			
			//GL11.glTranslatef(0.50f, 1, 0.5f);
			//GL11.glRotatef(130,1f,0.0f,1f);  
        	
			break;
		case FIRST_PERSON_MAP:
			//GL11.glTranslatef(0.f,-0.5f,0.5f); 
			break;
		case INVENTORY:
			//GL11.glScalef(1.0f, 1f, 1.0f);
			//GL11.glRotatef(45, 0, 1, 0);
			break;
		default:
			break;
		}
		getDescriptor(item).renderItem(type, item, data);
		
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	@Override
	public ArmorProperties getProperties(EntityLivingBase player,
			ItemStack armor, DamageSource source, double damage, int slot) {
		// TODO Auto-generated method stub
		return new ArmorProperties(10, 1.0, 10000);
	}

	@Override
	public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
		// TODO Auto-generated method stub
		return 4;
	}

	@Override
	public void damageArmor(EntityLivingBase entity, ItemStack stack,
			DamageSource source, int damage, int slot) {
		// TODO Auto-generated method stub
		
	}	

}
