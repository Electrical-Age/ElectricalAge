package mods.eln.sim.mna.component;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.state.State;

public class ResistorSwitch extends Resistor implements INBTTReady {

	boolean ultraImpedance = false;
	String name;

    boolean state = false;

    protected Resistance baseR = new Resistance(1);

	public ResistorSwitch(String name, State aPin, State bPin) {
		super(aPin, bPin);
		this.name = name;
	}

	public void setState(boolean state) {
		this.state = state;
		setR(baseR);
	}

	@Override
	public Resistor setR(Resistance r) {
		baseR = r;
		return super.setR(state ? r : (ultraImpedance ? Resistor.ultraImpedance : Resistor.highImpedance));
	}

	public boolean getState() {
		return state;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		str += name;
		setR(new Resistance(nbt.getDouble(str + "R")));
		if (baseR.isNaN() || baseR.getValue() == 0) {
			if (ultraImpedance)  ultraImpedance(); else highImpedance();
		}
		setState(nbt.getBoolean(str + "State"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		str += name;
		nbt.setDouble(str + "R", baseR.getValue());
		nbt.setBoolean(str + "State", getState());
	}

	public void mustUseUltraImpedance() {
		ultraImpedance = true;
	}
}
