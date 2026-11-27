package mods.eln.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class OreScanner extends GenericItemUsingDamageDescriptorUpgrade {

    public OreScanner(String name) {
        super(name);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Auto Miner upgrade: scans and extracts ores in a wide radius."));
    }
}
