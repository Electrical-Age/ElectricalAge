package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.obox;

/**
 * The Portable Electrical Mining Drill (Baughn 2026-10-30: the old one was a bit meh; it turns when used): a pistol-grip
 * drill pointing along +x, an orange motor housing with cooling vents and a battery pack under the grip, and part "bit"
 * (the chuck and a fluted bit) that the item draw turns about x through its origin while the drill digs. The model
 * spans x 2..16, centred on y 8, z 8 (the item draw centres it).
 */
public final class HandDrill {
    private HandDrill() {
    }

    /** The bit's axis (px): it turns about x through here. */
    static final double AY = 10.5, AZ = 8, NOSE = 11.5;

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // motor housing along the axis, a darker back cap with vent slots, a gearbox nose
        m.add(prism('x', AY, AZ, 2.2, 4.0, 10.0, "paint", 12));
        m.add(prism('x', AY, AZ, 1.9, 3.0, 4.0, "darksteel", 12, true, false));
        for (double y : new double[]{-1.0, 0.0, 1.0}) m.add(box(3.6, AY + y - 0.2, AZ - 2.25, 4.4, AY + y + 0.2, AZ - 2.05, "darksteel", "nswdu").free("a vent slot, a decal on the housing"));
        m.add(prism('x', AY, AZ, 1.6, 10.0, NOSE, "darksteel", 12, true, true));
        m.add(prism('x', AY, AZ, 2.35, 6.4, 7.0, "darksteel", 12, false, false));       // a band round the housing
        // the pistol grip, raked back, the trigger in front of it, the battery pack under it
        double gx = 6.2, gy = 5.6;
        double c = Math.cos(Math.toRadians(15)), s = Math.sin(Math.toRadians(15));
        m.add(obox(v(gx, gy, AZ), v(c, s, 0), v(-s, c, 0), v(0, 0, 1), 1.1, 3.4, 1.1, "rubber"));
        m.add(box(gx + 0.9, 7.2, AZ - 0.35, gx + 1.7, 8.4, AZ + 0.35, "red"));
        m.add(box(3.0, 0.6, AZ - 1.9, 9.0, 2.6, AZ + 1.9, "darksteel"));
        m.add(box(3.4, 0.6, AZ - 2.0, 8.6, 1.2, AZ + 2.0, "paint_green", "ns").free("the battery's charge stripe, a decal on its pack"));
        m.add(box(8.6, 1.6, AZ - 0.6, 9.1, 2.2, AZ + 0.6, "lamp"));                       // its charge lamp
        return m;
    }

    static List<Mesh> bit() {
        List<Mesh> m = new ArrayList<>();
        // the chuck: a knurled collar, a tapered nose, three jaws gripping the bit
        m.add(prism('x', AY, AZ, 1.35, NOSE, 12.6, "steel", 8));
        m.add(prism('x', AY, AZ, 1.35, 12.6, 13.5, "steel", 8, 0.75, true, true, Double.NaN));
        for (int k = 0; k < 3; k++) {
            double a = 2 * Math.PI * k / 3;
            m.add(obox(v(13.7, AY + 0.45 * Math.cos(a), AZ + 0.45 * Math.sin(a)), v(1, 0, 0), v(0, Math.cos(a), Math.sin(a)), v(0, -Math.sin(a), Math.cos(a)),
                0.35, 0.2, 0.2, "darksteel"));
        }
        // the bit: a core with two flutes winding round it (they read the spin)
        m.add(prism('x', AY, AZ, 0.35, 13.3, 16.0, "steel", 6));
        for (int k = 0; k < 6; k++) {
            double x = 13.9 + 0.35 * k, a = 1.1 * k;
            for (int f = 0; f < 2; f++) {
                double b = a + Math.PI * f;
                m.add(obox(v(x, AY + 0.35 * Math.cos(b), AZ + 0.35 * Math.sin(b)), v(1, 0, 0), v(0, Math.cos(b), Math.sin(b)), v(0, -Math.sin(b), Math.cos(b)),
                    0.2, 0.15, 0.12, "brass"));
            }
        }
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("handdrill", "welder");
        n.part("main", main());
        n.part("bit", bit());
        n.origin("bit", NOSE, AY, AZ);
        return List.of(n);
    }
}
