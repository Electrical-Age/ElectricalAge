package mods.eln.misc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.Color;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameRegistry;

import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.SixNodeEntity;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;


public class Utils {
	
	public static double minecraftDay = 60*24;
	
	static String floatToStr(double f,int high,int low)
	{
		String temp = "";
		for(int idx = 0;idx<high;idx++) temp = temp + "0";
		 temp = temp + ".";
		for(int idx = 0;idx<low;idx++) temp = temp + "0";
		
		String str = new DecimalFormat(temp).format(f);
		int idx = 0;
		char [] ch = str.toCharArray();
		while(true)
		{
			if(str.length() == idx)break;
			if(ch[idx] == '.') { ch[idx-1] = '0'; break; }
			if(ch[idx] != '0' && ch[idx]  != ' ') break;
			ch[idx] = '_';
			idx++;
		}
		
		return new String(ch);
	}
	
	public static boolean isTheClass(Object o,Class c)
	{
		if(o.getClass() == c) return true;
		for (Class classIterator = o.getClass().getSuperclass(); classIterator != null;classIterator = classIterator.getSuperclass())
		//for (Class classIterator : o.getClass().getClass())
		{
			if(classIterator == c)
			{
				return true;
			}
		}
		return false;
	}

	public static boolean hasTheInterface(Object o,Class c)
	{

		for(Class i :  o.getClass().getInterfaces())
		{
			if(i == c) return true;
		}
		return false;
	}

