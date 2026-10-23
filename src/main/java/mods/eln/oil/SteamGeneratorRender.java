package mods.eln.oil;

import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class SteamGeneratorRender extends TransparentNodeElementRender {
    /** The rotor's turn rate at nominal speed (degrees per second): slow enough to follow, a hint rather than 3000 rpm
     *  (Baughn: subtle). */
    static final float DEG_PER_S = 360;
    final SteamGeneratorDescriptor desc;
    final RcInterpolator speed = new RcInterpolator(1.5f), steam = new RcInterpolator(1.5f);
    static final java.util.Random RND = new java.util.Random();
    /** Puffs a second out of the vent at nominal steam: a calm stream (Baughn). */
    static final float PUFFS_PER_S = 6;
    float due;
    float angle = (float) (Math.random() * 360);

    public SteamGeneratorRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SteamGeneratorDescriptor) d;
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
        steam.step(deltaT);
        var level = tileEntity.getLevel();
        float[] v = desc.vent();
        if (level == null || v == null || steam.get() < 0.01f) return;
        due += deltaT * PUFFS_PER_S * (0.25f + 0.75f * Math.min(1.5f, steam.get()));   // (an idling turbine still wisps)
        // the outlet in the world: the model turned as glRotateXnRef turns it (about y: XN 0, ZP 90, XP 180, ZN 270)
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
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, x + jx, y, z + jz, jx * 0.2, 0.03 + RND.nextDouble() * 0.02, jz * 0.2);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            speed.setTarget(stream.readFloat());
            steam.setTarget(stream.readFloat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
