package mods.eln.electricalalarm;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.sim.IProcess;

public class ElectricalAlarmSlowProcess implements IProcess {
	
	ElectricalAlarmElement element;
	
	public ElectricalAlarmSlowProcess(ElectricalAlarmElement element) {
		this.element = element;
	}
	
	double timeCounter = 0, soundTimeTimeout = Math.random();
	static final double refreshPeriode = 0.25;
	
	@Override
	public void process(double time) {
		timeCounter += time;
		if(timeCounter > refreshPeriode) {
			timeCounter -= refreshPeriode;
			
			boolean warm = element.inputGate.Uc > Eln.instance.SVU / 2;		
			element.setWarm(warm);
			if(warm & !element.mute) {
				if(soundTimeTimeout == 0) {
					float speed = 1f;
					Coordonate coord = element.sixNode.coordonate;
					coord.world().playSoundEffect(coord.x, coord.y, coord.z, element.descriptor.soundName, element.descriptor.soundLevel, speed);
					soundTimeTimeout = element.descriptor.soundTime;
				}
			}
		}
		soundTimeTimeout -= time;
		if(soundTimeTimeout < 0) soundTimeTimeout = 0;
	}
}
