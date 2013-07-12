package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalConnection;

public class NodeConnection {
	Node N1,N2;
	Direction dir1,dir2;
	LRDU lrdu1,lrdu2;
	ElectricalConnection EC;
	ThermalConnection TC;
	public NodeConnection(Node N1,Direction dir1,LRDU lrdu1,Node N2,Direction dir2,LRDU lrdu2,ElectricalConnection EC, ThermalConnection TC) {
		this.N1 = N1;
		this.N2 = N2;
		this.dir1 = dir1;
		this.lrdu1 = lrdu1;
		this.dir2 = dir2;
		this.lrdu2 = lrdu2;
		this.EC = EC;
		this.TC = TC;
		// TODO Auto-generated constructor stub
	}

	public void destroy()
	{
		
		Eln.simulator.removeElectricalConnection(EC);
		Eln.simulator.removeThermalConnection(TC);
		
		N1.externalDisconnect(dir1, lrdu1);
		N2.externalDisconnect(dir2, lrdu2);
	}
}
