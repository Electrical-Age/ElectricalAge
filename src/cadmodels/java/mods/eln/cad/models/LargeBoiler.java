package mods.eln.cad.models;

import org.erisia.cad.Mesh;

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
 * The 2x2x2 Large Boiler (todo #472; its own model, todo #522, Baughn 2026-11-18): a big riveted steam drum along x, its
 * lower half bedded in a cast-iron heat-collector casing that fills the footprint. Drawn by BoilerRender from its origin
 * block (the front bottom left), full size: 0..32 px on each axis, front XN (x = 0), right z+, as LargeBoilerElement's
 * local frame (block x 0 front / 1 back, y 0 low / 1 high, z 0 left / 1 right). Its connections, where the element
 * has them (each at its block face's centre, as the ports' spots are):
 * <ul>
 * <li>thermal: a copper heat plate on each of the twelve block faces of the back and both flanks, at each block's
 * floor (the lower blocks at y 0, the upper at y 16);</li>
 * <li>steam out: an iron pipe's steam flange on a dome on top of each rear top block (x 24, z 8 and z 24; top at
 * y 32);</li>
 * <li>feedwater in: a flange on the front of the bottom right block (y 8, z 24) behind its check valve;</li>
 * <li>the level signal: a signal plate at the floor of the front of the origin block (z 8);</li>
 * <li>the gauge glass (part level, scaled by the render about its foot) and the pressure dial (part needle) on the
 * casing's front, upper left.</li>
 * </ul>
 */
public final class LargeBoiler {
    private LargeBoiler() {
    }

    /** The drum: axis (y, z), radius, its cylinder's ends (x). The casing's top. */
    static final double CY = 20.5, CZ = 16.0, R = 10.0, X0 = 3.0, X1 = 29.0, TOP = 20.5;
    /** The feed flange's centre on the front (y, z) and the steam flanges' on top (x; z each). */
    static final double FEED_Y = 8.0, FEED_Z = 24.0, STEAM_X = 24.0;
    static final double[] STEAM_Z = {8.0, 24.0};
    /** BoilerRender scales "level" in y about the column's foot and turns "needle" about x at the dial's centre. */
    static final double[] GAUGE = {0.6, 9.5, 5.0}, DIAL = {0.4, 16.0, 11.0};

