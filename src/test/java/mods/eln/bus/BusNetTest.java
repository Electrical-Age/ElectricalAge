package mods.eln.bus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The field bus without the game: links, buses, double buffering, highest writer, seeding across rebuilds, claims. */
class BusNetTest {
    @AfterEach
    void clear() {
        BusNet.clear();
    }

    static final int X = 3, Y = 14;

    /** A port claiming these channels. */
    static BusPort port(int... channels) {
        BusPort p = new BusPort();
        int m = 0;
        for (int c : channels) m |= 1 << c;
        p.claim(m);
        return p;
    }

    @Test
    void linkedPortsShareOneBusAfterATick() {
        BusPort a = port(X), cable = new BusPort(), b = port(Y);
        BusNet.link(a, cable);
        BusNet.link(cable, b);
        assertNull(a.bus(), "no bus until the net is rebuilt");
        BusNet.tick();
        assertNotNull(a.bus());
        assertSame(a.bus(), b.bus());
        assertTrue(a.bus().inUse(X) && a.bus().inUse(Y), "the channels its ports claim are in use");
        assertFalse(a.bus().inUse(0));
    }

    @Test
    void writesShowNextTickAndHighestWins() {
        BusPort a = port(X), b = port(X);
        BusNet.link(a, b);
        BusNet.tick();
        a.write(X, 0.3);
        b.write(X, 0.7);
        assertEquals(0, a.read(X), "not before the next tick");
        BusNet.tick();
        assertEquals(0.7, a.read(X), 1e-9);
        assertEquals(2, a.bus().writers(X));
        BusNet.tick();
        assertEquals(0, a.read(X), "nobody wrote it: 0");
    }

    @Test
    void everyChannelIsCarriedClaimedOrNot() {
        BusPort a = port(X), b = port();
        BusNet.link(a, b);
        BusNet.tick();
        b.write(0, 1);
        b.write(16, 1);   // no such channel: ignored
        b.write(-1, 1);
        BusNet.tick();
        assertEquals(1, a.read(0), 1e-9);
        assertTrue(a.bus().inUse(0), "written last tick: in use");
        assertEquals(0, a.read(16));
        assertEquals(0, a.read(-1));
        BusNet.tick();
        assertFalse(a.bus().inUse(0), "neither claimed nor written");
    }

    @Test
    void aRebuildKeepsValues() {
        BusPort a = port(X), b = port(X);
        BusNet.link(a, b);
        BusNet.tick();
        a.write(X, 0.5);
        BusNet.tick();
        BusPort c = new BusPort();
        BusNet.link(b, c);   // a cable added nearby
        a.write(X, 0.5);
        BusNet.tick();
        assertEquals(0.5, b.read(X), 1e-9, "no dip to 0 when the bus is rebuilt");
    }

    @Test
    void aNodeReconnectingKeepsValues() {
        BusPort a = port(X), cable = new BusPort(), b = port(X);
        BusNet.link(a, cable);
        BusNet.link(cable, b);
        BusNet.tick();
        a.write(X, 0.6);
        BusNet.tick();
        // the whole bus is one block that reconnects: every port detached, a new cable port, links again
        BusNet.detach(List.of(a, cable, b));
        BusPort cable2 = new BusPort();
        BusNet.link(a, cable2);
        BusNet.link(cable2, b);
        a.write(X, 0.6);
        BusNet.tick();
        assertEquals(0.6, b.read(X), 1e-9, "no dip to 0 when a block reconnects");
    }

    @Test
    void detachSplitsTheBus() {
        BusPort a = port(X), mid = new BusPort(), b = port(X);
        BusNet.link(a, mid);
        BusNet.link(mid, b);
        BusNet.tick();
        BusNet.detach(List.of(mid));
        BusNet.tick();
        assertNull(a.bus(), "a lone port is on no bus");
        a.write(X, 1);
        BusNet.tick();
        assertEquals(0, b.read(X));
    }
}
