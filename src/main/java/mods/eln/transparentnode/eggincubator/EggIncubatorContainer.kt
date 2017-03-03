package mods.eln.transparentnode.eggincubator

import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.ItemStackFilter
import mods.eln.gui.SlotFilter
import mods.eln.misc.BasicContainer
import mods.eln.node.INodeContainer
import mods.eln.node.Node
import mods.eln.node.NodeBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

import mods.eln.i18n.I18N.tr

class EggIncubatorContainer(player: EntityPlayer, inventory: IInventory, private val node: Node)//	new SlotFilter(inventory, 1, 62 + 18, 17, 1, new ItemStackFilter[]{new ItemStackFilter(Eln.sixNodeBlock, 0xFF, Eln.electricalCableId)})
    : BasicContainer(player, inventory, arrayOf<Slot>(SlotFilter(inventory, EggIncubatorContainer.EggSlotId, 176 / 2 - 8, 7, 64,
        ItemStackFilter(Items.egg),
        SlotSkin.medium, arrayOf(tr("Egg slot"))))), INodeContainer {

    override fun getNode(): NodeBase {
        return node
    }

    override fun getRefreshRateDivider(): Int {
        return 1
    }

    companion object {
        val EggSlotId = 0
    }
}
