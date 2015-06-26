package mods.eln.sixnode.powercapacitorsix;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.item.DielectricItem;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class PowerCapacitorSixContainer extends BasicContainer {

    static final int redId = 0;
    static final int dielectricId = 1;

    public PowerCapacitorSixContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
                new SlotFilter(inventory, redId, 132, 8, 13, new ItemStackFilter[]{new ItemStackFilter(Items.redstone)}, SlotSkin.medium, new String[]{"Redstone slot", "(Increases capacity)"}),
                new GenericItemUsingDamageSlot(inventory, dielectricId, 132 + 20, 8, 20, DielectricItem.class, SlotSkin.medium, new String[]{"Dielectric slot", "(Increases maximum voltage)"})
        });
    }
}
