package mods.eln.sim.mna.process;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.mna.component.DelayInterSystem2;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;

public class TransformerInterSystemProcess implements IRootSystemPreStepProcess {
	State aState, bState;
	VoltageSource aVoltgeSource, bVoltgeSource;

	double ratio = 1;
	

	public TransformerInterSystemProcess(State aState, State bState,VoltageSource aVoltgeSource,VoltageSource bVoltgeSource) {
		this.aState = aState;
		this.bState = bState;
		this.aVoltgeSource = aVoltgeSource;
		this.bVoltgeSource = bVoltgeSource;
	}

	@Override
	public void rootSystemPreStepProcess() {
		Th a = getTh(aState,aVoltgeSource);
		Th b = getTh(bState,bVoltgeSource);
		
		double aU = (a.U*b.R+ratio*b.U*a.R) / (b.R + ratio*ratio*a.R);
		if(Double.isNaN(aU)) {
			aU = 0;
		}
		
		aVoltgeSource.setU(aU);
		bVoltgeSource.setU(aU*ratio);
	}

	
	static class Th{
		double R,U;		
	}
	
	Th getTh(State d,VoltageSource voltageSource) {
		Th th = new Th();
		double originalU = d.state;

		double aU = 10;
		voltageSource.setU(aU);
		double aI = d.getSubSystem().solve(voltageSource.getCurrentState());

		double bU = 5;
		voltageSource.setU(bU);
		double bI = d.getSubSystem().solve(voltageSource.getCurrentState());

		double Rth = (aU - bU) / (bI - aI);
		double Uth;
		//if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
		if(Rth > 10000000000000000000.0 || Rth < 0) {
			Uth = 0;
			Rth = 10000000000000000000.0;
		} else {
			Uth = aU + Rth * aI;
		}
		voltageSource.setU(originalU);
		
		th.R = Rth;
		th.U = Uth;
		return th;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}
	
	


}
