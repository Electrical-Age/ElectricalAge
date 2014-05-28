package mods.eln.sound;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.text.html.parser.Entity;

import cpw.mods.fml.common.FMLCommonHandler;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class SoundServer {
	public static void playFromBlock(World world,int x, int y, int z, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(world, x+0.5, y+0.5, z+0.5, track, volume, pitch, rangeNominal, rangeMax);
	}
	public static void playFromCoord(Coordonate c, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(c.world(), c.x+0.5, c.y+0.5, c.z+0.5, track, volume, pitch, rangeNominal, rangeMax);
	}
	
	
	public static void play(World world,double x, double y, double z, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		
    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);   	

        try {
        
	        stream.writeByte(Eln.packetPlaySound);
	        
	        stream.writeByte(world.provider.dimensionId);
	        stream.writeInt((int)(x*8));
	        stream.writeInt((int)(y*8));
	        stream.writeInt((int)(z*8));
	        
	       
	        stream.writeUTF(track);
	        stream.writeFloat(volume);
	        stream.writeFloat(pitch);
	        stream.writeFloat(rangeNominal);
	        stream.writeFloat(rangeMax);
		
	       


	        
	        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();       

	        for (Object obj :  server.getConfigurationManager().playerEntityList)
		    {
		    	EntityPlayerMP player = (EntityPlayerMP) obj;
		    	if(player.dimension == world.provider.dimensionId && player.getDistance(x, y, z) < rangeMax + 2);
		    		Utils.sendPacketToClient(bos,player);
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}		
		
		
	}
}
