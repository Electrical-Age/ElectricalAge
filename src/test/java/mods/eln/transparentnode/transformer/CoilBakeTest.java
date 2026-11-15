package mods.eln.transparentnode.transformer;

import mods.eln.gl.Bake;
import mods.eln.gl.BakeMesh;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A transformer's coils are drawn by the chunk (todo #423): their textures are sprites of the block atlas and nothing
 * about them changes from frame to frame, so once settled the renderer sends none of their vertices; other turns,
 * layers or colour take them back until the chunk shows the new ones.
 */
class CoilBakeTest {
    /** As ModelSprites puts textures into the block atlas: eln's model/ and sprites/ (but sprites/gui/) png files that exist. */
    static boolean inAtlas(Identifier t) {
        if (t == null) return true;   // (untextured: the white sprite)
        String p = t.getPath();
        if (!t.getNamespace().equals("eln") || !p.endsWith(".png")) return false;
        if (!(p.startsWith("model/") || p.startsWith("sprites/") && !p.startsWith("sprites/gui/"))) return false;
        return CoilBakeTest.class.getResource("/assets/eln/" + p) != null;
    }

    final LinkedHashSet<Bake.Call> settled = new LinkedHashSet<>();
    final List<Identifier> asked = new ArrayList<>();
    int left;
    long clock = 2_000_000_000_000L;
    final Object owner = new Object();
    final BlockPos pos = new BlockPos(100, 15, -40);
    Bake.Host oldHost;
    int oldMode;

    @BeforeEach
    void host() {
        oldHost = Bake.host;
        oldMode = Bake.mode;
        Bake.clear();
        Bake.mode = 1;
        Bake.host = new Bake.Host() {
            public boolean hasSprite(Identifier texture) {
                asked.add(texture);
                return inAtlas(texture);
            }

            public void settled(Bake.Call c) {
                settled.add(c);
            }

            public void unsettled(Bake.Call c) {
                assertTrue(settled.remove(c));
                left++;
            }

            public void frame() {
            }

            public String stats() {
                return "";
            }
        };
    }

    @AfterEach
    void off() {
        Bake.sweep(o -> true);
        Bake.clear();
        Bake.mode = oldMode;
        Bake.host = oldHost;
        GlFrame.beginRenderFrame();
    }

    /** One rendered frame 50 ms later; the vertices the renderer still sends. */
    int frame(Runnable draw) {
        clock += 50_000_000L;
        GlFrame.beginRenderFrame(clock);
        GlFrame f = GlFrame.openBlock(new Matrix4f(), new Matrix3f());
        try {
            f.bake(owner, pos.asLong());
            draw.run();
        } finally {
            GlFrame.close();
        }
        int n = 0;
        for (GlFrame.Batch b : f.batches().values()) n += b.vertices();
        return n;
    }

    /** Frames until the parts have settled (0.4 s the same), then the chunk shows them. */
    void settleAndShow(Runnable draw, int vertices) {
        for (int i = 0; i < 12; i++) assertEquals(vertices, frame(draw), "the renderer draws everything until the chunk shows it");
        for (Bake.Call c : settled) c.shown = true;
    }

    /** The vertices the chunk mesh holds for the settled calls (a two-sided quad is written twice). */
    int meshVertices() {
        Map<Identifier, BakeMesh.Spr> sprites = new HashMap<>();
        for (Bake.Call c : settled) sprites.put(c.texture, new BakeMesh.Spr(0, 0, 1, 1, false));
        int[] n = {0};
        Bake.Call[] calls = Bake.inDrawOrder(settled);
        BakeMesh.write((cutout, x, y, z, color, u, v, light) -> n[0]++, calls, new int[calls.length], sprites, pos.getX(), pos.getY(), pos.getZ(), false);
        return n[0];
    }

