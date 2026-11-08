package mods.eln.cad.models;

import org.erisia.cad.Checker;
import org.erisia.cad.Face;
import org.erisia.cad.Mesh;
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
        String kind = b.item() ? "item" : "block";
        Path dir = res.resolve("assets/eln/models/" + kind);
        Files.createDirectories(dir);
        ObjWriter.block(b.model(), dir, b.name());
        Set<String> mats = new TreeSet<>();
        for (Solid s : b.model().solids) mats.add(String.valueOf(s.material));
        StringBuilder tx = new StringBuilder();
        for (String m : mats) tx.append(",\n  \"").append(m).append("\": \"eln:blocks/").append(b.sprites()).append('/').append(m).append('"');
        Files.writeString(dir.resolve(b.name() + ".json"), "{\n \"parent\": \"minecraft:block/block\",\n \"loader\": \"neoforge:obj\",\n \"model\": \"eln:models/" + kind + "/"
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

    /** The gap (px) a belt's straight runs keep from everything else, and how far into a run its pulleys may reach. */
    static final double BELT_GAP = 0.3;

    /**
     * What moves: each declared spinning part swept a full turn against main (Motion.sweep), and a belt (part beltlink's
     * .txt: ax ay ar bx by br, in px, the link part's z extent its width) whose straight runs, with BELT_GAP round them,
     * must clear every other solid (Motion.clearance). The runs stop short of the pulleys by the length over which a rim
     * comes within the gap of the belt (sqrt(2 r gap)).
     */
    static List<Checker.Issue> motion(NodeModel n) {
        List<Checker.Issue> out = new ArrayList<>();
        Model main = n.parts.get("main");
        for (var e : n.spins.entrySet()) {
            Model p = n.parts.get(e.getKey());
            double[] o = n.origins.get(e.getKey());
            if (p == null || o == null || main == null) {
                out.add(new Checker.Issue("swept", "part " + e.getKey() + " is declared turning but has no part or origin"));
                continue;
            }
            out.addAll(org.erisia.cad.Motion.sweep(e.getKey(), p.solids, o, e.getValue(), main.solids));
        }
        Map<String, Double> belt = new java.util.HashMap<>();
        boolean in = false;
        for (String line : n.txt) {
            String[] w = line.split(" ");
            if (w[0].equals("o")) in = w.length > 1 && w[1].equals("beltlink");
            else if (in && w[0].equals("f") && w.length > 2) belt.put(w[1], Double.parseDouble(w[2]));
        }
        Model link = n.parts.get("beltlink");
        if (link != null && belt.containsKey("ar")) {
            double z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
            for (Solid s : link.solids)
                for (var f : s.faces())
                    for (int i = 0; i < f.n; i++) {
                        z0 = Math.min(z0, f.xyz[3 * i + 2]);
                        z1 = Math.max(z1, f.xyz[3 * i + 2]);
                    }
            double ax = belt.get("ax"), ay = belt.get("ay"), ra = belt.get("ar"), bx = belt.get("bx"), by = belt.get("by"), rb = belt.get("br");
            double dx = bx - ax, dy = by - ay, d = Math.hypot(dx, dy), base = Math.atan2(dy, dx), off = Math.acos((ra - rb) / d);
            double half = 0.15, ta = Math.sqrt(2 * ra * BELT_GAP) + 0.2, tb = Math.sqrt(2 * rb * BELT_GAP) + 0.2;
            List<Mesh> env = new ArrayList<>();
            for (int sgn : new int[]{-1, 1}) {
                double ang = base + sgn * off;
                double[] nn = Mc.v(Math.cos(ang), Math.sin(ang), 0);
                double[] p0 = Mc.v(ax + ra * nn[0], ay + ra * nn[1], 0), p1 = Mc.v(bx + rb * nn[0], by + rb * nn[1], 0);
                double len = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]);
                double[] t = Mc.v((p1[0] - p0[0]) / len, (p1[1] - p0[1]) / len, 0);
                double l = (len - ta - tb) / 2;
                if (l <= 0) continue;
                double mid = (ta - tb) / 2;
                env.add(OilKit.obox(Mc.v((p0[0] + p1[0]) / 2 + t[0] * mid, (p0[1] + p1[1]) / 2 + t[1] * mid, (z0 + z1) / 2), t, nn, Mc.v(0, 0, 1),
                    l, half + BELT_GAP, (z1 - z0) / 2 + 0.1, "rubber"));
            }
            List<Solid> others = new ArrayList<>();
            for (var e : n.parts.entrySet())
                if (!e.getKey().equals("beltlink") && !e.getKey().equals("beltmark")) others.addAll(e.getValue().solids);
            out.addAll(org.erisia.cad.Motion.clearance("the belt (" + BELT_GAP + " px gap)", env, others));
        }
        return out;
    }

    /** The checker's issues for every catalog model, as lines (empty: all pass). */
    public static List<String> check() {
        List<String> out = new ArrayList<>();
        for (NodeModel n : nodes()) {
            for (Checker.Issue is : Checker.check(whole(n))) out.add(n.name + ": " + is);
            for (Checker.Issue is : motion(n)) out.add(n.name + ": " + is);
        }
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
