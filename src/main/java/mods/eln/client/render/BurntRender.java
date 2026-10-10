package mods.eln.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Scorched look for burnt-out devices without new art: the model is drawn a second time over itself, every
 * fragment replaced by a soot colour (texture combiner, alpha kept for cut-outs) and multiplied into what's there.
 * Plus a wisp of smoke now and then.
 */
public final class BurntRender {
    private BurntRender() {
    }

    private static final FloatBuffer SOOT = BufferUtils.createFloatBuffer(4);

    static {
        SOOT.put(new float[]{0.30f, 0.26f, 0.22f, 1f}).flip();
    }

    private static final Random rand = new Random();

    /** Call right after the normal draw, with the same transform. */
    public static void overlay(Runnable draw) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT
            | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(false);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, SOOT);
        GL11.glColor4f(SOOT.get(0), SOOT.get(1), SOOT.get(2), 1f); // untextured parts
        try {
            draw.run();
        } finally {
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /** A wisp of smoke, about every 3 s at 60 fps. */
    public static void smoke(World w, BlockPos pos) {
        if (w == null || Minecraft.getMinecraft().isGamePaused() || rand.nextInt(180) != 0) return;
        w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + 0.3 + rand.nextDouble() * 0.4,
            pos.getY() + 0.5 + rand.nextDouble() * 0.3, pos.getZ() + 0.3 + rand.nextDouble() * 0.4, 0, 0.03, 0);
    }
}
