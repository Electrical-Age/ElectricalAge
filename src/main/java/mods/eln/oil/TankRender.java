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
    /** In a box: its ports with a pipe on them (TankElement.coupled): a flange each; and its place in the box. */
    int coupled, ox, oy, oz;
    /** The shell's radius (px; tools/models/tank.py R), its top (px), and its flanges' depth (px). */
    static final float SHELL_R = 5.5f, SHELL_TOP = 15f, DISC = 0.75f;

    public TankRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (TankDescriptor) d;
    }

    @Override
    public void draw() {
        // a box's flanges, where pipes meet it, on this block's faces
        if (coupled != 0) {
            for (int face = 2; face <= 5; face++)
                for (int hi = 0; hi < 2; hi++) {
                    if ((coupled & 1 << (2 * face + hi)) == 0) continue;
                    mods.eln.gl.GL11.glPushMatrix();
                    mods.eln.misc.Direction.fromEnumFacing(net.minecraft.core.Direction.from3DDataValue(face)).glRotateXnRef();
                    desc.drawPart(hi == 0 ? "disc_low" : "disc_high");
                    // a nozzle from behind the flange in to the shell, which a box scales away from its faces
                    // (AdventurAgent 2026-10-29: on a 2 x 2 x 2 iron tank a pipe ended a third of a block short of it)
                    float len = nozzle(face) - DISC;
                    if (len > 0.05f) {
                        mods.eln.gl.GL11.glTranslatef(-0.5f + DISC / 16f, 0, 0);
                        mods.eln.gl.GL11.glScalef(len, 1, 1);
                        mods.eln.gl.GL11.glTranslatef(0.5f, 0, 0);
                        desc.drawPart(hi == 0 ? "nozzle_low" : "nozzle_high");
                    }
                    mods.eln.gl.GL11.glPopMatrix();
                }
            if ((coupled & 1 << 12) != 0) {
                desc.drawPart("disc_top");
                float len = 16 * sy - SHELL_TOP * sy - DISC;   // (the box's top down to its shell's)
                if (len > 0.05f) {
                    mods.eln.gl.GL11.glPushMatrix();
                    mods.eln.gl.GL11.glTranslatef(0, 0.5f - DISC / 16f, 0);
                    mods.eln.gl.GL11.glScalef(1, len, 1);
                    mods.eln.gl.GL11.glTranslatef(0, -0.5f, 0);
                    desc.drawPart("nozzle_top");
                    mods.eln.gl.GL11.glPopMatrix();
                }
            }
        }
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

    /**
     * From this block's FACE (3D value, a side) in to the box's shell (px): the shell is the lone tank's scaled to the box,
     * an upright cylinder of radius SHELL_R x the box's size across each axis, centred in it; the port is at the middle
     * of this block's face.
     */
    float nozzle(int face) {
        boolean alongX = face == 4 || face == 5;   // (west/east faces: the nozzle runs along x)
        int o = alongX ? ox : oz, n = alongX ? sx : sz, oAcross = alongX ? oz : ox, nAcross = alongX ? sz : sx;
        double across = (oAcross * 16 + 8) - nAcross * 8.0, half = SHELL_R * nAcross;
        double reach = SHELL_R * n * Math.sqrt(Math.max(0, 1 - across * across / (half * half)));
        boolean high = face == 3 || face == 5;   // (south/east: the face at the block's far side)
        double faceAt = high ? (o + 1) * 16 : o * 16, shellAt = high ? n * 8.0 + reach : n * 8.0 - reach;
        return (float) Math.max(0, Math.abs(shellAt - faceAt));
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
            coupled = stream.readShort();
            ox = stream.readByte();
            oy = stream.readByte();
            oz = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
