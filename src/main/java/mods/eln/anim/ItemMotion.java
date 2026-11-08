package mods.eln.anim;

import mods.eln.gl.GL11;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Items being worked in a machine (Baughn 2026-11-07: "the current item animation is just a rotating item"): an item
 * drawn at a place, turned, and scaled on each axis about its foot (so it squashes onto what it sits on), and the
 * shaping functions the machines use to move it along with their {@link Progress}.
 */
public final class ItemMotion {
    private ItemMotion() {
    }

    /** The item at (x, y, z) (its foot; block units, model frame), turned rotY about y, scaled sx, sy, sz. */
    public static void draw(ItemEntity item, double x, double y, double z, float rotY, float sx, float sy, float sz) {
        if (item == null || item.getItem().isEmpty()) return;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef(rotY, 0, 1, 0);
        GL11.glScalef(sx, sy, sz);
        mods.eln.gl.GlFrame.current().extra(item.getItem());
        GL11.glPopMatrix();
    }

    /** 0 below a, 1 above b, eased (smoothstep) between. */
    public static float ease(float t, float a, float b) {
        if (b <= a) return t >= b ? 1 : 0;
        float u = Math.max(0, Math.min(1, (t - a) / (b - a)));
        return u * u * (3 - 2 * u);
    }

    /** a at 0, b at 1. */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * A small shake while it's worked (an item rattling in a crusher): an offset of up to amp, from a phase the render
     * advances in refresh (deg of a {@link Spin}), so it stops with the machine and the game.
     */
    public static float shake(float phaseDeg, float amp, int axis) {
        double p = Math.toRadians(phaseDeg);
        return (float) (amp * (axis == 0 ? Math.sin(p * 7) * Math.cos(p * 3) : Math.sin(p * 5 + 1.3) * Math.cos(p * 11)));
    }
}
