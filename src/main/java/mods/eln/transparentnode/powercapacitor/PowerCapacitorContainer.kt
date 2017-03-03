package mods.eln.transparentnode.powercapacitor

import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.ItemStackFilter
import mods.eln.gui.SlotFilter
import mods.eln.item.DielectricItem
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

import mods.eln.i18n.I18N.tr

class PowerCapacitorContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf<Slot>(SlotFilter(inventory, PowerCapacitorContainer.redId, 132, 8, 13,
        ItemStackFilter(Items.redstone),
        SlotSkin.medium, arrayOf(tr("Redstone slot"), tr("(Increases capacity)"))), GenericItemUsingDamageSlot(inventory, PowerCapacitorContainer.dielectricId, 132 + 20, 8, 20,
        DielectricItem::class.java, SlotSkin.medium,
        arrayOf(tr("Dielectric slot"), tr("(Increases maximal voltage)"))))) {

    companion object {
        internal val redId = 0
        internal val dielectricId = 1
    }
}
