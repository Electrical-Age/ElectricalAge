package mods.eln.client;

import mods.eln.Eln;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class SoundLoader {
	
    @ForgeSubscribe
    public void onSound(SoundLoadEvent event) {
        try {
            //event.manager.soundPoolSounds.addSound("eln/sound/alarma.ogg", Eln.class.getResource("sound/alarma.ogg"));            
            //event.manager.soundPoolSounds.addSound("eln/sound/smallalarm_critical.ogg", Eln.class.getResource("sound/smallalarm_critical.ogg"));            
            event.manager.soundPoolSounds.addSound("eln:alarma.ogg");            
            event.manager.soundPoolSounds.addSound("eln:smallalarm_critical.ogg");  
            event.manager.soundPoolSounds.addSound("eln:wind_turbine.ogg");
            event.manager.soundPoolSounds.addSound("eln:water_turbine.ogg");
            event.manager.soundPoolSounds.addSound("eln:heat_turbine_200v.ogg");
            event.manager.soundPoolSounds.addSound("eln:heat_turbine_50v.ogg");
        } 
        catch (Exception e) {
            System.err.println("Failed to register one or more sounds.");
        }
    }
}
