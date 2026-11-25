package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * A power cable's specification (Baughn 2026-11-06, Wires stage 3): its copper cross-section and its insulation colour.
 * Its insulation is its tier (CablePart.TIERS 1..4 and 7: 48 V, 230 V, 400 V, 11 kV and 1 kV; in voltage order
 * {@link #POWER_TIERS}). Signal, thermal and bus cables have none.
 * <p>
 * The rated current follows real building wire (2.5 mm²: 21 A); the resistance follows copper (0.0172 ohm mm²/m),
 * scaled by RESISTANCE_SCALE so the 48 V preset keeps the 1.12 Low Voltage Cable's 0.023 ohm a block. So higher
 * voltage presets lose a smaller share a block, as in life (Baughn's Q2). The insulation trips at 1.3 x its voltage, the
 * copper burns at 1.2 x its rated current, as every EA cable.
 */
public record CableSpec(int mm2, int colour) {
    /** Cross-sections, mm² (index: mm2). */
    public static final double[] MM2 = {0.5, 0.75, 1, 1.5, 2.5, 4, 6, 10, 16, 25, 35, 50, 70, 95};
    /**
     * Rated current of each, A: building wire's, with 1.5 and 2.5 mm² at exactly the 230 V and 48 V tiers' currents
     * (3 kW / 230 V, 1 kW / 48 V), so cables laid before specs are unchanged.
     */
    public static final double[] AMPS = {3, 6, 10, 3000.0 / 230, 1000.0 / 48, 28, 36, 50, 66, 88, 109, 133, 170, 205};
    /** Copper's resistivity, ohm mm²/m. */
    public static final double COPPER = 0.0172;
    /**
     * EA's cables are lossier than life: 2.5 mm² has the 1.12 Low Voltage Cable's resistance a block, its drop factor
     * 0.2/20 of 1 kW at 48 V: 0.01 x 48^2 / 1000 = 0.02304 ohm.
     */
    public static final double RESISTANCE_SCALE = 0.01 * 48 * 48 / 1000 / (COPPER / 2.5);

    /** Dye ids (net.minecraft.world.item.DyeColor, 0 white .. 15 black). */
    public static final int BROWN = 12, BLACK = 15, GRAY = 7, RED = 14;

    public CableSpec {
        mm2 = Math.max(0, Math.min(MM2.length - 1, mm2));
        colour = Math.max(0, Math.min(15, colour));
    }

    /** The power tiers in voltage order: 48 V, 230 V, 400 V, 1 kV, 11 kV. The 1 kV tier came last (Baughn 2026-11-25,
     *  for solar strings), so its index is 7: tiers are stored by index in places (kits, windings, packets). */
    public static final int[] POWER_TIERS = {1, 2, 3, CablePart.KV, 4};

    /** A power tier (insulation): 1 48 V, 2 230 V, 3 400 V, 7 1 kV, 4 11 kV. */
    public static boolean power(int tier) {
        return tier >= 1 && tier <= 4 || tier == CablePart.KV;
    }

    /** The power tier rank steps from tier (-1 a tier lower, +1 higher), kept within 48 V .. max (voltage order). */
    public static int stepTier(int tier, int steps, int max) {
        int r = Math.max(0, rank(tier)) + steps;
        return POWER_TIERS[Math.max(0, Math.min(rank(max) < 0 ? 0 : rank(max), r))];
    }

    /** A power tier at most max (voltage order); anything not a power tier becomes 48 V. */
    public static int clampTier(int tier, int max) {
        return stepTier(tier, 0, max);
    }

    /** A power tier's place in voltage order (0 for 48 V .. 4 for 11 kV); -1 for any other tier. Compare these, not
     *  tier indices. */
    public static int rank(int tier) {
        for (int i = 0; i < POWER_TIERS.length; i++) if (POWER_TIERS[i] == tier) return i;
        return -1;
    }

    /** The insulation's voltage of a power tier. */
    public static double volts(int tier) {
        return switch (tier) {
            case 1 -> Eln.LVU;
            case 2 -> Eln.MVU;
            case 3 -> Eln.HVU;
            case CablePart.KV -> Eln.KVU;
            default -> Eln.VVU;
        };
    }

    /**
     * Each power tier's preset (the hand-crafted kits, and every cable laid before specs; Baughn: 48 V brown 1 kW, 230 V
     * black 3 kW / 13 A; 400 V grey and 11 kV red, Baughn's Q3): 2.5 mm² brown, 1.5 mm² black, 1.5 mm² grey,
     * 0.5 mm² red; 1 kV 1.5 mm² as the 400 V hand kit (the wire machine makes thicker), its jacket a red and black
     * twist (Baughn), so its colour is fixed (forTier).
     */
    public static CableSpec preset(int tier) {
        return switch (tier) {
            case 1 -> new CableSpec(4, BROWN);
            case 2 -> new CableSpec(3, BLACK);
            case 3 -> new CableSpec(3, GRAY);
            case CablePart.KV -> new CableSpec(3, RED);
            default -> new CableSpec(0, RED);
        };
    }

    public double squareMillimetres() {
        return MM2[mm2];
    }

    public double amps() {
        return AMPS[mm2];
    }

    /** Resistance a block (both ways, as EA cables count it), ohms. */
    public double ohmsPerBlock() {
        return RESISTANCE_SCALE * COPPER / MM2[mm2];
    }

    /** Width along its face, voxels: by insulation, thicker copper wider (at most 4). */
    public int width(int tier) {
        int w = switch (tier) {
            case 3, CablePart.KV -> 3;
            case 4 -> 4;
            default -> 2;
        };
        if (MM2[mm2] >= 10) w++;
        if (MM2[mm2] >= 35) w++;
        return Math.min(4, w);
    }

    /** Height off its face, voxels. */
    public int height(int tier) {
        return tier == 4 || MM2[mm2] >= 16 ? 2 : 1;
    }

    /** A jacket drawn in its own colours (the 1 kV tier's red and black twist), not tinted by the dye. */
    public static boolean twisted(int tier) {
        return tier == CablePart.KV;
    }

    /** This spec as a cable of tier has it: a twisted jacket has no dye of its own (its colour is the preset's), so
     *  1 kV cables and kits differing only by an invisible colour are the same cable and stack. */
    public CableSpec forTier(int tier) {
        return twisted(tier) && colour != preset(tier).colour() ? new CableSpec(mm2, preset(tier).colour()) : this;
    }

    /** The jacket as players read it: the dye's name, or "red and black twist". */
    public static String jacketName(int tier, int colour) {
        return twisted(tier) ? "red and black twist" : net.minecraft.world.item.DyeColor.byId(colour).getName().replace('_', ' ');
    }

    /** "2.5 mm², 48V". */
    public String label(int tier) {
        return mm2Number(MM2[mm2]) + " mm², " + voltsLabel(tier);
    }

    /** A cross-section in mm² as text without the unit: "4", "2.5" (whole sizes without ".0"). */
    public static String mm2Number(double m) {
        return m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m);
    }

    /** A power tier's voltage for players, as the device names write it: "48V", "230V", "400V", "11kV". A tier is a name,
     *  not a reading (readings go through Meters.volts). */
    public static String voltsLabel(int tier) {
        double v = volts(tier);
        return v >= 1000 ? Math.round(v / 1000) + "kV" : Math.round(v) + "V";
    }

    private static final java.util.Map<Integer, ElectricalCableDescriptor> DESCRIPTORS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Its electrical and thermal constants, as a cable descriptor (one per tier and cross-section: 70 at most). */
    public ElectricalCableDescriptor descriptor(int tier) {
        return DESCRIPTORS.computeIfAbsent(tier * 64 + mm2, k -> make(tier));
    }

    private ElectricalCableDescriptor make(int tier) {
        var base = CablePart.TIERS[tier].descriptor();
        double u = volts(tier), i = amps(), p = u * i;
        var d = new ElectricalCableDescriptor(CablePart.TIERS[tier].name(), base != null ? base.render : null,
            "A power cable", false);
        // setPhysicalConstantLikeNormalCable: Rs = P drop / I^2 / 2, so drop = (ohms a block) I / U
        d.setPhysicalConstantLikeNormalCable(u, p, ohmsPerBlock() * i / u,
            u * 1.3, p * 1.2,
            20,
            Eln.cableWarmLimit, -100,
            Eln.cableHeatingTime, Eln.cableThermalConductionTao);
        return d;
    }
}
