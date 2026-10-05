package mods.eln.mechanical

import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor

class FlywheelDescriptor(baseName: String, obj: Obj3D?) : SimpleShaftDescriptor(baseName,
    FlyWheelElement::class, ShaftRender::class, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = objParts(obj, "Stand", "Cowl")
    override val rotating = objParts(obj, "Flywheel", "Shaft")
}

class FlyWheelElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : StraightJointElement(node, desc_) {
    override val shaftMass = 100.0

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Speed", Utils.plotRads("", shaft.rads))
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        return info
    }
}