	public static Direction entityLivingViewDirection(EntityLiving entityLiving)
	{
        if(entityLiving.rotationPitch>45) return Direction.YN;
        if(entityLiving.rotationPitch<-45) return Direction.YP;
        int dirx = MathHelper.floor_double((double)(entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        if(dirx == 3) return Direction.XP;
        if(dirx == 0) return Direction.ZP;
        if(dirx == 1) return Direction.XN;
        return Direction.ZN;
        
	}
	public static Direction entityLivingHorizontalViewDirection(EntityLiving entityLiving)
	{
        int dirx = MathHelper.floor_double((double)(entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        if(dirx == 3) return Direction.XP;
        if(dirx == 0) return Direction.ZP;
        if(dirx == 1) return Direction.XN;
        return Direction.ZN;
        
	}
	
	

    /**
     * Returns the number of ticks that the supplied fuel item will keep the furnace burning, or 0 if the item isn't
     * fuel
     */
	/*
    public static int getItemBurnTime(ItemStack par0ItemStack)
    {
        if (par0ItemStack == null)
        {
            return 0;
        }
        else
        {
            int var1 = par0ItemStack.getItem().shiftedIndex;
            Item var2 = par0ItemStack.getItem();

            if (par0ItemStack.getItem() instanceof ItemBlock && Block.blocksList[var1] != null)
            {
                Block var3 = Block.blocksList[var1];

                if (var3 == Block.woodSingleSlab)
                {
                    return 150;
                }

                if (var3.blockMaterial == Material.wood)
                {
                    return 300;
                }
            }

            if (var2 instanceof ItemTool && ((ItemTool) var2).getToolMaterialName().equals("WOOD")) return 200;
            if (var2 instanceof ItemSword && ((ItemSword) var2).func_77825_f().equals("WOOD")) return 200;
            if (var2 instanceof ItemHoe && ((ItemHoe) var2).func_77842_f().equals("WOOD")) return 200;
            if (var1 == Item.stick.shiftedIndex) return 100;
            if (var1 == Item.coal.shiftedIndex) return 1600;
            if (var1 == Item.bucketLava.shiftedIndex) return 20000;
            if (var1 == Block.sapling.blockID) return 100;
            if (var1 == Item.blazeRod.shiftedIndex) return 2400;
            return GameRegistry.getFuelValue(par0ItemStack);
        }
    }*/
    
    public static final double burnTimeToEnergyFactor = 1.0;
    
    public static double getItemEnergie(ItemStack par0ItemStack)
    {
    	return burnTimeToEnergyFactor * 80000.0 / 1600 * TileEntityFurnace.getItemBurnTime(par0ItemStack);
    }
    
    public static double getCoalEnergyReference()
    {
    	return  burnTimeToEnergyFactor * 80000.0;
    }
    
    
    
	public static byte booleanSideMaskToByte(boolean[] side)
	{
		byte b = 0;
		if(side[0]) b |= 1<<0;
		if(side[1]) b |= 1<<1;
		if(side[2]) b |= 1<<2;
		if(side[3]) b |= 1<<3;
		if(side[4]) b |= 1<<4;
		if(side[5]) b |= 1<<5;
		return b;
	}
	public static void ByteTobooleanSideMask(byte b,boolean[] side)
	{
		for(int idx = 0;idx<6;idx++)
		{
			side[idx] = false;
			if(((b>>idx)&1)!=0) side[idx] = true; 
		}
	}

	public static String plotValue(double value,String unit)
	{
		double valueAbs = Math.abs(value);
		if(valueAbs < 0.001)
		{
			return "0" + unit;
			//return String.format("%1.5f", value*1000)+ "m" + unit;
		}
		else if(valueAbs < 0.00999)
		{
			return String.format("%1.2f", value*1000)+ "m" + unit;
		}
		else if(valueAbs < 0.0999)
		{
			return String.format("%2.1f", value*1000)+ "m" + unit;
		}
		else if(valueAbs < 0.999)
		{
			return String.format("%3.0f", value*1000)+ "m" + unit;
		}
		else if(valueAbs < 9.99)
		{
			return String.format("%1.2f", value)+ "" + unit ;
		}
		else if(valueAbs < 99.9)
		{
			return String.format("%2.1f", value)+ "" + unit ;
		}
		else if(valueAbs < 999)
		{
			return String.format("%3.0f", value)+ "" + unit ;
		}
		else if(valueAbs < 9999)
		{
			return String.format("%1.2f", value/1000.0)+ "K" + unit;
		}
		else if(valueAbs < 99999)
		{
			return String.format("%2.1f", value/1000.0)+ "K" + unit;
		}
		else// if(value < 1000000)
		{
			return String.format("%3.0f", value/1000.0)+ "K" + unit;
		}	
	}
	
	public static String plotVolt(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		return header + plotValue(value, "V  ");
	}
	public static String plotAmpere(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		return header + plotValue(value, "A  ");
	}
	public static String plotCelsius(String header,double value)
	{
		value += PhysicalConstant.Tref - PhysicalConstant.TCelsius;
		if(header.equals("") == false) header += " ";
		return header + plotValue(value, "C ");
	}
	public static String plotPercent(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		if(value >= 1.0)
			return header +  String.format("%3.0f", value * 100.0)+ "%   ";
		else
			return header +  String.format("%3.1f", value * 100.0)+ "%   ";

	}
	public static String plotEnergy(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		return header + plotValue(value, "J  ");
	}
	public static String plotPower(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		return header + plotValue(value, "W  ");
	}
	
	public static String plotUIP(double U,double I)
	{
		return plotVolt("U", U) + plotAmpere("I", I) + plotPower("P", U*I);
	}
	

	
	public static String plotTime(double value)
	{
		String str = "";
		if(value == 0.0)
			return str + "0'";
		int h,mn,s;
		h = (int) (value/3600); value = value % 3600;
		mn = (int) (value/60); value = value % 60;
		s = (int) (value/1);
		if(h != 0) str += h + "h";
		if(mn != 0) str += mn + "'";
		if(s != 0) str += s + "''";
		return str;
	}
		
	public static String plotTime(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		return header + plotTime(value);

	}
		
	
	public static void readFromNBT(NBTTagCompound nbt, String str,IInventory inventory) {
        NBTTagList var2 = nbt.getTagList(str);
        

        for (int var3 = 0; var3 < var2.tagCount(); ++var3)
        {
            NBTTagCompound var4 = (NBTTagCompound)var2.tagAt(var3);
            int var5 = var4.getByte("Slot") & 255;

            if (var5 >= 0 && var5 < inventory.getSizeInventory())
            {
            	inventory.setInventorySlotContents(var5, ItemStack.loadItemStackFromNBT(var4));
            }
        }
	}

	public static void writeToNBT(NBTTagCompound nbt, String str,IInventory inventory) {
		NBTTagList var2 = new NBTTagList();

        for (int var3 = 0; var3 < inventory.getSizeInventory(); ++var3)
        {
            if (inventory.getStackInSlot(var3) != null)
            {
                NBTTagCompound var4 = new NBTTagCompound();
                var4.setByte("Slot", (byte)var3);
                inventory.getStackInSlot(var3).writeToNBT(var4);
                var2.appendTag(var4);
            }
        }

        nbt.setTag(str, var2);
   
	}

	
	public static void sendPacketToServer(ByteArrayOutputStream bos)
	{

	    Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = Eln.channelName;
        packet.data = bos.toByteArray();
        packet.length = bos.size();
        	    	
    	PacketDispatcher.sendPacketToServer(packet);		
	}
	public static void sendPacketToClient(ByteArrayOutputStream bos,Player player)
	{

	    Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = Eln.channelName;
        packet.data = bos.toByteArray();
        packet.length = bos.size();
        	    	
    	PacketDispatcher.sendPacketToPlayer(packet,player);		
	}
	
    public static void bindTextureByName(String par1Str)
    {
    	Minecraft.getMinecraft().renderEngine.bindTexture(par1Str);
    }
    
    
    //private static Color[] dyeColors
    
   // public Color getDyeColor(ItemStack stack)
   // {
    	//ItemDye.dyeColors[stack.getItemDamage()];
   // }
    
    public static void setGlColorFromDye(int damage)
    {
    	int color = ItemDye.dyeColors[damage];
    	float gain = 1.0f;
    	switch(damage)
    	{
    	default: GL11.glColor3f(0.0f*gain, 0.0f*gain, 0.0f*gain); break;
    	case 0: GL11.glColor3f(0.2f*gain, 0.2f*gain, 0.2f*gain); break; //black
    	case 1: GL11.glColor3f(1.0f*gain, 0.0f*gain, 0.0f*gain); break; //red
    	case 2: GL11.glColor3f(0.2f*gain, 0.5f*gain, 0.1f*gain); break; //green
    	case 3: GL11.glColor3f(0.3f*gain, 0.2f*gain, 0.1f*gain); break; // braoun
    	case 4: GL11.glColor3f(0.2f*gain, 0.2f*gain, 1.0f*gain); break; // blue
    	case 5: GL11.glColor3f(0.7f*gain, 0.2f*gain, 1.0f*gain); break; //purple
    	case 6: GL11.glColor3f(0.2f*gain, 0.7f*gain, 0.9f*gain); break;
    	case 7: GL11.glColor3f(0.7f*gain, 0.7f*gain, 0.7f*gain); break;
    	case 8: GL11.glColor3f(0.4f*gain, 0.4f*gain, 0.4f*gain); break;
    	case 9: GL11.glColor3f(1.0f*gain, 0.5f*gain, 0.5f*gain); break;
    	case 10: GL11.glColor3f(0.0f*gain, 1.0f*gain, 0.0f*gain); break;
    	case 11: GL11.glColor3f(0.9f*gain, 0.8f*gain, 0.1f*gain); break;
    	case 12: GL11.glColor3f(0.4f*gain, 0.5f*gain, 1.0f*gain); break;
    	case 13: GL11.glColor3f(0.9f*gain, 0.3f*gain, 0.9f*gain); break;
    	case 14: GL11.glColor3f(1.0f*gain, 0.6f*gain, 0.3f*gain); break;
    	case 15: GL11.glColor3f(1.0f*gain, 1.0f*gain, 1.0f*gain); break;
    	
    	}
    	
    	//GL11.glColor3f(((color>>16) & 0xFF)/255f, ((color>>7) & 0xFF)/255f, ((color>>0) & 0xFF)/255f);
    }
    
    
    
    public static double getWind(World world)
    {
    	return Math.cos(world.getCelestialAngleRadians(0)*2)*5 + 10;
    }
    
    public static void dropItem(ItemStack itemStack,int x,int y,int z,World world)
    {
    	if(itemStack == null) return;
        if (world.getGameRules().getGameRuleBooleanValue("doTileDrops"))
        {
            float var6 = 0.7F;
            double var7 = (double)(world.rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            double var9 = (double)(world.rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            double var11 = (double)(world.rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            EntityItem var13 = new EntityItem(world, (double)x + var7, (double)y + var9, (double)z + var11, itemStack);
            var13.delayBeforeCanPickup = 10;
            world.spawnEntityInWorld(var13);
        }
    }
    public static  void dropItem(ItemStack itemStack,Coordonate coordonate)
    {
    	dropItem(itemStack, coordonate.x, coordonate.y, coordonate.z, coordonate.world());
    }
    
    
    
    public static boolean tryPutStackInInventory(ItemStack stack,IInventory inventory)
    {
    	for(int idx = 0;idx<inventory.getSizeInventory();idx++)
    	{
    		ItemStack targetStack = inventory.getStackInSlot(idx);
    		if(targetStack == null)
    		{
    			inventory.setInventorySlotContents(idx, stack.copy());
    			stack.stackSize = 0;
    			return true;
    		}
    		else if(targetStack.isItemEqual(stack))
    		{
    			int sizeLast = targetStack.stackSize;
    			//inventory.decrStackSize(idx, -stack.stackSize);
    			int transferMax = inventory.getInventoryStackLimit() - targetStack.stackSize;
    			if(transferMax > 0)
    			{
    				int transfer = stack.stackSize;
    				if(transfer > transferMax) transfer = transferMax;
    				inventory.decrStackSize(idx, -transfer);
    				stack.stackSize-=transfer;			
    			}
    			
    			if(stack.stackSize == 0)
    			{
    				return true;
    			}
    		}
    	}
    	
    	
    	return false;
    }
    
    
    

    public static boolean canPutStackInInventory(ItemStack[] stackList,IInventory inventory,int[] slotsIdList)
    {
    	int limit = inventory.getInventoryStackLimit();
    	ItemStack[] outputStack = new ItemStack[slotsIdList.length];
    	ItemStack[] inputStack = new ItemStack[stackList.length];
    	
    	for(int idx = 0;idx < outputStack.length;idx++)
    	{
    		if(inventory.getStackInSlot(slotsIdList[idx]) != null)
    			outputStack[idx] = inventory.getStackInSlot(slotsIdList[idx]).copy();
    	}
    	for(int idx = 0;idx < stackList.length;idx++)
    	{
    		inputStack[idx] = stackList[idx].copy();
    	}
    	
    	
    	boolean oneStackDone;
    	for(ItemStack stack : inputStack)
    	{
    		//if(stack == null) continue;
    		oneStackDone = false;
        	for(int idx = 0;idx < slotsIdList.length;idx++)
        	{
        		ItemStack targetStack = outputStack[idx];
       
        		if(targetStack == null)
        		{
        			outputStack[idx] = stack;
        			oneStackDone = true;
        			break;
        		}
        		else if(targetStack.isItemEqual(stack))
        		{
        			int sizeLast = targetStack.stackSize;
        			//inventory.decrStackSize(idx, -stack.stackSize);
        			int transferMax = limit - targetStack.stackSize;
        			if(transferMax > 0)
        			{
        				int transfer = stack.stackSize;
        				if(transfer > transferMax) transfer = transferMax;
        				outputStack[idx].stackSize += transfer;
        				stack.stackSize-=transfer;			
        			}
        			
        			if(stack.stackSize == 0)
        			{
        				oneStackDone = true;
        				break;
        			}
        		}
        	}
        	
        	if(oneStackDone == false) return false;
        }    		
        return true;
    }


    public static boolean tryPutStackInInventory(ItemStack[] stackList,IInventory inventory,int[] slotsIdList)
    {
    	int limit = inventory.getInventoryStackLimit();

    	for(ItemStack stack : stackList)
    	{
        	for(int idx = 0;idx < slotsIdList.length;idx++)
        	{
        		ItemStack targetStack = inventory.getStackInSlot(slotsIdList[idx]);
        		if(targetStack == null)
        		{
        			inventory.setInventorySlotContents(slotsIdList[idx], stack.copy());
        			stack.stackSize = 0;
        			break;
        		}
        		else if(targetStack.isItemEqual(stack))
        		{
        			int sizeLast = targetStack.stackSize;
        			//inventory.decrStackSize(idx, -stack.stackSize);
        			int transferMax = limit - targetStack.stackSize;
        			if(transferMax > 0)
        			{
        				int transfer = stack.stackSize;
        				if(transfer > transferMax) transfer = transferMax;
        				inventory.decrStackSize(slotsIdList[idx], -transfer);
        				stack.stackSize-=transfer;				
        			}
        			
        			if(stack.stackSize == 0)
        			{
        				break;
        			}
        		}
        	}
        	
        }    		
        return true;
    }

    public static double voltageMageFactor = 0.1;
    public static double voltageMargeFactorSub(double value)
    {
    	if(value > 1 + voltageMageFactor)
    	{
    		return value -  voltageMageFactor;
    	}
    	else if(value > 1)
    	{
    		return  1;
    	}
    	return value;
    }

	public static float distanceFromClientPlayer(World world, int xCoord, int yCoord,int zCoord) {
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		
		return (float) Math.sqrt(	(xCoord - player.posX)*(xCoord - player.posX) 
							+ (yCoord - player.posY) * (yCoord - player.posY)
							+ (zCoord - player.posZ) * (zCoord - player.posZ));
	}

	public static float distanceFromClientPlayer(SixNodeEntity tileEntity) {
		return distanceFromClientPlayer(tileEntity.worldObj,tileEntity.xCoord,tileEntity.yCoord,tileEntity.zCoord);
	}

	public static void bindGuiTexture(String string) {
		Minecraft.getMinecraft().renderEngine.bindTexture("/mods/eln/sprites/gui/" + string);
	}

	public static void drawGuiBackground(String string,GuiScreen guiScreen,int xSize,int ySize) {
		Utils.bindGuiTexture(string);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int x = (guiScreen.width - xSize) / 2;
		int y = (guiScreen.height - ySize) / 2;
		guiScreen.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}

    /*
    public float frameTime()
    {
    	float time = Minecraft.getMinecraft().entityRenderer.performanceToFps(par0)
    }*/
	
	
	

} 
