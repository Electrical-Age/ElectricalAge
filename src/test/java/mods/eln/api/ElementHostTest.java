package mods.eln.api;

import mods.eln.api.impl.ElementHost;
import mods.eln.api.impl.ElementSite;
import mods.eln.api.impl.Faces;
import mods.eln.api.impl.MachineCore;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The element host without a game: faces against every front, its parts in an element's lists, the game's tick
 * order in a bare Simulator (slow pre-processes, the step, slow processes), NBT and the burn path.
 */
public class ElementHostTest {
    static final MachineSpec MACERATOR = MachineSpec.consumer(Tier.V50).ratedWatts(200).onBrownout(Brownout.SLOW)
        .burnsOnOvervoltage().power(RelativeFace.LEFT, RelativeFace.RIGHT, RelativeFace.UP, RelativeFace.DOWN).build();

    /** An element's lists and a stub site. */
    static final class Element implements ElementSite {
        Direction front = Direction.XN;
        boolean burnt;
        final Set<Direction> connected = EnumSet.noneOf(Direction.class);
        final List<TripCause> burns = new ArrayList<>();
        final ArrayList<State> loads = new ArrayList<>();
        final ArrayList<Component> components = new ArrayList<>();
        final ArrayList<IProcess> slow = new ArrayList<>(), slowPre = new ArrayList<>();

        ElementHost machine(MachineSpec spec) {
            return new ElementHost(spec, this, loads, components, slow, slowPre);
        }

        public Direction front() {
            return front;
        }

        public boolean connected(Direction side) {
            return connected.contains(side);
        }

        public void checkThread() {
        }

        public boolean burnt() {
            return burnt;
        }

        public void burn(TripCause cause, double severity) {
            burns.add(cause);
        }

        public double powerRs() {
            return 0.01;
        }

        public double signalRs() {
            return 0.1;
        }

        void joinTo(Simulator sim) {
            for (State l : loads) sim.addElectricalLoad(l);
            sim.addAllElectricalComponent(components);
            for (IProcess p : slowPre) sim.addSlowPreProcess(p);
            sim.addAllSlowProcess(slow);
        }
    }

    @Test
    void relativeFacesAreOneToOneForEveryFront() {
        for (Direction front : Direction.values()) {
            Set<Direction> seen = EnumSet.noneOf(Direction.class);
            for (RelativeFace f : RelativeFace.values()) {
                Direction d = Faces.resolve(f, front);
                assertTrue(seen.add(d), front + ": " + f + " lands on " + d + " twice");
                assertEquals(f, Faces.relative(d, front));
            }
            assertEquals(front, Faces.resolve(RelativeFace.FRONT, front));
            assertEquals(front.getInverse(), Faces.resolve(RelativeFace.BACK, front));
        }
    }

    @Test
    void masksAndLoadsFollowTheFront() {
        Element e = new Element();
        MachineSpec spec = MachineSpec.consumer(Tier.V50).ratedWatts(100).power(RelativeFace.LEFT, RelativeFace.RIGHT)
            .signalIn("run", RelativeFace.FRONT, 1).signalOut("done", RelativeFace.BACK).build();
        ElementHost m = e.machine(spec);
        for (Direction front : Direction.values()) {
            e.front = front;
            assertEquals(NodeBase.maskElectricalPower, m.connectionMask(front.left()));
            assertEquals(NodeBase.maskElectricalPower, m.connectionMask(front.right()));
            assertEquals(NodeBase.maskElectricalGate, m.connectionMask(front));
            assertEquals(NodeBase.maskElectricalGate, m.connectionMask(front.back()));
            assertEquals(0, m.connectionMask(front.up()));
            assertEquals(0, m.connectionMask(front.down()));
            assertSame(m.powerLoad(), m.electricalLoad(front.left()));
            assertSame(m.powerLoad(), m.electricalLoad(front.right()));
            ElectricalLoad in = m.electricalLoad(front), out = m.electricalLoad(front.back());
            assertTrue(in != null && out != null && in != out && in != m.powerLoad());
            assertNull(m.electricalLoad(front.up()));
            assertEquals(RelativeFace.LEFT, m.relativeFace(front.left()).orElseThrow());
        }
        e.front = null; // not placed yet
        assertEquals(0, m.connectionMask(Direction.XN));
        assertTrue(m.relativeFace(Direction.XN).isEmpty());
        // an input reads its default until something is connected on its face
        e.front = Direction.ZP;
        assertEquals(1, m.signal("run"));
        e.connected.add(Direction.ZP);
        assertEquals(0, m.signal("run"));
        assertThrows(IllegalArgumentException.class, () -> m.signal("done"));
        assertTrue(m.outputLevel("done").isEmpty(), "outputs start floating");
        m.setSignal("done", 0.5);
        assertEquals(0.5, m.outputLevel("done").orElseThrow(), 1e-12);
        m.releaseSignal("done");
        assertTrue(m.outputLevel("done").isEmpty());
    }

