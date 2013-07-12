package mods.eln.sim;

import mods.eln.misc.FunctionTable;



public class ElectricalSourceECProcess implements IProcess {
	ElectricalLoad positiveLoad, negativeLoad;

	public double U = 0;
	public double Iout = 0;
	public double energyCounter = 0;
	public boolean generatorOnly = false;
	
	public ElectricalSourceECProcess(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad)
	{
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
	}

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		double Ureal = positiveLoad.Uc-negativeLoad.Uc; 
		if(generatorOnly && Ureal > U) return;
		double Cequ = (positiveLoad.getC()*negativeLoad.getC())/(positiveLoad.getC()+negativeLoad.getC());
		double QNeeded = (U - (Ureal))*Cequ;

	
		ElectricalLoad.moveCurrent(QNeeded/time, negativeLoad, positiveLoad);	

		Iout = QNeeded/time;
		//energyCounter += U*Iout*time;
		//energyCounter += (positiveLoad.Uc-negativeLoad.Uc - U)*(positiveLoad.Uc-negativeLoad.Uc - U) * Cequ / 2.0 * Math.signum(Iout);
		energyCounter +=  (Ureal) * Iout * time;
	}
	
	public void setAsGenerator()
	{
		generatorOnly = true;
	}

}
