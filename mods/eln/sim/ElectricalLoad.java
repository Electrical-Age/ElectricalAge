package mods.eln.sim;

import java.util.ArrayList;

import mods.eln.node.NodeBlockEntity;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.VoltageState;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalLoad extends VoltageState{
	

	
	//public VoltageState state = new VoltageState();
	
	
	private double Rs = Double.MAX_VALUE;
	
	public void setRs(double Rs){
		this.Rs = Rs;
		for(Component c : getConnectedComponents()){
			if(c instanceof ElectricalConnection){
				((ElectricalConnection)c).notifyRsChange();
			}
		}
	}
	
	public double getRs(){
		return Rs;
	}
	
//	ArrayList<ElectricalConnection> electricalConnections = new ArrayList<ElectricalConnection>(4);
	

}