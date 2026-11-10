package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A node's heat link through its wall (Node.wallUA, wallT): energy books, no overshoot, slopes. */
class WallTest {
    static final double DT = 0.05;

    /** A sealed vessel of water on a hot wall: its energy rises by exactly the wall's heat, towards the wall's temperature. */
    @Test
    void theWallsHeatIsTheEnergy() {
        Network net = new Network();
        var n = net.add(new Network.Node(0.008, 0, 1, false).fill(5, 1e-3, 300));
        var other = net.add(new Network.Node(0.002, 0, 1, false).fill(0.5, 1e-3, 300));
        net.connect(n, 1, other, 1, 1e-6);
        n.wallUA = 50;
        n.wallT = 470;
        double u0 = n.U + other.U, heat = 0;
        for (int s = 0; s < 4000; s++) {
            net.step(DT);
            heat += n.wallJ;
            assertTrue(n.T() <= 470 + 1e-6, "past the wall at step " + s + ": " + n.T());
        }
        System.out.printf("wall: %.0f J in, energy rose %.0f J, T %.2f K, %.2f bar%n", heat, n.U + other.U - u0, n.T(), n.P() / 1e5);
        assertEquals(heat, n.U + other.U - u0, 1e-9 * Math.abs(u0) + 1e-6 * heat, "energy rose by the wall's heat");
        assertTrue(n.T() > 350, "it heated: " + n.T());
    }

    /** A boiler boiled dry: a little air and vapour on a 1000 K wall, with a huge UA, can't go past the wall in a step. */
    @Test
    void aNearEmptyNodeDoesntOvershoot() {
        Network net = new Network();
        var n = net.add(new Network.Node(0.008, 0, 1, false).fill(0, 1e-4, 300));
        var sink = net.add(new Network.Node(1, 0, 1, true).fill(0, 1.2, 300));
        net.connect(n, 1, sink, 0, 1e-9);
        n.wallUA = 1e4;
        n.wallT = 1000;
        double peak = 0;
        for (int s = 0; s < 200; s++) {
            net.step(DT);
            peak = Math.max(peak, n.T());
        }
        System.out.printf("dry wall: peak %.2f K, now %.2f K, failures %d%n", peak, n.T(), net.stats.failures);
        assertTrue(peak <= 1000 + 1e-6, "past the wall: " + peak);
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    @Test
    void slopesMatchFiniteDifferences() {
        Network net = new Network();
        var n = net.add(new Network.Node(0.008, 0, 1, false).fill(4, 1e-3, 380));
        var other = net.add(new Network.Node(0.002, 0, 1, false).fill(0.5, 1e-3, 300));
        net.connect(n, 1, other, 1, 1e-6);
        n.wallUA = 200;
        n.wallT = 500;
        for (int s = 0; s < 20; s++) {
            String r = net.checkJacobian(DT);
            double worst = Double.parseDouble(r.substring(0, r.indexOf(' ')));
            assertTrue(worst < 1e-3, "step " + s + ": " + r);
            net.step(DT);
        }
    }
}
