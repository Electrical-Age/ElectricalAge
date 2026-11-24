package mods.eln.cad.models;

import org.erisia.cad.Frustum;
import org.erisia.cad.Mesh;
import org.erisia.cad.Solid;
import org.erisia.cad.Sweep;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.OilKit.boltRing;
import static mods.eln.cad.models.OilKit.dial;
import static mods.eln.cad.models.OilKit.handwheel;
import static mods.eln.cad.models.OilKit.signalPlate;

/**
 * The 2x2x2 Still Pot (refinery M2, todo #591): a riveted copper kettle standing on the Large Boiler's skid, a domed
 * head, a swan neck off the dome to the vapour flange, a thermometer and a sight glass on its front. No firebox: it is
 * heated from outside, as the Large Boiler. Drawn from its origin block (front bottom left), full size: 0..32 px on each
 * axis, front XN (x = 0), right z+, as StillPotElement's local frame. Its connections, each at its port's spot:
 * <ul>
 * <li>thermal: one copper heat plate (the Large Boiler's) on the back of the lower right block, its hot copper shoe
 * in to the kettle's foot;</li>
 * <li>vapour out: a steam flange on top of the rear left top block (x 24, z 8; top at y 32), at the end of the swan
 * neck;</li>
 * <li>drain: a flange low on the front of the bottom right block (y 4, z 24: a tank's low flange, the kettle's floor
 * at its bottom edge), its stub into the kettle's foot under a drain cock on the deck;</li>
 * <li>fill and steam: flanges low on the left and right flanks of the rear lower blocks (x 24, y 4), their stubs into
 * the kettle;</li>
 * <li>the temperature signal: a signal plate at the floor of the front of the origin block (z 8);</li>
 * <li>its safety valve on the crown, front right of the swan neck's collar (StillPotElement.RELIEF_PA).</li>
 * </ul>
 * No hitbox: its eight blocks are whole blocks (the node and its ghosts).
 */
public final class StillPot {
    private StillPot() {
    }

    /** The kettle's axis (x, z), radius, its foot (the skid's deck top), the dome's seam and top, the dome's top radius. */
    static final double CX = 16, CZ = 16, R = 11.5, DECK = 4.2, SEAM = 21.0, TOP = 26.5, R_TOP = 5.5;
    static final int N = 20;
    /** The vapour flange (x, z) and the front flanges (y, z). */
    static final double VX = 24, VZ = 8;

    /** The dome's radius at height y. */
    static double domeR(double y) {
        return R + (R_TOP - R) * (y - SEAM) / (TOP - SEAM);
    }

    /** Where the kettle's (or the dome's) skin is at height y, seen from the front, at z (x px). */
    static double frontSkin(double y, double z) {
        double r = y <= SEAM ? R : domeR(y);
        return CX - Math.sqrt(r * r - (z - CZ) * (z - CZ));
    }

    /**
     * A front flange at (y, z) with its reducer and a stub of radius rs into the kettle's skin (0.05 proud of the skid's
     * bed at x 1, as the flank flanges: flush, its inner face z-fought it).
     */
    static List<Mesh> frontFlange(double y, double z, double rs) {
        List<Mesh> m = new ArrayList<>();
        m.add(prism('x', y, z, 2.75, 0.0, 1.05, "steel", 12));
        m.add(prism('x', y, z, 2.0, 1.05, 1.45, "steel", 12, false, true));
        // (turned a twelfth of a half turn: no bolt head level with the deck's top)
        m.addAll(boltRing('x', y, z, 2.35, 1.05, 1.25, 6, "brass", 0.2, Math.PI / 12));
        m.add(prism('x', y, z, rs, 1.45, frontSkin(y, z) + 0.6, "copper", 10, false, false));
        return m;
    }

    /** A flange low on a flank (z 0, or z 32 for the right) of the rear lower block (x 24, y 4), its stub into the kettle. */
    static List<Mesh> flankFlange(boolean right) {
        List<Mesh> m = new ArrayList<>();
        double x = 24, y = 4.0, skin = Math.sqrt(R * R - (x - CX) * (x - CX));
        // (the checker: flush with the skid's bed at 1 and 31, the flange's inner face z-fought it; it stands 0.05 proud)
        double f0 = right ? 30.95 : 0.0, f1 = right ? 32.0 : 1.05, r0 = right ? 30.55 : 1.05, r1 = right ? 30.95 : 1.45;
        m.add(prism('z', x, y, 2.75, f0, f1, "steel", 12));
        m.add(prism('z', x, y, 2.0, r0, r1, "steel", 12, !right, right));
        m.add(prism('z', x, y, 1.2, right ? CZ + skin - 0.6 : r1, right ? r0 : CZ - skin + 0.6, "copper", 10, false, false));
        return m;
    }

