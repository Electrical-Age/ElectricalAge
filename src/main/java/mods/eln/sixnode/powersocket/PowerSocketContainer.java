package mods.eln.sixnode.powersocket;

import mods.eln.gui.ISlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.currentcable.CurrentCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class PowerSocketContainer extends BasicContainer {

    public static final int cableSlotId = 0;

    public PowerSocketContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
            new SixNodeItemSlot(
                inventory,
                cableSlotId,
                150, 8,
                1,
                new Class[]{ElectricalCableDescriptor.class, CurrentCableDescriptor.class},
                ISlotSkin.SlotSkin.medium,
                tr("Electrical cable slot\nMust be populated to connect.").split("\n"))
        });
    }
}
