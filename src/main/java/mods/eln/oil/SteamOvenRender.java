package mods.eln.oil;

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
    public float progress, perSecond, pressureBar, steamMbS;
    public boolean powered;
    final RcInterpolator steam = new RcInterpolator(1.5f);
    ItemEntity entityItemIn;
    final mods.eln.anim.Emitter plume = new mods.eln.anim.Emitter();
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
        progress = Math.min(1, progress + perSecond * deltaT);
        steam.step(deltaT);
        var level = tileEntity.getLevel();
        float[] v = desc.vent();
        // (the steam it doesn't use: a lively plume)
        int n = plume.step(deltaT, v != null && steam.get() >= 0.02f ? 8 * Math.min(1.5f, steam.get()) : 0);
        if (level == null || n == 0) return;
        double th = mods.eln.anim.Emitter.turn(front), c = Math.cos(th), s = Math.sin(th);
        var at = mods.eln.anim.Emitter.vent(tileEntity.getBlockPos(), front, v[0], v[1], v[2]);
        for (int i = 0; i < n; i++) {
            // out of the back face (model +x), then rising: a boiler set on top doesn't hide it (AdventurAgent)
            double out = 0.04 + RND.nextDouble() * 0.02;
            mods.eln.anim.Plume.STEAM.puff(level, at.x, at.y, at.z, 0.03, c * out, -s * out);
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
            steamMbS = stream.readFloat();
            powered = stream.readBoolean();
            entityItemIn = Utils.unserializeItemStackToEntityItem(stream, entityItemIn, tileEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
