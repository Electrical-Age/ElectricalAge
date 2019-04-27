package mods.eln.item;

import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalDrillDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public double nominalPower;
    public double operationTime;
    public double OperationEnergy;

    public ElectricalDrillDescriptor(String name, double operationTime, double operationEnergy) {
        super(name);
        this.OperationEnergy = operationEnergy;
        this.operationTime = operationTime;
        nominalPower = operationEnergy / operationTime;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Nominal:"));
        list.add("  " + tr("Power: %sW", Utils.plotValue(nominalPower)));
        list.add("  " + tr("Time per operation: %sh", operationTime));
        list.add("  " + tr("Energy per operation: %sJ", Utils.plotValue(OperationEnergy)));
    }
}
