package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N
import mods.eln.item.IConfigurable
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
import mods.eln.sixnode.electricaldatalogger.DataLogs
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.ParseException

class TachometerDescriptor(baseName: String, obj: Obj3D) : SimpleShaftDescriptor(baseName,
    TachometerElement::class, TachometerRender::class, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Shaft"))
}

open class TachometerElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_), IConfigurable {
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

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float,
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

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        minRads = nbt.getFloat("minRads")
        maxRads = nbt.getFloat("maxRads")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setFloat("minRads", minRads)
        nbt.setFloat("maxRads", maxRads)
    }

    override fun getWaila(): Map<String, String> {
        return mapOf()
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if(compound.hasKey("min"))
            minRads = compound.getFloat("min")
        if(compound.hasKey("max"))
            maxRads = compound.getFloat("max")
        needPublish()
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setFloat("min", minRads)
        compound.setFloat("max", maxRads)
        compound.setByte("unit", DataLogs.noType)
    }
}

class TachometerRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    override val cableRender: CableRenderDescriptor? = null
    private var renderPreProcess: CableRenderType? = null
    private val connections = LRDUMask()
    internal var minRads = TachometerElement.DefaultMinRads
    internal var maxRads = TachometerElement.DefaultMaxRads

    override fun draw() {
        renderPreProcess = drawCable(Direction.YN, Eln.instance.stdCableRenderSignal, connections, renderPreProcess)
        super.draw()
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connections.deserialize(stream)
        minRads = stream.readFloat()
        maxRads = stream.readFloat()
    }

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? = TachometerGui(this)
}

class TachometerGui(val render: TachometerRender) : GuiScreenEln() {
    val validate: GuiButton by lazy { newGuiButton(82, 12, 80, I18N.tr("Validate")) }
    val lowValue: GuiTextFieldEln by lazy { newGuiTextField(8, 24, 70) }
    val highValue: GuiTextFieldEln by lazy { newGuiTextField(8, 8, 70) }

    override fun newHelper(): GuiHelper? = GuiHelper(this, 169, 44)

    override fun initGui() {
        super.initGui()
        validate.enabled = true
        lowValue.setComment(I18N.tr("Rads/s corresponding\nto 0% output").split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
        highValue.setComment(I18N.tr("Rads/s corresponding\nto 100% output").split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
        lowValue.setText(render.minRads)
        highValue.setText(render.maxRads)
    }

    override fun guiObjectEvent(`object`: IGuiObject?) {
        super.guiObjectEvent(`object`)
        if (`object` === validate) {

            try {
                val minRads = NumberFormat.getInstance().parse(lowValue.text).toFloat()
                val maxRads = NumberFormat.getInstance().parse(highValue.text).toFloat()

                try {
                    val bos = ByteArrayOutputStream()
                    val stream = DataOutputStream(bos)

                    render.preparePacketForServer(stream)

                    stream.writeByte(TachometerElement.SetRangeEventId)
                    stream.writeFloat(minRads)
                    stream.writeFloat(maxRads)

                    render.sendPacketToServer(bos)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } catch (e: ParseException) {
            }
        }
    }
}
