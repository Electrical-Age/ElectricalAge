package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.i18n.I18N
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
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.ParseException

class TachometerDescriptor(baseName: String, obj: Obj3D) : SimpleShaftDescriptor(baseName,
    TachometerElement::class, null, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Shaft"))
}

open class TachometerElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    companion object {
        val SetRangeEventId = 1

        val DefaultMinRads = 0.0f
        val DefaultMaxRads = 1000f
    }

    override val shaftMass = 0.5
    private val outputGate = NbtElectricalGateOutput("rpmOutput")
    private val outputGateProcess = NbtElectricalGateOutputProcess("rpmOutputProcess", outputGate)
    private var minRads = DefaultMinRads
    private var maxRads = DefaultMaxRads
    private val outputGateSlowProcess = IProcess {
        outputGateProcess.setOutputNormalizedSafe((this.shaft.rads - minRads) / (maxRads - minRads))
    }

    init {
        electricalLoadList.add(outputGate)
        electricalComponentList.add(outputGateProcess)
        slowProcessList.add(outputGateSlowProcess)
    }

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = outputGate

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = if (side == front || side == front.inverse) {
        NodeBase.maskElectricalOutputGate
    } else {
        0
    }

    override fun thermoMeterString(side: Direction?): String? = null

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float,
                                  vz: Float): Boolean = false

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream)
        stream.writeFloat(minRads)
        stream.writeFloat(maxRads)
    }

    override fun hasGui(): Boolean = true

    override fun networkUnserialize(stream: DataInputStream?): Byte {
        val type = super.networkUnserialize(stream)
        when (type.toInt()) {
            SetRangeEventId -> {
                minRads = stream?.readFloat() ?: DefaultMinRads
                maxRads = stream?.readFloat() ?: DefaultMaxRads
                needPublish()
                return unserializeNulldId
            }
        }
        return type
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        minRads = nbt.getFloatOr("minRads", 0f)
        maxRads = nbt.getFloatOr("maxRads", 0f)
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        nbt.putFloat("minRads", minRads)
        nbt.putFloat("maxRads", maxRads)
    }

    override fun getWaila(): Map<String, String> {
        return mapOf()
    }
}

// 26.x port: class TachometerRender removed (client; redone in P3/P4)


// 26.x port: class TachometerGui removed (client; redone in P3/P4)

