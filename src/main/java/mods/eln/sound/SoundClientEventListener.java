package mods.eln.sound;

import java.util.ArrayList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mods.eln.client.UuidManager;
import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.client.event.sound.SoundEvent.SoundSourceEvent;
import net.minecraftforge.common.MinecraftForge;

public class SoundClientEventListener {
	UuidManager uuidManager;
	ArrayList<Integer> currentUuid = null;
	public SoundClientEventListener(UuidManager uuidManager){
		this.uuidManager = uuidManager;
		MinecraftForge.EVENT_BUS.register(this);		
	}
	@SubscribeEvent
	public void event(PlaySoundSourceEvent e){
		if(currentUuid == null) return;
		uuidManager.add(currentUuid,new SoundClientEntity(e.manager, e.sound));
	}
	
	static class KillSound{
		public ISound sound;
		public SoundManager sm;
		
		public void kill(){
			sm.stopSound(sound);
		}
	}
	

}
