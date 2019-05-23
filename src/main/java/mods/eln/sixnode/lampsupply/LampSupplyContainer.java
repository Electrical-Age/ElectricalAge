package mods.eln.sixnode.lampsupply;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.currentcable.CurrentCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.genericcable.GenericCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class LampSupplyContainer extends BasicContainer {

    public static final int cableSlotId = 0;

    public LampSupplyContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
            new SixNodeItemSlot(inventory, cableSlotId, 184, 144, 64, new Class[]{ElectricalCableDescriptor.class, CurrentCableDescriptor.class},
                SlotSkin.medium,
                tr("Electrical cable slot\nBase range is 32 blocks.\nEach additional cable\nincreases range by one.").split("\n")
            )
        });
    }
}
