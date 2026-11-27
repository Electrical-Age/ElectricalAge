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
        /** A round cabin (track B): its door open, "out" (away from the rail) as an angle; the posts' angles. */
        boolean round, doorOpen;
        float outX, outZ;
        /** Its drive's laid track as the rail beside the car (track B): points (m, from the car's drawn place), or empty. */
        float[] rail = new float[0];
    }

    /** The rail beside a laid track: how far from the car's centre (m), how high above its floor (the drive's top), how thick. */
    static final float RAIL_OFF = 2f, RAIL_UP = 1.5f, RAIL_W = 0.25f;

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
        s.round = car.round();
        s.rail = new float[0];
        if (car.onTrack() && !Double.isNaN(p)) {   // (any laid track: its rail is drawn with the car; there are no rail blocks)
            Vec3 out = car.towardRail(p).scale(-1);
            s.outX = (float) out.x;
            s.outZ = (float) out.z;
            s.doorOpen = car.doorOpen();
            var path = car.path();
            int n = (int) Math.ceil(path.length() / 0.5) + 1;
            s.rail = new float[3 * n];
            for (int i = 0; i < n; i++) {
                double q = Math.min(path.length(), i * 0.5);
                Vec3 r = car.at(q).add(car.towardRail(q).scale(RAIL_OFF)).add(0, RAIL_UP, 0);
                s.rail[3 * i] = (float) (r.x - s.x);
                s.rail[3 * i + 1] = (float) (r.y - s.y);
                s.rail[3 * i + 2] = (float) (r.z - s.z);
            }
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
        AABB b = car.getBoundingBox().inflate(0.2, 0, 0.2).expandTowards(0, ElevatorCar.CABIN, 0);
        var path = car.path();
        if (car.onTrack() && path != null)   // (its track's rail too: drawn with the car)
            for (double q = 0; q <= path.length(); q += 2) b = b.minmax(new AABB(car.at(q), car.at(q)).inflate(RAIL_OFF + 0.5, RAIL_UP + 0.5, RAIL_OFF + 0.5));
        return b;
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

    /**
     * A round cabin (track B), world frame from the plate's bottom centre: a floor disc, ElevatorCar.POSTS thin posts on
     * the rim (none in the open door, away from the rail), a ring at their tops and a roof disc.
     */
    static void drawRound(State s) {
        UtilsClient.disableTexture();
        float r = (float) ElevatorCar.HALF, top = (float) (ElevatorCar.CABIN - ElevatorCar.WALL), h = (float) ElevatorCar.POST_HALF;
        GL11.glColor3f(0.42f, 0.43f, 0.45f);
        disc(r, (float) ElevatorCar.THICK, true);
        GL11.glColor3f(0.30f, 0.31f, 0.33f);
        for (int k = 0; k < ElevatorCar.POSTS; k++) {
            double th = 2 * Math.PI * k / ElevatorCar.POSTS;
            float x = (float) (ElevatorCar.POST_R * Math.cos(th)), z = (float) (ElevatorCar.POST_R * Math.sin(th));
            if (s.doorOpen && Math.cos(th) * s.outX + Math.sin(th) * s.outZ > Math.cos(ElevatorCar.DOOR_HALF_ANGLE)) continue;
            box(x - h, x + h, (float) ElevatorCar.THICK, top, z - h, z + h);
        }
        GL11.glColor3f(0.36f, 0.37f, 0.40f);
        disc(r, top, false);
        disc(r, (float) ElevatorCar.CABIN, true);
        GL11.glColor3f(1f, 1f, 1f);
        UtilsClient.enableTexture();
    }

    /** A flat disc of radius r at height y, facing up (or down). */
    static void disc(float r, float y, boolean up) {
        int n = 32;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(0, up ? 1 : -1, 0);
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
            float x0 = (float) (r * Math.cos(a0)), z0 = (float) (r * Math.sin(a0)), x1 = (float) (r * Math.cos(a1)), z1 = (float) (r * Math.sin(a1));
            if (up) quad(0, y, 0, x1, y, z1, x0, y, z0, 0, y, 0);
            else quad(0, y, 0, x0, y, z0, x1, y, z1, 0, y, 0);
        }
        GL11.glEnd();
    }

    /** The laid track's rail: a square beam along s.rail's points. */
    static void drawRail(State s) {
        if (s.rail.length < 6) return;
        UtilsClient.disableTexture();
        GL11.glColor3f(0.55f, 0.56f, 0.58f);
        float w = RAIL_W / 2;
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i + 5 < s.rail.length; i += 3) {
            float ax = s.rail[i], ay = s.rail[i + 1], az = s.rail[i + 2], bx = s.rail[i + 3], by = s.rail[i + 4], bz = s.rail[i + 5];
            float dx = bx - ax, dz = bz - az, len = (float) Math.sqrt(dx * dx + dz * dz);
            float sx = len < 1e-6f ? w : -dz / len * w, sz = len < 1e-6f ? 0 : dx / len * w;   // (sideways, horizontal)
            GL11.glNormal3f(0, 1, 0);
            quad(ax - sx, ay + w, az - sz, ax + sx, ay + w, az + sz, bx + sx, by + w, bz + sz, bx - sx, by + w, bz - sz);
            GL11.glNormal3f(0, -1, 0);
            quad(ax - sx, ay - w, az - sz, bx - sx, by - w, bz - sz, bx + sx, by - w, bz + sz, ax + sx, ay - w, az + sz);
            GL11.glNormal3f(sx / w, 0, sz / w);
            quad(ax + sx, ay - w, az + sz, bx + sx, by - w, bz + sz, bx + sx, by + w, bz + sz, ax + sx, ay + w, az + sz);
            GL11.glNormal3f(-sx / w, 0, -sz / w);
            quad(ax - sx, ay - w, az - sz, ax - sx, ay + w, az - sz, bx - sx, by + w, bz - sz, bx - sx, by - w, bz - sz);
        }
        GL11.glEnd();
        GL11.glColor3f(1f, 1f, 1f);
        UtilsClient.enableTexture();
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.round) {   // (track B: the round cabin and its rail, in the world's frame)
            GlFrame frame = GlSubmit.open(pose);
            try {
                UtilsClient.glDefaultColor();
                drawRound(s);
                drawRail(s);
            } catch (RuntimeException e) {
                ElnRenderErrors.report("elevator car", e);
            } finally {
                GlFrame.close();
            }
            GlSubmit.submit(frame, pose, out, s.lightCoords);
            pose.pushPose();   // (its panel, where the walled cabin's back wall was: the panel part is there)
            pose.rotateDegrees(Axis.YP, s.yaw);
            pose.translate(0, 0.5, 0);
            GlFrame pf = GlSubmit.open(pose);
            try {
                drawPanel(s);
            } catch (RuntimeException e) {
                ElnRenderErrors.report("elevator car", e);
            } finally {
                GlFrame.close();
            }
            GlSubmit.submit(pf, pose, out, s.lightCoords);
            pose.popPose();
            super.submit(s, pose, out, camera);
            return;
        }
        if (s.rail.length >= 6) {   // (a straight laid track: the walled cabin, and the rail beside it)
            GlFrame rf = GlSubmit.open(pose);
            try {
                drawRail(s);
            } catch (RuntimeException e) {
                ElnRenderErrors.report("elevator car", e);
            } finally {
                GlFrame.close();
            }
            GlSubmit.submit(rf, pose, out, s.lightCoords);
        }
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
