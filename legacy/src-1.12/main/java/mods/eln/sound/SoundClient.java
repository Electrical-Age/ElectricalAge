package mods.eln.sound;

import mods.eln.client.ClientProxy;
import mods.eln.client.SoundLoader;
import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class SoundClient {
    private static final java.util.Set<String> warned = new java.util.HashSet<>();

    /** 1.7.10 played sounds by name; 1.12 needs a registered SoundEvent (ElnSounds). Unknown names are skipped. */
    static SoundEvent soundEvent(String track) {
        SoundEvent event = ElnSounds.event(track);
        if (event == null && warned.add(track)) Utils.println("Electrical Age: no sound event for track " + track);
        return event;
    }

    /*public static void playFromBlock(World world, int x, int y, int z, String track, float volume, float pitch, float rangeNominal, float rangeMax) {
		play(world, x + 0.5, y + 0.5, z + 0.5, track, volume, pitch, rangeNominal, rangeMax);
	}*/

    public static void play(SoundCommand p) {
        ClientProxy.soundClientEventListener.currentUuid = p.uuid; //trolilole

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (p.world.provider.getDimension() != player.dimension) return;
        double distance = Math.sqrt(Math.pow(p.x - player.posX, 2) + Math.pow(p.y - player.posY, 2) + Math.pow(p.z - player.posZ, 2));
        if (distance >= p.rangeMax) return;
        float distanceFactor = 1;
        if (distance > p.rangeNominal) {
            distanceFactor = (float) ((p.rangeMax - distance) / (p.rangeMax - p.rangeNominal));
        }

        float blockFactor = Utils.traceRay(p.world, player.posX, player.posY, player.posZ, p.x, p.y, p.z, new Utils.TraceRayWeightOpaque()) * p.blockFactor;

        int trackCount = SoundLoader.getTrackCount(p.track);

        if (trackCount == 1) {
            float temp = 1.0f / (1 + blockFactor);
            p.volume *= Math.pow(temp, 2);
            p.volume *= distanceFactor;
            if (p.volume <= 0) return;

            SoundEvent event = soundEvent(p.track);
            if (event != null)
                p.world.playSound(player.posX + 2 * (p.x - player.posX) / distance, player.posY + 2 * (p.y - player.posY) / distance, player.posZ + 2 * (p.z - player.posZ) / distance, event, ElnSounds.category(p.track), p.volume, p.pitch, false);
        } else {
            for (int idx = 0; idx < trackCount; idx++) {
                float bandVolume = p.volume;
                bandVolume *= distanceFactor;
                float normalizedBlockFactor = blockFactor;

                bandVolume -= ((trackCount - 1 - idx) / (trackCount - 1f) + 0.2) * normalizedBlockFactor;
                Utils.print(bandVolume + " ");
                SoundEvent event = soundEvent(p.track + "_" + idx + "x");
                if (event != null)
                    p.world.playSound(player.posX + 2 * (p.x - player.posX) / distance, player.posY + 2 * (p.y - player.posY) / distance, player.posZ + 2 * (p.z - player.posZ) / distance, event, ElnSounds.category(p.track), bandVolume, p.pitch, false);
            }
            Utils.println("");
        }

        ClientProxy.soundClientEventListener.currentUuid = null;
    }
}
