package mods.eln.transparentnode.electricalmachine;

import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;

public class ElectricalMachineSlowProcess implements IProcess {
    
	private ElectricalMachineElement element;

    double lastPublishAt = 0, lastUpdate = 0;
    boolean boot = true;

    SoundLooper sound;
	
	public ElectricalMachineSlowProcess(ElectricalMachineElement e) {
		this.element = e;
		
		sound = new SoundLooper(e) {
			
			@Override
			public SoundCommand mustStart() {
				double P = element.electricalResistor.getP().getValue();
				double normalisedP = Math.pow(P / element.descriptor.nominalP, 0.5);
				if (element.descriptor.runingSound == null || normalisedP < 0.3) return null;
				
				float pitch = (float)normalisedP;
				
				return element.descriptor.runingSound.copy().mulVolume((float)normalisedP, pitch); 
			}
		};
	}

	@Override
	public void process(double time) {
		double P = element.electricalResistor.getP().getValue();
		lastUpdate += time;
		if (!boot) {
			if (Math.abs((P - lastPublishAt) / (lastPublishAt + 1.0)) > 1 / 32.0 && lastUpdate > 0.2) {
				element.needPublish();
				lastPublishAt = P;
				lastUpdate = 0;
			}
		}
		
		sound.process(time);
			
		boot = false;
	}
}
