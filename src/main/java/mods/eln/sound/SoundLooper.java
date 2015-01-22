package mods.eln.sound;

import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public abstract class SoundLooper implements IProcess {

	public SoundLooper(IPlayer player,boolean couldStop) {
		if(couldStop) 
			uuid = Utils.getUuid();
		this.player = player;
		this.couldStop = couldStop;
	}
	public SoundLooper(IPlayer player) {
		this(player,false);
	}	
	
	IPlayer player;
	double loopTimeout = Math.random();
	boolean couldStop;
	int uuid;
	
	@Override
	public void process(double time){
		
		SoundCommand track;
		
		if(loopTimeout > 0){
			loopTimeout -= time;
		}
		
		if(loopTimeout <= 0 && (track = mustStart()) != null){
			SoundCommand cmd = track.copy();
			if(couldStop) cmd.addUuid(uuid);
			player.play(cmd);
			loopTimeout = track.trackLength/track.pitch;
		}
		
		if(couldStop && mustStop()){
			if(loopTimeout != 0){
				loopTimeout = 0;
				player.stop(uuid);
			}
		}

	}
	
	
	public abstract SoundCommand mustStart();
	public boolean mustStop(){return false;}
}
