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
 * <li>{@link #CONTROL}: opened by a signal (0-50 V): a signal cable laid into its block (on the floor under it)
 * connects (VoxelNode.connectValves). A turbine's throttle.</li>
 * <li>{@link #THROTTLE}: a handwheel set anywhere from shut to open on a screen with a slider (right-click).</li>
 * <li>{@link #PRESSURE}: the pressure valve (Baughn 2026-10-29, the shaft redo): one way, in the direction of its
 * arrow; RELIEF holds the pressure before it at its set point (opens as that rises over it: a back-pressure or dump
 * valve, e.g. a turbine bypass), REDUCING holds the pressure after it at its set point (shuts as that rises to it).
 * Throttling is isenthalpic (the network carries enthalpy). Set point on a screen, or a 0-50 V signal from a signal
 * cable laid into its block (0 to MAX_BAR) when one is there. It works as a pressure offset on its link (boost), which
 * the network solves within the step: no overshoot, no flapping.</li>
 * </ul>
 */
public final class ValvePart extends VoxelPart {
    public static final String KIND = "valve";
    public static final int ON_OFF = 0, CONTROL = 1, THROTTLE = 2, PRESSURE = 3;
    /** PRESSURE: its modes, the set point's range (bar), its band and averaging time (s). */
    public static final int RELIEF = 0, REDUCING = 1;
    public static final double MIN_BAR = 0.1, MAX_BAR = 150, BAND = 0.10;
    public final int x, y, z, axis, bore;
    public final int mode;
    public boolean open = true;
    /** THROTTLE: its setting (0 shut .. 1 open). */
    public double setting = 1;
    /** CONTROL: its signal input (server; in the simulation while the block is), and its opening from it. */
    public mods.eln.sim.nbt.NbtElectricalGateInput signal;
    /** CONTROL (client): the opening last published, for the drawing. */
    public double shown;
    /** PRESSURE: mode, set point (Pa), flow direction along its axis (+1 or -1), and its opening now; server: the
     *  averaged pressures before and after it. */
    public int pmode = RELIEF;
    public double setPa = 10e5, lift;
    public int flow = 1;

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
        if (mode == CONTROL || mode == PRESSURE) signal = new mods.eln.sim.nbt.NbtElectricalGateInput("valveSignal");
    }

    /** How far open (0..1) it is now. */
    public double opening() {
        return switch (mode) {
            case CONTROL -> signal == null ? shown : Math.max(0, Math.min(1, signal.getU() / mods.eln.Eln.SVU));
            case THROTTLE -> Math.max(0, Math.min(1, setting));
            case PRESSURE -> signal == null ? shown : lift;
            default -> open ? 1 : 0;
        };
    }


    /** PRESSURE: is a signal cable connected (it then sets the set point)? */
    public boolean signalled() {
        return signal != null && !signal.getConnectedComponents().isEmpty();
    }

    /** PRESSURE: its set point now (Pa): the screen's, or the signal's (0-50 V: 0 to MAX_BAR). */
    public double setPoint() {
        return signalled() ? Math.max(MIN_BAR, signal.getNormalized() * MAX_BAR) * 1e5 : setPa;
    }

    /**
     * PRESSURE: the pressure it takes off the drive across it (Pa, <= 0: its link's boost), with the pressures before and
     * after it now. Reducing: the drive becomes set - after, so it passes until the pressure after it reaches its set
     * point; relief: before - set, so it passes while the pressure before it is over its set point. The network solves
     * that within the step (a plug moving on last step's pressures overshot a small vessel by bars). Also sets lift, how
     * far open it is (for Jade and the drawing): its drive over BAND of the set point.
     */
    public double boost(double before, double after) {
        double set = setPoint();
        double b = pmode == RELIEF ? Math.min(0, after - set) : Math.min(0, set - before);
        lift = Math.max(0, Math.min(1, (before + b - after) / (BAND * set)));
        return b;
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
        int a = mode == CONTROL || mode == PRESSURE ? 2 : r;   // (the control and pressure valves: a square actuator, not a handwheel)
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
        if (mode == PRESSURE) {
            t.putInt("pmode", pmode);
            t.putDouble("set", setPa);
            t.putInt("flow", flow);
        }
        if (signal != null) signal.writeToNBT(t, "");
    }

    static ValvePart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 0, 4});
        ValvePart v = new ValvePart(a[0], a[1], a[2], a[3], a[4], t.getIntOr("mode", ON_OFF));
        v.open = t.getBooleanOr("open", true);
        v.setting = t.getDoubleOr("setting", 1);
        v.pmode = t.getIntOr("pmode", RELIEF);
        v.setPa = t.getDoubleOr("set", 10e5);
        v.flow = t.getIntOr("flow", 1) < 0 ? -1 : 1;
        if (v.signal != null) v.signal.readFromNBT(t, "");
        return v;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(axis | (open ? 0x10 : 0) | (flow < 0 ? 0x20 : 0) | pmode << 6);
        out.writeByte(mode);
        out.writeByte(bore);
        if (mode != ON_OFF) out.writeByte((int) Math.round(opening() * 255));
        if (mode == PRESSURE) out.writeFloat((float) setPa);
    }

    static ValvePart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), a = in.readByte(), mode = in.readByte(), b = in.readByte();
        ValvePart v = new ValvePart(x, y, z, a & 3, b, mode);
        v.flow = (a & 0x20) != 0 ? -1 : 1;
        v.pmode = (a >> 6) & 3;
        v.signal = null;   // (client: the published opening)
        v.open = (a & 0x10) != 0;
        if (mode != ON_OFF) v.shown = v.setting = (in.readByte() & 0xff) / 255.0;
        if (mode == PRESSURE) v.setPa = in.readFloat();
        return v;
    }
}
