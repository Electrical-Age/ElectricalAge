package mods.eln.bus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Bus packets without the game: wire form, next-tick delivery, order, the cap, who gets what, rebuilds. */
class PacketTest {
    @AfterEach
    void clear() {
        BusNet.clear();
    }

    static final int X = 3;

    static BusPort device(int address, int... channels) {
        BusPort p = BusNetTest.port(channels);
        p.setAddress(address);
        p.listen(true);
        return p;
    }

    static List<Packet> drain(BusPort p) {
        List<Packet> l = new ArrayList<>();
        for (Packet k; (k = p.poll()) != null; ) l.add(k);
        return l;
    }

    @Test
    void wireFormRoundTripsEveryType() {
        for (PacketType t : PacketType.values()) {
            Packet p = new Packet(40, 17, t, 7, 0.25f);
            byte[] raw = p.encode();
            assertEquals(3 + t.payloadLength(), raw.length, t.label());
            assertEquals(p, Packet.decode(raw), t.label());
        }
        assertEquals(5, PacketType.WRITE.payloadLength());
        assertEquals(1, PacketType.PRESS.payloadLength());
    }

    @Test
    void decodeDropsUnknownTypesAndWrongLengths() {
        assertNull(Packet.decode(new byte[]{0, 17, 99, 0}), "unknown type");
        assertNull(Packet.decode(new byte[]{0, 17, 2}), "press without its index");
        assertNull(Packet.decode(new byte[]{0, 17, 2, 1, 1}), "press with a byte too many");
        assertNull(Packet.decode(new byte[]{0, 17}));
        assertNull(Packet.decode(null));
    }

    @Test
    void fieldsAreBytesAndLevelsAreShares() {
        Packet p = new Packet(256 + 20, -1, PacketType.POWER, 9, 3f);
        assertEquals(20, p.target());
        assertEquals(255, p.source());
        assertEquals(0, p.index(), "power has no index");
        assertEquals(1f, p.value(), "a level is 0..1");
        assertEquals(0f, new Packet(20, 17, PacketType.WRITE, 1, Float.NaN).value());
        assertEquals(-4f, new Packet(20, 17, PacketType.WRITE, 1, -4f).value(), "a register takes any value");
        assertEquals(X, Packet.signal(17, X, 0.5).channel());
        assertEquals(BusChannel.UNUSED, new Packet(17, 18, PacketType.POWER, 0, 1).channel());
    }

    @Test
    void addressesAreDevicesOnly() {
        BusPort p = new BusPort();
        for (int a : new int[]{0, 1, 16, 256, -3}) {
            p.setAddress(a);
            assertEquals(0, p.address(), "" + a);
        }
        p.setAddress(17);
        assertEquals(17, p.address());
        p.setAddress(255);
        assertEquals(255, p.address());
    }

    @Test
    void deliveredNextTickToWhomItIsFor() {
        BusPort a = device(17), b = device(18, X), c = device(19), cable = new BusPort(), mute = BusNetTest.port(X);
        BusNet.link(a, cable);
        BusNet.link(cable, b);
        BusNet.link(cable, c);
        BusNet.link(cable, mute);
        BusNet.tick();
        assertTrue(a.send(new Packet(19, 17, PacketType.POWER, 0, 1)));
        assertTrue(a.send(Packet.signal(17, X, 0.5)));
        assertTrue(a.send(new Packet(Packet.BROADCAST, 17, PacketType.PRESS, 0, 0)));
        assertEquals(0, c.pending(), "not before the next tick");
        BusNet.tick();
        assertEquals(3, a.bus().delivered());
        assertEquals(List.of(PacketType.POWER, PacketType.PRESS), drain(c).stream().map(Packet::type).toList(),
            "its address's and the broadcast");
        assertEquals(List.of(PacketType.SIGNAL, PacketType.PRESS), drain(b).stream().map(Packet::type).toList(),
            "its channel's and the broadcast");
        assertEquals(0, a.pending(), "not its own");
        assertEquals(0, mute.pending(), "a port that doesn't listen gets none");
        assertEquals(0, cable.pending());
        BusNet.tick();
        assertEquals(0, a.bus().delivered(), "once");
        assertEquals(0, c.pending());
    }

