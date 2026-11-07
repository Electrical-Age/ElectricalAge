package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The Industrial Resin Collector (Baughn 2026-11-07, the rubber chain): a powered tapping rig. Its back (x = 16) stands
 * against a tree trunk: two struts end in rubber clamp pads on the bark, a tapping head (a small motor and its knife)
 * cuts the bark between them, and a gutter under the cut leads the latex into the blue cabinet, whose sight glass
 * shows it. A pump on the lid lifts it to the red OUT flange on top (the port, top face centre), its label in miniature.
 * Power on the two side faces; status LEDs on the front. Drawn by IndustrialResinCollectorRender. Front XN (x = 0).
 */
public final class ResinCollector {
    private ResinCollector() {
    }

    static List<Mesh> body(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 2.0, 11.5, 1.0, 14.0, "darksteel"));                                         // skid
        m.add(box(1.5, 1.0, 3.0, 10.5, 8.5, 13.0, "paint"));                                           // cabinet
        m.add(box(1.3, 8.5, 2.8, 10.7, 9.0, 13.2, "darksteel"));                                       // lid
        // the sight glass: latex in its lower part, the empty glass above it
        m.add(box(1.4, 5.2, 5.6, 1.5, 7.6, 7.4, "glass", "w").free("the sight glass, a decal on the cabinet"));
        m.add(box(1.4, 2.0, 5.6, 1.5, 5.2, 7.4, "latex", "w").free("latex in the sight glass"));
        // the struts to the trunk, the clamp pads on the bark
        for (double z : new double[]{4.5, 11.5}) {
            m.add(box(10.5, 6.0, z - 0.5, 15.4, 7.0, z + 0.5, "steel"));
            m.add(box(15.4, 4.0, z - 1.2, 15.95, 10.0, z + 1.2, "rubber"));
        }
        // the tapping head: its bracket from the lid, a small motor along x, the knife at the bark
        m.add(box(11.8, 9.0, 7.4, 12.6, 11.5, 8.6, "darksteel"));
        m.add(box(10.7, 8.6, 7.4, 12.6, 9.0, 8.6, "darksteel"));
        m.add(prism('x', 11.0, 8.0, 1.1, 12.6, 14.8, "steel", 10));
        m.add(prism('x', 11.0, 8.0, 1.25, 13.2, 13.6, "paint", 10));
        m.add(box(14.8, 10.2, 7.2, 15.9, 10.6, 8.8, "steel"));                                         // knife
        // the gutter under the cut, its drip pipe into the cabinet
        m.add(box(14.6, 7.2, 5.0, 15.4, 7.6, 11.0, "steel"));
        m.add(box(14.6, 7.6, 5.0, 15.4, 8.0, 5.2, "steel"));
        m.add(box(14.6, 7.6, 10.8, 15.4, 8.0, 11.0, "steel"));
        m.add(prism('x', 7.4, 8.0, 0.3, 10.5, 14.6, "darksteel", 8, false, false));
        // the pump on the lid and the riser to the OUT flange on top (the port: top face centre)
        m.add(box(4.0, 9.0, 6.0, 7.0, 10.6, 10.0, "castiron"));
        m.add(prism('z', 5.5, 9.8, 0.5, 10.0, 11.0, "darksteel", 8, true, false));
        m.add(box(7.0, 9.4, 7.6, 8.4, 10.2, 8.4, "darksteel"));
        m.add(prism('y', 8.0, 8.0, 0.5, 9.0, 15.2, "darksteel", 8, false, false));
        m.add(Mc.tube('y', 8.0, 8.0, 0.5, 0.7, 12.0, 12.4, "steel", 8));                                  // a clamp on it
        m.add(prism('y', 8.0, 8.0, 1.0, 15.2, 15.5, "paint_red", 12));
        m.add(prism('y', 8.0, 8.0, 1.45, 15.5, 15.75, "steel", 12));
        m.addAll(OilKit.boltRing('y', 8.0, 8.0, 1.25, 15.75, 15.85, 4, "darksteel", 0.17, Math.PI / 4));
        m.add(MachineKit.flangeLabelUp(15.75, 8.0, 8.0, 0.7, "label_out"));
        // rivets on the cabinet's front, the control box with its LEDs, a nameplate
        MachineKit.rivetsX(m, 3.6, 12.4, 8.0, 1.5, -1, 5);
        MachineKit.rivetsX(m, 3.6, 5.0, 1.6, 1.5, -1, 2);
        leds.addAll(MachineKit.controlBox(m, 1.5, 6.2, 10.6));
        m.add(box(1.4, 2.2, 8.4, 1.5, 3.4, 10.6, "brass", "w").free("a nameplate on the cabinet"));
        // power: floor terminals on the side faces, junction boxes against the cabinet
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 3.0, "castiron"));
        m.add(box(4.5, 1.0, 13.0, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("resincollector230", OilKit.PAINT_200V);
        List<List<Mesh>> l = new ArrayList<>();
        n.part("main", body(l));
        n.part("ledpower", l.get(0));
        n.part("ledrun", l.get(1));
        return List.of(n);
    }
}
