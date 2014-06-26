package mods.eln.sim;

import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.node.NodeBlockEntity;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sim.mna.state.VoltageStateLineReady;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalLoad extends VoltageStateLineReady{
	
	public static State groundLoad = null;
	
	public ElectricalLoad() {
		
	}
	
	//public VoltageState state = new VoltageState();
	
	
	private double Rs = 1000000000.0;
	
	public void setRs(double Rs){
		/*boolean reAdd = false;
		if(line != null || getSubSystem() != null){
			reAdd = true;
			Eln.simulator.removeElectricalLoad(this);
		}
		*/
		this.Rs = Rs;
		for(Component c : getConnectedComponents()){
			if(c instanceof ElectricalConnection){
				((ElectricalConnection)c).notifyRsChange();
			}
		}
		
		/*
		if(reAdd){
			Eln.simulator.addElectricalLoad(this);
		}*/
		
		
	}
	
	public double getRs(){
		return Rs;
	}
	
//	ArrayList<ElectricalConnection> electricalConnections = new ArrayList<ElectricalConnection>(4);
	

}