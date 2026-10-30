package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.signalPlate;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Submersible pump (50V / 200V): a motor in a can standing in water, an intake screen round its foot, a pump head on
 * top and a riser up to an iron pipe's flange at the centre of the top face (SubmersiblePumpElement's outlet port).
 * Drawn by SubmersiblePumpRender -> SubmersiblePumpDescriptor.draw() (part main), after front.glRotateXnRef().
 * Connections: the speed signal on the front (x = 0), power on both flanks (z = 0 and z = 16), all at the floor on
 * terminal plates; the outlet is the flange on top. Front XN (x = 0):
 * <ul>
 * <li>skid: cast-iron foot plate with three terminal plates, each wired to the can by a conduit; a lifting eye at the back;</li>
 * <li>can: tier-painted motor can with seam bands; the intake screen (louvres) just above the skid;</li>
 * <li>head: steel pump head, the riser and the flange with its bolts.</li>
 * </ul>
 */
public final class SubmersiblePump {
    private SubmersiblePump() {
    }

    /** r: can radius; top: can top; head: pump head radius. */
    record Tier(String name, String paint, double r, double top, double head) {
    }

    static final List<Tier> TIERS = List.of(new Tier("50v", OilKit.PAINT_50V, 3.5, 9.5, 2.75), new Tier("200v", OilKit.PAINT_200V, 4.25, 10.5, 3.25));
    static final double C = 8.0;

    static List<Mesh> pump(Tier t) {
        List<Mesh> m = new ArrayList<>();
        double r = t.r;
        // skid, terminal plates (signal on the front, power on the flanks) and their conduits to the can
        m.add(box(2.0, 0, 2.0, 14.0, 1.25, 14.0, "castiron", "nsewu"));
        m.add(box(14.0, 0, 6.5, 16.0, 0.75, 9.5, "castiron", "nsewu"));          // tongue with the lifting eye, back
        m.add(box(14.75, 0.75, 7.5, 15.5, 2.25, 8.5, "darksteel", "nsewu"));
        for (int k : new int[]{1, 3}) {
            List<Mesh> tp = terminalPlate();
            tp.add(box(0.85, 1.9, 7.4, C - r + 0.2, 2.6, 8.6, "darksteel"));
            m.addAll(turned(tp, k));
        }
        List<Mesh> sp = signalPlate();                                             // (the speed signal)
        sp.add(box(0.85, 2.0, 7.7, C - r + 0.2, 2.5, 8.3, "darksteel"));
        m.addAll(turned(sp, 0));
        // intake screen, can, seam bands
        m.add(prism('y', C, C, r - 0.3, 1.25, 1.5, "darksteel", 12));
        m.add(prism('y', C, C, r, 1.5, 3.75, "louvre", 12, false, false));
        m.add(prism('y', C, C, r, 3.75, t.top, "paint", 12, false, true));
        for (double y : new double[]{3.75, t.top - 0.6}) m.add(prism('y', C, C, r + 0.15, y, y + 0.5, "darksteel", 12, false, false));
        // pump head, riser, flange (an iron pipe's) with bolts
        m.add(prism('y', C, C, t.head, t.top, t.top + 1.75, "steel", 12, false, true));
        m.add(prism('y', C, C, 2.0, t.top + 1.75, 15.0, "steel", 12, false, false));
        m.add(prism('y', C, C, 2.75, 15.0, 16.0, "steel", 12));
        m.addAll(boltRing('y', C, C, 2.35, 14.75, 15.0, 4, "brass", 0.22, 0));
        return m;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) {
            NodeModel n = new NodeModel("submersiblepump" + t.name, t.paint);
            n.part("main", pump(t));
            out.add(n);
        }
        return out;
    }
}
