package mods.eln.bike;

import org.erisia.cad.Box;
import org.erisia.cad.Model;
import org.erisia.cad.Part;
import org.erisia.cad.Solid;

/**
 * The electric bike's and the terrain bike's solids (board #44: the checked model library), in px with y up from the
 * ground, the front toward -z: parts (wheels, cranks, kickstand turn) holding boxes, each in a texture region of one
 * material. BikeModel (client) turns them into the entity model (EntityLayout: y down, ground at 24); BikeTexture paints
 * the regions; the tests run the checker over both bikes. The numbers are BikeGeometry's (y down, shared with the dye
 * aiming), turned y up here with {@link #up}.
 */
public final class BikeShape {
    private BikeShape() {
    }

    /** The entity model's ground (y down, px): authoring y = GROUND - model y. */
    public static final float GROUND = 24;

    /** Texture regions (u, v): each holds one material, big enough for its largest box's net. PAINT, TRUNK, LID and
     *  SADDLE are neutral, tinted per dye group (BikeRenderer). */
    public static final int[] PAINT = {0, 0}, TIRE = {28, 0}, SADDLE = {44, 0}, TRUNK = {0, 14}, LID = {28, 14}, METAL = {0, 26},
        BATTERY = {28, 23}, MOTOR = {46, 23}, BLACK = {28, 32};
    /** Each region's size (texels), and the texture's. */
    public static final int[][] REGION_SIZE = {{28, 14}, {16, 7}, {16, 7}, {28, 12}, {32, 9}, {28, 10}, {18, 9}, {16, 8}, {12, 8}};
    public static final int[][] REGIONS = {PAINT, TIRE, SADDLE, TRUNK, LID, METAL, BATTERY, MOTOR, BLACK};
    public static final String[] REGION_NAME = {"paint", "tire", "saddle", "trunk", "lid", "metal", "battery", "motor", "black"};
    public static final int TEX_W = 64, TEX_H = 64;

    /** Model y (down) to authoring y (up). */
    static float up(float y) {
        return GROUND - y;
    }

    static final float R = BikeGeometry.R, HUB = up(BikeGeometry.HUB_Y), FRONT_Z = BikeGeometry.FRONT_Z, REAR_Z = BikeGeometry.REAR_Z,
        HEAD_Z = BikeGeometry.HEAD_Z, HEAD_TOP = up(BikeGeometry.HEAD_TOP), HEAD_BOT = up(BikeGeometry.HEAD_BOT), BB = up(BikeGeometry.BB_Y),
        BB_Z = BikeGeometry.BB_Z, SEAT_Z = BikeGeometry.SEAT_Z, SEAT = up(BikeGeometry.SEAT_Y);
    static final float PI = (float) Math.PI;
    /** Vanilla's texture coordinates on every box (for the checker's look comparison and previews). */
    static final org.erisia.cad.UvMap UV = org.erisia.cad.entity.EntityLayout.uv(TEX_W, TEX_H);

    /** The battery's place along the down tube (shares of its length) and its size across (px). */
    static final float BATTERY_FROM = 0.15f, BATTERY_LEN = 0.5f, BATTERY_W = 3, BATTERY_H = 2.6f;

    /** The building model: the tubes' thickness factor (the terrain bike's are fatter). */
    static final class Build {
        final Model m = new Model();
        final float thick;

        Build(boolean terrain) {
            thick = terrain ? 1.35f : 1;
        }

        Part part(String name, Part parent, float x, float y, float z, float rx, float ry, float rz) {
            return m.part(name, parent, x, y, z, rx, ry, rz);
        }

        /** A box given as vanilla's addBox (corner and size, y down in the part's frame), made y up. */
        Solid cube(Part p, int[] region, float x, float y, float z, float w, float h, float d) {
            Box b = m.add(new Box(x, -y - h, z, x + w, -y, z + d));
            b.in(p).tex(region[0], region[1]).material(region).named(p.name).uv(UV);
            return b;
        }

        /** A box {x0, y0, z0, x1, y1, z1} in model px (y down, BikeGeometry), made y up. */
        Solid cube(Part p, int[] region, float[] b) {
            return cube(p, region, b[0], b[1], b[2], b[3] - b[0], b[4] - b[1], b[5] - b[2]);
        }

        /** A tube of thickness t from (y0, z0) to (y1, z1) at x (y up): a square box along z, its part turned about x. */
        Solid tube(String name, int[] region, float x, float y0, float z0, float y1, float z1, float t) {
            if (region == PAINT) t *= thick;
            float dy = y1 - y0, dz = z1 - z0, len = (float) Math.sqrt(dy * dy + dz * dz);
            float th = (float) Math.atan2(-dy, dz);
            Part p = part(name, null, x, y0, z0, th, 0, 0);
            return cube(p, region, -t / 2, -t / 2, 0, t, t, len);
        }