    /** The two coils as the transformer draws them (TransformerDescriptor.drawCoils), without its core. */
    static Runnable coils(Coil primary, int primaryRgb, Coil secondary, int secondaryRgb) {
        return () -> {
            TransformerDescriptor.drawCoil(primary, primaryRgb);
            GL11.glPushMatrix();
            GL11.glRotatef(180, 0f, 1f, 0f);
            TransformerDescriptor.drawCoil(secondary, secondaryRgb);
            GL11.glPopMatrix();
        };
    }

    static final int RED = 0xB02E26, BLUE = 0x3C44AA, BLACK = 0x1D1D21, WHITE = 0xF9FFFE, YELLOW = 0xFED83D, ORANGE = 0xF9801D, GRAY = 0x474F52;

    @Test
    void settledCoilsLeaveTheRendererAndComeBackWhenRewound() {
        double total = 12 * 2.5 + 58 * 1.5;
        Coil p = Coil.of(12, 2.5, 150, total), s = Coil.of(58, 1.5, 150, total);
        int all = TransformerDescriptor.coilVertices(WindingTexture.bands(p.turnsPerLayer())) + TransformerDescriptor.coilVertices(WindingTexture.bands(s.turnsPerLayer()));
        Runnable draw = coils(p, RED, s, BLUE);
        settleAndShow(draw, all);
        assertEquals(4, settled.size(), "two coils, each its sides and its ends");
        assertEquals(0, frame(draw), "vertices left with the renderer once the chunk shows the coils");
        assertEquals(2 * all, meshVertices(), "the chunk holds every quad, front and back");
        for (Identifier t : asked) assertTrue(inAtlas(t), t + " is in the block atlas");
        System.out.println("[coil bake] 12 x 2.5 mm2 + 58 x 1.5 mm2: " + p + " " + s + ": " + all + " vertices a frame before the chunk shows them, 0 after; "
            + meshVertices() + " vertices in the chunk mesh; a coil was 2096 a frame with a painted texture");

        // another colour on the secondary: its two parts come back to the renderer, then settle again
        Runnable recoloured = coils(p, RED, s, YELLOW);
        int secondary = TransformerDescriptor.coilVertices(WindingTexture.bands(s.turnsPerLayer()));
        left = 0;
        assertEquals(secondary, frame(recoloured), "the recoloured coil is the renderer's again, the other stays in the chunk");
        // (a part that changed after it had settled waits twice as long the next time: 0.8 s)
        for (int i = 0; i < 30; i++) frame(recoloured);
        assertEquals(2, left, "the chunk was told to drop the old colour's sides and ends");
        assertEquals(4, settled.size());
        for (Bake.Call c : settled) c.shown = true;
        assertEquals(0, frame(recoloured));

        // other turns on the primary (more turns of thinner wire): other bands on its sides, other rings on its ends
        Coil p2 = Coil.of(40, 0.75, 150, 40 * 0.75 + 58 * 1.5);
        assertNotEquals(WindingTexture.bands(p.turnsPerLayer()), WindingTexture.bands(p2.turnsPerLayer()));
        Runnable rewound = coils(p2, RED, Coil.of(58, 1.5, 150, 40 * 0.75 + 58 * 1.5), YELLOW);
        left = 0;
        int first = frame(rewound);
        assertTrue(first >= TransformerDescriptor.coilVertices(WindingTexture.bands(p2.turnsPerLayer())), "the rewound coil is the renderer's again: " + first);
        for (int i = 0; i < 60; i++) frame(rewound);
        assertTrue(left >= 2, "the chunk was told to drop the old winding");
        assertEquals(4, settled.size());
        for (Bake.Call c : settled) c.shown = true;
        assertEquals(0, frame(rewound));
    }

