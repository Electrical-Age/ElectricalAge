package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUMask
import mods.eln.misc.Obj3D
import mods.eln.node.NodeBase
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer
import java.io.DataInputStream
import java.io.DataOutputStream

class TachometerDescriptor(baseName : String, obj : Obj3D): SimpleShaftDescriptor(baseName,
        TachometerElement::class, TachometerRender::class, EntityMetaTag.Basic) {
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

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream)
    }
}

class TachometerRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor): ShaftRender(entity, desc) {
    private var renderPreProcess: CableRenderType? = null
    private val connections = LRDUMask()

    override fun draw() {
        renderPreProcess = drawCable(Direction.YN, Eln.instance.stdCableRenderSignal, connections, renderPreProcess)
        super.draw()
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connections.deserialize(stream)
    }
}
