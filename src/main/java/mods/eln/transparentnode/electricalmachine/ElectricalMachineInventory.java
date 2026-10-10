package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

public class ElectricalMachineInventory extends TransparentNodeElementInventory {
    private ElectricalMachineElement machineElement;

    public ElectricalMachineInventory(int size, int stackLimit, ElectricalMachineElement machineElement) {
        super(size, stackLimit, machineElement);
        this.machineElement = machineElement;
    }
    // 26.x port: the client (render) constructor comes back with P4 menus

    ElectricalMachineDescriptor getDescriptor() {
        if (transparentNodeElement != null) return ((ElectricalMachineElement) transparentNodeElement).descriptor;
        return null;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
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
