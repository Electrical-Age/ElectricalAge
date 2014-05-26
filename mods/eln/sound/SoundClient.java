package mods.eln.sound;

import java.util.ArrayList;

import javax.swing.text.html.parser.Entity;

import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
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
		if(distance > rangeNominal){
			volume *= (rangeMax - distance)/(rangeMax-rangeNominal);
		}
		
		float b = Utils.traceRay(world, player.posX, player.posY, player.posZ, x, y, z,new Utils.TraceRayWeightOpaque());
		volume -= b*0.2;
		//System.out.println(b);
		
		if(volume <= 0) return;
		world.playSound(player.posX + (x-player.posX)/distance, player.posY + (y-player.posY)/distance, player.posZ + (z-player.posZ)/distance, track, volume, pitch, false);
	}
}
