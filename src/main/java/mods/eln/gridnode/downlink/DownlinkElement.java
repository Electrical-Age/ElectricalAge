package mods.eln.gridnode.downlink;

import mods.eln.gridnode.GridElement;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * Created by svein on 25/08/15.
 */
public class DownlinkElement extends GridElement {
    DownlinkDescriptor desc;
    ElectricalCableDescriptor cableDescriptor;

    NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    ElectricalLoadHeatThermalLoad heater = new ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad);
    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    public DownlinkElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor, 6);
        desc = (DownlinkDescriptor) descriptor;

        electricalLoad.setCanBeSimplifiedByLine(true);
        desc.cableDescriptor.applyTo(electricalLoad);
        desc.cableDescriptor.applyTo(thermalLoad);
        electricalLoadList.add(electricalLoad);

        thermalLoadList.add(thermalLoad);
        slowProcessList.add(heater);
        thermalLoad.setAsSlow();
        slowProcessList.add(thermalWatchdog);
        thermalWatchdog
                .set(thermalLoad)
                .setLimit(desc.cableDescriptor.thermalWarmLimit, desc.cableDescriptor.thermalCoolLimit)
                .set(new WorldExplosion(this).cableExplosion());

        slowProcessList.add(voltageWatchdog);
        voltageWatchdog
                .set(electricalLoad)
                .setUMaxMin(desc.cableDescriptor.electricalNominalVoltage * 16)
                .set(new WorldExplosion(this).cableExplosion());

    }

    @Override
    protected ElectricalLoad getGridElectricalLoad(Direction side) {
        return electricalLoad;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return electricalLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return NodeBase.maskElectricalPower;
    }
}
