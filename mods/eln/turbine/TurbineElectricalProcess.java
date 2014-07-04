package mods.eln.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;


public class TurbineElectricalProcess implements IProcess,IRootSystemPreStepProcess{
	TurbineElement turbine;

	int id;
	public TurbineElectricalProcess(TurbineElement t) {
		this.turbine = t;
	}
	

	
	@Override
	public void process(double time) {
		TurbineDescriptor descriptor = turbine.descriptor;
		double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
		if(deltaT < 0) return;
		double deltaU = turbine.positiveLoad.getSubSystem().solve(turbine.positiveLoad);//turbine.positiveLoad.getU();
		double targetU = descriptor.TtoU.getValue(deltaT);

		PowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		double eP = 0;
		if(targetU - deltaU > 0)
			eP = (targetU - deltaU) * descriptor.powerOutPerDeltaU;
		//eP = eps.getP()*0.9 + eP*0.1;
		
		eps.setP(eP);		
	}



	@Override
	public void rootSystemPreStepProcess() {
		process(0);
	}

}
