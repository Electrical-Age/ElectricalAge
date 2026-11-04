package mods.eln.robotics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.eln.Eln;
import mods.eln.client.render.ElnRenderErrors;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.robotics.servo.ElevatorCar;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The elevator car (model elevatorcar, through EA's GL11 facade), drawn where its segment puts it at this frame:
 * between the client's last two car ticks (local clock - 1 + partial tick), the same span the carried player's camera
 * glides over, so car and rider move smoothly together whatever the frame rate.
 */
public class ElevatorCarRenderer extends EntityRenderer<ElevatorCar, ElevatorCarRenderer.State> {
    public static class State extends EntityRenderState {
        /** Degrees about y turning the model's +x (its rail side) to the car's rail. */
        float yaw;
    }

    Obj3D obj;

    public ElevatorCarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElevatorCar car, State s, float partial) {
        super.extractRenderState(car, s, partial);
        double p = car.clientPos(car.clientT - 1 + partial);
        if (!Double.isNaN(p)) {
            Vec3 at = car.at(p);
            s.x = at.x;
            s.y = at.y;
            s.z = at.z;
        }
        s.yaw = switch (car.railSide) {
            case NORTH -> 90;
            case WEST -> 180;
            case SOUTH -> 270;
            default -> 0;
        };
    }

    /** The frame reaches 1.7 m up and into the rail's column: cull by that, not by the 1 px plate. */
    @Override
    protected AABB getBoundingBoxForCulling(ElevatorCar car, float partial) {
        return car.getBoundingBox().inflate(1.2, 0, 1.2).expandTowards(0, 1.8, 0);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (obj == null) obj = Eln.obj.getObj("elevatorcar");
        if (obj == null) return;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, s.yaw);
        pose.translate(0, 0.5, 0);   // (the model is centred; its floor at -0.5)
        GlFrame frame = GlSubmit.open(pose);
        try {
            UtilsClient.glDefaultColor();
            var main = obj.getPart("main");
            if (main != null) main.draw();
        } catch (RuntimeException e) {
            ElnRenderErrors.report("elevator car", e);
        } finally {
            GlFrame.close();
        }
        GlSubmit.submit(frame, pose, out, s.lightCoords);
        pose.popPose();
        super.submit(s, pose, out, camera);
    }
}
