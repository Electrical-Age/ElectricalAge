package mods.eln.electricalalarm;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundServer;

public class ElectricalAlarmSlowProcess implements IProcess {
	
	ElectricalAlarmElement element;
	
	public ElectricalAlarmSlowProcess(ElectricalAlarmElement element) {
		this.element = element;
	}
	
	double timeCounter = 0, soundTimeTimeout = Math.random()*2;
	static final double refreshPeriode = 0.25;
	int soundUuid = Utils.getUuid();
	boolean oldWarm = false;
	
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
					element.play(new SoundCommand(element.descriptor.soundName).setVolume(1F, 1.0F).longRange().addUuid(soundUuid));
					soundTimeTimeout = element.descriptor.soundTime;
				}
			}
			if(oldWarm == true && warm == false){
				element.killUuid(soundUuid);
				soundTimeTimeout = 0;
			}
			
			oldWarm = warm;
		}
		soundTimeTimeout -= time;
		if(soundTimeTimeout < 0) soundTimeTimeout = 0;
	}
}
