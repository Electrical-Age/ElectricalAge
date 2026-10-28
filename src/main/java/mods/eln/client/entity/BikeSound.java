package mods.eln.client.entity;

import mods.eln.bike.BikePhysics;
import mods.eln.bike.ElectricBike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * The bike's motor: EA's rotor tones, quiet, pitch with speed, while it runs on battery. It takes a sound source only
 * while it's heard: started when the motor runs, stopped after 2 s quiet or with the bike.
 */
public class BikeSound extends AbstractTickableSoundInstance {
    static final java.util.Map<ElectricBike, BikeSound> PLAYING = new java.util.WeakHashMap<>();
    final ElectricBike bike;
    int quiet;

    BikeSound(ElectricBike bike, net.minecraft.sounds.SoundEvent e) {
        super(e, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.bike = bike;
        looping = true;
        delay = 0;
        volume = 0;
        x = bike.getX();
        y = bike.getY();
        z = bike.getZ();
    }

    /** Each client tick of a bike (ElectricBike.clientSound): start its sound when the motor runs. */
    public static void tick(ElectricBike bike) {
        BikeSound playing = PLAYING.get(bike);
        if (playing != null && !playing.isStopped() || !running(bike)) return;
        var e = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.fromNamespaceAndPath("eln", "rotor_tone"));
        if (e == null) return;
        BikeSound s = new BikeSound(bike, e);
        PLAYING.put(bike, s);
        Minecraft.getInstance().getSoundManager().play(s);
    }

    static double speed(ElectricBike bike) {
        return Math.hypot(bike.getX() - bike.xo, bike.getZ() - bike.zo) / BikePhysics.VMAX_POWERED;
    }

    static boolean running(ElectricBike bike) {
        return bike.powered() && bike.mode() == ElectricBike.Mode.RIDDEN && speed(bike) > 0.02;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (bike.isRemoved()) {
            stop();
            return;
        }
        x = bike.getX();
        y = bike.getY();
        z = bike.getZ();
        double v = speed(bike);
        boolean on = running(bike);
        volume = on ? (float) (0.06 + 0.1 * Math.min(1, v)) : 0;
        quiet = on ? 0 : quiet + 1;
        if (quiet > 40) stop();
        pitch = (float) Math.max(0.5, Math.min(2, 0.7 + 1.0 * Math.min(1.2, v)));
    }
}
