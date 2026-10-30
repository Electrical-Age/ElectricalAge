package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.Model;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * The headlamp's item model (Baughn 2026-10-30): a leather band round a head, a small lamp at its front (-z, north) with a
 * lens and a reflector rim, a battery pack at its back, a buckle on the side. An item model (neoforge:obj, block poses).
 */
public final class Headlamp {
    private Headlamp() {
    }

    static Model model() {
        List<Mesh> m = new ArrayList<>();
        // the band: an elastic strap round the head, a buckle on its right
        m.add(tube('y', 8, 8, 4.4, 5.0, 6.0, 8.0, "leather", 16));
        m.add(box(12.6, 6.3, 7.0, 13.4, 7.7, 9.0, "steel"));
        // the lamp: a housing on the strap's front, a reflector rim, the lens, a switch on top
        m.add(box(6.3, 5.4, 2.0, 9.7, 8.6, 3.5, "darksteel"));
        m.add(prism('z', 8, 7, 1.6, 1.4, 2.0, "steel", 12, true, false));
        m.add(prism('z', 8, 7, 1.3, 1.3, 1.4, "lamp", 12, true, false).free("the lens, a disc in the rim's mouth"));
        m.add(box(7.6, 8.6, 2.4, 8.4, 9.0, 3.1, "red"));
        // the battery pack at the back, a wire round the strap's top to the lamp
        m.add(box(6.6, 5.6, 12.5, 9.4, 8.4, 13.8, "darksteel"));
        m.add(box(8.9, 6.3, 13.8, 9.3, 7.7, 13.9, "paint_green", "s").free("the charge stripe, a decal on the pack"));
        Model model = new Model();
        model.addAll(m);
        return model;
    }

    public static List<BlockModel> models() {
        return List.of(new BlockModel("headlamp", model(), "headlamp", "leather", true));
    }
}
