package mods.eln.mechanical

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.NodeBase
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer

class TachometerDescriptor(baseName : String, obj : Obj3D): SimpleShaftDescriptor(baseName,
        TachometerElement::class, ShaftRender::class, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Shaft"))
}

open class TachometerElement(node : TransparentNode, desc_ : TransparentNodeDescriptor): SimpleShaftElement(node, desc_) {
    override val shaftMass = 0.5
    private val outputGate = NbtElectricalGateOutput("rpmOutput")
    private val outputGateProcess = NbtElectricalGateOutputProcess("rpmOutputProcess", outputGate)
    private val outputGateSlowProcess = IProcess {
        outputGateProcess.setOutputNormalizedSafe(this.shaft.rads / 50) // TODO: Make configurable
    }

    init {
        electricalLoadList.add(outputGate)
        electricalComponentList.add(outputGateProcess)
        slowProcessList.add(outputGateSlowProcess)
    }

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = outputGate

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = if (side == front|| side == front.inverse) {
        NodeBase.maskElectricalOutputGate
    } else {
        0
    }

    override fun thermoMeterString(side: Direction?): String? = null

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float,
                                  vz: Float): Boolean = false
}

// TODO: Custom render that draws the signal cable.
