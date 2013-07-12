package mods.eln.sim;

public class ElectricalPowerSource implements IProcess{

	public ElectricalPowerSource(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad ) {
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
	}

	ElectricalLoad positiveLoad, negativeLoad;
	double P = 0;
	double Imax = 10000.0;
	double Umax = 0;
	double energyCounter = 0;
	
	public double getEnergyCounter()
	{
		return energyCounter;
	}
	
	public void clearEnergyCounter()
	{
		energyCounter = 0;
	}
	public double getImax() {
		return Imax;
	}
	public void setImax(double imax) {
		Imax = imax;
	}
	
	public double getP()
	{
		return P;
	}
	public double getUmax() {
		return Umax;
	}
	
	public void setP(double P)
	{
		this.P = P;
	}
	public void setUmax(double Umax)
	{
		this.Umax = Umax;
	}
	
	
	public double getU()
	{
		return positiveLoad.Uc-negativeLoad.Uc;
	}
	
	@Override
	public void process(double time) {
		double U = positiveLoad.Uc - negativeLoad.Uc;
		if(U > Umax) return;
		double C = positiveLoad.getC()/2;
		double E = P * time + U*U*C/2;
		double UTarget = Math.sqrt(2*E/C);
		if(UTarget > Umax) UTarget = Umax;
		double Q = (UTarget-U)*C;
		double I = Q/time;
		if(I > Imax) I = Imax;
		ElectricalLoad.moveCurrent(I, negativeLoad, positiveLoad);
		//energyCounter += UTarget*UTarget*C/2 - U*U*C/2;
		energyCounter += I*U*time;
	}


}
