package mods.eln.cad.models;

import org.erisia.cad.Mesh;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;
import static mods.eln.cad.models.OilKit.terminalPlate;

/**
 * The 230V Pipe Heater (Baughn 2026-11-26): heating coils wound round a length of pipe line, laid out as the Inline
 * Pump. The line runs through its flanks (z = 0 to 16) at the pump's height (InlinePumpElement.LINE_Y = 4 px), iron pipe
 * flanges at the face centres; a heated section between painted end collars on two cast-iron saddles; power on the front
 * floor terminal (x = 0), a terminal box behind it and a conduit to the coils. Parts main and element (the coil turns,
 * drawn again glowing when hot by ElectricHeaterRender). Front XN (x = 0).
 */
public final class PipeHeater {
    private PipeHeater() {
    }

    static final double C = 8.0;
    /** The line's height (InlinePumpElement.LINE_Y). */
    static final double LY = 4.0;
    /** The heated section's radius and z span, and the coil turns' outer radius. */
    static final double R = 1.6, Z0 = 3.0, Z1 = 13.0, RC = 2.15;

    static List<Mesh> mainPart() {
        List<Mesh> m = new ArrayList<>();
        m.add(box(2.0, 0, 3.0, 14.0, 1.0, 13.0, "darksteel", "nsewu"));
        // two saddles carry the heated section (their tops 0.2 px into it)
        for (double z : new double[]{3.6, 11.6}) m.add(box(5.5, 1.0, z, 10.5, LY - R + 0.2, z + 0.8, "castiron"));
        // the heated section, its painted end collars, and pipe stubs to flanges on z = 0 and 16 (as the inline pump's)
        m.add(prism('z', C, LY, R, Z0 + 0.5, Z1 - 0.5, "steel", 12, false, false));
        m.add(prism('z', C, LY, RC + 0.35, Z0, Z0 + 0.6, "paint", 12, true, true));
        m.add(prism('z', C, LY, RC + 0.35, Z1 - 0.6, Z1, "paint", 12, true, true));
        m.add(prism('z', C, LY, 0.75, 0.9, Z0 + 0.1, "steel", 8, false, false));
        m.add(prism('z', C, LY, 0.75, Z1 - 0.1, 15.1, "steel", 8, false, false));
        m.add(prism('z', C, LY, 1.5, 0.0, 1.0, "steel", 10, true, false));
        m.add(prism('z', C, LY, 1.5, 15.0, 16.0, "steel", 10, false, true));
        // power: the front floor terminal, a terminal box behind it, a brass conduit into the section between two turns
        m.addAll(terminalPlate());
        m.add(box(0.85, 0, 5.0, 3.5, 5.5, 11.0, "steel", "nseu"));
        m.add(box(0.75, 5.5, 4.9, 3.6, 5.85, 11.1, "darksteel", "nsewud"));
        m.add(prism('x', 5.0, C, 0.45, 3.5, C - 1.05, "brass", 6, false, false));   // (in the gap between the coil's halves)
        return m;
    }

    /** The coil turns round the heated section (each touches the section in part main, not its neighbours). */
    static List<Mesh> elementPart() {
        List<Mesh> m = new ArrayList<>();
        // (z 5 to 11, clear of the saddles; none at 8, where the conduit comes in between the coil's two halves)
        for (double z : new double[]{5, 6, 7, 9, 10, 11}) {
            m.add(tube('z', C, LY, R - 0.05, RC, z - 0.3, z + 0.3, "coil", 12, Double.NaN, false)
                .free("a coil turn, wound on the heated section in part main"));
        }
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("pipeheater200v", OilKit.PAINT_200V);
        n.part("main", mainPart());
        n.part("element", elementPart());
        return List.of(n);
    }
}
