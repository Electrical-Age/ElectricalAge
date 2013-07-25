package mods.eln.thermaldissipatoractive;

import mods.eln.sim.IProcess;

public class ThermalDissipatorActiveSlowProcess implements IProcess{
	ThermalDissipatorActiveElement dissipator;
	
	public ThermalDissipatorActiveSlowProcess(ThermalDissipatorActiveElement dissipator) {
		this.dissipator = dissipator;
	}
	
	@Override
	public void process(double time) {
		ThermalDissipatorActiveDescriptor descriptor = dissipator.descriptor;
		double poweredFactor = dissipator.positiveLoad.getRpPower() / descriptor.electricalNominalP;
		double thermalRp = 1/(1/descriptor.thermalRp + poweredFactor/(descriptor.electricalToThermalRp));
		dissipator.thermalLoad.setRp(thermalRp);
		
		
		if(Math.abs(dissipator.lastPowerFactor - (float) (poweredFactor)) > 0.2f)
			dissipator.needPublish();
	}

}
