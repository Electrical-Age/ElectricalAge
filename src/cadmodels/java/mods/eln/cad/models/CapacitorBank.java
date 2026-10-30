package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * Capacitor bank (Baughn 2026-10-28, was tools/models/capacitorbank.py): six big capacitor cans in two layers of three
 * on a steel rack, joined by copper busbars, with a terminal plate on each flank: + (red band, Baughn: polarities
 * red/blue) on the left of the front, - (blue band) on the right, as you face its front. Internally one large capacitor
 * (CapacitorBankElement). The cans' band carries the tier colour. Drawn by CapacitorBankRender through Obj3D. Front XN
 * (x = 0); after glRotateXnRef, front.left() of XN is ZN: the + plate sits at z = 0, the - plate at z = 16.
 * (The Python also wrote the item icons textures/item/capacitor_bank.png and capacitor_cell.png; not ported here.)
 */
public final class CapacitorBank {
    private CapacitorBank() {
    }

    static int[] all(int k) {
        return k == 0 ? null : new int[]{k, k, k};
    }


    static final double CAN_R = 2.3;
    /** The three cans of a layer, along z. */
    static final double[] ZS = {3.0, 8.0, 13.0};
    static final double[][] LAYERS = {{1.5, 7.0}, {8.5, 14.0}};

    static List<Mesh> bank() {
        List<Mesh> m = new ArrayList<>();
        // the rack: base plate, four corner posts, a shelf between the layers, a top plate
        m.add(box(1.0, 0, 0.6, 15.0, 1.0, 15.4, "darksteel"));
        for (double[] xz : new double[][]{{1.0, 0.6}, {13.8, 0.6}, {1.0, 14.2}, {13.8, 14.2}})
            m.add(box(xz[0], 1.0, xz[1], xz[0] + 1.2, 14.4, xz[1] + 1.2, "steel"));
        m.add(box(1.1, 7.4, 0.7, 14.9, 8.1, 15.3, "darksteel"));   // (inset: no shared faces with the posts)
        // an open top frame (the can tops and busbars stay visible): rails round the edge, a carrying bar across
        m.add(box(1.0, 14.4, 0.6, 15.0, 15.0, 1.8, "darksteel"));
        m.add(box(1.0, 14.4, 14.2, 15.0, 15.0, 15.4, "darksteel"));
        m.add(box(1.0, 14.4, 1.8, 2.2, 15.0, 14.2, "darksteel"));
        m.add(box(13.8, 14.4, 1.8, 15.0, 15.0, 14.2, "darksteel"));
        // (the checker: the carrying bar spanned between the rails, touching them only along its end edges; it rests on
        // them now, z 1.2..14.8 (was 1.8..14.2, visible), held by two hidden pins inside bar and rail)
        m.add(box(7.7, 15.0, 1.2, 8.3, 15.6, 14.8, "steel"));
        for (double z : new double[]{1.3, 14.3}) m.add(box(7.8, 14.6, z, 8.2, 15.2, z + 0.4, "steel"));
        // six cans: a can body, a band with the tier colour, a dark top with two studs, joined by busbars over the tops
        for (double[] l : LAYERS) {
            double y0 = l[0], y1 = l[1];
            for (double z : ZS) {
                m.add(prism('y', 8.0, z, CAN_R, y0, y1 - 0.6, "can", 12));
                m.add(tube('y', 8.0, z, CAN_R - 0.05, CAN_R + 0.08, y0 + 1.0, y0 + 1.8, "hvtier", 12, Double.NaN, false));
                m.add(prism('y', 8.0, z, CAN_R - 0.2, y1 - 0.6, y1 - 0.3, "cantop", 12));
                // the studs, marked: + red (the + busbar's side), - blue
                m.add(prism('y', 6.9, z, 0.45, y1 - 0.3, y1, "red", 6));
                m.add(prism('y', 9.1, z, 0.45, y1 - 0.3, y1, "blue", 6));
            }
            m.add(box(6.6, y1, 1.0, 7.2, y1 + 0.35, 15.0, "copper"));   // the + busbar of the layer
            m.add(box(8.8, y1, 1.0, 9.4, y1 + 0.35, 15.0, "copper"));   // the - busbar
        }
        // the risers joining the layers' busbars, down the flanks
        m.add(box(6.6, LAYERS[0][1], 0.5, 7.2, LAYERS[1][1] + 0.35, 1.0, "copper"));     // + side (z 0)
        m.add(box(8.8, LAYERS[0][1], 15.0, 9.4, LAYERS[1][1] + 0.35, 15.5, "copper"));   // - side (z 16)
        // the terminal plates, low on the flanks: + (left of the front: z = 0) red, - (z = 16) blue (Baughn); the cans'
        // sleeve bands keep the HV tier colour
        // (the checker: the plates hung 0.5 px above the floor, touching the base's edge only; they reach the floor, y 0..4.5)
        for (Object[] p : new Object[][]{{0.0, 0.6, "red"}, {15.4, 16.0, "blue"}}) {
            double z0 = (double) p[0], z1 = (double) p[1];
            m.add(box(4.5, 0.0, z0, 11.5, 4.5, z1, "steel"));
            m.add(box(4.5, 4.5, z0, 11.5, 5.0, z1, (String) p[2]));
            for (double x : new double[]{5.75, 8.0, 10.25}) m.add(box(x - 0.5, 1.75, z0 - 0.1, x + 0.5, 2.75, z1 + 0.1, "copper"));
        }
        // a rating label on the front of the base
        m.add(box(0.9, 0.2, 4.0, 1.0, 0.9, 12.0, "label", "w").free("the rating label, a decal 0.1 px proud of the base"));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("capacitorbank", "hvtier");   // (no "paint" material)
        n.part("main", bank());
        return List.of(n);
    }
}
