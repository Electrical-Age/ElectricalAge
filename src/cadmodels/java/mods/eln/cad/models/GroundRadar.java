package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;

/**
 * The Ground Radar (Baughn 2026-11-24): a painted antenna sled flat on the ground on a skid plate, a console on its back
 * half with a sloped-in screen facing the front, status LEDs under the screen, a battery hatch on the sled's front half,
 * a carrying handle over the console and a short whip antenna. Drawn by GroundRadarRender. Front XN (x = 0).
 */
public final class GroundRadar {
    private GroundRadar() {
    }

    static List<Mesh> body(List<List<Mesh>> leds) {
        List<Mesh> m = new ArrayList<>();
        m.add(box(1.0, 0, 2.0, 15.0, 1.0, 14.0, "darksteel"));                                        // skid plate
        m.add(box(1.5, 1.0, 2.5, 14.5, 4.5, 13.5, "paint"));                                          // the antenna sled
        m.add(box(1.4, 2.4, 2.4, 14.6, 2.8, 13.6, "darksteel"));                                      // its seam band
        m.add(box(7.0, 4.5, 3.5, 14.0, 11.0, 12.5, "paint"));                                         // the console
        m.add(box(6.8, 6.8, 4.5, 7.1, 10.5, 11.5, "darksteel"));                                      // the screen's bezel
        m.add(box(6.7, 7.1, 4.8, 6.8, 10.2, 11.2, "screen").free("the screen, a decal on its bezel"));
        // the battery hatch on the sled, its latch
        m.add(box(2.0, 4.5, 4.0, 6.0, 4.9, 12.0, "darksteel"));
        m.add(box(2.6, 4.9, 7.4, 3.4, 5.2, 8.6, "steel"));
        // status LEDs (power, scanning) under the screen
        leds.addAll(MachineKit.controlBox(m, 7.05, 5.6, 8.0));
        // the carrying handle over the console
        m.add(box(9.8, 11.0, 4.5, 10.8, 12.6, 5.5, "darksteel"));
        m.add(box(9.8, 11.0, 10.5, 10.8, 12.6, 11.5, "darksteel"));
        m.add(box(9.6, 12.4, 4.3, 11.0, 13.4, 11.7, "rubber"));
        // the whip antenna on its base
        m.add(prism('y', 13.0, 11.5, 0.6, 11.0, 11.6, "darksteel", 8));
        m.add(prism('y', 13.0, 11.5, 0.2, 11.6, 15.5, "steel", 6));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("ground_radar", "paint_orange");
        List<List<Mesh>> l = new ArrayList<>();
        n.part("main", body(l));
        n.part("ledpower", l.get(0));
        n.part("ledrun", l.get(1));
        return List.of(n);
    }
}
