package mods.eln.sim.nbt;

import mods.eln.node.NodeBase;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.BatterySlowProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sim.process.destruct.IDestructable;

public class NbtBatterySlowProcess extends BatterySlowProcess {

    NodeBase node;
    /** Set by the owner; overcharge or reverse charge is handed to it (see Failure). */
    public IDestructable failure;

    public NbtBatterySlowProcess(NodeBase node, BatteryProcess batteryProcess, ThermalLoad thermalLoad) {
        super(batteryProcess, thermalLoad);
        this.node = node;
    }

    @Override
    public void destroy() {
        if (failure != null) {
            failure.fail(FailureCause.OVERVOLTAGE, batteryProcess.getU());
        } else {
            node.physicalSelfDestruction(2);
        }
    }
}
