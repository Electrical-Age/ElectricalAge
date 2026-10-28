package mods.eln.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The electric bike (Baughn 2026-10-28): a diamond frame in EA blue, a battery on the down tube and a hub motor in the
 * back wheel, a trunk on a rack over it, a kickstand that folds up when ridden. Model space in pixels: y down, the
 * ground at 24, the front toward -z. Wheels are octagon rims with four spokes across; they turn with the distance rolled.
 * The texture (textures/entity/electric_bike.png, drawn by tools/bike/texture.py) is regions of one material each.
 */
public class BikeModel extends EntityModel<BikeRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("eln", "electric_bike"), "main");

    /** Wheel radius in pixels (ElectricBike.WHEEL_RADIUS in blocks), hub heights, and where things sit along z. */
    static final float R = 5.5f, HUB_Y = 24 - R, FRONT_Z = -10, REAR_Z = 10;
    static final float HEAD_Z = -7, HEAD_TOP = 8, HEAD_BOT = 11, BB_Y = 18.5f, BB_Z = 1, SEAT_Z = 3.5f, SEAT_Y = 9.5f;

    // texture regions (u, v): one material each
    static final int[] PAINT = {0, 0}, TIRE = {32, 0}, METAL = {0, 38}, SADDLE = {32, 8}, TRUNK = {0, 16}, BATTERY = {24, 16},
        MOTOR = {44, 16}, BLACK = {44, 24}, LID = {0, 28};

    private final ModelPart frontWheel, rearWheel, kickstand;

    public BikeModel(ModelPart root) {
        super(root);
        frontWheel = root.getChild("front_wheel");
        rearWheel = root.getChild("rear_wheel");
        kickstand = root.getChild("kickstand");
    }

    /** A tube of thickness t from (y0, z0) to (y1, z1) at x, as a box along z turned about x. */
    static void tube(PartDefinition root, String name, int[] uv, float x, float y0, float z0, float y1, float z1, float t) {
        float dy = y1 - y0, dz = z1 - z0, len = (float) Math.sqrt(dy * dy + dz * dz);
        float th = (float) Math.atan2(-dy, dz);
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(uv[0], uv[1]).addBox(-t / 2, -t / 2, 0, t, t, len),
            PartPose.offsetAndRotation(x, y0, z0, th, 0, 0));
    }

    static void wheel(PartDefinition root, String name, float z) {
        PartDefinition w = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0, HUB_Y, z));
        float seg = 2 * R * (float) Math.tan(Math.PI / 8) + 0.2f;
        for (int i = 0; i < 8; i++)
            w.addOrReplaceChild("rim" + i, CubeListBuilder.create().texOffs(TIRE[0], TIRE[1]).addBox(-1, -R, -seg / 2, 2, 1.5f, seg),
                PartPose.rotation(i * Mth.PI / 4, 0, 0));
        for (int i = 0; i < 4; i++)
            w.addOrReplaceChild("spoke" + i, CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-0.25f, -R + 1, -0.25f, 0.5f, 2 * R - 2, 0.5f),
                PartPose.rotation(i * Mth.PI / 4, 0, 0));
        w.addOrReplaceChild("hub", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-1.5f, -1, -1, 3, 2, 2), PartPose.ZERO);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        wheel(root, "front_wheel", FRONT_Z);
        wheel(root, "rear_wheel", REAR_Z);
        // the hub motor in the back wheel (it's electric)
        root.getChild("rear_wheel").addOrReplaceChild("motor", CubeListBuilder.create().texOffs(MOTOR[0], MOTOR[1]).addBox(-2, -2, -2, 4, 4, 4), PartPose.ZERO);
        // the frame: head tube, top tube, down tube, seat tube, chain and seat stays, fork
        tube(root, "head_tube", PAINT, 0, HEAD_TOP, HEAD_Z, HEAD_BOT + 1, HEAD_Z - 1, 1.8f);
        tube(root, "top_tube", PAINT, 0, HEAD_TOP + 1, HEAD_Z, SEAT_Y + 1.5f, SEAT_Z - 1, 1.5f);
        tube(root, "down_tube", PAINT, 0, HEAD_BOT, HEAD_Z - 0.5f, BB_Y, BB_Z, 1.6f);
        tube(root, "seat_tube", PAINT, 0, BB_Y, BB_Z, SEAT_Y + 0.5f, SEAT_Z - 0.6f, 1.5f);
        for (float x : new float[]{-1.2f, 1.2f}) {
            String s = x < 0 ? "_l" : "_r";
            tube(root, "chain_stay" + s, PAINT, x, BB_Y, BB_Z, HUB_Y, REAR_Z, 1f);
            tube(root, "seat_stay" + s, PAINT, x, SEAT_Y + 1.5f, SEAT_Z - 0.4f, HUB_Y, REAR_Z, 1f);
            tube(root, "fork" + s, PAINT, x * 1.1f, HEAD_BOT, HEAD_Z - 1, HUB_Y, FRONT_Z, 1.1f);
        }
        // the battery along the down tube, the bottom bracket and pedal cranks
        float dy = BB_Y - HEAD_BOT, dz = BB_Z - HEAD_Z, th = (float) Math.atan2(-dy, dz), len = (float) Math.sqrt(dy * dy + dz * dz);
        root.addOrReplaceChild("battery", CubeListBuilder.create().texOffs(BATTERY[0], BATTERY[1]).addBox(-1.5f, -3.4f, len * 0.2f, 3, 2.6f, len * 0.6f),
            PartPose.offsetAndRotation(0, HEAD_BOT, HEAD_Z - 0.5f, th, 0, 0));
        root.addOrReplaceChild("bottom_bracket", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-2, -1, -1, 4, 2, 2), PartPose.offset(0, BB_Y, BB_Z));
        root.addOrReplaceChild("crank_l", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-0.5f, -0.5f, -0.5f, 1, 4, 1)
            .addBox(-2.5f, 3, -1, 2, 1, 2), PartPose.offsetAndRotation(-2.2f, BB_Y, BB_Z, 0.6f, 0, 0));
        root.addOrReplaceChild("crank_r", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-0.5f, -0.5f, -0.5f, 1, 4, 1)
            .addBox(0.5f, 3, -1, 2, 1, 2), PartPose.offsetAndRotation(2.2f, BB_Y, BB_Z, 0.6f + Mth.PI, 0, 0));
        // seat post and saddle
        tube(root, "seat_post", METAL, 0, SEAT_Y + 0.6f, SEAT_Z - 0.6f, SEAT_Y - 1.4f, SEAT_Z - 0.2f, 1f);
        root.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(SADDLE[0], SADDLE[1]).addBox(-1.5f, -1, -3.5f, 3, 1.5f, 5)
            .addBox(-2.5f, -1, -0.5f, 5, 1.5f, 2.5f), PartPose.offset(0, SEAT_Y - 1.8f, SEAT_Z));
        // stem and handlebars, grips
        tube(root, "stem", METAL, 0, HEAD_TOP + 0.5f, HEAD_Z, HEAD_TOP - 2.5f, HEAD_Z + 0.6f, 1f);
        root.addOrReplaceChild("bars", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-5, -0.5f, -0.5f, 10, 1, 1),
            PartPose.offset(0, HEAD_TOP - 2.5f, HEAD_Z + 1.2f));
        root.addOrReplaceChild("grips", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-7, -0.7f, -0.7f, 2.5f, 1.4f, 1.4f)
            .addBox(4.5f, -0.7f, -0.7f, 2.5f, 1.4f, 1.4f), PartPose.offset(0, HEAD_TOP - 2.5f, HEAD_Z + 1.2f));
        // the lamp on the front (EA makes lamps)
        root.addOrReplaceChild("lamp", CubeListBuilder.create().texOffs(MOTOR[0], MOTOR[1]).addBox(-1, -1, -1.5f, 2, 2, 1.5f),
            PartPose.offset(0, HEAD_TOP + 1.5f, HEAD_Z - 1.2f));
        // the rack over the back wheel and the trunk on it (Baughn: a small trunk on the back)
        root.addOrReplaceChild("rack", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-3, 0, 0, 6, 0.6f, 8),
            PartPose.offset(0, HUB_Y - R - 2.2f, REAR_Z - 4));
        tube(root, "rack_strut_l", METAL, -2.6f, HUB_Y - R - 1.8f, REAR_Z + 3.5f, HUB_Y, REAR_Z + 0.5f, 0.6f);
        tube(root, "rack_strut_r", METAL, 2.6f, HUB_Y - R - 1.8f, REAR_Z + 3.5f, HUB_Y, REAR_Z + 0.5f, 0.6f);
        root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(TRUNK[0], TRUNK[1]).addBox(-3.5f, -4.5f, 0, 7, 4.5f, 7),
            PartPose.offset(0, HUB_Y - R - 2.2f, REAR_Z - 3.5f));
        root.addOrReplaceChild("trunk_lid", CubeListBuilder.create().texOffs(LID[0], LID[1]).addBox(-3.8f, -1, -0.3f, 7.6f, 1, 7.6f),
            PartPose.offset(0, HUB_Y - R - 6.7f, REAR_Z - 3.5f));
        // the kickstand: pivots at the bottom bracket's back, down to the ground on the left; folds back along the stay
        root.addOrReplaceChild("kickstand", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-0.5f, 0, -0.5f, 1, 6.2f, 1)
            .addBox(-1, 5.7f, -1, 2, 0.6f, 2), PartPose.offset(-1.5f, BB_Y, BB_Z + 2.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BikeRenderState s) {
        super.setupAnim(s);
        frontWheel.xRot = s.wheel;
        rearWheel.xRot = s.wheel;
        // down: splayed out to the side; up: folded back along the chain stay
        kickstand.zRot = Mth.lerp(s.stand, 0.35f, 0.15f);
        kickstand.xRot = Mth.lerp(s.stand, 0.05f, 1.45f);
    }
}