    static List<Solid> main() {
        List<Solid> m = new ArrayList<>();
        // the skid: the Large Boiler's cast-iron bed under a darksteel deck
        m.add(box(1.0, 0, 1.0, 31.0, DECK - 0.8, 31.0, "castiron", "nsew"));
        m.add(box(0.6, DECK - 0.8, 0.6, 31.4, DECK, 31.4, "darksteel", "nsewud"));
        // the kettle: a darksteel foot ring, the copper body, riveted seam bands, the dome and its crown
        m.add(prism('y', CX, CZ, R + 0.4, DECK, DECK + 1.7, "darksteel", N));
        m.add(prism('y', CX, CZ, R, DECK, SEAM, "copper", N, false, false));
        for (double y : new double[]{DECK + 5.0, DECK + 10.5, SEAM - 0.4}) m.add(prism('y', CX, CZ, R + 0.15, y, y + 0.4, "darksteel", N, false, false));
        m.add(prism('y', CX, CZ, R, SEAM, TOP, "copper", N, R_TOP, false, false, Double.NaN));
        m.add(prism('y', CX, CZ, R_TOP, TOP, TOP + 0.6, "copper", N, R_TOP - 1.0, false, true, Double.NaN));
        // the heat plates on the skid's back and flanks, each with a hot copper shoe in to the kettle's foot
        // its one heat plate (Baughn 2026-11-24: one is enough), on the back of the lower right block, its shoe in to the
        // kettle's foot
        double hu = 24, side = Math.sqrt(R * R - (hu - CZ) * (hu - CZ));
        m.addAll(LargeBoiler.heatPlate('x', 32, hu, 0));
        m.add(box(CX + side - 1.0, DECK - 0.6, hu - 2.4, 31.6, DECK + 1.2, hu + 2.4, "hotcopper"));
        // the swan neck: a collar on the crown, up and over to the vapour flange on top of the rear left block
        m.add(prism('y', CX, CZ, 2.6, TOP + 0.4, TOP + 1.4, "darksteel", 12));
        double ny = 29.6;
        for (Frustum f : Sweep.along(new double[]{CX, TOP + 0.6, CZ, CX, ny, CZ, VX, ny, VZ, VX, 30.6, VZ}, 1.6, 0.5)) m.add(f.material("copper"));
        m.add(prism('y', VX, VZ, 2.0, 30.0, 31.0, "steel", 12, true, false));
        m.add(prism('y', VX, VZ, 2.75, 31.0, 32.0, "steel", 12));
        m.addAll(boltRing('y', VX, VZ, 2.35, 30.8, 31.0, 6, "brass", 0.2, 0));
        // its safety valve on the crown, front right of the neck's collar: a brass body, a darksteel bonnet and lever
        double sx = CX - 3.5, sz = CZ + 2.5;
        m.add(prism('y', sx, sz, 0.9, TOP + 0.4, TOP + 1.6, "brass", 10));
        m.add(prism('y', sx, sz, 0.55, TOP + 1.6, TOP + 2.5, "darksteel", 8));
        m.add(box(sx - 0.2, TOP + 2.2, sz - 2.0, sx + 0.2, TOP + 2.5, sz - 0.55, "darksteel"));
        // the neck's stay down to the dome
        double stayX = (CX + VX) / 2, stayZ = (CZ + VZ) / 2;
        m.add(prism('y', stayX, stayZ, 0.4, TOP - 1.0, ny - 1.4, "darksteel", 8, false, false));
        // the front: the bottoms drain, low right at the kettle's floor, its cock on the deck over the stub
        m.addAll(frontFlange(4.0, 24.0, 1.2));
        m.add(box(2.4, DECK, 22.8, 4.2, DECK + 1.6, 25.2, "castiron"));
        m.add(prism('y', 3.3, 24.0, 0.25, DECK + 1.6, DECK + 2.3, "darksteel", 6, false, false));
        m.addAll(handwheel(3.3, DECK + 2.4, 24.0, 0.9, 'y'));
        // the fill (left flank) and the steam inlet (right flank), low on the rear blocks, at a tank's low flange height
        m.addAll(flankFlange(false));
        m.addAll(flankFlange(true));
        // the temperature signal: the plate at the floor of the origin's front, its transmitter on the deck, its line up
        m.addAll(signalPlate());
        m.add(box(0.85, 0, 6.5, 1.2, 3.5, 9.5, "steel", "nsu"));
        m.add(box(1.0, DECK, 7.0, 2.8, DECK + 1.6, 10.2, "darksteel"));
        // the thermometer: a dial on a stalk out of the kettle's front, its sensor line down to the transmitter
        double ty = 15.0, tz = 13.0, skin = frontSkin(ty, tz);
        m.add(prism('x', ty, tz, 0.4, skin - 1.6, skin + 0.5, "brass", 8, false, false));
        m.addAll(dial(skin - 2.0, ty, tz, 2.0, "xn", 0.4).subList(0, 2));
        m.add(prism('y', 2.3, 8.6, 0.25, DECK + 1.6, ty - 2.2, "darksteel", 6, false, false));
        m.add(box(2.05, ty - 2.45, 8.35, 2.55, ty - 1.95, tz - 0.6, "darksteel"));
        // the sight glass: a glass tube between two brass cocks on the kettle's front right
        double gz = 19.5, g0 = 8.0, g1 = 18.0, gx = frontSkin(g0, gz) - 1.2;
        for (double y : new double[]{g0 - 0.8, g1}) m.add(box(gx - 0.8, y, gz - 0.8, frontSkin(y, gz) + 0.6, y + 0.8, gz + 0.8, "brass"));
        m.add(prism('y', gx, gz, 0.5, g0, g1, "sightglass", 8, false, false));
        return m;
    }

    public static List<NodeModel> models() {
        NodeModel n = new NodeModel("stillpot", OilKit.PAINT_BOILER);
        n.part("main", main());
        return List.of(n);
    }
}
