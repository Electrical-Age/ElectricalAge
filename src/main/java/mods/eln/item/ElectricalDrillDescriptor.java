package mods.eln.item;

import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ElectricalDrillDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public double nominalPower;
    public double operationTime, OperationEnergy;

    public ElectricalDrillDescriptor(String name, double operationTime, double operationEnergy) {
        super(name);
        this.OperationEnergy = operationEnergy;
        this.operationTime = operationTime;
        nominalPower = operationEnergy / operationTime;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add("Nominal :");
        list.add(Utils.plotPower("    Power :", nominalPower));
        list.add(Utils.plotTime("    Time per Operation :", operationTime));
        list.add(Utils.plotEnergy("Energy per Operation :", OperationEnergy));
    }
}
