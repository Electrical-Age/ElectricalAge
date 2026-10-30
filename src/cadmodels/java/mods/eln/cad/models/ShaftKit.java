package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mods.eln.cad.models.Mc.prism;

/**
 * Shared pieces of the shaft machines (was tools/models/shaftkit.py; docs/cad.md): the shaft line along z at x = 8,
 * y = 8, half couplings at the shaft faces, port flanges drawn as the pipe renderer draws a pipe's end.
 */
public final class ShaftKit {
    private ShaftKit() {
    }

    public static final double AX = 8.0, AY = 8.0, SHAFT_R = 1.0, COUPLING_R = 2.25, COUPLING_DEPTH = 0.5;
    public static final Map<String, Double> PIPE_R = Map.of("small", 0.75, "large", 3.0), FLANGE_R = Map.of("small", 1.5, "large", 3.75);
    /** The rotor's origin (it turns about z through the shaft line). */
    public static final double[] CENTRE_ORIGIN = {AX, AY, 8.0};

    /** Device paints (the "paint" material's palette entry). */
    public static final String PAINT_HP = "paint_blue", PAINT_LP = "paint_lp", PAINT_GAS = "paint_gas", PAINT_GEN = "paint_gen",
        PAINT_MOTOR = "paint_blue", PAINT_PLAIN = "paint_plain", PAINT_CONDENSER = "paint_condenser", PAINT_HX = "paint_hx";

    public static Mesh shaft(double z0, double z1, String mat, double r) {
        return prism('z', AX, AY, r, z0, z1, mat, 8, false, false);
    }

    public static Mesh shaft(double z0, double z1) {
        return shaft(z0, z1, "darksteel", SHAFT_R);
    }

    /** The half coupling at a shaft face (0: z = 0, 16: z = 16); it turns with the rotor. */
    public static List<Mesh> halfCoupling(int face) {
        double z0, z1, b0, b1;
        if (face == 0) {
            z0 = 0;
            z1 = COUPLING_DEPTH;
            b0 = COUPLING_DEPTH;
            b1 = COUPLING_DEPTH + 0.25;
        } else {
            z0 = 16 - COUPLING_DEPTH;
            z1 = 16;
            b0 = 16 - COUPLING_DEPTH - 0.25;
            b1 = 16 - COUPLING_DEPTH;
        }
        List<Mesh> ms = new ArrayList<>(List.of(prism('z', AX, AY, COUPLING_R, z0, z1, "steel", 12)));
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + Math.PI / 2 * k;
            ms.add(prism('z', AX + 1.6 * Math.cos(a), AY + 1.6 * Math.sin(a), 0.3, b0, b1, "brass", 6));
        }
        return ms;
    }

    /** The shaft from each coupling face to the body (zLo, zHi) and the half couplings, for the rotor part. */
    public static List<Mesh> shaftEnds(double zLo, double zHi, boolean left, boolean right) {
        List<Mesh> ms = new ArrayList<>();
        if (left) {
            ms.add(shaft(COUPLING_DEPTH, zLo));
            ms.addAll(halfCoupling(0));
        }
        if (right) {
            ms.add(shaft(zHi, 16 - COUPLING_DEPTH));
            ms.addAll(halfCoupling(16));
        }
        return ms;
    }

    public static List<Mesh> shaftEnds(double zLo, double zHi) {
        return shaftEnds(zLo, zHi, true, true);
    }

    /**
     * A port's flange on a block face, as the pipe renderer draws a pipe's end: face one of "x0", "x16", "y16", "z0",
     * "z16"; (c1, c2) the spot on the face; a stub of the bore stub px inward. bolts < 0: the default (6 large, 4 small).
     */
    public static List<Mesh> pipeFlange(String face, double c1, double c2, String size, double stub, String mat, int bolts) {
        double fr = FLANGE_R.get(size), pr = PIPE_R.get(size);
        char axis = face.charAt(0);
        boolean at0 = face.endsWith("0") && !face.endsWith("16");
        boolean large = size.equals("large");
        int nb = bolts >= 0 ? bolts : large ? 6 : 4;
        List<Mesh> ms = new ArrayList<>();
        if (at0) {
            ms.add(prism(axis, c1, c2, fr, 0.0, 1.0, mat, large ? 14 : 10, true, true));
            ms.add(prism(axis, c1, c2, pr, 1.0, 1.0 + stub, mat, large ? 12 : 8, false, true));
            ms.addAll(OilKit.boltRing(axis, c1, c2, fr - 0.45, 1.0, 1.25, nb, "brass", large ? 0.25 : 0.18, 0));
        } else {
            ms.add(prism(axis, c1, c2, fr, 15.0, 16.0, mat, large ? 14 : 10, true, true));
            ms.add(prism(axis, c1, c2, pr, 15.0 - stub, 15.0, mat, large ? 12 : 8, true, false));
            for (int k = 0; k < nb; k++) {
                double a = 2 * Math.PI * (k + 0.5) / nb, r = fr - 0.45;
                ms.add(prism(axis, c1 + r * Math.cos(a), c2 + r * Math.sin(a), large ? 0.25 : 0.18, 14.75, 15.0, "brass", 6, true, false));
            }
        }
        return ms;
    }

    public static List<Mesh> pipeFlange(String face, double c1, double c2, String size) {
        return pipeFlange(face, c1, c2, size, 1.5, "steel", -1);
    }
}
