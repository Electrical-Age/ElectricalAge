package mods.eln.sixnode

import mods.eln.misc.*
import mods.eln.node.Node
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.ResistorSwitch
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class EmergencyLampDescriptor(name: String, val cable: ElectricalCableDescriptor, val batteryCapacity: Double,
                              val chargePower: Double, val consumption: Double, val lightLevel: Int, model: Obj3D)
    : SixNodeDescriptor(name, EmergencyLampElement::class.java, EmergencyLampRender::class.java) {

    val mainCeiling: Obj3D.Obj3DPart = model.getPart("coreCeil")
    val lightCeiling: Obj3D.Obj3DPart = model.getPart("lightCeil")
    val mainWall: Obj3D.Obj3DPart = model.getPart("coreWall")
    val lightWall: Obj3D.Obj3DPart = model.getPart("lightWall")

    init {
        voltageLevelColor = VoltageLevelColor.fromCable(cable)
        changeDefaultIcon("emergencylamp")
    }

    fun draw(onCeiling: Boolean = false, on: Boolean = false) {
        if (onCeiling) {
            if (on)
                preserveMatrix {
                    UtilsClient.drawLight(mainCeiling)
                    GL11.glColor3f(0.3f, 0.3f, 0.3f)
                    UtilsClient.drawLight(lightCeiling)
                }
            else
                mainCeiling.draw()
        } else {
            if (on)
                preserveMatrix {
                    UtilsClient.drawLight(mainWall)
                    GL11.glColor3f(0.3f, 0.3f, 0.3f)
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

    val desc = descriptor as EmergencyLampDescriptor
    val load = NbtElectricalLoad("load")
    val chargingResistor = ResistorSwitch("chargingResistor", load, null)
    var on = false
        set(value) {
            if (field != value) needPublish()
            field = value
            sixNode.lightValue = if (value) desc.lightLevel else 0
        }
    var charge = 0.0

    val process = IProcess { deltaT ->
        if (chargingResistor.u > 0.5 * desc.cable.electricalNominalVoltage) {
            on = false
            if (charge < desc.batteryCapacity) {
                chargingResistor.state = true
                charge = Math.min(charge + chargingResistor.p * deltaT, desc.batteryCapacity)
            } else {
                chargingResistor .state = false
            }
        } else {
            chargingResistor.state = false
            if (charge > 0) {
                on = true
                charge = Math.max(charge - desc.consumption * deltaT, 0.0)
            } else {
                on = false
            }
        }
    }

    override fun initialize() {
        chargingResistor.r =
            desc.cable.electricalNominalVoltage * desc.cable.electricalNominalVoltage / desc.chargePower
        desc.cable.applyTo(load)

        electricalLoadList.add(load)
        electricalComponentList.add(chargingResistor)
        slowProcessList.add(process)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(on)
    }

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.left(), front.right() -> Node.maskElectricalPower
        else -> 0
    }
    override fun getElectricalLoad(lrdu: LRDU) = load
    override fun getThermalLoad(lrdu: LRDU) = null
    override fun multiMeterString() = "TODO"
    override fun thermoMeterString() = ""
    override fun getWaila() = mapOf( // TODO: Improve.
        "State" to if (on) "discharging" else "charging",
        "Charge" to Utils.plotPercent("", charge / (sixNodeElementDescriptor as EmergencyLampDescriptor).batteryCapacity)
    )
}

class EmergencyLampRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElementRender(entity, side, descriptor) {

    val desc = descriptor as EmergencyLampDescriptor
    var on = false

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        desc.draw(side == Direction.YP, on)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        on = stream.readBoolean()
    }
}
