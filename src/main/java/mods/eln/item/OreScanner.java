package mods.eln.item;

import mods.eln.misc.VoltageLevelColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class OreScanner extends GenericItemUsingDamageDescriptorUpgrade {

    public OreScanner(String name) {
        super(name);
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        /*
		list.add("Nominal :");
		list.add(Utils.plotEnergy("Energy per Operation :", OperationEnergy));
		list.add("Scan Area :" + (radius * 2 + 1) * (radius * 2 + 1) + " blocks");
*/
    }
}