    /** The control: bound to a texture that is not in the atlas (as the painted ones were), the same coil stays with the renderer. */
    @Test
    void aTextureOutsideTheAtlasKeepsTheCoilWithTheRenderer() {
        Coil c = Coil.of(12, 2.5, 150, 117);
        int bands = WindingTexture.bands(c.turnsPerLayer());
        Identifier painted = Identifier.fromNamespaceAndPath("eln", "dynamic/winding_0");
        assertFalse(inAtlas(painted));
        Runnable draw = () -> {
            GL11.bindTexture(painted);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glPushMatrix();
            GL11.glScalef(1 / 16f, 1 / 16f, 1 / 16f);
            GL11.glCallList(TransformerDescriptor.coilSidesList(c.build(), bands));
            GL11.glCallList(TransformerDescriptor.coilEndsList(c.build()));
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_CULL_FACE);
        };
        int all = TransformerDescriptor.coilVertices(bands);
        for (int i = 0; i < 14; i++) assertEquals(all, frame(draw));
        assertTrue(settled.isEmpty());
    }

    /** What the renderer is handed for a coil: two batches (sides, ends) under the wire's set of sprites, every vertex in the tint, seen from both sides. */
    @Test
    void aCoilIsTwoTintedBatches() {
        Coil c = Coil.of(12, 2.5, 150, 117);
        int[][] wires = {{RED, WindingTexture.NORMAL}, {BLACK, WindingTexture.DARK}, {WHITE, WindingTexture.BRIGHT}};
        for (int[] w : wires) {
            GlFrame.beginRenderFrame();
            GlFrame f = GlFrame.openBlock(new Matrix4f(), new Matrix3f());
            try {
                TransformerDescriptor.drawCoil(c, w[0]);
            } finally {
                GlFrame.close();
            }
            assertEquals(2, f.batches().size());
            int tint = 0xFF000000 | WindingTexture.tint(w[0], false), sides = 0, ends = 0;
            for (Map.Entry<GlFrame.Key, GlFrame.Batch> e : f.batches().entrySet()) {
                GlFrame.Key k = e.getKey();
                GlFrame.Batch b = e.getValue();
                assertFalse(k.cull());
                assertEquals(GlFrame.Blend.NONE, k.blend());
                if (k.texture().equals(WindingTexture.side(w[1]))) sides = b.vertices();
                else if (k.texture().equals(WindingTexture.end(c.layers(), w[1]))) ends = b.vertices();
                for (int i = 0; i < b.floats; i += GlFrame.STRIDE) {
                    assertEquals(tint, Float.floatToRawIntBits(b.data[i + 5]), "the vertex colour is the tint");
                    float u = b.data[i + 3], v = b.data[i + 4];
                    assertTrue(u >= 0 && u <= 1 && v >= 0 && v <= 1, "inside the sprite");
                }
            }
            assertEquals(16 * WindingTexture.bands(c.turnsPerLayer()), sides);
            assertEquals(2080, ends);
        }
    }

    @Test
    void theSpritesAreFilesTheAtlasTakes() throws Exception {
        try (InputStream in = CoilBakeTest.class.getResourceAsStream("/assets/minecraft/atlases/blocks.json")) {
            assertNotNull(in, "the block atlas' sources");
            assertTrue(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("\"eln:model_textures\""), "ModelSprites is a source of the block atlas");
        }
        for (int kind = 0; kind < 3; kind++) {
            BufferedImage turn = image(WindingTexture.side(kind));
            assertEquals(WindingTexture.TURN_W, turn.getWidth());
            assertEquals(WindingTexture.TURN_H, turn.getHeight());
            for (int y = 0; y < turn.getHeight(); y++)
                for (int x = 0; x < turn.getWidth(); x++) assertEquals(grey(WindingTexture.turnGrey(y, kind)), turn.getRGB(x, y), "turn " + kind + " at " + x + " " + y);
            for (int n = 1; n <= WindingTexture.MAX_RINGS; n++) {
                BufferedImage end = image(WindingTexture.end(n, kind));
                // (multiples of 16: ModelSprites pads nothing, the sprite is the file)
                assertEquals(WindingTexture.END_W, end.getWidth());
                assertEquals(WindingTexture.END_H, end.getHeight());
                for (int y = 0; y < end.getHeight(); y++)
                    for (int x = 0; x < end.getWidth(); x++) assertEquals(grey(WindingTexture.endGrey(x, n, kind)), end.getRGB(x, y), "end " + kind + " of " + n + " at " + x);
            }
        }
        assertEquals(WindingTexture.end(10, 0), WindingTexture.end(37, 0), "more than ten layers show as ten rings");
        assertEquals(WindingTexture.MAX_BANDS, WindingTexture.bands(200));
    }

    static int grey(int v) {
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    static BufferedImage image(Identifier id) throws Exception {
        assertTrue(inAtlas(id), id + " is a file ModelSprites stitches");
        try (InputStream in = CoilBakeTest.class.getResourceAsStream("/assets/eln/" + id.getPath())) {
            BufferedImage img = ImageIO.read(in);
            assertNotNull(img, id.toString());
            return img;
        }
    }

    /** The colour of a painted texture's pixel (the look until 2026-11-15): the wire's channel times the shade, cut off at 255. */
    static int painted(int c, double shade) {
        return Math.min(255, (int) Math.round(c * shade));
    }

    /**
     * Tint times sprite gives what the painted texture had: to a rounding step for most colours and for black; a bright
     * colour (a channel over 212) loses a little of its highlights in its other channels.
     */
    @Test
    void tintedSpritesLookAsThePaintedTexture() {
        int worstBright = 0;
        for (int rgb : new int[]{RED, BLUE, GRAY, BLACK, WHITE, YELLOW, ORANGE})
            for (boolean over : new boolean[]{false, true}) {
                int kind = WindingTexture.kind(rgb, over), wire = WindingTexture.wire(rgb, over), tint = WindingTexture.tint(rgb, over);
                if (!over) assertEquals(rgb == BLACK ? WindingTexture.DARK : rgb == WHITE || rgb == YELLOW || rgb == ORANGE ? WindingTexture.BRIGHT : WindingTexture.NORMAL, kind,
                    Integer.toHexString(rgb));
                else assertNotEquals(WindingTexture.DARK, kind, "an overfull coil is red, never dark");
                for (int shift = 0; shift <= 16; shift += 8) {
                    int c = wire >> shift & 255, t = tint >> shift & 255;
                    for (int part = 0; part <= WindingTexture.MAX_RINGS; part++) {   // (0 = the sides' turn, n = the end of n rings)
                        int size = part == 0 ? WindingTexture.TURN_H : WindingTexture.END_W;
                        for (int i = 0; i < size; i++) {
                            int g = part == 0 ? WindingTexture.turnGrey(i, kind) : WindingTexture.endGrey(i, part, kind);
                            int now = Math.round(t * g / 255f);
                            int was = was(c, part, i, kind);
                            int d = Math.abs(now - was);
                            if (kind == WindingTexture.BRIGHT) {
                                worstBright = Math.max(worstBright, d);
                                assertTrue(now <= was + 1 && d <= 0.2 * c + 1, "bright: " + now + " was " + was);
                            } else assertTrue(d <= 3, Integer.toHexString(rgb) + " part " + part + " at " + i + ": " + now + " was " + was);
                        }
                    }
                }
            }
        System.out.println("[coil bake] bright wire: a channel differs from the painted texture by at most " + worstBright + " of 255");
        assertTrue(worstBright <= 40);
    }

    /** The painted texture's channel at row i of a turn (part 0) or column i of an end of part rings, at the sprites' sizes. */
    static int was(int c, int part, int i, int kind) {
        boolean dark = kind == WindingTexture.DARK;
        if (part == 0) {
            boolean line = i == WindingTexture.TURN_H - 1;
            double t = (i + 0.5) / WindingTexture.TURN_H;
            return line && dark ? Math.min(255, c + 46) : painted(c, line ? 0.45 : 1.12 - 0.4 * t);
        }
        int w = WindingTexture.END_W, k = i * part / w, x0 = k * w / part, x1 = (k + 1) * w / part;
        double t = (i - x0 + 0.5) / Math.max(1, x1 - x0);
        boolean line = x1 - x0 >= 3 && i == x1 - 1;
        return line && dark ? Math.min(255, c + 46) : painted(c, line ? 0.5 : 1.2 - 0.4 * t);
    }
}
