package mods.eln.api.impl;

import io.netty.buffer.Unpooled;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.Tier;
import mods.eln.api.TripCause;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The client's machine: a synced snapshot read back whole; before any, UNPOWERED and zeros; writes throw. */
public class ClientMachineTest {
    static final MachineSpec SPEC = MachineSpec.consumer(50).ratedWatts(100).build();

    @Test
    void showsWhatTheServerSynced() {
        MachineCore core = new MachineCore(SPEC);
        for (int i = 0; i < 30; i++) {
            core.beforeStep();
            core.afterStep(50, i > 12 ? 5 : 0);
        }
        TripCause trip = new TripCause(MachineState.OVERVOLTAGE_TRIPPED, 70, 65, false);
        MachineData.Snapshot s = MachineData.Snapshot.of(core);
        s = new MachineData.Snapshot(s.state(), s.volts(), s.voltageRatio(), s.amps(), s.watts(), s.bufferJoules(),
            s.bufferCapacityJoules(), s.celsius(), trip);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        s.write(buf);
        MachineData d = new MachineData();
        d.snapshot = MachineData.Snapshot.read(buf);
        ClientMachine m = new ClientMachine(SPEC, d);
        assertEquals(core.state(), m.state());
        assertEquals(core.volts(), m.volts());
        assertEquals(core.voltageRatio(), m.voltageRatio());
        assertEquals(core.watts(), m.watts());
        assertEquals(core.amps(), m.amps());
        assertEquals(core.bufferJoules(), m.bufferJoules());
        assertEquals(core.bufferCapacityJoules(), m.bufferCapacityJoules());
        assertEquals(core.celsius(), m.celsius());
        assertEquals(trip, m.lastTrip().orElseThrow());
        assertEquals(5, m.ratedJoulesPerTick(), 1e-12);
        assertEquals(m.state(), m.view().state());
    }

    @Test
    void beforeAnySyncItIsUnpoweredAndWritesThrow() {
        ClientMachine m = new ClientMachine(SPEC, new MachineData());
        assertEquals(MachineState.UNPOWERED, m.state());
        assertEquals(0, m.volts());
        assertThrows(IllegalStateException.class, () -> m.takeJoules(1));
        assertThrows(IllegalStateException.class, () -> m.tryTakeJoules(1));
        assertThrows(IllegalStateException.class, () -> m.signal("x"));
        assertThrows(IllegalStateException.class, () -> m.setSignal("x", 1));
        assertThrows(IllegalStateException.class, m::bus);
        assertThrows(IllegalStateException.class, () -> m.respec(SPEC));
    }

    /** The server syncs again only when the view moved: a state change at once, numbers past 1%. */
    @Test
    void nearIgnoresNoise() {
        var a = new MachineData.Snapshot(MachineState.RUNNING, 50, 1, 2, 100, 5, 10, 20, null);
        assertEquals(true, a.near(new MachineData.Snapshot(MachineState.RUNNING, 50.2, 1.004, 2, 100.5, 5.004, 10, 20, null)));
        assertEquals(false, a.near(new MachineData.Snapshot(MachineState.RUNNING, 48, 0.96, 2, 100, 5, 10, 20, null)));
        assertEquals(false, a.near(new MachineData.Snapshot(MachineState.UNDERVOLTAGE, 50, 1, 2, 100, 5, 10, 20, null)));
    }
}
