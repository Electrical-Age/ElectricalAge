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
            "first person, after 1.12 transformSideFirstPerson (== 1.7.10's), before equipped_tail: six-node items and "
                + "items without the render helper only");
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
        // Held items whose shouldUseRenderHelper(type, EQUIPPED_BLOCK) is true (1.7.10: Forge's translate -0.5 -0.5 -0.5
        // helper frame instead of equipped_tail, and RenderPlayer's block branch in third person). Six-node items are
        // not in these groups (first_person / third_person_six_node + equipped_tail). Math: core-log "Client test 3".
        def("equipped_helper_first_person", "translate -0.15 -0.02 0; rotate 45 0 1 0; scale 0.2 0.2 0.2; translate -0.5 -0.5 -0.5",
            "icon items with the render helper (tools, flashlight, portable battery, brush), first person, after 1.12 "
                + "transformSideFirstPerson: first_person + the helper translate (client test 3 tuning)");
        def("equipped_helper_third_person", "translate 0 0.25 0.03125; rotate -90 0 1 0; rotate 55 0 0 1; scale 0.85 0.85 0.85; "
                + "translate 0 -0.2 0; rotate 90 0 1 0; translate -0.5 -0.5 -0.5",
            "icon items with the render helper, third person, in the 1.12 hand frame (no *_undo/third_person_arm; "
                + "mirrored for the left hand): vanilla item/handheld pose, then EA's icon quad mapped onto the vanilla icon");
        def("equipped_model_first_person", "translate -0.1 0.1 0; rotate 45 0 1 0; scale 0.25 0.25 0.25; translate -0.5 -0.5 -0.5",
            "OBJ-model items with the render helper (X-ray scanner, fuse), first person, after 1.12 "
                + "transformSideFirstPerson: the 1.7.10 frame (rotate 45, scale 0.4) + Forge's helper translate");
        def("equipped_model_third_person", "translate 0 0.1875 -0.3125; rotate 20 1 0 0; rotate 45 0 1 0; "
                + "scale -0.375 -0.375 0.375; translate -0.5 -0.5 -0.5",
            "OBJ-model items with the render helper, third person, after third_person_*_undo and third_person_arm: "
                + "1.7.10 RenderPlayer block branch + Forge's helper translate");
        def("node_first_person", "translate -0.08 0.15 -0.08; rotate 45 0 1 0; scale 0.3 0.3 0.3",
            "transparent-node items (machines, batteries...), first person, after 1.12 transformSideFirstPerson: "
                + "vanilla block/block pose (models are centred on the origin like the vanilla cube)");
        def("node_third_person", "translate 0 0.15625 0; rotate 75 1 0 0; rotate 45 0 1 0; scale 0.375 0.375 0.375",
            "transparent-node items, third person, in the 1.12 hand frame (mirrored for the left hand): vanilla "
                + "block/block pose");
        def("ground", "translate 0 -0.25 0", "dropped item: cancel 1.12's +0.25 lift (bob and spin stay)");
        def("fixed", "", "item frame");
        def("head", "translate 0 -0.25 0", "on a head");
        def("entity_six_node", "translate 0 -0.15 0; scale 0.4 0.4 0.4",
            "after ground/fixed/head: six-node items with a model (1.7.10 3D path)");
        def("entity_item", "scale 0.5 0.5 0.5", "after ground/fixed/head: other items");
        // Icon-only six-node items (cables): their extruded icon model, with these matrices instead of vanilla
        // item/generated's (a full-width rod there: oversized in first person, across the torso in third person)
        def("icon_first_person", "translate -0.05 0.25 0.070625; rotate -90 0 1 0; rotate 45 0 0 1; scale 0.25 0.25 0.25",
            "icon-only six-node items (cables), first person (client test 3 tuning)");
        def("icon_third_person", "translate 0 0.25 0.03125; rotate -90 0 1 0; rotate 55 0 0 1; scale 0.5 0.5 0.5",
            "icon-only six-node items (cables), third person: held like a tool (vanilla item/handheld angles), scale 0.5");
        def("icon_ground", "translate 0 -0.12 0; scale 0.5 0.5 0.5",
            "icon-only six-node items (cables), dropped (client test 3 tuning: stands on its shadow)");
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
        // client test 3 (AdventurAgent): cable tuning became the defaults
        SUPERSEDED.put("icon_first_person", "translate 0.070625 0.2 0.070625; rotate -90 0 1 0; rotate 25 0 0 1; scale 0.4 0.4 0.4");
        SUPERSEDED.put("icon_ground", "scale 0.5 0.5 0.5");
        // client test 4 tuning (AdventurAgent)
        SUPERSEDED.put("equipped_model_first_person", "rotate 45 0 1 0; scale 0.4 0.4 0.4; translate -0.5 -0.5 -0.5");
        SUPERSEDED.put("node_first_person", "rotate 45 0 1 0; scale 0.4 0.4 0.4");
    }

    /** Keys no longer read; removed from the file on load (equipped_helper: split into the equipped_helper_* / equipped_model_* / node_* pairs). */
    private static final String[] OBSOLETE = {"equipped_helper"};

    private static void def(String key, String ops, String comment) {
        DEFAULTS.put(key, new String[]{ops, comment});
    }

    private static final Map<String, float[][]> OPS = new LinkedHashMap<>();
    private static final Map<String, javax.vecmath.Matrix4f> MATRICES = new java.util.HashMap<>();
    private static Configuration config;

    private ItemTransforms() {
    }

    /** (Re)load config/eln-render.cfg; returns problems (empty = fine). */
    public static synchronized List<String> load() {
        List<String> problems = new ArrayList<>();
        File file = new File(Loader.instance().getConfigDir(), "eln-render.cfg");
        config = new Configuration(file);
        MATRICES.clear();
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
        for (String key : OBSOLETE) {
            if (config.getCategory("transforms").containsKey(key)) {
                config.getCategory("transforms").remove(key);
                config.save();
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

    /**
     * The ops of a key as one matrix (GL order: M = op1 * op2 * ...), for baked-model perspective transforms; null when
     * the key has no ops.
     */
    public static synchronized javax.vecmath.Matrix4f matrix(String key) {
        if (config == null) load();
        if (MATRICES.containsKey(key)) return MATRICES.get(key);
        float[][] ops = OPS.get(key);
        javax.vecmath.Matrix4f m = null;
        if (ops != null && ops.length > 0) {
            m = new javax.vecmath.Matrix4f();
            m.setIdentity();
            for (float[] v : ops) {
                javax.vecmath.Matrix4f op = new javax.vecmath.Matrix4f();
                op.setIdentity();
                if (v[0] == 0) {
                    op.setTranslation(new javax.vecmath.Vector3f(v[1], v[2], v[3]));
                } else if (v[0] == 1) {
                    javax.vecmath.Vector3f axis = new javax.vecmath.Vector3f(v[2], v[3], v[4]);
                    if (axis.length() == 0) continue;
                    axis.normalize();
                    op.set(new javax.vecmath.AxisAngle4f(axis, (float) Math.toRadians(v[1])));
                } else {
                    op.m00 = v[1];
                    op.m11 = v[2];
                    op.m22 = v[3];
                }
                m.mul(op);
            }
        }
        MATRICES.put(key, m);
        return m;
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
