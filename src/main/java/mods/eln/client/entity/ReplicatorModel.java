package mods.eln.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * The replicator (1.12 ReplicatorModel, Techne): a low spider, legs spread nearly flat, 64x64 texture. 1.12 drew it
 * static (no leg animation); the head follows the look direction here, as the spider's does.
 */
public class ReplicatorModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("eln", "replicator"), "main");

    private final ModelPart head;

    public ReplicatorModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4).mirror().addBox(-4, -4, -8, 8, 8, 8),
            PartPose.offset(0, 20, -3));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-3, -3, -3, 6, 6, 6),
            PartPose.offset(0, 20, 0));
        root.addOrReplaceChild("rear_end", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-5, -4, -6, 10, 8, 12),
            PartPose.offset(0, 20, 9));
        CubeListBuilder left = CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-1, -1, -1, 16, 2, 2);
        CubeListBuilder right = CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-15, -1, -1, 16, 2, 2);
        float tilt = 0.1919862f;
        float[] yaw = {0.5759587f, 0.2792527f, -0.2792527f, -0.5759587f};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("left_leg" + i, left, PartPose.offsetAndRotation(4, 20, i - 1, 0, yaw[i], tilt));
            root.addOrReplaceChild("right_leg" + i, right, PartPose.offsetAndRotation(-4, 20, i - 1, 0, -yaw[i], -tilt));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        head.yRot = state.yRot * (float) (Math.PI / 180.0);
        head.xRot = state.xRot * (float) (Math.PI / 180.0);
    }
}
