package mods.eln.transparentnode

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.player.EntityPlayer

class FridgeDescriptor(name: String, obj: Obj3D) :
        TransparentNodeDescriptor(name, FridgeElement::class.java, FridgeRender::class.java) {
    val main: Obj3D.Obj3DPart = obj.getPart("core")
    val fridgeDoor: Obj3D.Obj3DPart = obj.getPart("doorfridge")
    val freezerDoor: Obj3D.Obj3DPart = obj.getPart("doorfreezer")

    internal fun draw(open: Float = 0f) {
        main.draw()
        fridgeDoor.draw(open * -90, 0f, 1f, 0f)
        freezerDoor.draw(open * -90, 0f, 1f, 0f)
    }
}

class FridgeElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElement(node, descriptor) {
    override fun initialize() {}
    override fun getConnectionMask(side: Direction?, lrdu: LRDU?) = 0
    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun multiMeterString(side: Direction?) = ""
    override fun thermoMeterString(side: Direction?) = ""
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false
}

class FridgeRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElementRender(entity, descriptor) {
    private val descriptor = descriptor as FridgeDescriptor
    var open = 0f

    override fun draw() {
        front.glRotateZnRef()
        descriptor.draw(open)
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        open += deltaT / 5
        if (open > 1) open = 0f
    }
}
