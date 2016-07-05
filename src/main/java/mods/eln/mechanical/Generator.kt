package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.DataInputStream
import java.io.DataOutputStream


class GeneratorDescriptor(
        name: String,
        obj: Obj3D,
        cable: ElectricalCableDescriptor,
        nominalRads: Float,
        nominalU: Float,
        powerOutPerDeltaU: Float,
        nominalP: Float,
        thermalLoadInitializer: ThermalLoadInitializer):
        SimpleShaftDescriptor(name, GeneratorElement::class, GeneratorRender::class, EntityMetaTag.Basic) {

    val RtoU = LinearFunction(0f, 0f, nominalRads, nominalU)
    val cable = cable
    val thermalLoadInitializer = thermalLoadInitializer
    val powerOutPerDeltaU = powerOutPerDeltaU
    val nominalRads = nominalRads
    val nominalP = nominalP
    val nominalU = nominalU
    val generationEfficiency = 0.95

    init {
        thermalLoadInitializer.setMaximalPower(nominalP.toDouble() * (1 - generationEfficiency))
    }

    override val obj = obj
    override val static = arrayOf(
            obj.getPart("Cowl"),
            obj.getPart("Stand")
    ).requireNoNulls()
    override val rotating = arrayOf(obj.getPart("Shaft")).requireNoNulls()
    val powerLights = arrayOf(
            obj.getPart("LED_0"),
            obj.getPart("LED_1"),
            obj.getPart("LED_2"),
            obj.getPart("LED_3"),
            obj.getPart("LED_4"),
            obj.getPart("LED_5"),
            obj.getPart("LED_6")
    ).requireNoNulls()

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add("Converts mechanical energy into electricity, or (badly) vice versa.")
        list.add("Nominal usage ->")
        list.add(Utils.plotVolt("  Voltage out: ", nominalU.toDouble()))
        list.add(Utils.plotPower("  Power out: ", nominalP.toDouble()))
        list.add(Utils.plotRads("  Rads: ", nominalRads.toDouble()))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

class GeneratorRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor): ShaftRender(entity, desc_) {
    val entity = entity

    override val cableRender = Eln.instance.stdCableRender3200V
    val desc = desc_ as GeneratorDescriptor

    val ledColors: Array<Color> = arrayOf(
            java.awt.Color.black,
            java.awt.Color.black,
            java.awt.Color.black,
            java.awt.Color.black,
            java.awt.Color.black,
            java.awt.Color.black,
            java.awt.Color.black
    )
    val ledColorBase: Array<HSLColor> = arrayOf(
            GREEN,
            GREEN,
            GREEN,
            GREEN,
            YELLOW,
            RED,
            RED
    )

    fun calcPower(power: Double) {
        if (power < 0) {
            for (i in 1..6) {
                ledColors[i] = Color.black
            }
            ledColors[0] = RED.adjustLuminanceClamped((-power / desc.nominalP * 4 * 100).toFloat(), 0f, 60f)
        } else {
            val slice = desc.nominalP / 5
            var remainder = power
            for (i in 0..6) {
                ledColors[i] = ledColorBase[i].adjustLuminanceClamped((remainder / slice * 100).toFloat(), 0f, 65f)
                remainder -= slice
            }
        }
    }

    override fun draw() {
        draw {
            ledColors.forEachIndexed { i, color ->
                GL11.glColor3f(
                        color.red / 255f,
                        color.green / 255f,
                        color.blue / 255f
                )
                desc.powerLights[i].draw()
            }
        }
    }

	override fun getCableRender(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        if (lrdu == LRDU.Down && side == front) return Eln.instance.stdCableRender3200V
		return null
	}


    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        calcPower(stream.readDouble())
    }

}

