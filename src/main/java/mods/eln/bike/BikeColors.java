package mods.eln.bike;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/**
 * The bike's paint (Baughn 2026-10-28: "dyeable, preferably per frame element"): four groups, each a vanilla dye colour
 * or its default (the frame's and fork's EA blue, the trunk's slate, the saddle's leather). Right-click a part with a
 * dye to paint its group (BikeModel.Group has the parts). Saved on the entity and, picked up, on its item.
 */
public final class BikeColors {
    private BikeColors() {
    }

    public static final int FRAME = 0, FORK = 1, TRUNK = 2, SADDLE = 3, GROUPS = 4;
    /** Not dyed: draw the group's default. */
    public static final int DEFAULT = -1;
    /** The defaults as tints over the model's neutral (226 grey) texture: the colours it had before dyes. */
    public static final int[] DEFAULTS = {0x3466BC, 0x3466BC, 0x4F5F6D, 0x3B2D26};
    static final String KEY = "eln_bike_colors";

    /** The tint to draw group g with, given its stored colour. */
    public static int tint(int g, int stored) {
        return stored == DEFAULT ? DEFAULTS[g] : stored;
    }

    /** How far a dye reaches along the look ray (blocks): a player's entity interaction range and a little. */
    public static final double REACH = 4.5;

    /**
     * Which group a look ray (eye, unit direction, in the world) meets first on a bike at pos facing yaw (degrees), or
     * -1: the ray in the bike's frame (side, up, forward: yaw-rotated, so the boxes turn with the bike) against a box per
     * part, nearest hit wins. Not the entity's bounding box: that is 0.8 square and says nothing about which part
     * (AdventurAgent: side-on clicks on the frame missed, a saddle click painted the trunk).
     */
    public static int pick(Vec3 eye, Vec3 dir, Vec3 pos, float yaw) {
        float yr = yaw * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yr), fz = Mth.cos(yr);   // forward; side is (fz, -fx)
        Vec3 d = eye.subtract(pos);
        double[] o = {d.x * fz - d.z * fx, d.y, d.x * fx + d.z * fz};
        double[] r = {dir.x * fz - dir.z * fx, dir.y, dir.x * fx + dir.z * fz};
        int best = -1;
        double bestT = Double.MAX_VALUE;
        for (double[] b : BOXES) {
            double t = hit(o, r, new double[]{-b[1], b[2], b[4]}, new double[]{b[1], b[3], b[5]});
            if (t >= 0 && t <= REACH && t < bestT) {
                bestT = t;
                best = (int) b[0];
            }
        }
        return best;
    }

    /**
     * {group, half-width, up from, up to, forward from, forward to}, in blocks, from BikeModel's geometry (model px:
     * forward = -z / 16, up = (24 - y) / 16, side = x / 16), each a little generous but apart from its neighbours:
     * the saddle (z 0-5.5, y 6.7-9.2, x +-2.5) with its post; the trunk and lid (z 6.2-13.8, y 5.3-10.8, x +-3.8);
     * the fork (z -7 to -10, y 11-18.5, x +-1.3); the frame's tubes and stays (z -8 to 10, y 8-18.5), as three boxes:
     * the head and top tube, the down and seat tubes, the stays.
     */
    static final double[][] BOXES = {
        {SADDLE, 0.20, 0.86, 1.13, -0.37, 0.03},
        {TRUNK, 0.27, 0.78, 1.21, -0.89, -0.38},
        {FORK, 0.14, 0.28, 0.80, 0.50, 0.70},
        {FRAME, 0.10, 0.78, 1.02, -0.20, 0.52},   // head tube and top tube
        {FRAME, 0.10, 0.30, 0.86, -0.20, 0.50},   // down tube, seat tube, bottom bracket
        {FRAME, 0.12, 0.30, 0.86, -0.66, -0.20},  // chain and seat stays
    };

    /** Slab test: the distance along r where the ray from o enters the box [lo, hi], or -1 if it misses. */
    static double hit(double[] o, double[] r, double[] lo, double[] hi) {
        double t0 = 0, t1 = 16;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(r[i]) < 1e-9) {
                if (o[i] < lo[i] || o[i] > hi[i]) return -1;
                continue;
            }
            double a = (lo[i] - o[i]) / r[i], b = (hi[i] - o[i]) / r[i];
            t0 = Math.max(t0, Math.min(a, b));
            t1 = Math.min(t1, Math.max(a, b));
            if (t0 > t1) return -1;
        }
        return t0;
    }

    /** An item's colours (all DEFAULT if it has none). */
    public static int[] of(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        int[] c = d == null ? new int[0] : d.copyTag().getIntArray(KEY).orElse(new int[0]);
        int[] out = new int[GROUPS];
        for (int g = 0; g < GROUPS; g++) out[g] = g < c.length ? c[g] : DEFAULT;
        return out;
    }

    /** Store colours on an item (nothing if all are default, so plain bikes stack and compare as plain). */
    public static void put(ItemStack s, int[] c) {
        boolean plain = true;
        for (int v : c) plain &= v == DEFAULT;
        if (plain) return;
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        t.putIntArray(KEY, c.clone());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }
}
