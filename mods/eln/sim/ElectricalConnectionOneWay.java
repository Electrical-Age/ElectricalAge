package mods.eln.sim;

public class ElectricalConnectionOneWay implements IProcess{

	public ElectricalLoad to;
	public ElectricalLoad from;

	public ElectricalConnectionOneWay(ElectricalLoad from,ElectricalLoad to){
		this.from = from;
		this.to = to;
	}
	
	@Override
	public void process(double time) {
		double U = from.Uc - to.Uc;
		if(U > 0){
			ElectricalLoad.moveCurrent(U/(from.getRs()+to.getRs()), from, to);
		}
	}
 
}
