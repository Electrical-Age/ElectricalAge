package mods.eln.item.regulator;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.sim.RegulatorType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class RegulatorSlot extends GenericItemUsingDamageSlot {


    RegulatorType[] type;

    public RegulatorSlot(IInventory inventory, int slot, int x, int y,
                         int stackLimit, RegulatorType[] type, SlotSkin skin) {
        super(inventory, slot, x, y, stackLimit, IRegulatorDescriptor.class, skin, new String[]{"Regulator Slot"});
        this.type = type;
    }

    @Override
    public boolean isItemValid(ItemStack itemStack) {
        if (!super.isItemValid(itemStack)) return false;
        IRegulatorDescriptor element = (IRegulatorDescriptor) IRegulatorDescriptor.getDescriptor(itemStack);
        for (RegulatorType t : type) {
            if (t == element.getType()) return true;
        }
        return false;
    }
}
