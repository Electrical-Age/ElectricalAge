package mods.eln.sixnode

import mods.eln.misc.*
import mods.eln.node.Node
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.ResistorSwitch
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class EmergencyLampDescriptor(name: String, val nominalVoltage: Double, val batteryCapacity: Double, model: Obj3D)
    : SixNodeDescriptor(name, EmergencyLampElement::class.java,
    EmergencyLampRender::class.java) {

    val mainCeiling: Obj3D.Obj3DPart = model.getPart("coreCeil")
    val lightCeiling: Obj3D.Obj3DPart = model.getPart("lightCeil")
    val mainWall: Obj3D.Obj3DPart = model.getPart("coreWall")
    val lightWall: Obj3D.Obj3DPart = model.getPart("lightWall")

    init {
        voltageLevelColor = VoltageLevelColor.fromVoltage(nominalVoltage)
    }

    fun draw(onCeiling: Boolean = false, on: Boolean = false) {
        if (onCeiling) {
            if (on)
                preserveMatrix {
                    UtilsClient.drawLight(mainCeiling)
                    GL11.glColor3f(0.5f, 0.5f, 0.5f)
                    UtilsClient.drawLight(lightCeiling)
                }
            else
                mainCeiling.draw()
        } else {
            if (on)
                preserveMatrix {
                    UtilsClient.drawLight(mainWall)
                    GL11.glColor3f(0.5f, 0.5f, 0.5f)
                    UtilsClient.drawLight(lightWall)
                }
            else
                mainWall.draw()
        }


    }

    override fun getFrontFromPlace(side: Direction?, player: EntityPlayer?)
        = super.getFrontFromPlace(side, player).inverse()

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>,
                                par4: Boolean) {
        // TODO...
    }
}

class EmergencyLampElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElement(sixNode, side, descriptor) {

    val load = NbtElectricalLoad("load")
    val chargingResistor = ResistorSwitch("chargingResistor", load, null)
    var on = false
        set(value) {
            if (field != value) needPublish()
            field = value
            sixNode.lightValue = if (value) 8 else 0
        }
    var charge = 0.0

    val process = IProcess { deltaT ->
        val descriptor = descriptor as EmergencyLampDescriptor

        // Charging or discharging?
        if (chargingResistor.u > 0.5 * descriptor.nominalVoltage) {
            on = false
            if (charge < descriptor.batteryCapacity) {
                chargingResistor.state = true
                charge = Math.min(charge + chargingResistor.p * deltaT, descriptor.batteryCapacity)
            } else {
                chargingResistor .state = false
            }
        } else {
            chargingResistor.state = false
            if (charge > 0) {
                on = true
                charge = Math.max(charge - 10 * deltaT, 0.0)
            } else {
                on = false
            }
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(on)
    }

    override fun initialize() {
        val descriptor = sixNodeElementDescriptor as EmergencyLampDescriptor

        electricalLoadList.add(load)
        electricalComponentList.add(chargingResistor)
        slowProcessList.add(process)

        chargingResistor.r = descriptor.nominalVoltage * descriptor.nominalVoltage / 10
        load.rs = 0.0
    }

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.left(), front.right() -> Node.maskElectricalPower
        else -> 0
    }
    override fun getElectricalLoad(lrdu: LRDU) = load
    override fun getThermalLoad(lrdu: LRDU) = null
    override fun multiMeterString() = ""
    override fun thermoMeterString() = ""
    override fun getWaila() = mapOf(
        "State" to if (on) "discharging" else "charging",
        "Charge" to Utils.plotPercent("", charge / (sixNodeElementDescriptor as EmergencyLampDescriptor).batteryCapacity)
    )
}

class EmergencyLampRender(entity: SixNodeEntity, side: Direction, val descriptor: SixNodeDescriptor)
    : SixNodeElementRender(entity, side, descriptor) {

    var on = false

    override fun draw() {
        front.glRotateOnX()
        (descriptor as EmergencyLampDescriptor).draw(side == Direction.YP, on)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        on = stream.readBoolean()
    }
}
