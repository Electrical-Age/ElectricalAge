package mods.eln.item.electricalitem;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import java.util.List;

public class ElectricalTool extends GenericItemUsingDamageDescriptor implements IItemEnergyBattery {

    int light, range;
    double energyStorage, energyPerBlock, chargePower;

    float strengthOff, strengthOn;

    ResourceLocation rIcon;

    public ElectricalTool(String name, float strengthOn, float strengthOff,
                          double energyStorage, double energyPerBlock, double chargePower) {
        super(name);

        this.chargePower = chargePower;
        this.energyPerBlock = energyPerBlock;
        this.energyStorage = energyStorage;
        this.strengthOn = strengthOn;
        this.strengthOff = strengthOff;

        rIcon = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + ".png");
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        if (entityLiving.worldObj.isRemote) return false;

        Eln.itemEnergyInventoryProcess.addExclusion(this, 2);
        return super.onEntitySwing(entityLiving, stack);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World w, Block block, int x, int y, int z, EntityLivingBase entity) {
        if (getStrVsBlock(stack, block) == strengthOn) {
            double e = getEnergy(stack) - energyPerBlock;
            if (e < 0) e = 0;
            setEnergy(stack, e);
        }
        Utils.println("destroy");
        return true;
    }

    // public static final Block[] blocksEffectiveAgainst = new Block[] {Block.cobblestone, Block.stoneDoubleSlab, Block.stoneSingleSlab, Block.stone, Block.sandStone, Block.cobblestoneMossy, Block.oreIron, Block.blockIron, Block.oreCoal, Block.blockGold, Block.oreGold, Block.oreDiamond, Block.blockDiamond, Block.ice, Block.netherrack, Block.oreLapis, Block.blockLapis, Block.oreRedstone, Block.oreRedstoneGlowing, Block.rail, Block.railDetector, Block.railPowered, Block.railActivator};
    public static final Block[] blocksEffectiveAgainst = new Block[]{Blocks.grass, Blocks.dirt, Blocks.sand, Blocks.gravel, Blocks.snow, Blocks.snow, Blocks.clay, Blocks.farmland, Blocks.soul_sand, Blocks.mycelium};

    //@Override
    //public abstract float getStrVsBlock(ItemStack stack, Block block);

    public float getStrength(ItemStack stack) {
        return getEnergy(stack) >= energyPerBlock ? strengthOn : strengthOff;
    }

    @Override
    public NBTTagCompound getDefaultNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("energy", 0);
        nbt.setBoolean("powerOn", false);
        nbt.setInteger("rand", (int) (Math.random() * 0xFFFFFFF));
        return nbt;
    }

    boolean getPowerOn(ItemStack stack) {
        return getNbt(stack).getBoolean("powerOn");
    }

    void setPowerOn(ItemStack stack, boolean value) {
        getNbt(stack).setBoolean("powerOn", value);
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
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(Utils.plotEnergy("Energy Stored:", getEnergy(itemStack)) + "(" + (int) (getEnergy(itemStack) / energyStorage * 100) + "%)");
        //list.add("Power button is " + (getPowerOn(itemStack) ? "ON" : "OFF"));
    }

    public double getEnergy(ItemStack stack) {
        return getNbt(stack).getDouble("energy");
    }

    public void setEnergy(ItemStack stack, double value) {
        getNbt(stack).setDouble("energy", value);
    }

    @Override
    public double getEnergyMax(ItemStack stack) {
        return energyStorage;
    }

    @Override
    public double getChargePower(ItemStack stack) {
        return chargePower;
    }

    @Override
    public double getDischagePower(ItemStack stack) {
        return 0;
    }

    @Override
    public int getPriority(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (type == ItemRenderType.INVENTORY)
            return false;
        return true;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY)
            UtilsClient.drawEnergyBare(type, (float) (getEnergy(item) / getEnergyMax(item)));
        UtilsClient.drawIcon(type, rIcon);
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
    }
}
