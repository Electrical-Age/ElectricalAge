package mods.eln.client.render;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Live-tunable GL transforms of the IItemRenderer bridge ({@link ItemBridgeRenderer}), read from
 * config/eln-render.cfg (category "transforms") and reloadable in game with `/elnclient reloadrender`.
 * Each value is a ';'-separated list of ops applied in order (GL post-multiplication, like the code it replaced):
 * {@code translate x y z}, {@code rotate angle x y z}, {@code scale x y z} (or {@code t}, {@code r}, {@code s}).
 * An empty value means no op. A value that does not parse keeps the previous ops (and is reported).
 */
@SideOnly(Side.CLIENT)
public final class ItemTransforms {
    /** key -> {default ops, comment}; defaults = the 1.7.10 frames (see core-log "WP5 item transforms"). */
    private static final Map<String, String[]> DEFAULTS = new LinkedHashMap<>();

    static {
        def("inventory", "translate -0.5 0.5 0; scale 0.0625 -0.0625 0.0625",
            "GUI slot: 1.12 GUI frame -> 1.7.10 pixel frame (origin top-left, y down). Lighting is off for this type.");
        def("first_person_left", "scale -1 1 1", "extra ops before first_person for the left hand");
        def("first_person", "translate -0.15 -0.02 0; rotate 45 0 1 0; scale 0.2 0.2 0.2",
            "first person, after 1.12 transformSideFirstPerson (== 1.7.10's), before equipped_tail");
        def("third_person_right_undo", "translate -0.0625 -0.125 0.625; rotate -180 0 1 0; rotate 90 1 0 0",
            "undo 1.12 LayerHeldItem, right hand");
        def("third_person_left_undo", "translate 0.0625 -0.125 0.625; rotate -180 0 1 0; rotate 90 1 0 0",
            "undo 1.12 LayerHeldItem, left hand");
        def("third_person_arm", "translate -0.0625 0.4375 0.0625", "1.7.10 RenderPlayer after bipedRightArm.postRender");
        def("third_person_six_node", "translate 0 0.8 -0.3125; rotate 20 1 0 0; rotate 45 0 1 0; scale -0.375 -0.375 0.375",
            "six-node items with a model (1.7.10 block branch); +y moves the item down here");
        def("third_person_item", "translate 0.25 0.1875 -0.1875; scale 0.375 0.375 0.375; rotate 60 0 0 1; rotate -90 1 0 0; rotate 20 0 0 1",
            "other items (1.7.10 item branch)");
        def("equipped_tail", "translate 0 -0.3 0; scale 1.5 1.5 1.5; rotate 50 0 1 0; rotate 335 0 0 1; translate -0.9375 -0.0625 0",
            "Forge 1.7.10 renderEquippedItem (non-helper), after first_person and third_person_*");
        def("ground", "translate 0 -0.25 0", "dropped item: cancel 1.12's +0.25 lift (bob and spin stay)");
        def("fixed", "", "item frame");
        def("head", "translate 0 -0.25 0", "on a head");
        def("entity_six_node", "translate 0 -0.15 0; scale 0.4 0.4 0.4",
            "after ground/fixed/head: six-node items with a model (1.7.10 3D path)");
        def("entity_item", "scale 0.5 0.5 0.5", "after ground/fixed/head: other items");
    }

    /**
     * Earlier defaults, replaced by the client test 1 tuning (AdventurAgent, 2026-10-05): a cfg value still equal
     * to one of these is updated to the current default. Icon-only items (cables...) no longer use these keys
     * outside the GUI: they render with their vanilla generated item model (ItemBridgeModel.isIconOnly).
     */
    private static final Map<String, String> SUPERSEDED = new LinkedHashMap<>();

    static {
        SUPERSEDED.put("first_person", "rotate 45 0 1 0; scale 0.4 0.4 0.4");
        SUPERSEDED.put("third_person_six_node", "translate 0 0.1875 -0.3125; rotate 20 1 0 0; rotate 45 0 1 0; scale -0.375 -0.375 0.375");
        SUPERSEDED.put("entity_six_node", "scale 0.25 0.25 0.25");
    }

    private static void def(String key, String ops, String comment) {
        DEFAULTS.put(key, new String[]{ops, comment});
    }

    private static final Map<String, float[][]> OPS = new LinkedHashMap<>();
    private static Configuration config;

    private ItemTransforms() {
    }

    /** (Re)load config/eln-render.cfg; returns problems (empty = fine). */
    public static synchronized List<String> load() {
        List<String> problems = new ArrayList<>();
        File file = new File(Loader.instance().getConfigDir(), "eln-render.cfg");
        config = new Configuration(file);
        config.load();
        config.setCategoryComment("transforms", "Electrical Age item render transforms (client). Ops: translate x y z; "
            + "rotate angle x y z; scale x y z. Reload in game: /elnclient reloadrender");
        for (Map.Entry<String, String[]> e : DEFAULTS.entrySet()) {
            Property p = config.get("transforms", e.getKey(), e.getValue()[0], e.getValue()[1]);
            if (p.getString().trim().equals(SUPERSEDED.get(e.getKey()))) p.set(e.getValue()[0]);
            p.setComment(e.getValue()[1]);
            try {
                OPS.put(e.getKey(), parse(p.getString()));
            } catch (IllegalArgumentException ex) {
                problems.add(e.getKey() + ": " + ex.getMessage() + " (kept the previous value)");
                if (!OPS.containsKey(e.getKey())) OPS.put(e.getKey(), parse(e.getValue()[0]));
            }
        }
        if (config.hasChanged()) config.save();
        return problems;
    }

    static float[][] parse(String text) {
        List<float[]> out = new ArrayList<>();
        for (String raw : text.split(";")) {
            String op = raw.trim();
            if (op.isEmpty()) continue;
            String[] w = op.split("[\\s,()]+");
            String name = w[0].toLowerCase(Locale.ROOT);
            int kind;
            int n;
            if (name.equals("translate") || name.equals("t")) {
                kind = 0;
                n = 3;
            } else if (name.equals("rotate") || name.equals("r")) {
                kind = 1;
                n = 4;
            } else if (name.equals("scale") || name.equals("s")) {
                kind = 2;
                n = 3;
            } else {
                throw new IllegalArgumentException("unknown op '" + w[0] + "'");
            }
            if (w.length != n + 1) throw new IllegalArgumentException("'" + op + "' needs " + n + " numbers");
            float[] v = new float[n + 1];
            v[0] = kind;
            for (int i = 0; i < n; i++) {
                try {
                    v[i + 1] = Float.parseFloat(w[i + 1]);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("bad number '" + w[i + 1] + "' in '" + op + "'");
                }
            }
            out.add(v);
        }
        return out.toArray(new float[0][]);
    }

    /** Apply the ops of a key to the current GL matrix. */
    public static void apply(String key) {
        float[][] ops = OPS.get(key);
        if (ops == null) {
            if (config == null) load();
            ops = OPS.get(key);
            if (ops == null) return;
        }
        for (float[] v : ops) {
            if (v[0] == 0) GL11.glTranslatef(v[1], v[2], v[3]);
            else if (v[0] == 1) GL11.glRotatef(v[1], v[2], v[3], v[4]);
            else GL11.glScalef(v[1], v[2], v[3]);
        }
    }

    /** Current values, for `/elnclient showrender`. */
    public static synchronized List<String> describe() {
        List<String> out = new ArrayList<>();
        if (config == null) load();
        for (String key : DEFAULTS.keySet()) {
            out.add(key + " = " + config.getCategory("transforms").get(key).getString());
        }
        return out;
    }
}
