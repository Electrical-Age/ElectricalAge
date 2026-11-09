package mods.eln.node.transparent;

import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A device's hitbox from its model (Baughn 2026-10-29, the shaft art pass): NAME.hitbox beside NAME.obj, one box a line
 * (x0 y0 z0 x1 y1 z1, px, corner origin, front XN: docs/models/shaft.md), turned onto the block's front as the
 * render turns the model (Direction.glRotateXnRef) and cached per front.
 */
public final class ModelHitbox {
    private final List<double[]> boxes, collisionBoxes;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class), collisions = new EnumMap<>(Direction.class);

    private ModelHitbox(List<double[]> boxes, List<double[]> collisionBoxes) {
        this.boxes = boxes;
        this.collisionBoxes = collisionBoxes;
    }

    /**
     * From a model's triangles (block units, front XN, the block's corner at the origin): the outline on the fine grid
     * and the collision on the coarse one, gaps closed (ModelShape; Baughn 2026-11-04, collision choice A); null if
     * nothing of it is in the block.
     */
    public static ModelHitbox computed(List<double[]> triangles) {
        return computedParts(List.of(triangles));
    }

    /** The same from a model's parts, each tested on its own for an inside (ModelShape). */
    public static ModelHitbox computedParts(List<List<double[]>> parts) {
        List<double[]> outline = mods.eln.misc.ModelShape.outlineOfParts(parts);
        if (outline.isEmpty()) return null;
        return new ModelHitbox(outline, mods.eln.misc.ModelShape.collisionOfParts(parts));
    }

    /**
     * Both: a model's hand-written hitbox (drawn generously on purpose, a shaft part clickable along its whole length)
     * with its computed one (which holds what the hand boxes missed). Either may be null.
     */
    public static ModelHitbox union(ModelHitbox hand, ModelHitbox computed) {
        if (hand == null || computed == null) return hand == null ? computed : hand;
        List<double[]> outline = new ArrayList<>(hand.boxes), collision = new ArrayList<>(hand.collisionBoxes);
        outline.addAll(computed.boxes);
        collision.addAll(computed.collisionBoxes);
        return new ModelHitbox(outline, collision);
    }

    /** Its outline boxes (block units, front XN). */
    public List<double[]> boxes() {
        return boxes;
    }

    /** The model's hitbox, or null (no .hitbox file, or no model). */
    public static ModelHitbox of(Obj3D obj) {
        if (obj == null || obj.filePath == null) return null;
        String path = obj.filePath.toLowerCase(java.util.Locale.ROOT).replaceAll("\\.obj$", ".hitbox");
        if (!path.startsWith("model/")) path = "model/" + path;
        try (InputStream in = ModelHitbox.class.getClassLoader().getResourceAsStream("assets/eln/" + path)) {
            if (in == null) return null;
            List<double[]> boxes = new ArrayList<>();
            for (String line : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList()) {
                String[] w = line.trim().split("\\s+");
                if (w.length != 6) continue;
                double[] b = new double[6];
                for (int i = 0; i < 6; i++) b[i] = Double.parseDouble(w[i]) / 16.0;
                boxes.add(b);
            }
            return boxes.isEmpty() ? null : new ModelHitbox(boxes, boxes);
        } catch (Exception e) {
            return null;
        }
    }

    /** Its outline turned onto FRONT (block coordinates 0..1). */
    public synchronized VoxelShape shape(Direction front) {
        return shapes.computeIfAbsent(front, f -> turnedShape(boxes, f));
    }

    /** Its collision turned onto FRONT (the outline's, for a hand-written hitbox). */
    public synchronized VoxelShape collision(Direction front) {
        return collisions.computeIfAbsent(front, f -> turnedShape(collisionBoxes, f));
    }

    static VoxelShape turnedShape(List<double[]> boxes, Direction f) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            double[] r = turned(b, f);
            s = Shapes.or(s, Shapes.box(r[0], r[1], r[2], r[3], r[4], r[5]));
        }
        return s.optimize();
    }

    /**
     * A box (0..1, front XN) turned as glRotateXnRef turns the model about the block's centre: XP 180 degrees about y,
     * ZP 90 ((x, z) -> (z, -x)), ZN 270, YP -90 about z ((x, y) -> (y, -x)), YN 90 about z after a (1, -1, -1) scale
     * ((x, y, z) -> (y, x, -z)). Up and down fronts were left unturned before (a device facing up, such as an antenna,
     * got its sideways box).
     */
    static double[] turned(double[] b, Direction f) {
        double[] p = turnedPoint(b[0] - 0.5, b[1] - 0.5, b[2] - 0.5, f), q = turnedPoint(b[3] - 0.5, b[4] - 0.5, b[5] - 0.5, f);
        return new double[]{Math.min(p[0], q[0]) + 0.5, Math.min(p[1], q[1]) + 0.5, Math.min(p[2], q[2]) + 0.5,
            Math.max(p[0], q[0]) + 0.5, Math.max(p[1], q[1]) + 0.5, Math.max(p[2], q[2]) + 0.5};
    }

    /** A point about the block's centre turned as glRotateXnRef turns the model (see turned). */
    public static double[] turnedPoint(double x, double y, double z, Direction f) {
        return switch (f) {
            case XP -> new double[]{-x, y, -z};
            case ZP -> new double[]{z, y, -x};
            case ZN -> new double[]{-z, y, x};
            case YP -> new double[]{y, -x, z};
            case YN -> new double[]{y, x, -z};
            default -> new double[]{x, y, z};
        };
    }
}
