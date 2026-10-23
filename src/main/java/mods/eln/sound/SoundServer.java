package mods.eln.sound;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SoundServer {
    /*public static void playFromBlock(World world,int x, int y, int z, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(world, x+0.5, y+0.5, z+0.5, track, volume, pitch, rangeNominal, rangeMax,1);
	}
	public static void playFromCoord(Coordonate c, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(c.world(), c.x+0.5, c.y+0.5, c.z+0.5, track, volume, pitch, rangeNominal, rangeMax,1);
	}
	
	public static void playFromCoord(Coordonate c, String track, float volume, float pitch,float rangeNominal,float rangeMax,float blockFactor){
		play(c.world(), c.x+0.5, c.y+0.5, c.z+0.5, track, volume, pitch, rangeNominal, rangeMax,blockFactor);
	}*/


    public static void play(SoundCommand p) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.packetPlaySound);

            stream.writeInt(mods.eln.misc.Dimensions.id(p.world)); // 1.12 port: dimension as int (was byte)

            p.writeTo(stream);

            if (!(p.world instanceof net.minecraft.server.level.ServerLevel)) return;
            for (ServerPlayer player : ((net.minecraft.server.level.ServerLevel) p.world).players()) {
                // WP16: 1.7.10 had an empty `;` body here, so every sound went to every player (any dimension)
                if (Math.sqrt(player.distanceToSqr(p.x, p.y, p.z)) < p.rangeMax + 2)
                    Utils.sendPacketToClient(bos, player);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
