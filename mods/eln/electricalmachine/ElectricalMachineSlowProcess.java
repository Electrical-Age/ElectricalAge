package mods.eln.electricalmachine;

import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;

public class ElectricalMachineSlowProcess implements IProcess {
	private ElectricalMachineElement element;
	
	public ElectricalMachineSlowProcess(ElectricalMachineElement element) {
		this.element = element;
	}
	
	double lastPublishAt = 0, lastUpdate = 0;
	boolean boot = true;
	
	double playSoundCounter;
	
	@Override
	public void process(double time) {
		double P = element.electricalResistor.getP();
		lastUpdate += time;
		if(!boot) {
			if(Math.abs((P - lastPublishAt) / (lastPublishAt + 1.0)) > 1 / 32.0 && lastUpdate > 0.2) {
				element.needPublish();
				lastPublishAt = P;
				lastUpdate = 0;
			}
		}
		
		double normalisedP = Math.pow(P/element.descriptor.nominalP,0.5);
		
		if(element.descriptor.runingSound != null && normalisedP > 0.3){
			if(playSoundCounter <= 0){
				float pitch = (float)normalisedP;
				playSoundCounter = element.descriptor.runingSoundLength/pitch;
				element.play(new SoundCommand(element.descriptor.runingSound).setVolume((float)normalisedP, pitch));
			}
		}
		
		if(playSoundCounter > 0)
			playSoundCounter -= time;
			
		boot = false;
	}
}
