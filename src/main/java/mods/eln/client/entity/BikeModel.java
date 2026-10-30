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
import mods.eln.bike.BikeShape;
import org.erisia.cad.entity.EntityLayout;
import net.minecraft.util.Mth;

/**
 * The electric bike (Baughn 2026-10-28): a diamond frame in EA blue, a battery on the down tube and a hub motor in the
 * back wheel, a trunk on a rack over it, a kickstand that folds up when ridden. Model space in pixels: y down, the
 * ground at 24, the front toward -z. Wheels are octagon rims with four spokes across; they turn with the distance rolled.
 * Its solids are BikeShape's (the checked model library, board #44); the texture (textures/entity/electric_bike.png,
 * painted by BikeTexture) is regions of one material each.
 */
public class BikeModel extends EntityModel<BikeRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("eln", "electric_bike"), "main");
    /** The terrain bike (Baughn 2026-10-29): the same bike, fatter: knobbly tyres, a suspension fork, thicker tubes. */
    public static final ModelLayerLocation LAYER_TERRAIN = new ModelLayerLocation(Identifier.fromNamespaceAndPath("eln", "terrain_bike"), "main");

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
        java.util.Map.entry("fork_crown", Group.FORK), java.util.Map.entry("fork_l", Group.FORK), java.util.Map.entry("fork_r", Group.FORK),
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
        if ((terrain ? NAMES_TERRAIN : NAMES_BIKE).isEmpty()) createLayer(terrain);   // (the names, if this layer came from elsewhere)
        for (String name : terrain ? NAMES_TERRAIN : NAMES_BIKE) parts.add(java.util.Map.entry(root.getChild(name), GROUPS.getOrDefault(name, Group.BASE)));
    }

    /** The top-level part names of each layer (createLayer fills the one it builds). */
    static final java.util.List<String> NAMES_BIKE = new java.util.ArrayList<>(), NAMES_TERRAIN = new java.util.ArrayList<>();

    public static LayerDefinition createLayer() {
        return createLayer(false);
    }

    public static LayerDefinition createLayerTerrain() {
        return createLayer(true);
    }

    /** The layer from BikeShape's solids (the checked model library), as vanilla cubes. */
    static LayerDefinition createLayer(boolean terrain) {
        java.util.List<String> names = terrain ? NAMES_TERRAIN : NAMES_BIKE;
        names.clear();
        MeshDefinition mesh = new MeshDefinition();
        java.util.Map<String, PartDefinition> made = new java.util.HashMap<>();
        for (EntityLayout.EPart p : EntityLayout.layout(BikeShape.model(terrain), BikeShape.GROUND)) {
            CubeListBuilder cubes = CubeListBuilder.create();
            for (EntityLayout.EBox b : p.boxes()) cubes.texOffs(b.u(), b.v()).addBox(b.x(), b.y(), b.z(), b.w(), b.h(), b.d());
            PartPose pose = PartPose.offsetAndRotation(p.ox(), p.oy(), p.oz(), p.rx(), p.ry(), p.rz());
            PartDefinition parent = p.parent() == null ? mesh.getRoot() : made.get(p.parent());
            made.put(p.name(), parent.addOrReplaceChild(p.name(), cubes, pose));
            if (p.parent() == null) names.add(p.name());
        }
        return LayerDefinition.create(mesh, BikeShape.TEX_W, BikeShape.TEX_H);
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
