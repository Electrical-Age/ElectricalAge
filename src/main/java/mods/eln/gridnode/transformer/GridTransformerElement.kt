package mods.eln.gridnode.transformer

import mods.eln.Eln
import mods.eln.gridnode.GridElement
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.process.TransformerInterSystemProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import net.minecraft.util.Vec3
import java.io.DataOutputStream

class GridTransformerElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) : GridElement(node, descriptor, 8) {
    var primaryLoad = NbtElectricalLoad("primaryLoad")
    var secondaryLoad = NbtElectricalLoad("secondaryLoad")
    var primaryVoltageSource = VoltageSource("primaryVoltageSource", primaryLoad, null)
    var secondaryVoltageSource = VoltageSource("secondaryVoltageSource", secondaryLoad, null)
    var interSystemProcess = TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource)
    internal var desc: GridTransformerDescriptor = descriptor as GridTransformerDescriptor
    internal var maxCurrent = desc.cableDescriptor.electricalMaximalCurrent.toFloat()

    // Primary is the T2 coupling, secondary is the lower-voltage T1 coupling.
    internal val secondaryVoltage = desc.cableDescriptor.electricalNominalVoltage * 16
    internal val primaryVoltage = secondaryVoltage * 4

    internal val explosion = WorldExplosion(this).machineExplosion()

    internal var voltagePrimaryWatchdog = VoltageStateWatchDog().apply {
        setUNominal(primaryVoltage)
        set(primaryLoad)
        set(explosion)
        slowProcessList.add(this)
    }
    internal var voltageSecondaryWatchdog = VoltageStateWatchDog().apply {
        setUNominal(secondaryVoltage)
        set(secondaryLoad)
        set(explosion)
        slowProcessList.add(this)
    }

    internal val thermalLoad = NbtThermalLoad("thermal").apply {
        desc.cableDescriptor.applyTo(this)
        setAsSlow()
        slowProcessList.add(ElectricalLoadHeatThermalLoad(secondaryLoad, this))
        thermalLoadList.add(this)
    }
    internal val thermalWatchdog = ThermalLoadWatchDog().apply {
        setLimit(desc.cableDescriptor.thermalWarmLimit, desc.cableDescriptor.thermalCoolLimit)
        set(thermalLoad)
        set(explosion)
        slowProcessList.add(this)
    }

    init {
        electricalLoadList.add(primaryLoad)
        electricalLoadList.add(secondaryLoad)
        electricalComponentList.add(primaryVoltageSource)
        electricalComponentList.add(secondaryVoltageSource)

        desc.cableDescriptor.applyTo(primaryLoad, 4.0)
        desc.cableDescriptor.applyTo(secondaryLoad)

        interSystemProcess.setRatio(0.25)

        // Publish load from time to time.
        slowProcessList.add(NodePeriodicPublishProcess(node, 1.0, 0.5))
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.mna.removeProcess(interSystemProcess)
    }

    override fun connectJob() {
        Eln.simulator.mna.addProcess(interSystemProcess)
        super.connectJob()
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return when (side) {
            front.left() -> primaryLoad
            front.right() -> secondaryLoad
            else -> null
        }
    }

    // TODO: Factor this against super.
    public override fun getCablePoint(side: Direction, i: Int): Vec3 {
        if (i >= 2) throw AssertionError("Invalid cable point index")
        val idx = when (side) {
            front.left() -> 1
            front.right() -> 0
            else -> throw AssertionError("Invalid connection side")
        }
        val part = (if (i == 0) desc.plus else desc.gnd)[idx]
        return part.boundingBox().centre()
    }

    override fun getGridElectricalLoad(side: Direction): ElectricalLoad? {
        return getElectricalLoad(side, LRDU.Down)
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU) = thermalLoad

    override fun multiMeterString(side: Direction): String {
        if (side == front.up())
            return Utils.plotVolt("UP+:", primaryLoad.u) + Utils.plotAmpere("IP+:", primaryLoad.current)
        if (side == front.left())
            return Utils.plotVolt("US+:", secondaryLoad.u) + Utils.plotAmpere("IS+:", secondaryLoad.current)

        return (Utils.plotVolt("UP+:", primaryLoad.u) + Utils.plotAmpere("IP+:", primaryLoad.current)
            + Utils.plotVolt("  US+:", secondaryLoad.u) + Utils.plotAmpere("IS+:", secondaryLoad.current))
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat((secondaryLoad.current / maxCurrent).toFloat())
    }

    override fun getLightOpacity(): Float {
        return 1.0f
    }
}


