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
import mods.eln.bike.BikeGeometry;
import net.minecraft.util.Mth;

/**
 * The electric bike (Baughn 2026-10-28): a diamond frame in EA blue, a battery on the down tube and a hub motor in the
 * back wheel, a trunk on a rack over it, a kickstand that folds up when ridden. Model space in pixels: y down, the
 * ground at 24, the front toward -z. Wheels are octagon rims with four spokes across; they turn with the distance rolled.
 * The texture (textures/entity/electric_bike.png, drawn by tools/bike/texture.py) is regions of one material each.
 */
public class BikeModel extends EntityModel<BikeRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("eln", "electric_bike"), "main");
    /** The terrain bike (Baughn 2026-10-29): the same bike, fatter: knobbly tyres, a suspension fork, thicker tubes. */
    public static final ModelLayerLocation LAYER_TERRAIN = new ModelLayerLocation(Identifier.fromNamespaceAndPath("eln", "terrain_bike"), "main");

    /** Wheel radius in pixels (ElectricBike.WHEEL_RADIUS in blocks), hub heights, and where things sit along z. */
    static final float R = BikeGeometry.R, HUB_Y = BikeGeometry.HUB_Y, FRONT_Z = BikeGeometry.FRONT_Z, REAR_Z = BikeGeometry.REAR_Z;
    static final float HEAD_Z = BikeGeometry.HEAD_Z, HEAD_TOP = BikeGeometry.HEAD_TOP, HEAD_BOT = BikeGeometry.HEAD_BOT, BB_Y = BikeGeometry.BB_Y,
        BB_Z = BikeGeometry.BB_Z, SEAT_Z = BikeGeometry.SEAT_Z, SEAT_Y = BikeGeometry.SEAT_Y;

    // texture regions (u, v), one material each, each big enough for its largest box's UV layout (2(d+w) x (d+h)):
    // the trunk's 28 px overran a 24 px region into the battery's (Baughn: an oddity at the trunk's back). PAINT, TRUNK,
    // LID and SADDLE are neutral, tinted per dye group (BikeRenderer).
    static final int[] PAINT = {0, 0}, TIRE = {28, 0}, SADDLE = {44, 0}, TRUNK = {0, 14}, LID = {28, 14}, METAL = {0, 26},
        BATTERY = {28, 23}, MOTOR = {46, 23}, BLACK = {28, 32};

    /** The dyeable groups (Baughn: dyeable per frame element), and what each holds; everything else is BASE. */
    public enum Group {
        BASE, FRAME, FORK, TRUNK, SADDLE,
        /** The front lamp: drawn full bright when on (BikeRenderer). */
        LAMP
    }

    static final java.util.Map<String, Group> GROUPS = java.util.Map.ofEntries(
        java.util.Map.entry("head_tube", Group.FRAME), java.util.Map.entry("top_tube", Group.FRAME), java.util.Map.entry("down_tube", Group.FRAME),
        java.util.Map.entry("seat_tube", Group.FRAME), java.util.Map.entry("chain_stay_l", Group.FRAME), java.util.Map.entry("chain_stay_r", Group.FRAME),
        java.util.Map.entry("seat_stay_l", Group.FRAME), java.util.Map.entry("seat_stay_r", Group.FRAME),
        java.util.Map.entry("fork_l", Group.FORK), java.util.Map.entry("fork_r", Group.FORK),
        java.util.Map.entry("trunk", Group.TRUNK), java.util.Map.entry("trunk_lid", Group.TRUNK), java.util.Map.entry("saddle", Group.SADDLE),
        java.util.Map.entry("lamp", Group.LAMP));

    private final ModelPart frontWheel, rearWheel, kickstand, crankL, crankR;
    /** Every top-level part with its group, for drawing one group at a time. */
    private final java.util.List<java.util.Map.Entry<ModelPart, Group>> parts = new java.util.ArrayList<>();

    public BikeModel(ModelPart root) {
        this(root, false);
    }

    public BikeModel(ModelPart root, boolean terrain) {
        super(root);
        frontWheel = root.getChild("front_wheel");
        rearWheel = root.getChild("rear_wheel");
        kickstand = root.getChild("kickstand");
        crankL = root.getChild("crank_l");
        crankR = root.getChild("crank_r");
        for (String name : terrain ? NAMES_TERRAIN : NAMES_BIKE) parts.add(java.util.Map.entry(root.getChild(name), GROUPS.getOrDefault(name, Group.BASE)));
    }

    /** The top-level part names of each layer (createLayer fills the one it builds). */
    static final java.util.List<String> NAMES_BIKE = new java.util.ArrayList<>(), NAMES_TERRAIN = new java.util.ArrayList<>();
    static java.util.List<String> PART_NAMES = NAMES_BIKE;
    /** While building the terrain layer: tubes thicker by this. */
    static float thick = 1;

    /** A tube of thickness t from (y0, z0) to (y1, z1) at x, as a box along z turned about x. */
    static void tube(PartDefinition root, String name, int[] uv, float x, float y0, float z0, float y1, float z1, float t) {
        PART_NAMES.add(name);
        if (uv == PAINT) t *= thick;
        float dy = y1 - y0, dz = z1 - z0, len = (float) Math.sqrt(dy * dy + dz * dz);
        float th = (float) Math.atan2(-dy, dz);
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(uv[0], uv[1]).addBox(-t / 2, -t / 2, 0, t, t, len),
            PartPose.offsetAndRotation(x, y0, z0, th, 0, 0));
    }

    static void wheel(PartDefinition root, String name, float z) {
        PART_NAMES.add(name);
        PartDefinition w = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0, HUB_Y, z));
        float seg = 2 * R * (float) Math.tan(Math.PI / 8) + 0.2f;
        boolean fat = thick > 1;   // (the terrain bike's tyres: wider, deeper, with knobs on the tread)
        float tw = fat ? 3.2f : 2, td = fat ? 2.2f : 1.5f;
        for (int i = 0; i < 8; i++)
            w.addOrReplaceChild("rim" + i, CubeListBuilder.create().texOffs(TIRE[0], TIRE[1]).addBox(-tw / 2, -R, -seg / 2, tw, td, seg),
                PartPose.rotation(i * Mth.PI / 4, 0, 0));
        if (fat)
            for (int i = 0; i < 16; i++)
                w.addOrReplaceChild("knob" + i, CubeListBuilder.create().texOffs(TIRE[0], TIRE[1]).addBox(i % 2 == 0 ? -1.5f : 0.2f, -R - 0.4f, -0.6f, 1.3f, 0.6f, 1.2f),
                    PartPose.rotation(i * Mth.PI / 8, 0, 0));
        for (int i = 0; i < 4; i++)
            w.addOrReplaceChild("spoke" + i, CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-0.25f, -R + 1, -0.25f, 0.5f, 2 * R - 2, 0.5f),
                PartPose.rotation(i * Mth.PI / 4, 0, 0));
        w.addOrReplaceChild("hub", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-1.5f, -1, -1, 3, 2, 2), PartPose.ZERO);
    }

    /** A cuboid given as {x0, y0, z0, x1, y1, z1} (BikeGeometry). */
    static CubeListBuilder box(CubeListBuilder c, float[] b) {
        return c.addBox(b[0], b[1], b[2], b[3] - b[0], b[4] - b[1], b[5] - b[2]);
    }

    /** A top-level part, remembered by name for the dye groups. */
    static void part(PartDefinition root, String name, CubeListBuilder cubes, PartPose pose) {
        PART_NAMES.add(name);
        root.addOrReplaceChild(name, cubes, pose);
    }

    public static LayerDefinition createLayer() {
        return createLayer(false);
    }

    public static LayerDefinition createLayerTerrain() {
        return createLayer(true);
    }

    static LayerDefinition createLayer(boolean terrain) {
        PART_NAMES = terrain ? NAMES_TERRAIN : NAMES_BIKE;
        PART_NAMES.clear();
        thick = terrain ? 1.35f : 1;
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
            if (!terrain) {
                tube(root, "fork" + s, PAINT, x * 1.1f, HEAD_BOT, HEAD_Z - 1, HUB_Y, FRONT_Z, 1.1f);
            } else {
                // a suspension fork: bare stanchions into fat lower legs (the dyeable part)
                float my = HEAD_BOT + 0.45f * (HUB_Y - HEAD_BOT), mz = HEAD_Z - 1 + 0.45f * (FRONT_Z - HEAD_Z + 1);
                tube(root, "fork_upper" + s, METAL, x * 1.2f, HEAD_BOT, HEAD_Z - 1, my + 1, mz - 0.27f, 1.1f);
                tube(root, "fork" + s, PAINT, x * 1.3f, my, mz, HUB_Y, FRONT_Z, 1.4f);
            }
        }
        // the battery along the down tube, the bottom bracket and pedal cranks
        float dy = BB_Y - HEAD_BOT, dz = BB_Z - HEAD_Z, th = (float) Math.atan2(-dy, dz), len = (float) Math.sqrt(dy * dy + dz * dz);
        // (clear of the tube and of the seat tube at its lower end: Baughn saw it clip the frame)
        part(root, "battery", CubeListBuilder.create().texOffs(BATTERY[0], BATTERY[1]).addBox(-1.5f, -3.7f, len * 0.15f, 3, 2.6f, len * 0.5f),
            PartPose.offsetAndRotation(0, HEAD_BOT, HEAD_Z - 0.5f, th, 0, 0));
        part(root, "bottom_bracket", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-2, -1, -1, 4, 2, 2), PartPose.offset(0, BB_Y, BB_Z));
        part(root, "crank_l", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-0.5f, -0.5f, -0.5f, 1, 4, 1)
            .addBox(-2.5f, 3, -1, 2, 1, 2), PartPose.offsetAndRotation(-2.2f, BB_Y, BB_Z, 0.6f, 0, 0));
        part(root, "crank_r", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-0.5f, -0.5f, -0.5f, 1, 4, 1)
            .addBox(0.5f, 3, -1, 2, 1, 2), PartPose.offsetAndRotation(2.2f, BB_Y, BB_Z, 0.6f + Mth.PI, 0, 0));
        // seat post and saddle
        tube(root, "seat_post", METAL, 0, SEAT_Y + 0.6f, SEAT_Z - 0.6f, SEAT_Y - 1.4f, SEAT_Z - 0.2f, 1f);
        // (BikeGeometry's boxes, which the dyes aim at too)
        part(root, "saddle", box(box(CubeListBuilder.create().texOffs(SADDLE[0], SADDLE[1]), BikeGeometry.SADDLE_BOXES[0]), BikeGeometry.SADDLE_BOXES[1]),
            PartPose.offset(0, BikeGeometry.SADDLE_OY, BikeGeometry.SADDLE_OZ));
        // stem and handlebars, grips
        tube(root, "stem", METAL, 0, HEAD_TOP + 0.5f, HEAD_Z, HEAD_TOP - 2.5f, HEAD_Z + 0.6f, 1f);
        part(root, "bars", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-5, -0.5f, -0.5f, 10, 1, 1),
            PartPose.offset(0, HEAD_TOP - 2.5f, HEAD_Z + 1.2f));
        part(root, "grips", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-7, -0.7f, -0.7f, 2.5f, 1.4f, 1.4f)
            .addBox(4.5f, -0.7f, -0.7f, 2.5f, 1.4f, 1.4f), PartPose.offset(0, HEAD_TOP - 2.5f, HEAD_Z + 1.2f));
        // the lamp on the front (EA makes lamps)
        part(root, "lamp", CubeListBuilder.create().texOffs(MOTOR[0], MOTOR[1]).addBox(-1, -1, -1.5f, 2, 2, 1.5f),
            PartPose.offset(0, HEAD_TOP + 1.5f, HEAD_Z - 1.2f));
        // the rack over the back wheel and the trunk on it (Baughn: a small trunk on the back)
        part(root, "rack", CubeListBuilder.create().texOffs(METAL[0], METAL[1]).addBox(-3, 0, 0, 6, 0.6f, 7),
            PartPose.offset(0, HUB_Y - R - 2.2f, REAR_Z - 3.5f));
        // its front braced down to the seat stays (Baughn: the front lacked a support)
        float rackY = HUB_Y - R - 1.6f, stayZ = SEAT_Z + 1.5f, stayY = SEAT_Y + 1.5f + (stayZ - (SEAT_Z - 0.4f)) / (REAR_Z - (SEAT_Z - 0.4f)) * (HUB_Y - SEAT_Y - 1.5f);
        tube(root, "rack_brace_l", METAL, -1.9f, rackY, REAR_Z - 3.2f, stayY, stayZ, 0.6f);
        tube(root, "rack_brace_r", METAL, 1.9f, rackY, REAR_Z - 3.2f, stayY, stayZ, 0.6f);
        tube(root, "rack_strut_l", METAL, -2.6f, HUB_Y - R - 1.8f, REAR_Z + 3.5f, HUB_Y, REAR_Z + 0.5f, 0.6f);
        tube(root, "rack_strut_r", METAL, 2.6f, HUB_Y - R - 1.8f, REAR_Z + 3.5f, HUB_Y, REAR_Z + 0.5f, 0.6f);
        part(root, "trunk", box(CubeListBuilder.create().texOffs(TRUNK[0], TRUNK[1]), BikeGeometry.TRUNK_BOX),
            PartPose.offset(0, BikeGeometry.RACK_Y, BikeGeometry.TRUNK_OZ));
        part(root, "trunk_lid", box(CubeListBuilder.create().texOffs(LID[0], LID[1]), BikeGeometry.LID_BOX),
            PartPose.offset(0, BikeGeometry.RACK_Y, BikeGeometry.TRUNK_OZ));
        // the kickstand: pivots at the bottom bracket's back, down to the ground on the left; folds back along the stay
        part(root, "kickstand", CubeListBuilder.create().texOffs(BLACK[0], BLACK[1]).addBox(-0.5f, 0, -0.5f, 1, 6.2f, 1)
            .addBox(-1, 5.7f, -1, 2, 0.6f, 2), PartPose.offset(-1.5f, BB_Y, BB_Z + 2.5f));
        thick = 1;
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BikeRenderState s) {
        super.setupAnim(s);
        // one dye group per draw (BikeRenderer draws each with its colour)
        for (var e : parts) e.getKey().visible = e.getValue() == s.group;
        frontWheel.xRot = s.wheel;
        rearWheel.xRot = s.wheel;
        // the cranks, half a turn apart (RiderPose puts the rider's feet where these put the pedals)
        crankL.xRot = s.crank;
        crankR.xRot = s.crank + Mth.PI;
        // down: splayed out to the side; up: folded back along the chain stay
        kickstand.zRot = Mth.lerp(s.stand, 0.35f, 0.15f);
        kickstand.xRot = Mth.lerp(s.stand, 0.05f, 1.45f);
    }
}
