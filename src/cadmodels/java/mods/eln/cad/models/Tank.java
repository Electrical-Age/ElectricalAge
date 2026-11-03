package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.boltRing;

/**
 * Pipe tanks (Iron Tank / Alloy Tank; Baughn 2026-10-29, the shaft redo): an upright riveted vessel on a skid with a
 * sight glass on its front. Ports (an iron pipe's flange): the top centre (vapour), and on each side face a low one
 * (liquid, 4 px up) and a high one (vapour, 12 px up); the bottom port is a plain opening under the skid.
 * <p>
 * Parts: shell; glass (the sight glass) and level: the water in it, 2..14 px, which TankRender scales to the fill;
 * disc_* (a flange, drawn only where a pipe meets the port: Baughn 2026-11-03, a lone tank with all seven looked silly)
 * and nozzle_* (a stub of large pipe, 1 px long, which TankRender stretches from the flange in past the shell). A box
 * of tanks (TankGroup) is drawn as one: shell, glass and level scaled to the box. Front XN (x = 0).
 */
public final class Tank {
    private Tank() {
    }


    record Tier(String name, String paint, String shell, String band) {
    }

    static final List<Tier> TIERS = List.of(new Tier("iron", "paint_tankiron", "darksteel", "castiron"),
        new Tier("alloy", "paint_tankalloy", "steel", "paint"));
    static final double C = 8.0, R = 5.5;

    static List<Mesh> shellPart(Tier t) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.5, 0, 1.5, 14.5, 1.0, 14.5, "darksteel", "nsewu"));
        m.add(prism('y', C, C, R, 1.0, 14.5, t.shell, 14, false, false));
        m.add(prism('y', C, C, R, 14.5, 15.0, t.shell, 14, R - 1.5, false, true, Double.NaN));
        m.add(prism('y', C, C, R - 0.5, 1.0, 1.5, t.shell, 14, true, false));
        for (double y : new double[]{3.0, 8.0, 13.0}) m.add(tube('y', C, C, R - 0.1, R + 0.4, y, y + 0.6, t.band, 14, Double.NaN, false));
        return m;
    }

    /** A flange disc alone on the x = 0 face at height y (a port with a pipe on it: TankRender turns it to the face). */
    static List<Mesh> discPart(double y) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', y, C, 3.75, 0.0, 0.75, "steel", 14, true, true));
        m.addAll(boltRing('x', y, C, 3.3, 0.75, 1.0, 6, "brass", 0.25, 0));
        return m;
    }

    static List<Mesh> topDiscPart() {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('y', C, C, 3.75, 15.25, 16.0, "steel", 14));
        m.addAll(boltRing('y', C, C, 3.3, 15.0, 15.25, 6, "brass", 0.25, 0));
        return m;
    }

    /**
     * A port's nozzle on the x = 0 face at height y: a large pipe's bore, 1 px long from x = 0 (TankRender stretches it
     * from behind the flange disc in past the shell's surface: a merged box's shell sits well inside its faces). Its
     * inner end is closed (Baughn 2026-11-03: a nozzle ending short of the curved shell showed its open end).
     */
    static List<Mesh> nozzlePart(double y, Tier t) {
        return List.of(prism('x', y, C, 3.0, 0.0, 1.0, t.shell, 12, false, true));
    }

    /** The top port's nozzle, 1 px long down from y = 16 (stretched down into the shell's top), closed below. */
    static List<Mesh> nozzleTopPart(Tier t) {
        return List.of(prism('y', C, C, 3.0, 15.0, 16.0, t.shell, 12, true, false));
    }

    /** The sight glass on the front: brass frame, dark backing, 1.5..14.5 px. */
    static List<Mesh> glassPart() {
        return List.of(box(C - R - 0.75, 1.5, C - 1.25, C - R + 0.5, 14.5, C - 0.75, "brass"),
            box(C - R - 0.75, 1.5, C + 0.75, C - R + 0.5, 14.5, C + 1.25, "brass"),
            box(C - R - 0.6, 1.5, C - 0.75, C - R - 0.5, 14.5, C + 0.75, "darksteel", "w"));   // (behind the water)
    }

    static List<Mesh> levelPart() {
        return List.of(box(C - R - 0.8, 2.0, C - 0.7, C - R - 0.65, 14.0, C + 0.7, "water", "w"));
    }

    public static List<NodeModel> models() {
        List<NodeModel> out = new ArrayList<>();
        for (Tier t : TIERS) {
            NodeModel n = new NodeModel("tank" + t.name, t.paint);
            n.part("shell", shellPart(t));
            n.part("glass", glassPart());
            n.part("level", levelPart());
            n.part("disc_low", discPart(4.0));
            n.part("disc_high", discPart(12.0));
            n.part("disc_top", topDiscPart());
            n.part("nozzle_low", nozzlePart(4.0, t));
            n.part("nozzle_high", nozzlePart(12.0, t));
            n.part("nozzle_top", nozzleTopPart(t));
            out.add(n);
        }
        return out;
    }
}
