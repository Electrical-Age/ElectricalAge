package mods.eln.electricalbreaker;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.IProcess;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalBreakerCutProcess implements IProcess,INBTTReady {
	ElectricalBreakerElement breaker;
	
	public ElectricalBreakerCutProcess(ElectricalBreakerElement breaker) {
		this.breaker = breaker;
	}
	
	double T = 0;
	
	@Override
	public void process(double time) {
		double U = breaker.aLoad.Uc;
		double I = breaker.aLoad.getCurrent();
		double Tmax = 0;
		ElectricalCableDescriptor cable = breaker.cableDescriptor;
		if(cable == null) {
			T = 0;
		}
		else {
			Math.min(I, cable.electricalNominalPower / cable.electricalMaximalVoltage * 10);			
			double P = I * I * cable.electricalRs * 2 - T / cable.thermalRp * 0.9;
			/*if(P > 200) {
				int i = 0;
				i++;
				Utils.println(P);
			}*/
			double pMax = Eln.electricalCableDeltaTMax * cable.thermalC;
			T += Math.min(pMax, P) / cable.thermalC * time;
			Tmax = cable.thermalWarmLimit * 0.8;
		}
		//Utils.println(T);
		
		if(U >= breaker.voltageMax || U < breaker.voltageMin || T > Tmax) {
			breaker.setSwitchState(false);
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		T = nbt.getFloat(str + "T");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setFloat(str + "T", (float) T);
	}
}
