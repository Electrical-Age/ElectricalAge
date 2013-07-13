package mods.eln.sim;

import mods.eln.misc.FunctionTable;



public class BatteryProcess implements IProcess {
	ElectricalLoad positiveLoad, negativeLoad;
	public  FunctionTable voltageFunction;
	public double Q = 0,QNominal = 0;
	public double uNominal = 0;
	public double dischargeCurrentMesure = 0;
	public double life = 1.0;
	//public double efficiency = 1.0;
	
	public boolean cut;
	public boolean isRechargeable = true;
	
	public BatteryProcess(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad,FunctionTable voltageFunction )
	{
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
		this.voltageFunction = voltageFunction;
		setCut(false);
	}

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		if(cut)
		{
			dischargeCurrentMesure = 0;
			return;
		}
		double voltage = computeVoltage();
		double Cequ = (positiveLoad.getC()*negativeLoad.getC())/(positiveLoad.getC()+negativeLoad.getC());
		double QNeeded = (voltage - (positiveLoad.Uc-negativeLoad.Uc))*Cequ;
		//Q -= QNeeded > 0 ? QNeeded : QNeeded * efficiency;
		if(isRechargeable == true || QNeeded > 0)
		{
			Q -= QNeeded;
			
			ElectricalLoad.moveCurrent(QNeeded/time, negativeLoad, positiveLoad);
			dischargeCurrentMesure = QNeeded/time;
		}
		else
		{
			dischargeCurrentMesure = 0;
		}
		
	}
	
	public void setCut(boolean enable)
	{
		cut = enable;
	}
	
	double computeVoltage()
	{
		double voltage = voltageFunction.getValue(Q/(QNominal*life));
		return voltage*uNominal;
	}	

	
	public double getQRatio()
	{
		return Q/QNominal;
	}
	
	public void changeLife(double newLife)
	{
		if(newLife < life)
		{
			this.Q *= newLife/life;
		}
		life = newLife;
	}
	
	public double getCharge()
	{
		return Q/(QNominal*life);
	}
	public void setCharge(double charge)
	{
		Q = QNominal * life * charge;
	}
	public double getEnergy()
	{
		int stepNbr = 50;
		double chargeStep = getCharge() / stepNbr;
		double chargeIntegrator = 0;
		double energy = 0;
		double QperStep = QNominal*life*getCharge() / stepNbr;
		
		for(int step = 0; step < stepNbr;step++)
		{	
			chargeIntegrator += chargeStep;
			double voltage = voltageFunction.getValue(chargeIntegrator)*uNominal;
			energy += voltage*QperStep;
		}
		
		return energy;
		
	}
	public double getEnergyMax()
	{
		int stepNbr = 50;
		double chargeStep = 1.0 / stepNbr;
		double chargeIntegrator = 0;
		double energy = 0;
		double QperStep = QNominal*life*1.0 / stepNbr;
		
		for(int step = 0; step < stepNbr;step++)
		{	
			chargeIntegrator += chargeStep;
			double voltage = voltageFunction.getValue(chargeIntegrator)*uNominal;
			energy += voltage*QperStep;
		}
		
		return energy;
		
	}	
	public double getU()
	{
		return computeVoltage();
	}
}
