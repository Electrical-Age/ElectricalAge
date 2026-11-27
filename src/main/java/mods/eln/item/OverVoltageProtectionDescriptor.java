package mods.eln.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class OverVoltageProtectionDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public OverVoltageProtectionDescriptor(String name) {
        super(name);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Battery upgrade: protects against over-voltage and stops discharge below 10% (resumes above 15%)."));
    }
}