    @Test
    void orderedBySourceThenSending() {
        BusPort a = device(40), b = device(20), rx = device(30);
        BusNet.link(a, b);
        BusNet.link(b, rx);
        BusNet.tick();
        a.send(new Packet(30, 40, PacketType.READ, 1, 0));
        b.send(new Packet(30, 20, PacketType.READ, 2, 0));
        a.send(new Packet(30, 40, PacketType.READ, 3, 0));
        b.send(new Packet(30, 20, PacketType.READ, 4, 0));
        a.send(new Packet(30, 5, PacketType.READ, 5, 0));   // a faked source
        BusNet.tick();
        assertEquals(List.of(5, 2, 4, 1, 3), drain(rx).stream().map(Packet::index).toList());
    }

    @Test
    void theCapDropsTheRestAndCountsThem() {
        BusPort a = device(17), rx = device(18);
        BusNet.link(a, rx);
        BusNet.tick();
        for (int i = 0; i < Bus.CAP + 6; i++) a.send(new Packet(18, 17, PacketType.READ, i, 0));
        BusNet.tick();
        assertEquals(Bus.CAP, a.bus().delivered());
        assertEquals(6, a.bus().dropped());
        List<Packet> got = drain(rx);
        assertEquals(Bus.CAP, got.size());
        assertEquals(Bus.CAP - 1, got.get(got.size() - 1).index(), "the first ones sent");
        BusNet.tick();
        assertEquals(0, a.bus().dropped());
        assertEquals(6, a.bus().droppedTotal(), "kept for Jade");
    }

    @Test
    void aFullInboxDropsTheOldest() {
        BusPort a = device(17), rx = device(18);
        BusNet.link(a, rx);
        BusNet.tick();
        for (int t = 0; t < 2; t++) {
            for (int i = 0; i < 40; i++) a.send(new Packet(18, 17, PacketType.READ, t * 40 + i, 0));
            BusNet.tick();
        }
        assertEquals(BusPort.INBOX, rx.pending());
        assertEquals(80 - BusPort.INBOX, rx.overflow());
        assertEquals(80 - BusPort.INBOX, rx.poll().index());
    }

    @Test
    void aRebuildKeepsQueuedPackets() {
        BusPort a = device(17), rx = device(18);
        BusNet.link(a, rx);
        BusNet.tick();
        a.send(new Packet(18, 17, PacketType.PRESS, 1, 0));
        BusNet.link(rx, new BusPort());   // a cable placed nearby
        BusNet.tick();
        assertEquals(1, rx.pending(), "the rebuilt bus delivers it");
    }

    @Test
    void aSenderCutOffLosesItsPackets() {
        BusPort a = device(17), mid = new BusPort(), rx = device(18);
        BusNet.link(a, mid);
        BusNet.link(mid, rx);
        BusNet.tick();
        a.send(new Packet(18, 17, PacketType.PRESS, 1, 0));
        BusNet.detach(List.of(a));
        BusNet.tick();
        assertEquals(0, rx.pending());
        assertFalse(a.send(new Packet(18, 17, PacketType.PRESS, 1, 0)), "on no bus");
    }

    @Test
    void stoppingListeningEmptiesTheInbox() {
        BusPort a = device(17), rx = device(18);
        BusNet.link(a, rx);
        BusNet.tick();
        a.send(new Packet(18, 17, PacketType.PRESS, 1, 0));
        BusNet.tick();
        rx.listen(false);
        assertNull(rx.poll());
        assertFalse(rx.listening());
    }

    @Test
    void theBusListsAddressesAndDuplicates() {
        BusPort a = device(20), b = device(17), c = device(20), cable = new BusPort();
        BusNet.link(a, cable);
        BusNet.link(cable, b);
        BusNet.link(cable, c);
        BusNet.tick();
        assertEquals(java.util.Map.of(17, 1, 20, 2), a.bus().addresses());
        assertEquals(List.of(17, 20), List.copyOf(a.bus().addresses().keySet()), "in order");
    }
}
