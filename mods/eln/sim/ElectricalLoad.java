package mods.eln.sim;

import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.node.NodeBlockEntity;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Line;
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

		if(this.Rs != Rs){
			this.Rs = Rs;
			for(Component c : getConnectedComponents()){
				if(c instanceof ElectricalConnection){
					((ElectricalConnection)c).notifyRsChange();
				}else 
				if(c instanceof Line){
					((Line)c).recalculateR();
				}
			}
		}
		
	}
	
	public double getRs(){
		return Rs;
	}
	
	
	public void highImpedance(){
		setRs(1000000000.0);
	}
//	ArrayList<ElectricalConnection> electricalConnections = new ArrayList<ElectricalConnection>(4);
	

}