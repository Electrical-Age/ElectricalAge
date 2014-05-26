package mods.eln.client;

import java.util.HashMap;

import mods.eln.Eln;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class SoundLoader {

	@ForgeSubscribe
	public void onSound(SoundLoadEvent event) {
		try {

			event.manager.soundPoolSounds.addSound("eln:alarma.ogg");
			event.manager.soundPoolSounds.addSound("eln:smallalarm_critical.ogg");
			event.manager.soundPoolSounds.addSound("eln:wind_turbine.ogg");
			event.manager.soundPoolSounds.addSound("eln:water_turbine.ogg");
			event.manager.soundPoolSounds.addSound("eln:heat_turbine_200v.ogg");
			//event.manager.soundPoolSounds.addSound("eln:heat_turbine_50v.ogg");
			//event.manager.soundPoolSounds.addSound("eln:heat_turbine_50v_0x.ogg");
			loadMultiTrack(event, "eln:heat_turbine_50v", 3);
		} catch (Exception e) {
			System.err.println("Failed to register one or more sounds.");
		}
	}

	public void loadMultiTrack(SoundLoadEvent event, String name, int count) {
		for (int idx = 0; idx < count; idx++) {
			event.manager.soundPoolSounds.addSound(name + "_" + idx + "x.ogg");
		}
		multiTrackMap.put(name, count);
	}

	static HashMap<String, Integer> multiTrackMap = new HashMap<String, Integer>();

	static public int getTrackCount(String name) {
		Integer i = multiTrackMap.get(name);
		if (i == null)
			return 1;
		return i;
	}
}
