package mods.eln.sixnode.energymeter;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class EnergyMeterContainer extends BasicContainer {

    public static final int cableSlotId = 0;

    public EnergyMeterContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
                new SixNodeItemSlot(inventory, cableSlotId, 160, 106, 1, new Class[]{ElectricalCableDescriptor.class}, SlotSkin.medium, new String[]{"Electrical Cable slot"})
        });
    }
}
