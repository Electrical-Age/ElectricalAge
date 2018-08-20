package mods.eln.sound;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import mods.eln.client.UuidManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;

public class SoundClientEventListener {

    UuidManager uuidManager;
    ArrayList<Integer> currentUuid = null;

    public SoundClientEventListener(UuidManager uuidManager) {
        this.uuidManager = uuidManager;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void event(PlaySoundSourceEvent e) {
        if (currentUuid == null) return;
        uuidManager.add(currentUuid, new SoundClientEntity(e.manager, e.sound));
    }

    static class KillSound {
        public ISound sound;
        public SoundManager sm;

        public void kill() {
            sm.stopSound(sound);
        }
    }
}
