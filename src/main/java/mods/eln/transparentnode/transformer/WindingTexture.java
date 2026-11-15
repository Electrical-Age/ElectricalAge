package mods.eln.transparentnode.transformer;

import net.minecraft.resources.Identifier;

/**
 * A converter winding's look (Baughn 2026-11-07: stylized, generated, the configuration readable at a glance). A coil's
 * sides are one band a turn of a layer, each showing the whole "one turn" sprite (light at its top, dark at its foot, a
 * line between turns); its ends show one ring a layer, from a sprite per number of rings. The sprites are greyscale files
 * (assets/eln/sprites/winding, written by tools/winding_sprites.py) and the wire's insulation colour is the coil's vertex
 * colour ({@link #tint}), reddened when the core's window is overfull. So the textures are in the block atlas
 * (ModelSprites takes every eln sprite) and an unchanging coil is drawn by the chunk ({@link mods.eln.gl.Bake}), not every
 * frame: a texture painted at run time per configuration, as this was until 2026-11-15, kept 2,096 vertices a coil with
 * the renderer ("no sprite").
 */
public final class WindingTexture {
    private WindingTexture() {
    }

    /** The sprites' white stands for this shade (the brightest: the inner edge of an end's ring). */
    public static final double SCALE = 1.2;
    /** On dark wire the line between turns is a highlight (the sprites' white); the wire's body is this much of it. */
    public static final double DARK_BODY = 0.4;
    /** What the highlight line on dark wire adds to each channel. */
    static final int SHEEN = 46;
    /** Bands on a coil's side at most (more turns a layer than this show as this many), and rings on its end. */
    public static final int MAX_BANDS = 64, MAX_RINGS = 10;
    public static final int TURN_W = 16, TURN_H = 32, END_W = 32, END_H = 16;

    /**
     * The sets of sprites, by the wire's colour: NORMAL (white = shade 1.2, the tint is the colour times 1.2), BRIGHT
     * (a channel over 212 can't be tinted up that far: white = shade 1 and brighter is cut off, as a painted texture's
     * channel was at 255; the tint is the colour itself) and DARK (black wire: the line between turns is a highlight).
     */
    public static final int NORMAL = 0, BRIGHT = 1, DARK = 2;
    static final String[] SETS = {"", "_bright", "_dark"};
    private static final Identifier[] TURN = new Identifier[3];
    private static final Identifier[][] END = new Identifier[3][MAX_RINGS + 1];

    static {
        for (int kind = 0; kind < 3; kind++) {
            TURN[kind] = id("turn" + SETS[kind]);
            for (int n = 1; n <= MAX_RINGS; n++) END[kind][n] = id("end" + SETS[kind] + "_" + n);
        }
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("eln", "sprites/winding/" + name + ".png");
    }

    /** The bands drawn on a coil's side for its turns per layer. */
    public static int bands(int turnsPerLayer) {
        return Math.max(1, Math.min(MAX_BANDS, turnsPerLayer));
    }

    /** The rings drawn on a coil's end for its layers (more than ten would blur into a slab: ten at most, each a few layers). */
    public static int rings(int layers) {
        return Math.max(1, Math.min(MAX_RINGS, layers));
    }

    /** The wire's colour as drawn, 0xRRGGBB: dark insulation lifted a little (it would hide the shading), red when over. */
    static int wire(int rgb, boolean over) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int lift = Math.max(0, 34 - Math.max(r, Math.max(g, b)));
        r += lift;
        g += lift;
        b += lift;
        if (over) {
            r = (r + 230) / 2;
            g = g / 2;
            b = b / 2;
        }
        return r << 16 | g << 8 | b;
    }

    /** The set of sprites for wire of this colour: DARK under 70 in every channel, BRIGHT with a channel over 212. */
    public static int kind(int rgb, boolean over) {
        int w = wire(rgb, over);
        int max = Math.max(w >> 16 & 255, Math.max(w >> 8 & 255, w & 255));
        return max < 70 ? DARK : (int) (max * SCALE + 0.5) > 255 ? BRIGHT : NORMAL;
    }

    /** The colour the coil's vertices carry, 0xRRGGBB: times a sprite's grey (of the colour's set) it gives the wire's colour at that shade. */
    public static int tint(int rgb, boolean over) {
        int w = wire(rgb, over);
        int r = w >> 16 & 255, g = w >> 8 & 255, b = w & 255;
        return switch (kind(rgb, over)) {
            case DARK -> Math.min(255, r + SHEEN) << 16 | Math.min(255, g + SHEEN) << 8 | Math.min(255, b + SHEEN);
            case BRIGHT -> w;
            default -> (int) (r * SCALE + 0.5) << 16 | (int) (g * SCALE + 0.5) << 8 | (int) (b * SCALE + 0.5);
        };
    }

    /** The texture of a coil's sides (one turn, repeated by its bands) in a set. */
    public static Identifier side(int kind) {
        return TURN[kind];
    }

    /** The texture of a coil's ends for its layers in a set. */
    public static Identifier end(int layers, int kind) {
        return END[kind][rings(layers)];
    }

    // ---- what the sprite files hold (tools/winding_sprites.py paints the same; WindingTextureTest compares)

    /** A sprite's grey (0..255) for a shade; line = the line between turns or rings. */
    static int grey(double shade, boolean line, int kind) {
        if (kind == DARK) return line ? 255 : Math.min(255, (int) (255 * shade * DARK_BODY + 0.5));
        if (kind == BRIGHT) return Math.min(255, (int) (255 * shade + 0.5));
        return Math.min(255, (int) (255 * shade / SCALE + 0.5));
    }

    /** The "one turn" sprite's grey in row y (0 = the turn's top). */
    static int turnGrey(int y, int kind) {
        boolean line = y == TURN_H - 1;
        double t = (y + 0.5) / TURN_H;
        return grey(line ? 0.45 : 1.12 - 0.4 * t, line, kind);
    }

    /** An end sprite's grey in column x (0 = at the hole) for n rings. */
    static int endGrey(int x, int n, int kind) {
        int k = x * n / END_W, x0 = k * END_W / n, x1 = (k + 1) * END_W / n;
        double t = (x - x0 + 0.5) / Math.max(1, x1 - x0);
        boolean line = x1 - x0 >= 3 && x == x1 - 1;
        return grey(line ? 0.5 : 1.2 - 0.4 * t, line, kind);
    }
}
