package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * The energy converters (EA -> other), block models (neoforge:obj), one per tier: a small synchronous condenser on a
 * skid. The EA side (the node's front, where EA cables attach) is north (z = 0); the export side south (z = 16); the
 * blockstate turns it per FRONT. Was tools/models/converter.py.
 */
public final class Converter {
    private Converter() {
    }

    /** Tier paint, bushing count, bushing height (px), sheds per bushing, bushing radius. */
    record Tier(String paint, int bushings, double h, int sheds, double r) {
    }

    static final Map<String, Tier> TIERS = Map.of("lvu", new Tier("paint_green", 2, 1.6, 0, 0.75), "mvu", new Tier("paint_blue", 3, 2.4, 1, 0.8),
        "hvu", new Tier("paint_orange", 3, 3.0, 2, 0.85));
    /** The stator axis (x, y; it runs north-south) and radius. */
    static final double CX = 8.0, CY = 6.75, R = 4.75;

    static Model condenser(String tier) {
        Tier t = TIERS.get(tier);
        List<Mesh> m = new ArrayList<>();
        // skid: two rails and cross members, the full footprint, so it sits on the block floor; feet under the stator
        // (the checker: the feet stood a quarter on the rails, which hung on nothing else; the rails are 1.5 px wider,
        // inward, so the feet stand on them, and the cross members are shorter)
        m.add(box(1.5, 0, 0.5, 5.5, 1.5, 15.5, "darksteel"));
        m.add(box(10.5, 0, 0.5, 14.5, 1.5, 15.5, "darksteel"));
        m.add(box(5.5, 0.25, 2, 10.5, 1.25, 3.5, "darksteel"));
        m.add(box(5.5, 0.25, 12.5, 10.5, 1.25, 14, "darksteel"));
        for (double[] xz : new double[][]{{3.5, 4}, {10.5, 4}, {3.5, 10}, {10.5, 10}}) m.add(box(xz[0], 1.5, xz[1], xz[0] + 2, 3.5, xz[1] + 2, "steel"));
        // stator housing (12-gon), end bells, cooling fins, tier band
        m.add(prism('z', CX, CY, R, 3, 13, "steel", 12, false, false));
        m.add(prism('z', CX, CY, R, 2, 3, "steel", 12, false, false));
        m.add(prism('z', CX, CY, R - 1.25, 1.25, 2, "steel", 12, R, true, false, Double.NaN));      // north end bell
        m.add(prism('z', CX, CY, R, 13, 14, "steel", 12, false, false));
        m.add(prism('z', CX, CY, R, 14, 14.75, "steel", 12, R - 1.25, false, true, Double.NaN));    // south end bell
        // (the checker: the third fin's end lay in the terminal box's south face; it is 0.1 px further south)
        for (double z : new double[]{3.5, 5.0, 11.1, 12.5}) m.add(tube('z', CX, CY, R - 0.1, R + 0.7, z, z + 0.5, "darksteel", 12, Double.NaN, false));
        m.add(tube('z', CX, CY, R - 0.1, R + 0.45, 6.5, 9.5, t.paint(), 12, Double.NaN, false));
        // shaft: north stub into the EA terminal plate, south coupling to the export flange
        m.add(prism('z', CX, CY, 1.0, 0.75, 1.25, "darksteel", 8));
        m.add(prism('z', CX, CY, 1.0, 14.75, 15.0, "darksteel", 8));
        m.add(prism('z', CX, CY, 2.0, 15.0, 15.5, "copper", 8));
        // EA side (north): terminal plate down to the floor, where cables meet it (the checker: as every other plate now,
        // recessed 0.1 px with the studs reaching the face, the studs were 0.02 px thin and the plate's face lay in their
        // plane; it reaches back to the skid's cross member, it stood on nothing; the tier stripe sits on its top, its
        // sides lay in the plate's)
        m.add(box(4.5, 0, 0.1, 11.5, 4.0, 2.0, "steel", "nsewu"));   // (no underside: on the floor, in the rails')
        for (double x : new double[]{5.75, 8.0, 10.25}) m.add(box(x - 0.5, 1.75, 0, x + 0.5, 2.75, 0.1, "copper", "nsewu"));
        m.add(box(4.5, 4.0, 0.1, 11.5, 4.5, 0.85, t.paint(), "nsewu"));
        // export side (south): pedestal, plate frame, panel and its brass bolts
        m.add(box(5.0, 0.0, 15.25, 11.0, 2.5, 16.0, "darksteel"));
        // (the checker: the panel and the bolt heads lay 0.01 px apart in the face; the frame is recessed 0.2 px, the panel
        // a decal 0.1 px over it, the bolts real heads standing 0.2 px proud)
        m.add(box(5.5, 2.5, 15.5, 10.5, 10.0, 15.8, "darksteel"));
        m.add(box(6.25, 3.25, 15.9, 9.75, 9.25, 15.9, "export", "s").free("the export panel, a decal 0.1 px over the frame"));
        for (double[] b : new double[][]{{5.9, 2.9}, {10.1, 2.9}, {5.9, 9.6}, {10.1, 9.6}})
            m.add(box(b[0] - 0.25, b[1] - 0.25, 15.8, b[0] + 0.25, b[1] + 0.25, 16.0, "brass", "nsewu"));
        // terminal box on top, with the tier's bushings
        double top = CY + R;
        m.add(box(5.0, top - 0.75, 4.5, 11.0, top + 1.25, 11.5, "steel"));
        m.add(box(4.75, top + 1.25, 4.25, 11.25, top + 1.5, 11.75, "darksteel"));                  // lid
        double by = top + 1.5;
        int n = t.bushings();
        for (int i = 0; i < n; i++) {
            double x = n == 1 ? 8.0 : 8.0 + (i - (n - 1) / 2.0) * 2.25, z = 8.0;
            m.add(prism('y', x, z, t.r(), by, by + t.h(), "porcelain", 8, false, false));
            m.add(prism('y', x, z, 0.45, by + t.h(), by + t.h() + 0.5, "copper", 6, false, true));
            for (int k = 0; k < t.sheds(); k++) {
                double sy = by + (k + 1) * t.h() / (t.sheds() + 1) - 0.2;
                m.add(prism('y', x, z, t.r() + 0.55, sy, sy + 0.35, "porcelain", 8));
            }
            m.add(prism('y', x, z, t.r() + 0.2, by, by + 0.3, t.paint(), 8));                       // tier collar
        }
        // nameplate on the east side of the terminal box
        m.add(box(11.0, top - 0.25, 6.0, 11.1, top + 0.75, 10.0, "brass"));
        Model model = new Model();
        model.addAll(m);
        return model;
    }

    public static List<BlockModel> models() {
        return List.of(new BlockModel("energy_converter_lvu", condenser("lvu"), "converter", "steel"),
            new BlockModel("energy_converter_mvu", condenser("mvu"), "converter", "steel"),
            new BlockModel("energy_converter_hvu", condenser("hvu"), "converter", "steel"));
    }
}
