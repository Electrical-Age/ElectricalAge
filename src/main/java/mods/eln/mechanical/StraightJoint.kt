package mods.eln.mechanical

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.player.EntityPlayer

class StraightJointDescriptor(baseName : String, obj : Obj3D): SimpleShaftDescriptor(baseName,
        StraightJointElement::class, ShaftRender::class, EntityMetaTag.Fluid) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Shaft"))
}

class StraightJointElement(node : TransparentNode, desc_ : TransparentNodeDescriptor): SimpleShaftElement(node, desc_) {
    override val shaftMass = 0.5

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = 0

    override fun thermoMeterString(side: Direction?): String? = null

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float,
                                  vz: Float): Boolean = false
}
