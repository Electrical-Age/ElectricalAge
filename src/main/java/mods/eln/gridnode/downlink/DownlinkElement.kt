package mods.eln.gridnode.downlink

import mods.eln.gridnode.GridElement
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor

/**
 * Created by svein on 25/08/15.
 */
class DownlinkElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) : GridElement(node, descriptor, 6) {
    internal var cableDescriptor: ElectricalCableDescriptor? = null

    internal var electricalLoad = NbtElectricalLoad("electricalLoad")
    internal var thermalLoad = NbtThermalLoad("thermalLoad")
    internal var heater = ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad)
    internal var thermalWatchdog = ThermalLoadWatchDog()
    internal var voltageWatchdog = VoltageStateWatchDog()

    init {
        val desc = descriptor as DownlinkDescriptor

        electricalLoad.setCanBeSimplifiedByLine(true)
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
        voltageWatchdog
                .set(electricalLoad)
                .setUMaxMin(desc.cableDescriptor.electricalNominalVoltage * 16)
                .set(WorldExplosion(this).cableExplosion())

    }

    override fun getGridElectricalLoad(side: Direction): ElectricalLoad {
        return electricalLoad
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad {
        return thermalLoad
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return NodeBase.MASK_ELECTRICAL_POWER
    }
}
