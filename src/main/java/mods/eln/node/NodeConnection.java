package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ThermalConnection;

import java.util.ArrayList;
import java.util.List;

public class NodeConnection {
    public NodeBase N1, N2;
    public Direction dir1, dir2;
    public LRDU lrdu1, lrdu2;
    public List<ElectricalConnection> EC;
    public List<ThermalConnection> TC;

    public NodeConnection(NodeBase N1, Direction dir1, LRDU lrdu1, NodeBase N2, Direction dir2, LRDU lrdu2) {
        this.N1 = N1;
        this.N2 = N2;
        this.dir1 = dir1;
        this.lrdu1 = lrdu1;
        this.dir2 = dir2;
        this.lrdu2 = lrdu2;
        this.EC = new ArrayList<>();
        this.TC = new ArrayList<>();
    }

    public void destroy() {
        for(ElectricalConnection ec : EC) Eln.simulator.removeElectricalComponent(ec);
        for(ThermalConnection tc : TC) Eln.simulator.removeThermalConnection(tc);

        if (N1 != null) N1.externalDisconnect(dir1, lrdu1);
        if (N2 != null) N2.externalDisconnect(dir2, lrdu2);
    }

    public void addConnection(ElectricalConnection ec) { EC.add(ec); }
    public void addConnection(ThermalConnection tc) { TC.add(tc); }
}
