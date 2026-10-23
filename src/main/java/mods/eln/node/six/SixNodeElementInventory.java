package mods.eln.node.six;

import mods.eln.node.ElementInventory;
import net.minecraft.world.item.ItemStack;

/** Inventory of a six-node element. Six-node devices expose no slots to hoppers or pipes. */
public class SixNodeElementInventory extends ElementInventory {
    private final SixNodeElement sixNodeElement;

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElement sixNodeElement) {
        super(size, stackLimit);
        this.sixNodeElement = sixNodeElement;
    }

    @Override
    public String getInventoryName() {
        return "tco.SixNodeInventory";
    }

    @Override
    public void markDirty() {
        if (sixNodeElement != null && !sixNodeElement.sixNode.isDestructing()) {
            sixNodeElement.inventoryChanged();
        }
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return false;
    }
}
