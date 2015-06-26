package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import net.minecraft.item.ItemStack;

public class ElectricalMachineInventory extends TransparentNodeElementInventory {

    ElectricalMachineElement machineElement;

    public ElectricalMachineInventory(int size, int stackLimit, ElectricalMachineElement machineElement) {
        super(size, stackLimit, machineElement);
        this.machineElement = machineElement;
    }

    public ElectricalMachineInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
        super(size, stackLimit, TransparentnodeRender);
    }


    ElectricalMachineDescriptor getDescriptor() {
        if (transparentNodeRender != null) return ((ElectricalMachineRender) transparentNodeRender).descriptor;
        if (transparentNodeElement != null) return ((ElectricalMachineElement) transparentNodeElement).descriptor;
        return null;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        // ElectricalMachineDescriptor d = getDescriptor();

        if (transparentNodeElement == null) return new int[0];

        switch (Direction.fromIntMinecraftSide(side)) {
            case YP:
                return new int[]{machineElement.inSlotId};
            default:
                int[] slots = new int[machineElement.descriptor.outStackCount];
                for (int idx = 0; idx < slots.length; idx++) {
                    slots[idx] = idx + machineElement.outSlotId;
                }
                return slots;
        }
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, int side) {
        switch (Direction.fromIntMinecraftSide(side)) {
            case YP:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, int side) {
        switch (Direction.fromIntMinecraftSide(side)) {
            case YP:
                return false;
            default:
                return true;
        }
    }
}
