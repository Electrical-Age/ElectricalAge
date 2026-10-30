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
    private final List<double[]> boxes;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    private ModelHitbox(List<double[]> boxes) {
        this.boxes = boxes;
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
            return boxes.isEmpty() ? null : new ModelHitbox(boxes);
        } catch (Exception e) {
            return null;
        }
    }

    /** Its boxes turned onto FRONT (block coordinates 0..1). */
    public synchronized VoxelShape shape(Direction front) {
        return shapes.computeIfAbsent(front, f -> {
            VoxelShape s = Shapes.empty();
            for (double[] b : boxes) {
                double[] r = turned(b, f);
                s = Shapes.or(s, Shapes.box(r[0], r[1], r[2], r[3], r[4], r[5]));
            }
            return s.optimize();
        });
    }

    /** A box (0..1, front XN) turned as glRotateXnRef turns the model about the block's centre line: XP 180 degrees about
     *  y, ZP 90, ZN 270 ((x, z) -> (z, -x) for 90, centred). Vertical fronts aren't turned (shaft parts face sideways). */
    static double[] turned(double[] b, Direction f) {
        double x0 = b[0] - 0.5, x1 = b[3] - 0.5, z0 = b[2] - 0.5, z1 = b[5] - 0.5;
        double nx0, nx1, nz0, nz1;
        switch (f) {
            case XP -> { nx0 = -x1; nx1 = -x0; nz0 = -z1; nz1 = -z0; }
            case ZP -> { nx0 = z0; nx1 = z1; nz0 = -x1; nz1 = -x0; }
            case ZN -> { nx0 = -z1; nx1 = -z0; nz0 = x0; nz1 = x1; }
            default -> { nx0 = x0; nx1 = x1; nz0 = z0; nz1 = z1; }
        }
        return new double[]{nx0 + 0.5, b[1], nz0 + 0.5, nx1 + 0.5, b[4], nz1 + 0.5};
    }
}
