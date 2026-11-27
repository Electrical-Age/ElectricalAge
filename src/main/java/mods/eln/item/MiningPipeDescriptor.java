package mods.eln.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class MiningPipeDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public MiningPipeDescriptor(String name) {
        super(name);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Extends an Auto Miner's drill shaft downward."));
    }
}
