package mods.eln.sixnode.resistor

import mods.eln.cable.CableRenderType
import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

import java.io.DataInputStream
import java.io.IOException

class ResistorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {

    var descriptor: ResistorDescriptor
    internal var inventory = SixNodeElementInventory(2, 64, this)
    private val renderPreProcess: CableRenderType? = null

    private var wiperPos = 0f

    init {
        this.descriptor = descriptor as ResistorDescriptor
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            if (descriptor.isRheostat) wiperPos = stream.readFloat()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun draw() {
        GL11.glRotatef(90f, 1f, 0f, 0f)
        front.glRotateOnX()
        descriptor.draw(wiperPos)
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return ResistorGui(player, inventory, this)
    }
}
