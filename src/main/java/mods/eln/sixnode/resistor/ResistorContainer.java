package mods.eln.sixnode.resistor;

import mods.eln.gui.ISlotSkin;
import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

/**
 * Created by svein on 05/08/15.
 */
public class ResistorContainer extends BasicContainer {
    public static final int coreId = 0;

    public ResistorContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
            new SlotFilter(inventory, coreId, 135, 8, 64, ItemStackFilter.OreDict("dustResistive"),
                ISlotSkin.SlotSkin.medium,
                new String[]{tr("Resistive Dust slot"), tr("(Each dust adds more resistance)")})
        }
            );
    }
}
