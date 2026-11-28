package mods.eln.cad.models;

import org.erisia.cad.Frustum;
import org.erisia.cad.Mesh;
import org.erisia.cad.Solid;
import org.erisia.cad.Sweep;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.v;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.obox;
import static mods.eln.cad.models.OilKit.signalPlate;

/**
 * The 2x2x2 Large Boiler (todo #472; its own model, todo #522; redrawn, todo #588, Baughn 2026-11-23: the drum is the
 * device, not a boiler in a shoe box, and no firebox: it is heated from outside): a big riveted steam drum along x with
 * dished heads, in two steel box-girder cradles on a low skid, six heat pipes up from its heat plates into its belly
 * beside the cradles, a steam dome with its header to the two steam flanges and a safety-valve dome on its crest.
 * Drawn by BoilerRender from its origin block (the front bottom left), full size: 0..32 px on each
 * axis, front XN (x = 0), right z+, as LargeBoilerElement's local frame (block x 0 front / 1 back, y 0 low / 1 high,
 * z 0 left / 1 right). Its connections, where the element has them (each at its block face's centre, as the ports'
 * spots are):
 * <ul>
 * <li>thermal: a copper heat plate on each of the six lower block faces of the back and both flanks, at the floor, on
 * the skid; from each a heat pipe (r 1.75) rises and bends in to the drum's lower flank or back head (Baughn
 * 2026-11-23: one row of plates, piped to the drum so they don't look disconnected);</li>
 * <li>steam out: an iron pipe's steam flange on a riser at each end of the steam header, on top of each rear top block
 * (x 24, z 8 and z 24; top at y 32);</li>
 * <li>feedwater in: a flange on the front of the bottom right block (y 8, z 24), its copper feed line up the front
 * right corner through the check valve and into the drum's flank;</li>
 * <li>the level signal: a signal plate at the floor of the front of the origin block (z 8), the level transmitter's box
 * behind it and its line up to the gauge glass;</li>
 * <li>the gauge glass (part level, scaled by the render about its foot) on the front head, left, and the pressure dial
 * (part needle) on a siphon stalk above the front head, right.</li>
 * </ul>
 * No hitbox: its eight blocks are whole blocks (the node and its ghosts).
 */
public final class LargeBoiler {
    private LargeBoiler() {
    }

    /** The drum: axis (y, z), radius, sides, its cylinder's ends (x), the heads' depth. The skid's deck top. */
    static final double CY = 16.5, CZ = 16.0, R = 12.0, X0 = 5.0, X1 = 27.5, HEAD = 2.5, DECK = 4.2;
    static final int N = 20;
    /** The feed flange's centre on the front (y, z) and the steam flanges' on top (x; z each). */
    static final double FEED_Y = 8.0, FEED_Z = 24.0, STEAM_X = 24.0;
    static final double[] STEAM_Z = {8.0, 24.0};
    /**
     * BoilerRender scales "level" in y about the column's foot and turns "needle" about x at the dial's centre. The
     * column is half the glass (GLASS) high and starts on a whole block (y 16), so the planar UVs put it on the water
     * cell's blue half.
     */
    static final double[] GAUGE = {1.4, 16.0, 9.5}, DIAL = {2.6, 27.6, 22.0};
    static final double GLASS = 7.0, DIAL_R = 2.2;
    /** The feed line's corner column (x, z) and the height it turns back into the drum at. */
    static final double FX = 2.2, FZ = 27.2, FTOP = 22.0;

    /**
     * A thermal contact: a heat-tinted copper plate 8 px wide and 4 high at (centre u, floor y) on the face at w (0 or
     * 32) across axis 'x' or 'z', 0.1 px short of the block face, bedded 0.2 px into what carries it, with four bolt heads.
     */
    static List<Mesh> heatPlate(char axis, double w, double u, double y) {
        List<Mesh> m = new ArrayList<>();
        boolean far = w > 16;
        double a0 = far ? 30.8 : 0.1, a1 = far ? 31.9 : 1.2, b0 = far ? 31.9 : 0.0, b1 = far ? 32.0 : 0.1;
        if (axis == 'x') m.add(box(a0, y, u - 4, a1, y + 4, u + 4, "hotcopper"));
        else m.add(box(u - 4, y, a0, u + 4, y + 4, a1, "hotcopper"));
        for (double[] p : new double[][]{{y + 0.9, u - 3.1}, {y + 0.9, u + 3.1}, {y + 3.1, u - 3.1}, {y + 3.1, u + 3.1}})
            m.add(axis == 'x' ? prism('x', p[0], p[1], 0.35, b0, b1, "brass", 6, !far, far) : prism('z', p[1], p[0], 0.35, b0, b1, "brass", 6, !far, far));
        return m;
    }

