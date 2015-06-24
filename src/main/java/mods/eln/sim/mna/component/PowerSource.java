package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Power;
import mods.eln.sim.mna.primitives.Voltage;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class PowerSource extends VoltageSource implements IRootSystemPreStepProcess, INBTTReady {
	
	String name;

	Power P;
	Voltage Umax;
	Current Imax;
    
	public PowerSource(String name,State aPin) {
		super(name, aPin, null);
		this.name = name;
	}

	public void setP(Power P) {
		this.P = P;
	}
	
	void setMax(Voltage Umax, Current Imax) {
		this.Umax = Umax;
		this.Imax = Imax;
	}

	public void setImax(Current imax) {
		Imax = imax;
	}
	
	public void setUmax(Voltage umax) {
		Umax = umax;
	}

	public Power getP() {
		return P;
	}

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
		SubSystem.Th t = aPin.getSubSystem().getTh(aPin, this);
		
		Voltage U = new Voltage(Math.sqrt(t.U.getValue() * t.U.getValue() + 4 * P.getValue() * t.R.getValue())).add(t.U).multiply(0.5);
		U =  Voltage.min(Voltage.min(U, Umax), t.U.add(t.R.multiply(Imax)));
		if (U.isNaN()) U = new Voltage();
		U = Voltage.max(U, t.U);
	
		setU(U);
	}

	public Power getEffectiveP() {
		return getBipoleU().multiply(getCurrent());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		super.readFromNBT(nbt, str);
		
		str += name;
		
		setP(new Power(nbt.getDouble(str + "P")));
		setUmax(new Voltage(nbt.getDouble(str + "Umax")));
		setImax(new Current(nbt.getDouble(str + "Imax")));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		super.writeToNBT(nbt, str);
		
		str += name;
		
		nbt.setDouble(str + "P", getP().getValue());
		nbt.setDouble(str + "Umax", Umax.getValue());
		nbt.setDouble(str + "Imax", Imax.getValue());
	}
}
