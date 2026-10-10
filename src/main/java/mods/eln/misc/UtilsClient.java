package mods.eln.misc;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D.Obj3DPart;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.awt.Color;

/**
 * The drawing helpers of 1.12 UtilsClient that EA's render code calls, on the GL11 shim (P3). Server-safe: what needs
 * the client (player position, light levels, item rendering) goes through {@link #hooks}, which the client setup
 * replaces. GUI and item-icon helpers of 1.12 are not here (P4b / R5).
 */
public final class UtilsClient {
    private UtilsClient() {
    }

    /** Client services the draw code needs; the defaults are what a server (or a test) can answer. */
    public interface Hooks {
        /** Manhattan distance from the client player to a point (1.12 drawHalo's d). */
        default float distanceFromClientPlayer(double x, double y, double z) {
            return 0;
        }

        /** Combined block light level 0..15 at a position (1.12 UtilsClient.getLight). */
        default int getLight(Level w, int x, int y, int z) {
            return 15;
        }

        /** The game is paused (single player menu): animations stop. */
        default boolean isGameInPause() {
            return false;
        }

        /** The client player's yaw, pitch (degrees) and position (reflection effects on screens and glass). */
        default float playerYaw() {
            return 0;
        }

        default float playerPitch() {
            return 0;
        }

        default double playerX() {
            return 0;
        }

        default double playerZ() {
            return 0;
        }

        /** The client player's helmet (signal inductor: goggles). */
        default net.minecraft.world.item.ItemStack playerHelmet() {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        /** Draw an item entity at the current GL11 matrix (1.12 drawEntityItem). */
        default void drawEntityItem(ItemEntity entityItem, double x, double y, double z, float roty, float scale) {
        }
    }

    public static Hooks hooks = new Hooks() {
    };

    public static final Identifier WHITE = Identifier.fromNamespaceAndPath("eln", "textures/misc/white.png");

    public static void bindTexture(Identifier resource) {
        GL11.bindTexture(resource);
    }

    public static void enableBilinear() {
    }

    public static void disableBilinear() {
    }

    public static void disableCulling() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public static void enableCulling() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public static void disableTexture() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public static void enableTexture() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** 1.12: lighting and the lightmap off, i.e. full-bright. */
    public static void disableLight() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.setFullBright(true);
    }

