package mods.eln.sim;

public class ElectricalResistorWithCounter extends ElectricalResistor{

	public double energyCounter = 0;
	
	public ElectricalResistorWithCounter(ElectricalLoad a,ElectricalLoad b) {
		super(a,b);
	}
	
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		I = (a.Uc-b.Uc)*Rinv;
		
		ElectricalLoad.moveCurrent(I, a, b);	

		energyCounter += (a.Uc-b.Uc)*I*time;
	}

}
