package org.erisia.cad.entity;

import org.erisia.cad.Box;
import org.erisia.cad.Face;
import org.erisia.cad.Frame;
import org.erisia.cad.Model;
import org.erisia.cad.Part;
import org.erisia.cad.Solid;

import java.util.ArrayList;
import java.util.List;

/**
 * The entity-model target (Minecraft's ModelPart cubes): model space in px with y DOWN and the ground at y = ground, front
 * toward -z. A model for it is parts holding boxes (no other solids: vanilla draws only cubes), each box with its
 * texture region's corner (Solid.tex). Authoring is y up from the ground; this flips: a root part's pivot y becomes
 * ground - y, a child's -y, a box's y range [y0, y1] becomes [-y1, -y0], and rotations about x and z change sign (a
 * reflection in y conjugates Rz Ry Rx into Rz(-z) Ry(y) Rx(-x)).
 * <p>
 * Texture: vanilla unwraps a box of size (w, h, d) at (u, v) into a net 2(d + w) wide and d + h tall, float sizes kept
 * (26.x: ModelPart.Cube takes the sizes as floats; only the corner is an int). {@link #net} gives each face's rectangle.
 */
public final class EntityLayout {
    private EntityLayout() {
    }

    /** One vanilla cube: corner and size in the part's frame (y down), texture corner. */
    public record EBox(float x, float y, float z, float w, float h, float d, int u, int v) {
    }

    /** One vanilla part: its pivot (y down) and rotation, its parent's name (null: the root), its cubes. */
    public record EPart(String name, String parent, float ox, float oy, float oz, float rx, float ry, float rz, List<EBox> boxes) {
    }

    public static List<EPart> layout(Model m, double ground) {
        List<EPart> out = new ArrayList<>();
        for (Part p : m.parts) {
            List<EBox> boxes = new ArrayList<>();
            for (Solid s : m.solids) {
                if (s.part != p || s.ghost) continue;
                if (!(s instanceof Box b) || !s.frame.identity())
                    throw new IllegalArgumentException(s + ": an entity model draws only unrotated boxes in their part (turn the part instead)");
                boxes.add(new EBox((float) b.lo[0], (float) -b.hi[1], (float) b.lo[2], (float) (b.hi[0] - b.lo[0]), (float) (b.hi[1] - b.lo[1]),
                    (float) (b.hi[2] - b.lo[2]), s.texU, s.texV));
            }
            double oy = p.parent == null ? ground - p.oy : -p.oy;
            out.add(new EPart(p.name, p.parent == null ? null : p.parent.name, (float) p.ox, (float) oy, (float) p.oz, (float) -p.rx, (float) p.ry,
                (float) -p.rz, boxes));
        }
        return out;
    }

    /** Face numbers in Box order: -x +x -y +y -z +z (y up). */
    public static final String[] FACE_NAME = {"-x", "+x", "-y (bottom)", "+y (top)", "-z (front)", "+z (back)"};

    /**
     * The texture rectangle of each face of a box (Box order, y up) as vanilla's ModelPart.Cube passes it to Polygon:
     * {u0, v0, u1, v1} texels, corner 0 of the polygon at (u1, v0), 1 at (u0, v0), 2 at (u0, v1), 3 at (u1, v1); the
     * bottom's v runs backwards (take min and max for the area). Its top (+y up) is vanilla's DOWN face (y down in model space), its bottom vanilla's UP.
     */
    public static float[][] net(Box b) {
        float w = (float) (b.hi[0] - b.lo[0]), h = (float) (b.hi[1] - b.lo[1]), d = (float) (b.hi[2] - b.lo[2]);
        float u0 = b.texU, u1 = u0 + d, u2 = u1 + w, u22 = u2 + w, u3 = u2 + d, u4 = u3 + w, v0 = b.texV, v1 = v0 + d, v2 = v1 + h;
        return new float[][]{
            {u0, v1, u1, v2},     // -x: WEST
            {u2, v1, u3, v2},     // +x: EAST
            {u2, v1, u22, v0},    // -y up = model +y: vanilla UP (v runs up: its rows are flipped)
            {u1, v0, u2, v1},     // +y up = model -y: vanilla DOWN
            {u1, v1, u2, v2},     // -z: NORTH
            {u3, v1, u4, v2}};    // +z: SOUTH
    }

    /** The whole net {u0, v0, u1, v1}: 2(d + w) by d + h. */
    public static float[] netBounds(Box b) {
        float w = (float) (b.hi[0] - b.lo[0]), h = (float) (b.hi[1] - b.lo[1]), d = (float) (b.hi[2] - b.lo[2]);
        return new float[]{b.texU, b.texV, b.texU + 2 * (d + w), b.texV + d + h};
    }

    /**
     * A UV map giving each face of a box vanilla's texture coordinates (texels / size), corner by corner as
     * ModelPart.Polygon assigns them, so the checker compares what is drawn.
     */
    public static org.erisia.cad.UvMap uv(int texW, int texH) {
        return f -> {
            Box b = (Box) f.solid;
            float[] r = net(b)[f.index];
            // vanilla's corners per face (y down): which local corner gets (u1,v0), (u0,v0), (u0,v1), (u1,v1)
            int[] order = VANILLA[f.index];
            float[] uv = new float[8];
            float[][] corner = {{r[2], r[1]}, {r[0], r[1]}, {r[0], r[3]}, {r[2], r[3]}};
            for (int i = 0; i < 4; i++) {
                int c = f.vid[i];
                int slot = -1;
                for (int j = 0; j < 4; j++) if (order[j] == c) slot = j;
                uv[2 * i] = corner[slot][0] / texW;
                uv[2 * i + 1] = corner[slot][1] / texH;
            }
            return uv;
        };
    }

    /**
     * Vanilla's corner order per face, as Box corner numbers (bit 0 x, bit 1 y UP, bit 2 z). Vanilla names corners in y
     * down: its minY is our hiY. From ModelPart.Cube: DOWN l1 l0 t0 t1, UP t2 t3 l3 l2, WEST t0 l0 l3 t3, NORTH t1 t0 t3 t2,
     * EAST l1 t1 t2 l2, SOUTH l0 l1 l2 l3, where t = minZ, l = maxZ; 0 (minX minY), 1 (maxX minY), 2 (maxX maxY), 3 (minX maxY).
     */
    static final int[][] VANILLA;

    static {
        // vanilla corner -> Box corner: x bit from minX/maxX, y bit = 1 for vanilla minY (our top), z bit from t/l
        int t0 = 2, t1 = 3, t2 = 1, t3 = 0, l0 = 6, l1 = 7, l2 = 5, l3 = 4;
        VANILLA = new int[][]{
            {t0, l0, l3, t3},   // -x WEST
            {l1, t1, t2, l2},   // +x EAST
            {t2, t3, l3, l2},   // -y up: vanilla UP (its maxY)
            {l1, l0, t0, t1},   // +y up: vanilla DOWN (its minY)
            {t1, t0, t3, t2},   // -z NORTH
            {l0, l1, l2, l3}};  // +z SOUTH
    }

    /** All faces of the model's drawn boxes with vanilla UVs (for the checker and previews). */
    public static List<Face> faces(Model m, int texW, int texH) {
        org.erisia.cad.UvMap uv = uv(texW, texH);
        List<Face> out = new ArrayList<>();
        for (Solid s : m.solids) {
            if (s.ghost) continue;
            org.erisia.cad.UvMap keep = s.uv;
            s.uv = uv;
            s.emit(out::add);
            s.uv = keep;
        }
        return out;
    }

}
