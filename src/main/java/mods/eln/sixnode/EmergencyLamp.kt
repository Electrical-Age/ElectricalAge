package mods.eln.sixnode

import mods.eln.gl.GL11

import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.Node
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.published
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.ResistorSwitch
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.Failure
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.lampsupply.LampSupplyElement
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream

class EmergencyLampDescriptor(name: String, val cable: ElectricalCableDescriptor, val batteryCapacity: Double,
                              val chargePower: Double, val consumption: Double, val lightLevel: Int, model: Obj3D)
    : SixNodeDescriptor(name, EmergencyLampElement::class.java, null) {

    val mainCeiling: Obj3D.Obj3DPart = model.getPart("coreCeil")
    val panelCeiling: Obj3D.Obj3DPart = model.getPart("panelCeil")
    val lightCeiling: Obj3D.Obj3DPart = model.getPart("lightCeil")
    val mainWall: Obj3D.Obj3DPart = model.getPart("coreWall")
    val mainWallR: Obj3D.Obj3DPart = model.getPart("coreWallR")
    val mainWallL: Obj3D.Obj3DPart = model.getPart("coreWallL")
    val lightWall: Obj3D.Obj3DPart = model.getPart("lightWall")

    init {
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable)
        setDefaultIcon("emergencylamp")
    }
    fun draw(onCeiling: Boolean = false, on: Boolean = false, mirrorSign: Boolean = false) {
        if (onCeiling) {
            mainCeiling.draw()

            if (on) {
                preserveMatrix {
                    UtilsClient.drawLight(panelCeiling)
                    GL11.glColor3f(0.3f, 0.3f, 0.3f)
                    UtilsClient.drawLight(lightCeiling)
                }
            } else {
                panelCeiling.draw()
            }
        } else {
            if (on) {
                preserveMatrix {
                    UtilsClient.drawLight(mainWall)
                    UtilsClient.drawLight(if (mirrorSign) mainWallL else mainWallR)
                    GL11.glColor3f(0.3f, 0.3f, 0.3f)
                    UtilsClient.drawLight(lightWall)
                }
            } else {
                preserveMatrix {
                    mainWall.draw()
                    (if (mirrorSign) mainWallL else mainWallR).draw()
                }
            }
        }
    }


    override fun getFrontFromPlace(side: Direction?, player: Player?)
        = super.getFrontFromPlace(side, player).inverse()

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>,
                                par4: Boolean) {
        with(list) {
            add("As long as power is provided, the internal battery")
            add("is charged and the lamp is off. On a power failure,")
            add("the lamp turns on and runs on batteries.")
            add(Utils.plotVolt("Nominal voltage:", cable.electricalNominalVoltage))
            add(Utils.plotEnergy("Battery capacity:", batteryCapacity))
        }
    }
}

class EmergencyLampElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElement(sixNode, side, descriptor) {

    enum class Event(val value: Byte) {
        TOGGLE_POWERED_BY_CABLE(1),
        SET_CHANNEL(2)
    }

    val desc = descriptor as EmergencyLampDescriptor
    val load = NbtElectricalLoad("load")
    val chargingResistor = ResistorSwitch("chargingResistor", load, null)
    var on by published(false, {
        sixNode.lightValue = if (it) desc.lightLevel else 0
    })
    var charge = desc.batteryCapacity / 2
    var poweredByCable by published(false, {
        if (it) isConnectedToLampSupply = false
    }, triggerReconnect = true)
    var channel by published("Default channel")
    var isConnectedToLampSupply by published(false)

    val process = IProcess { deltaT ->
        if (!poweredByCable) {
            var closestPowerSupply: LampSupplyElement.PowerSupplyChannelHandle? = null
            var closestDistance = 10000f

            LampSupplyElement.channelMap[channel]?.forEach {
                val distance = it.element.sixNode.coordonate.trueDistanceTo(sixNode.coordonate).toFloat()
                if (distance < closestDistance && distance <= it.element.range) {
                    closestDistance = distance
                    closestPowerSupply = it
                }
            }

            if (closestPowerSupply != null) {
                isConnectedToLampSupply = true
                if (closestPowerSupply!!.element.getChannelState(closestPowerSupply!!.id)) {
                    closestPowerSupply!!.element.addToRp(chargingResistor.r)
                    load.state = closestPowerSupply!!.element.powerLoad.state
                } else {
                    load.state = 0.0
                }
            } else {
                isConnectedToLampSupply = false
                load.state = 0.0
            }
        }

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
        slowProcessList.add(NodePeriodicPublishProcess(sixNode, 2.0, 0.5))
        slowProcessList.add(VoltageStateWatchDog().set(load).setUNominal(desc.cable.electricalNominalVoltage)
            .set(Failure(this).electronics()))
    }

    override fun getConnectionMask(lrdu: LRDU) = when {
        poweredByCable && side == Direction.YP -> Node.maskElectricalPower
        poweredByCable && (lrdu == front.left() || lrdu == front.right()) -> Node.maskElectricalPower
        else -> 0
    }

    override fun getElectricalLoad(lrdu: LRDU) = load
    override fun getThermalLoad(lrdu: LRDU) = null
    override fun multiMeterString() = buildString {
        append(Utils.plotVolt("U:", load.u))
        append(Utils.plotAmpere("I:", load.i))
        append(Utils.plotPercent("Charge:", charge / (sixNodeElementDescriptor as EmergencyLampDescriptor).batteryCapacity))
    }
    override fun thermoMeterString() = ""
    override fun getWaila() = mapOf(
        "State" to when {
            on -> "On"
            chargingResistor.state -> "Charging..."
            charge <= 0.0 -> "Batteries empty"
            else -> "Fully charged"
        },
        "Charge" to Utils.plotPercent("", charge / (sixNodeElementDescriptor as EmergencyLampDescriptor).batteryCapacity)
    )

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(charge.toFloat() / desc.batteryCapacity.toFloat())
        stream.writeBoolean(on)
        stream.writeBoolean(poweredByCable)
        stream.writeUTF(channel)
        stream.writeBoolean(isConnectedToLampSupply)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        when (stream.readByte()) {
            Event.TOGGLE_POWERED_BY_CABLE.value -> poweredByCable = !poweredByCable
            Event.SET_CHANNEL.value -> channel = stream.readUTF()
        }
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        on = nbt.getBooleanOr("on", false)
        charge = nbt.getDoubleOr("charge", 0.0)
        poweredByCable = nbt.getBooleanOr("poweredByCable", false)
        channel = nbt.getStringOr("channel", "")
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        nbt.putBoolean("on", on)
        nbt.putDouble("charge", charge)
        nbt.putBoolean("poweredByCable", poweredByCable)
        nbt.putString("channel", channel)
    }

    override fun hasGui() = true
}

class EmergencyLampRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElementRender(entity, side, descriptor) {

    val desc = descriptor as EmergencyLampDescriptor
    var charge = 0f
    var on = false
    var poweredByCable = false
    var channel = "Default channel"
    var isConnectedToLampSupply = false

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        desc.draw(side == Direction.YP, on, front == LRDU.Up)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        charge = stream.readFloat()
        on = stream.readBoolean()
        poweredByCable = stream.readBoolean()
        channel = stream.readUTF()
        isConnectedToLampSupply = stream.readBoolean()
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor? = if (poweredByCable) when {
        side == Direction.YP -> desc.cable.render
        lrdu == front.left() || lrdu == front.right() -> desc.cable.render
        else -> null
    } else null
}


// 26.x port: class EmergencyLampGui removed (client; redone in P3/P4)

