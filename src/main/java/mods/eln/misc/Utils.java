package mods.eln.misc;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.init.Cable;
import mods.eln.init.Config;
import mods.eln.init.Items;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.ITileEntitySpawnClient;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static net.minecraft.init.Blocks.FLOWING_WATER;
import static net.minecraft.init.Blocks.WATER;

public class Utils {

    public static final Object[] d = new Object[5];

    public static final double minecraftDay = 60 * 24;

    public static final Random random = new Random();

    public static final double burnTimeToEnergyFactor = 1.0;

    public static final double voltageMageFactor = 0.1;

    private static int uuid = 1;

    private Utils() {
    }

    public static double rand(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    public static void println(String str) {
        if (!Config.INSTANCE.getDebugEnable())
            return;
        System.out.println(str);
    }

    public static void println(Object str) {
        if (!Config.INSTANCE.getDebugEnable())
            return;
        System.out.println(str.toString());
    }

    public static void print(String str) {
        if (!Config.INSTANCE.getDebugEnable())
            return;
        System.out.print(str);
    }

    public static void print(Object str) {
        if (!Config.INSTANCE.getDebugEnable())
            return;
        System.out.print(str.toString());
    }

    public static void print(String format, Object... data) {
        if (!Config.INSTANCE.getDebugEnable()) return;
        print(String.format(format, data));
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
        int dirx = MathHelper.floor((double) (entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        if (dirx == 3)
            return Direction.XP;
        if (dirx == 0)
            return Direction.ZP;
        if (dirx == 1)
            return Direction.XN;
        return Direction.ZN;
    }

    public static Direction entityLivingHorizontalViewDirection(EntityLivingBase entityLiving) {
        int dirx = MathHelper.floor((double) (entityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
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
	 * public static int getItemBurnTime(ItemStack par0ItemStack) { if (par0ItemStack == null) { return 0; } else { int var1 = par0ItemStack.getItem().shiftedIndex; Items var2 = par0ItemStack.getItem();
	 * 
	 * if (par0ItemStack.getItem() instanceof ItemBlock && Block.blocksList[var1] != null) { Block var3 = Block.blocksList[var1];
	 * 
	 * if (var3 == Block.woodSingleSlab) { return 150; }
	 * 
	 * if (var3.blockMaterial == Material.wood) { return 300; } }
	 * 
	 * if (var2 instanceof ItemTool && ((ItemTool) var2).getToolMaterialName().equals("WOOD")) return 200; if (var2 instanceof ItemSword && ((ItemSword) var2).func_77825_f().equals("WOOD")) return 200; if (var2 instanceof ItemHoe && ((ItemHoe) var2).func_77842_f().equals("WOOD")) return 200; if (var1 == Items.stick.shiftedIndex) return 100; if (var1 == Items.coal.shiftedIndex) return 1600; if (var1 == Items.bucketLava.shiftedIndex) return 20000; if (var1 == Block.sapling.blockID) return 100; if (var1 == Items.blazeRod.shiftedIndex) return 2400; return GameRegistry.getFuelValue(par0ItemStack); } }
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
        if (valueAbs < 0.0001) {
            return "0";
        } else if (valueAbs < 0.000999) {
            return String.format("%1.2fµ",value * 10000);
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

    public static String plotVolt(double value) {
        return plotValue(value, "V  ");
    }

    public static String plotVolt(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotVolt(value);
    }

    public static String plotAmpere(double value) {
        return plotValue(value, "A  ");
    }

    public static String plotAmpere(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotAmpere(value);
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

    public static String plotEnergy(double value) {
        return plotValue(value, "J  ");
    }

    public static String plotEnergy(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotEnergy(value);
    }

    public static String plotRads(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotValue(value, "rad/s ");
    }

    public static String plotER(double E, double R) {
        return plotEnergy("E", E) + plotRads("R", R);
    }

    public static String plotPower(double value) {
        return plotValue(value, "W  ");
    }

    public static String plotPower(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotPower(value);
    }

    public static String plotOhm(double value) {
        return plotValue(value, "\u2126 ");
    }

    public static String plotOhm(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotOhm(value);
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

    public static String plotBuckets(String header, double buckets) {
        if (!header.equals(""))
            header += " ";
        return header + plotValue(buckets, "B ");
    }

    public static void readFromNBT(NBTTagCompound nbt, String str, IInventory inventory) {
        NBTTagList var2 = nbt.getTagList(str, 10);

        for (int var3 = 0; var3 < var2.tagCount(); ++var3) {
            NBTTagCompound var4 = (NBTTagCompound) var2.getCompoundTagAt(var3);
            int var5 = var4.getByte("Slot") & 255;

            if (var5 < inventory.getSizeInventory()) {
                inventory.setInventorySlotContents(var5, new ItemStack(var4));
            }
        }
    }

    public static NBTTagCompound writeToNBT(NBTTagCompound nbt, String str, IInventory inventory) {
        NBTTagList var2 = new NBTTagList();

        for (int var3 = 0; var3 < inventory.getSizeInventory(); ++var3) {
            if (!inventory.getStackInSlot(var3).isEmpty()) {
                NBTTagCompound var4 = new NBTTagCompound();
                var4.setByte("Slot", (byte) var3);
                inventory.getStackInSlot(var3).writeToNBT(var4);
                var2.appendTag(var4);
            }
        }

        nbt.setTag(str, var2);
        return nbt;
    }

    public static void sendPacketToClient(ByteArrayOutputStream bos, EntityPlayerMP player) {
        ElnServerPacket packet = new ElnServerPacket(bos.toByteArray());
        player.connection.sendPacket(packet);
    }

    public static void setGlColorFromDye(int damage) {
        setGlColorFromDye(damage, 1.0f);
    }

    public static void setGlColorFromDye(int damage, float gain) {
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
        return FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dim);
    }

    public static boolean getWorldExist(int dim) {
        return DimensionManager.getWorld(dim) != null;
    }

    public static double getWind(int worldId, int y) {
        if (!getWorldExist(worldId)) {
            return Math.max(0.0, Eln.windProcess.getWind(y));
        } else {
            World world = getWorld(worldId);
            float factor = 1f + world.getRainStrength(0) * 0.2f + world.getThunderStrength(0) * 0.2f;
            return Math.max(0.0, Eln.windProcess.getWind(y) * factor + world.getRainStrength(0) * 1f + world.getThunderStrength(0) * 2f);
        }
    }

    // public static double getWind(World world, int y)
    // {
    // float factor = 1f + world.getRainStrength(0) * 0.2f + world.getWeightedThunderStrength(0) * 0.2f;
    // return Math.max(0.0, Eln.wind.getWind(y) * factor + world.getRainStrength(0) * 1f + world.getWeightedThunderStrength(0) * 2f);
    // }

    public static void dropItem(ItemStack itemStack, BlockPos pos, World world) {
        if (itemStack == null)
            return;
        if (world.getGameRules().getBoolean("doTileDrops")) {
            float var6 = 0.7F;
            double var7 = (double) (world.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var9 = (double) (world.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var11 = (double) (world.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            EntityItem drop = new EntityItem(world, (double) pos.getX() + var7, (double) pos.getY() + var9, (double) pos.getZ() + var11, itemStack);
            drop.setPickupDelay(10);
            world.spawnEntity(drop);
        }
    }

    public static void dropItem(ItemStack itemStack, Coordinate coordinate) {
        dropItem(itemStack, coordinate.pos, coordinate.world());

    }

    public static boolean tryPutStackInInventory(ItemStack stack, IInventory inventory) {
        if (inventory == null) return false;
        int limit = inventory.getInventoryStackLimit();

        // First, make a list of possible target slots.
        ArrayList<Integer> slots = new ArrayList<>(4);
        int need = stack.getCount();
        for (int i = 0; i < inventory.getSizeInventory() && need > 0; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && slot.getCount() < limit && slot.isItemEqual(stack)) {
                slots.add(i);
                need -= limit - slot.getCount();
            }
        }
        for (int i = 0; i < inventory.getSizeInventory() && need > 0; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                slots.add(i);
                need -= limit;
            }
        }

        // Is there space enough?
        if (need > 0) {
            return false;
        }

        // Yes. Proceed.
        int toPut = stack.getCount();
        for (Integer slot : slots) {
            ItemStack target = inventory.getStackInSlot(slot);
            if (target.isEmpty()) {
                int amount = Math.min(toPut, limit);
                inventory.setInventorySlotContents(slot, new ItemStack(stack.getItem(), amount, stack.getItemDamage()));
                toPut -= amount;
            } else {
               int space = limit - target.getCount();
               int amount = Math.min(toPut, space);
               target.setCount(target.getCount() + amount);
               toPut -= amount;
            }
            if (toPut <= 0) break;
        }

        return true;
    }

    @Deprecated
    public static boolean canPutStackInInventory(ItemStack[] stackList, IInventory inventory, int[] slotsIdList) {
        int limit = inventory.getInventoryStackLimit();
        ItemStack[] outputStack = new ItemStack[slotsIdList.length];
        ItemStack[] inputStack = new ItemStack[stackList.length];

        for (int idx = 0; idx < outputStack.length; idx++) {
            if (!inventory.getStackInSlot(slotsIdList[idx]).isEmpty())
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
                    // inventory.decrStackSize(idx, -stack.stackSize);
                    int transferMax = limit - targetStack.getCount();
                    if (transferMax > 0) {
                        int transfer = stack.getCount();
                        if (transfer > transferMax)
                            transfer = transferMax;
                        outputStack[idx].setCount(outputStack[idx].getCount() + transfer);
                        stack.setCount(stack.getCount() - transfer);
                    }

                    if (stack.isEmpty()) {
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

    @Deprecated
    public static boolean tryPutStackInInventory(ItemStack[] stackList, IInventory inventory, int[] slotsIdList) {
        int limit = inventory.getInventoryStackLimit();

        for (ItemStack stack : stackList) {
            for (int i : slotsIdList) {
                ItemStack targetStack = inventory.getStackInSlot(i);
                if (targetStack.isEmpty()) {
                    inventory.setInventorySlotContents(i, stack.copy());
                    stack.setCount(0);
                    break;
                } else if (targetStack.isItemEqual(stack)) {
                    // inventory.decrStackSize(idx, -stack.stackSize);
                    int transferMax = limit - targetStack.getCount();
                    if (transferMax > 0) {
                        int transfer = stack.getCount();
                        if (transfer > transferMax)
                            transfer = transferMax;
                        inventory.decrStackSize(i, -transfer);
                        stack.setCount(stack.getCount() - transfer);
                    }

                    if (stack.isEmpty()) {
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
        } else if (value > 1) {
            return 1;
        }
        return value;
    }

	/*
	 * public static void bindGuiTexture(String string) { Utils.bindTextureByName("/sprites/gui/" + string); }
	 */

    public static void serialiseItemStack(DataOutputStream stream, ItemStack stack) throws IOException {
        if (stack == null) {
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
            if (old == null || Item.getIdFromItem(old.getItem().getItem()) != itemId || old.getItem().getItemDamage() != ItemDamage) {
                BlockPos pos = tileEntity.getPos();
                return new EntityItem(tileEntity.getWorld(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 1.2, Utils.newItemStack(itemId, 1, ItemDamage));
            } else {
                return old;
            }
        }
    }

    public static boolean isGameInPause() {
        return Minecraft.getMinecraft().isGamePaused();
    }

    public static int getLight(World w, EnumSkyBlock e, BlockPos pos) {
        return w.getLightFor(e, pos);
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
        BlockPos pos = t.getPos();
        World w = t.getWorld();
        TileEntity o;
        o = w.getTileEntity(pos.add(1, 0, 0));
        if (o != null && o instanceof ITileEntitySpawnClient)
            ((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
        o = w.getTileEntity(pos.add(-1, 0, 0));
        if (o != null && o instanceof ITileEntitySpawnClient)
            ((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
        o = w.getTileEntity(pos.add(0, 1, 0));
        if (o != null && o instanceof ITileEntitySpawnClient)
            ((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
        o = w.getTileEntity(pos.add(0, -1, 0));
        if (o != null && o instanceof ITileEntitySpawnClient)
            ((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
        o = w.getTileEntity(pos.add(0, 0, 1));
        if (o != null && o instanceof ITileEntitySpawnClient)
            ((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
        o = w.getTileEntity(pos.add(0, 0, -1));
        if (o != null && o instanceof ITileEntitySpawnClient)
            ((ITileEntitySpawnClient) o).tileEntityNeighborSpawn();
    }

    public static boolean playerHasMeter(EntityPlayer entityPlayer) {
        // In case future Minecraft versions allow you to grow more hands.
        for (EnumHand hand : EnumHand.values()) {
            ItemStack heldItem = entityPlayer.getHeldItem(hand);
            if (Items.multiMeterElement.checkSameItemStack(heldItem)
                || Items.thermometerElement.checkSameItemStack(heldItem)
                || Items.allMeterElement.checkSameItemStack(heldItem))
                return true;
        }
        return false;
    }

    public static int getRedstoneLevelAround(Coordinate coord, Direction side) {
        int level = coord.world().getStrongPower(coord.pos);
        if (level >= 15) return 15;

        EnumFacing facing = side.getInverse().toForge();
        switch (side) {
            case YN:
            case YP:
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(1, 0, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(-1, 0, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, 0, 1), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, 0, -1), facing));

            case XN:
            case XP:
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, 1, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, -1, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, 0, 1), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, 0, -1), facing));

            case ZN:
            case ZP:
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, 1, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(0, -1, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(1, 0, 0), facing));
                if (level >= 15) return 15;
                level = Math.max(level, coord.world().getRedstonePower(coord.pos.add(-1, 0, 0), facing));
        }

        return level;
    }

    public static boolean isPlayerAround(World world, AxisAlignedBB axisAlignedBB) {
        return !world.getEntitiesWithinAABB(EntityPlayer.class, axisAlignedBB).isEmpty();
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

    @Deprecated
    static public void getItemStack(String name, List list) {
        // TODO: Fuck this function.
        Iterator itItem = Item.REGISTRY.iterator();
        NonNullList<ItemStack> tempList = NonNullList.create();
        Item item;

        while (itItem.hasNext()) {
            item = (Item) itItem.next();
            if (item != null) {
                item.getSubItems(item.getCreativeTab(), tempList);
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

    public static Vec3d getVec05(Coordinate c) {
        int x = c.pos.getX(), y = c.pos.getY(), z = c.pos.getZ();
        return new Vec3d(x + (x < 0 ? -1 : 1) * 0.5, y + (y < 0 ? -1 : 1) * 0.5, z + (z < 0 ? -1 : 1) * 0.5);
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
        return entityPlayer.isCreative();
    }

    public static boolean mustDropItem(EntityPlayerMP entityPlayer) {
        return entityPlayer == null || !isCreative(entityPlayer);
    }

    public static void serverTeleport(Entity e, double x, double y, double z) {
        if (e instanceof EntityPlayerMP)
            e.setPositionAndUpdate(x, y, z);
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
            if (world.isBlockLoaded(new BlockPos( x, y, z))) {
                //ASKS FOR BLOCK ID with Utils.getBlock()
                Block b = world.getBlockState(new BlockPos(x,y,z)).getBlock();
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
        float getWeight(IBlockState block);
    }

    public static class TraceRayWeightOpaque implements TraceRayWeight {

        @Override
        public float getWeight(IBlockState block) {
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

        float stackRed = 0;
        float d = 0;

        while (d < rangeMax) {
            float xFloor = MathHelper.floor(x);
            float yFloor = MathHelper.floor(y);
            float zFloor = MathHelper.floor(z);

            float dx = x - xFloor, dy = y - yFloor, dz = z - zFloor;
            dx = (vx > 0 ? (1 - dx) * vxInv : -dx * vxInv);
            dy = (vy > 0 ? (1 - dy) * vyInv : -dy * vyInv);
            dz = (vz > 0 ? (1 - dz) * vzInv : -dz * vzInv);

            float dBest = Math.min(Math.min(dx, dy), dz) + 0.01f;

            int xInt = (int) xFloor;
            int yInt = (int) yFloor;
            int zInt = (int) zFloor;

            IBlockState blockState = Blocks.AIR.getDefaultState();

            BlockPos pos = new BlockPos(xInt + posXint, yInt + posYint, zInt + posZint);
            if (!w.isAirBlock(pos))
                blockState = w.getBlockState(pos);

            float dToStack;

            if (d + dBest < rangeMax)
                dToStack = dBest;
            else {
                dToStack = (rangeMax - d);
            }

            stackRed += weight.getWeight(blockState) * dToStack;

            x += vx * dBest;
            y += vy * dBest;
            z += vz * dBest;

            d += dBest;
        }

        return stackRed;
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
        } catch (IllegalArgumentException | SecurityException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static ItemStack[][] getItemStackGrid(IRecipe r) {
        throw new IllegalStateException("The wiki should not be used.");
/*

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
                            if (o instanceof List && !((List) o).isEmpty())
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
                    if (o instanceof List && !((List) o).isEmpty()) {
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
*/
    }

    public static ArrayList<ItemStack> getRecipeInputs(IRecipe r) {
        throw new IllegalStateException("The wiki should not be used.");
/*

        try {
            ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
            if (r instanceof ShapedRecipes) {
                stacks.addAll(Arrays.asList(((ShapedRecipes) r).recipeItems));
            }
            if (r instanceof ShapelessRecipes) {
                for (Object stack : ((ShapelessRecipes) r).recipeItems) {
                    stacks.add((ItemStack) stack);
                }
            }
            if (r instanceof ShapedOreRecipe) {
                for (Object o : ((ShapedOreRecipe) r).getInput()) {
                    if (o instanceof List) {
                        stacks.addAll((List) o);
                    }

                    if (o instanceof ItemStack) {
                        stacks.add((ItemStack) o);
                    }
                }
            }
            if (r instanceof ShapelessOreRecipe) {
                for (Object o : ((ShapelessOreRecipe) r).getInput()) {
                    if (o instanceof List) {
                        stacks.addAll((List) o);
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
*/
    }

    public static double getWorldTime(World world) {
        return world.getWorldTime() / (23999.0);
    }

    public static boolean isWateryEnoughForTurbine(Coordinate waterCoord) {
        IBlockState blockState = waterCoord.getBlockState();
        Block block = blockState.getBlock();
        return Block.isEqualTo(block, FLOWING_WATER) || Block.isEqualTo(block, WATER);
    }

    public static void sendMessage(EntityPlayer entityPlayer, String string) {
        entityPlayer.sendStatusMessage(new TextComponentString(string), true);  // TODO(1.12): Or false?
    }

    public static ItemStack newItemStack(int i, int size, int damage) {
        return new ItemStack(Item.getItemById(i), size, damage);
    }

    public static ItemStack newItemStack(Item i, int size, int damage) {
        return new ItemStack(i, size, damage);
    }

    public static List<NBTTagCompound> getTags(NBTTagCompound nbt) {
        Object[] set = nbt.getKeySet().toArray();

        ArrayList<NBTTagCompound> tags = new ArrayList<NBTTagCompound>();

        for (Object aSet : set) {
            tags.add(nbt.getCompoundTag((String) aSet));
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

//    public static void updateSkylight(Chunk chunk) {
//        chunk.generateSkylightMap();
//    }
//
//    public static void updateAllLightTypes(World world, int xCoord, int yCoord, int zCoord) {
//        world.func_147451_t(xCoord, yCoord, zCoord);
//        world.markBlocksDirtyVertical(xCoord, zCoord, 0, 255);
//    }

    public static int getItemId(ItemStack stack) {
        return Item.getIdFromItem(stack.getItem());
    }

    public static int getItemId(Block block) {
        return Item.getIdFromItem(Item.getItemFromBlock(block));
    }

    // public static RecipesList smeltRecipeList = new RecipesList();

    public static void addSmelting(Item parentItem, int parentItemDamage, ItemStack findItemStack, float f) {
        FurnaceRecipes.instance().addSmeltingRecipe(newItemStack(parentItem, 1, parentItemDamage), findItemStack, f);
    }

    public static void addSmelting(Block parentBlock, int parentItemDamage, ItemStack findItemStack, float f) {
        FurnaceRecipes.instance().addSmeltingRecipeForBlock(parentBlock, findItemStack, f);
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
        return new float[]{Math.abs(obj.zMin * 16), Math.abs(obj.zMax * 16), Math.abs(obj.yMin * 16), Math.abs(obj.yMax * 16)};
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getDisplayName().toLowerCase().contains("wrench");
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
        return plotVolt("U", U) + plotAmpere("I", I) + plotPercent("Value", U / Cable.SVU);
    }

    public static float limit(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    public static double limit(double value, double min, double max) {
        return Math.max(Math.min(value, max), min);
    }

    public static void printFunction(FunctionTable func, double start, double end, double step) {
        Utils.println("********");
        double x;
        for (int idx = 0; (x = start + step * idx) < end + 0.00001; idx++) {
            Utils.println(func.getValue(x));
        }
        Utils.println("********");
    }

    public static int getMetaFromPos(World worldIn, BlockPos pos){
        IBlockState state = worldIn.getBlockState(pos);
        return state.getBlock().getMetaFromState(state);
    }

    public static int getMetaFromPos(Coordinate coord){
        IBlockState state = coord.world().getBlockState(coord.pos);
        return state.getBlock().getMetaFromState(state);
    }

    public static int[] posToArray(BlockPos pos){
        int[] array = new int[3];
        array[0] = pos.getX();
        array[1] = pos.getY();
        array[2] = pos.getZ();
        return array;
    }

}
