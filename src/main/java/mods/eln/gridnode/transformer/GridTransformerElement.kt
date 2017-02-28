package mods.eln.gridnode.transformer

import mods.eln.Eln
import mods.eln.gridnode.GridElement
import mods.eln.misc.*
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.process.TransformerInterSystemProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.util.Vec3

class GridTransformerElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) : GridElement(node, descriptor, 8) {
    var primaryLoad = NbtElectricalLoad("primaryLoad")
    var secondaryLoad = NbtElectricalLoad("secondaryLoad")
    var primaryVoltageSource = VoltageSource("primaryVoltageSource", primaryLoad, null)
    var secondaryVoltageSource = VoltageSource("secondaryVoltageSource", secondaryLoad, null)
    var interSystemProcess = TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource)
    internal var desc: GridTransformerDescriptor = descriptor as GridTransformerDescriptor
    internal var primaryMaxCurrent = desc.cableDescriptor.electricalMaximalCurrent.toFloat()
    internal var secondaryMaxCurrent = desc.cableDescriptor.electricalMaximalCurrent.toFloat()
    //SoundLooper highLoadSoundLooper;

    // Primary is the T2 coupling, secondary is the lower-voltage T1 coupling.
    internal val secondaryVoltage = desc.cableDescriptor.electricalNominalVoltage * 16
    internal val primaryVoltage = secondaryVoltage * 4

    internal var voltagePrimaryWatchdog = VoltageStateWatchDog().apply { setUNominal(primaryVoltage) }
    internal var voltageSecondaryWatchdog = VoltageStateWatchDog().apply { setUNominal(secondaryVoltage) }

    init {

        electricalLoadList.add(primaryLoad)
        electricalLoadList.add(secondaryLoad)
        electricalComponentList.add(primaryVoltageSource)
        electricalComponentList.add(secondaryVoltageSource)
        val exp = WorldExplosion(this).machineExplosion()
        slowProcessList.add(voltagePrimaryWatchdog.set(primaryLoad).set(exp))
        slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(exp))

        desc.cableDescriptor.applyTo(primaryLoad, 4.0)
        desc.cableDescriptor.applyTo(secondaryLoad)

        interSystemProcess.setRatio(0.25)

        /* TODO: Do looping on client.
        highLoadSoundLooper = new SoundLooper(this) {
            @Override
            public SoundCommand mustStart() {
                if (primaryMaxCurrent != 0 && secondaryMaxCurrent != 0) {
                    float load = (float) Math.max(primaryLoad.getI() / primaryMaxCurrent, secondaryLoad.getI() / secondaryMaxCurrent);
                    if (load > desc.minimalLoadToHum)
                        return desc.highLoadSound.copy().mulVolume(0.2f * (load - desc.minimalLoadToHum) / (1 - desc.minimalLoadToHum), 1f).smallRange();
                }
                return null;
            }
        };
        slowProcessList.add(highLoadSoundLooper);*/
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

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun multiMeterString(side: Direction): String {
        if (side == front.up())
            return Utils.plotVolt("UP+:", primaryLoad.u) + Utils.plotAmpere("IP+:", primaryLoad.current)
        if (side == front.left())
            return Utils.plotVolt("US+:", secondaryLoad.u) + Utils.plotAmpere("IS+:", secondaryLoad.current)

        return (Utils.plotVolt("UP+:", primaryLoad.u) + Utils.plotAmpere("IP+:", primaryLoad.current)
            + Utils.plotVolt("  US+:", secondaryLoad.u) + Utils.plotAmpere("IS+:", secondaryLoad.current))
    }

    override fun thermoMeterString(side: Direction): String? {
        return null
    }

    override fun initialize() {
        super.initialize()
    }

    override fun getLightOpacity(): Float {
        return 1.0f
    }
}


