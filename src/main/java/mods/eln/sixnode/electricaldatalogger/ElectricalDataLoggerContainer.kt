package mods.eln.sixnode.electricaldatalogger

import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.ItemStackFilter
import mods.eln.gui.SlotFilter
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

class ElectricalDataLoggerContainer(player: EntityPlayer, inventory: IInventory)
    : BasicContainer(player, inventory, arrayOf<Slot>(SlotFilter(inventory, ElectricalDataLoggerContainer.paperSlotId, 176 / 2 - 44, 125, 64, ItemStackFilter(Items.paper), SlotSkin.medium, arrayOf("Paper Slot")), GenericItemUsingDamageSlot(inventory, ElectricalDataLoggerContainer.printSlotId, 176 / 2 + 45 - 17, 125, 1, DataLogsPrintDescriptor::class.java, SlotSkin.medium, arrayOf<String>()))) {

    companion object {
        val paperSlotId = 0
        val printSlotId = 1
    }
}
