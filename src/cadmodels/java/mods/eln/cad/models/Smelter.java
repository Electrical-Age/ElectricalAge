package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;

/**
 * Smelter (Baughn 2026-10-23, was tools/models/smelter.py): a firebrick furnace in a cast-iron frame on short legs, a
 * hopper's square funnel on top (a hopper above feeds it) and a chute underneath (a hopper below empties it), a firebox
 * window in front that the renderer lights by temperature (part "glow"), a copper thermal plate on the back down to the
 * floor (the thermal cable), and 50 V terminal plates low on both sides (its controls). Drawn by SmelterRender through
 * Obj3D. Front XN (x = 0).
 */
public final class Smelter {
    private Smelter() {
    }


    static final double Q = Math.PI / 4;

    /** The same faces wound the other way (seen from inside: a funnel's inner wall). */
    static Mesh flipped(Mesh m) {
        return m.mapped(p -> {
        }, true);
    }

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // legs and the chute below
        for (double[] xz : new double[][]{{1.5, 1.5}, {13, 1.5}, {1.5, 13}, {13, 13}})
            m.add(box(xz[0], 0, xz[1], xz[0] + 1.5, 3, xz[1] + 1.5, "darksteel"));
        m.add(prism('y', 8, 8, 1.6, 0.4, 3.0, "castiron", 4, 2.8, true, false, Q));
        m.add(prism('y', 8, 8, 1.2, 0.35, 0.41, "soot", 4, Double.NaN, true, false, Q));   // (its mouth)
        // the body: firebrick between cast-iron corner posts and bands
        // (the checker: the brick's bottom lay in the plane of the lower band's; the brick's, hidden, is dropped)
        m.add(box(1.5, 3, 1.5, 14.5, 12.5, 14.5, "brick", "nsewu"));
        for (double[] xz : new double[][]{{1, 1}, {14, 1}, {1, 14}, {14, 14}})
            m.add(box(xz[0], 3, xz[1], xz[0] + 1, 13, xz[1] + 1, "castiron"));
        for (double y : new double[]{3, 12}) m.add(box(1, y, 1, 15, y + 1, 15, "castiron", y == 12 ? "nsewu" : "nsewd"));
        // the firebox window in front (its glow: part glow), its frame and door hinges
        m.add(box(0.6, 5, 4.5, 1.5, 10, 11.5, "steel"));
        for (double y : new double[]{5.5, 8.5}) m.add(box(0.3, y, 11.5, 1.0, y + 1, 12.3, "darksteel"));
        // the funnel on top: a square hopper mouth, walls inside and out
        // (the checker: the funnel stood on the band only along its throat's edge; a cast-iron collar round the throat,
        // 7 x 7 x 0.3 px on the band, holds it. Visible: a low square collar under the funnel's flare. The checker judges
        // a joint by the part of the first-listed piece inside the second's hull, and an inside-out wall has no hull:
        // the throat and the inner wall are listed before the outer wall they sit in, no change)
        m.add(prism('y', 8, 8, 3.4, 13.35, 13.4, "soot", 4, Double.NaN, false, true, Q));      // (the throat)
        m.add(flipped(prism('y', 8, 8, 3.4, 13.4, 16, "darksteel", 4, 7.6, false, false, Q)));
        m.add(prism('y', 8, 8, 4.0, 13, 16, "castiron", 4, 8.2, false, false, Q));
        m.add(box(4.5, 13, 4.5, 11.5, 13.3, 11.5, "castiron", "nsewu"));
        m.add(prism('y', 8, 8, 8.2, 15.6, 16, "steel", 4, 8.2, false, false, Q));              // (rim)
        // the back: a copper thermal plate down to the floor, where the thermal cable joins
        m.add(box(15.1, 0, 4.5, 15.9, 7, 11.5, "copper"));
        m.add(box(14.5, 1, 5.5, 15.1, 6, 10.5, "darksteel"));
        // the sides: 50 V terminal plates, low (the controls), with a tier stripe
        // (the checker: the plates stood 0.15 px off the lower band; they reach back to it (z 0.1..1.0 and 15.0..15.9, was
        // ..0.85 and 15.15..), barely visible)
        for (double[] zz : new double[][]{{0.1, 0.85, 1.0}, {15.15, 15.9, 15.0}}) {
            double z0 = zz[0], z1 = zz[1], zc = (z0 + z1) / 2, p0 = Math.min(z0, zz[2]), p1 = Math.max(z1, zz[2]);
            m.add(box(4.5, 0, p0, 11.5, 4.0, p1, "steel"));
            m.add(box(4.5, 4.0, p0, 11.5, 4.5, p1, "paint"));
            for (double x : new double[]{5.75, 8.0, 10.25})
                m.add(box(x - 0.5, 1.75, Math.min(zc, z0 < 8 ? z0 - 0.1 : z1), x + 0.5, 2.75, Math.max(zc, z0 > 8 ? z1 + 0.1 : z0), "copper"));
        }
        return m;
    }

    static List<Mesh> glow() {
        return List.of(box(0.5, 5.75, 5.25, 0.6, 9.25, 10.75, "glow", "w"));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("smelter", OilKit.PAINT_50V);   // (50 V green, the cable colour)
        n.part("main", main());
        n.part("glow", glow());
        return List.of(n);
    }
}
