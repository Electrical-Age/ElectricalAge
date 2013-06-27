package mods.eln.mppt;

import java.util.ArrayList;

import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;

public class MpptSlowProcess implements IProcess,INBTTReady{

	MpptElement mppt;
	
	private double Utarget = 0;
	
	
	public void setUtarget(double utarget) {
		Utarget = utarget;
		mppt.outPowerSource.setUmax(Utarget);
	}
	
	public double getUtarget() {
		return Utarget;
	}
	
	public MpptSlowProcess(MpptElement mppt)
	{
		this.mppt = mppt;
	}
	
	boolean fall = true;
	double lastTryP = 0;
	double inResistorValue = Double.POSITIVE_INFINITY;
	
	double TimeLeft;
	


	void newTry()
	{
		double factor;
		factor = mppt.descriptor.inResistorStepFactor;
		if(tryNoChangeNumber > 3)
		{
			factor *= 1 + (tryNoChangeNumber-2)*0.2;
		}
		if(tryNoChangeNumber > 20) tryNoChangeNumber = 20;
		
		
		if(inResistorValue == mppt.descriptor.inResistorMin)
		{
			fall = false;
			tryNoChangeNumber = 0;
		}
		if(inResistorValue == mppt.descriptor.inResistorMax)
		{
			fall = true;
			tryNoChangeNumber = 0;
		}
		
		lastTryP = mppt.inResistor.getP();	
		if(fall)
		{
			inResistorValue = inResistorValue*(1-factor);	
			System.out.print("-- ");
		}
		else
		{
			inResistorValue = inResistorValue*(1+factor);		
			System.out.print("++ ");
		}
		
		
		if(inResistorValue < mppt.descriptor.inResistorMin)inResistorValue = mppt.descriptor.inResistorMin;
		if(inResistorValue > mppt.descriptor.inResistorMax)inResistorValue = mppt.descriptor.inResistorMax;
		
		System.out.println("P : "  + mppt.inResistor.getP() + "   inResistor : " + inResistorValue + "skype : " + (TimeLeft) + "  consecutiveTry : " + tryNoChangeNumber);
			
		mppt.inResistor.setR(inResistorValue);
		
		
		PList.clear();
		PDeltaList.clear();
		tryStepCounter = 0;
		TimeLeft = mppt.descriptor.inResistorLowHighTime;

	}
	

	boolean getCanSkype()
	{
		if(tryStepCounter >= 400)
		{
			if(Math.abs(pDerive2) / mppt.inResistor.getP() <  mppt.descriptor.inResistorStepFactor  * mppt.descriptor.inResistorStepFactor )
			{ 
				return true; 
			} 
		}
		return false;
	}
	
	int tryStepCounter = 0;
	int tryNoChangeNumber = 0;

	boolean riseP = true;
	
	double pLast;
	double pDerive1 = 0,pDerive2 = 0,pDerive1Last = 0;
	ArrayList<Double> PList = new ArrayList<Double>(),PDeltaList = new ArrayList<Double>();
	
	
	@Override
	public void process(double time) 
	{
		double inP = mppt.inResistor.getP();
		double outP = 0;
		MpptDescriptor descriptor = mppt.descriptor;
		
		if(mppt.outPowerSource.getU() *1.01 > mppt.outPowerSource.getUmax())
		{
			riseP = false;
		}
		else
		{
			riseP = true;
		}
		
		
		tryStepCounter++;
			
		pDerive1Last = pDerive1;
		pDerive1 = (mppt.inResistor.getP() - pLast) / time;
		pDerive2 = pDerive1-pDerive1Last;
		
		//InToOut
		if(mppt.inResistor.getU() >= descriptor.inUmin)
		{
			outP = descriptor.PoutfPin.getValue(inP);
		}
		mppt.outPowerSource.setP(outP);
		
		
		
		
		//SM
		TimeLeft -= time;
		
		PList.add(mppt.inResistor.getP());
		PDeltaList.add(mppt.inResistor.getP()-pLast);		
		
		if(TimeLeft < 0.0  || getCanSkype()) 
		{
			tryNoChangeNumber++;
			if(riseP)
			{
				if(mppt.inResistor.getP() < lastTryP)
				{
					fall = ! fall;
					tryNoChangeNumber = 0;
				}
			}
			else
			{
				if(fall == true)
				{
					fall = false;
					tryNoChangeNumber = 0;
				}
			}

			
			newTry();			
		}



		pLast = mppt.inResistor.getP();
	}

	
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		Utarget = nbt.getDouble(str + "MPPTSP" + "Utarget");
		inResistorValue = nbt.getDouble(str + "MPPTSP" + "inResistorValue");
		fall = nbt.getBoolean(str + "MPPTSP" + "fall");
		lastTryP = nbt.getDouble(str + "MPPTSP" + "lastTryP");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "MPPTSP" + "Utarget",Utarget);
		nbt.setDouble(str + "MPPTSP" + "inResistorValue", inResistorValue);
		nbt.setBoolean(str + "MPPTSP" + "fall", fall);
		nbt.setDouble(str + "MPPTSP" + "lastTryP", lastTryP);
	}
}
