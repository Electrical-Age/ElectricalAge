package mods.eln.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.erisia.cad.Face;
import org.erisia.cad.Flat;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A baked entity model's polygons as vanilla draws them, as faces for the checker and the previews: model px turned
 * y up (y = 24 - y), each face's look its texture and its top-level part's dye group (a colour of its own per group).
 */
final class BikeFaces {
    private BikeFaces() {
    }

    static List<Face> of(ModelPart root, Map<String, ?> groups) {
        List<Face> out = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            String top = path.split("/").length > 1 ? path.split("/")[1] : path;
            Object group = groups.get(top);
            int argb = group == null ? 0xFFFFFFFF : 0xFF000000 | (group.toString().hashCode() & 0xFFFFFF);
            for (ModelPart.Polygon poly : cube.polygons) {
                int n = poly.vertices().length;
                double[] xyz = new double[3 * n];
                float[] uv = new float[2 * n];
                for (int i = 0; i < n; i++) {
                    // reversed: the reflection in y turns the winding round
                    ModelPart.Vertex v = poly.vertices()[n - 1 - i];
                    Vector3f p = pose.pose().transformPosition(v.x() / 16, v.y() / 16, v.z() / 16, new Vector3f());
                    xyz[3 * i] = p.x * 16;
                    xyz[3 * i + 1] = 24 - p.y * 16;
                    xyz[3 * i + 2] = p.z * 16;
                    uv[2 * i] = v.u();
                    uv[2 * i + 1] = v.v();
                }
                out.add(Flat.face(xyz, uv, argb, "bike texture", path + "#" + index));
            }
        });
        return out;
    }
}
