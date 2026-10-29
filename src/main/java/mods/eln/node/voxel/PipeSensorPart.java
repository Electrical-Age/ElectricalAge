package mods.eln.node.voxel;

import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A pipe sensor (Baughn 2026-10-27): a dial on a short stem, fitted on a straight stretch of pipe like the safety valve,
 * reading the pressure (absolute) or the temperature of the run it sits on. Its reading goes out as a 0-50 V signal (0 at
 * zero, 50 V at full scale) on any signal cable laid into its block, as a control valve takes its signal
 * (VoxelNode.connectSensors). An empty-hand right-click steps its full scale through {@link #PRESSURE_BAR},
 * {@link #TEMPERATURE_C} or {@link #WETNESS_PCT}. The steam quality sensor (Baughn 2026-10-29, the shaft redo: a turbine's
 * blades die of wet steam) reads the run's wetness: the share of its water that is liquid, in percent.
 */
public final class PipeSensorPart extends VoxelPart {
    public static final String KIND = "pipe_sensor";
    public static final int PRESSURE = 0, TEMPERATURE = 1, QUALITY = 2;
    /** Full scales to step through: bar (absolute) and degrees C. */
    public static final double[] PRESSURE_BAR = {2, 5, 10, 15, 25, 50, 100}, TEMPERATURE_C = {100, 150, 200, 300, 500},
        WETNESS_PCT = {5, 10, 25, 50, 100};
    public static final double DEFAULT_BAR = 25, DEFAULT_C = 200, DEFAULT_PCT = 10;

    /** The default full scale for a kind of sensor. */
    public static double defaultScale(int sensing) {
        return sensing == PRESSURE ? DEFAULT_BAR : sensing == TEMPERATURE ? DEFAULT_C : DEFAULT_PCT;
    }
    public final int x, y, z, outlet, bore, sensing;
    /** Full scale: bar or degrees C. */
    public double scale;
    /** Server: the signal out (in the simulation while the block is). */
    public mods.eln.sim.nbt.NbtElectricalGateOutput out;
    public mods.eln.sim.nbt.NbtElectricalGateOutputProcess outProcess;
    /** The last reading (bar or C), and its share of full scale as last published (the dial's colour). */
    public double reading, shown;

    public PipeSensorPart(int x, int y, int z, int outlet, int bore, int sensing, double scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.outlet = outlet;
        this.bore = bore;
        this.sensing = sensing;
        this.scale = scale;
        out = new mods.eln.sim.nbt.NbtElectricalGateOutput("sensorOut");
        outProcess = new mods.eln.sim.nbt.NbtElectricalGateOutputProcess("sensorOutProcess", out);
    }

    @Override
    public String kind() {
        return KIND;
    }

    public String name() {
        return sensing == PRESSURE ? "Pressure sensor" : sensing == TEMPERATURE ? "Temperature sensor" : "Steam quality sensor";
    }

    public String unit() {
        return sensing == PRESSURE ? "bar" : sensing == TEMPERATURE ? "°C" : "% wet";
    }

    /** Read the run under it: absolute pressure (bar), temperature (C) or wetness (% of its water that is liquid; 0 with
     *  no water); sets the output. */
    public void read(mods.eln.sim.fluid.FluidNode n) {
        reading = sensing == PRESSURE ? n.P() / 1e5 : sensing == TEMPERATURE ? n.T() - mods.eln.sim.fluid.Water.T0
            : n.mWater > 1e-12 ? 100 * Math.max(0, Math.min(1, n.mLiquid() / n.mWater)) : 0;
        if (outProcess != null) outProcess.setOutputNormalized(reading / scale);
    }

    /** Share of full scale (0..1). */
    public double share() {
        return Math.max(0, Math.min(1, reading / scale));
    }

    /** The next full scale up (wrapping). */
    public double nextScale() {
        double[] s = sensing == PRESSURE ? PRESSURE_BAR : sensing == TEMPERATURE ? TEMPERATURE_C : WETNESS_PCT;
        for (double v : s) if (v > scale + 1e-6) return v;
        return s[0];
    }

    /** The stem and dial: a stack 4 voxels across, 3 out from the pipe, towards the outlet. */
    @Override
    public List<int[]> boxes() {
        int h = bore / 2;
        int[] b = switch (outlet) {
            case 0 -> new int[]{x - 2, y - h - 3, z - 2, x + 2, y - h, z + 2};
            case 1 -> new int[]{x - 2, y + h, z - 2, x + 2, y + h + 3, z + 2};
            case 2 -> new int[]{x - 2, y - 2, z - h - 3, x + 2, y + 2, z - h};
            case 3 -> new int[]{x - 2, y - 2, z + h, x + 2, y + 2, z + h + 3};
            case 4 -> new int[]{x - h - 3, y - 2, z - 2, x - h, y + 2, z + 2};
            default -> new int[]{x + h, y - 2, z - 2, x + h + 3, y + 2, z + 2};
        };
        return List.of(ValvePart.clamp(b));
    }

    @Override
    public void write(CompoundTag t) {
        t.putIntArray("at", new int[]{x, y, z, outlet, bore, sensing});
        t.putDouble("scale", scale);
        if (outProcess != null) outProcess.writeToNBT(t, "");
    }

    static PipeSensorPart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 1, 4, PRESSURE});
        int sensing = a.length > 5 ? a[5] : PRESSURE;
        var p = new PipeSensorPart(a[0], a[1], a[2], a[3], a[4], sensing, t.getDoubleOr("scale", defaultScale(sensing)));
        p.outProcess.readFromNBT(t, "");
        return p;
    }

    @Override
    public void serialize(DataOutputStream o) throws IOException {
        o.writeByte(x);
        o.writeByte(y);
        o.writeByte(z);
        o.writeByte(outlet | sensing << 4);
        o.writeByte(bore);
        o.writeFloat((float) scale);
        o.writeByte((int) Math.round(share() * 255));
    }

    static PipeSensorPart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), a = in.readByte(), b = in.readByte();
        var p = new PipeSensorPart(x, y, z, a & 15, b, (a >> 4) & 3, in.readFloat());
        p.out = null;   // (client: the published reading only)
        p.outProcess = null;
        p.shown = (in.readByte() & 0xff) / 255.0;
        p.reading = p.shown * p.scale;
        return p;
    }
}
