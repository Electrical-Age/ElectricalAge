package mods.eln.sixnode.electricalmath

import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.ItemStackFilter
import mods.eln.gui.SlotFilter
import mods.eln.misc.BasicContainer
import mods.eln.node.NodeBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

import mods.eln.i18n.I18N.tr

class ElectricalMathContainer(node: NodeBase, player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf<Slot>(SlotFilter(inventory, ElectricalMathContainer.restoneSlotId, 125 + 27 + 44 / 2, 25, 64,
        ItemStackFilter(Items.redstone), SlotSkin.medium, arrayOf(tr("Redstone slot"))))) {

    internal var node: NodeBase? = null

    init {
        this.node = node
    }

    companion object {
        val restoneSlotId = 0
    }
}
