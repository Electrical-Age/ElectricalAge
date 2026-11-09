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
 * The Steam Cracker (the rubber chain's oil route, 2026-11-07; 400 V, so the tier's orange): a riveted cast-iron firebox
 * on a skid, its electric heater bus run in from junction boxes at the power terminals on both side faces; two
 * peepholes that glow while it cracks (part glow); on top the transfer-line exchanger, a vertical drum the coil's outlet
 * crosses into. Two blue IN flanges on the back (gasoline "C8" at z 4, steam "H2O" at z 12) and two red OUT flanges on the
 * front (butadiene "C4H6" at z 4, natural gas "CH4" at z 12), all 4 px up, their pipes into the firebox, labelled in
 * miniature (IN/OUT over the fluid). Status LEDs in a box between the peepholes. Drawn by SteamCrackerRender. Front XN.
 */
public final class SteamCracker {
    private SteamCracker() {
    }

    static {
        Palette.put("label_in_c8", 52, 102, 176, 0, Compressor.text2("IN", "C8"));
        Palette.put("label_in_h2o", 52, 102, 176, 0, Compressor.text2("IN", "H2O"));
        Palette.put("label_out_c4h6", 176, 48, 40, 0, Compressor.text2("OUT", "C4H6"));
        Palette.put("label_out_ch4", 176, 48, 40, 0, Compressor.text2("OUT", "CH4"));
    }

    static final double FY = 4.0, ZA = 4.0, ZB = 12.0;
    /** The firebox. */
    static final double X0 = 4.5, X1 = 11.5, Y1 = 12.0, Z0 = 3.0, Z1 = 13.0;

    /** A blue IN flange on the back (x = 16) at z, its pipe from the firebox. */
    static List<Mesh> inFlange(double z, String label) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', FY, z, 0.45, X1, 15.5, "darksteel", 8, false, false));
        m.add(prism('x', FY, z, 1.0, 15.5, 15.7, "paint_blue", 12));
        m.add(prism('x', FY, z, 1.45, 15.7, 15.9, "steel", 12));
        m.addAll(OilKit.boltRing('x', FY, z, 1.25, 15.9, 15.98, 4, "darksteel", 0.17, Math.PI / 4));
        m.add(MachineKit.flangeLabel(15.9, FY, z, 1.0, label));
        return m;
    }

    /** A red OUT flange on the front (x = 0) at z, its pipe from the firebox. */
    static List<Mesh> outFlange(double z, String label) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', FY, z, 0.45, 0.5, X0, "darksteel", 8, false, false));
        m.add(prism('x', FY, z, 1.0, 0.3, 0.5, "paint_red", 12));
        m.add(prism('x', FY, z, 1.45, 0.1, 0.3, "steel", 12));
        m.addAll(MachineKit.mirroredBoltsX(0.02, 0.1, FY, z));
        m.add(MachineKit.flangeLabelFront(0.04, FY, z, 1.0, label));
        return m;
    }

    static List<Mesh> body(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.5, 0, 0.85, 14.5, 1.0, 15.15, "darksteel"));                                       // skid
        m.add(box(X0, 1.6, Z0, X1, Y1, Z1, "castiron"));                                                // firebox, on its foot band
        m.add(box(X0 - 0.2, Y1, Z0 - 0.2, X1 + 0.2, Y1 + 0.6, Z1 + 0.2, "paint"));                      // its roof, tier-painted
        m.add(box(X0 - 0.2, 1.0, Z0 - 0.2, X1 + 0.2, 1.6, Z1 + 0.2, "darksteel"));                      // its foot band
        for (double y : new double[]{5.0, 8.5})                                                          // buckstays on the sides
            for (double z : new double[]{Z0, Z1}) {
                double za = z == Z0 ? Z0 - 0.3 : Z1, zb = z == Z0 ? Z0 : Z1 + 0.3;
                m.add(box(X0 + 0.3, y - 0.25, za, X1 - 0.3, y + 0.25, zb, "darksteel"));
            }
        MachineKit.rivetsZ(m, X0 + 0.8, X1 - 0.8, 10.8, Z1, 1, 5);
        MachineKit.rivetsZ(m, X0 + 0.8, X1 - 0.8, 10.8, Z0, -1, 5);
        MachineKit.rivetsX(m, Z0 + 0.8, Z1 - 0.8, 11.3, X0, -1, 6);
        // the peepholes' brass rims (their glass is the glow part)
        for (double z : new double[]{5.2, 10.8}) m.add(Mc.tube('x', 8.0, z, 0.55, 0.85, X0 - 0.2, X0, "brass", 10));
        leds.addAll(MachineKit.controlBox(m, X0, 8.0, 8.0));
        // the transfer-line exchanger on the roof, and the coil's crossover into it
        m.add(prism('y', 8.0, 8.0, 1.8, Y1 + 0.6, 14.6, "steel", 16));
        m.add(prism('y', 8.0, 8.0, 1.8, 14.6, 15.6, "steel", 16, 1.0, false, true, Double.NaN));
        for (double y : new double[]{13.4, 14.3}) m.add(Mc.tube('y', 8.0, 8.0, 1.8, 1.95, y - 0.12, y + 0.12, "darksteel", 16));
        m.add(prism('y', 10.6, 5.0, 0.45, Y1 + 0.6, 14.0, "darksteel", 8, false, false));                // crossover: up from the roof
        m.add(prism('x', 14.0, 5.0, 0.45, 8.0, 10.6, "darksteel", 8, false, false).free("the crossover's run into the drum, through its wall"));
        m.add(prism('y', 10.6, 5.0, 0.6, 13.6, 14.4, "darksteel", 8).free("the crossover's elbow"));
        m.add(prism('y', 8.0, 8.0, 0.3, 15.5, 15.75, "brass", 6));                                     // a vent on the drum
        m.add(prism('y', 8.0, 8.0, 0.45, 15.75, 15.95, "red", 6));
        // the flanges
        m.addAll(inFlange(ZA, "label_in_c8"));
        m.addAll(inFlange(ZB, "label_in_h2o"));
        m.addAll(outFlange(ZA, "label_out_c4h6"));
        m.addAll(outFlange(ZB, "label_out_ch4"));
        // power: floor terminals on the side faces, junction boxes, heater bus conduits into the firebox
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.0, "castiron"));
        m.add(box(4.5, 1.0, 14.0, 11.5, 4.5, 15.15, "castiron"));
        for (double[] zz : new double[][]{{2.0, Z0 - 0.3}, {Z1 + 0.3, 14.0}})
            for (double x : new double[]{6.5, 9.5}) m.add(box(x - 0.5, 2.6, zz[0], x + 0.5, 3.6, zz[1], "copper"));
        return m;
    }

    static List<Mesh> glow() {
        List<Mesh> g = new ArrayList<>();
        for (double z : new double[]{5.2, 10.8}) g.add(prism('x', 8.0, z, 0.55, X0 - 0.1, X0, "glow", 10, true, false).free("a peephole's glass, set in the firebox wall"));
        return g;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("steamcracker", "paint_orange");
        List<List<Mesh>> l = new ArrayList<>();
        n.part("main", body(l));
        n.part("ledpower", l.get(0));
        n.part("ledrun", l.get(1));
        n.part("toggle", l.get(2));
        n.part("glow", glow());
        return List.of(n);
    }
}
