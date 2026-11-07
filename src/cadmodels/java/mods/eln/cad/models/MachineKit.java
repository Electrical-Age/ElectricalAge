package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.tex.Palette;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;

/**
 * Small parts for the four main machines (Baughn 2026-11-07: "too clean; use the steam turbine or heat furnaces as
 * inspiration"): rivet rows, a control box with status LEDs (parts ledpower and ledrun, which
 * ElectricalMachineDescriptor lights), and a flange's face label in miniature text (a whole atlas cell squeezed onto the
 * flange's face, so its letters are finer than a pixel: you zoom in to read them).
 */
final class MachineKit {
    private MachineKit() {
    }

    static {
        Palette.put("led_green", 70, 230, 90, 6, null);
        Palette.put("led_amber", 250, 170, 40, 6, null);
        Palette.put("paint_red", 176, 48, 40, 8, null);
        Palette.put("label_in", 52, 102, 176, 0, Compressor.text("IN", 4, 5));
        Palette.put("label_out", 176, 48, 40, 0, Compressor.text("OUT", 2, 5));
        Palette.put("latex", 242, 238, 220, 4, null);
    }

    /** Rivets on a z face (at z, facing +z if out > 0): count of them from x0 to x1 at height y. */
    static void rivetsZ(List<Mesh> m, double x0, double x1, double y, double z, int out, int count) {
        for (int k = 0; k < count; k++) {
            double x = count == 1 ? (x0 + x1) / 2 : x0 + (x1 - x0) * k / (count - 1);
            double za = out > 0 ? z - 0.05 : z - 0.15, zb = out > 0 ? z + 0.15 : z + 0.05;
            m.add(box(x - 0.2, y - 0.2, za, x + 0.2, y + 0.2, zb, "brass"));
        }
    }

    /** Rivets on an x face (at x, facing +x if out > 0): count of them from z0 to z1 at height y. */
    static void rivetsX(List<Mesh> m, double z0, double z1, double y, double x, int out, int count) {
        for (int k = 0; k < count; k++) {
            double z = count == 1 ? (z0 + z1) / 2 : z0 + (z1 - z0) * k / (count - 1);
            double xa = out > 0 ? x - 0.05 : x - 0.15, xb = out > 0 ? x + 0.15 : x + 0.05;
            m.add(box(xa, y - 0.2, z - 0.2, xb, y + 0.2, z + 0.2, "brass"));
        }
    }

    /**
     * A control box on a face facing -x (the front) at x, its middle at (y, z): a steel box, two LED lenses (the parts),
     * a toggle switch. Adds the box to main and returns {ledpower meshes, ledrun meshes}.
     */
    static List<List<Mesh>> controlBox(List<Mesh> main, double x, double y, double z) {
        main.add(box(x - 0.8, y - 1.0, z - 1.2, x + 0.05, y + 1.0, z + 1.2, "darksteel"));
        main.add(box(x - 0.9, y - 0.9, z - 1.1, x - 0.8, y + 0.9, z + 1.1, "steel", "w").free("the control box's face plate, a decal"));
        main.add(box(x - 1.3, y - 0.75, z + 0.55, x - 0.8, y - 0.35, z + 0.85, "steel"));                 // toggle
        List<Mesh> pw = new ArrayList<>(List.of(prism('x', y + 0.4, z - 0.5, 0.28, x - 1.05, x - 0.8, "led_green", 8)));
        List<Mesh> run = new ArrayList<>(List.of(prism('x', y + 0.4, z + 0.5, 0.28, x - 1.05, x - 0.8, "led_amber", 8)));
        return List.of(pw, run);
    }

    /** The label on a flange's face facing up at y, centre (x, z), radius r; readable from the front (x-). */
    static Mesh flangeLabelUp(double y, double x, double z, double r, String mat) {
        Mesh d = prism('y', x, z, r, y, y + 0.06, mat, 12, false, true);
        d.uv(f -> {
            float[] uv = new float[2 * f.n];
            for (int i = 0; i < f.n; i++) {
                double fx = f.xyz[3 * i], fz = f.xyz[3 * i + 2];
                // seen from above with the front (x-) at the bottom: right is +z, up is +x
                uv[2 * i] = (float) Math.min(1, Math.max(0, (fz - (z - r)) / (2 * r)));
                uv[2 * i + 1] = (float) Math.min(1, Math.max(0, (fx - (x - r)) / (2 * r)));
            }
            return uv;
        });
        d.free("a label printed on the flange's face");
        return d;
    }

    /**
     * The label on a flange's face (it faces +x at x, centre (y, z), radius r): a thin disc whose face shows the label
     * material's whole cell, upright and readable from behind the machine.
     */
    static Mesh flangeLabel(double x, double y, double z, double r, String mat) {
        Mesh d = prism('x', y, z, r, x, x + 0.06, mat, 12, false, true);
        d.uv(f -> {
            float[] uv = new float[2 * f.n];
            for (int i = 0; i < f.n; i++) {
                double fz = f.xyz[3 * i + 2], fy = f.xyz[3 * i + 1];
                // seen from +x, right is -z: u from z + r (left) to z - r (right); v from y - r up
                uv[2 * i] = (float) Math.min(1, Math.max(0, (z + r - fz) / (2 * r)));
                uv[2 * i + 1] = (float) Math.min(1, Math.max(0, (fy - (y - r)) / (2 * r)));
            }
            return uv;
        });
        d.free("a label printed on the flange's face");
        return d;
    }
}