    @Test
    void partsGoInTheElementsLists() {
        Element e = new Element();
        IProcess work = t -> {
        };
        e.slow.add(work);
        ElementHost m = e.machine(MACERATOR);
        assertSame(m.afterStepProcess(), e.slow.get(0), "books first, before the element's own work");
        assertSame(work, e.slow.get(1));
        assertEquals(List.of(m.beforeStepProcess()), e.slowPre);
        assertTrue(e.loads.contains(m.powerLoad()));
        assertFalse(e.components.isEmpty());
        String msg = assertThrows(IllegalStateException.class, () -> e.machine(MACERATOR)).getMessage();
        assertTrue(msg.contains("one machine per element"), msg);
    }

    /**
     * In a simulator, in the game's order: the owner's takes in its slow process get what the step just delivered,
     * a tick after asking; at 50 V it gets its rated 10 J a tick, on a sagging line about rated x (U/Unom)^2.
     */
    @Test
    void runsInTheSimulatorsTickOrder() {
        Simulator sim = new Simulator(0.05, 0.05, 50, 1 / 400.0);
        sim.init();
        Element e = new Element();
        double[] got = {0};
        int[] ticks = {0};
        e.slow.add(t -> {
            ticks[0]++;
        });
        ElementHost m = e.machine(MACERATOR);
        e.slow.add(t -> got[0] += m.takeJoules(m.ratedJoulesPerTick()));
        e.joinTo(sim);
        ElectricalLoad emf = new ElectricalLoad();
        sim.addElectricalLoad(emf);
        VoltageSource supply = new VoltageSource("supply", emf, null).setU(50);
        Resistor line = new Resistor(emf, m.powerLoad()).setR(0.01);
        sim.addElectricalComponent(supply);
        sim.addElectricalComponent(line);
        for (int i = 0; i < 40; i++) sim.tick();
        assertEquals(MachineState.RUNNING, m.state());
        got[0] = 0;
        for (int i = 0; i < 100; i++) sim.tick();
        assertEquals(100 * 10, got[0], 100 * 10 * 0.01, "rated 200 W at 50 V");
        // a weak line: the machine sags it and runs slower (Brownout.SLOW), the draw that of the old resistor
        line.setR(12.5); // as much as the machine's own 12.5 ohm: half the volts
        for (int i = 0; i < 60; i++) sim.tick();
        got[0] = 0;
        for (int i = 0; i < 100; i++) sim.tick();
        double r = m.volts() / 50;
        assertEquals(0.5, r, 0.05);
        assertEquals(100 * 10 * r * r, got[0], 100 * 10 * r * r * 0.05);
        MachineCore c = m.core();
        assertEquals(c.joulesIn(), c.joulesOut() + c.bufferJoules() + c.joulesLost(), 1e-9 * c.joulesIn());
    }

    /** Saved under api.machine.*, taken back at the first tick (after the element's own ratings); written through until then. */
    @Test
    void nbtRoundTrips() {
        Element e = new Element();
        ElementHost m = e.machine(MACERATOR);
        MachineCore c = m.core();
        double[] ohms = {Double.POSITIVE_INFINITY};
        for (int i = 0; i < 12; i++) {
            c.afterStep(50, Double.isInfinite(ohms[0]) ? 0 : 2500 / ohms[0] * 0.05);
            c.takeJoules(2);
            c.beforeStep();
            ohms[0] = c.loadOhms();
        }
        c.afterStep(50, 2500 / ohms[0] * 0.05);
        assertTrue(c.bufferJoules() > 0);
        CompoundTag a = new CompoundTag();
        INBT.write(e, a);
        for (String k : a.keySet()) assertTrue(k.startsWith("api.machine."), k);
        assertEquals(c.bufferJoules(), a.getDoubleOr("api.machine.bufferJ", -1), 0);

        Element f = new Element();
        ElementHost n = f.machine(MACERATOR);
        INBT.read(f, a);
        CompoundTag b = new CompoundTag();
        INBT.write(f, b);
        assertEquals(a, b, "written through before the first tick");
        f.slowPre.get(0).process(0.05); // the first tick
        assertEquals(c.bufferJoules(), n.bufferJoules(), 0);
        CompoundTag d = new CompoundTag();
        INBT.write(f, d);
        assertEquals(a, d);
    }

    /** The element's failure path burns it; while the element is burnt the core stays so, and it comes back unburnt. */
    @Test
    void burnGoesThroughTheElement() {
        Element e = new Element();
        ElementHost m = e.machine(MACERATOR);
        m.core().afterStep(70, 0);
        assertEquals(1, e.burns.size());
        assertTrue(e.burns.get(0).burns());
        e.burnt = true;
        assertTrue(m.core().burnt());
        e.burnt = false; // repaired (or the failure was called off)
        e.slow.get(0).process(0.05);
        assertFalse(m.core().burnt());
        assertEquals(MachineState.UNPOWERED, m.state());
    }

    /** The element's NBT pass: every INBTTReady entry of its lists. */
    static final class INBT {
        static void write(Element e, CompoundTag t) {
            mods.eln.misc.INBTTReady.writeAll(t, e.loads, e.components, e.slow, e.slowPre);
        }

        static void read(Element e, CompoundTag t) {
            mods.eln.misc.INBTTReady.readAll(t, e.loads, e.components, e.slow, e.slowPre);
        }
    }
}
