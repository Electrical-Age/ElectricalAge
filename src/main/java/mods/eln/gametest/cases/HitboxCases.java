package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.transparent.ModelHitbox;
import mods.eln.node.transparent.TransparentNodeDescriptor;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hitboxes from model geometry (Baughn 2026-10-30, todo #159): every full-block machine with a model has a computed
 * outline that isn't empty, and the share of its model drawn outside its block is listed (a render that moves the model
 * further than the block's centre shows there).
 */
public final class HitboxCases {
    private HitboxCases() {
    }

    public static List<TestCase> cases() {
        return List.of(new Survey(), new WallDevices());
    }

    static final class Survey implements TestCase {
        public String id() {
            return "hitbox_from_geometry_survey";
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            int with = 0, empty = 0;
            List<String> lostIn = new java.util.ArrayList<>();
            for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.all()).entrySet()) {
                TransparentNodeDescriptor d = e.getValue();
                List<double[]> t = d.hitboxTriangles();
                if (t.isEmpty()) {
                    StringBuilder why = new StringBuilder();
                    for (Class<?> c = d.getClass(); c != null && c != Object.class; c = c.getSuperclass())
                        for (var f : c.getDeclaredFields()) {
                            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                            try {
                                f.setAccessible(true);
                                Object v = f.get(d);
                                if (v instanceof mods.eln.misc.Obj3D m) {
                                    why.append(" ").append(f.getName()).append("=").append(m.parts().size()).append(" parts (").append(m.filePath).append(":");
                                    for (var p : m.parts()) {
                                        var l = new java.util.ArrayList<double[]>();
                                        p.triangles(l);
                                        why.append(" ").append(p.name()).append(" ").append(l.size());
                                    }
                                    why.append(")");
                                }
                                else if (f.getType() == mods.eln.misc.Obj3D.class) why.append(" ").append(f.getName()).append("=null");
                            } catch (ReflectiveOperationException | RuntimeException x) {
                                why.append(" ").append(f.getName()).append(":").append(x.getClass().getSimpleName());
                            }
                        }
                    ctx.line("hitbox: " + d.name + ": no model parts;" + why);
                    continue;
                }
                int out = 0, n = 0;
                double[] lo = {9, 9, 9}, hi = {-9, -9, -9};
                for (double[] tri : t)
                    for (int v = 0; v < 3; v++) {
                        n++;
                        boolean o = false;
                        for (int a = 0; a < 3; a++) {
                            double c = tri[v * 3 + a];
                            lo[a] = Math.min(lo[a], c);
                            hi[a] = Math.max(hi[a], c);
                            o |= c < -0.02 || c > 1.02;
                        }
                        if (o) out++;
                    }
                ModelHitbox h = d.modelHitbox();
                // every triangle's centre inside the block lies in the outline (within half a cell: the grid's rounding)
                int lost = 0;
                if (h != null) for (double[] tri : t) {
                    double cx = (tri[0] + tri[3] + tri[6]) / 3, cy = (tri[1] + tri[4] + tri[7]) / 3, cz = (tri[2] + tri[5] + tri[8]) / 3;
                    if (cx < 0 || cx > 1 || cy < 0 || cy > 1 || cz < 0 || cz > 1) continue;
                    boolean in = false;
                    double m = 1 / 32.0;
                    for (double[] b : h.boxes())
                        in |= cx >= b[0] - m && cx <= b[3] + m && cy >= b[1] - m && cy <= b[4] + m && cz >= b[2] - m && cz <= b[5] + m;
                    if (!in) {
                        lost++;
                        if (lost == 1) ctx.line(String.format("hitbox: %s leaves out a triangle at %.4f %.4f %.4f", d.name, cx, cy, cz));
                    }
                }
                if (lost > 0) lostIn.add(d.name + " (" + lost + ")");
                double vol = 0;
                int boxes = 0;
                if (h != null) for (double[] b : h.boxes()) {
                    vol += (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
                    boxes++;
                }
                if (h == null) empty++;
                else with++;
                ctx.line(String.format("hitbox: %s: %d triangles, %.0f%% outside, model %.2f..%.2f x %.2f..%.2f y %.2f..%.2f z; %d boxes, %.2f of the block",
                    d.name, t.size(), 100.0 * out / n, lo[0], hi[0], lo[1], hi[1], lo[2], hi[2], boxes, vol));
            }
            ctx.line("hitbox: " + with + " computed or hand-written, " + empty + " without (multiblocks and models outside their block)");
            ctx.check("hitbox: every drawn triangle in its block lies in the outline", lostIn.isEmpty(), String.join(", ", lostIn));
        }
    }

    /**
     * Wall and floor devices (todo #140, #159): a device in a plain six-node block is outlined at its model's box on its
     * face, not the 3/16 plate every element had; a cable keeps the plate. A suspended lamp socket's footprint is a column
     * its socket's width (its halo plane left out); the anemometer's leaves out its undrawn ground base.
     */
    static final class WallDevices implements TestCase {
        net.minecraft.core.BlockPos chip;

        public String id() {
            return "hitbox_wall_devices";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            chip = ctx.at(1, 1, 1);
            ctx.placeSix(Ids.six("NOT Chip"), chip);
        }

        public void measure(TestContext ctx) {
            var lamp = Eln.sixNodeItem.getDescriptor(Ids.six("Suspended Lamp Socket"));
            int[] fp = lamp.footprint();
            ctx.check("suspended lamp socket: a column its socket's width", fp[0] == 2 && fp[1] == 16, java.util.Arrays.toString(fp));
            var wind = (mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor) Eln.sixNodeItem.getDescriptor(Ids.six("Electrical Anemometer Sensor"));
            int[] wf = wind.footprint();
            // (its height is its drawn parts', the wall base and the cups: the ground base, never drawn, is left out)
            var m = Eln.obj.getObj("Anemometer");
            double drawn = Math.max(m.getPart("base_wall").boundingBox().max.x, m.getPart("anemometer").boundingBox().max.x);
            double ground = m.getPart("base_ground").boundingBox().max.x;
            ctx.line(String.format("anemometer parts: drawn to %.3f, ground base to %.3f", drawn, ground));
            ctx.check("anemometer: its footprint is its drawn parts' height", wf[1] == (int) Math.ceil(drawn * 16 - 0.01), java.util.Arrays.toString(wf));
            var shape = ctx.level().getBlockState(chip).getShape(ctx.level(), chip);
            var chipDesc = Eln.sixNodeItem.getDescriptor(Ids.six("NOT Chip"));
            int[] b = chipDesc.footprintBox(mods.eln.misc.Direction.YN.getInt());
            var bounds = shape.bounds();
            ctx.line("wall devices: lamp " + java.util.Arrays.toString(fp) + ", anemometer " + java.util.Arrays.toString(wf) + ", chip outline " + bounds
                + " vs its footprint " + java.util.Arrays.toString(b));
            ctx.check("a NOT chip on the floor is outlined at its model's box, not the plate",
                chipDesc.footprintFromModel() && Math.abs(bounds.maxY - b[4] / 16.0) < 1e-9 && Math.abs(bounds.minX - b[0] / 16.0) < 1e-9,
                bounds + " vs " + java.util.Arrays.toString(b));
        }
    }
}
