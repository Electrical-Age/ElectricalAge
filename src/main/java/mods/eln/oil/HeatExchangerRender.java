package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The heat exchanger (mods.eln.cad.models.HeatExchanger); a stack draws as one unit from its lowest corner. */
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
        // the shell over the whole stack (scaled, as a tank box), from its lowest corner
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glTranslatef((sx - 1) / 2f, (sy - 1) / 2f, (sz - 1) / 2f);
        front.glRotateXnRef();
        boolean alongX = front == mods.eln.misc.Direction.XN || front == mods.eln.misc.Direction.XP;
        mods.eln.gl.GL11.glScalef(alongX ? sx : sz, sy, alongX ? sz : sx);
        desc.drawPart("shell");
        mods.eln.gl.GL11.glPopMatrix();
        // the end flanges, unscaled, on every block of both end faces: the front end's on the front, the back end's
        // turned to the back (mods.eln.cad.models.HeatExchanger)
        int len = alongX ? sx : sz, across = alongX ? sz : sx;
        boolean frontAtHigh = front == mods.eln.misc.Direction.XP || front == mods.eln.misc.Direction.ZP;
        for (int end = 0; end < 2; end++) {
            int a = end == 0 ? (frontAtHigh ? len - 1 : 0) : (frontAtHigh ? 0 : len - 1);   // (0: the front end)
            for (int y = 0; y < sy; y++)
                for (int c = 0; c < across; c++) {
                    mods.eln.gl.GL11.glPushMatrix();
                    mods.eln.gl.GL11.glTranslatef(alongX ? a : c, y, alongX ? c : a);
                    front.glRotateXnRef();
                    if (end == 1) mods.eln.gl.GL11.glRotatef(180, 0f, 1f, 0f);
                    desc.drawPart("flange");
                    mods.eln.gl.GL11.glPopMatrix();
                }
        }
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
