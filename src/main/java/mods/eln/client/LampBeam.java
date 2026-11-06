package mods.eln.client;

import mods.eln.item.electricalitem.LampItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * EA's lamp beams (flashlights, the worn headlamp, the bike's front lamp: LampItem.beamLight) drawn per pixel by a post
 * effect (assets/eln/post_effect/lamp_beam.json), so they follow the view at once (Baughn: light blocks along the beam
 * lag). The server only lights the holder's own block (LampItem.light). Moved here from Stasis Delvers, which first
 * drew them (Baughn 2026-11-06: EA draws its own beams, with no fallback of light blocks). First in the list of post
 * effects, so another mod's effect after it (Stasis Delvers' grey) sees the lit world.
 * <ul>
 * <li>Your own beam in first person starts at the camera: everything you see is in its line of sight, so it needs no
 * shadows.</li>
 * <li>Others' beams (and yours in third person) start at the eyes, and end where the look ray first hits a block, so
 * they don't shine through walls in front of them.</li>
 * <li>With an Iris shaderpack in use the beam is off (shaderpacks light the held item themselves).</li>
 * </ul>
 */
public final class LampBeam {
    private LampBeam() {
    }

    public static final Identifier EFFECT = Identifier.fromNamespaceAndPath(mods.eln.Eln.MODID, "lamp_beam");
    static final Identifier DATA = Identifier.fromNamespaceAndPath(mods.eln.Eln.MODID, "textures/effect/beamdata.png");
    static final int MAX = 5;
    /** Tuning (later: client config). Beam length = light level x LENGTH_PER_LIGHT blocks. */
    // (Baughn 2026-10-18: half as bright, covering most of the screen; near walls no longer cut a dark hole: the near
    // cut, which keeps the hand unlit, now fades in from 0.14 to 0.26 m instead of a hard 0.9: a player stands 0.3 from
    // a wall at the closest)
    static final float STRENGTH = 4.5F, LENGTH_PER_LIGHT = 2.0F, HALF_ANGLE_DEG = 50.0F, NEAR = 0.14F;
    static final float[] TINT = {1.0F, 0.93F, 0.78F};
    static final double SEE_RANGE = 64.0;

    static DynamicTexture texture;
    /** Beams found at the last tick (holders and their light). */
    static final List<Player> holders = new ArrayList<>();
    static final List<Integer> lights = new ArrayList<>();

    /** How many beams are drawn (for bug reports, e.g. Stasis Delvers'). */
    public static int beams() {
        return holders.size();
    }

    /** The light of the beam a player casts: a lit EA flashlight in either hand or a worn headlamp (EA's one rule), else 0. */
    static int beamLight(Player p) {
        return LampItem.beamLight(p);
    }

    /** Whether an Iris shaderpack is in use (then no beams are drawn: shaderpacks light the held item themselves). */
    public static boolean shaderPackInUse() {
        if (!net.neoforged.fml.ModList.get().isLoaded("iris")) return false;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isShaderPackInUse").invoke(inst);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        holders.clear();
        lights.clear();
        if (mc.player == null || mc.level == null) return;
        if (!shaderPackInUse())
            for (Player p : mc.level.players()) {
                if (holders.size() == MAX) break;
                if (p.isSpectator() || p.distanceToSqr(mc.player) > SEE_RANGE * SEE_RANGE) continue;
                int l = beamLight(p);
                if (l > 0) {
                    holders.add(p);
                    lights.add(l);
                }
            }
        List<Identifier> effects = mc.player.getActivePostEffects();
        boolean want = texture != null && !holders.isEmpty();
        if (want != effects.contains(EFFECT)) {
            List<Identifier> l = new ArrayList<>(effects);
            if (want) l.add(0, EFFECT);   // before the stasis effect: the Grey stays grey in the beam
            else l.remove(EFFECT);
            mc.player.setActivePostEffects(l);
        }
    }

