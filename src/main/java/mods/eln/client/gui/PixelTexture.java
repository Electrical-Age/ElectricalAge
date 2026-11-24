package mods.eln.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A texture drawn pixel by pixel on the client (a picture a texel a cell: the Ground Radar's views, the Auto Miner's
 * monitor): filled in place while its size holds, made anew when it changes, released with release(). A dynamic
 * texture samples nearest by default, so cells stay square.
 */
public final class PixelTexture {
    /** A pixel's colour (ARGB) at (x, y). */
    public interface Painter {
        int argb(int x, int y);
    }

    private static int next;
    private final String name;
    private DynamicTexture texture;
    private Identifier id;

    /** name: a label for the texture manager (and part of its id). */
    public PixelTexture(String name) {
        this.name = name;
    }

    /** Paint it w x h; its id, or null where there is no GPU. */
    public Identifier paint(int w, int h, Painter p) {
        try {
            if (texture == null || texture.getPixels() == null || texture.getPixels().getWidth() != w || texture.getPixels().getHeight() != h) {
                release();
                texture = new DynamicTexture(() -> "eln " + name, w, h, false);
                id = Identifier.fromNamespaceAndPath("eln", "dynamic/" + name + "_" + next++);
                Minecraft.getInstance().getTextureManager().register(id, texture);
            }
            var img = texture.getPixels();
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) img.setPixel(x, y, p.argb(x, y));
            texture.upload();
            return id;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /** Its id as last painted, or null. */
    public Identifier id() {
        return id;
    }

    public void release() {
        if (id != null) Minecraft.getInstance().getTextureManager().release(id);
        id = null;
        texture = null;
    }
}
