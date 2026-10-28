package mods.eln.client.entity;

import mods.eln.bike.ElectricBike;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;

/**
 * A player riding the electric bike sits on its saddle with feet on the pedals and hands on the grips (Baughn
 * 2026-10-28). The crank angle (ElectricBike.crank: it turns with the wheel, freewheeling while the bike slows) goes
 * into the player's render state through a NeoForge render-state modifier, so other players' riders pedal too;
 * HumanoidModelMixin calls apply after vanilla's setupAnim. The player model has no knees: each leg points from the hip
 * at its pedal, and when the pedal is nearer than a leg's length the hip rises by the difference so the foot still
 * meets it (within 2 px; at the bottom of the stroke the hip drops up to 1.5 px). Geometry from BikeModel and ElectricBike.SEAT_Y, in model pixels (y down, front toward -z), relative to
 * the hips.
 */
public final class RiderPose {
    private RiderPose() {
    }

    public static final ContextKey<Float> CRANK = new ContextKey<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "bike_crank"));

    /** The bottom bracket below and ahead of the hips; crank length to the pedal; the grips from the shoulders. */
    static final float BB_DOWN = 10.8f, BB_AHEAD = 2.5f, CRANK_LEN = 3.5f, LEG = 12;
    static final float ARM_PITCH = -0.80f;

    /** The render-state modifier (players, AvatarRenderer): one on an ElectricBike carries its crank angle. */
    public static void extract(net.minecraft.world.entity.Entity avatar, HumanoidRenderState state) {
        if (avatar.getVehicle() instanceof ElectricBike b) {
            float a = net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            state.setRenderData(CRANK, Mth.lerp(a, b.crankO, b.crank));
        } else {
            state.setRenderData(CRANK, null);
        }
    }

    public static void apply(HumanoidModel<?> m, HumanoidRenderState state) {
        Float crank = state.getRenderData(CRANK);
        if (crank == null) return;
        // the bike's left crank (model -x) is under the rider's right leg (also -x); the other is half a turn on
        leg(m.rightLeg, crank, -1.9f);
        leg(m.leftLeg, crank + Mth.PI, 1.9f);
        for (var arm : new net.minecraft.client.model.geom.ModelPart[]{m.rightArm, m.leftArm}) {
            arm.xRot = ARM_PITCH;
            arm.yRot = 0;
            arm.zRot = arm == m.rightArm ? 0.08f : -0.08f;   // (out a little to the grips)
        }
    }

    static void leg(net.minecraft.client.model.geom.ModelPart leg, float angle, float x) {
        // pedal relative to the hip: the crank turned about x (as BikeModel turns it)
        float dy = BB_DOWN + CRANK_LEN * Mth.cos(angle), dz = -BB_AHEAD + CRANK_LEN * Mth.sin(angle);
        float d = Mth.sqrt(dy * dy + dz * dz);
        // no knee: the hip comes up (or down a little at the bottom) instead; the foot then misses its pedal by under 2 px
        float lift = Math.max(-1.5f, Math.min(4, LEG - d));
        leg.xRot = (float) Math.atan2(dz, dy);
        leg.yRot = 0;
        leg.zRot = 0;
        leg.x = x;
        leg.y = 12 - lift;
    }
}