        void wheel(String name, float z) {
            Part w = part(name, null, 0, HUB, z, 0, 0, 0);
            float seg = 2 * R * (float) Math.tan(Math.PI / 8) + 0.2f;
            boolean fat = thick > 1;   // (the terrain bike's tyres: wider, deeper, with knobs on the tread)
            float tw = fat ? 3.2f : 2, td = fat ? 2.2f : 1.5f;
            for (int i = 0; i < 8; i++)
                cube(part("rim" + i, w, 0, 0, 0, -(i * PI / 4), 0, 0), TIRE, -tw / 2, -R, -seg / 2, tw, td, seg);
            if (fat)
                for (int i = 0; i < 16; i++)
                    cube(part("knob" + i, w, 0, 0, 0, -(i * PI / 8), 0, 0), TIRE, i % 2 == 0 ? -1.5f : 0.2f, -R - 0.4f, -0.6f, 1.3f, 0.6f, 1.2f);
            for (int i = 0; i < 4; i++)
                cube(part("spoke" + i, w, 0, 0, 0, -(i * PI / 4), 0, 0), METAL, -0.25f, -R + 1, -0.25f, 0.5f, 2 * R - 2, 0.5f);
            cube(part("hub", w, 0, 0, 0, 0, 0, 0), METAL, -1.5f, -1, -1, 3, 2, 2);
        }
    }

