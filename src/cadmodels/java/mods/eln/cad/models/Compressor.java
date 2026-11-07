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

    static List<Mesh> bodyA() {
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

    static {
        Palette.put("paint_red", 176, 48, 40, 8, null);
        Palette.put("label_in", 52, 102, 176, 4, text("IN", 5, 10));
        Palette.put("label_out", 176, 48, 40, 4, text("OUT", 3, 4));
    }

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
            // (the back panel's texture runs right to left as seen from behind: mirrored)
            int r = y - row, c = w - 1 - (x - col);
            if (r < 0 || r >= 5 || c < 0 || c >= w) return null;
            int k = c / 4, cc = c % 4;
            if (k >= s.length() || cc == 3) return null;
            return FONT.get(s.charAt(k))[r].charAt(cc) == '#' ? new int[]{180, 180, 180} : null;
        };
    }

    /** Column centres (x, z). */
    static final double[][] COLS = {{3.4, 4.0}, {3.4, 12.0}, {9.6, 4.0}, {9.6, 12.0}};

    static List<Mesh> bodyB() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 0.85, 15.6, 1.0, 15.15, "darksteel"));                                       // skid
        m.add(box(1.5, 1.0, 2.5, 11.5, 5.0, 13.5, "castiron"));                                        // bed
        m.add(box(5.0, 5.0, 6.0, 8.0, 7.0, 10.0, "steel"));                                            // die (top 7)
        for (double[] c : COLS) {
            m.add(prism('y', c[0], c[1], 0.8, 5.0, 13.0, "steel", 10, false, true));   // (no foot cap: it lay in the nut's)
            m.add(prism('y', c[0], c[1], 1.15, 5.0, 5.6, "darksteel", 10));                            // column nuts
        }
        m.add(box(1.5, 13.0, 2.5, 11.5, 15.0, 13.5, "castiron"));                                      // crown
        m.add(prism('y', 6.5, 8.0, 2.4, 15.0, 16.0, "paint", 14));                                    // cylinder
        m.add(box(5.4, 5.0, 11.0, 8.6, 5.3, 13.0, "darksteel"));                                       // output tray
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
        // the back panel with the two flanges and their labels (Baughn: blue IN, red OUT, marked in the texture)
        m.add(box(15.0, 1.0, 1.5, 15.5, 12.0, 14.5, "castiron"));
        m.add(box(15.5, 1.0, 3.0, 15.6, 6.0, 14.0, "label_in", "e").free("the IN label, a decal on the panel"));
        m.add(box(15.5, 7.0, 3.0, 15.6, 12.0, 14.0, "label_out", "e").free("the OUT label, a decal on the panel"));
        m.addAll(flange(3.5, 13.0, "paint_blue"));
        m.addAll(flange(9.5, 13.0, "paint_red"));
        // a nameplate on the bed's front
        m.add(box(1.4, 2.0, 6.0, 1.5, 3.5, 10.0, "brass", "w").free("a nameplate, a decal on the bed"));
        // power: floor terminals on the z faces, junction boxes against the bed and the tank
        m.addAll(turned(terminalPlate(), 1));
        m.addAll(turned(terminalPlate(), 3));
        m.add(box(4.5, 1.0, 0.85, 11.5, 4.5, 2.5, "castiron"));
        m.add(box(4.5, 1.0, 13.5, 11.5, 4.5, 15.15, "castiron"));
        return m;
    }

    /** A pipe flange on the back panel (x 15.5): a painted collar and a steel face, its centre at (y, z). */
    static List<Mesh> flange(double y, double z, String paint) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', y, z, 1.0, 15.5, 15.75, paint, 12));
        m.add(prism('x', y, z, 1.4, 15.75, 15.95, "steel", 12));
        m.add(prism('x', y, z, 0.5, 15.95, 16.0, "darksteel", 8));
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
        a.part("main", bodyA());
        a.part("move", ramA());
        items(a, new double[]{8.0, 6.5, 8.0}, new double[]{7.0, 5.8, 12.0});
        out.add(a);
        NodeModel b = new NodeModel("compressorb", OilKit.PAINT_200V);
        b.part("main", bodyB());
        b.part("move", ramB());
        items(b, new double[]{6.5, 7.0, 8.0}, new double[]{7.0, 5.3, 12.0});
        out.add(b);
        return out;
    }
}
