package mods.eln.bike;

import org.erisia.cad.Box;
import org.erisia.cad.tex.Sheet;

/**
 * The bike's entity texture (textures/entity/electric_bike.png; both bikes use it), painted from BikeShape's regions
 * (was tools/bike/texture.py, which keeps the item icons). Each region is one material with a little noise and a
 * vertical shade; PAINT, TRUNK, LID and SADDLE are neutral greys that BikeRenderer tints per dye group. The battery's
 * green charge stripe is a ring round its sides and ends, one row of texels that meets itself at the edges (Baughn
 * 2026-10-30: the old stripe, painted blind across the region, didn't match up round the edges).
 * <p>
 * Regenerate after changing BikeShape: ./gradlew bikeTexture (BikeTextureTest fails until the PNG matches).
 */
public final class BikeTexture {
    private BikeTexture() {
    }

    static final long SEED = 20261028;
    static final int NEUTRAL = 0xE2E2E2, TIRE = 0x262628, METAL = 0xB0B6BC, BATTERY = 0x363A3E, MOTOR = 0x2C2E32, BLACK = 0x1C1C1E;
    static final int STRIPE = 0x3CBE50;
    /** Where the stripe runs: this share of the battery's height down from its top. */
    static final double STRIPE_AT = 0.5;

    public static Sheet paint() {
        Sheet s = new Sheet(BikeShape.TEX_W, BikeShape.TEX_H, SEED);
        int[] p = BikeShape.PAINT, t = BikeShape.TIRE, sa = BikeShape.SADDLE, tr = BikeShape.TRUNK, l = BikeShape.LID, m = BikeShape.METAL,
            b = BikeShape.BATTERY, mo = BikeShape.MOTOR, bl = BikeShape.BLACK;
        int[][] size = BikeShape.REGION_SIZE;
        s.region(p[0], p[1], size[0][0], size[0][1], NEUTRAL, 0.05, null);
        s.region(t[0], t[1], size[1][0], size[1][1], TIRE, 0.08, (x, y) -> (x + y) % 3 == 0 ? 0x18181A : -1);
        s.region(sa[0], sa[1], size[2][0], size[2][1], NEUTRAL, 0.08, (x, y) -> y == 1 ? 0xFAFAFA : -1);
        // the trunk: a strap and rivets, in neutral shades
        s.region(tr[0], tr[1], size[3][0], size[3][1], NEUTRAL, 0.05, (x, y) -> y == 5 || y == 6 ? 0x969696 : x % 7 == 3 && (y == 2 || y == 9) ? 0xFFFFFF : -1);
        s.region(l[0], l[1], size[4][0], size[4][1], NEUTRAL, 0.05, (x, y) -> y == 0 ? 0xFFFFFF : -1);
        s.region(m[0], m[1], size[5][0], size[5][1], METAL, 0.05, (x, y) -> y == 0 ? 0xD2D6DA : -1);
        // the battery: a dark case, the stripe round it
        s.region(b[0], b[1], size[6][0], size[6][1], BATTERY, 0.05, null);
        Box bat = BikeShape.battery(BikeShape.model(false));
        s.ring(bat.texU, bat.texV, (float) (bat.hi[0] - bat.lo[0]), (float) (bat.hi[1] - bat.lo[1]), (float) (bat.hi[2] - bat.lo[2]), STRIPE_AT, STRIPE, 0.05);
        // motor and lamp share it: the lamp box (d 1.5) has its face where the motor box (d 4) has an unused corner
        s.region(mo[0], mo[1], size[7][0], size[7][1], MOTOR, 0.06, (x, y) -> x >= 1 && x <= 3 && y >= 1 && y <= 3 ? 0xECE8C4 : -1);
        s.region(bl[0], bl[1], size[8][0], size[8][1], BLACK, 0.06, null);
        return s;
    }

    /** Usage: BikeTexture PNG. */
    public static void main(String[] args) throws java.io.IOException {
        paint().writePng(java.nio.file.Path.of(args[0]));
    }
}
