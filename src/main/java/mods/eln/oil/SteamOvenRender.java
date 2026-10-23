package mods.eln.oil;

import mods.eln.client.FrameTime;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The steam oven (SteamOvenDescriptor): the model, the plume from its vent while it cooks, and the GUI's values. */
public class SteamOvenRender extends TransparentNodeElementRender {
    final SteamOvenDescriptor desc;
    public float progress, perSecond, pressureBar;
    public boolean powered;
    final RcInterpolator steam = new RcInterpolator(1.5f);
    ItemEntity entityItemIn;
    float due;
    static final java.util.Random RND = new java.util.Random();

    public SteamOvenRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SteamOvenDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }

    @Override
    public void refresh(float deltaT) {
        progress = Math.min(1, progress + perSecond * FrameTime.getNotCaped2());
        steam.step(deltaT);
        var level = tileEntity.getLevel();
        float[] v = desc.vent();
        if (level == null || v == null || steam.get() < 0.02f) return;
        due += deltaT * 8 * Math.min(1.5f, steam.get());   // (the steam it doesn't use: a lively plume)
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
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, x + jx, y, z + jz, jx * 0.2, 0.04 + RND.nextDouble() * 0.03, jz * 0.2);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            progress = stream.readFloat();
            perSecond = stream.readFloat();
            pressureBar = stream.readFloat();
            steam.setTarget(stream.readFloat());
            powered = stream.readBoolean();
            entityItemIn = Utils.unserializeItemStackToEntityItem(stream, entityItemIn, tileEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
