package mods.eln.sim;



public class ElectricalSourceWithCurrentLimitationProcess extends ElectricalSourceProcess {


	public double U = 0,Imax = 0;
	public double Iout = 0;
	
	public ElectricalSourceWithCurrentLimitationProcess(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad,double U,double Imax)
	{
		super(positiveLoad, negativeLoad, U);

		this.Imax = Imax;
	}

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		double Cequ = (positiveLoad.getC()*negativeLoad.getC())/(positiveLoad.getC()+negativeLoad.getC());
		double QNeeded = (U - (positiveLoad.Uc-negativeLoad.Uc))*Cequ;
		double I = QNeeded/time;
		if(I > Imax) I = Imax;
		if(I < -Imax) I = -Imax;
		ElectricalLoad.moveCurrent(I, negativeLoad, positiveLoad);	

		
		Iout = I;
	}
	

}
