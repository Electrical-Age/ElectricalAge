package mods.eln.sim;

public class ElectricalResistor implements IProcess{
	ElectricalLoad a,b;
	
	protected double R,Rinv;
	double I = 0;
	
	public ElectricalResistor(ElectricalLoad a,ElectricalLoad b) {
		this.a = a;
		this.b = b;
		highImpedance();
	}
	
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		I = (a.Uc-b.Uc)*Rinv;
		
		ElectricalLoad.moveCurrent(I, a, b);	
		
	}
	
	public double getP()
	{
		//	return (a.Uc - b.Uc)*(a.Uc - b.Uc)*Rinv; //has change -> check
		return Math.abs((a.Uc - b.Uc)*I); //has change -> check
	}

	public void setR(double r) {
		R = r;
		Rinv = 1 / r;
	}
	public double getR() {
		return R;
	}	
	public double getU() {
		return I*R;
	}
	
	public double getCurrent()
	{
		return I;
	}
	
	public void highImpedance() {
		setR(1000000000.0);
	}
}
