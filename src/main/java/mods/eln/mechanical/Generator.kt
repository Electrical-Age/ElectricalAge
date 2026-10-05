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
    thermalLoadInitializer: ThermalLoadInitializer) :
    SimpleShaftDescriptor(name, GeneratorElement::class, GeneratorRender::class, EntityMetaTag.Basic) {

    val RtoU = LinearFunction(0f, 0f, nominalRads, nominalU)
    val cable = cable
    val thermalLoadInitializer = thermalLoadInitializer
    val powerOutPerDeltaU = powerOutPerDeltaU
    val nominalRads = nominalRads
    val nominalP = nominalP
    val nominalU = nominalU
    val generationEfficiency = 0.95
    /** Steady temperature at nominal output, as a fraction of the thermal warm limit. */
    val nominalHeatFraction = 0.65
    override val sound = "eln:generator"

    init {
        // WP16b: sized from the heat the generator really makes at nominal output (loss P(1/eff - 1) plus the
        // drag share), so that it sits at nominalHeatFraction of its warm limit there and overheats only on
        // overload (~1.6 x nominal). 1.7.10 sized it at P(1 - eff) but heated it with 1/20 of its loss.
        thermalLoadInitializer.setMaximalPower(nominalHeatPower() / nominalHeatFraction)

        voltageLevelColor = VoltageLevelColor.VeryHighVoltage
    }

    /** Heat (W) at nominal power and speed: electrical loss + drag heat (+ ~0.3 W cable I^2 R, ignored). */
    fun nominalHeatPower() = nominalP.toDouble() * (1 / generationEfficiency - 1) +
        defaultDrag * 20 * nominalRads * (1 - generationEfficiency)

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

    override fun addInformation(stack: ItemStack, player: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        list.add("Converts mechanical energy into electricity, or (badly) vice versa.")
        list.add("Nominal usage ->")
        list.add(Utils.plotVolt("  Voltage out: ", nominalU.toDouble()))
        list.add(Utils.plotPower("  Power out: ", nominalP.toDouble()))
        list.add(Utils.plotRads("  Rads: ", nominalRads.toDouble()))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

class GeneratorRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor) : ShaftRender(entity, desc_) {
    val entity = entity

    override val cableRender = Eln.stdCableRender3200V
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
        if (lrdu == LRDU.Down && side == front) return Eln.stdCableRender3200V
        return null
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        val power = stream.readDouble()
        calcPower(power)
        volumeSetting.target = 0.05f + Math.abs(power / desc.nominalP).toFloat() / 4f
    }
}

class GeneratorElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
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
        // WP16b: slow list (like poles/downlinks): the heater moves a power for one step of the load, and the
        // thermal load is slow. 1.7.10 had it on the fast list, i.e. 20 x the cable's I^2 R per tick.
        slowProcessList.add(heater)

        // TODO: Add running lights. (More. Electrical sparks, perhaps?)
        // TODO: Add the thermal explosions—there should be some.
    }

    inner class GeneratorElectricalProcess : IProcess, IRootSystemPreStepProcess {
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

    inner class GeneratorShaftProcess() : IProcess {
        private var powerFraction = 0.0f

        override fun process(time: Double) {
            val p = electricalPowerSource.p
            powerFraction = (p / desc.nominalP).toFloat()
            val eff = desc.generationEfficiency
            val electricalE = p * time // energy delivered to the circuit this step; < 0 when motoring
            var E = electricalE
            if (E < 0)
                E *= 0.75  // Not a very efficient motor.
            maybePublishE(E / time)
            // WP16: generating, the shaft has to supply E / eff (1.7.10 took E * eff: 5 % of the output was free
            // energy). Motoring keeps 1.7.10's shaft gain (0.75 * 0.95 of the absorbed energy); the rest is heat
            // (1.7.10 moved a negative heat, i.e. motoring cooled the generator).
            val shaftE = if (E >= 0) E / eff else E * eff
            val lossE = if (E >= 0) shaftE - E else shaftE - electricalE
            // The Math.max makes the shaft harder to spin up without an auxilliary power source.
            // defaultDrag is J per tick (1/20 s) per rad/s; WP16b: scaled by the step length (1.7.10 applied it
            // per electrical step, which is a tick only at the default electricalFrequency of 20 Hz).
            val drag = defaultDrag * Math.max(shaft.rads, 10.0) * time * 20
            shaft.energy -= shaftE + drag * eff
            // WP16b: energy (J) of this step. 1.7.10 passed these joules to movePowerTo (W), i.e. heated with
            // 1/20 of the loss; moveEnergyTo is right for a process at any rate (this one is electrical).
            thermal.moveEnergyTo(lossE + drag * (1 - eff))
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

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        info.put("Speed", Utils.plotRads("", shaft.rads))
        if (Eln.wailaEasyMode) {
            info.put("Voltage", Utils.plotVolt("", electricalPowerSource.getU()))
            info.put("Current", Utils.plotAmpere("", electricalPowerSource.getI()))
            info.put("Temperature", Utils.plotCelsius("", thermal.t))
        }
        return info
    }
}
