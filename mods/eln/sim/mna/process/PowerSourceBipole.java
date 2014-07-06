package mods.eln.sim.mna.process;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;

public class PowerSourceBipole implements IRootSystemPreStepProcess,INBTTReady{
	
	private VoltageSource aSrc;
	private VoltageSource bSrc;
	private State aPin;
	private State bPin;

	public PowerSourceBipole(State aPin,State bPin,VoltageSource aSrc,VoltageSource bSrc) {
		this.aSrc = aSrc;
		this.bSrc = bSrc;
		this.aPin = aPin;
		this.bPin = bPin;
	}
	
		
	
	public void setP(double P){
		this.P = P;
	}
	
	void setMax(double Umax,double Imax){
		this.Umax = Umax;
		this.Imax = Imax;
	}
	
	
	public void setImax(double imax) {
		Imax = imax;
	}
	
	public void setUmax(double umax) {
		Umax = umax;
	}
	public double getP() {
		return P;
	}
	double P,Umax,Imax;
	

	@Override
	public void rootSystemPreStepProcess() {
		SubSystem.Th a = aPin.getSubSystem().getTh(aPin,aSrc);
		SubSystem.Th b = bPin.getSubSystem().getTh(bPin,bSrc);
		
		double Uth = a.U - b.U;
		double Rth = a.R + b.R;
		if(Uth >= Umax){
			aSrc.setU(a.U);
			bSrc.setU(b.U);			
		}else{	
			double U = (Math.sqrt(Uth*Uth+4*P*Rth)+Uth)/2;
			U =  Math.min(Math.min(U, Umax),Uth+Rth*Imax);
			if(Double.isNaN(U)) U = 0;
			
			double I = (Uth-U)/Rth;
			aSrc.setU(a.U-I*a.R);
			bSrc.setU(b.U+I*b.R);
		}
	}
	
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setP(nbt.getDouble(str + "P"));
		setUmax(nbt.getDouble(str + "Umax"));
		setImax(nbt.getDouble(str + "Imax"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "P",getP());
		nbt.setDouble(str + "Umax",Umax);
		nbt.setDouble(str + "Imax",Imax);
	}
}