    /** The bike (terrain: the terrain bike), with what it means: the battery stands on the down tube's surface, clear of
     *  the seat tube, however thick the tube is drawn. */
    public static Model model(boolean terrain) {
        Build b = new Build(terrain);
        b.wheel("front_wheel", FRONT_Z);
        b.wheel("rear_wheel", REAR_Z);
        // the hub motor in the back wheel (it's electric)
        b.cube(b.part("motor", b.m.part("rear_wheel"), 0, 0, 0, 0, 0, 0), MOTOR, -2, -2, -2, 4, 4, 4);
        // the frame: head tube, top tube, down tube, seat tube, chain and seat stays, fork
        b.tube("head_tube", PAINT, 0, HEAD_TOP, HEAD_Z, HEAD_BOT - 1, HEAD_Z - 1, 1.8f);
        b.tube("top_tube", PAINT, 0, HEAD_TOP - 1, HEAD_Z, SEAT - 1.5f, SEAT_Z - 1, 1.5f);
        Solid down = b.tube("down_tube", PAINT, 0, HEAD_BOT, HEAD_Z - 0.5f, BB, BB_Z, 1.6f);
        Solid seat = b.tube("seat_tube", PAINT, 0, BB, BB_Z, SEAT - 0.5f, SEAT_Z - 0.6f, 1.5f);
        for (float x : new float[]{-1.2f, 1.2f}) {
            String s = x < 0 ? "_l" : "_r";
            b.tube("chain_stay" + s, PAINT, x, BB, BB_Z, HUB, REAR_Z, 1f);
            b.tube("seat_stay" + s, PAINT, x, SEAT - 1.5f, SEAT_Z - 0.4f, HUB, REAR_Z, 1f);
            if (!terrain) {
                b.tube("fork" + s, PAINT, x * 1.1f, HEAD_BOT, HEAD_Z - 1, HUB, FRONT_Z, 1.1f);
            } else {
                // a suspension fork: bare stanchions into fat lower legs (the dyeable part)
                float my = HEAD_BOT + 0.45f * (HUB - HEAD_BOT), mz = HEAD_Z - 1 + 0.45f * (FRONT_Z - HEAD_Z + 1);
                b.tube("fork_upper" + s, METAL, x * 1.2f, HEAD_BOT, HEAD_Z - 1, my - 1, mz - 0.27f, 1.1f);
                b.tube("fork" + s, PAINT, x * 1.3f, my, mz, HUB, FRONT_Z, 1.4f);
            }
        }
        // the battery along the down tube, standing on its drawn surface (Baughn 2026-10-30: it floated over the bike's
        // tube and nearly met the terrain bike's fatter one), in the down tube's frame; the bottom bracket and cranks
        Box tube = (Box) down;
        float dy = BB - HEAD_BOT, dz = BB_Z - (HEAD_Z - 0.5f), len = (float) Math.sqrt(dy * dy + dz * dz);
        Part bp = b.part("battery", null, 0, HEAD_BOT, HEAD_Z - 0.5f, (float) down.part.rx, 0, 0);
        float surface = (float) tube.hi[1];
        Solid battery = b.m.add(new Box(-BATTERY_W / 2, surface, len * BATTERY_FROM, BATTERY_W / 2, surface + BATTERY_H, len * (BATTERY_FROM + BATTERY_LEN)));
        battery.in(bp).tex(BATTERY[0], BATTERY[1]).material(BATTERY).named("battery").uv(UV);
        b.m.abut(battery, down).disjoint(battery, seat).disjoint(battery, b.m.solid("head_tube"));
        b.cube(b.part("bottom_bracket", null, 0, BB, BB_Z, 0, 0, 0), METAL, -2, -1, -1, 4, 2, 2);
        Part cl = b.part("crank_l", null, -2.2f, BB, BB_Z, -0.6f, 0, 0);
        b.cube(cl, BLACK, -0.5f, -0.5f, -0.5f, 1, 4, 1);
        b.cube(cl, BLACK, -2.5f, 3, -1, 2, 1, 2);
        Part cr = b.part("crank_r", null, 2.2f, BB, BB_Z, -(0.6f + PI), 0, 0);
        b.cube(cr, BLACK, -0.5f, -0.5f, -0.5f, 1, 4, 1);
        b.cube(cr, BLACK, 0.5f, 3, -1, 2, 1, 2);
        // seat post and saddle
        b.tube("seat_post", METAL, 0, SEAT - 0.6f, SEAT_Z - 0.6f, SEAT + 1.4f, SEAT_Z - 0.2f, 1f);
        // (BikeGeometry's boxes, which the dyes aim at too)
        Part sp = b.part("saddle", null, 0, up(BikeGeometry.SADDLE_OY), BikeGeometry.SADDLE_OZ, 0, 0, 0);
        b.cube(sp, SADDLE, BikeGeometry.SADDLE_BOXES[0]);
        b.cube(sp, SADDLE, BikeGeometry.SADDLE_BOXES[1]);
        // stem and handlebars, grips
        b.tube("stem", METAL, 0, HEAD_TOP - 0.5f, HEAD_Z, HEAD_TOP + 2.5f, HEAD_Z + 0.6f, 1f);
        b.cube(b.part("bars", null, 0, HEAD_TOP + 2.5f, HEAD_Z + 1.2f, 0, 0, 0), METAL, -5, -0.5f, -0.5f, 10, 1, 1);
        Part grips = b.part("grips", null, 0, HEAD_TOP + 2.5f, HEAD_Z + 1.2f, 0, 0, 0);
        b.cube(grips, BLACK, -7, -0.7f, -0.7f, 2.5f, 1.4f, 1.4f);
        b.cube(grips, BLACK, 4.5f, -0.7f, -0.7f, 2.5f, 1.4f, 1.4f);
        // the lamp on the front (EA makes lamps)
        b.cube(b.part("lamp", null, 0, HEAD_TOP - 1.5f, HEAD_Z - 1.2f, 0, 0, 0), MOTOR, -1, -1, -1.5f, 2, 2, 1.5f);
        // the rack over the back wheel and the trunk on it (Baughn: a small trunk on the back)
        b.cube(b.part("rack", null, 0, HUB + R + 2.2f, REAR_Z - 3.5f, 0, 0, 0), METAL, -3, 0, 0, 6, 0.6f, 7);
        // its front braced down to the seat stays (Baughn: the front lacked a support)
        float rackY = HUB + R + 1.6f, stayZ = SEAT_Z + 1.5f,
            stayY = SEAT - 1.5f - (stayZ - (SEAT_Z - 0.4f)) / (REAR_Z - (SEAT_Z - 0.4f)) * (SEAT - 1.5f - HUB);
        b.tube("rack_brace_l", METAL, -1.9f, rackY, REAR_Z - 3.2f, stayY, stayZ, 0.6f);
        b.tube("rack_brace_r", METAL, 1.9f, rackY, REAR_Z - 3.2f, stayY, stayZ, 0.6f);
        b.tube("rack_strut_l", METAL, -2.6f, HUB + R + 1.8f, REAR_Z + 3.5f, HUB, REAR_Z + 0.5f, 0.6f);
        b.tube("rack_strut_r", METAL, 2.6f, HUB + R + 1.8f, REAR_Z + 3.5f, HUB, REAR_Z + 0.5f, 0.6f);
        b.cube(b.part("trunk", null, 0, up(BikeGeometry.RACK_Y), BikeGeometry.TRUNK_OZ, 0, 0, 0), TRUNK, BikeGeometry.TRUNK_BOX);
        b.cube(b.part("trunk_lid", null, 0, up(BikeGeometry.RACK_Y), BikeGeometry.TRUNK_OZ, 0, 0, 0), LID, BikeGeometry.LID_BOX);
        // the kickstand: pivots at the bottom bracket's back, down to the ground on the left; folds back along the stay
        Part k = b.part("kickstand", null, -1.5f, BB, BB_Z + 2.5f, 0, 0, 0);
        b.cube(k, BLACK, -0.5f, 0, -0.5f, 1, 6.2f, 1);
        b.cube(k, BLACK, -1, 5.7f, -1, 2, 0.6f, 2);
        return b.m;
    }

    /** The battery box (for the texture's stripe). */
    public static Box battery(Model m) {
        return (Box) m.solid("battery");
    }
}