    /** HEAT_R: the heat pipes' radius; the flank pipes rise at z HEAT_Z (and 32 - HEAT_Z), the back pipes at x 32 - HEAT_Z. */
    static final double HEAT_R = 1.75, HEAT_Z = 2.0;
    /** The cradles' centres (x): inboard of the flank heat pipes (at x 8 and 24). */
    static final double[] CRADLE_X = {12.5, 19.5};

    /**
     * A heat pipe along the points (px): Sweep's copper pieces, run on past each bend by half the radius so their
     * outsides meet, and a darksteel collar (a short Sweep piece along the bend's bisector) over each bend.
     */
    static List<Solid> heatPipe(double[] pts) {
        List<Solid> m = new ArrayList<>();
        for (Frustum f : Sweep.along(pts, HEAT_R, 0.5)) m.add(f.material("copper"));
        for (int i = 1; 3 * i + 3 < pts.length; i++) {
            double[] t = new double[3];
            double l0 = 0, l1 = 0;
            for (int k = 0; k < 3; k++) {
                l0 += Math.pow(pts[3 * i + k] - pts[3 * i - 3 + k], 2);
                l1 += Math.pow(pts[3 * i + 3 + k] - pts[3 * i + k], 2);
            }
            for (int k = 0; k < 3; k++)
                t[k] = (pts[3 * i + k] - pts[3 * i - 3 + k]) / Math.sqrt(l0) + (pts[3 * i + 3 + k] - pts[3 * i + k]) / Math.sqrt(l1);
            double lt = Math.sqrt(t[0] * t[0] + t[1] * t[1] + t[2] * t[2]);
            double[] c = new double[6];
            for (int k = 0; k < 3; k++) {
                c[k] = pts[3 * i + k] - 0.5 * t[k] / lt;
                c[3 + k] = pts[3 * i + k] + 0.5 * t[k] / lt;
            }
            for (Frustum f : Sweep.along(c, HEAT_R + 0.3, 0)) m.add(f.material("darksteel"));
        }
        return m;
    }

    /** A copper elbow (a cube) at the feed line's corner (x, y, z). */
    static Mesh elbow(double x, double y, double z) {
        return box(x - 0.9, y - 0.9, z - 0.9, x + 0.9, y + 0.9, z + 0.9, "copper");
    }

