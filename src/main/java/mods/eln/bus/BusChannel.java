package mods.eln.bus;

/**
 * The field bus's 16 channels, one per Minecraft dye colour, numbered as Minecraft numbers its dyes (DyeColor ids):
 * 0 white .. 15 black (Baughn: colour-tuned channels, not named ones). The names are DyeColor's, which signal processor
 * programs write as bus.name. Wherever a channel's colour is shown its number and name go with it (Baughn: the colours
 * aren't colourblind-safe). Plain Java, so the bus can be tested without the game; BusChannelTest holds the table to
 * DyeColor.
 */
public final class BusChannel {
    private BusChannel() {
    }

    public static final int COUNT = 16;
    /** A terminal or logger channel tuned to no bus channel. */
    public static final int UNUSED = -1;

    static final String[] NAMES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    /** DyeColor.getTextureDiffuseColor, as 0xRRGGBB. */
    static final int[] RGB = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
        0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

    public static boolean valid(int c) {
        return c >= 0 && c < COUNT;
    }

    /** A saved or sent value: a channel, or UNUSED for anything else. */
    public static int orUnused(int c) {
        return valid(c) ? c : UNUSED;
    }

    /** Its dye name, as Minecraft writes it (light_blue). */
    public static String name(int c) {
        return valid(c) ? NAMES[c] : "";
    }

    /** Its number and name, as shown to players: "3 light_blue". */
    public static String label(int c) {
        return valid(c) ? c + " " + NAMES[c] : "";
    }

    /** Its swatch colour, 0xRRGGBB. */
    public static int rgb(int c) {
        return valid(c) ? RGB[c] : 0;
    }

    /** The channel with this dye name, or UNUSED. */
    public static int byName(String name) {
        for (int c = 0; c < COUNT; c++) if (NAMES[c].equals(name)) return c;
        return UNUSED;
    }

    /** The 16 names in channel order, comma separated (for messages listing the choices). */
    public static String choices() {
        return String.join(", ", NAMES);
    }

    /**
     * The channel an EA brush paints: EA numbers its brushes and wire colours the other way round, 0 black .. 15 white
     * (1.12's dye item damage), so brush colour b is channel 15 - b.
     */
    public static int fromBrush(int brushColour) {
        return orUnused(15 - (brushColour & 0xF));
    }

    /** The next channel round the cycle UNUSED, 0 .. 15, UNUSED (a screen's button); back: the other way. */
    public static int cycle(int c, boolean back) {
        int i = orUnused(c) + 1; // 0 = UNUSED
        i = Math.floorMod(i + (back ? -1 : 1), COUNT + 1);
        return i - 1;
    }
}
