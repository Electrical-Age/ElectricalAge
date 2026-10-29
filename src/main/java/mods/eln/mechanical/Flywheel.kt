package mods.eln.mechanical

import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor

class FlywheelDescriptor(baseName: String, obj: Obj3D) : SimpleShaftDescriptor(baseName,
    FlyWheelElement::class, null) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Flywheel"), obj.getPart("Shaft"))
}

class FlyWheelElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : StraightJointElement(node, desc_) {
    /** 200 kJ at 800 rad/s: 12.5 s of a 16 kW turbine (Argon: H 10-15 s, for stability and as a store). */
    override val shaftInertia = inertiaOf(12.5, 16000.0)

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Speed", Utils.plotRads("", shaft.rads))
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        return info
    }
}
