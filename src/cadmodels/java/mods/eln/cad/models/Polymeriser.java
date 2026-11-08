package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.tex.Palette;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The Polymeriser (the rubber chain's oil route, 2026-11-07; 230 V blue): a jacketed stirred reactor on a skid, its
 * agitator driven from a motor and gearbox on a lantern frame over the domed head (part stirrer, the coupling and shaft,
 * turns about the vessel's axis while it runs: a red disc with a brass bar, so it is seen to turn); a Soul Dust hopper on the head's shoulder; a tray of Raw Rubber crumb at
 * its front foot; the butadiene's blue IN flange on the back (4 px up, centred, labelled); status LEDs over the jacket;
 * power on the two side faces. Drawn by PolymeriserRender. Front XN (x = 0).
 */
public final class Polymeriser {
    private Polymeriser() {
    }

    static {
        Palette.put("soul_dust", 92, 70, 56, 12, null);
        Palette.put("rubber_crumb", 214, 196, 150, 10, null);
    }

    static final double CX = 8.5, CZ = 8.0, R = 3.6, TOP = 10.5, HEAD = 11.8;

    static List<Mesh> body(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 0.85, 14.5, 1.0, 15.15, "darksteel"));                                       // skid
        m.add(prism('y', CX, CZ, R, 1.0, TOP, "steel", 20));                                           // the vessel
        m.add(prism('y', CX, CZ, R, TOP, HEAD, "steel", 20, 2.0, false, true, Double.NaN));            // its domed head
        m.add(Mc.tube('y', CX, CZ, R, R + 0.25, 3.0, 8.0, "paint", 20));                               // the jacket
        for (double y : new double[]{3.3, 7.7}) m.add(Mc.tube('y', CX, CZ, R + 0.25, R + 0.4, y - 0.15, y + 0.15, "darksteel", 20));
        m.add(Mc.tube('y', CX, CZ, R, R + 0.15, 1.0, 1.4, "darksteel", 20));                           // the foot ring
        // the drive: four lantern posts on the head, the gearbox, the motor standing on it, its terminal box
        for (double[] p : new double[][]{{-1.1, -1.1}, {1.1, -1.1}, {-1.1, 1.1}, {1.1, 1.1}})
            m.add(box(CX + p[0] - 0.2, HEAD, CZ + p[1] - 0.2, CX + p[0] + 0.2, 13.2, CZ + p[1] + 0.2, "darksteel"));
        m.add(box(CX - 1.6, 13.2, CZ - 1.6, CX + 1.6, 14.2, CZ + 1.6, "castiron"));                    // gearbox
        m.add(prism('y', CX, CZ, 1.3, 14.2, 15.9, "paint", 14));                                       // motor
        for (double y : new double[]{14.7, 15.15, 15.6}) m.add(Mc.tube('y', CX, CZ, 1.3, 1.45, y - 0.1, y + 0.1, "darksteel", 14));
        m.add(box(CX + 1.3, 14.4, CZ - 0.5, CX + 1.9, 15.2, CZ + 0.5, "darksteel"));
        // the Soul Dust hopper on the head's shoulder, its dust showing
        m.add(box(5.2, 10.6, 9.4, 7.2, 12.4, 11.4, "darksteel"));
        m.add(box(5.4, 12.4, 9.6, 7.0, 12.5, 11.2, "soul_dust").free("Soul Dust heaped in the hopper"));
        // the Raw Rubber tray at the front foot, its chute from the vessel
        m.add(box(1.4, 1.0, 6.2, 4.8, 2.4, 9.8, "darksteel"));
        m.add(box(1.6, 2.4, 6.4, 4.6, 2.6, 9.6, "rubber_crumb").free("Raw Rubber crumb in the tray"));
        m.add(box(4.2, 2.6, 7.4, 5.2, 4.0, 8.6, "darksteel"));
        leds.addAll(MachineKit.controlBox(m, CX - R + 0.05, 9.2, CZ));
        // the butadiene's blue IN flange on the back, its pipe from the vessel
        m.add(prism('x', 4.0, 8.0, 0.45, CX + R - 0.2, 15.5, "darksteel", 8, false, false));
        m.add(prism('x', 4.0, 8.0, 1.0, 15.5, 15.7, "paint_blue", 12));
        m.add(prism('x', 4.0, 8.0, 1.45, 15.7, 15.9, "steel", 12));
        m.addAll(OilKit.boltRing('x', 4.0, 8.0, 1.25, 15.9, 15.98, 4, "darksteel", 0.17, Math.PI / 4));
        m.add(MachineKit.flangeLabel(15.9, 4.0, 8.0, 0.7, "label_in"));
        // power: floor terminals on the side faces, junction boxes, conduits to the vessel
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.0, "castiron"));
        m.add(box(4.5, 1.0, 14.0, 11.5, 4.5, 15.15, "castiron"));
        m.add(box(8.0, 2.0, 2.0, 9.0, 2.8, CZ - R + 0.3, "copper"));
        m.add(box(8.0, 2.0, CZ + R - 0.3, 9.0, 2.8, 14.0, "copper"));
        return m;
    }

    /** The agitator's shaft between the head and the gearbox, its coupling a red disc with a brass bar across it. */
    static List<Mesh> stirrer() {
        List<Mesh> s = new ArrayList<>();
        s.add(prism('y', CX, CZ, 0.3, HEAD, 13.2, "steel", 10));
        s.add(prism('y', CX, CZ, 0.85, 12.2, 12.5, "red", 14).free("the coupling's disc, on the agitator shaft"));
        s.add(box(CX - 0.8, 12.5, CZ - 0.15, CX + 0.8, 12.75, CZ + 0.15, "brass").free("the coupling's driving bar, on its disc"));
        return s;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("polymeriser", OilKit.PAINT_200V);
        List<List<Mesh>> l = new ArrayList<>();
        n.part("main", body(l));
        n.part("ledpower", l.get(0));
        n.part("ledrun", l.get(1));
        n.part("stirrer", stirrer());
        n.origin("stirrer", CX, 12.5, CZ);
        return List.of(n);
    }
}
