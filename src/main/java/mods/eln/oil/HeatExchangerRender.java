package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The heat exchanger (the Steam Condenser's model until its own); a stack draws as one unit from its lowest corner. */
public class HeatExchangerRender extends TransparentNodeElementRender {
    final HeatExchangerDescriptor desc;
    /** Whether it draws (alone, or a stack's lowest corner), and the stack's size. */
    boolean draws = true;
    int sx = 1, sy = 1, sz = 1;

    public HeatExchangerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (HeatExchangerDescriptor) d;
    }

    @Override
    public void draw() {
        if (!draws) return;
        if (sx > 1 || sy > 1 || sz > 1) {
            // (one body over the whole stack, as a tank box: TankRender)
            mods.eln.gl.GL11.glTranslatef((sx - 1) / 2f, (sy - 1) / 2f, (sz - 1) / 2f);
            front.glRotateXnRef();
            boolean alongX = front == mods.eln.misc.Direction.XN || front == mods.eln.misc.Direction.XP;
            mods.eln.gl.GL11.glScalef(alongX ? sx : sz, sy, alongX ? sz : sx);
        } else front.glRotateXnRef();
        desc.draw();
    }

    /** A stack draws past its block: its whole size. */
    @Override
    public net.minecraft.world.phys.AABB renderBounds() {
        var p = tileEntity.getBlockPos();
        return draws && (sx > 1 || sy > 1 || sz > 1) ? new net.minecraft.world.phys.AABB(p.getX(), p.getY(), p.getZ(), p.getX() + sx, p.getY() + sy, p.getZ() + sz).inflate(1) : null;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            draws = stream.readBoolean();
            sx = stream.readByte();
            sy = stream.readByte();
            sz = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
