package mods.eln.sound;

import mods.eln.client.ClientProxy;
import mods.eln.client.SoundLoader;
import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class SoundClient {
    // TODO(1.10): Fix sounds.
    public static void play(SoundCommand p) {
        ClientProxy.soundClientEventListener.currentUuid = p.uuid; //trolilole

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (p.world.provider.getDimension() != player.dimension) return;
        double distance = Math.sqrt(Math.pow(p.x - player.posX, 2) + Math.pow(p.y - player.posY, 2) + Math.pow(p.z - player.posZ, 2));
        if (distance >= p.rangeMax) return;
        float distanceFactor = 1;
        if (distance > p.rangeNominal) {
            distanceFactor = (float) ((p.rangeMax - distance) / (p.rangeMax - p.rangeNominal));
        }

        float blockFactor = Utils.traceRay(p.world, player.posX, player.posY, player.posZ, p.x, p.y, p.z, new Utils.TraceRayWeightOpaque()) * p.blockFactor;

        int trackCount = SoundLoader.getTrackCount(p.track);

        BlockPos soundPos = new BlockPos(p.x, p.y, p.z);
        if (trackCount == 1) {
            float temp = 1.0f / (1 + blockFactor);
            p.volume *= Math.pow(temp, 2);
            p.volume *= distanceFactor;
            if (p.volume <= 0) return;
            p.world.playSound(
                player,
                soundPos,
                new SoundEvent(new ResourceLocation("eln", p.track)),
                SoundCategory.BLOCKS,  // TODO(1.10): Move this to the sound command.
                p.volume,
                p.pitch);
        } else {
            for (int idx = 0; idx < trackCount; idx++) {
                float bandVolume = p.volume;
                bandVolume *= distanceFactor;
                bandVolume -= ((trackCount - 1 - idx) / (trackCount - 1f) + 0.2) * blockFactor;
                Utils.print(bandVolume + " ");
                if (bandVolume > 0) {
                    p.world.playSound(
                        player,
                        soundPos,
                        new SoundEvent(new ResourceLocation("eln", p.track + "_" + idx + "x")),
                        SoundCategory.BLOCKS,
                        bandVolume,
                        p.pitch);
                }
            }
            Utils.println("");
        }

        ClientProxy.soundClientEventListener.currentUuid = null;
    }
}
