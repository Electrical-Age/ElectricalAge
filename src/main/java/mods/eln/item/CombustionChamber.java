package mods.eln.item;

import mods.eln.misc.VoltageLevelColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class CombustionChamber extends GenericItemUsingDamageDescriptorUpgrade {

    public CombustionChamber(String name) {
        super(name);
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Upgrade for the Stone Heat Furnace."));
    }
}