class GeneratorElement(node: TransparentNode, desc_: TransparentNodeDescriptor):
        SimpleShaftElement(node, desc_) {
    val desc = desc_ as GeneratorDescriptor

    internal val inputLoad = NbtElectricalLoad("inputLoad")
    internal val positiveLoad = NbtElectricalLoad("positiveLoad")
    internal val inputToPositiveResistor = Resistor(inputLoad, positiveLoad)
    internal val electricalPowerSource = VoltageSource("PowerSource", positiveLoad, null)
    internal val electricalProcess = GeneratorElectricalProcess()
    internal val shaftProcess = GeneratorShaftProcess()

    internal val thermal = NbtThermalLoad("thermal")
    internal val heater: ElectricalLoadHeatThermalLoad
    internal val thermalLoadWatchDog = ThermalLoadWatchDog()

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(inputLoad)
        electricalComponentList.add(electricalPowerSource)
        electricalComponentList.add(inputToPositiveResistor)

        electricalProcessList.add(shaftProcess)
        desc.cable.applyTo(inputLoad)
        desc.cable.applyTo(inputToPositiveResistor)
        desc.cable.applyTo(positiveLoad)

        desc.thermalLoadInitializer.applyTo(thermal)
        desc.thermalLoadInitializer.applyTo(thermalLoadWatchDog)
        thermal.setAsSlow()
        thermalLoadList.add(thermal)
        thermalLoadWatchDog.set(thermal).set(WorldExplosion(this).machineExplosion())

        heater = ElectricalLoadHeatThermalLoad(inputLoad, thermal)
        thermalFastProcessList.add(heater)

        // TODO: Add whine. Sound's good.
        // TODO: Add running lights. (More. Electrical sparks, perhaps?)
        // TODO: Add the thermal explosions—there should be some.
    }

    inner class GeneratorElectricalProcess: IProcess, IRootSystemPreStepProcess {
        override fun process(time: Double) {
            val targetU = desc.RtoU.getValue(shaft.rads)

            // Most things below were copied from TurbineElectricalProcess.
            // Some comments on what math is going on would be great.
            val th = positiveLoad.getSubSystem().getTh(positiveLoad, electricalPowerSource)
            var Ut: Double
            if (targetU < th.U) {
                Ut = th.U * 0.999 + targetU * 0.001
            } else if (th.isHighImpedance()) {
                Ut = targetU
            } else {
                val a = 1 / th.R
                val b = desc.powerOutPerDeltaU - th.U / th.R
                val c = -desc.powerOutPerDeltaU * targetU
                Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a)
            }
            electricalPowerSource.setU(Ut)
        }

        override fun rootSystemPreStepProcess() {
            process(0.0)
        }
    }

    inner class GeneratorShaftProcess: IProcess {
        override fun process(time: Double) {
            var E = electricalPowerSource.getP() * time
            if (E < 0)
                E *= 0.75  // Not a very efficient motor.
            maybePublishE(E / time)
            // The Math.max makes the shaft harder to spin up without an auxilliary power source.
            E += defaultDrag * Math.max(shaft.rads, 10.0)
            shaft.energy -= (E * desc.generationEfficiency)
            thermal.movePowerTo(E * (1 - desc.generationEfficiency))
        }
    }

    var lastE = 0.0
    fun maybePublishE(E: Double) {
        if (Math.abs(E - lastE) / desc.nominalP > 0.01) {
            lastE = E
            needPublish()
        }
    }

    override fun connectJob() {
        super.connectJob()
        Eln.simulator.mna.addProcess(electricalProcess)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.mna.removeProcess(electricalProcess)
    }


    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null;
        return when (side) {
            front -> inputLoad
            front.back() -> inputLoad
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = thermal

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        if (lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.maskElectricalPower
        return 0
    }

    override fun multiMeterString(side: Direction?) =
        Utils.plotER(shaft.energy, shaft.rads) + Utils.plotUIP(electricalPowerSource.getU(), electricalPowerSource.getI())

    override fun thermoMeterString(side: Direction?) = Utils.plotCelsius("T", thermal.getT())

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }


    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(lastE)
    }
}