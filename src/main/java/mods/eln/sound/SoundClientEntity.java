package mods.eln.sound;

import mods.eln.client.IUuidEntity;
import mods.eln.misc.Utils;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;

public class SoundClientEntity implements IUuidEntity {

    public ISound sound;
    public SoundManager sm;

    int borneTimer = 5;

    public SoundClientEntity(SoundManager sm, ISound sound) {
        this.sound = sound;
        this.sm = sm;
    }

    @Override
    public boolean isAlive() {
        if (borneTimer != 0) {
            borneTimer--;
            return true;
        }
        return sm.isSoundPlaying(sound);
    }

    @Override
    public void kill() {
        Utils.println("Sound deleted");
        sm.stopSound(sound);
    }
}
