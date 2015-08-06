package mods.eln.sixnode.resistor;

import mods.eln.gui.ISlotSkin;
import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/**
 * Created by svein on 05/08/15.
 */
public class ResistorContainer extends BasicContainer {
    static final int coreId = 0;

    public ResistorContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
                new SlotFilter(inventory, coreId, 132, 8, 64, ItemStackFilter.OreDict("dustCoal"), ISlotSkin.SlotSkin.medium, new String[]{"Coal dust slot", "(Sets resistance)"})
        });
    }
}
