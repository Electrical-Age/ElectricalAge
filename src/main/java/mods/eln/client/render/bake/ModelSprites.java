package mods.eln.client.render.bake;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import mods.eln.gl.Bake;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts the model textures (assets/eln/model, bound whole by the block entity renderers) into the block atlas, so chunk
 * geometry can use them: a chunk layer samples nothing else. A sprite is named as the texture is bound. It is padded
 * to multiples of 16 pixels by repeating its edge (an odd size would cap the atlas' mipmap levels), and its alpha
 * becomes 0 or 255 at the renderers' cutoff of 0.1: the chunk's cutout layer discards below 0.5.
 * Adds nothing while baking is off.
 */
public final class ModelSprites implements SpriteSource {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("eln", "model_textures");
    public static final MapCodec<ModelSprites> CODEC = MapCodec.unit(new ModelSprites());

    /** Where a texture is in the atlas: (u, v) of the texture maps to (u0 + u * du, v0 + v * dv). holes = has discarded texels. */
    public record Spr(float u0, float v0, float du, float dv, boolean holes) {
    }

    /** Sizes as loaded: original width, height, padded width, height, 1 = has holes. */
    private static final Map<Identifier, int[]> LOADED = new ConcurrentHashMap<>();
    /** The sprites of the atlas in use, by bound texture (the white texture stands for "untextured"). Replaced whole. */
    public static volatile Map<Identifier, Spr> table = Map.of();

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        if (Bake.mode == 0) return;
        Map<Identifier, Resource> found = new HashMap<>(resourceManager.listResources("model",
            id -> id.getNamespace().equals("eln") && id.getPath().endsWith(".png")));
        resourceManager.getResource(UtilsClient.WHITE).ifPresent(r -> found.put(UtilsClient.WHITE, r));
        for (Map.Entry<Identifier, Resource> e : found.entrySet()) {
            Identifier id = e.getKey();
            Resource resource = e.getValue();
            output.add(id, (DiscardableLoader) loader -> load(id, resource));
        }
    }

    private static SpriteContents load(Identifier id, Resource resource) {
        try (InputStream in = resource.open(); NativeImage src = NativeImage.read(in)) {
            int w = src.getWidth(), h = src.getHeight();
            int pw = Math.max(16, (w + 15) & ~15), ph = Math.max(16, (h + 15) & ~15);
            NativeImage out = new NativeImage(NativeImage.Format.RGBA, pw, ph, false);
            boolean holes = false;
            for (int y = 0; y < ph; y++) {
                for (int x = 0; x < pw; x++) {
                    int p = src.getPixel(Math.min(x, w - 1), Math.min(y, h - 1));
                    boolean keep = (p >>> 24) > 25;
                    if (!keep) holes = true;
                    out.setPixel(x, y, keep ? p | 0xFF000000 : p & 0xFFFFFF);
                }
            }
            LOADED.put(id, new int[]{w, h, pw, ph, holes ? 1 : 0});
            return new SpriteContents(id, new FrameSize(pw, ph), out);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("eln").warn("model texture {} not added to the block atlas: {}", id, e.toString());
            return null;
        }
    }

    /** The atlas was built: if it is the one holding the model textures, note where each one is. */
    public static void stitched(TextureAtlas atlas) {
        Map<Identifier, TextureAtlasSprite> sprites = atlas.getTextures();
        if (!sprites.containsKey(UtilsClient.WHITE)) return;
        Map<Identifier, Spr> t = new HashMap<>();
        for (Map.Entry<Identifier, int[]> e : LOADED.entrySet()) {
            TextureAtlasSprite s = sprites.get(e.getKey());
            if (s == null) continue;
            int[] d = e.getValue();
            t.put(e.getKey(), new Spr(s.getU0(), s.getV0(), (s.getU1() - s.getU0()) * d[0] / d[2], (s.getV1() - s.getV0()) * d[1] / d[3], d[4] != 0));
        }
        table = t;
        long pixels = 0;
        for (int[] d : LOADED.values()) pixels += (long) d[2] * d[3];
        org.slf4j.LoggerFactory.getLogger("eln").info("[eln bake] {} model textures in the block atlas ({} pixels)", t.size(), pixels);
        ChunkBake.INSTANCE.reset();
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }
}
