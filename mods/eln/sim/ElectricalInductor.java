package mods.eln.sim;

public class ElectricalInductor implements IProcess{
	ElectricalLoad positiveLoad,negativeLoad;
	double henri = 1;
	double current = 0;
	
	
	public ElectricalInductor(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad) {
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
	}
	
	public void setHenri(double henri){
		this.henri = henri;
	}
	
	public double getHenri() {
		return henri;
	}
	
	public void setCurrent(double current) {
		this.current = current;
	}
	public double getCurrent() {
		return current;
	}
	@Override
	public void process(double time) {
		double U = positiveLoad.Uc-negativeLoad.Uc;
		current += U/henri;
		ElectricalLoad.moveCurrent(current, positiveLoad, negativeLoad);
	}

}
