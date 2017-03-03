package mods.eln.transparentnode.powercapacitor

import mods.eln.cable.CableRenderType
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer

import java.io.DataInputStream

class PowerCapacitorRender(tileEntity: TransparentNodeEntity,
                           descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(tileEntity, descriptor) {

    var descriptor: PowerCapacitorDescriptor
    private val renderPreProcess: CableRenderType? = null

    init {
        this.descriptor = descriptor as PowerCapacitorDescriptor
    }


    override fun draw() {
        descriptor.draw()
    }

    override fun refresh(deltaT: Float) {
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
    }

    internal var inventory = TransparentNodeElementInventory(2, 64, this)

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return PowerCapacitorGui(player, inventory, this)
    }

}
