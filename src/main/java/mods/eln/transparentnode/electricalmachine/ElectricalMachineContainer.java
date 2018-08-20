package mods.eln.transparentnode.electricalmachine;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class ElectricalMachineContainer extends BasicContainer implements INodeContainer {
    private NodeBase node = null;

    public ElectricalMachineContainer(NodeBase node, EntityPlayer player, IInventory inventory,
                                      ElectricalMachineDescriptor descriptor) {
        super(player, inventory, getSlot(inventory, descriptor));
        this.node = node;
    }

    private static Slot[] getSlot(IInventory inventory, ElectricalMachineDescriptor descriptor) {
        Slot[] slots = new Slot[2 + descriptor.outStackCount];
        for (int idx = 0; idx < descriptor.outStackCount; idx++) {
            slots[idx] = new SlotWithSkin(inventory, idx, 130 - 32 + idx * 18, 12, SlotSkin.medium);
        }
        slots[descriptor.outStackCount] = new SlotWithSkin(inventory,
            descriptor.outStackCount, 8 + 36, 12, SlotSkin.medium);
        slots[descriptor.outStackCount + 1] = new GenericItemUsingDamageSlot(inventory,
            descriptor.outStackCount + 1, 8, 12, 5,
            MachineBoosterDescriptor.class,
            SlotSkin.medium,
            new String[]{tr("Booster slot")});

        return slots;
    }

    @Override
    public NodeBase getNode() {
        return node;
    }

    @Override
    public int getRefreshRateDivider() {
        return 1;
    }
}
