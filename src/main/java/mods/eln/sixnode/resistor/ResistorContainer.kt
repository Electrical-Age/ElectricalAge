package mods.eln.sixnode.resistor

import mods.eln.gui.ISlotSkin
import mods.eln.gui.ItemStackFilter
import mods.eln.gui.SlotFilter
import mods.eln.i18n.I18N.tr
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

/**
 * Created by svein on 05/08/15.
 */
class ResistorContainer(player: EntityPlayer, inventory: IInventory)
    : BasicContainer(player, inventory,
    arrayOf<Slot>(
        SlotFilter(inventory, ResistorContainer.coreId, 132, 8, 64,
            ItemStackFilter("dustCoal").add("dustCharcoal"),
            ISlotSkin.SlotSkin.medium,
            arrayOf(tr("Coal dust slot"), tr("(Sets resistance)"))))) {

    companion object {
        val coreId = 0
    }
}
