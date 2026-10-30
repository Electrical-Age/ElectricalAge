package mods.eln.client.entity;

import mods.eln.bike.BikeShape;
import org.erisia.cad.Box;
import org.erisia.cad.Checker;
import org.erisia.cad.Face;
import org.erisia.cad.Model;
import org.erisia.cad.Solid;
import org.erisia.cad.entity.EntityLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bikes on the checked model library (board #44): both pass the checker (the battery stands on the down tube's
 * surface, clear of the seat tube, at either tube thickness), every box's texture net fits its region, and the layer
 * draws what it drew before (LegacyBikeModel) except the battery.
 */
public class BikeModelCadTest {
    /**
     * Z-fights in geometry drawn as before, left as they were (they need Baughn's say on the look): the octagon rims'
     * segments overlap at their joints, the stays meet side by side at the rear hub, the top and seat tubes at the
     * seat cluster, and the saddle's two boxes share their underside (and their tops are only 0.05 px apart).
     */
    static final String[][] KNOWN = {{"rim", "rim"}, {"chain_stay", "seat_stay"}, {"top_tube", "seat_tube"}, {"saddle", "saddle"}};

    static boolean known(Checker.Issue is) {
        if (!is.rule().equals("z-fight")) return false;
        String[] l = is.what().split("\n");
        for (String[] k : KNOWN)
            if (l.length == 2 && (l[0].contains("Box " + k[0]) && l[1].contains("Box " + k[1]) || l[0].contains("Box " + k[1]) && l[1].contains("Box " + k[0])))
                return true;
        return false;
    }

    @Test
    void bothBikesPassTheChecker() {
        for (boolean terrain : new boolean[]{false, true}) {
            Model m = BikeShape.model(terrain);
            List<String> bad = new ArrayList<>();
            int known = 0;
            for (Checker.Issue is : Checker.check(m)) {
                if (known(is)) known++;
                else bad.add(is.toString());
            }
            System.out.println((terrain ? "terrain bike" : "bike") + ": " + m.solids.size() + " boxes, known z-fights left as drawn: " + known);
            assertTrue(bad.isEmpty(), (terrain ? "terrain bike: " : "bike: ") + String.join("\n", bad));
        }
    }

    @Test
    void theBatteryStandsOnTheDownTube() {
        for (boolean terrain : new boolean[]{false, true}) {
            Model m = BikeShape.model(terrain);
            Box tube = (Box) m.solid("down_tube"), bat = BikeShape.battery(m);
            assertEquals(tube.hi[1], bat.lo[1], 1e-6, "in the tube's frame, the battery's underside is the tube's top");
            assertEquals(m.solid("down_tube").part.rx, bat.part.rx, 1e-9);
            assertTrue(Checker.relations(m).isEmpty(), Checker.relations(m).toString());
        }
    }

    @Test
    void everyNetFitsItsRegion() {
        int n = BikeShape.REGIONS.length;
        for (int i = 0; i < n; i++) {
            int[] a = BikeShape.REGIONS[i], sa = BikeShape.REGION_SIZE[i];
            assertTrue(a[0] + sa[0] <= BikeShape.TEX_W && a[1] + sa[1] <= BikeShape.TEX_H, BikeShape.REGION_NAME[i] + " inside the texture");
            for (int j = i + 1; j < n; j++) {
                int[] b = BikeShape.REGIONS[j], sb = BikeShape.REGION_SIZE[j];
                boolean apart = a[0] + sa[0] <= b[0] || b[0] + sb[0] <= a[0] || a[1] + sa[1] <= b[1] || b[1] + sb[1] <= a[1];
                assertTrue(apart, BikeShape.REGION_NAME[i] + " and " + BikeShape.REGION_NAME[j] + " overlap");
            }
        }
        for (boolean terrain : new boolean[]{false, true})
            for (Solid s : BikeShape.model(terrain).solids) {
                int r = -1;
                for (int i = 0; i < n; i++) if (BikeShape.REGIONS[i] == s.material) r = i;
                assertTrue(r >= 0, s + " has a region");
                float[] net = EntityLayout.netBounds((Box) s);
                int[] at = BikeShape.REGIONS[r], size = BikeShape.REGION_SIZE[r];
                assertTrue(net[0] >= at[0] && net[1] >= at[1] && net[2] <= at[0] + size[0] + 1e-4 && net[3] <= at[1] + size[1] + 1e-4,
                    s + "'s net " + java.util.Arrays.toString(net) + " overruns the " + BikeShape.REGION_NAME[r] + " region");
            }
    }

    static List<String> keys(List<Face> faces) {
        List<String> out = new ArrayList<>();
        for (Face f : faces) {
            if (f.solid.name.startsWith("/battery")) continue;
            String[] c = new String[f.n];
            for (int i = 0; i < f.n; i++)
                c[i] = String.format("(%.3f %.3f %.3f|%.4f %.4f)", f.xyz[3 * i], f.xyz[3 * i + 1], f.xyz[3 * i + 2], f.uv[2 * i], f.uv[2 * i + 1]).replace("-0.000", "0.000");
            int start = 0;
            for (int i = 1; i < f.n; i++) if (c[i].compareTo(c[start]) < 0) start = i;
            StringBuilder b = new StringBuilder(f.solid.name.replaceAll("#.*", "")).append(Integer.toHexString(f.argb()));
            for (int i = 0; i < f.n; i++) b.append(c[(start + i) % f.n]);
            out.add(b.toString());
        }
        out.sort(null);
        return out;
    }

    @Test
    void drawsAsBeforeExceptTheBattery() {
        for (boolean terrain : new boolean[]{false, true}) {
            var was = keys(BikeFaces.of(LegacyBikeModel.createLayer(terrain).bakeRoot(), LegacyBikeModel.GROUPS));
            var now = keys(BikeFaces.of(BikeModel.createLayer(terrain).bakeRoot(), BikeModel.GROUPS));
            assertEquals(was.size(), now.size(), "faces");
            for (int i = 0; i < was.size(); i++) assertEquals(was.get(i), now.get(i), (terrain ? "terrain " : "") + "face " + i);
        }
    }

    @Test
    void theModelsFacesAreWhatVanillaDraws() {
        // EntityLayout's corners and texture coordinates against the baked layer's polygons, battery included (to 0.005
        // px and 1e-4 of the texture: the layer is floats)
        for (boolean terrain : new boolean[]{false, true}) {
            List<Face> mine = BikeShape.model(terrain).faces(), vanilla = new ArrayList<>(BikeFaces.of(BikeModel.createLayer(terrain).bakeRoot(), java.util.Map.of()));
            assertEquals(vanilla.size(), mine.size());
            for (Face f : mine) {
                Face hit = null;
                for (Face g : vanilla) if (same(f, g)) hit = g;
                assertTrue(hit != null, "vanilla draws no face like " + f + " uv " + java.util.Arrays.toString(f.uv));
                vanilla.remove(hit);
            }
        }
    }

    static boolean same(Face f, Face g) {
        if (f.n != g.n) return false;
        for (int s = 0; s < f.n; s++) {
            boolean ok = true;
            for (int i = 0; i < f.n && ok; i++) {
                int j = (i + s) % f.n;
                for (int k = 0; k < 3; k++) ok &= Math.abs(f.xyz[3 * i + k] - g.xyz[3 * j + k]) < 0.005;
                for (int k = 0; k < 2; k++) ok &= Math.abs(f.uv[2 * i + k] - g.uv[2 * j + k]) < 1e-4;
            }
            if (ok) return true;
        }
        return false;
    }
}
