package mods.eln.item.electricalitem;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;

public class ElectricalTool extends GenericItemUsingDamageDescriptor implements IItemEnergyBattery{

	public ElectricalTool(
			String name,
			float strengthOn,float strengthOff,
			double energyStorage,double energyPerBlock,double chargePower
			
			) {
		super(name);

		this.chargePower = chargePower;
		this.energyPerBlock = energyPerBlock;
		this.energyStorage = energyStorage;
		this.strengthOn = strengthOn;
		this.strengthOff = strengthOff;
		
		rIcon = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + ".png");
	}
	int light,range;
	double energyStorage, energyPerBlock, chargePower;

	float strengthOff,strengthOn;

	ResourceLocation rIcon;
	
	@Override
	public boolean onBlockDestroyed(ItemStack stack, World w, int blockId, int x, int y, int z, EntityLivingBase entity) {
		Block b = Block.blocksList[blockId];
		if(getStrVsBlock(stack, b) == strengthOn){
			double e = getEnergy(stack) - energyPerBlock;
			if(e < 0) e = 0;
			setEnergy(stack, e);
		}
		System.out.println("destroy");
		return false;
	}
	
   // public static final Block[] blocksEffectiveAgainst = new Block[] {Block.cobblestone, Block.stoneDoubleSlab, Block.stoneSingleSlab, Block.stone, Block.sandStone, Block.cobblestoneMossy, Block.oreIron, Block.blockIron, Block.oreCoal, Block.blockGold, Block.oreGold, Block.oreDiamond, Block.blockDiamond, Block.ice, Block.netherrack, Block.oreLapis, Block.blockLapis, Block.oreRedstone, Block.oreRedstoneGlowing, Block.rail, Block.railDetector, Block.railPowered, Block.railActivator};
    public static final Block[] blocksEffectiveAgainst = new Block[] {Block.grass, Block.dirt, Block.sand, Block.gravel, Block.snow, Block.blockSnow, Block.blockClay, Block.tilledField, Block.slowSand, Block.mycelium};

	//@Override
	//public abstract float getStrVsBlock(ItemStack stack, Block block);

	
	public float getStrength(ItemStack stack){
		return getEnergy(stack) >= energyPerBlock ? strengthOn : strengthOff;
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound("itemStackNBT");
		nbt.setDouble("energy",0);
		nbt.setBoolean("powerOn",false);
		nbt.setInteger("rand", (int) (Math.random()*0xFFFFFFF));
		return nbt;
	}
	

	
	boolean getPowerOn(ItemStack stack)
	{
		return getNbt(stack).getBoolean("powerOn");
	}
	void setPowerOn(ItemStack stack,boolean value)
	{
		getNbt(stack).setBoolean("powerOn",value);
	}
	

	/*
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4,
			boolean par5) {
		
		if(world.isRemote == false && entity instanceof EntityPlayer && ((EntityPlayer) entity).inventory.getCurrentItem() == stack && Eln.playerManager.get((EntityPlayer) entity).getInteractRise()){
			boolean status = ! getPowerOn(stack);
			if(status)
				((EntityPlayer) entity).addChatMessage("Flashlight ON");
			else
				((EntityPlayer) entity).addChatMessage("Flashlight OFF");
			setPowerOn(stack, status);
		}
		super.onUpdate(stack, world, entity, par4, par5);
	}
*/
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add(Utils.plotEnergy("Energy Stored:", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
		//list.add("Power button is " + (getPowerOn(itemStack) ? "ON" : "OFF"));
	}


	public double getEnergy(ItemStack stack)
	{
		return getNbt(stack).getDouble("energy");
	}
	public void setEnergy(ItemStack stack,double value)
	{
		getNbt(stack).setDouble("energy",value);
	}

	@Override
	public double getEnergyMax(ItemStack stack) {
		// TODO Auto-generated method stub
		return energyStorage;
	}

	@Override
	public double getChargePower(ItemStack stack) {
		// TODO Auto-generated method stub
		return chargePower;
	}

	@Override
	public double getDischagePower(ItemStack stack) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPriority(ItemStack stack) {
		// TODO Auto-generated method stub
		return 0;
	}



	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		if(type == ItemRenderType.INVENTORY)
			return false;
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {		
		if(type == ItemRenderType.INVENTORY)		
			Utils.drawEnergyBare(type,(float) (getEnergy(item)/getEnergyMax(item)));
		Utils.drawIcon(type,rIcon);
	}

	@Override
	public void electricalItemUpdate(ItemStack stack,
			double time) {
		
	}

}
