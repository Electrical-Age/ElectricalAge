package mods.eln.node.voxel;

import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A pressure-relief valve with a whistle (Baughn): a tap on a pipe, its outlet pointing out of the pipe's side. Shut
 * below its set pressure, wide open 5% above it (the second opinion: no leak at the set pressure; a 5% band damps
 * chatter). What it lets out whistles if it's steam and sputters if it's water, and scalds a short way past the
 * outlet. An empty-hand right-click steps the set pressure through PRESETS.
 */
public final class SafetyValvePart extends VoxelPart {
    public static final String KIND = "safety_valve";
    /** Set pressures to step through (bar). */
    public static final double[] PRESETS = {2, 5, 8, 10, 15, 20, 30, 50, 75, 100, 150};
    /** Wide open, it passes this (m^3): about an iron pipe's bore. */
    static final double VENT_K = 2e-11;
    public final int x, y, z, outlet, bore;
    public double setPa;

    public SafetyValvePart(int x, int y, int z, int outlet, int bore, double setPa) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.outlet = outlet;
        this.bore = bore;
        this.setPa = setPa;
    }

    @Override
    public String kind() {
        return KIND;
    }

    /** Opening (0..1) at pressure P. */
    public double opening(double P) {
        return Math.max(0, Math.min(1, (P / setPa - 1) / 0.05));
    }

    /** The body: a short stack 4 voxels across standing on the pipe, towards the outlet. */
    @Override
    public List<int[]> boxes() {
        int h = bore / 2;
        int[] b = switch (outlet) {
            case 0 -> new int[]{x - 2, y - h - 4, z - 2, x + 2, y - h, z + 2};
            case 1 -> new int[]{x - 2, y + h, z - 2, x + 2, y + h + 4, z + 2};
            case 2 -> new int[]{x - 2, y - 2, z - h - 4, x + 2, y + 2, z - h};
            case 3 -> new int[]{x - 2, y - 2, z + h, x + 2, y + 2, z + h + 4};
            case 4 -> new int[]{x - h - 4, y - 2, z - 2, x - h, y + 2, z + 2};
            default -> new int[]{x + h, y - 2, z - 2, x + h + 4, y + 2, z + 2};
        };
        return List.of(ValvePart.clamp(b));
    }

    @Override
    public void write(CompoundTag t) {
        t.putIntArray("at", new int[]{x, y, z, outlet, bore});
        t.putDouble("set", setPa);
    }

    static SafetyValvePart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 1, 4});
        return new SafetyValvePart(a[0], a[1], a[2], a[3], a[4], t.getDoubleOr("set", 10e5));
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(outlet);
        out.writeByte(bore);
        out.writeFloat((float) setPa);
    }

    static SafetyValvePart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), o = in.readByte(), b = in.readByte();
        return new SafetyValvePart(x, y, z, o, b, in.readFloat());
    }
}
