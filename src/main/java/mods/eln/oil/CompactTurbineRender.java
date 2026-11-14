package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import mods.eln.sound.MachineSounds;

import java.io.DataInputStream;
import java.io.IOException;

/** The compact turbine: its rotor turns while it makes power; steam puffs from its stack while the safety valve lets go. */
public class CompactTurbineRender extends TransparentNodeElementRender {
    static final float DEG_PER_S = 360, PUFFS_PER_S = 8;
    final CompactTurbineDescriptor desc;
    final mods.eln.anim.Spin rotor = new mods.eln.anim.Spin(DEG_PER_S, 1.5).randomPhase();
    /** The safety valve's letting go (0..1), eased. */
    final mods.eln.anim.Smooth vent = new mods.eln.anim.Smooth(0.5);
    final mods.eln.anim.Emitter puffs = new mods.eln.anim.Emitter();

    public CompactTurbineRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (CompactTurbineDescriptor) d;
        // the 50 V steam turbine's sounds at the same rotor speed (Baughn), but a cheaper rotor: its tones waver; the
        // safety valve letting go hisses loud
        for (LoopedSound l : MachineSounds.electric(coordonate(), rotor::speedNow, () -> Math.min(1, rotor.speedNow()), 1, 0.015f, 0.8f))
            addLoopedSound(l);
        addLoopedSound(new LoopedSound("eln:steam_hiss", coordonate()) {
            @Override
            public float getVolume() {
                return 0.1f * Math.min(1, rotor.speedNow()) + 0.25f * (float) Math.min(1, vent.get());
            }

            @Override
            public float getPitch() {
                return 0.8f + 0.16f * (float) Math.min(1, vent.get());
            }
        });
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(rotor.deg());
    }

    @Override
    public void refresh(float deltaT) {
        rotor.step(deltaT);
        vent.step(deltaT);
        var level = tileEntity.getLevel();
        float[] v = desc.vent();
        int n = puffs.step(deltaT, vent.get() < 0.01 ? 0 : PUFFS_PER_S * Math.min(1, vent.get()));
        if (level == null || v == null || n == 0) return;
        var at = mods.eln.anim.Emitter.vent(tileEntity.getBlockPos(), front, v[0], v[1], v[2]);   // (the stack's outlet in the world)
        for (int i = 0; i < n; i++) mods.eln.anim.Plume.STEAM.puff(level, at.x, at.y, at.z, 0.03);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            rotor.speed(stream.readFloat());
            vent.target(stream.readFloat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
