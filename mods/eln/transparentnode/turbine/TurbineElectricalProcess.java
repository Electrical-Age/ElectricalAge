package mods.eln.transparentnode.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.mna.SubSystem.Th;
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
		/*TurbineDescriptor descriptor = turbine.descriptor;
		double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
		if(deltaT < 0) return;
		double deltaU = turbine.positiveLoad.getSubSystem().solve(turbine.positiveLoad);//turbine.positiveLoad.getU();
		double targetU = descriptor.TtoU.getValue(deltaT);

		PowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		double eP = 0;
		if(targetU - deltaU > 0)
			eP = (targetU - deltaU) * descriptor.powerOutPerDeltaU;
		eP = eps.getP()*0.1 + eP*0.9;
		
		eP = Math.min(eP, turbine.descriptor.nominalP*3);
		//eP = 250;
		eps.setP(eP);		*/
		TurbineDescriptor descriptor = turbine.descriptor;
		double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
		if(deltaT < 0) return;
		double deltaU = turbine.positiveLoad.getSubSystem().solve(turbine.positiveLoad);//turbine.positiveLoad.getU();
		double targetU = descriptor.TtoU.getValue(deltaT);

		
		Th th = turbine.positiveLoad.getSubSystem().getTh(turbine.positiveLoad, turbine.electricalPowerSourceProcess);
		double Ut;
		if(targetU < th.U){
			Ut = th.U;
		}else{
			double f = descriptor.powerOutPerDeltaU;
			Ut = (Math.sqrt(-4*f*th.R*th.U+f*f*th.R*th.R+4*f*targetU*th.R)+2*th.U-f*th.R)/2;			
		}

//		Ut = th.U;
		//double Ut2 = -(Math.sqrt(-4*f*th.R*th.U+f*f*th.R*th.R+4*f*targetU*th.R)-2*th.U+f*th.R)/2;
		
		turbine.electricalPowerSourceProcess.setU(Ut);
		
		/*PowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		double eP = 0;
		if(targetU - deltaU > 0)
			eP = (targetU - deltaU) * descriptor.powerOutPerDeltaU;
		eP = eps.getP()*0.1 + eP*0.9;
		
		eP = Math.min(eP, turbine.descriptor.nominalP*3);
		
		eps.setUmax(Ut);
		eP = 2500;
		eps.setP(eP);	*/
	}



	@Override
	public void rootSystemPreStepProcess() {
		process(0);
	}

}
