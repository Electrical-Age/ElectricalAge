package mods.eln.item;

import mods.eln.misc.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Auto Miner drill head: %1$W (%2$s, %3$J per block).",
            Utils.plotValue(nominalPower), Utils.plotValue(operationTime), Utils.plotValue(OperationEnergy)));
    }
}