    public static void frame(FrameGraphSetupEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (texture == null) {
            texture = new DynamicTexture(() -> "eln beam data", 128, 1, true);
            mc.getTextureManager().register(DATA, texture);
        }
        var cam = e.getCameraState();
        float pt = cam.cameraEntityPartialTicks;
        float[] d = new float[128];
        viewProjection(cam).invert().get(d, 0);   // (the view bob's turn included; its shift moves the eye)
        Vec3 eye = eye(cam);
        d[17] = STRENGTH;
        d[18] = TINT[0];
        d[19] = TINT[1];
        d[20] = TINT[2];
        d[21] = NEAR;
        float cosHalf = (float) Math.cos(Math.toRadians(HALF_ANGLE_DEG));
        int n = 0;
        for (int i = 0; i < holders.size(); i++) {
            Player p = holders.get(i);
            float len = lights.get(i) * LENGTH_PER_LIGHT, cut = len;
            Vec3 org, dir;
            if (p == mc.getCameraEntity() && cam.isFirstPerson) {
                org = eye;   // (the bobbed eye: the beam stays fixed to the view as the camera bobs)
                dir = Vec3.directionFromRotation(cam.xRot, cam.yRot);
            } else {
                org = p.getEyePosition(pt);
                dir = p.getViewVector(pt);
                var hit = mc.level.clip(new ClipContext(org, org.add(dir.scale(len)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
                // ends just past the first block hit (no light through that wall); the falloff stays the full beam's
                if (hit.getType() != HitResult.Type.MISS) cut = (float) Math.min(len, hit.getLocation().distanceTo(org) + 1.0);
            }
            int o = 24 + 9 * n++;
            d[o] = (float) (org.x - eye.x);
            d[o + 1] = (float) (org.y - eye.y);
            d[o + 2] = (float) (org.z - eye.z);
            d[o + 3] = (float) dir.x;
            d[o + 4] = (float) dir.y;
            d[o + 5] = (float) dir.z;
            d[o + 6] = len;
            d[o + 7] = cosHalf;
            d[o + 8] = cut;
        }
        d[16] = n;
        var img = texture.getPixels();
        if (img == null) return;
        for (int i = 0; i < 128; i++) img.setPixelABGR(i, 0, Float.floatToRawIntBits(d[i]));
        texture.upload();
    }

    /**
     * The projection the level was drawn with this frame (mixin/client/GameRendererMixin: vanilla's projection times
     * the view bob, the hurt tilt and the nausea warp), or null when the hook didn't run. CameraRenderState's own
     * projection lacks all three, so positions rebuilt from depth with it sway as the camera bobs.
     */
    private static Matrix4f levelProjection;

    /** GameRendererMixin, in renderLevel: the projection the level is drawn with (null at the start of the frame). */
    public static void levelProjection(Matrix4f m) {
        levelProjection = m == null ? null : new Matrix4f(m);
    }

    /** The bob as the level applied it this frame: B with level projection = camera projection x B, or null. */
    static Matrix4f bob(net.minecraft.client.renderer.state.level.CameraRenderState cs) {
        Matrix4f p = levelProjection;
        return p == null ? null : new Matrix4f(cs.projectionMatrix).invert().mul(p);
    }

    /** projection x view rotation as the shader inverts it: only the bob's linear part goes in; its shift moves the eye ({@link #eye}). */
    static Matrix4f viewProjection(net.minecraft.client.renderer.state.level.CameraRenderState cs) {
        Matrix4f b = bob(cs);
        Matrix4f m = new Matrix4f(cs.projectionMatrix);
        if (b != null) m.mul(new Matrix4f(b).setTranslation(0, 0, 0));
        return m.mul(cs.viewRotationMatrix);
    }

    /** Where the eye really is: the camera's position, moved by the view bob's shift. */
    static Vec3 eye(net.minecraft.client.renderer.state.level.CameraRenderState cs) {
        Matrix4f b = bob(cs);
        if (b == null) return cs.pos;
        org.joml.Vector3f t = b.getTranslation(new org.joml.Vector3f());
        org.joml.Vector3f e = new Matrix4f(b).setTranslation(0, 0, 0).invert().transformPosition(t.negate());
        new Matrix4f(cs.viewRotationMatrix).invert().transformPosition(e);
        return cs.pos.add(e.x, e.y, e.z);
    }
}
