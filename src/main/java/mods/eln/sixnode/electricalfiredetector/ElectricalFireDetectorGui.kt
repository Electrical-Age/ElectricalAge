package mods.eln.sixnode.electricalfiredetector

import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class ElectricalFireDetectorGui(player: EntityPlayer, inventory: IInventory, var render: ElectricalFireDetectorRender)
    : GuiContainerEln(ElectricalFireDetectorContainer(player, inventory)) {
    override fun newHelper(): GuiHelperContainer = GuiHelperContainer(this, 176, 166 - 52, 8, 84 - 52)
}
