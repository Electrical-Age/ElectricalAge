package mods.eln.transparentnode.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;


public class TurbineSlowProcess implements IProcess{
	TurbineElement turbine;
	//double timeCounter = 0, soundTimerCounter = Math.random()*soundTimeOut, energyCounterGlobal = 0;
	static int staticId = 0;
	int id;
	public TurbineSlowProcess(TurbineElement t) {
		this.turbine = t;
		id = staticId++;	
		soundLooper = new SoundLooper(t) {						
			@Override
			public SoundCommand mustStart() {
				double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
				if(deltaT < 40) return null;
				float factor = (float)(deltaT / turbine.descriptor.nominalDeltaT);
				SoundCommand track = turbine.descriptor.sound.copy().mulVolume(1 * (0.1f * factor), 0.9f + 0.2f * factor);
				return track;
			}
		};
	}
	
	SoundLooper soundLooper;

	
	
	@Override
	public void process(double time) {

		
		soundLooper.process(time);
		
	}

}
