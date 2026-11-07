package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The Desulfuriser (the rubber chain's oil route, 2026-11-07): a vertical pressure vessel on legs, its heater jacket
 * painted, lagging above it, a domed head with a relief valve; the cut comes in at the blue IN flange on the back and
 * leaves by the red OUT flange on the front (both 4 px up, centred, labelled in miniature); a sulfur drawer at its foot;
 * status LEDs in a box above the OUT flange; power on the two side faces. Drawn by DesulfuriserRender. Front XN (x = 0).
 */
public final class Desulfuriser {
    private Desulfuriser() {
    }

    static List<Mesh> body(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(2.0, 0, 0.85, 14.0, 1.0, 15.15, "darksteel"));                                       // base plate (under the junction boxes)
        // (the checker: legs at the corners stood clear of the vessel's foot; under it now)
        for (double[] p : new double[][]{{5.5, 5.5}, {10.5, 5.5}, {5.5, 10.5}, {10.5, 10.5}})          // legs
            m.add(box(p[0] - 0.5, 1.0, p[1] - 0.5, p[0] + 0.5, 3.0, p[1] + 0.5, "darksteel"));
        m.add(prism('y', 8.0, 8.0, 4.0, 3.0, 12.0, "steel", 20));                                      // the vessel
        m.add(prism('y', 8.0, 8.0, 4.0, 12.0, 14.0, "steel", 20, 2.0, false, true, Double.NaN));       // its domed head
        m.add(prism('y', 8.0, 8.0, 4.0, 3.0, 3.4, "steel", 20, 3.0, true, false, Double.NaN).free("the vessel's dished foot"));
        m.add(Mc.tube('y', 8.0, 8.0, 4.0, 4.3, 5.0, 8.0, "paint", 20));                                 // heater jacket
        m.add(Mc.tube('y', 8.0, 8.0, 4.0, 4.25, 8.4, 11.6, "lagging", 20));                             // lagging
        for (double y : new double[]{5.0, 8.0}) m.add(Mc.tube('y', 8.0, 8.0, 4.3, 4.45, y - 0.15, y + 0.15, "darksteel", 20));
        m.add(prism('y', 9.5, 8.0, 0.4, 13.8, 15.2, "brass", 8));                                       // relief valve
        m.add(prism('y', 9.5, 8.0, 0.65, 15.2, 15.8, "red", 8));
        // the flanges: blue IN on the back, red OUT on the front, their pipes into the vessel
        m.add(prism('x', 4.0, 8.0, 0.45, 11.6, 15.5, "darksteel", 8, false, false));
        m.add(prism('x', 4.0, 8.0, 1.0, 15.5, 15.7, "paint_blue", 12));
        m.add(prism('x', 4.0, 8.0, 1.45, 15.7, 15.9, "steel", 12));
        m.addAll(OilKit.boltRing('x', 4.0, 8.0, 1.25, 15.9, 15.98, 4, "darksteel", 0.17, Math.PI / 4));
        m.add(MachineKit.flangeLabel(15.9, 4.0, 8.0, 0.7, "label_in"));
        m.add(prism('x', 4.0, 8.0, 0.45, 0.5, 4.4, "darksteel", 8, false, false));
        m.add(prism('x', 4.0, 8.0, 1.0, 0.3, 0.5, "paint_red", 12));
        m.add(prism('x', 4.0, 8.0, 1.45, 0.1, 0.3, "steel", 12));
        m.addAll(MachineKit.mirroredBoltsX(0.02, 0.1, 4.0, 8.0));
        m.add(MachineKit.flangeLabelFront(0.04, 4.0, 8.0, 0.7, "label_out"));
        // the sulfur drawer at the front-right foot, its handle; the control box over the OUT flange
        m.add(box(1.2, 1.0, 9.6, 4.0, 2.8, 13.4, "darksteel"));
        m.add(box(1.4, 2.8, 9.8, 3.8, 3.0, 13.2, "sulfur").free("sulfur on the drawer"));
        m.add(box(0.8, 1.4, 10.9, 1.2, 2.2, 12.1, "steel"));
        leds.addAll(MachineKit.controlBox(m, 4.05, 9.5, 8.0));
        // power: floor terminals on the side faces, junction boxes to the base plate
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.0, "castiron"));
        m.add(box(4.5, 1.0, 14.0, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("desulfuriser", OilKit.PAINT_200V);
        List<List<Mesh>> l = new ArrayList<>();
        n.part("main", body(l));
        n.part("ledpower", l.get(0));
        n.part("ledrun", l.get(1));
        return List.of(n);
    }
}
