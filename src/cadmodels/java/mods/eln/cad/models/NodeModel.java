package mods.eln.cad.models;

import org.erisia.cad.Mesh;
import org.erisia.cad.Model;
import org.erisia.cad.Solid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An EA node model (Obj3D, drawn by a Render class): its parts (in order; the first is usually "main"), the
 * materials' palette entries ("paint" is the device's paint), the rotation origins (NAME.txt), extra lines for NAME.txt,
 * and the hitbox (NAME.hitbox, px, front XN). Written by {@link Catalog} to assets/eln/model/NAME/.
 */
public final class NodeModel {
    public final String name;
    public final Map<String, Model> parts = new LinkedHashMap<>();
    public final Map<String, double[]> origins = new LinkedHashMap<>();
    public final List<String> txt = new ArrayList<>();
    public final List<double[]> hitbox = new ArrayList<>();
    /** Material -> palette entry, where not the same name ("paint" -> the device's paint). */
    public final Map<String, String> palette = new LinkedHashMap<>();
    public boolean writeHitbox;

    public NodeModel(String name, String paint) {
        this.name = name;
        palette.put("paint", paint);
    }

    /** A part with these meshes. */
    public Model part(String part, List<? extends Solid> meshes) {
        Model m = parts.computeIfAbsent(part, k -> new Model());
        m.addAll(meshes);
        return m;
    }

    public NodeModel origin(String part, double x, double y, double z) {
        origins.put(part, new double[]{x, y, z});
        return this;
    }

    public NodeModel hitbox(double... box) {
        writeHitbox = true;
        for (int i = 0; i < box.length; i += 6) hitbox.add(java.util.Arrays.copyOfRange(box, i, i + 6));
        return this;
    }

    /** All meshes of a part as a list (for mcmodel-style building). */
    public static List<Mesh> list(Mesh... ms) {
        return new ArrayList<>(List.of(ms));
    }
}
