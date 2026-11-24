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
 *
 * A breather (refinery M2, Baughn 2026-11-24: a third item on this part) is a purge valve's float and a vacuum breaker
 * at the air's pressure: gas out and air in past FluidDevice.Vent.BREATHE_PA either side of it (as FlareStackElement's tip),
 * so a receiver under a still's condenser fills and drains at 1 atm. No setting, no whistle, no scalding jet. A purge
 * valve there pulls the still under vacuum and holds the product in the condenser (its 1.1 bar deadband would stall
 * gravity drainage too). Natural gas goes out of it as any gas does (Baughn: "a good way to lose natural gas").
 */
public final class SafetyValvePart extends VoxelPart {
    public static final String KIND = "safety_valve";
    /** The screen's slider range (bar). */
    public static final double MIN_BAR = 1.1, MAX_BAR = 150, PURGE_PA = 1.1e5;
    /** What it is: SAFETY (relief, any phase), PURGE (gas only, set pressure), BREATHER (gas out and air in at 1 atm). */
    public static final int SAFETY = 0, PURGE = 1, BREATHER = 2;
    /** Wide open, it passes this (m^3): about an iron pipe's bore. */
    static final double VENT_K = 2e-11;
    public final int x, y, z, outlet, bore;
    public double setPa;
    public final int mode;
    /** A purge valve (gas only, never liquid); a breather. */
    public final boolean purge, breather;
    /**
     * Baughn 2026-10-27: it pulsed, popping open and shut every second or so. A boiler's pressure jitters by several
     * percent from tick to tick, more than the old 5% band, so the valve flapped between shut and wide open. It now acts
     * on the pressure averaged over AVG_TAU seconds (pAvg), and opens over a wider band, BAND above its set pressure.
     * Server only, not saved: pAvg starts from the first reading. Only purge valves still work this way; a safety valve
     * is a relief regulator in the solve now (relieve, below).
     */
    public double lift, pAvg = Double.NaN;
    /**
     * What it has let out to the air since loaded (kg by species id; null until it has), and the air it took in (kg; a
     * breather). Baughn 2026-11-24: a breather vents into the room it stands in; a later room-air model reads these.
     * Server only, not saved.
     */
    public double[] ventedKg;
    public double airInKg;
    /**
     * Seconds left showing that it vented or drew air (HOLD_S after the last step it did): a breather's flow comes and
     * goes from step to step, and its Jade line flickered between "Venting gas" and "Idle" (AdventurAgent).
     */
    public double ventShown, airShown;
    public static final double HOLD_S = 1;

    /** Book one step's vent edge (VoxelNode.afterFluidStep): what went out by species, or the air that came in. */
    public void book(mods.eln.sim.fluid.FluidEdge e, boolean inward, double dt) {
        if (inward) {
            airInKg += Math.max(0, e.flow) * dt;
            airShown = e.flow > 0 ? HOLD_S : Math.max(0, airShown - dt);
            return;
        }
        ventShown = e.flow > 0 ? HOLD_S : Math.max(0, ventShown - dt);
        if (e.fSpecies == null || e.flow <= 0) return;
        if (ventedKg == null || ventedKg.length < e.fSpecies.length) ventedKg = ventedKg == null ? new double[e.fSpecies.length] : java.util.Arrays.copyOf(ventedKg, e.fSpecies.length);
        for (int i = 0; i < e.fSpecies.length; i++) ventedKg[i] += Math.max(0, e.fSpecies[i]) * dt;
    }
    public static final double AVG_TAU = 2.0, BAND = 0.10;

    public SafetyValvePart(int x, int y, int z, int outlet, int bore, double setPa) {
        this(x, y, z, outlet, bore, setPa, false);
    }

    public SafetyValvePart(int x, int y, int z, int outlet, int bore, double setPa, boolean purge) {
        this(x, y, z, outlet, bore, setPa, purge ? PURGE : SAFETY);
    }

    public SafetyValvePart(int x, int y, int z, int outlet, int bore, double setPa, int mode) {
        this.mode = mode;
        this.purge = mode == PURGE;
        this.breather = mode == BREATHER;
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

    /**
     * A safety valve (not a purge valve) is the fluid core's relief regulator (Baughn 2026-11-12: with the new solver the
     * averaged pressure only lagged, and the valve cycled like a siren). Inside the solve it passes RELIEF_K times the
     * pressure over its set point, so a boiler settles a little over the setting with a steady flow and a steady
     * whistle. The lift is only shown and heard: it follows the pressure under it, without averaging. Returns the set
     * pressure for the vent.
     */
    public double relieve(double P) {
        pAvg = P;
        lift = opening(P);
        return setPa;
    }

    /** A safety valve's conductance over its set point: the wide-open vent's flow BAND over it (m^3). */
    static final double RELIEF_K = VENT_K / BAND;

    /** A breather's two openings (0..1) at pressure P: {gas out, air in} (FluidDevice.Vent.breatheOut/In). */
    public static double[] breathe(double P) {
        double dp = P - mods.eln.sim.fluid.Water.P_ATM;
        return new double[]{mods.eln.sim.fluid.FluidDevice.Vent.breatheOut(dp), mods.eln.sim.fluid.FluidDevice.Vent.breatheIn(dp)};
    }

    /** Where on the run under it it draws: a valve on top lets out what's at the top, one on a side what's half way up. */
    public double draw() {
        return outlet == 1 ? 1 : outlet == 0 ? 0 : 0.5;
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
        if (breather) t.putBoolean("breather", true);   // (safety and purge valves write what they always did)
    }

    static SafetyValvePart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 1, 4});
        return new SafetyValvePart(a[0], a[1], a[2], a[3], t.getBooleanOr("g", false) ? a[4] : PipePart.oldBore(a[4]), t.getDoubleOr("set", 10e5),
            t.getBooleanOr("breather", false) ? BREATHER : t.getBooleanOr("purge", false) ? PURGE : SAFETY);
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(outlet);
        out.writeByte(bore);
        out.writeFloat((float) setPa);
        out.writeByte(mode);
    }

    static SafetyValvePart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), o = in.readByte(), b = in.readByte();
        float set = in.readFloat();
        return new SafetyValvePart(x, y, z, o, b, set, (int) in.readByte());
    }
}
