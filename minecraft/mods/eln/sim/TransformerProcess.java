package mods.eln.sim;

import mods.eln.misc.FunctionTable;

//All electrical load must have same C

public class TransformerProcess implements IProcess {
	ElectricalLoad positivePrimaryLoad, negativePrimaryLoad,positiveSecondaryLoad,negativeSecondaryLoad;
	private double ratio,precalcPrimary,precalcSecondary;
	
	public void setRatio(double ratio)
	{
		this.ratio = ratio;
		this.precalcPrimary = 1.0/(ratio*ratio + 1);
		this.precalcSecondary = ratio*ratio/(ratio*ratio + 1);
	}
	
	public TransformerProcess(ElectricalLoad positivePrimaryLoad,ElectricalLoad negativePrimaryLoad,ElectricalLoad positiveSecondaryLoad,ElectricalLoad negativeSecondaryLoad)
	{
		this.positivePrimaryLoad = positivePrimaryLoad;
		this.negativePrimaryLoad = negativePrimaryLoad;
		this.positiveSecondaryLoad = positiveSecondaryLoad;
		this.negativeSecondaryLoad = negativeSecondaryLoad;
	}

	@Override
	public void process(double time) {
		if(enable == false) return;
		// TODO Auto-generated method stub
		/*double Up,Us;
		double C = positivePrimaryLoad.C;
		Up = positivePrimaryLoad.Uc-negativePrimaryLoad.Uc;
		Us = positiveSecondaryLoad.Uc-negativeSecondaryLoad.Uc;
		double Ip,Is;
		if(Up*ratio > Us)
		{
			Ip = -10.0f;
			Is = 10.0f/ratio;
		}
		else
		{
			Ip = 10.0f;
			Is = -10.0f/ratio;			
		}
		
		positivePrimaryLoad.IcTemp += Ip;
		negativePrimaryLoad.IcTemp -= Ip;
		
		positiveSecondaryLoad.IcTemp += Is;
		negativeSecondaryLoad.IcTemp -= Is;
		*/
		
		double Up,Us;
		double Cp = positivePrimaryLoad.getC();
		double Cs = positiveSecondaryLoad.getC();
		Up = positivePrimaryLoad.Uc-negativePrimaryLoad.Uc;
		Us = positiveSecondaryLoad.Uc-negativeSecondaryLoad.Uc;
		double e = Math.signum(Up)*Up*Up*Cp + Math.signum(Us)* Math.signum(ratio)*Us*Us*Cs;
		double UpTarget,UsTarget;

	//	UpTarget = Math.signum(e)*Math.sqrt(Math.abs(e)*precalcPrimary);
	//	UsTarget = Math.signum(e)*Math.signum(ratio)*Math.sqrt(Math.abs(e)*precalcSecondary);
		
		UpTarget = Math.signum(e)*Math.sqrt(Math.abs(e)/(Cp + ratio*ratio*Cs));
		UsTarget = UpTarget * ratio;
	
		
		double Ip = (UpTarget-Up)*Cp/time/2;
		double Is = (UsTarget-Us)*Cs/time/2;

		ElectricalLoad.moveCurrent(Ip, negativePrimaryLoad, positivePrimaryLoad);
		ElectricalLoad.moveCurrent(Is, negativeSecondaryLoad, positiveSecondaryLoad);
		

	}
	boolean enable;
	public void setEnable(boolean enable)
	{
		this.enable = enable;
	}
	public double getRatio() {
		// TODO Auto-generated method stub
		return ratio;
	}
	

}
