package mods.eln.misc;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.Color;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.value.ModbusValue;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.PlayerManager;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.gui.ISlotSkin;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.ITileEntitySpawnClient;
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
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;


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

	public static Direction entityLivingViewDirection(EntityLivingBase entityLiving)
	{
        if(entityLiving.rotationPitch>45) return Direction.YN;
        if(entityLiving.rotationPitch<-45) return Direction.YP;
        int dirx = MathHelper.floor_double((double)(entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        if(dirx == 3) return Direction.XP;
        if(dirx == 0) return Direction.ZP;
        if(dirx == 1) return Direction.XN;
        return Direction.ZN;
        
	}
	public static Direction entityLivingHorizontalViewDirection(EntityLivingBase entityLiving)
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
		return header + plotValue(value, "\u00B0C ");
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
	public static String plotOhm(String header,double value)
	{
		if(header.equals("") == false) header += " ";
		return header + plotValue(value, "ohm  ");
	}
	
	public static String plotUIP(double U,double I)
	{
		return plotVolt("U", U) + plotAmpere("I", I) + plotPower("P", Math.abs(U*I));
	}
	

	
	public static String plotTime(double value)
	{
		String str = "";
		if(value == 0.0)
			return str + "0''";
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
    	//Minecraft.getMinecraft().renderEngine.func_110577_a(new ResourceLocation(par1Str));
    	Minecraft.getMinecraft().renderEngine.func_110577_a(new ResourceLocation("eln",par1Str));
    }
    public static void bindTexture(ResourceLocation ressource)
    {
    	//Minecraft.getMinecraft().renderEngine.func_110577_a(new ResourceLocation(par1Str));
    	Minecraft.getMinecraft().renderEngine.func_110577_a(ressource);
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
    
    
    
    public static double getWind(World world,int y)
    {
    	float factor = 1f;
    	if(world.isRaining()) factor *= 1.2;
    	if(world.isThundering()) factor *= 1.25;
    	return Math.max(0.0,(Math.cos(world.getCelestialAngleRadians(0)*2)*5 + 4 + y/20.0)*factor);
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
/*
	public static void bindGuiTexture(String string) {
		Utils.bindTextureByName("/sprites/gui/" + string);
	}*/

	public static void drawGuiBackground(ResourceLocation ressource,GuiScreen guiScreen,int xSize,int ySize) {
		Utils.bindTexture(ressource);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int x = (guiScreen.width - xSize) / 2;
		int y = (guiScreen.height - ySize) / 2;
		guiScreen.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}


	public static void serialiseItemStack(DataOutputStream stream,ItemStack stack) throws IOException
	{
		
		if((stack) == null)
		{
			stream.writeShort(-1);
			stream.writeShort(-1);
		}
		else
		{
			stream.writeShort(stack.itemID);
			stream.writeShort(stack.getItemDamage());				
		}
	}
	public static ItemStack unserialiseItemStack(DataInputStream stream) throws IOException
	{
		short id,damage;
		id = stream.readShort();
		damage = stream.readShort();
		if(id == -1)
			return null;
		return new ItemStack(id,1,damage);
	}
	public static   EntityItem unserializeItemStackToEntityItem(DataInputStream stream,EntityItem old,TileEntity tileEntity) throws IOException
	{
		short itemId,ItemDamage;
		if((itemId = stream.readShort()) == -1)
		{
			stream.readShort();
			return  null;
			
		}
		else
		{
			ItemDamage = stream.readShort();
			if(old == null || old.getEntityItem().itemID != itemId || old.getEntityItem().getItemDamage() != ItemDamage)
				return  new EntityItem(tileEntity.worldObj,tileEntity.xCoord + 0.5, tileEntity.yCoord + 0.5, tileEntity.zCoord + 1.2, new ItemStack(itemId, 1, ItemDamage));
			else
				return old;
		}
		
	}

	public static boolean isGameInPause() {
		// TODOUPDATE
		return false;
	}

	static boolean lightmapTexUnitTextureEnable;
	public static void disableLight() {
		// TODO Auto-generated method stub
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        lightmapTexUnitTextureEnable = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
        if(lightmapTexUnitTextureEnable)
        	GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_LIGHTING);
	}

	public static void enableLight() {
		// TODO Auto-generated method stub
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        if(lightmapTexUnitTextureEnable)
        	GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_LIGHTING);		
	}

	public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);

	}

	public static void disableBlend() {
		// TODO Auto-generated method stub
		GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
	}
	
	public static void ledOnOffColor(boolean on)
	{
		if(! on)
			GL11.glColor3f(0.7f, 0f, 0f);
		else
			GL11.glColor3f(0f, 0.7f, 0f);
	}
	
	public static void drawLight(Obj3DPart part)
	{
		if(part == null) return;
		disableLight();
		enableBlend();
		
		part.draw();
	
		enableLight();
		disableBlend();
		
	}
	
	public static void drawLightNoBind(Obj3DPart part) {

		if(part == null) return;
		disableLight();
		enableBlend();
		
		part.drawNoBind();
	
		enableLight();
		disableBlend();		
	}

	public static void drawLight(Obj3DPart part,float angle,float x,float y,float z)
	{
		if(part == null) return;
		disableLight();
		enableBlend();
		
		part.draw(angle, x, y, z);

		enableLight();
		disableBlend();
		
	}
	public static void glDefaultColor() {
		GL11.glColor4f(1f,1f, 1f, 1f);
	}

	public static void enableBilinear() {
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);			
	}

	public static void disableBilinear() {
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		
	}

	public static EntityClientPlayerMP getClientPlayer() {
		// TODO Auto-generated method stub
		return Minecraft.getMinecraft().thePlayer;
	}

	public static void drawHaloNoLightSetup(Obj3DPart halo, float distance) {
		if(halo == null) return;
		halo.bindTexture();
		Utils.enableBilinear();
		float scale = distance*0.5f;
		if(scale > 1f) scale = 1f;
		scale = scale *scale;
		GL11.glPushMatrix();
		GL11.glScalef(1f,scale,scale);
		halo.drawNoBind(distance*5,1f,0f,0f);
		GL11.glPopMatrix();
	}
	public static void drawHalo(Obj3DPart halo, float distance) {

		disableLight();
		enableBlend();
		
		drawHaloNoLightSetup(halo, distance);
		enableLight();
		disableBlend();
	}

	static public void drawEntityItem(EntityItem entityItem,double x, double y , double z,float roty,float scale)
	{
		if(entityItem == null) return;
		


		entityItem.hoverStart = 0.0f;
		entityItem.rotationYaw = 0.0f;
		entityItem.motionX = 0.0;
		entityItem.motionY = 0.0;
		entityItem.motionZ =0.0;
		
		Render var10 = null;
		var10 = RenderManager.instance.getEntityRenderObject(entityItem);
		GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, (float)z);
			GL11.glRotatef(roty, 0, 1, 0);
			GL11.glScalef(scale, scale, scale);
			var10.doRender(entityItem,0, 0, 0, 0, 0);	
		GL11.glPopMatrix();	
		

	}
    /*
    public float frameTime()
    {
    	float time = Minecraft.getMinecraft().entityRenderer.performanceToFps(par0)
    }*/

	public static void notifyNeighbor(TileEntity t) {
		int x = t.xCoord;
		int y = t.yCoord;
		int z = t.zCoord;
		World w = t.worldObj;
		TileEntity o;
		o = w.getBlockTileEntity(x+1, y, z);
		if(o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient)o).tileEntityNeighborSpawn();
		o = w.getBlockTileEntity(x-1, y, z);
		if(o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient)o).tileEntityNeighborSpawn();
		o = w.getBlockTileEntity(x, y+1, z);
		if(o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient)o).tileEntityNeighborSpawn();
		o = w.getBlockTileEntity(x, y-1, z);
		if(o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient)o).tileEntityNeighborSpawn();
		o = w.getBlockTileEntity(x, y, z+1);
		if(o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient)o).tileEntityNeighborSpawn();
		o = w.getBlockTileEntity(x, y, z-1);
		if(o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient)o).tileEntityNeighborSpawn();
		
	}

	public static boolean playerHasMeter(EntityPlayer entityPlayer) {
		if(Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{ 
    		return true;
    	}
    	if(Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{ 
    		return true;
    	}
    	if(Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{
    		return true;
    	}    
    	return false;
	}

	public static int getRedstoneLevelAround(Coordonate coord) {
		int level = 0;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z)); if(level == 15) return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x+1, coord.y, coord.z)); if(level == 15) return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y+1, coord.z)); if(level == 15) return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y-1, coord.z)); if(level == 15) return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z+1)); if(level == 15) return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z-1)); if(level == 15) return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z)); if(level == 15) return 15;
		
		
		return level;
	}

	public static int getRedstoneLevelAround(World w,int x,int y,int z) {
		int level = 0;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z)); if(level == 15) return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x+1, y, z)); if(level == 15) return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y+1, z)); if(level == 15) return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y-1, z)); if(level == 15) return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z+1)); if(level == 15) return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z-1)); if(level == 15) return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z)); if(level == 15) return 15;
		
		
		return level;
	}
	public static void disableCulling() {
		GL11.glDisable(GL11.GL_CULL_FACE);
	}
	
	
	public static void enableCulling() {
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

	public static boolean isPlayerAround(World world,AxisAlignedBB axisAlignedBB) {
		// TODO Auto-generated method stub
		return world.getEntitiesWithinAABB(EntityPlayer.class, axisAlignedBB).size() != 0;
	}

	public static Object getItemObject(ItemStack stack) {
		if(stack == null) return null;
		Item i = stack.getItem();
		if(i instanceof GenericItemUsingDamage){
			return ((GenericItemUsingDamage)i).getDescriptor(stack);
		}
		if(i instanceof GenericItemBlockUsingDamage){
			return ((GenericItemBlockUsingDamage)i).getDescriptor(stack);
		}
		return i;
	}
	public static void drawIcon(ItemRenderType type,Icon icon) {
		drawIcon(type, icon.getIconName());
	}
	public static void drawIcon(ItemRenderType type) {
		if(type == ItemRenderType.INVENTORY){
			
			Utils.disableCulling();
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glTexCoord2f(1f, 0f); GL11.glVertex3f(16f,0f,0f);
				GL11.glTexCoord2f(0f, 0f);GL11.glVertex3f(0f,0f,0f);
				GL11.glTexCoord2f(0f, 1f);GL11.glVertex3f(0f,16f,0f);
				GL11.glTexCoord2f(1f, 1f);GL11.glVertex3f(16f,16f,0f);
			GL11.glEnd();
			Utils.enableCulling();
		}
		else if(type == ItemRenderType.ENTITY){
			
			Utils.disableCulling();
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glTexCoord2f(1f, 1f); GL11.glVertex3f(0,0f,0.5f);
				GL11.glTexCoord2f(0f, 1f);GL11.glVertex3f(0.0f,0f,-0.5f);
				GL11.glTexCoord2f(0f, 0f);GL11.glVertex3f(0.0f,1f,-0.5f);
				GL11.glTexCoord2f(1f, 0f);GL11.glVertex3f(0.0f,1f,0.5f);
			GL11.glEnd();
			Utils.enableCulling();					
		}
		else{
			GL11.glTranslatef(0.5f, -0.3f, 0.5f);
			
			Utils.disableCulling();
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glTexCoord2f(1f, 1f);GL11.glVertex3f(0.0f,0.5f,0.5f);
				GL11.glTexCoord2f(0f, 1f);GL11.glVertex3f(0.0f,0.5f,-0.5f);
				GL11.glTexCoord2f(0f, 0f);GL11.glVertex3f(0.0f,1.5f,-0.5f);
				GL11.glTexCoord2f(1f, 0f);GL11.glVertex3f(0.0f,1.5f,0.5f);
			GL11.glEnd();
			Utils.enableCulling();			
		}
	}
	public static void drawIcon(ItemRenderType type,String icon) {
		Utils.bindTextureByName(icon);
		drawIcon(type);
	}
	public static void drawIcon(ItemRenderType type,ResourceLocation icon) {
		Utils.bindTexture(icon);
		drawIcon(type);
	}

	public static void drawEnergyBare(ItemRenderType type,float e) {
		float x = 13f,y = 15f-e*14f;
		GL11.glDisable(GL11.GL_TEXTURE_2D);	
		
		GL11.glColor3f(0f, 0f, 0f);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex3f(x+2f,1,0.01f);
			GL11.glVertex3f(x,1,0f);
			GL11.glVertex3f(x,15f,0f);
			GL11.glVertex3f(x+2f,15f,0.01f);
		GL11.glEnd();		
		
		GL11.glColor3f(1, e, 0f);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex3f(x+1f,y,0.01f);
			GL11.glVertex3f(x,y,0f);
			GL11.glVertex3f(x,15f,0f);
			GL11.glVertex3f(x+1f,15f,0.01f);
		GL11.glEnd();
	
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(1f, 1f, 1f);
		
	}
/*
 * 	public static void drawIcon(Icon icon) {
		Utils.bindTextureByName(icon.getIconName());
		Utils.disableCulling();
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(0f, 0f); GL11.glVertex3f(0.5f,-0.5f,0f);
			GL11.glTexCoord2f(0f, 0f);GL11.glVertex3f(-0.5f,-0.5f,0f);
			GL11.glTexCoord2f(0f, 1f);GL11.glVertex3f(-0.5f,0.5f,0f);
			GL11.glTexCoord2f(1f, 1f);GL11.glVertex3f(0.5f,0.5f,0f);
		GL11.glEnd();
		Utils.enableCulling();
	}

	public static void drawEnergyBare(float e) {
		float x = 14f/16f,y = 15f/16f-e*14f/16f;
		GL11.glColor3f(e, e, 0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex3f(x+1f/16f,y,0.01f);
			GL11.glVertex3f(x,y,0f);
			GL11.glVertex3f(x,15f/16f,0f);
			GL11.glVertex3f(x+1f/16f,15f/16f,0.01f);
		GL11.glEnd();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(1f, 1f, 1f);
	}

 */
	public static void guiScale() {
		GL11.glScalef(16f, 16f, 1f);
		
	}
	
	
    static public void getItemStack(String name,List list)
    {
        
        Item[] aitem = Item.itemsList;
        ArrayList<ItemStack> tempList = new ArrayList<ItemStack>(3000);
        int i = aitem.length;
        int j;

        for (j = 0; j < i; ++j)
        {
            Item item = aitem[j];

            if (item != null && item.getCreativeTab() != null)
            {
                item.getSubItems(item.itemID, (CreativeTabs)null, tempList);
            }
        }
        /*
        Enchantment[] aenchantment = Enchantment.enchantmentsList;
        i = aenchantment.length;

        for (j = 0; j < i; ++j)
        {
            Enchantment enchantment = aenchantment[j];

            if (enchantment != null && enchantment.type != null)
            {
                Item.enchantedBook.func_92113_a(enchantment, containercreative.itemList);
            }
        }*/

  
        String s = name.toLowerCase();

        for(ItemStack itemstack : tempList)  {
            //String s1 = itemstack.getDisplayName();

            if (itemstack.getDisplayName().toLowerCase().contains(s))
            {
            	list.add(itemstack);
            }
    
        }

    }
	protected static RenderItem itemRendererr;
	
	static RenderItem getItemRender(){
		if(itemRendererr == null){
			itemRendererr = new RenderItem();
		}
		
		return itemRendererr;
	}
	
	static Minecraft mc()
	{
		return Minecraft.getMinecraft();
	}
    
    public static void drawItemStack(ItemStack par1ItemStack, int x, int y, String par4Str,boolean gui)
    {
    	RenderItem itemRenderer = getItemRender();
    	
       // GL11.glTranslatef(0.0F, 0.0F, 32.0F);
       
        itemRenderer.zLevel = 400.0F;
        FontRenderer font = null;
        if (par1ItemStack != null) font = par1ItemStack.getItem().getFontRenderer(par1ItemStack);
        if (font == null) font = mc().fontRenderer;
        itemRenderer.renderItemAndEffectIntoGUI(font, mc().func_110434_K(), par1ItemStack, x, y);
        itemRenderer.renderItemOverlayIntoGUI(font, mc().func_110434_K(), par1ItemStack, x, y, par4Str);
        
        itemRenderer.zLevel = 0.0F;
        
        if(gui){
            GL11.glDisable(GL11.GL_LIGHTING);
        }
    }

	public static GuiScreen guiLastOpen;

    public static void clientOpenGui(GuiScreen gui)
    {
    	guiLastOpen = gui;
		EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) Utils.getClientPlayer();	
		clientPlayer.openGui(Eln.instance,GuiHandler.genericOpen,clientPlayer.worldObj, 0,0,0);
    }

	public static void printSide(String string) {
		System.out.println(string);
	}

	public static short modbusToShort(double outputNormalized, int i) {
		int bit = Float.floatToRawIntBits((float)outputNormalized);
		if(i == 1)
			return (short) bit;
		else
			return (short) (bit >>> 16);
		
		
	}
    
	public static float modbusToFloat(short first,short second) {
		int bit = ((((int)first)&0xFFFF) << 16) + (((int)second)&0xFFFF);
		return Float.intBitsToFloat(bit);
	}
    
       
    

} 