    static List<Solid> main() {
        List<Solid> m = new ArrayList<>();
        // the skid: a low cast-iron bed under a darksteel deck (the deck's edge stands 0.4 px out; the lower heat plates
        // reach into it)
        m.add(box(1.0, 0, 1.0, 31.0, DECK - 0.8, 31.0, "castiron", "nsew"));
        m.add(box(0.6, DECK - 0.8, 0.6, 31.4, DECK, 31.4, "darksteel", "nsewud"));
        // the cradles under the drum: steel box girders right across the skid (bottom flange, the box, top flange,
        // darksteel stiffeners on its faces and end plates), a stepped saddle box on each up to the drum and a knee web at
        // each end; they stand inboard of the flank heat pipes
        for (double x : CRADLE_X) {
            m.add(box(x - 2.0, DECK, 1.6, x + 2.0, DECK + 0.5, 30.4, "darksteel"));
            m.add(box(x - 1.6, DECK + 0.5, 1.8, x + 1.6, DECK + 2.6, 30.2, "steel"));
            m.add(box(x - 2.0, DECK + 2.6, 1.6, x + 2.0, DECK + 3.1, 30.4, "darksteel"));
            for (double z : new double[]{1.6, 30.2}) m.add(box(x - 1.8, DECK + 0.5, z, x + 1.8, DECK + 2.6, z + 0.2, "darksteel"));
            m.add(box(x - 1.6, DECK + 3.1, 6.5, x + 1.6, 9.6, 25.5, "steel"));
            m.add(box(x - 1.6, 9.6, 6.5, x + 1.6, 10.6, 9.0, "steel"));
            m.add(box(x - 1.6, 9.6, 23.0, x + 1.6, 10.6, 25.5, "steel"));
            m.add(box(x - 0.3, DECK + 3.1, 3.4, x + 0.3, 11.6, 6.5, "darksteel"));
            m.add(box(x - 0.3, DECK + 3.1, 25.5, x + 0.3, 11.6, 28.6, "darksteel"));
            for (double z : new double[]{4.5, 10.0, 16.0, 22.0, 27.5})
                for (double s : new double[]{-1, 1})
                    m.add(box(x + s * 1.6 - (s < 0 ? 0.3 : 0), DECK + 0.5, z - 0.25, x + s * 1.6 + (s > 0 ? 0.3 : 0), DECK + 2.6, z + 0.25, "darksteel"));
        }
        // the drum, its dished heads, a seam band at each head's joint and two between
        m.add(prism('x', CY, CZ, R, X0, X1, "rivet", N, false, false));
        m.add(prism('x', CY, CZ, R - HEAD, X0 - HEAD, X0, "rivet", N, R, true, false, Double.NaN));
        m.add(prism('x', CY, CZ, R, X1, X1 + HEAD, "rivet", N, R - HEAD, false, true, Double.NaN));
        for (double x : new double[]{X0, 12.0, 19.6, X1 - 0.4}) m.add(prism('x', CY, CZ, R + 0.15, x, x + 0.4, "darksteel", N, false, false));
        // the heat plates on the skid's back and flanks, each with a heat-tinted shoe on its top, and from the shoe a heat
        // pipe: on the flanks up beside the cradle's end and in to the drum's lower flank, at the back up and in to the
        // back head; a darksteel flange where each meets the drum
        double hz = HEAT_Z, hb = 32 - HEAT_Z, fy = 12.5;
        for (double u : new double[]{8, 24}) {
            m.addAll(heatPlate('x', 32, u, 0));
            m.addAll(heatPlate('z', 0, u, 0));
            m.addAll(heatPlate('z', 32, u, 0));
            m.add(box(u - 2.4, DECK - 0.6, 0.4, u + 2.4, DECK + 1.2, hz + 1.9, "hotcopper"));
            m.add(box(u - 2.4, DECK - 0.6, 32 - hz - 1.9, u + 2.4, DECK + 1.2, 31.6, "hotcopper"));
            m.add(box(hb - 1.9, DECK - 0.6, u - 2.4, 31.6, DECK + 1.2, u + 2.4, "hotcopper"));
            m.addAll(heatPipe(new double[]{u, DECK, hz, u, fy - 1.5, hz, u, fy, hz + 1.5, u, fy, 7.0}));
            m.addAll(heatPipe(new double[]{u, DECK, 32 - hz, u, fy - 1.5, 32 - hz, u, fy, 30.5 - hz, u, fy, 25.0}));
            m.add(prism('z', u, fy, HEAT_R + 0.6, 4.6, 5.4, "darksteel", 10));
            m.add(prism('z', u, fy, HEAT_R + 0.6, 26.6, 27.4, "darksteel", 10));
            m.addAll(heatPipe(new double[]{hb, DECK, u, hb, 7.6, u, hb - 2.0, 9.6, u, 26.5, 9.6, u}));
            m.add(prism('x', 9.6, u, HEAT_R + 0.6, 27.8, 28.6, "darksteel", 10));
        }
        double crest = CY + R * Math.cos(Math.PI / N);
        // the steam dome on the crest over the rear blocks, its header across to a riser at each steam flange (as the
        // Boiler's: neck, flange at the top face, bolts under it), each riser stayed down to the drum
        m.add(prism('y', STEAM_X, CZ, 4.1, crest - 0.6, crest + 0.2, "darksteel", 12));
        m.add(prism('y', STEAM_X, CZ, 3.5, crest - 1.0, 30.2, "rivet", 12, false, false));
        m.add(prism('y', STEAM_X, CZ, 3.5, 30.2, 31.0, "rivet", 12, 2.3, false, true, Double.NaN));
        m.add(prism('z', STEAM_X, 29.6, 0.9, STEAM_Z[0], STEAM_Z[1], "steel", 8, false, false));
        for (double z : STEAM_Z) {
            m.add(prism('y', STEAM_X, z, 1.4, 28.4, 30.0, "steel", 12));
            m.add(prism('y', STEAM_X, z, 2.0, 30.0, 31.0, "steel", 12, true, false));
            m.add(prism('y', STEAM_X, z, 2.75, 31.0, 32.0, "steel", 12));
            m.addAll(boltRing('y', STEAM_X, z, 2.35, 30.8, 31.0, 6, "brass", 0.2, 0));
            double shoulder = CY + Math.sqrt(R * R - (z - CZ) * (z - CZ));
            m.add(prism('y', STEAM_X, z, 0.5, shoulder - 1.0, 28.4, "darksteel", 8, false, false));
        }
        // the safety-valve dome on the crest over the front blocks: a brass valve, its bonnet, a lever out to the front
        // with its weight
        double sx = 10.0;
        m.add(prism('y', sx, CZ, 2.9, crest - 0.6, crest + 0.2, "darksteel", 12));
        m.add(prism('y', sx, CZ, 2.4, crest - 1.0, 29.6, "rivet", 12, false, true));
        m.add(prism('y', sx, CZ, 1.0, 29.6, 30.8, "brass", 10, false, true));
        m.add(prism('y', sx, CZ, 0.5, 30.8, 31.5, "darksteel", 8, false, true));
        m.add(box(5.6, 31.0, CZ - 0.2, sx + 0.6, 31.3, CZ + 0.2, "darksteel"));
        m.add(box(5.4, 30.3, CZ - 0.7, 6.8, 31.7, CZ + 0.7, "castiron"));
        // the front head: a manway (cover, bolts, yoke and nut) low in the middle, the brass nameplate above it
        double my = 12.5;
        m.add(prism('x', my, CZ, 2.6, 1.9, X0 - HEAD + 0.1, "darksteel", 12));
        for (int k = 0; k < 8; k++) {
            double a = 2 * Math.PI * (k + 0.5) / 8;
            m.add(prism('x', my + 2.2 * Math.cos(a), CZ + 2.2 * Math.sin(a), 0.2, 1.75, 1.9, "brass", 6, true, false));
        }
        m.add(box(1.4, my - 0.4, CZ - 3.0, 1.9, my + 0.4, CZ + 3.0, "darksteel"));
        m.add(prism('x', my, CZ, 0.5, 1.1, 1.4, "darksteel", 6, true, false));
        m.add(box(2.3, 19.0, 12.5, X0 - HEAD + 0.1, 22.0, 19.5, "brass"));
        m.add(box(2.2, 20.6, 13.3, 2.3, 21.0, 18.7, "darksteel"));
        m.add(box(2.2, 19.8, 14.0, 2.3, 20.2, 18.0, "darksteel"));
        // the gauge glass on the front head between two brass cocks, guard rods; the level transmitter's box on the deck
        // behind the signal plate, and its line up the head to the lower cock
        double gz = GAUGE[2], g0 = GAUGE[1], g1 = GAUGE[1] + GLASS;
        for (double y : new double[]{g0 - 0.6, g1}) m.add(box(0.5, y, gz - 0.85, X0 - HEAD + 0.1, y + 0.6, gz + 0.85, "brass"));
        m.add(box(0.55, g0 - 0.3, gz - 0.75, 0.7, g1 + 0.3, gz - 0.6, "darksteel"));
        m.add(box(0.55, g0 - 0.3, gz + 0.6, 0.7, g1 + 0.3, gz + 0.75, "darksteel"));
        m.add(prism('y', GAUGE[0], gz, 0.3, g0, g1, "sightglass", 6, false, false));
        m.addAll(signalPlate());
        m.add(box(0.85, 0, 6.5, 1.2, 3.5, 9.5, "steel", "nsu"));
        m.add(box(1.0, DECK, 7.0, 2.8, DECK + 1.6, 10.2, "darksteel"));
        m.add(prism('y', 2.3, gz, 0.25, DECK + 1.6, g0 - 0.6, "darksteel", 6, false, false));
        // the pressure dial on its siphon stalk out of the front head's top (its needle: part needle)
        m.add(prism('y', DIAL[0] + 0.25, DIAL[2], 0.22, 23.5, DIAL[1] - DIAL_R + 0.4, "brass", 6, false, false));
        m.addAll(dial(DIAL[0], DIAL[1], DIAL[2], DIAL_R, "xn", 0.5).subList(0, 2));
        // the feedwater line: the flange at the front face and a reducer, a copper elbow round to the front right corner,
        // up through the check valve (handwheel to the right) and back along the flank into the drum
        m.add(prism('x', FEED_Y, FEED_Z, 2.75, 0.0, 1.0, "steel", 12));
        m.add(prism('x', FEED_Y, FEED_Z, 2.0, 1.0, 1.4, "steel", 12, false, true));
        m.add(elbow(FX, FEED_Y, FEED_Z));
        m.add(prism('z', FX, FEED_Y, 0.7, FEED_Z + 0.6, FZ - 0.6, "copper", 8, false, false));
        m.add(elbow(FX, FEED_Y, FZ));
        double vy = 12.8;
        m.add(prism('y', FX, FZ, 0.7, FEED_Y + 0.6, vy - 0.9, "copper", 8, false, false));
        m.add(box(FX - 1.0, vy - 1.2, FZ - 1.0, FX + 1.0, vy + 1.2, FZ + 1.0, "castiron"));
        m.add(prism('z', FX, vy, 0.25, FZ + 1.0, FZ + 1.5, "darksteel", 6, false, false));
        m.addAll(handwheel(FX, vy, FZ + 1.6, 0.9, 'z'));
        m.add(prism('y', FX, FZ, 0.7, vy + 0.9, FTOP - 0.6, "copper", 8, false, false));
        m.add(elbow(FX, FTOP, FZ));
        m.add(prism('x', FTOP, FZ, 0.7, FX + 0.6, 6.4, "copper", 8, false, false));
        m.add(elbow(7.0, FTOP, FZ));
        // the blowdown at the back head's foot: a stub, its cock (handwheel on top) and the drain down to the deck
        m.add(prism('x', 8.0, CZ, 0.5, X1 + HEAD - 0.3, 30.6, "steel", 8, false, false));
        m.add(box(30.4, 7.2, CZ - 0.8, 31.6, 8.8, CZ + 0.8, "castiron"));
        m.add(prism('y', 31.0, CZ, 0.5, DECK - 0.1, 7.2, "steel", 8, false, false));
        m.add(prism('y', 31.0, CZ, 0.2, 8.8, 9.5, "darksteel", 6, false, false));
        m.addAll(handwheel(31.0, 9.6, CZ, 0.7, 'y'));
        return m;
    }

    static List<Mesh> level() {
        // the water round the empty glass tube, the glass's whole height (the render scales it to the level: Boiler.level)
        return List.of(prism('y', GAUGE[0], GAUGE[2], 0.4, GAUGE[1], GAUGE[1] + GLASS, "gaugewater", 6, false, true));
    }

    static List<Mesh> needle() {
        double x = DIAL[0], y = DIAL[1], z = DIAL[2];
        return List.of(obox(v(x - 0.15, y + 0.75, z), v(1, 0, 0), v(0, 1, 0), v(0, 0, 1), 0.04, 0.8, 0.09, "darksteel"),
            prism('x', y, z, 0.25, x - 0.2, x - 0.1, "darksteel", 6, true, false));
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("largeboiler", OilKit.PAINT_BOILER);
        n.part("main", main());
        n.part("level", level());
        n.part("needle", needle());
        n.origin("level", GAUGE[0], GAUGE[1], GAUGE[2]).origin("needle", DIAL[0], DIAL[1], DIAL[2]);
        return List.of(n);
    }
}
