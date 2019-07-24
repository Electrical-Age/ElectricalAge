package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ThermalConnection;

public class NodeConnection {
    NodeBase N1, N2;
    Direction dir1, dir2;
    LRDU lrdu1, lrdu2;
    ElectricalConnection EC;
    ThermalConnection TC;

    public NodeConnection(NodeBase N1, Direction dir1, LRDU lrdu1, NodeBase N2, Direction dir2, LRDU lrdu2, ElectricalConnection EC, ThermalConnection TC) {
        this.N1 = N1;
        this.N2 = N2;
        this.dir1 = dir1;
        this.lrdu1 = lrdu1;
        this.dir2 = dir2;
        this.lrdu2 = lrdu2;
        this.EC = EC;
        this.TC = TC;
    }

    public void destroy() {
        Eln.simulator.removeElectricalComponent(EC);
        Eln.simulator.removeThermalConnection(TC);

        if (N1 != null) N1.externalDisconnect(dir1, lrdu1);
        if (N2 != null) N2.externalDisconnect(dir2, lrdu2);
    }
}