    public static void enableLight() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.setFullBright(false);
    }

    public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void enableDepthTest() {
    }

    public static void disableDepthTest() {
    }

    public static void ledOnOffColor(boolean on) {
        if (!on) GL11.glColor3f(0.7f, 0f, 0f); // red
        else GL11.glColor3f(0f, 0.7f, 0f); // green
    }

    public static Color ledOnOffColorC(boolean on) {
        return on ? new Color(0f, 0.7f, 0f) : new Color(0.7f, 0f, 0f);
    }

    public static void glDefaultColor() {
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    public static void drawLight(Obj3DPart part) {
        if (part == null) return;
        disableLight();
        enableBlend();
        part.draw();
        enableLight();
        disableBlend();
    }

    public static void drawLightNoBind(Obj3DPart part) {
        if (part == null) return;
        disableLight();
        enableBlend();
        part.drawNoBind();
        enableLight();
        disableBlend();
    }

    public static void drawLight(Obj3DPart part, float angle, float x, float y, float z) {
        if (part == null) return;
        disableLight();
        enableBlend();
        part.draw(angle, x, y, z);
        enableLight();
        disableBlend();
    }

    /** 1.12: 1 thundering, 0.5 raining, else 0. */
    public static double getWeather(Level world) {
        if (world == null) return 0;
        if (world.isThundering()) return 1.0;
        if (world.isRaining()) return 0.5;
        return 0.0;
    }

    public static int getLight(Level w, int x, int y, int z) {
        return hooks.getLight(w, x, y, z);
    }

    /** A halo fades with the light level; its part spins with the player's distance (1.12). */
    public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, Level w, int x, int y, int z, boolean bilinear) {
        if (halo == null) return;
        int light = getLight(w, x, y, z) * 19 / 15 - 4;
        float d = hooks.distanceFromClientPlayer(x, y, z);
        GL11.glColor4f(r, g, b, 1f - (light / 15f));
        halo.draw(d * 20, 1, 0, 0);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    public static void drawHalo(Obj3DPart halo, float r, float g, float b, Level w, int x, int y, int z, boolean bilinear) {
        disableLight();
        enableBlend();
        drawHaloNoLightSetup(halo, r, g, b, w, x, y, z, bilinear);
        enableLight();
        disableBlend();
    }

    public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, BlockEntity e, boolean bilinear) {
        BlockPos p = e.getBlockPos();
        drawHaloNoLightSetup(halo, r, g, b, e.getLevel(), p.getX(), p.getY(), p.getZ(), bilinear);
    }

    public static void drawHalo(Obj3DPart halo, float r, float g, float b, BlockEntity e, boolean bilinear) {
        BlockPos p = e.getBlockPos();
        drawHalo(halo, r, g, b, e.getLevel(), p.getX(), p.getY(), p.getZ(), bilinear);
    }

    public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, Entity e, boolean bilinear) {
        if (halo == null) return;
        int light = getLight(e.level(), (int) Math.floor(e.getX()), (int) Math.floor(e.getY()), (int) Math.floor(e.getZ()));
        GL11.glColor4f(r, g, b, 1f - (light / 15f));
        halo.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    public static void drawHalo(Obj3DPart halo, float r, float g, float b, Entity e, boolean bilinear) {
        disableLight();
        enableBlend();
        drawHaloNoLightSetup(halo, r, g, b, e, bilinear);
        enableLight();
        disableBlend();
    }

    public static void drawHaloNoLightSetup(Obj3DPart halo, float distance) {
        if (halo == null) return;
        halo.draw();
    }

    public static void drawHalo(Obj3DPart halo, float distance) {
        disableLight();
        enableBlend();
        drawHaloNoLightSetup(halo, distance);
        enableLight();
        disableBlend();
    }

    /** An item on a machine (1.12 drawEntityItem: an EntityItem rendered at x, y, z, turned by roty, scaled). */
    public static void drawEntityItem(ItemEntity entityItem, double x, double y, double z, float roty, float scale) {
        if (entityItem == null || entityItem.getItem().isEmpty()) return;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef(roty, 0, 1, 0);
        GL11.glScalef(scale, scale, scale);
        mods.eln.gl.GlFrame.current().extra(entityItem.getItem());
        GL11.glPopMatrix();
    }

    public static boolean isGameInPause() {
        return hooks.isGameInPause();
    }

    public static float distanceFromClientPlayer(Level world, int x, int y, int z) {
        return hooks.distanceFromClientPlayer(x + 0.5, y + 0.5, z + 0.5);
    }

    public static float distanceFromClientPlayer(BlockEntity e) {
        BlockPos p = e.getBlockPos();
        return hooks.distanceFromClientPlayer(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
    }

    public static double clientDistanceTo(BlockEntity e) {
        return distanceFromClientPlayer(e);
    }

    public static double clientDistanceTo(Entity e) {
        return hooks.distanceFromClientPlayer(e.getX(), e.getY(), e.getZ());
    }

    private static int uuid = Integer.MIN_VALUE;

    /** Client-side ids for sounds and the like (1.12: negative, never clashing with the server's). */
    public static int getUuid() {
        if (uuid > -1) uuid = Integer.MIN_VALUE;
        return uuid++;
    }

    public static int glGenListsSafe() {
        return GL11.glGenLists(1);
    }

    public static void glDeleteListsSafe(int id) {
        GL11.glDeleteLists(id, 1);
    }

    public static void drawConnectionPinSixNode(float d, float w, float h) {
        d += 0.1f;
        d *= 0.0625f;
        w *= 0.0625f;
        h *= 0.0625f;
        float w2 = w * 0.5f;
        disableTexture();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-w2, d, 0);
        GL11.glVertex3f(w2, d, 0);
        GL11.glVertex3f(w2, d, h);
        GL11.glVertex3f(-w2, d, h);
        GL11.glEnd();
        enableTexture();
    }

    public static void drawConnectionPinSixNode(LRDU front, float[] dList, float w, float h) {
        float d = dList[front.toInt()];
        d += 0.04f;
        d *= 0.0625f;
        w *= 0.0625f;
        h *= 0.0625f;
        float w2 = w * 0.5f;
        disableTexture();
        GL11.glBegin(GL11.GL_QUADS);
        switch (front) {
            case Left:
                GL11.glVertex3f(0, -w2, -d);
                GL11.glVertex3f(0, w2, -d);
                GL11.glVertex3f(h, w2, -d);
                GL11.glVertex3f(h, -w2, -d);
                break;
            case Right:
                GL11.glVertex3f(h, -w2, d);
                GL11.glVertex3f(h, w2, d);
                GL11.glVertex3f(0, w2, d);
                GL11.glVertex3f(0, -w2, d);
                break;
            case Down:
                GL11.glVertex3f(h, -d, -w2);
                GL11.glVertex3f(h, -d, w2);
                GL11.glVertex3f(0, -d, w2);
                GL11.glVertex3f(0, -d, -w2);
                break;
            case Up:
                GL11.glVertex3f(0, d, -w2);
                GL11.glVertex3f(0, d, w2);
                GL11.glVertex3f(h, d, w2);
                GL11.glVertex3f(h, d, -w2);
                break;
            default:
                break;
        }
        GL11.glEnd();
        enableTexture();
    }

    public static float clientPlayerYaw() {
        return hooks.playerYaw();
    }

    public static float clientPlayerPitch() {
        return hooks.playerPitch();
    }

    public static double clientPlayerX() {
        return hooks.playerX();
    }

    public static double clientPlayerZ() {
        return hooks.playerZ();
    }

    public static net.minecraft.world.item.ItemStack clientPlayerHelmet() {
        return hooks.playerHelmet();
    }

    /** Text in the world at the current GL11 matrix (1.12 FontRenderer.drawString in a TESR); R4. */
    public static void drawString(String text, int x, int y, int color) {
        mods.eln.gl.GlFrame.current().text(text, x, y, color);
    }
}
