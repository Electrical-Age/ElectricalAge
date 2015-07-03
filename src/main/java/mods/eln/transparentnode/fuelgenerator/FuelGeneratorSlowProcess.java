package mods.eln.transparentnode.fuelgenerator;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;

public class FuelGeneratorSlowProcess implements IProcess,INBTTReady {

	FuelGeneratorElement generator;

	public FuelGeneratorSlowProcess(FuelGeneratorElement generator) {
		this.generator = generator;
	}

	double refreshTimeout = 0;
	static final double RefreshPeriod = 0.5;
	
	@Override
	public void process(double time) 
	{
		FuelGeneratorDescriptor d = generator.descriptor;

		refreshTimeout -=time;
		if(refreshTimeout < 0){
			refreshTimeout = RefreshPeriod;
			generator.powerSource.setP(d.nominalPower);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
	}
}
