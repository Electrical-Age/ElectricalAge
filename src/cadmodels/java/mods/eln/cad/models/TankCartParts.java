package mods.eln.cad.models;

import org.erisia.cad.Model;

import java.util.List;

import static mods.eln.cad.models.Mc.box;
import static mods.eln.cad.models.Mc.prism;
import static mods.eln.cad.models.Mc.tube;

/**
 * The Tank Minecart's tank and the Tank Loading Station (mods.eln.oil.TankCart, TankStationBlock; Baughn 2026-11-25, todo
 * #611), block models.
 * <ul>
 * <li>tank_cart_load_iron, tank_cart_load_alloy: a horizontal tank along x on two saddles, with dished ends, two bands
 * and a manway on top; the iron tank's dark shell and cast bands, the alloy's steel and its paint. Vanilla's minecart
 * renderer draws it as the cart's display block, as it draws a chest minecart's chest.</li>
 * <li>tank_loading_station, tank_loading_station_on: along north-south (z), a plate under two rails, a riser with a
 * flange on each side across the track (4 px up, centred: TankStationEntity's ports, a tank's low side port), and on
 * the east side a post with a hose arm over the track and its signal lamp: red while the station holds the cart, green
 * while it is powered and lets it go.</li>
 * </ul>
 */
public final class TankCartParts {
    private TankCartParts() {
    }

    static Model load(String shell, String band) {
        Model m = new Model();
        m.add(prism('x', 8, 8, 6, 1, 15, shell, 16, Double.NaN, false, false, Double.NaN));        // the shell
        m.add(prism('x', 8, 8, 4.5, 0, 1, shell, 16, 6, true, false, Double.NaN));                 // its dished ends
        m.add(prism('x', 8, 8, 6, 15, 16, shell, 16, 4.5, false, true, Double.NaN));
        for (double x : new double[]{4.2, 11.2}) m.add(tube('x', 8, 8, 5.9, 6.4, x, x + 0.6, band, 16, Double.NaN, false));
        m.add(prism('y', 8, 8, 1.6, 13.6, 15, "steel", 12));                                      // the manway
        m.add(box(3, 0, 3, 5, 3, 13, "darksteel"));                                               // the saddles
        m.add(box(11, 0, 3, 13, 3, 13, "darksteel"));
        return m;
    }

    static Model station(boolean on) {
        Model m = new Model();
        m.add(box(0.5, 0, 0.5, 15.5, 1, 15.5, "darksteel"));                                      // the plate
        m.add(box(2.25, 1, 0, 3.5, 2, 16, "steel"));                                              // the rails
        m.add(box(12.5, 1, 0, 13.75, 2, 16, "steel"));
        m.add(box(0.75, 1, 5, 2.25, 7, 11, "darksteel"));                                         // the risers under the flanges
        m.add(box(13.75, 1, 5, 15.25, 7, 11, "darksteel"));
        m.add(prism('x', 4, 8, 2.5, 0, 0.75, "steel", 12));                                       // the flanges
        m.add(prism('x', 4, 8, 2.5, 15.25, 16, "steel", 12));
        m.add(box(14.1, 7, 7.3, 14.9, 15, 8.7, "steel"));                                         // the hose arm's post
        m.add(box(7.4, 15, 7.3, 14.9, 16, 8.7, "steel"));                                         // its arm over the track
        m.add(prism('y', 8, 8, 0.6, 13.6, 15, "rubber", 8));                                      // the hose's end
        m.add(box(14.9, 10, 7.5, 15.4, 11.5, 8.5, on ? "paint_green" : "red"));                   // the signal lamp
        return m;
    }

    public static List<BlockModel> blocks() {
        return List.of(new BlockModel("tank_cart_load_iron", load("darksteel", "castiron"), "tankcart", "darksteel"),
            new BlockModel("tank_cart_load_alloy", load("steel", "paint_tankalloy"), "tankcart", "steel"),
            new BlockModel("tank_loading_station", station(false), "tankcart", "steel"),
            new BlockModel("tank_loading_station_on", station(true), "tankcart", "steel"));
    }
}
