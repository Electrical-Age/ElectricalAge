package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import mods.eln.sound.MachineSounds;

import java.io.DataInputStream;
import java.io.IOException;

public class SteamGeneratorRender extends TransparentNodeElementRender implements SteamValveControl.Rendered {
    /** The rotor's turn rate at nominal speed (degrees per second): slow enough to follow, a hint rather than 3000 rpm
     *  (Baughn: subtle). */
    static final float DEG_PER_S = 360;
    final SteamGeneratorDescriptor desc;
    final mods.eln.anim.Spin rotor = new mods.eln.anim.Spin(DEG_PER_S, 1.5).randomPhase();
    /** The steam it takes (per unit of nominal), eased. */
    final mods.eln.anim.Smooth steam = new mods.eln.anim.Smooth(1.5);
    final mods.eln.anim.Emitter puffs = new mods.eln.anim.Emitter();
    /** Puffs a second out of the vent at nominal steam: a calm stream (Baughn). */
    static final float PUFFS_PER_S = 6;
    /** The steam's loudness at nominal flow (the loops are ~-18 dBFS; EA's older loops play quieter still). */
    static final float HISS_VOLUME = 0.1f;
    /** The rotor's pitch: the 50 V turbine turns as fast as the compact one; the bigger 200 V rotor sounds lower
     *  (Baughn). */
    final float PITCH;
    /** A condenser against its exhaust side: the duct into it, no vent stack (and no puffs). */
    boolean condenser;
    /** Its controls, for the screen (SteamGeneratorScreen). */
    public final SteamValveControl.Render controls;

    @Override
    public SteamValveControl.Render controls() {
        return controls;
    }


    public SteamGeneratorRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SteamGeneratorDescriptor) d;
        controls = new SteamValveControl.Render(this, SteamGeneratorElement.TARGET_MAX_BAR);
        PITCH = desc.nominalU > 100 ? 0.8f : 1f;
        // a generator's hum (by the steam it takes, as its output follows it), rotor tones and air (MachineSounds),
        // and the steam rushing through, muffled by a condenser
        for (LoopedSound l : MachineSounds.electric(coordonate(), rotor::speedNow, () -> (float) Math.min(1, steam.get()), PITCH, 0, 1))
            addLoopedSound(l);
        addLoopedSound(new LoopedSound("eln:steam_hiss", coordonate()) {
            @Override
            public float getVolume() {
                return HISS_VOLUME * (float) Math.min(1, steam.get()) * (condenser ? 0.5f : 1);
            }

            @Override
            public float getPitch() {
                return 0.72f + 0.16f * (float) Math.min(1, steam.get());
            }
        });
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(rotor.deg(), condenser);
    }

    @Override
    public void refresh(float deltaT) {
        rotor.step(deltaT);
        steam.step(deltaT);
        var level = tileEntity.getLevel();
        float[] v = desc.vent();
        boolean venting = v != null && !condenser && steam.get() >= 0.01;
        // (an idling turbine still wisps)
        int n = puffs.step(deltaT, venting ? PUFFS_PER_S * (0.25 + 0.75 * Math.min(1.5, steam.get())) : 0);
        if (level == null || n == 0) return;
        var at = mods.eln.anim.Emitter.vent(tileEntity.getBlockPos(), front, v[0], v[1], v[2]);   // (the vent stack's outlet)
        for (int i = 0; i < n; i++) mods.eln.anim.Plume.STEAM.puff(level, at.x, at.y, at.z, 0.03);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            rotor.speed(stream.readFloat());
            steam.target(stream.readFloat());
            condenser = stream.readBoolean();
            controls.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
