package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.tex.Palette;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.terminalPlate;
import static mods.eln.cad.models.OilKit.turned;

/**
 * The compressors (Baughn 2026-11-07, the four main machines restyled): presses. Drawn by CompressorDescriptor: main,
 * then part move shifted down in y by up to -tyon (its .txt: tyoff 0, tyon -0.125, so 2 px) as the work goes on, the
 * input item on the die and the output item beside it (positions in move's .txt: inx iny inz, outx outy outz, block
 * units from the centre). Power on the two z faces: floor terminal plates there. Front XN (x = 0).
 * <ul>
 * <li>compressora (48V, green): a bench press: a painted bed, two columns, a crown with a small gearbox, the ram and
 * platen coming down onto a die.</li>
 * <li>compressorb (230V, blue): a four-column hydraulic press with a crown cylinder and a moving platen, and a
 * hydraulic power pack at the back (tank, upright motor, pump, gauge). Its back panel carries two pipe flanges (Baughn
 * msg 1568615658370105405): blue IN low, red OUT higher, each with a label that spells it out.</li>
 * </ul>
 */
public final class Compressor {
    private Compressor() {
    }

    /** The ram's travel (px). */
    static final double TRAVEL = 2.0;

    // ---- 48V

    static List<Mesh> bodyA(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.5, 0, 1.5, 14.5, 1.0, 14.5, "darksteel"));                                         // plinth
        m.add(box(2.0, 1.0, 3.0, 14.0, 5.0, 13.0, "paint"));                                           // bed
        m.add(box(1.8, 5.0, 2.8, 14.2, 5.5, 13.2, "darksteel"));                                       // bed top plate
        m.add(box(6.0, 5.5, 6.0, 10.0, 6.5, 10.0, "steel"));                                           // die
        // (the checker: their feet lay in the tray's underside; no bottom caps, they stand on the bed)
        for (double z : new double[]{4.0, 12.0}) m.add(prism('y', 8.0, z, 0.8, 5.5, 13.5, "steel", 10, false, true));   // columns
        m.add(box(4.5, 13.0, 2.6, 11.5, 15.0, 13.4, "paint"));                                         // crown
        m.add(box(6.0, 15.0, 5.5, 10.0, 15.8, 10.5, "castiron"));                                      // gearbox
        m.add(prism('z', 8.0, 15.4, 0.35, 10.5, 12.5, "darksteel", 8));                                // its shaft
        m.add(prism('z', 8.0, 14.4, 0.9, 13.4, 15.0, "steel", 10));                                    // small motor, z+ end
        // an output tray on the bed, beside the die
        m.add(box(5.0, 5.5, 11.0, 9.0, 5.8, 13.0, "darksteel"));
        // fiddly bits: column collars under the crown, rivets on the crown and the bed, a band round the bed, the
        // motor's fins and terminal box, the control box with its LEDs, a conduit to the motor
        for (double z : new double[]{4.0, 12.0}) m.add(prism('y', 8.0, z, 1.1, 12.4, 13.0, "darksteel", 10));
        MachineKit.rivetsX(m, 3.5, 12.5, 14.0, 4.5, -1, 5);
        MachineKit.rivetsX(m, 4.5, 11.5, 14.0, 11.5, 1, 4);
        m.add(box(1.9, 1.6, 2.9, 14.1, 2.0, 13.1, "darksteel", "nsewu"));
        for (double z : new double[]{13.8, 14.3}) m.add(prism('z', 8.0, 14.4, 1.05, z - 0.15, z + 0.15, "darksteel", 10));
        m.add(box(8.6, 14.4, 13.6, 9.6, 15.6, 14.6, "darksteel"));   // (motor terminal box, from the motor's axis)
        leds.addAll(MachineKit.controlBox(m, 2.0, 3.2, 11.5));
        m.add(box(13.0, 5.5, 12.6, 13.6, 13.0, 13.2, "darksteel"));
        // a nameplate on the bed's front
        m.add(box(1.9, 2.0, 6.0, 2.0, 3.5, 10.0, "brass", "w").free("a nameplate, a decal on the bed"));
        // power: floor terminals on the z faces, against junction boxes on the bed
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 3.0, "castiron"));
        m.add(box(4.5, 1.0, 13.0, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    static List<Mesh> ramA() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(5.5, 8.5, 5.5, 10.5, 9.3, 10.5, "steel"));                                           // platen (down: 6.5)
        m.add(prism('y', 8.0, 8.0, 1.0, 9.3, 14.5, "darksteel", 10));                                  // ram
        return m;
    }

    // ---- 230V

    /** 3x5 capitals for the flange labels. */
    static final java.util.Map<Character, String[]> FONT = java.util.Map.of(
        'I', new String[]{"###", ".#.", ".#.", ".#.", "###"},
        'N', new String[]{"#.#", "###", "###", "###", "#.#"},
        'O', new String[]{"###", "#.#", "#.#", "#.#", "###"},
        'U', new String[]{"#.#", "#.#", "#.#", "#.#", "###"},
        'T', new String[]{"###", ".#.", ".#.", ".#.", ".#."});

    /** A shade that writes s in white from texel (col, row) of the cell (row 0 at the top). */
    static Palette.Shade text(String s, int col, int row) {
        int w = 4 * s.length() - 1;
        return (x, y) -> {
            int r = y - row, c = x - col;
            if (r < 0 || r >= 5 || c < 0 || c >= w) return null;
            int k = c / 4, cc = c % 4;
            if (k >= s.length() || cc == 3) return null;
            return FONT.get(s.charAt(k))[r].charAt(cc) == '#' ? new int[]{180, 180, 180} : null;
        };
    }

    /** Column centres (x, z). */
    static final double[][] COLS = {{3.4, 4.0}, {3.4, 12.0}, {9.6, 4.0}, {9.6, 12.0}};

    static List<Mesh> bodyB(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 0.85, 15.6, 1.0, 15.15, "darksteel"));                                       // skid
        m.add(box(1.5, 1.0, 2.5, 11.5, 5.0, 13.5, "castiron"));                                        // bed
        m.add(box(5.0, 5.0, 6.0, 8.0, 7.0, 10.0, "steel"));                                            // die (top 7)
        for (double[] c : COLS) {
            m.add(prism('y', c[0], c[1], 0.8, 5.0, 13.0, "steel", 10, false, true));   // (no foot cap: it lay in the nut's)
            m.add(prism('y', c[0], c[1], 1.15, 5.0, 5.6, "darksteel", 10));                            // column nuts
        }
        m.add(box(1.5, 13.0, 2.5, 11.5, 15.0, 13.5, "castiron"));                                      // crown
        m.add(prism('y', 6.5, 8.0, 2.4, 15.0, 15.85, "paint", 14));                                   // cylinder
        m.add(box(5.4, 5.0, 11.0, 8.6, 5.3, 13.0, "darksteel"));                                       // output tray
        // fiddly bits: column nuts on the crown, the cylinder's bolt ring and gland, rivets along crown and bed, the
        // tank's seam band and drain plug, the motor's fins and terminal box, a hose to the cylinder, the control box
        for (double[] c : COLS) m.add(prism('y', c[0], c[1], 1.1, 15.0, 15.4, "darksteel", 6));
        m.addAll(OilKit.boltRing('y', 6.5, 8.0, 2.05, 15.8, 15.98, 8, "darksteel", 0.2, 0));
        MachineKit.rivetsX(m, 3.3, 12.7, 14.0, 1.5, -1, 6);
        MachineKit.rivetsX(m, 3.3, 9.6, 3.0, 1.5, -1, 4);
        MachineKit.rivetsZ(m, 2.5, 10.5, 14.0, 13.5, 1, 4);
        m.add(box(11.4, 3.6, 1.9, 15.1, 4.0, 10.1, "darksteel", "nsewu"));
        m.add(prism('z', 13.0, 1.8, 0.35, 1.6, 2.0, "brass", 6, true, false));
        for (double y : new double[]{8.4, 9.2, 11.4}) m.add(prism('y', 13.2, 4.6, 1.7, y - 0.15, y + 0.15, "darksteel", 12));
        m.add(box(11.0, 9.0, 4.0, 11.8, 10.4, 5.2, "darksteel"));
        leds.addAll(MachineKit.controlBox(m, 1.5, 3.0, 11.6));
        // the power pack at the back: tank, motor standing on it, pump, gauge; its pipe up to the cylinder
        m.add(box(11.5, 1.0, 2.0, 15.0, 7.0, 10.0, "paint"));                                         // tank
        m.add(box(11.3, 7.0, 1.8, 15.2, 7.4, 10.2, "darksteel"));                                      // tank lid
        m.add(prism('y', 13.2, 4.6, 1.5, 7.4, 12.0, "steel", 12));                                     // motor
        m.add(prism('y', 13.2, 4.6, 1.65, 10.0, 10.8, "paint", 12));
        m.add(box(12.2, 7.4, 7.0, 14.6, 9.4, 9.6, "castiron"));                                        // pump
        m.add(prism('y', 13.4, 8.3, 0.4, 9.4, 14.0, "darksteel", 8));                                  // pressure line up
        m.add(prism('x', 14.0, 8.3, 0.4, 8.5, 13.4, "darksteel", 8, false, false));                    // ... across to the cylinder
        m.add(prism('y', 8.7, 8.3, 0.4, 14.0, 15.0, "darksteel", 8, false, false));
        m.addAll(dial(11.4, 5.0, 6.0, 0.9));                                                          // gauge (front of the tank)
        // the manifold block at the back with the two flanges (Baughn: blue IN, red OUT; the words in miniature on
        // each flange's face, a backstop you zoom in to read), their pipes into the power pack
        m.add(box(14.6, 1.0, 11.0, 15.5, 11.5, 15.0, "castiron"));
        MachineKit.rivetsX(m, 11.6, 14.4, 11.0, 15.5, 1, 3);
        m.addAll(flange(3.5, 13.0, "paint_blue", "label_in"));
        m.addAll(flange(9.0, 13.0, "paint_red", "label_out"));
        m.add(prism('z', 9.0, 13.0, 0.45, 10.0, 11.0, "darksteel", 8, false, false));                  // OUT line to the pump
        m.add(prism('z', 3.5, 13.0, 0.45, 10.0, 11.0, "darksteel", 8, false, false));                  // IN line to the tank
        // a nameplate on the bed's front
        m.add(box(1.4, 2.0, 6.0, 1.5, 3.5, 10.0, "brass", "w").free("a nameplate, a decal on the bed"));
        // power: floor terminals on the z faces, junction boxes against the bed and the tank
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.5, "castiron"));
        m.add(box(4.5, 1.0, 13.5, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    /** A pipe flange on the manifold (x 15.5): a painted collar, a steel face with four bolts and its label, centre (y, z). */
    static List<Mesh> flange(double y, double z, String paint, String label) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', y, z, 1.0, 15.5, 15.7, paint, 12));
        m.add(prism('x', y, z, 1.45, 15.7, 15.9, "steel", 12));
        m.addAll(OilKit.boltRing('x', y, z, 1.25, 15.9, 15.98, 4, "darksteel", 0.17, Math.PI / 4));
        m.add(MachineKit.flangeLabel(15.9, y, z, 0.7, label));   // (AdventurAgent: readable at 4.5 blocks; smaller, a zoom-in backstop)
        return m;
    }

    static List<Mesh> ramB() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(2.2, 9.0, 3.0, 10.8, 10.4, 13.0, "paint"));                                         // platen (down: 7)
        m.add(box(2.4, 8.6, 5.5, 10.6, 9.0, 10.5, "darksteel"));                                       // its face
        m.add(prism('y', 6.5, 8.0, 1.0, 10.4, 14.5, "steel", 12));                                    // piston rod
        return m;
    }

    static void items(NodeModel n, double[] in, double[] out) {
        n.txt.addAll(List.of("o move", "f tyoff 0.0", "f tyon " + Catalog.fmt(-TRAVEL / 16),
            "f inx " + Catalog.fmt(in[0] / 16 - 0.5), "f iny " + Catalog.fmt(in[1] / 16 - 0.5), "f inz " + Catalog.fmt(in[2] / 16 - 0.5),
            "f outx " + Catalog.fmt(out[0] / 16 - 0.5), "f outy " + Catalog.fmt(out[1] / 16 - 0.5), "f outz " + Catalog.fmt(out[2] / 16 - 0.5)));
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        NodeModel a = new NodeModel("compressora", OilKit.PAINT_50V);
        List<List<Mesh>> la = new ArrayList<>();
        a.part("main", bodyA(la));
        a.part("move", ramA());
        a.part("ledpower", la.get(0));
        a.part("ledrun", la.get(1));
        items(a, new double[]{8.0, 6.5, 8.0}, new double[]{7.0, 5.8, 12.0});
        out.add(a);
        NodeModel b = new NodeModel("compressorb", OilKit.PAINT_200V);
        List<List<Mesh>> lb = new ArrayList<>();
        b.part("main", bodyB(lb));
        b.part("move", ramB());
        b.part("ledpower", lb.get(0));
        b.part("ledrun", lb.get(1));
        items(b, new double[]{6.5, 7.0, 8.0}, new double[]{7.0, 5.3, 12.0});
        out.add(b);
        return out;
    }
}
