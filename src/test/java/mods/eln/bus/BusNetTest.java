package mods.eln.bus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The field bus without the game: links, buses, next-tick levels, the last writer, seeding across rebuilds, claims. */
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

    /** Was writesShowNextTickAndHighestWins (the highest of a tick's writes won, Baughn 2026-11-01); now the last. */
    @Test
    void writesShowNextTickAndTheLastWins() {
        BusPort a = port(X), b = port(X);
        BusNet.link(a, b);
        BusNet.tick();
        a.write(X, 0.7);
        b.write(X, 0.3);
        assertEquals(0, a.read(X), "not before the next tick");
        BusNet.tick();
        assertEquals(0.3, a.read(X), 1e-6, "the last written, not the highest");
        assertEquals(2, a.bus().writers(X));
        a.write(X, 0.7);
        b.write(X, 0.3);
        BusNet.tick();
        assertEquals(0.3, a.read(X), 1e-6, "unchanged writes send nothing: still b's");
        assertEquals(0, a.bus().delivered());
        a.write(X, 0.6);
        b.write(X, 0.3);
        BusNet.tick();
        assertEquals(0.6, a.read(X), 1e-6, "a changed: a's");
        BusNet.tick();
        assertEquals(0, a.read(X), "nobody wrote it: 0");
        assertEquals(0, a.bus().writers(X));
    }

    @Test
    void aWriterStoppingHandsTheChannelBack() {
        BusPort a = port(X), b = port(X);
        BusNet.link(a, b);
        BusNet.tick();
        a.write(X, 0.2);
        BusNet.tick();
        a.write(X, 0.2);
        b.write(X, 0.8);
        BusNet.tick();
        assertEquals(0.8, a.read(X), 1e-6);
        a.write(X, 0.2);   // b stops
        BusNet.tick();
        assertEquals(0.8, a.read(X), 1e-6, "b let go this tick; a writes again");
        assertEquals(1, a.bus().writers(X));
        a.write(X, 0.2);
        BusNet.tick();
        assertEquals(0.2, a.read(X), 1e-6, "a's level back");
    }

    @Test
    void levelsAreHeldBetweenWrites() {
        BusPort a = port(X), b = port(X);
        BusNet.link(a, b);
        BusNet.tick();
        for (int t = 0; t < 5; t++) {
            a.write(X, 0.5);
            BusNet.tick();
        }
        assertEquals(0.5, b.read(X), 1e-6);
        assertEquals(1, a.bus().writers(X));
    }

    @Test
    void smallChangesAreNotSent() {
        BusPort a = port(X), b = port(X);
        BusNet.link(a, b);
        BusNet.tick();
        a.write(X, 0.5);
        BusNet.tick();
        a.write(X, 0.5 + BusPort.DEADBAND / 2);
        BusNet.tick();
        assertEquals(0, a.bus().delivered(), "noise");
        a.write(X, 0.5 + 2 * BusPort.DEADBAND);
        BusNet.tick();
        assertEquals(1, a.bus().delivered());
        a.write(X, 1);
        BusNet.tick();
        a.write(X, 1 - BusPort.DEADBAND / 2);
        BusNet.tick();
        assertEquals(1, a.bus().delivered(), "off a rail is always sent");
    }

    @Test
    void aDroppedSignalIsSentAgain() {
        BusPort a = port(X), b = port(X), noisy = new BusPort();
        BusNet.link(a, b);
        BusNet.link(b, noisy);
        BusNet.tick();
        for (int i = 0; i < Bus.CAP; i++) noisy.send(new Packet(40, 0, PacketType.READ, i, 0));
        a.write(X, 0.4);   // sent after them, source 0 too: over the cap
        BusNet.tick();
        assertEquals(1, a.bus().dropped());
        assertEquals(0, b.read(X));
        a.write(X, 0.4);
        BusNet.tick();
        assertEquals(0.4, b.read(X), 1e-6, "written again");
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
        assertEquals(1, a.read(0), 1e-6);
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
        assertEquals(0.5, b.read(X), 1e-6, "no dip to 0 when the bus is rebuilt");
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
        assertEquals(0.6, b.read(X), 1e-6, "no dip to 0 when a block reconnects");
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
