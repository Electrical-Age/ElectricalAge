package mods.eln.sim;

public class ElectricalCurrentSource implements IProcess{

	public ElectricalCurrentSource(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad ) {
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
	}

	ElectricalLoad positiveLoad, negativeLoad;
	double I = 0;
	
	public void setI(double I)
	{
		this.I = I;
	}

	@Override
	public void process(double time) {
		ElectricalLoad.moveCurrent(I, negativeLoad,positiveLoad);
	}

	public double getI() {
		// TODO Auto-generated method stub
		return I;
	}

}
