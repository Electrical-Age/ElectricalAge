package mods.eln.sim;

import mods.eln.misc.FunctionTable;



public class ElectricalSourceProcess implements IProcess {
	ElectricalLoad positiveLoad, negativeLoad;

	public double U = 0;
	public double Iout = 0;
	
	public ElectricalSourceProcess(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad,double U )
	{
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
		this.U = U;
	}

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		double Cequ = (positiveLoad.getC()*negativeLoad.getC())/(positiveLoad.getC()+negativeLoad.getC());
		double QNeeded = (U - (positiveLoad.Uc-negativeLoad.Uc))*Cequ;

		ElectricalLoad.moveCurrent(QNeeded/time, negativeLoad, positiveLoad);	

		
		Iout = QNeeded/time;
	}
	

}