    /**
     * A thermal contact: a heat-tinted copper plate 8 px wide and 4 high at (centre u, floor y) on the face at w (0 or
     * 32) across axis 'x' or 'z', 0.1 px short of the block face, bedded 0.2 px into the casing, with four bolt heads.
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

    static List<Mesh> main() {
        List<Mesh> m = new ArrayList<>();
        // the casing (the heat collector), its top plate, four darksteel corner posts and a plinth band round its foot
        // (no tops on the casing and posts: the top plate covers them; they z-fought in its plane)
        m.add(box(1.0, 0, 1.0, 31.0, TOP, 31.0, "castiron", "nsew"));
        m.add(box(0.6, TOP, 0.6, 31.4, TOP + 0.6, 31.4, "darksteel", "nsewud"));
        for (double[] c : new double[][]{{0.6, 0.6}, {29.4, 0.6}, {0.6, 29.4}, {29.4, 29.4}})
            m.add(box(c[0], 0, c[1], c[0] + 2.0, TOP, c[1] + 2.0, "darksteel", "nsew"));
        // the heat plates: the back (x = 32) and both flanks (z = 0, z = 32), each block face's floor
        for (double y : new double[]{0, 16})
            for (double u : new double[]{8, 24}) {
                m.addAll(heatPlate('x', 32, u, y));
                m.addAll(heatPlate('z', 0, u, y));
                m.addAll(heatPlate('z', 32, u, y));
            }
        // the drum, its dished heads, three seam bands, a manhole on top between the domes' line and the front
        m.add(prism('x', CY, CZ, R, X0, X1, "rivet", 20, false, false));
        m.add(prism('x', CY, CZ, R - 2.5, X0 - 1.5, X0, "rivet", 20, R, true, false, Double.NaN));
        m.add(prism('x', CY, CZ, R, X1, X1 + 1.5, "rivet", 20, R - 2.5, false, true, Double.NaN));
        for (double x : new double[]{9.0, 16.0, 23.0}) m.add(prism('x', CY, CZ, R + 0.15, x, x + 0.5, "darksteel", 20, false, false));
        double top = CY + R * Math.cos(Math.PI / 20);
        m.add(prism('y', 10.5, CZ, 2.0, top - 0.8, top + 0.6, "darksteel", 10));
        m.addAll(boltRing('y', 10.5, CZ, 1.6, top + 0.6, top + 0.8, 6, "brass", 0.2, 0));
        // the steam domes on the rear top blocks, each to an iron pipe's steam flange at the top face (as the Boiler's)
        for (double z : STEAM_Z) {
            double y0 = CY + Math.sqrt(R * R - (z - CZ) * (z - CZ)) - 1.0;
            m.add(prism('y', STEAM_X, z, 2.2, y0, 29.5, "rivet", 12, false, true));
            m.add(prism('y', STEAM_X, z, 2.0, 29.5, 31.0, "steel", 12, false, false));
            m.add(prism('y', STEAM_X, z, 2.75, 31.0, 32.0, "steel", 12));
            m.addAll(boltRing('y', STEAM_X, z, 2.35, 30.8, 31.0, 6, "brass", 0.2, 0));
        }
        // the feedwater inlet on the front of the bottom right block: its flange at the face, a reducer, the check valve
        // with its handwheel on top, the pipe into the casing
        m.add(prism('x', FEED_Y, FEED_Z, 2.75, 0.0, 1.0, "steel", 12, true, false));
        m.add(prism('x', FEED_Y, FEED_Z, 2.0, 1.0, 1.4, "steel", 12, false, false));
        m.add(obox(v(2.2, FEED_Y, FEED_Z), v(1, 0, 0), v(0, 1, 0), v(0, 0, 1), 0.8, 1.3, 1.3, "castiron"));
        m.add(prism('y', 2.2, FEED_Z, 0.25, FEED_Y + 1.3, FEED_Y + 2.3, "darksteel", 6, false, false));
        m.addAll(handwheel(2.2, FEED_Y + 2.4, FEED_Z, 0.9, 'y'));
        // the level signal's plate at the front of the origin block, on a backing into the casing
        m.addAll(signalPlate());
        m.add(box(0.85, 0, 6.5, 1.2, 3.5, 9.5, "steel", "nsu"));
        // the gauge glass between two brass cocks on the casing's front, guard rods; the dial on a boss (its needle: part needle)
        double gz = GAUGE[2];
        for (double y : new double[]{GAUGE[1] - 0.6, GAUGE[1] + 5.15}) m.add(box(0.15, y, gz - 0.55, 1.2, y + 0.6, gz + 0.55, "brass"));
        m.add(box(0.1, GAUGE[1], gz - 0.65, 0.25, GAUGE[1] + 5.15, gz - 0.5, "darksteel"));
        m.add(box(0.1, GAUGE[1], gz + 0.5, 0.25, GAUGE[1] + 5.15, gz + 0.65, "darksteel"));
        m.add(prism('x', DIAL[1], DIAL[2], 0.9, 0.9, 1.2, "darksteel", 10));
        m.addAll(dial(DIAL[0], DIAL[1], DIAL[2], 1.6, "xn", 0.5).subList(0, 2));
        // the nameplate on the left flank, between the lower heat plates
        m.add(box(14.6, 1.5, 0.9, 17.4, 3.0, 1.0, "brass", "n").free("a nameplate decal, 0.1 px proud of the casing's face"));
        return m;
    }

    static List<Mesh> level() {
        // half the glass's height (the render stretches it, x 0..2)
        return List.of(prism('y', GAUGE[0], GAUGE[2], 0.4, GAUGE[1], GAUGE[1] + 2.55, "water", 6));
    }

    static List<Mesh> needle() {
        double x = DIAL[0], y = DIAL[1], z = DIAL[2];
        return List.of(obox(v(x - 0.15, y + 0.45, z), v(1, 0, 0), v(0, 1, 0), v(0, 0, 1), 0.04, 0.6, 0.08, "darksteel"),
            prism('x', y, z, 0.2, x - 0.2, x - 0.1, "darksteel", 6, true, false));
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
