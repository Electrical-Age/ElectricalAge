package mods.eln.node.voxel;

import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A pressure-relief valve with a whistle (Baughn): a tap on a pipe, its outlet pointing out of the pipe's side. Shut
 * below its set pressure, wide open BAND above it (the second opinion: no leak at the set pressure), by the pressure
 * averaged over AVG_TAU (below). What it lets out whistles if it's steam and sputters if it's water, and scalds a short way past the
 * outlet. An empty-hand right-click opens its screen, a slider for the set pressure (Baughn
 * 2026-10-27; MIN_BAR..MAX_BAR).
 *
 * A purge valve is the same tap with a float (Baughn 2026-11-10: "a valve that lets out only gas, never liquid ... it
 * goes by phase, not species; it'll let out steam"): it vents only the gas of the run under it, shuts as the liquid
 * reaches its outlet, and lets nothing back in. Its set pressure starts at PURGE_PA, just over the air's, so it bleeds a
 * tank's gas (a crude tank's natural gas, the air of a filling run) by default.
 */
public final class SafetyValvePart extends VoxelPart {
    public static final String KIND = "safety_valve";
    /** The screen's slider range (bar). */
    public static final double MIN_BAR = 1.1, MAX_BAR = 150, PURGE_PA = 1.1e5;
    /** Wide open, it passes this (m^3): about an iron pipe's bore. */
    static final double VENT_K = 2e-11;
    public final int x, y, z, outlet, bore;
    public double setPa;
    /** A purge valve (gas only, never liquid). */
    public final boolean purge;
    /**
     * Baughn 2026-10-27: it pulsed, popping open and shut every second or so. A boiler's pressure jitters by several
     * percent from tick to tick, more than the old 5% band, so the valve flapped between shut and wide open. It now acts
     * on the pressure averaged over AVG_TAU seconds (pAvg), and opens over a wider band, BAND above its set pressure.
     * Server only, not saved: pAvg starts from the first reading.
     */
    public double lift, pAvg = Double.NaN;
    public static final double AVG_TAU = 2.0, BAND = 0.10;

    public SafetyValvePart(int x, int y, int z, int outlet, int bore, double setPa) {
        this(x, y, z, outlet, bore, setPa, false);
    }

    public SafetyValvePart(int x, int y, int z, int outlet, int bore, double setPa, boolean purge) {
        this.purge = purge;
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

    /** Take in the pressure P under it for dt seconds; returns the lift (by the averaged pressure). */
    public double follow(double P, double dt) {
        pAvg = Double.isNaN(pAvg) ? P : pAvg + (P - pAvg) * Math.min(1, dt / AVG_TAU);
        lift = opening(pAvg);
        return lift;
    }

    /** Opening (0..1) at pressure P. */
    public double opening(double P) {
        return Math.max(0, Math.min(1, (P / setPa - 1) / BAND));
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
        t.putBoolean("g", true);   // (grid widths: PipePart.GRID)
        t.putDouble("set", setPa);
        if (purge) t.putBoolean("purge", true);
    }

    static SafetyValvePart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 1, 4});
        return new SafetyValvePart(a[0], a[1], a[2], a[3], t.getBooleanOr("g", false) ? a[4] : PipePart.oldBore(a[4]), t.getDoubleOr("set", 10e5),
            t.getBooleanOr("purge", false));
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(outlet);
        out.writeByte(bore);
        out.writeFloat((float) setPa);
        out.writeBoolean(purge);
    }

    static SafetyValvePart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), o = in.readByte(), b = in.readByte();
        float set = in.readFloat();
        return new SafetyValvePart(x, y, z, o, b, set, in.readBoolean());
    }
}
