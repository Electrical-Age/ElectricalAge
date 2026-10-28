package mods.eln.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.eln.bike.ElectricBike;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The electric bike: BikeModel turned to its heading. Parked, it leans a little onto its kickstand (to the left); fallen,
 * it lies on its side; hit, it rocks like a boat.
 */
public class BikeRenderer extends EntityRenderer<ElectricBike, BikeRenderState> {
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("eln", "textures/entity/electric_bike.png");
    final BikeModel model;

    public BikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        model = new BikeModel(ctx.bakeLayer(BikeModel.LAYER));
        shadowRadius = 0.45f;
    }

    @Override
    public BikeRenderState createRenderState() {
        return new BikeRenderState();
    }

    @Override
    public void extractRenderState(ElectricBike bike, BikeRenderState s, float a) {
        super.extractRenderState(bike, s, a);
        s.yRot = bike.getYRot(a);
        s.wheel = Mth.lerp(a, bike.wheelO, bike.wheel);
        s.stand = Mth.lerp(a, bike.standO, bike.stand);
        s.crank = Mth.lerp(a, bike.crankO, bike.crank);
        s.mode = bike.mode();
        s.hurtTime = bike.getHurtTime() - a;
        s.hurtDir = bike.getHurtDir();
        s.damageTime = Math.max(bike.getDamage() - a, 0);
    }

    @Override
    public void submit(BikeRenderState s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, 180 - s.yRot);
        // lean about the line where the tyres touch the ground: onto the stand when parked, flat when fallen
        // (the stand is on the model's -x, which the flip below puts on +x here; leaning that way is negative about z)
        float lean = s.mode == ElectricBike.Mode.FALLEN ? -90 : -8 * (1 - s.stand);
        if (s.mode == ElectricBike.Mode.FALLEN) pose.translate(0, 0.42f, 0);   // (on its side it rests on a grip)
        pose.rotateDegrees(Axis.ZP, lean);
        if (s.hurtTime > 0) pose.rotateDegrees(Axis.XP, Mth.sin(s.hurtTime) * s.hurtTime * s.damageTime / 10f * s.hurtDir);
        pose.scale(-1, -1, 1);
        pose.translate(0, -1.5f, 0);
        out.submitModel(model, s, pose, TEXTURE, s.lightCoords, OverlayTexture.NO_OVERLAY, s.outlineColor);
        pose.popPose();
        super.submit(s, pose, out, camera);
    }
}
