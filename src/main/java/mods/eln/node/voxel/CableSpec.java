package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * A power cable's specification (Baughn 2026-11-06, Wires stage 3): its copper cross-section and its insulation colour.
 * Its insulation is its tier (CablePart.TIERS 1..4: 48 V, 230 V, 400 V, 11 kV). Signal, thermal and bus cables have
 * none.
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

    /** A power tier (insulation): 1 48 V, 2 230 V, 3 400 V, 4 11 kV. */
    public static boolean power(int tier) {
        return tier >= 1 && tier <= 4;
    }

    /** The insulation's voltage of a power tier. */
    public static double volts(int tier) {
        return switch (tier) {
            case 1 -> Eln.LVU;
            case 2 -> Eln.MVU;
            case 3 -> Eln.HVU;
            default -> Eln.VVU;
        };
    }

    /**
     * Each power tier's preset (the hand-crafted kits, and every cable laid before specs; Baughn: 48 V brown 1 kW, 230 V
     * black 3 kW / 13 A; 400 V grey and 11 kV red, Baughn's Q3): 2.5 mm² brown, 1.5 mm² black, 1.5 mm² grey,
     * 0.5 mm² red.
     */
    public static CableSpec preset(int tier) {
        return switch (tier) {
            case 1 -> new CableSpec(4, BROWN);
            case 2 -> new CableSpec(3, BLACK);
            case 3 -> new CableSpec(3, GRAY);
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
            case 3 -> 3;
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

    /** "2.5 mm², 48 V". */
    public String label(int tier) {
        double m = MM2[mm2];
        String mm = m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m);
        double v = volts(tier);
        return mm + " mm², " + (v >= 1000 ? (int) (v / 1000) + " kV" : (int) v + " V");
    }

    private static final java.util.Map<Integer, ElectricalCableDescriptor> DESCRIPTORS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Its electrical and thermal constants, as a cable descriptor (one per tier and cross-section: 56 at most). */
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
