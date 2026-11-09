package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The tank: its vessel, its sight glass (TankElement's fill and steam, published) and its ports' flanges. */
public class TankRender extends TransparentNodeElementRender {
    final TankDescriptor desc;
    public float fill;
    /** Its liquid layers when it holds more than one (each one's top and colour; empty: one liquid, drawn as before). */
    float[] layerTops = new float[0];
    int[] layerColours = new int[0];
    /** Whether its gas is mostly steam (the sight glass shows it over the water). */
    public boolean steam;
    /** Whether it draws the tank (a lone tank, or a box's lowest corner), and the box's size. */
    public boolean draws = true;
    public int sx = 1, sy = 1, sz = 1;
    /** Its ports with a pipe on them (TankElement.coupled): a flange each; its place in the box (0 alone). */
    public int coupled, ox, oy, oz;
    /** The shell's radius (px; mods.eln.cad.models.Tank R), its top (px), and its flanges' depth (px). */
    public static final float SHELL_R = 5.5f, SHELL_TOP = 15f, DISC = 0.75f;
    /** The shell's flat top's radius and its rim's height (px), a nozzle's radius, how far it sinks into the shell (px). */
    public static final double TOP_FLAT_R = 4.0, TOP_RIM = 14.5, NOZZLE_R = 3.0, SINK = 0.5;
    /** The 14-sided shell's flats lie this much inside its radius. */
    static final double FLATS = Math.cos(Math.PI / 14);

    public TankRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (TankDescriptor) d;
    }

    @Override
    public void draw() {
        // its flanges, where pipes meet it, on this block's faces (Baughn 2026-11-03: a lone tank too, not all seven)
        if (coupled != 0) {
            for (int face = 2; face <= 5; face++)
                for (int hi = 0; hi < 2; hi++) {
                    if ((coupled & 1 << (2 * face + hi)) == 0) continue;
                    mods.eln.gl.GL11.glPushMatrix();
                    mods.eln.misc.Direction.fromEnumFacing(net.minecraft.core.Direction.from3DDataValue(face)).glRotateXnRef();
                    desc.drawPart(hi == 0 ? "disc_low" : "disc_high");
                    // a nozzle from behind the flange in past the shell, which a box scales away from its faces
                    // (AdventurAgent 2026-10-29: on a 2 x 2 x 2 iron tank a pipe ended a third of a block short of it)
                    float[] nz = sideNozzle(face);
                    float len = nz[0] - DISC;
                    if (len > 0.05f) {
                        mods.eln.gl.GL11.glTranslatef(-0.5f + DISC / 16f, 0, 0);
                        if (nz[1] != 0) mods.eln.gl.GL11.glRotatef(nz[1], 0, 1, 0);
                        mods.eln.gl.GL11.glScalef(len, 1, 1);
                        mods.eln.gl.GL11.glTranslatef(0.5f, 0, 0);
                        desc.drawPart(hi == 0 ? "nozzle_low" : "nozzle_high");
                    }
                    mods.eln.gl.GL11.glPopMatrix();
                }
            if ((coupled & 1 << 12) != 0) {
                desc.drawPart("disc_top");
                float len = topNozzle() - DISC;
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
            desc.drawShell();
            desc.drawGlass(fill, steam, layerTops, layerColours);
            return;
        }
        // a box (TankGroup): one shell and sight glass over all of it, from its lowest corner (Baughn 2026-10-29)
        mods.eln.gl.GL11.glTranslatef((sx - 1) / 2f, (sy - 1) / 2f, (sz - 1) / 2f);
        front.glRotateXnRef();
        boolean alongX = front == mods.eln.misc.Direction.XN || front == mods.eln.misc.Direction.XP;
        float across = alongX ? sx : sz;
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glScalef(across, sy, alongX ? sz : sx);
        desc.drawShell();
        mods.eln.gl.GL11.glPopMatrix();
        // the sight glass on the scaled shell's front, the box's height but its own width (Baughn 2026-11-03)
        mods.eln.gl.GL11.glTranslatef(-(across - 1) * SHELL_R / 16f, 0, 0);
        mods.eln.gl.GL11.glScalef(1, sy, 1);
        desc.drawGlass(fill, steam, layerTops, layerColours);
    }

    /**
     * From this block's FACE (3D value, a side) to just inside the box's shell (px), all round the nozzle's bore (Baughn
     * 2026-11-03: a nozzle cut at the shell's centre line left a gap at its sides, the shell curving away there). The
     * shell is the lone tank's scaled to the box, an upright 14-sided prism of radius SHELL_R x the box's size across each
     * axis, centred in it (taken at its flats); the port is at the middle of this block's face. A port near a box's
     * corner, whose bore reaches past the shell's side, runs in as far as the shell is there.
     */
    public float nozzle(int face) {
        boolean alongX = face == 4 || face == 5;   // (west/east faces: the nozzle runs along x)
        int o = alongX ? ox : oz, n = alongX ? sx : sz, oAcross = alongX ? oz : ox, nAcross = alongX ? sz : sx;
        double across = (oAcross * 16 + 8) - nAcross * 8.0, half = SHELL_R * FLATS * nAcross;
        double edge = Math.min(Math.abs(across) + NOZZLE_R, 0.95 * half);
        double reach = SHELL_R * FLATS * n * Math.sqrt(1 - edge * edge / (half * half));
        boolean high = face == 3 || face == 5;   // (south/east: the face at the block's far side)
        double faceAt = high ? (o + 1) * 16 : o * 16, shellAt = high ? n * 8.0 + reach : n * 8.0 - reach;
        return (float) (Math.max(0, Math.abs(shellAt - faceAt)) + SINK);
    }

    /**
     * A side nozzle: {its length from this block's FACE (px), its turn about y (degrees, the model's frame turned to the
     * face; positive turns its +x toward -z)}. Straight in (nozzle) where its bore meets the shell; a port beside the
     * shell (near a box's corner, where the round shell leaves the block mostly empty) aims at the nearest point of the
     * shell instead (Baughn 2026-11-09: a pipe on a corner flange didn't meet the tank), long enough that its bore's
     * edge reaches in past the shell's curve.
     */
    public float[] sideNozzle(int face) {
        boolean alongX = face == 4 || face == 5;
        int o = alongX ? ox : oz, n = alongX ? sx : sz, oAcross = alongX ? oz : ox, nAcross = alongX ? sz : sx;
        double au = SHELL_R * FLATS * n, av = SHELL_R * FLATS * nAcross;
        double v = oAcross * 16 + 8;
        if (Math.abs(v - nAcross * 8.0) + NOZZLE_R <= 0.95 * av) return new float[]{nozzle(face), 0};
        boolean high = face == 3 || face == 5;
        double faceAt = high ? (o + 1) * 16 : o * 16, u = high ? faceAt - DISC : faceAt + DISC;
        // (the nearest point of the shell's flats' ellipse to the flange's centre)
        double best = Double.MAX_VALUE, bu = 0, bv = 0;
        for (int k = 0; k < 1440; k++) {
            double t = 2 * Math.PI * k / 1440, pu = n * 8.0 + au * Math.cos(t), pv = nAcross * 8.0 + av * Math.sin(t);
            double d = Math.hypot(pu - u, pv - v);
            if (d < best) {
                best = d;
                bu = pu;
                bv = pv;
            }
        }
        double in = Math.abs(bu - u), side = bv - v;
        double r = Math.min(au, av), sag = r > NOZZLE_R ? r - Math.sqrt(r * r - NOZZLE_R * NOZZLE_R) : NOZZLE_R;
        // (the across axis in the face's model frame: model z is world x on the south face, -x north, z west, -z east)
        double sideModel = switch (face) {
            case 2, 5 -> -side;
            default -> side;
        };
        return new float[]{(float) (DISC + best + sag + SINK), (float) Math.toDegrees(Math.atan2(-sideModel, in))};
    }

    /**
     * From this block's top down into the box's shell (px), all round the bore: the shell's top is flat out to TOP_FLAT_R
     * of its radius and slopes down to its rim at TOP_RIM (scaled to the box), and a top port off the flat reaches the
     * slope (past the shell's side: its rim).
     */
    public float topNozzle() {
        double dx = (ox * 16 + 8) - sx * 8.0, dz = (oz * 16 + 8) - sz * 8.0;
        // (how far out the bore's far edge is, as a share of the shell's radius there)
        double rho = Math.hypot(dx / (SHELL_R * sx), dz / (SHELL_R * sz)) + NOZZLE_R / (SHELL_R * Math.min(sx, sz));
        double flat = TOP_FLAT_R / SHELL_R;
        double top = rho <= flat ? SHELL_TOP : rho >= 1 ? TOP_RIM : SHELL_TOP - (SHELL_TOP - TOP_RIM) * (rho - flat) / (1 - flat);
        return (float) (16 * sy - top * sy + SINK);
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
            steam = stream.readBoolean();
            int n = stream.readByte();
            layerTops = new float[n];
            layerColours = new int[n];
            for (int l = 0; l < n; l++) {
                layerTops[l] = stream.readFloat();
                layerColours[l] = stream.readInt();
            }
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
