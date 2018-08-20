package mods.eln.item.electricalitem;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class BatteryItem extends GenericItemUsingDamageDescriptor implements IItemEnergyBattery {

    private int priority;

    ResourceLocation iconResource;
    double energyStorage, dischargePower, chargePower;

    public BatteryItem(String name, double energyStorage, double chargePower, double dischargePower, int priority) {
        super(name);
        this.priority = priority;
        this.chargePower = chargePower;
        this.dischargePower = dischargePower;
        this.energyStorage = energyStorage;
        iconResource = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + ".png");
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addPortable(newItemStack());
    }

    @Override
    public NBTTagCompound getDefaultNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("energy", 0);
        return nbt;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Charge power: %1$W", Utils.plotValue(chargePower)));
        list.add(tr("Discharge power: %1$W", Utils.plotValue(dischargePower)));
        if (itemStack != null) {
            list.add(tr("Stored energy: %1$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (int) (getEnergy(itemStack) / energyStorage * 100)));
        }
    }

    public double getEnergy(ItemStack stack) {
        return getNbt(stack).getDouble("energy");
    }

    public void setEnergy(ItemStack stack, double value) {
        if (value < 0) value = 0;
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
        return dischargePower;
    }

    @Override
    public int getPriority(ItemStack stack) {
        return priority;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        super.renderItem(type, item, data);
        if (type == ItemRenderType.INVENTORY) {
            UtilsClient.drawEnergyBare(type, (float) (getEnergy(item) / getEnergyMax(item)));
        }
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
    }
}
