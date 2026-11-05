package mods.eln.robotics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.eln.Eln;
import mods.eln.client.render.ElnRenderErrors;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.gl.GL11;
import mods.eln.robotics.servo.ElevatorCar;
import mods.eln.robotics.servo.ElevatorPanel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The elevator car, its 3 x 3 x 3 cabin (model elevatorcar, through EA's GL11 facade), drawn where its segment puts it at this frame:
 * between the client's last two car ticks (local clock - 1 + partial tick), the same span the carried player's camera
 * glides over, so car and rider move smoothly together whatever the frame rate.
 */
public class ElevatorCarRenderer extends EntityRenderer<ElevatorCar, ElevatorCarRenderer.State> {
    public static class State extends EntityRenderState {
        /** Degrees about y turning the model's +x (its rail side) to the car's rail. */
        float yaw;
        /** The panel's slots (ElevatorCar.PANEL_*) and lamps. */
        final int[] kinds = new int[ElevatorPanel.SLOTS];
        final boolean[] lit = new boolean[ElevatorPanel.SLOTS];
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
        for (int i = 0; i < ElevatorPanel.SLOTS; i++) {
            s.kinds[i] = car.panelKind(i);
            s.lit[i] = car.panelLit(i);
        }
        s.yaw = switch (car.railSide) {
            case NORTH -> 90;
            case WEST -> 180;
            case SOUTH -> 270;
            default -> 0;
        };
    }

    /** The cabin reaches 3 m up, its guide shoes into the rail's column: cull by that, not by the 1 px plate. */
    @Override
    protected AABB getBoundingBoxForCulling(ElevatorCar car, float partial) {
        return car.getBoundingBox().inflate(0.2, 0, 0.2).expandTowards(0, ElevatorCar.CABIN, 0);
    }

    /**
     * The parts in the panel's slots, on the board inside the back wall (model frame: m from the plate's centre, its
     * floor at -0.5, +x the rail): a button is a grey square with its lamp (green, bright when lit), a light a small red square.
     */
    static void drawPanel(State s) {
        double face = ElevatorCar.HALF - ElevatorCar.WALL - ElevatorPanel.D;   // (the board's front, toward the cabin)
        UtilsClient.disableTexture();
        for (int i = 0; i < ElevatorPanel.SLOTS; i++) {
            if (s.kinds[i] == ElevatorCar.PANEL_NONE) continue;
            int row = i / ElevatorPanel.COLS, col = i % ElevatorPanel.COLS;
            // (a rider facing the rail, +x, has -z on the left: column 0 there)
            float z = (float) (-ElevatorPanel.W / 2 + (col + 0.5) * ElevatorPanel.CELL);
            float y = (float) (ElevatorPanel.UP + ElevatorPanel.H - (row + 0.5) * ElevatorPanel.CELL) - 0.5f;   // (the frame's floor is at -0.5)
            float x = (float) face;
            if (s.kinds[i] == ElevatorCar.PANEL_BUTTON) {
                GL11.glColor3f(0.45f, 0.45f, 0.47f);
                box(x - 0.03f, x, y - 0.08f, y + 0.08f, z - 0.08f, z + 0.08f);
                if (s.lit[i]) GL11.glColor3f(0.4f, 1f, 0.4f);
                else GL11.glColor3f(0.1f, 0.3f, 0.1f);
                box(x - 0.05f, x - 0.03f, y - 0.04f, y + 0.04f, z - 0.04f, z + 0.04f);
            } else {
                if (s.lit[i]) GL11.glColor3f(1f, 0.35f, 0.15f);
                else GL11.glColor3f(0.3f, 0.06f, 0.05f);
                box(x - 0.03f, x, y - 0.05f, y + 0.05f, z - 0.05f, z + 0.05f);
            }
        }
        GL11.glColor3f(1f, 1f, 1f);
        UtilsClient.enableTexture();
    }

    /** An axis-aligned box's six faces, outward. */
    static void box(float x0, float x1, float y0, float y1, float z0, float z1) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(-1, 0, 0);
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        GL11.glNormal3f(1, 0, 0);
        quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        GL11.glNormal3f(0, -1, 0);
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        GL11.glNormal3f(0, 1, 0);
        quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        GL11.glNormal3f(0, 0, -1);
        quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        GL11.glNormal3f(0, 0, 1);
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        GL11.glEnd();
    }

    static void quad(float... v) {
        for (int i = 0; i < 12; i += 3) GL11.glVertex3f(v[i], v[i + 1], v[i + 2]);
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
            drawPanel(s);
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
