package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.heatPad;
import static mods.eln.cad.models.OilKit.turned;

/**
 * Steam condenser (Small Steam Condenser / Steam Condenser, for the 50V / 200V Steam Turbine): a shell-and-tube
 * condenser lying on two saddles, ringed by copper cooling fins, with its condensate pump and outlet flange at the
 * front, a makeup flange on top, and a duct stub toward the turbine beside it. Drawn by CondenserRender ->
 * CondenserDescriptor.draw() after front.glRotateXnRef(): part main always, and one of duct_right (z = 16), duct_back
 * (x = 16), duct_left (z = 0) toward the turbine exhausting into it (its exhaust flange meets the stub's, at the
 * turbine's shaft height: the steam turbine's EXHAUST_R and cy). Front XN (x = 0): the condensate outlet (an iron
 * pipe's flange, low on the front face, the pump behind it); top: makeup flange at the centre; thermal cables on the
 * other sides at the floor.
 */
public final class Condenser {
    private Condenser() {
    }

    /** r: shell radius; cy: the turbine's shaft height (its exhaust flange); fins: x of the cooling fins. */
    record Tier(String name, String paint, double r, double cy, double[] fins) {
    }

    static final List<Tier> TIERS = List.of(
        new Tier("50v", OilKit.PAINT_50V, 4.5, 6.75, new double[]{4.0, 6.0, 8.0, 10.0, 12.0}),
        new Tier("200v", OilKit.PAINT_200V, 5.25, 7.25, new double[]{3.5, 5.25, 7.0, 8.75, 10.5, 12.25}));
    static final double C = 8.0;
    /** The shell's axis height. */
    static final double SY = 7.5;
    /** The steam turbine's. */
    static final double EXHAUST_R = 2.25;

    static List<Mesh> mainPart(Tier t) {
        List<Mesh> m = new ArrayList<>();
        double r = t.r;
        // skid and saddles
        m.add(box(1.5, 0, 3.0, 14.5, 1.0, 13.0, "darksteel", "nsewu"));
        // (the checker: the saddles' bottoms z-fought the pump motor's on the skid's top; dropped, they sit on the skid)
        for (double x : new double[]{3.0, 11.5}) m.add(box(x, 1.0, C - r + 0.75, x + 1.5, SY - r + 1.0, C + r - 0.75, "castiron", "nsewu"));
        // the shell (tier band), end caps, cooling fins
        m.add(prism('x', SY, C, r, 2.0, 14.0, "steel", 14, false, false));
        m.add(prism('x', SY, C, r - 1.0, 1.25, 2.0, "steel", 14, r, true, false, Double.NaN));
        m.add(prism('x', SY, C, r, 14.0, 14.75, "steel", 14, r - 1.0, false, true, Double.NaN));
        for (double x : t.fins) m.add(tube('x', SY, C, r - 0.1, r + 0.7, x, x + 0.4, "copper", 14, Double.NaN, false));
        m.add(tube('x', SY, C, r - 0.1, r + 0.3, 13.0, 13.75, "paint", 14, Double.NaN, false));
        // makeup: a riser to an iron pipe's flange on the top face
        m.add(prism('y', C, C, 2.0, SY + r - 0.5, 15.0, "steel", 12, false, false));
        m.add(prism('y', C, C, 2.75, 15.0, 16.0, "steel", 12));
        m.addAll(boltRing('y', C, C, 2.35, 14.75, 15.0, 4, "brass", 0.22, 0));
        // condensate pump (under the front cap) and the outlet: an iron pipe's flange low on the front face
        m.add(prism('y', 3.5, C, 1.75, 1.0, 3.75, "paint", 10));                       // pump motor
        m.add(prism('x', 2.75, C, 1.25, 1.0, 2.5, "steel", 10, false, true));
        m.add(prism('x', 2.75, C, 2.0, 0.0, 1.0, "steel", 12, true, false));          // outlet flange on the face
        m.addAll(boltRing('x', 2.75, C, 1.6, 1.0, 1.25, 4, "brass", 0.2, 0));
        // thermal pads on the floor of the three other sides (cables there take its heat)
        // (the checker: the pads stood clear of the skid, 0.25 px at the back and 1.75 px on the flanks; a darksteel foot,
        // 1 px high like the skid, joins each pad's backing to the skid)
        for (int k : new int[]{1, 2, 3}) {
            m.addAll(turned(heatPad(0.75, 3.0), k));
            m.addAll(turned(List.of(box(1.25, 0, 4.0, k == 2 ? 1.5 : 3.0, 1.0, 12.0, "darksteel", "nsu")), k));
        }
        return m;
    }

    /** Toward the turbine on the x = 0 side (turned for the others): the shell's nozzle to a flange on that face. */
    static List<Mesh> duct(Tier t) {
        List<Mesh> ms = new ArrayList<>();
        ms.add(prism('x', t.cy, C, EXHAUST_R, 0.75, C - t.r + 1.0, "castiron", 12, false, false));
        ms.add(prism('x', t.cy, C, EXHAUST_R + 0.75, 0.0, 0.75, "steel", 12, true, false));
        ms.addAll(boltRing('x', t.cy, C, EXHAUST_R + 0.4, 0.75, 1.0, 4, "brass", 0.25, 0));
        return ms;
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) {
            NodeModel n = new NodeModel("condenser" + t.name, t.paint);
            n.part("main", mainPart(t));
            for (var e : List.of(Map.entry("duct_right", 1), Map.entry("duct_back", 2), Map.entry("duct_left", 3)))
                n.part(e.getKey(), turned(duct(t), e.getValue()));
            out.add(n);
        }
        return out;
    }
}
