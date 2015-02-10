package mods.eln.sound;

public interface IPlayer {
    
	void play(SoundCommand cmd);
	void stop(int uuid);
}
