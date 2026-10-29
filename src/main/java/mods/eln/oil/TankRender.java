package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The tank: its vessel, and the water in its sight glass (TankElement's fill, published). */
public class TankRender extends TransparentNodeElementRender {
    final TankDescriptor desc;
    float fill;
    /** Whether it draws the tank (a lone tank, or a box's lowest corner), and the box's size. */
    boolean draws = true;
    int sx = 1, sy = 1, sz = 1;

    public TankRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (TankDescriptor) d;
    }

    @Override
    public void draw() {
        if (!draws) return;
        if (sx == 1 && sy == 1 && sz == 1) {
            front.glRotateXnRef();
            desc.draw(fill);
            return;
        }
        // a box (TankGroup): one shell and sight glass over all of it, from its lowest corner (Baughn 2026-10-29)
        mods.eln.gl.GL11.glTranslatef((sx - 1) / 2f, (sy - 1) / 2f, (sz - 1) / 2f);
        front.glRotateXnRef();
        boolean alongX = front == mods.eln.misc.Direction.XN || front == mods.eln.misc.Direction.XP;
        mods.eln.gl.GL11.glScalef(alongX ? sx : sz, sy, alongX ? sz : sx);
        desc.drawBox(fill);
    }

    /** A box draws past its block: its whole size. */
    @Override
    public net.minecraft.world.phys.AABB renderBounds() {
        var p = tileEntity.getBlockPos();
        return draws && (sx > 1 || sy > 1 || sz > 1) ? new net.minecraft.world.phys.AABB(p.getX(), p.getY(), p.getZ(), p.getX() + sx, p.getY() + sy, p.getZ() + sz).inflate(1) : null;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            fill = stream.readFloat();
            draws = stream.readBoolean();
            sx = stream.readByte();
            sy = stream.readByte();
            sz = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
