package mods.eln.misc;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.ITileEntitySpawnClient;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class Utils {

	public static Object d[] = new Object[5];

	public static double minecraftDay = 60 * 24;

	public static Random random = new Random();

	public static final double burnTimeToEnergyFactor = 1.0;

	public static double voltageMageFactor = 0.1;

	private static int uuid = 1;

	public static double rand(double min, double max) {
		return random.nextDouble() * (max - min) + min;
	}

	public static void println(String str) {
		if (!Eln.debugEnabled)
			return;
		System.out.println(str);
	}

	public static void println(Object str) {
		if (!Eln.debugEnabled)
			return;
		System.out.println(str.toString());
	}

	public static void print(String str) {
		if (!Eln.debugEnabled)
			return;
		System.out.print(str);
	}

	public static void print(Object str) {
		if (!Eln.debugEnabled)
			return;
		System.out.print(str.toString());
	}

	static String floatToStr(double f, int high, int low) {
		String temp = "";
		for (int idx = 0; idx < high; idx++)
			temp = temp + "0";
		temp = temp + ".";
		for (int idx = 0; idx < low; idx++)
			temp = temp + "0";

		String str = new DecimalFormat(temp).format(f);
		int idx = 0;
		char[] ch = str.toCharArray();
		while (true) {
			if (str.length() == idx)
				break;
			if (ch[idx] == '.') {
				ch[idx - 1] = '0';
				break;
			}
			if (ch[idx] != '0' && ch[idx] != ' ')
				break;
			ch[idx] = '_';
			idx++;
		}

		return new String(ch);
	}

	public static boolean isTheClass(Object o, Class c) {
		if (o.getClass() == c)
			return true;
		for (Class classIterator = o.getClass().getSuperclass(); classIterator != null; classIterator = classIterator.getSuperclass())
		// for (Class classIterator : o.getClass().getClass())
		{
			if (classIterator == c) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasTheInterface(Object o, Class c) {
		for (Class i : o.getClass().getInterfaces()) {
			if (i == c)
				return true;
		}
		return false;
	}

	public static Direction entityLivingViewDirection(EntityLivingBase entityLiving) {
		if (entityLiving.rotationPitch > 45)
			return Direction.YN;
		if (entityLiving.rotationPitch < -45)
			return Direction.YP;
		int dirx = MathHelper.floor_double((double) (entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		if (dirx == 3)
			return Direction.XP;
		if (dirx == 0)
			return Direction.ZP;
		if (dirx == 1)
			return Direction.XN;
		return Direction.ZN;
	}

	public static Direction entityLivingHorizontalViewDirection(EntityLivingBase entityLiving) {
		int dirx = MathHelper.floor_double((double) (entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		if (dirx == 3)
			return Direction.XP;
		if (dirx == 0)
			return Direction.ZP;
		if (dirx == 1)
			return Direction.XN;
		return Direction.ZN;
	}

	/**
	 * Returns the number of ticks that the supplied fuel item will keep the furnace burning, or 0 if the item isn't fuel
	 */
	/*
	 * public static int getItemBurnTime(ItemStack par0ItemStack) { if (par0ItemStack == null) { return 0; } else { int var1 = par0ItemStack.getItem().shiftedIndex; Item var2 = par0ItemStack.getItem();
	 * 
	 * if (par0ItemStack.getItem() instanceof ItemBlock && Block.blocksList[var1] != null) { Block var3 = Block.blocksList[var1];
	 * 
	 * if (var3 == Block.woodSingleSlab) { return 150; }
	 * 
	 * if (var3.blockMaterial == Material.wood) { return 300; } }
	 * 
	 * if (var2 instanceof ItemTool && ((ItemTool) var2).getToolMaterialName().equals("WOOD")) return 200; if (var2 instanceof ItemSword && ((ItemSword) var2).func_77825_f().equals("WOOD")) return 200; if (var2 instanceof ItemHoe && ((ItemHoe) var2).func_77842_f().equals("WOOD")) return 200; if (var1 == Item.stick.shiftedIndex) return 100; if (var1 == Item.coal.shiftedIndex) return 1600; if (var1 == Item.bucketLava.shiftedIndex) return 20000; if (var1 == Block.sapling.blockID) return 100; if (var1 == Item.blazeRod.shiftedIndex) return 2400; return GameRegistry.getFuelValue(par0ItemStack); } }
	 */

	public static double getItemEnergie(ItemStack par0ItemStack) {
		return burnTimeToEnergyFactor * 80000.0 / 1600 * TileEntityFurnace.getItemBurnTime(par0ItemStack);
	}

	public static double getCoalEnergyReference() {
		return burnTimeToEnergyFactor * 80000.0;
	}

	public static byte booleanSideMaskToByte(boolean[] side) {
		byte b = 0;
		if (side[0])
			b |= 1 << 0;
		if (side[1])
			b |= 1 << 1;
		if (side[2])
			b |= 1 << 2;
		if (side[3])
			b |= 1 << 3;
		if (side[4])
			b |= 1 << 4;
		if (side[5])
			b |= 1 << 5;
		return b;
	}

	public static void ByteTobooleanSideMask(byte b, boolean[] side) {
		for (int idx = 0; idx < 6; idx++) {
			side[idx] = false;
			if (((b >> idx) & 1) != 0)
				side[idx] = true;
		}
	}

	public static String plotValue(double value) {
		double valueAbs = Math.abs(value);
		if (valueAbs < 0.001) {
			return "0";
		} else if (valueAbs < 0.00999) {
			return String.format("%1.2fm", value * 1000);
		} else if (valueAbs < 0.0999) {
			return String.format("%2.1fm", value * 1000);
		} else if (valueAbs < 0.999) {
			return String.format("%3.0fm", value * 1000);
		} else if (valueAbs < 9.99) {
			return String.format("%1.2f", value);
		} else if (valueAbs < 99.9) {
			return String.format("%2.1f", value);
		} else if (valueAbs < 999) {
			return String.format("%3.0f", value);
		} else if (valueAbs < 9999) {
			return String.format("%1.2fk", value / 1000.0);
		} else if (valueAbs < 99999) {
			return String.format("%2.1fk", value / 1000.0);
		} else { // if(value < 1000000)
			return String.format("%3.0fk", value / 1000.0);
		}
	}

	public static String plotValue(double value, String unit) {
		return plotValue(value) + unit;
	}

	public static String plotVolt(String header, double value) {
		if (!header.equals(""))
			header += " ";
		return header + plotValue(value, "V  ");
	}

	public static String plotAmpere(String header, double value) {
		if (!header.equals(""))
			header += " ";
		return header + plotValue(value, "A  ");
	}

	public static String plotCelsius(String header, double value) {
		value += PhysicalConstant.Tref - PhysicalConstant.TCelsius;
		if (!header.equals(""))
			header += " ";
		return header + plotValue(value, "\u00B0C ");
	}

	public static String plotPercent(String header, double value) {
		if (!header.equals(""))
			header += " ";
		if (value >= 1.0)
			return header + String.format("%3.0f", value * 100.0) + "%   ";
		else
			return header + String.format("%3.1f", value * 100.0) + "%   ";
	}

	public static String plotEnergy(String header, double value) {
		if (!header.equals(""))
			header += " ";
		return header + plotValue(value, "J  ");
	}

	public static String plotPower(String header, double value) {
		if (!header.equals(""))
			header += " ";
		return header + plotValue(value, "W  ");
	}

	public static String plotOhm(String header, double value) {
		if (!header.equals(""))
			header += " ";
		return header + plotValue(value, "ohm  ");
	}

	public static String plotUIP(double U, double I) {
		return plotVolt("U", U) + plotAmpere("I", I) + plotPower("P", Math.abs(U * I));
	}

	public static String plotTime(double value) {
		String str = "";
		int h, mn, s;

		if (value == 0.0)
			return str + "0''";

		h = (int) (value / 3600);
		value = value % 3600;
		mn = (int) (value / 60);
		value = value % 60;
		s = (int) (value / 1);

		if (h != 0)
			str += h + "h";
		if (mn != 0)
			str += mn + "'";
		if (s != 0)
			str += s + "''";
		return str;
	}

	public static String plotTime(String header, double value) {
		if (!header.equals(""))
			header += " ";
		return header + plotTime(value);
	}

	public static void readFromNBT(NBTTagCompound nbt, String str, IInventory inventory) {
		NBTTagList var2 = nbt.getTagList(str, 10);

		for (int var3 = 0; var3 < var2.tagCount(); ++var3) {
			NBTTagCompound var4 = (NBTTagCompound) var2.getCompoundTagAt(var3);
			int var5 = var4.getByte("Slot") & 255;

			if (var5 >= 0 && var5 < inventory.getSizeInventory()) {
				inventory.setInventorySlotContents(var5, ItemStack.loadItemStackFromNBT(var4));
			}
		}
	}

	public static void writeToNBT(NBTTagCompound nbt, String str, IInventory inventory) {
		NBTTagList var2 = new NBTTagList();

		for (int var3 = 0; var3 < inventory.getSizeInventory(); ++var3) {
			if (inventory.getStackInSlot(var3) != null) {
				NBTTagCompound var4 = new NBTTagCompound();
				var4.setByte("Slot", (byte) var3);
				inventory.getStackInSlot(var3).writeToNBT(var4);
				var2.appendTag(var4);
			}
		}

		nbt.setTag(str, var2);
	}

	public static void sendPacketToClient(ByteArrayOutputStream bos, EntityPlayerMP player) {
		// Profiler p = new Profiler();
		// p.add("A");
		// ElnServerPacket packet = new ElnServerPacket(Eln.channelName, bos.toByteArray());
		// ByteBuf b = Unpooled.buffer().capacity(bos.size()).setBytes(0, bos.toByteArray());
		// p.add("B");
		// Eln.eventChannel.sendTo(new FMLProxyPacket(b, Eln.channelName), player);
		// p.stop();
		// Utils.println(p);

		S3FPacketCustomPayload packet = new S3FPacketCustomPayload(Eln.channelName, bos.toByteArray());
		player.playerNetServerHandler.sendPacket(packet);

		// FMLCommonHandler.instance().getMinecraftServerInstance().getEln.eventChannel.sendTo(new FMLProxyPacket(packet),player);
	}

	/*
	 * public static void sendPacketToPlayer( ElnServerPacket packet, EntityPlayerMP player) {
	 * 
	 * Eln.eventChannel.sendTo(new FMLProxyPacket(packet), player); // player.playerNetServerHandler.sendPacket(new FMLProxyPacket(packet)); }
	 */

	// private static Color[] dyeColors

	// public Color getDyeColor(ItemStack stack)
	// {
	// ItemDye.dyeColors[stack.getItemDamage()];
	// }

	public static void setGlColorFromDye(int damage){
		setGlColorFromDye(damage,1.0f);
	}

	public static void setGlColorFromDye(int damage, float gain) {
		int color = ItemDye.field_150922_c[damage]; // dyeColors
		switch (damage) {
			default:
				GL11.glColor3f(0.05f * gain, 0.05f * gain, 0.05f * gain);
				break;
			case 0:
				GL11.glColor3f(0.2f * gain, 0.2f * gain, 0.2f * gain);
				break; // black
			case 1:
				GL11.glColor3f(1.0f * gain, 0.05f * gain, 0.05f * gain);
				break; // red
			case 2:
				GL11.glColor3f(0.2f * gain, 0.5f * gain, 0.1f * gain);
				break; // green
			case 3:
				GL11.glColor3f(0.3f * gain, 0.2f * gain, 0.1f * gain);
				break; // brown
			case 4:
				GL11.glColor3f(0.2f * gain, 0.2f * gain, 1.0f * gain);
				break; // blue
			case 5:
				GL11.glColor3f(0.7f * gain, 0.05f * gain, 1.0f * gain);
				break; // purple
			case 6:
				GL11.glColor3f(0.2f * gain, 0.7f * gain, 0.9f * gain);
				break;
			case 7:
				GL11.glColor3f(0.7f * gain, 0.7f * gain, 0.7f * gain);
				break;
			case 8:
				GL11.glColor3f(0.4f * gain, 0.4f * gain, 0.4f * gain);
				break;
			case 9:
				GL11.glColor3f(1.0f * gain, 0.5f * gain, 0.5f * gain);
				break;
			case 10:
				GL11.glColor3f(0.05f * gain, 1.0f * gain, 0.05f * gain);
				break;
			case 11:
				GL11.glColor3f(0.9f * gain, 0.8f * gain, 0.1f * gain);
				break;
			case 12:
				GL11.glColor3f(0.4f * gain, 0.5f * gain, 1.0f * gain);
				break;
			case 13:
				GL11.glColor3f(0.9f * gain, 0.3f * gain, 0.9f * gain);
				break;
			case 14:
				GL11.glColor3f(1.0f * gain, 0.6f * gain, 0.3f * gain);
				break;
			case 15:
				GL11.glColor3f(1.0f * gain, 1.0f * gain, 1.0f * gain);
				break;
		}
		// GL11.glColor3f(((color >> 16) & 0xFF) / 255f, ((color >> 7) & 0xFF) / 255f, ((color >> 0) & 0xFF) / 255f);
	}

	public static void setGlColorFromLamp(int colorIdx) {
		switch (colorIdx) {
			default:
			case 15: //White
				GL11.glColor3f(1.0f, 1.0f, 1.0f);
				break;
			case 0: //Black
				GL11.glColor3f(0.25f, 0.25f, 0.25f);
				break;
			case 1: //Red
				GL11.glColor3f(1.0f, 0.5f, 0.5f);
				break;
			case 2: //Green
				GL11.glColor3f(0.5f, 1.0f, 0.5f);
				break;
			case 3: //Brown
				GL11.glColor3f(0.5647f, 0.36f, 0.36f);
				break;
			case 4: //Blue
				GL11.glColor3f(0.5f, 0.5f, 1.0f);
				break;
			case 5: //Purple
				GL11.glColor3f(0.78125f, 0.46666f, 1.0f);
				break;
			case 6: //Cyan
				GL11.glColor3f(0.5f, 1.0f, 1.0f);
				break;
			case 7: //Silver
				GL11.glColor3f(0.75f, 0.75f, 0.75f);
				break;
			case 8: //Gray
				GL11.glColor3f(0.5f, 0.5f, 0.5f);
				break;
			case 9: //Pink
				GL11.glColor3f(1.0f, 0.5f, 0.65882f);
				break;
			case 10: //Lime
				GL11.glColor3f(0.75f, 1.0f, 0.5f);
				break;
			case 11: //Yellow
				GL11.glColor3f(1.0f, 1.0f, 0.5f);
				break;
			case 12: //Light Blue
				GL11.glColor3f(0.5f, 0.75f, 1.0f);
				break;
			case 13: //Magenta
				GL11.glColor3f(1.0f, 0.5f, 1.0f);
				break;
			case 14: //Orange
				GL11.glColor3f(1.0f, 0.80f, 0.5f);
				break;
		}
	}

	/*
	 * public static double getWeather(World world) { if(world.isThundering()) return 1.0; if(world.isRaining()) return 0.5; return 0.0;
	 * 
	 * }
	 */

	// Into utilsClient To
	public static double getWeatherNoLoad(int dim) {
		if (!getWorldExist(dim)) return 0.0;
		World world = getWorld(dim);
		if (world.isThundering())
			return 1.0;
		if (world.isRaining())
			return 0.5;
		return 0.0;
	}

	public static World getWorld(int dim) {
		return FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dim);
	}

	public static boolean getWorldExist(int dim) {
		return DimensionManager.getWorld(dim) != null;
	}

	public static double getWind(int worldId, int y) {
		if (!getWorldExist(worldId)) {
			return Math.max(0.0, Eln.instance.wind.getWind(y));
		} else {
			World world = getWorld(worldId);
			float factor = 1f + world.getRainStrength(0) * 0.2f + world.getWeightedThunderStrength(0) * 0.2f;
			return Math.max(0.0, Eln.instance.wind.getWind(y) * factor + world.getRainStrength(0) * 1f + world.getWeightedThunderStrength(0) * 2f);
		}
	}

	// public static double getWind(World world, int y)
	// {
	// float factor = 1f + world.getRainStrength(0) * 0.2f + world.getWeightedThunderStrength(0) * 0.2f;
	// return Math.max(0.0, Eln.instance.wind.getWind(y) * factor + world.getRainStrength(0) * 1f + world.getWeightedThunderStrength(0) * 2f);
	// }

	public static void dropItem(ItemStack itemStack, int x, int y, int z, World world) {
		if (itemStack == null)
			return;
		if (world.getGameRules().getGameRuleBooleanValue("doTileDrops")) {
			float var6 = 0.7F;
			double var7 = (double) (world.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
			double var9 = (double) (world.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
			double var11 = (double) (world.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
			EntityItem var13 = new EntityItem(world, (double) x + var7, (double) y + var9, (double) z + var11, itemStack);
			var13.delayBeforeCanPickup = 10;
			world.spawnEntityInWorld(var13);
		}
	}

	public static void dropItem(ItemStack itemStack, Coordonate coordonate) {
		dropItem(itemStack, coordonate.x, coordonate.y, coordonate.z, coordonate.world());
	}

	public static boolean tryPutStackInInventory(ItemStack stack, IInventory inventory, int start, int count) {
		if (inventory == null) return false;
		for (int idx = start; idx < start + count; idx++) {
			ItemStack targetStack = inventory.getStackInSlot(idx);
			if (targetStack == null) {
				inventory.setInventorySlotContents(idx, stack.copy());
				stack.stackSize = 0;
				return true;
			} else if (targetStack.isItemEqual(stack)) {
				int sizeLast = targetStack.stackSize;
				// inventory.decrStackSize(idx, -stack.stackSize);
				int transferMax = inventory.getInventoryStackLimit() - targetStack.stackSize;
				if (transferMax > 0) {
					int transfer = stack.stackSize;
					if (transfer > transferMax)
						transfer = transferMax;
					inventory.decrStackSize(idx, -transfer);
					stack.stackSize -= transfer;
				}

				if (stack.stackSize == 0) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean canPutStackInInventory(ItemStack[] stackList, IInventory inventory, int[] slotsIdList) {
		int limit = inventory.getInventoryStackLimit();
		ItemStack[] outputStack = new ItemStack[slotsIdList.length];
		ItemStack[] inputStack = new ItemStack[stackList.length];

		for (int idx = 0; idx < outputStack.length; idx++) {
			if (inventory.getStackInSlot(slotsIdList[idx]) != null)
				outputStack[idx] = inventory.getStackInSlot(slotsIdList[idx]).copy();
		}
		for (int idx = 0; idx < stackList.length; idx++) {
			inputStack[idx] = stackList[idx].copy();
		}

		boolean oneStackDone;
		for (ItemStack stack : inputStack) {
			// if(stack == null) continue;
			oneStackDone = false;
			for (int idx = 0; idx < slotsIdList.length; idx++) {
				ItemStack targetStack = outputStack[idx];

				if (targetStack == null) {
					outputStack[idx] = stack;
					oneStackDone = true;
					break;
				} else if (targetStack.isItemEqual(stack)) {
					int sizeLast = targetStack.stackSize;
					// inventory.decrStackSize(idx, -stack.stackSize);
					int transferMax = limit - targetStack.stackSize;
					if (transferMax > 0) {
						int transfer = stack.stackSize;
						if (transfer > transferMax)
							transfer = transferMax;
						outputStack[idx].stackSize += transfer;
						stack.stackSize -= transfer;
					}

					if (stack.stackSize == 0) {
						oneStackDone = true;
						break;
					}
				}
			}

			if (!oneStackDone)
				return false;
		}
		return true;
	}

	public static boolean tryPutStackInInventory(ItemStack[] stackList, IInventory inventory, int[] slotsIdList) {
		int limit = inventory.getInventoryStackLimit();

		for (ItemStack stack : stackList) {
			for (int idx = 0; idx < slotsIdList.length; idx++) {
				ItemStack targetStack = inventory.getStackInSlot(slotsIdList[idx]);
				if (targetStack == null) {
					inventory.setInventorySlotContents(slotsIdList[idx], stack.copy());
					stack.stackSize = 0;
					break;
				} else if (targetStack.isItemEqual(stack)) {
					int sizeLast = targetStack.stackSize;
					// inventory.decrStackSize(idx, -stack.stackSize);
					int transferMax = limit - targetStack.stackSize;
					if (transferMax > 0) {
						int transfer = stack.stackSize;
						if (transfer > transferMax)
							transfer = transferMax;
						inventory.decrStackSize(slotsIdList[idx], -transfer);
						stack.stackSize -= transfer;
					}

					if (stack.stackSize == 0) {
						break;
					}
				}
			}
		}
		return true;
	}

	public static double voltageMargeFactorSub(double value) {
		if (value > 1 + voltageMageFactor) {
			return value - voltageMageFactor;
		}
		else if (value > 1) {
			return 1;
		}
		return value;
	}

	/*
	 * public static void bindGuiTexture(String string) { Utils.bindTextureByName("/sprites/gui/" + string); }
	 */

	public static void serialiseItemStack(DataOutputStream stream, ItemStack stack) throws IOException {
		if ((stack) == null) {
			stream.writeShort(-1);
			stream.writeShort(-1);
		} else {
			stream.writeShort(Item.getIdFromItem(stack.getItem()));
			stream.writeShort(stack.getItemDamage());
		}
	}

	public static ItemStack unserialiseItemStack(DataInputStream stream) throws IOException {
		short id, damage;
		id = stream.readShort();
		damage = stream.readShort();
		if (id == -1)
			return null;
		return Utils.newItemStack(id, 1, damage);
	}

	public static EntityItem unserializeItemStackToEntityItem(DataInputStream stream, EntityItem old, TileEntity tileEntity) throws IOException {
		short itemId, ItemDamage;
		if ((itemId = stream.readShort()) == -1) {
			stream.readShort();
			return null;

		} else {
			ItemDamage = stream.readShort();
			if (old == null || Item.getIdFromItem(old.getEntityItem().getItem()) != itemId || old.getEntityItem().getItemDamage() != ItemDamage)
				return new EntityItem(tileEntity.getWorldObj(), tileEntity.xCoord + 0.5, tileEntity.yCoord + 0.5, tileEntity.zCoord + 1.2, Utils.newItemStack(itemId, 1, ItemDamage));
			else
				return old;
		}
	}

	public static boolean isGameInPause() {
		return Minecraft.getMinecraft().isGamePaused();
	}

	public static int getLight(World w, EnumSkyBlock e, int x, int y, int z) {
		return w.getSavedLightValue(e, x, y, z);
	}

	/*
	 * int b = w.getSkyBlockTypeBrightness(EnumSkyBlock.Block, x, y, z); int s = w.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, x, y, z) - w.calculateSkylightSubtracted(0f); return Math.max(b, s); }
	 */

	/*
	 * public static void drawHalo(Obj3DPart halo,float r,float g,float b,World w,int x,int y,int z,boolean bilinear) {
	 * 
	 * disableLight(); enableBlend();
	 * 
	 * drawHaloNoLightSetup(halo,r,g,b, w,x,y,z,bilinear); enableLight(); disableBlend(); }
	 */

	/*
	 * public float frameTime() { float time = Minecraft.getMinecraft().entityRenderer.performanceToFps(par0) }
	 */

	public static void notifyNeighbor(TileEntity t) {
		int x = t.xCoord;
		int y = t.yCoord;
		int z = t.zCoord;
		World w = t.getWorldObj();
		TileEntity o;
		o = w.getTileEntity(x + 1, y, z);
		if (o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
		o = w.getTileEntity(x - 1, y, z);
		if (o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
		o = w.getTileEntity(x, y + 1, z);
		if (o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
		o = w.getTileEntity(x, y - 1, z);
		if (o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
		o = w.getTileEntity(x, y, z + 1);
		if (o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
		o = w.getTileEntity(x, y, z - 1);
		if (o != null && o instanceof ITileEntitySpawnClient)
			((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
	}

	public static boolean playerHasMeter(EntityPlayer entityPlayer) {
		if (Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
			return true;
		}
		if (Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
			return true;
		}
		if (Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
			return true;
		}
		return false;
	}

	public static int getRedstoneLevelAround(Coordonate coord) {
		int level = 0;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z));
		if (level == 15)
			return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x + 1, coord.y, coord.z));
		if (level == 15)
			return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y + 1, coord.z));
		if (level == 15)
			return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y - 1, coord.z));
		if (level == 15)
			return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z + 1));
		if (level == 15)
			return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z - 1));
		if (level == 15)
			return 15;
		level = Math.max(level, coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z));
		if (level == 15)
			return 15;

		return level;
	}

	public static int getRedstoneLevelAround(World w, int x, int y, int z) {
		int level = 0;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z));
		if (level == 15)
			return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x + 1, y, z));
		if (level == 15)
			return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y + 1, z));
		if (level == 15)
			return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y - 1, z));
		if (level == 15)
			return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z + 1));
		if (level == 15)
			return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z - 1));
		if (level == 15)
			return 15;
		level = Math.max(level, w.getStrongestIndirectPower(x, y, z));
		if (level == 15)
			return 15;

		return level;
	}

	public static boolean isPlayerAround(World world, AxisAlignedBB axisAlignedBB) {
		return world.getEntitiesWithinAABB(EntityPlayer.class, axisAlignedBB).size() != 0;
	}

	public static Object getItemObject(ItemStack stack) {
		if (stack == null)
			return null;
		Item i = stack.getItem();
		if (i instanceof GenericItemUsingDamage) {
			return ((GenericItemUsingDamage) i).getDescriptor(stack);
		}
		if (i instanceof GenericItemBlockUsingDamage) {
			return ((GenericItemBlockUsingDamage) i).getDescriptor(stack);
		}
		return i;
	}

	/*
	 * public static void drawIcon(Icon icon) { Utils.bindTextureByName(icon.getIconName()); Utils.disableCulling(); GL11.glBegin(GL11.GL_QUADS); GL11.glTexCoord2f(0f, 0f); GL11.glVertex3f(0.5f,-0.5f,0f); GL11.glTexCoord2f(0f, 0f);GL11.glVertex3f(-0.5f,-0.5f,0f); GL11.glTexCoord2f(0f, 1f);GL11.glVertex3f(-0.5f,0.5f,0f); GL11.glTexCoord2f(1f, 1f);GL11.glVertex3f(0.5f,0.5f,0f); GL11.glEnd(); Utils.enableCulling(); }
	 * 
	 * public static void drawEnergyBare(float e) { float x = 14f/16f,y = 15f/16f-e*14f/16f; GL11.glColor3f(e, e, 0f); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glBegin(GL11.GL_QUADS); GL11.glVertex3f(x+1f/16f,y,0.01f); GL11.glVertex3f(x,y,0f); GL11.glVertex3f(x,15f/16f,0f); GL11.glVertex3f(x+1f/16f,15f/16f,0.01f); GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor3f(1f, 1f, 1f); }
	 */

	static public void getItemStack(String name, List list) {
		Iterator aitem = Item.itemRegistry.iterator();
		List<ItemStack> tempList = new ArrayList<ItemStack>(3000);
		Item item;

		while (aitem.hasNext()) {
			item = (Item) aitem.next();
			if (item != null && item.getCreativeTab() != null) {
				item.getSubItems(item, (CreativeTabs) null, tempList);
			}
		}

		String s = name.toLowerCase();

		for (ItemStack itemstack : tempList) {
			// String s1 = itemstack.getDisplayName();

			if (itemstack.getDisplayName().toLowerCase().contains(s)) {
				list.add(itemstack);
			}
		}
	}

	public static Side getSide() {
		return FMLCommonHandler.instance().getEffectiveSide();
	}

	public static boolean isServer() {
		return getSide() == Side.SERVER;
	}

	public static void printSide(String string) {
		Utils.println(string);
	}

	public static short modbusToShort(double outputNormalized, int i) {
		int bit = Float.floatToRawIntBits((float) outputNormalized);
		if (i == 1)
			return (short) bit;
		else
			return (short) (bit >>> 16);
	}

	public static float modbusToFloat(short first, short second) {
		int bit = ((((int) first) & 0xFFFF) << 16) + (((int) second) & 0xFFFF);
		return Float.intBitsToFloat(bit);
	}

	public static boolean areSame(ItemStack stack, ItemStack output) {
		try {
			if (stack.getItem() == output.getItem() && stack.getItemDamage() == output.getItemDamage()) return true;
			int[] stackIds = OreDictionary.getOreIDs(stack);
			int[] outputIds = OreDictionary.getOreIDs(output);
			// System.out.println(Arrays.toString(stackIds) + "   " + Arrays.toString(outputIds));
			for (int i : outputIds) {
				for (int j : stackIds) {
					if (i == j) return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	public static Vec3 getVec05(Coordonate c) {
		return Vec3.createVectorHelper(c.x + (c.x < 0 ? -1 : 1) * 0.5, c.y + (c.y < 0 ? -1 : 1) * 0.5, c.z + (c.z < 0 ? -1 : 1) * 0.5);
	}

	public static double getHeadPosY(Entity e) {
		if (e instanceof EntityOtherPlayerMP)
			return e.posY + e.getEyeHeight();
		return e.posY;
	}

	/*
	 * public static boolean isPlayerInteractRiseWith(EntityPlayerMP entity, ItemStack stack) {
	 * 
	 * return entity.inventory.getCurrentItem() == stack && Eln.playerManager.get(entity).getInteractRise(); }
	 */

	public static boolean isCreative(EntityPlayerMP entityPlayer) {
		return entityPlayer.theItemInWorldManager.isCreative();
		/*
		 * Minecraft m = Minecraft.getMinecraft(); return m.getIntegratedServer().getGameType().isCreative();
		 */
	}

	public static boolean mustDropItem(EntityPlayerMP entityPlayer) {
		if (entityPlayer == null)
			return true;
		return !isCreative(entityPlayer);
	}

	public static void serverTeleport(Entity e, double x, double y, double z) {
		if (e instanceof EntityPlayerMP)
			((EntityPlayerMP) e).setPositionAndUpdate(x, y, z);
		else
			e.setPosition(x, y, z);
	}

	public static ArrayList<Block> traceRay(World world, double x, double y,
			double z, double tx, double ty, double tz) {
		ArrayList<Block> blockList = new ArrayList<Block>();

		double dx, dy, dz;
		dx = tx - x;
		dy = ty - y;
		dz = tz - z;
		double norm = (Math.sqrt(dx * dx + dy * dy + dz * dz));
		double normInv = 1 / (norm + 0.000000001);
		dx *= normInv;
		dy *= normInv;
		dz *= normInv;
		double d = 0;

		while (d < norm) {
			if (Utils.isBlockLoaded(world, x, y, z)) {
				Block b = Utils.getBlock(world, x, y, z);
				if (b != null)
					blockList.add(b);
			}

			x += dx;
			y += dy;
			z += dz;
			d += 1;
		}

		return blockList;
	}

	interface TraceRayWeight {
		float getWeight(Block block);
	}

	public static class TraceRayWeightOpaque implements TraceRayWeight {

		@Override
		public float getWeight(Block block) {
			if (block == null)
				return 0;
			return block.isOpaqueCube() ? 1f : 0f;
		}
	}

	public static float traceRay(World w, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, TraceRayWeight weight) {
		int posXint = (int) Math.round(posX);
		int posYint = (int) Math.round(posY);
		int posZint = (int) Math.round(posZ);

		float x = (float) (posX - posXint), y = (float) (posY - posYint), z = (float) (posZ - posZint);

		float vx = (float) (targetX - posX);
		float vy = (float) (targetY - posY);
		float vz = (float) (targetZ - posZ);

		float rangeMax = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
		float normInv = 1f / rangeMax;
		vx *= normInv;
		vy *= normInv;
		vz *= normInv;

		if (vx == 0)
			vx += 0.0001f;
		if (vy == 0)
			vy += 0.0001f;
		if (vz == 0)
			vz += 0.0001f;

		float vxInv = 1f / vx, vyInv = 1f / vy, vzInv = 1f / vz;

		float stackRed = 0, stackBlue = 0, stackGreen = 0;
		float d = 0;

		while (d < rangeMax) {
			float xFloor = MathHelper.floor_float(x);
			float yFloor = MathHelper.floor_float(y);
			float zFloor = MathHelper.floor_float(z);

			float dx = x - xFloor, dy = y - yFloor, dz = z - zFloor;
			dx = (vx > 0 ? (1 - dx) * vxInv : -dx * vxInv);
			dy = (vy > 0 ? (1 - dy) * vyInv : -dy * vyInv);
			dz = (vz > 0 ? (1 - dz) * vzInv : -dz * vzInv);

			float dBest = Math.min(Math.min(dx, dy), dz) + 0.01f;

			int xInt = (int) xFloor;
			int yInt = (int) yFloor;
			int zInt = (int) zFloor;

			Block block = Blocks.air;

			if (w.blockExists(xInt + posXint, yInt + posYint, zInt + posZint))
				block = w.getBlock(xInt + posXint, yInt + posYint, zInt + posZint);

			float dToStack;

			if (d + dBest < rangeMax)
				dToStack = dBest;
			else {
				dToStack = (rangeMax - d);
			}

			stackRed += weight.getWeight(block) * dToStack;

			x += vx * dBest;
			y += vy * dBest;
			z += vz * dBest;

			d += dBest;
		}

		return stackRed;
	}

	public static boolean isBlockLoaded(World world, double x, double y, double z) {
		return world.blockExists(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
	}

	public static Block getBlock(World world, double x, double y, double z) {
		Block block = world.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
		return block;
	}

	public static double getLength(double x, double y,
			double z, double tx, double ty, double tz) {
		double dx, dy, dz;
		dx = tx - x;
		dy = ty - y;
		dz = tz - z;
		double norm = (Math.sqrt(dx * dx + dy * dy + dz * dz));
		return norm;
	}

	public static <T> int readPrivateInt(Object o, String feildName) {
		try {
			Field f = o.getClass().getDeclaredField(feildName);
			f.setAccessible(true);
			return f.getInt(o);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static ItemStack[][] getItemStackGrid(IRecipe r) {
		ItemStack[][] stacks = new ItemStack[3][3];
		try {
			if (r instanceof ShapedRecipes) {
				ShapedRecipes s = (ShapedRecipes) r;
				for (int idx2 = 0; idx2 < 3; idx2++) {
					for (int idx = 0; idx < 3; idx++) {
						ItemStack rStack = null;
						if (idx < s.recipeWidth && idx2 < s.recipeHeight) {
							rStack = s.recipeItems[idx + idx2 * s.recipeWidth];
						}
						stacks[idx2][idx] = rStack;
					}
				}
				return stacks;
			}
			if (r instanceof ShapedOreRecipe) {
				ShapedOreRecipe s = (ShapedOreRecipe) r;
				int width = readPrivateInt(s, "width");
				int height = readPrivateInt(s, "height");
				Object[] inputs = s.getInput();

				for (int idx2 = 0; idx2 < height; idx2++) {
					for (int idx = 0; idx < width; idx++) {
						Object o = inputs[idx + idx2 * width];
						ItemStack stack = null;
						if (o instanceof List) {
							if (o instanceof List && ((List) o).size() > 0)
								stack = (ItemStack) ((List) o).get(0);
						}

						if (o instanceof ItemStack) {
							stack = (ItemStack) o;
						}
						stacks[idx2][idx] = stack;
					}
				}

				return stacks;
			}
			if (r instanceof ShapelessRecipes) {
				ShapelessRecipes s = (ShapelessRecipes) r;
				int idx = 0;
				for (Object o : s.recipeItems) {
					ItemStack stack = (ItemStack) o;
					stacks[idx / 3][idx % 3] = stack;
					idx++;
				}
				return stacks;
			}
			if (r instanceof ShapelessOreRecipe) {
				ShapelessOreRecipe s = (ShapelessOreRecipe) r;
				int idx = 0;
				for (Object o : s.getInput()) {
					ItemStack stack = null;
					if (o instanceof List && ((List) o).size() > 0) {
						stack = (ItemStack) ((List) o).get(0);
					}

					if (o instanceof ItemStack) {
						stack = (ItemStack) o;
					}
					stacks[idx / 3][idx % 3] = stack;
					idx++;
				}
				return stacks;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	public static ArrayList<ItemStack> getRecipeInputs(IRecipe r) {
		try {
			ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
			if (r instanceof ShapedRecipes) {
				for (ItemStack stack : ((ShapedRecipes) r).recipeItems) {
					stacks.add(stack);
				}
			}
			if (r instanceof ShapelessRecipes) {
				for (Object stack : ((ShapelessRecipes) r).recipeItems) {
					stacks.add((ItemStack) stack);
				}
			}
			if (r instanceof ShapedOreRecipe) {
				ShapedOreRecipe rr = (ShapedOreRecipe) r;
				for (Object o : ((ShapedOreRecipe) r).getInput()) {
					if (o instanceof List) {
						stacks.addAll(((List) o));
					}

					if (o instanceof ItemStack) {
						stacks.add((ItemStack) o);
					}
				}
			}
			if (r instanceof ShapelessOreRecipe) {
				ShapelessOreRecipe rr = (ShapelessOreRecipe) r;
				for (Object o : ((ShapelessOreRecipe) r).getInput()) {
					if (o instanceof List) {
						stacks.addAll(((List) o));
					}

					if (o instanceof ItemStack) {
						stacks.add((ItemStack) o);
					}
				}
			}
			return stacks;
		} catch (Exception e) {
			return new ArrayList<ItemStack>();
		}
	}

	public static double getWorldTime(World world) {
		return world.getWorldTime() / (23999.0);
	}

	public static boolean isWater(Coordonate waterCoord) {
		Block block = waterCoord.getBlock();
		return (block == Blocks.flowing_water || block == Blocks.water);
	}

	public static void addChatMessage(EntityPlayer entityPlayer, String string) {
		entityPlayer.addChatMessage(new ChatComponentText(string));
	}

	public static ItemStack newItemStack(int i, int size, int damage) {
		return new ItemStack(Item.getItemById(i), size, damage);
	}

	public static ItemStack newItemStack(Item i, int size, int damage) {
		return new ItemStack(i, size, damage);
	}

	public static List<NBTTagCompound> getTags(NBTTagCompound nbt) {
		Object[] set = nbt.func_150296_c().toArray();

		ArrayList<NBTTagCompound> tags = new ArrayList<NBTTagCompound>();

		for (int idx = 0; idx < set.length; idx++) {
			tags.add(nbt.getCompoundTag((String) set[idx]));
		}
		return tags;
	}

	public static boolean isRemote(IBlockAccess world) {
		if (!(world instanceof World)) {
			fatal();
		}
		return ((World) world).isRemote;
	}

	public static boolean nullCheck(Object o) {
		return (o == null);
	}

	public static void nullFatal(Object o) {
		if (o == null)
			fatal();
	}

	public static void fatal() {
		try {
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Block getBlock(int blockId) {
		return Block.getBlockById(blockId);
	}

	public static void updateSkylight(Chunk chunk) {
		chunk.func_150804_b(false);
	}

	public static void updateAllLightTypes(World worldObj, int xCoord, int yCoord, int zCoord) {
		worldObj.func_147451_t(xCoord, yCoord, zCoord);

		worldObj.markBlocksDirtyVertical(xCoord, zCoord, 0, 255);
	}

	public static int getItemId(ItemStack stack) {
		return Item.getIdFromItem(stack.getItem());
	}

	public static int getItemId(Block block) {
		return Item.getIdFromItem(Item.getItemFromBlock(block));
	}

	// public static RecipesList smeltRecipeList = new RecipesList();

	public static void addSmelting(Item parentItem, int parentItemDamage, ItemStack findItemStack, float f) {
		FurnaceRecipes.smelting().func_151394_a(newItemStack(parentItem, 1, parentItemDamage), findItemStack, f);
	}

	public static void addSmelting(Block parentBlock, int parentItemDamage, ItemStack findItemStack, float f) {
		FurnaceRecipes.smelting().func_151394_a(newItemStack(Item.getItemFromBlock(parentBlock), 1, parentItemDamage), findItemStack, f);
	}

	public static void addSmelting(Item parentItem, int parentItemDamage, ItemStack findItemStack) {
		addSmelting(parentItem, parentItemDamage, findItemStack, 0.3f);
	}

	public static void addSmelting(Block parentBlock, int parentItemDamage, ItemStack findItemStack) {
		addSmelting(parentBlock, parentItemDamage, findItemStack, 0.3f);
	}

	public static NBTTagCompound newNbtTagCompund(NBTTagCompound nbt, String string) {
		NBTTagCompound cmp = new NBTTagCompound();
		nbt.setTag(string, cmp);
		return cmp;
	}
	public static String getMapFolder() {
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		String savesAt = !server.isDedicatedServer() ? "saves/" : "";
		return savesAt + server.getFolderName() + "/";
	}

	public static File getMapFile(String name) {
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		File f = server.getFile(getMapFolder() + name);
		return f;
	}

	public static String readMapFile(String name) throws IOException {
		File file = getMapFile(name);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		String s = new String(data, "UTF-8");
		return s;
	}

	public static void generateHeightMap(Chunk chunk) {
	}

	public static int getUuid() {
		if (uuid < 1) uuid = 1;
		return uuid++;
	}

	/*
	 * public static float[] getSixNodePinDistance(Obj3D obj) {
	 * 
	 * return new float[]{obj.zMin*16,obj.zMax*16,obj.yMin*16,obj.yMax*16}; }
	 */

	public static float[] getSixNodePinDistance(Obj3DPart obj) {
		return new float[] { Math.abs(obj.zMin * 16), Math.abs(obj.zMax * 16), Math.abs(obj.yMin * 16), Math.abs(obj.yMax * 16) };
	}

	public static boolean isWrench(ItemStack stack) {
		return areSame(stack, Eln.instance.wrenchItemStack) || stack.getDisplayName().toLowerCase().contains("wrench");
	}

	// @SideOnly(Side.SERVER)
	public static boolean isPlayerUsingWrench(EntityPlayer player) {
		if (player == null) return false;
		if (Eln.playerManager.get(player).getInteractEnable()) return true;
		ItemStack stack = player.inventory.getCurrentItem();
		if (stack == null) return false;
		return isWrench(stack);
	}

	public static boolean isClassLoaded(String name) {
		try {
			Class<?> cc = Class.forName(name);
			if (cc != null) {
				return true;
			}
		} catch (ClassNotFoundException e) {
		}
		return false;
	}

	public static String plotSignal(double U, double I) {
		return plotVolt("U", U) + plotAmpere("I", I) + plotPercent("Value", U / Eln.SVU);
	}

	public static float limit(float value, float min, float max) {
		return Math.max(Math.min(value, max), min);
	}

	public static void printFunction(FunctionTable func, double start, double end, double step) {
		Utils.println("********");
		double x;
		for(int idx = 0;(x = start + step * idx) < end + 0.00001; idx++) {
			Utils.println(func.getValue(x));
		}
		Utils.println("********");
	}
}
