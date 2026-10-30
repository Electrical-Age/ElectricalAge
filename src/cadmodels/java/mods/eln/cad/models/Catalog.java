package mods.eln.cad.models;

import org.erisia.cad.Checker;
import org.erisia.cad.Face;
import org.erisia.cad.Model;
import org.erisia.cad.Solid;
import org.erisia.cad.obj.ObjWriter;
import org.erisia.cad.tex.Atlas;
import org.erisia.cad.tex.Palette;
import org.erisia.cad.tex.Sheet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Every static model made with the checked model library (board #44), written at build time into generated resources
 * (./gradlew generateCadModels, run by processResources, cached on its inputs). The build fails if any of them breaks
 * the checker: z-fights, thin solids, loose pieces.
 */
public final class Catalog {
    private Catalog() {
    }

    /** The node models (EA Obj3D). */
    public static List<NodeModel> nodes() {
        List<NodeModel> out = new ArrayList<>();
        for (var supplier : Models.NODES) out.addAll(supplier.get());
        return out;
    }

    public static List<BlockModel> blocks() {
        List<BlockModel> out = new ArrayList<>();
        for (var supplier : Models.BLOCKS) out.addAll(supplier.get());
        return out;
    }

    /** Python's repr of a rounded float is what the old files had; any float text parses the same. */
    static String fmt(double v) {
        double r = Math.round(v * 1e6) / 1e6;
        if (r == 0) r = 0;
        String s = String.format(Locale.ROOT, "%.6f", r).replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    /** The node model's solids as one model, each part its own body (for the checker). */
    public static Model whole(NodeModel n) {
        Model all = new Model();
        for (var e : n.parts.entrySet())
            for (Solid s : e.getValue().solids) {
                if (s.body == null) s.body = e.getKey();
                all.add(s);
            }
        all.relations.addAll(n.parts.values().stream().flatMap(m -> m.relations.stream()).toList());
        return all;
    }

    public static void writeNode(Path res, NodeModel n) throws IOException {
        Path dir = res.resolve("assets/eln/model/" + n.name);
        Files.createDirectories(dir);
        Map<String, String> cells = new LinkedHashMap<>();
        List<ObjWriter.EPart> parts = new ArrayList<>();
        for (var e : n.parts.entrySet()) {
            for (Solid s : e.getValue().solids) cells.putIfAbsent(String.valueOf(s.material), n.palette.getOrDefault(String.valueOf(s.material), String.valueOf(s.material)));
            parts.add(new ObjWriter.EPart(e.getKey(), e.getValue().solids));
        }
        Atlas atlas = new Atlas(4, cells);
        ObjWriter.eln(parts, atlas, dir, n.name, "tex.png");
        atlas.paint(n.name.hashCode()).writePng(dir.resolve("tex.png"));
        if (!n.origins.isEmpty() || !n.txt.isEmpty()) {
            List<String> txt = new ArrayList<>();
            for (var e : n.origins.entrySet())
                txt.addAll(List.of("o " + e.getKey(), "f originX " + fmt(e.getValue()[0] / 16 - 0.5), "f originY " + fmt(e.getValue()[1] / 16 - 0.5),
                    "f originZ " + fmt(e.getValue()[2] / 16 - 0.5)));
            txt.addAll(n.txt);
            Files.writeString(dir.resolve(n.name + ".txt"), String.join("\n", txt) + "\n");
        }
        if (n.writeHitbox) {
            StringBuilder b = new StringBuilder();
            for (double[] h : n.hitbox) {
                for (int i = 0; i < 6; i++) b.append(i == 0 ? "" : " ").append(fmt(h[i]));
                b.append('\n');
            }
            Files.writeString(dir.resolve(n.name + ".hitbox"), b.toString());
        }
    }

    public static void writeBlock(Path res, BlockModel b) throws IOException {
        Path dir = res.resolve("assets/eln/models/block");
        Files.createDirectories(dir);
        ObjWriter.block(b.model(), dir, b.name());
        Set<String> mats = new TreeSet<>();
        for (Solid s : b.model().solids) mats.add(String.valueOf(s.material));
        StringBuilder tx = new StringBuilder();
        for (String m : mats) tx.append(",\n  \"").append(m).append("\": \"eln:blocks/").append(b.sprites()).append('/').append(m).append('"');
        Files.writeString(dir.resolve(b.name() + ".json"), "{\n \"parent\": \"minecraft:block/block\",\n \"loader\": \"neoforge:obj\",\n \"model\": \"eln:models/block/"
            + b.name() + ".obj\",\n \"flip_v\": true,\n \"automatic_culling\": true,\n \"shade_quads\": true,\n \"textures\": {\n  \"particle\": \"eln:blocks/"
            + b.sprites() + "/" + b.particle() + "\"" + tx + "\n }\n}\n");
        Path tex = res.resolve("assets/eln/textures/blocks/" + b.sprites());
        Files.createDirectories(tex);
        for (String m : mats) {
            Sheet s = new Sheet(16, 16, 0);
            Palette.paint(s, 0, 0, Palette.get(m), new java.util.SplittableRandom(m.hashCode()));
            s.writePng(tex.resolve(m + ".png"));
        }
    }

    /** The checker's issues for every catalog model, as lines (empty: all pass). */
    public static List<String> check() {
        List<String> out = new ArrayList<>();
        for (NodeModel n : nodes())
            for (Checker.Issue is : Checker.check(whole(n))) out.add(n.name + ": " + is);
        for (BlockModel b : blocks())
            for (Checker.Issue is : Checker.check(b.model())) out.add(b.name() + ": " + is);
        return out;
    }

    /** Usage: Catalog OUT_RESOURCES_DIR [--no-check]. */
    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[0]);
        List<String> issues = args.length > 1 && args[1].equals("--no-check") ? List.of() : check();
        if (!issues.isEmpty()) {
            System.err.println("The checked models fail the checker (" + issues.size() + " issues):");
            for (String s : issues.subList(0, Math.min(40, issues.size()))) System.err.println("  " + s.replace("\n", "\n    "));
            System.exit(1);
        }
        int n = 0;
        for (NodeModel m : nodes()) {
            writeNode(out, m);
            n++;
        }
        for (BlockModel b : blocks()) {
            writeBlock(out, b);
            n++;
        }
        System.out.println("generateCadModels: " + n + " models written to " + out);
    }
}
