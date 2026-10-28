package mods.eln.oil;

import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;

import java.io.DataInputStream;
import java.io.IOException;

/** The compact turbine: its rotor turns while it makes power; steam puffs from its stack while the safety valve lets go. */
public class CompactTurbineRender extends TransparentNodeElementRender {
    static final float DEG_PER_S = 360, PUFFS_PER_S = 8;
    static final java.util.Random RND = new java.util.Random();
    final CompactTurbineDescriptor desc;
    final RcInterpolator speed = new RcInterpolator(1.5f), vent = new RcInterpolator(0.5f);
    float angle = (float) (Math.random() * 360), due;

    public CompactTurbineRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (CompactTurbineDescriptor) d;
        // the 50 V steam turbine's sounds at the same rotor speed (Baughn), but a cheaper rotor: its whine wavers; the
        // safety valve letting go hisses loud
        addLoopedSound(new LoopedSound("eln:steam_turbine_spin", coordonate()) {
            @Override
            public float getVolume() {
                return 0.13f * Math.min(1, speed.get() * 1.4f);
            }

            @Override
            public float getPitch() {
                float waver = 1 + 0.015f * (float) Math.sin(System.nanoTime() * 1e-9 * 2 * Math.PI * 2.7);
                return waver * (0.5f + 0.5f * Math.min(2, speed.get()));
            }
        });
        addLoopedSound(new LoopedSound("eln:steam_hiss", coordonate()) {
            @Override
            public float getVolume() {
                return 0.1f * Math.min(1, speed.get()) + 0.25f * Math.min(1, vent.get());
            }

            @Override
            public float getPitch() {
                return 0.8f + 0.16f * Math.min(1, vent.get());
            }
        });
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(angle);
    }

    @Override
    public void refresh(float deltaT) {
        speed.step(deltaT);
        angle = (angle + deltaT * DEG_PER_S * speed.get()) % 360;
        vent.step(deltaT);
        var level = tileEntity.getLevel();
        float[] v = desc.vent();
        if (level == null || v == null || vent.get() < 0.01f) return;
        due += deltaT * PUFFS_PER_S * Math.min(1, vent.get());
        // the outlet in the world: the model turned as glRotateXnRef turns it (as SteamGeneratorRender)
        double th = Math.toRadians(switch (front) {
            case XP -> 180;
            case ZP -> 90;
            case ZN -> 270;
            default -> 0;
        });
        double c = Math.cos(th), s = Math.sin(th);
        var p = tileEntity.getBlockPos();
        double x = p.getX() + 0.5 + v[0] * c + v[2] * s, y = p.getY() + 0.5 + v[1], z = p.getZ() + 0.5 - v[0] * s + v[2] * c;
        while (due >= 1) {
            due -= 1;
            double jx = (RND.nextDouble() - 0.5) * 0.06, jz = (RND.nextDouble() - 0.5) * 0.06;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, x + jx, y, z + jz, jx * 0.2, 0.05 + RND.nextDouble() * 0.03, jz * 0.2);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            speed.setTarget(stream.readFloat());
            vent.setTarget(stream.readFloat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
