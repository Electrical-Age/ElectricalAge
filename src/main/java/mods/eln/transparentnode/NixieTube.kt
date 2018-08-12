package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class NixieTubeDescriptor(val name: String, val obj: Obj3D) : TransparentNodeDescriptor(name, NixieTubeElement::class.java, NixieTubeRender::class.java) {
    val display = obj.getPart("display")
    val base = obj.getPart("base")
    val tube = obj.getPart("tube")

    val pinDistance = Utils.getSixNodePinDistance(base)!!

    val voltageLevelColor = VoltageLevelColor.Neutral

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list?.add(tr("Displays a single glowing digit."))
    }

    fun draw(_digit: Int, blank: Boolean) {
        var digit = _digit
        if(digit < 0) digit = 0
        if(digit > 9) digit = 9

        base.draw()

        UtilsClient.enableBlend()
        UtilsClient.disableLight()
        UtilsClient.disableCulling()
        obj.bindTexture("digit_atlas.png")
        if(blank) {
            display.draw(10.0f / 16.0f, 0.0f)
        } else {
            display.draw(digit.toFloat() / 16.0f, 0.0f)
        }
        UtilsClient.enableLight()
        UtilsClient.enableCulling()

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f)
        tube.draw()
        UtilsClient.disableBlend()
    }
}

class NixieTubeElement(node: TransparentNode, _descriptor: TransparentNodeDescriptor) : TransparentNodeElement(node, _descriptor) {
    val descriptor = _descriptor as NixieTubeDescriptor

    val digitIn = NbtElectricalGateInput("digitIn")
    val blankIn = NbtElectricalGateInput("blankIn")

    var curDigit = 0
    var lastDigit = 0
    var curBlank = false
    var lastBlank = false

    inner class NixieTubeProcess : IProcess {
        override fun process(time: Double) {
            //Utils.println("NTP.p")
            curDigit = (digitIn.normalized * 10.0).toInt()
            curBlank = (blankIn.normalized >= 0.5)
            if (curDigit != lastDigit) {
                lastDigit = curDigit
                needPublish()
            }
            if (curBlank != lastBlank) {
                lastBlank = curBlank
                needPublish()
            }
        }
    }

    var process: NixieTubeProcess? = null

    override fun initialize() {
        electricalLoadList.add(digitIn)
        electricalLoadList.add(blankIn)
        process = NixieTubeProcess()
        slowProcessList.add(process)
        reconnect()
        Utils.println("NTE.initialize")
    }

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? {
        if(lrdu != LRDU.Down) return null
        return when(side) {
            front -> blankIn
            front.inverse -> digitIn
            else -> null
        }
    }

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        if(lrdu != LRDU.Down) return 0
        return when(side) {
            front -> NodeBase.maskElectricalInputGate
            front.inverse -> NodeBase.maskElectricalInputGate
            else -> 0
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        //Utils.println("NTE.nS")
        try {
            stream.writeInt(curDigit)
            stream.writeBoolean(curBlank)
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun thermoMeterString(side: Direction?): String = ""
    override fun multiMeterString(side: Direction?): String = ""
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean = false
}

class NixieTubeRender(entity: TransparentNodeEntity, _descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, _descriptor) {
    val descriptor = _descriptor as NixieTubeDescriptor

    var digit = 0
    var blank = false

    override fun draw() {
        preserveMatrix {
            front.glRotateXnRef()
            descriptor.draw(digit, blank)
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        //Utils.println("NTR.nU")
        try {
            digit = stream.readInt()
            blank = stream.readBoolean()
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCableRender(side: Direction?, lrdu: LRDU?): CableRenderDescriptor? {
        if(lrdu != LRDU.Down) return null
        if(side == front || side == front.inverse) return Eln.instance.stdCableRenderSignal
        return null
    }
}
