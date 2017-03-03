package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import net.minecraft.entity.player.EntityPlayer

/**
 * A comparator-alike. It doesn't "compare" anything, though.
 */
class ScannerDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, ScannerElement::class.java, ScannerRender::class.java) {

    val main = obj.getPart("main")!!

    fun draw() {
        main.draw()
    }

}


class ScannerElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {
    override fun getElectricalLoad(lrdu: LRDU?) = null
    override fun getThermalLoad(lrdu: LRDU?) = null

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.inverse() -> NodeBase.maskElectricalOutputGate
        else -> 0
    }

    override fun multiMeterString() = ""

    override fun thermoMeterString() = ""

    override fun initialize() {
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false
}


class ScannerRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(entity, side, descriptor) {
    val desc = descriptor as ScannerDescriptor

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        desc.draw()
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor = Eln.instance.signalCableDescriptor.render
}
