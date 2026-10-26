package mods.eln.node.voxel;

import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A valve in a pipe (notes/pipes/DESIGN.md section 5: valves scale an edge's conductance): it sits at a point inside
 * the block, between two pipes that end there (the pipe it was fitted to, cut in two). Open, it passes what a short
 * piece of pipe would; shut, nothing. Three kinds (Baughn 2026-10-26: two more pipe modules):
 * <ul>
 * <li>{@link #ON_OFF}: a right-click turns it open or shut.</li>
 * <li>{@link #CONTROL}: opened by a signal (0-50 V) on its actuator: a signal cable touching the actuator's top
 * ({@link #contact}) connects. A turbine's throttle.</li>
 * <li>{@link #THROTTLE}: a handwheel set anywhere from shut to open on a screen with a slider (right-click).</li>
 * </ul>
 */
public final class ValvePart extends VoxelPart {
    public static final String KIND = "valve";
    public static final int ON_OFF = 0, CONTROL = 1, THROTTLE = 2;
    public final int x, y, z, axis, bore;
    public final int mode;
    public boolean open = true;
    /** THROTTLE: its setting (0 shut .. 1 open). */
    public double setting = 1;
    /** CONTROL: its signal input (server; in the simulation while the block is), and its opening from it. */
    public mods.eln.sim.nbt.NbtElectricalGateInput signal;
    /** CONTROL (client): the opening last published, for the drawing. */
    public double shown;

    public ValvePart(int x, int y, int z, int axis, int bore) {
        this(x, y, z, axis, bore, ON_OFF);
    }

    public ValvePart(int x, int y, int z, int axis, int bore, int mode) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.axis = axis;
        this.bore = bore;
        this.mode = mode;
        if (mode == CONTROL) signal = new mods.eln.sim.nbt.NbtElectricalGateInput("valveSignal");
    }

    /** How far open (0..1) it is now. */
    public double opening() {
        return switch (mode) {
            case CONTROL -> signal == null ? shown : Math.max(0, Math.min(1, signal.getU() / mods.eln.Eln.SVU));
            case THROTTLE -> Math.max(0, Math.min(1, setting));
            default -> open ? 1 : 0;
        };
    }

    /**
     * CONTROL: the voxels just past the actuator's top (the end of its stem) that a touching signal cable takes, as a
     * device's pin (VoxelNode.connectValves).
     */
    public int[] contact() {
        int h = bore / 2 + 1, s = reach();
        return axis == 1 ? clamp(new int[]{x + s + 1, y - 1, z - 1, x + s + 2, y + 1, z + 1})
            : clamp(new int[]{x - 1, y + s + 1, z - 1, x + 1, y + s + 2, z + 1});
    }

    @Override
    public String kind() {
        return KIND;
    }

    /** The handwheel's radius (voxels): wider than the pipe, so it reads as a valve (Baughn). */
    public int wheel() {
        return bore / 2 + 3;
    }

    /** How far the wheel sits from the pipe's axis (voxels). */
    public int reach() {
        return bore / 2 + 3;
    }

    /** The body (a cube a little wider than the bore), the stem, and the handwheel's disc at its end (on top, or on the
     *  side for a vertical pipe). */
    @Override
    public List<int[]> boxes() {
        int h = bore / 2 + 1, r = wheel(), s = reach();
        List<int[]> out = new ArrayList<>();
        out.add(clamp(new int[]{x - h, y - h, z - h, x + h, y + h, z + h}));
        int a = mode == CONTROL ? 2 : r;   // (the control valve: a square actuator, not a handwheel)
        if (axis == 1) {
            out.add(clamp(new int[]{x + h, y - 1, z - 1, x + s, y + 1, z + 1}));
            out.add(clamp(new int[]{x + s, y - a, z - a, x + s + 1, y + a, z + a}));
        } else {
            out.add(clamp(new int[]{x - 1, y + h, z - 1, x + 1, y + s, z + 1}));
            out.add(clamp(new int[]{x - a, y + s, z - a, x + a, y + s + 1, z + a}));
        }
        return out;
    }

    static int[] clamp(int[] b) {
        for (int i = 0; i < 6; i++) b[i] = Math.max(0, Math.min(16, b[i]));
        return b;
    }

    @Override
    public void write(CompoundTag t) {
        t.putIntArray("at", new int[]{x, y, z, axis, bore});
        t.putBoolean("open", open);
        if (mode != ON_OFF) t.putInt("mode", mode);
        if (mode == THROTTLE) t.putDouble("setting", setting);
        if (signal != null) signal.writeToNBT(t, "");
    }

    static ValvePart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 0, 4});
        ValvePart v = new ValvePart(a[0], a[1], a[2], a[3], a[4], t.getIntOr("mode", ON_OFF));
        v.open = t.getBooleanOr("open", true);
        v.setting = t.getDoubleOr("setting", 1);
        if (v.signal != null) v.signal.readFromNBT(t, "");
        return v;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(axis | (open ? 0x10 : 0) | mode << 5);
        out.writeByte(bore);
        if (mode != ON_OFF) out.writeByte((int) Math.round(opening() * 255));
    }

    static ValvePart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), a = in.readByte(), b = in.readByte();
        int mode = (a >> 5) & 3;
        ValvePart v = new ValvePart(x, y, z, a & 3, b, mode);
        v.signal = null;   // (client: the published opening)
        v.open = (a & 0x10) != 0;
        if (mode != ON_OFF) v.shown = v.setting = (in.readByte() & 0xff) / 255.0;
        return v;
    }
}
