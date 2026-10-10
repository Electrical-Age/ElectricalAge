package mods.eln.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Client playback of EA sound commands (1.12 SoundClient; client only): volume falls off linearly between the nominal
 * and the maximum range, and the sound is placed 2 blocks from the player towards the source, as in 1.12. Not ported:
 * the muffling by blocks in between (1.12 traceRay weights) and the multi-band tracks.
 */
public final class SoundClient {
    private SoundClient() {
    }

    private static final Set<String> warned = new HashSet<>();

    static SoundEvent event(String track) {
        SoundEvent e = ElnSounds.event(track);
        if (e == null && warned.add(track)) mods.eln.Eln.LOG.warn("EA: no sound event for track {}", track);
        return e;
    }

    public static void play(SoundCommand p) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || p.world == null || p.world != player.level()) return;
        double dx = p.x - player.getX(), dy = p.y - player.getY(), dz = p.z - player.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance >= p.rangeMax) return;
        float factor = distance > p.rangeNominal ? (float) ((p.rangeMax - distance) / (p.rangeMax - p.rangeNominal)) : 1f;
        float volume = p.volume * factor;
        if (volume <= 0) return;
        SoundEvent e = event(p.track);
        if (e == null) return;
        double d = Math.max(distance, 0.001);
        mc.getSoundManager().play(new SimpleSoundInstance(e.location(), ElnSounds.category(p.track), volume, p.pitch,
            net.minecraft.util.RandomSource.create(), false, 0, net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
            player.getX() + 2 * dx / d, player.getY() + 2 * dy / d, player.getZ() + 2 * dz / d, false));
    }
}
