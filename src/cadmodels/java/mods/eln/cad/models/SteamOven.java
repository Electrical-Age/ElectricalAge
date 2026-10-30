package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;

/**
 * Steam oven (Baughn 2026-10-23, was tools/models/steamoven.py): a steel cabinet on feet with a glass door in front,
 * the steam pipe coming in on top (an iron pipe's 4 px bore to a laid pipe's flange on the top face, where a pipe
 * couples), a vent out of the back face that the renderer puffs steam from (the .txt's ventX/Y/Z), and 50 V terminal
 * plates low on both sides (the fan). Drawn by SteamOvenRender through Obj3D. Front XN (x = 0).
 */
public final class SteamOven {
    private SteamOven() {
    }


    static final double PIPE_R = 2.0;
    /** The vent outlet's centre (px); it faces +x, the back. */
    static final double[] VENT = {16, 11, 8};

    static List<Mesh> oven() {
        List<Mesh> m = new ArrayList<>();
        for (double[] xz : new double[][]{{1.5, 1.5}, {13, 1.5}, {1.5, 13}, {13, 13}})   // feet
            m.add(box(xz[0], 0, xz[1], xz[0] + 1.5, 1.5, xz[1] + 1.5, "darksteel"));
        // (the checker: the cabinet's bottom lay in the plane of the trim band's bottom; the cabinet's, hidden, is dropped)
        m.add(box(1, 1.5, 1, 15, 13, 15, "steel", "nsewu"));                            // the cabinet
        // trim (bands round the sides)
        m.add(box(0.8, 1.5, 0.8, 15.2, 2.0, 15.2, "darksteel", "nsewd"));
        m.add(box(0.8, 12.5, 0.8, 15.2, 13.0, 15.2, "darksteel", "nsew"));
        m.add(box(0.5, 3, 2.5, 1.0, 12, 13.5, "darksteel"));                            // the door frame
        m.add(box(0.4, 4, 3.5, 0.5, 11, 12.5, "darkglass", "w").free("the window pane, a decal 0.1 px proud of the door frame"));   // its window
        m.add(box(0.0, 6.5, 12.6, 0.5, 8.5, 13.2, "brass"));                            // the handle
        for (double y : new double[]{4.5, 10.0}) m.add(box(0.2, y, 2.3, 0.8, y + 1, 2.9, "darksteel"));   // hinges
        // the steam pipe in on top: an iron pipe's bore to a laid pipe's flange
        m.add(prism('y', 8, 8, PIPE_R, 13, 15, "castiron", 12, false, false));
        m.add(prism('y', 8, 8, PIPE_R + 0.75, 15, 16, "steel", 12));
        m.add(prism('y', 8, 8, PIPE_R + 0.35, 13, 13.6, "paint", 12, false, true));     // (50 V green collar)
        // the vent out of the back face, high up (a boiler set on top would hide one on the roof, AdventurAgent)
        m.add(prism('x', 11, 8, 0.9, 15, 15.9, "castiron", 8, false, false));
        m.add(prism('x', 11, 8, 1.2, 15.6, 16, "darksteel", 8, false, false));
        // (the checker: the soot plug's 0.05 px side walls lay 0.05 px inside the pipe's and z-fought; only its face is kept)
        m.add(capOnly(prism('x', 11, 8, 0.85, 15.3, 15.35, "soot", 8, false, true)));
        // the fan's 50 V terminal plates, low on both sides
        // (the checker: the plates stood 0.15 px off the cabinet, held only by the trim band's edge; they reach back 0.15 px
        // to the cabinet's side now, behind the trim: plate z 0.1..1.0 and 15.0..15.9, was ..0.85 and 15.15..)
        for (double[] zz : new double[][]{{0.1, 0.85, 1.0}, {15.15, 15.9, 15.0}}) {
            double z0 = zz[0], z1 = zz[1], p0 = Math.min(z0, zz[2]), p1 = Math.max(z1, zz[2]);
            m.add(box(4.5, 0, p0, 11.5, 4.0, p1, "steel"));
            m.add(box(4.5, 4.0, p0, 11.5, 4.5, p1, "paint"));
            for (double x : new double[]{5.75, 8.0, 10.25}) m.add(box(x - 0.5, 1.75, z0 - 0.1, x + 0.5, 2.75, z1 + 0.1, "copper"));
        }
        return m;
    }

    /** Only the last polygon (a prism's far cap): a disc without its side walls. */
    static Mesh capOnly(Mesh m) {
        Mesh d = new Mesh((String) m.material);
        d.polys.add(m.polys.get(m.polys.size() - 1));
        return d;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("steamoven", OilKit.PAINT_50V);
        n.part("main", oven());
        n.txt.add("o main");
        String[] k = {"ventX", "ventY", "ventZ"};
        for (int i = 0; i < 3; i++) n.txt.add(String.format(Locale.ROOT, "f %s %.5f", k[i], VENT[i] / 16 - 0.5));
        return List.of(n);
    }
}
