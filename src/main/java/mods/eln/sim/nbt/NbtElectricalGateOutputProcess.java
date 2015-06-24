package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.primitives.Capacitance;
import mods.eln.sim.mna.primitives.Voltage;
import net.minecraft.nbt.NBTTagCompound;

public class NbtElectricalGateOutputProcess extends Capacitor implements INBTTReady {

	double U;
	String name;

	boolean highImpedance = false;

	public NbtElectricalGateOutputProcess(String name, ElectricalLoad positiveLoad) {
		super(positiveLoad, null);
		this.name = name;
		setHighImpedance(false);
	}
		
	public void setHighImpedance(boolean enable) {
		this.highImpedance = enable;
		double baseC = Eln.instance.gateOutputCurrent / Eln.instance.electricalFrequancy / Eln.SVU;
		if (enable) {
			setC(new Capacitance(baseC / 1000));
		} else {
			setC(new Capacitance(baseC));
		}
	}
	
	@Override
	public void simProcessI(SubSystem s) {
		if (!highImpedance)
			aPin.state = U;
		super.simProcessI(s);
	}
	
	public boolean isHighImpedance() {
		return highImpedance;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setHighImpedance(nbt.getBoolean(str + name + "highImpedance"));
		U = nbt.getDouble(str + name + "U");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setBoolean(str + name + "highImpedance", highImpedance);
		nbt.setDouble(str + name + "U", U);
	}
	
	public void setOutputNormalized(double value) {
		setOutputNormalizedSafe(value);
	}

	public void state(boolean value) {
		if (value)
			U = Eln.SVU;
		else
			U = 0.0;
	}

	public double getOutputNormalized() {
		return U / Eln.SVU;
	}

	public boolean getOutputOnOff() {
		return U >= Eln.SVU / 2;
	}

	public void setOutputNormalizedSafe(double value) {
		if (value > 1.0) value = 1.0;
		if (value < 0.0) value = 0.0;
		if (Double.isNaN(value)) value = 0.0;
		U = value * Eln.SVU;
	}

	public void setU(double U) {
		this.U = U;
	}
	
	public Voltage getU() {
		return new Voltage(U);
	}
}
