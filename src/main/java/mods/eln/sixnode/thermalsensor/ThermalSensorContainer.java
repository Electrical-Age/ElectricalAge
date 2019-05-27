package mods.eln.sixnode.thermalsensor;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.currentcable.CurrentCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class ThermalSensorContainer extends BasicContainer {

    public static final int cableSlotId = 0;

    public ThermalSensorContainer(EntityPlayer player, IInventory inventory, boolean acceptsElectricalCable) {
        super(player, inventory, new Slot[]{
            new SixNodeItemSlot(inventory, cableSlotId, 152, 62, 1, acceptsElectricalCable ?
                new Class[]{ThermalCableDescriptor.class, ElectricalCableDescriptor.class, CurrentCableDescriptor.class} :
                new Class[]{ThermalCableDescriptor.class}, SlotSkin.medium,
                new String[]{tr("Cable slot")})
        });
    }
}
