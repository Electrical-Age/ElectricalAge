package mods.eln.transparentnode.transformer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A converter winding's look (Baughn 2026-11-07: stylized, generated, the configuration readable at a glance), painted
 * once per configuration and cached. Left half: the coil's sides, one stripe a turn of a layer, each shaded light at its
 * top and dark at its foot (a baked bump). Right half: its top, one ring a layer. The wire's insulation colour; tinted
 * red when the core's window is overfull. Client only.
 */
public final class WindingTexture {
    static final int W = 64, H = 256;
    static int next;
    /** At most this many configurations kept (the least recently drawn are freed). */
    static final int KEEP = 32;
    static final Map<String, WindingTexture> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, WindingTexture> e) {
            if (size() <= KEEP) return false;
            e.getValue().close();
            return true;
        }
    };
    static boolean noGpu;

    final Identifier id;
    final DynamicTexture texture;

    WindingTexture(int turnsPerLayer, int layers, int rgb, boolean over) {
        id = Identifier.fromNamespaceAndPath("eln", "dynamic/winding_" + next++);
        texture = new DynamicTexture(() -> "eln winding", W, H, true);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        NativeImage img = texture.getPixels();
        if (img != null) {
            paint(img, turnsPerLayer, layers, rgb, over);
            texture.upload();
        }
    }

    void close() {
        Minecraft.getInstance().getTextureManager().release(id);
    }

    /** The texture for a coil of turns per layer and layers of a wire of rgb; null where there is no GPU (a test). */
    static Identifier of(int turnsPerLayer, int layers, int rgb, boolean over) {
        if (noGpu) return null;
        String key = turnsPerLayer + "/" + layers + "/" + rgb + "/" + over;
        WindingTexture t = CACHE.get(key);
        if (t == null) {
            try {
                t = new WindingTexture(turnsPerLayer, layers, rgb, over);
            } catch (RuntimeException | LinkageError e) {   // (no GPU: a headless test draws it untextured)
                noGpu = true;
                return null;
            }
            CACHE.put(key, t);
        }
        return t.id;
    }

    static void paint(NativeImage img, int turnsPerLayer, int layers, int rgb, boolean over) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        // (dark insulation would hide the shading: lift it a little)
        int lift = Math.max(0, 34 - Math.max(r, Math.max(g, b)));
        r += lift;
        g += lift;
        b += lift;
        if (over) {
            r = (r + 230) / 2;
            g = g / 2;
            b = b / 2;
        }
        int tpl = Math.max(1, Math.min(H, turnsPerLayer)), lay = Math.max(1, Math.min(W / 2, layers));
        for (int y = 0; y < H; y++) {
            int k = y * tpl / H, y0 = k * H / tpl, y1 = (k + 1) * H / tpl;
            float t = (y - y0 + 0.5f) / Math.max(1, y1 - y0);
            float shade = y1 - y0 >= 3 && y == y1 - 1 ? 0.45f : 1.12f - 0.4f * t;
            int c = colour(r, g, b, shade);
            for (int x = 0; x < W / 2; x++) img.setPixel(x, y, c);
        }
        for (int x = 0; x < W / 2; x++) {
            int k = x * lay / (W / 2), x0 = k * (W / 2) / lay, x1 = (k + 1) * (W / 2) / lay;
            float t = (x - x0 + 0.5f) / Math.max(1, x1 - x0);
            float shade = x1 - x0 >= 3 && x == x1 - 1 ? 0.5f : 1.2f - 0.4f * t;
            int c = colour(r, g, b, shade);
            for (int y = 0; y < H; y++) img.setPixel(W / 2 + x, y, c);
        }
    }

    /** ARGB of rgb scaled by shade. */
    static int colour(int r, int g, int b, float shade) {
        int rr = Math.min(255, Math.round(r * shade)), gg = Math.min(255, Math.round(g * shade)), bb = Math.min(255, Math.round(b * shade));
        return 0xFF000000 | rr << 16 | gg << 8 | bb;
    }
}
