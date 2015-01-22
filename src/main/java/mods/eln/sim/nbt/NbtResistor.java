package mods.eln.sim.nbt;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.State;

public class NbtResistor extends Resistor implements INBTTReady{
	
	public NbtResistor(String name,State aPin,State bPin) {
		super(aPin, bPin);
		this.name = name;
	}
	String name;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		name += str;
		setR(nbt.getDouble(str + "R"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		name += str;
		nbt.setDouble(str + "R",getR());
	}

}
