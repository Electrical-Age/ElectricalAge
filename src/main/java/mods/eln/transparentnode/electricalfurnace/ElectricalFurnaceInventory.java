package mods.eln.transparentnode.electricalfurnace;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.world.item.ItemStack;

public class ElectricalFurnaceInventory extends TransparentNodeElementInventory {

    public ElectricalFurnaceInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        super(size, stackLimit, TransparentNodeElement);
    }
// 26.x port: ElectricalFurnaceInventory() removed (client rendering; redone in P3)

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        switch (Direction.fromIntMinecraftSide(side)) {
            case YP:
                return new int[]{ElectricalFurnaceElement.inSlotId};
            default:
                return new int[]{ElectricalFurnaceElement.outSlotId};
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
