package mods.eln.sixnode.powercapacitorsix

import mods.eln.cable.CableRenderType
import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

class PowerCapacitorSixRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {

    var descriptor: PowerCapacitorSixDescriptor
    private val renderPreProcess: CableRenderType? = null

    internal var inventory = SixNodeElementInventory(2, 64, this)

    init {
        this.descriptor = descriptor as PowerCapacitorSixDescriptor
    }

    override fun draw() {
        GL11.glRotatef(90f, 1f, 0f, 0f)
        front.glRotateOnX()
        descriptor.draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return PowerCapacitorSixGui(player, inventory, this)
    }
}
