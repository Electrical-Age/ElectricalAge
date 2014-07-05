package mods.eln.sim;

import mods.eln.sim.mna.component.Bipole;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageStateLineReady;


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
	
	public double getI() {
		double i = 0;
		for(Component c : getConnectedComponents()){
			if(c instanceof Bipole)
				i += Math.abs(((Bipole)c).getCurrent());
		}
		return i*0.5;
	}
    
	public double getCurrent() {
		return getI();
	}


	
}