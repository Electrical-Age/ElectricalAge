package mods.eln.sound;

import java.util.ArrayList;

import javax.swing.text.html.parser.Entity;

import mods.eln.client.SoundLoader;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundPool;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class SoundClient {
	public static void playFromBlock(World world,int x, int y, int z, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(world, x+0.5, y+0.5, z+0.5, track, volume, pitch, rangeNominal, rangeMax);
	}
	
	
	public static void play(World world,double x, double y, double z, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(world.provider.dimensionId != player.dimension) return;
		double distance = Math.sqrt(Math.pow(x-player.posX, 2) + Math.pow(y-player.posY, 2) + Math.pow(z-player.posZ, 2));
		if(distance >= rangeMax) return;
		float distanceFactor = 1;
		if(distance > rangeNominal){
			distanceFactor = (float) ((rangeMax - distance)/(rangeMax-rangeNominal));
		}
		
		float blockFactor = Utils.traceRay(world, player.posX, player.posY, player.posZ, x, y, z,new Utils.TraceRayWeightOpaque());

		int trackCount = SoundLoader.getTrackCount(track);
		
		if(trackCount == 1){
			volume -= blockFactor*0.2f;
			volume *= distanceFactor;
			if(volume <= 0) return;
			world.playSound(player.posX + (x-player.posX)/distance, player.posY + (y-player.posY)/distance, player.posZ + (z-player.posZ)/distance, track, volume, pitch, false);
		}else{
			for(int idx = 0;idx < trackCount;idx++){
				float bandVolume = volume;
				bandVolume *= distanceFactor;
				float normalizedBlockFactor = blockFactor*0.5f;
				
				bandVolume -= ((trackCount-1-idx)/(trackCount-1f)+0.2)*normalizedBlockFactor;
				Utils.print(bandVolume + " ");
				world.playSound(player.posX + (x-player.posX)/distance, player.posY + (y-player.posY)/distance, player.posZ + (z-player.posZ)/distance, track + "_" + idx + "x", bandVolume, pitch, false);
			}
			Utils.println("");
		}
		
		
		
	}
}
