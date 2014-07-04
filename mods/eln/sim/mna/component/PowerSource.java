package mods.eln.sim.mna.component;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;

public class PowerSource extends VoltageSource implements IRootSystemPreStepProcess,INBTTReady{
	
	public PowerSource() {
		// TODO Auto-generated constructor stub
	}
	
	public PowerSource(State aPin) {
		super(aPin, null);
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
	public void quitSubSystem() {
		getSubSystem().getRoot().removeProcess(this);
		super.quitSubSystem();
	}
	
	@Override
	public void addedTo(SubSystem s) {
		super.addedTo(s);
		getSubSystem().getRoot().addProcess(this);
		s.addProcess(this);
	}

	@Override
	public void rootSystemPreStepProcess() {
		SubSystem.Th t = aPin.getSubSystem().getTh(aPin,this);
		
		double U = (Math.sqrt(t.U*t.U+4*P*t.R)+t.U)/2;
		U =  Math.min(Math.min(U, Umax),t.U+t.R*Imax);
		if(Double.isNaN(U)) U = 0;
		if(U < t.U) U = t.U;
	
		setU(U);
	}

	public double getEffectiveP() {
		return getBipoleU()*getCurrent();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		super.readFromNBT(nbt, str);
		setP(nbt.getDouble(str + "P"));
		setUmax(nbt.getDouble(str + "Umax"));
		setImax(nbt.getDouble(str + "Imax"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		super.writeToNBT(nbt, str);
		nbt.setDouble(str + "P",getP());
		nbt.setDouble(str + "Umax",Umax);
		nbt.setDouble(str + "Imax",Imax);
	}
}
