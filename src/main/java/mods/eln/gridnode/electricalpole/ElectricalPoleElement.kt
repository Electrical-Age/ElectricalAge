package mods.eln.gridnode.electricalpole

import mods.eln.Eln
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.GridElement
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.process.TransformerInterSystemProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad

import java.io.DataOutputStream
import java.io.IOException

data class Transformer(
    val secondaryLoad: NbtElectricalLoad,
    val primaryVoltageSource: VoltageSource,
    val secondaryVoltageSource: VoltageSource,
    val interSystemProcess: TransformerInterSystemProcess,
    val voltageSecondaryWatchdog: VoltageStateWatchDog
)

class ElectricalPoleElement(node: TransparentNode, descriptor: TransparentNodeDescriptor)
    : GridElement(node, descriptor, (descriptor as GridDescriptor).connectRange) {
    private val desc = descriptor as ElectricalPoleDescriptor

    var electricalLoad = NbtElectricalLoad("electricalLoad")
    var thermalLoad = NbtThermalLoad("thermalLoad")
    internal var heater = ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad)
    internal var thermalWatchdog = ThermalLoadWatchDog()
    internal var voltageWatchdog = VoltageStateWatchDog()
    internal var secondaryMaxCurrent = 0f

    val trafo: Transformer?

    init {
        electricalLoad.setCanBeSimplifiedByLine(true)
        // Most of the resistance is in the cable, which is handled in GridLink.
        // We put some of it here, thereby allowing the thermal watchdog to work.
        desc.cableDescriptor.applyTo(electricalLoad)
        desc.cableDescriptor.applyTo(thermalLoad)
        electricalLoadList.add(electricalLoad)

        thermalLoadList.add(thermalLoad)
        slowProcessList.add(heater)
        thermalLoad.setAsSlow()
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog
                .set(thermalLoad)
                .setLimit(desc.cableDescriptor.thermalWarmLimit, desc.cableDescriptor.thermalCoolLimit)
                .set(WorldExplosion(this).cableExplosion())

        slowProcessList.add(voltageWatchdog)
        // Electrical poles can handle higher voltages, due to air insulation.
        // This puts utility poles at 4 * Very High Voltage.
        // There'll be a second tier of pole later, handling 16x.
        val exp: WorldExplosion
        if (desc.includeTransformer) {
            exp = WorldExplosion(this).machineExplosion()
        } else {
            exp = WorldExplosion(this).cableExplosion()
        }
        voltageWatchdog
                .set(electricalLoad)
                .setUMaxMin(desc.voltageLimit)
                .set(exp)

        if (desc.includeTransformer) {
            val secondaryLoad = NbtElectricalLoad("secondaryLoad")
            val primaryVoltageSource = VoltageSource("primaryVoltageSource", electricalLoad, null)
            val secondaryVoltageSource = VoltageSource("secondaryVoltageSource", secondaryLoad, null)
            val interSystemProcess = TransformerInterSystemProcess(electricalLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource)
            val voltageSecondaryWatchdog = VoltageStateWatchDog()

            trafo = Transformer(
                secondaryLoad,
                primaryVoltageSource,
                secondaryVoltageSource,
                interSystemProcess,
                voltageSecondaryWatchdog
            )

            desc.cableDescriptor.applyTo(secondaryLoad, 4.0)

            electricalLoadList.add(secondaryLoad)
            electricalComponentList.add(primaryVoltageSource)
            electricalComponentList.add(secondaryVoltageSource)
            slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(exp))

            // Publish load from time to time.
            slowProcessList.add(NodePeriodicPublishProcess(node, 1.0, 0.5))
        } else {
            trafo = null
        }
    }

    override fun disconnectJob() {
        super.disconnectJob()
        trafo?.apply {
            Eln.simulator.mna.removeProcess(interSystemProcess)
        }
    }

    override fun connectJob() {
        trafo?.apply {
            Eln.simulator.mna.addProcess(interSystemProcess)
        }
        super.connectJob()
    }

    override fun multiMeterString(side: Direction): String {
        if (trafo != null) {
            return (Utils.plotVolt("GridU:", electricalLoad.u) + Utils.plotAmpere("GridP:", electricalLoad.current)
                + Utils.plotVolt("  GroundU:", trafo.secondaryLoad.u) + Utils.plotAmpere("GroundP:", trafo.secondaryLoad.current))
        } else {
            return super.multiMeterString(side)
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return trafo?.secondaryLoad
    }

    override fun getGridElectricalLoad(side: Direction): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad {
        return thermalLoad
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (desc.includeTransformer) {
            return NodeBase.maskElectricalPower
        } else {
            return 0
        }
    }

    override fun initialize() {
        trafo?.apply {
            voltageSecondaryWatchdog.setUNominal(Eln.instance.veryHighVoltageCableDescriptor.electricalNominalVoltage)
            secondaryMaxCurrent = desc.cableDescriptor.electricalMaximalCurrent.toFloat()
            interSystemProcess.setRatio(0.25)
        }
        super.initialize()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        node.lrduCubeMask.getTranslate(front.down()).serialize(stream)
        try {
            if (trafo != null && secondaryMaxCurrent != 0f) {
                stream.writeFloat((trafo.secondaryLoad.i / secondaryMaxCurrent).toFloat())
            } else {
                stream.writeFloat(0f)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
