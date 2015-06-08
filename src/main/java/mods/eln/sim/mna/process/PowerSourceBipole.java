package mods.eln.sim.mna.process;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Power;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.primitives.Voltage;
import mods.eln.sim.mna.state.State;

public class PowerSourceBipole implements IRootSystemPreStepProcess, INBTTReady {
	
	private VoltageSource aSrc;
	private VoltageSource bSrc;
	private State aPin;
	private State bPin;

	Power P;
	Voltage Umax;
	Current Imax;

	public PowerSourceBipole(State aPin, State bPin, VoltageSource aSrc, VoltageSource bSrc) {
		this.aSrc = aSrc;
		this.bSrc = bSrc;
		this.aPin = aPin;
		this.bPin = bPin;
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
	public void rootSystemPreStepProcess() {
		SubSystem.Th a = aPin.getSubSystem().getTh(aPin, aSrc);
		SubSystem.Th b = bPin.getSubSystem().getTh(bPin, bSrc);
		
		Voltage Uth = a.U.substract(b.U);
		Resistance Rth = a.R.add(b.R);
		if (Uth.getValue() >= Umax.getValue()) {
			aSrc.setU(a.U);
			bSrc.setU(b.U);			
		} else {
			Voltage U = new Voltage(Math.sqrt(Uth.getValue() * Uth.getValue() + 4 * P.getValue() * Rth.getValue())).add(Uth).multiply(0.5);
			U =  Voltage.min(Voltage.min(U, Umax), Uth.add(Rth.multiply(Imax)));
			if (U.isNaN()) U = new Voltage();
			
			Current I = Uth.substract(U).divide(Rth);
			aSrc.setU(a.U.substract(I.multiply(a.R)));
			bSrc.setU(b.U.add(I.multiply(b.R)));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setP(new Power(nbt.getDouble(str + "P")));
		setUmax(new Voltage(nbt.getDouble(str + "Umax")));
		setImax(new Current(nbt.getDouble(str + "Imax")));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "P", getP().getValue());
		nbt.setDouble(str + "Umax", Umax.getValue());
		nbt.setDouble(str + "Imax", Imax.getValue());
	}
}
