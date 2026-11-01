package mods.eln.bus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The field bus without the game: links, buses, double buffering, highest writer, seeding across rebuilds, the cap. */
class BusNetTest {
    @AfterEach
    void clear() {
        BusNet.clear();
    }

    static BusPort port(String... names) {
        BusPort p = new BusPort();
        p.claim(List.of(names));
        return p;
    }

    @Test
    void linkedPortsShareOneBusAfterATick() {
        BusPort a = port("x"), cable = new BusPort(), b = port("y");
        BusNet.link(a, cable);
        BusNet.link(cable, b);
        assertNull(a.bus(), "no bus until the net is rebuilt");
        BusNet.tick();
        assertNotNull(a.bus());
        assertSame(a.bus(), b.bus());
        assertEquals(java.util.Set.of("x", "y"), a.bus().names());
    }

    @Test
    void writesShowNextTickAndHighestWins() {
        BusPort a = port("x"), b = port("x");
        BusNet.link(a, b);
        BusNet.tick();
        a.write("x", 0.3);
        b.write("x", 0.7);
        assertEquals(0, a.read("x"), "not before the next tick");
        BusNet.tick();
        assertEquals(0.7, a.read("x"), 1e-9);
        assertEquals(2, a.bus().writers("x"));
        BusNet.tick();
        assertEquals(0, a.read("x"), "nobody wrote it: 0");
    }

    @Test
    void unclaimedNamesAreNotCarried() {
        BusPort a = port("x"), b = port();
        BusNet.link(a, b);
        BusNet.tick();
        b.write("nope", 1);
        BusNet.tick();
        assertEquals(0, a.read("nope"));
    }

    @Test
    void aRebuildKeepsValues() {
        BusPort a = port("x"), b = port("x");
        BusNet.link(a, b);
        BusNet.tick();
        a.write("x", 0.5);
        BusNet.tick();
        BusPort c = new BusPort();
        BusNet.link(b, c);   // a cable added nearby
        a.write("x", 0.5);
        BusNet.tick();
        assertEquals(0.5, b.read("x"), 1e-9, "no dip to 0 when the bus is rebuilt");
    }

    @Test
    void detachSplitsTheBus() {
        BusPort a = port("x"), mid = new BusPort(), b = port("x");
        BusNet.link(a, mid);
        BusNet.link(mid, b);
        BusNet.tick();
        BusNet.detach(List.of(mid));
        BusNet.tick();
        assertNull(a.bus(), "a lone port is on no bus");
        a.write("x", 1);
        BusNet.tick();
        assertEquals(0, b.read("x"));
    }

    @Test
    void sixtyFourNamesAtMost() {
        BusPort a = new BusPort(), b = new BusPort();
        BusNet.link(a, b);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < 64; i++) names.add("c" + i);
        a.claim(names);
        BusNet.tick();
        assertTrue(BusNet.room(b, List.of("c3")), "a name already on the bus costs nothing");
        assertFalse(BusNet.room(b, List.of("new")), "the 65th name is refused");
        assertTrue(BusNet.room(a, List.of("new")), "a port's own old names make room");
    }

    @Test
    void namesAreValidated() {
        assertTrue(Bus.validName("tank_level"));
        assertFalse(Bus.validName("Tank"));
        assertFalse(Bus.validName("a.b"));
        assertFalse(Bus.validName(""));
        assertFalse(Bus.validName("abcdefghijklmnopq"));
    }
}
