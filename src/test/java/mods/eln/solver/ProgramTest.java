package mods.eln.solver;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The signal processor's programs (Baughn 2026-10-30): named components, the output line, cost, state, the rs latch. */
public class ProgramTest {
    static final class In implements ISymbole {
        final String name;
        double v;

        In(String name) {
            this.name = name;
        }

        public double getValue() {
            return v;
        }

        public String getName() {
            return name;
        }
    }

    final In a = new In("A"), b = new In("B"), c = new In("C");

    List<ISymbole> inputs() {
        return new ArrayList<>(List.of(a, b, c));
    }

    @Test
    void anOldExpressionIsStillAProgram() {
        Program p = Program.compile("A*0.5", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0.8;
        assertEquals(0.4, p.getValue(0.05), 1e-12);
        // an old = in the output line is rewritten on load (ProgramUpgrade); today a single = there is no expression
        assertFalse(Program.compile("A=B", inputs()).isValid());
        Program eq = Program.compile(ProgramUpgrade.upgrade("A=B", 1), inputs());
        assertTrue(eq.isValid(), eq.error);
    }

    @Test
    void componentsAreComputedOnceAndPaidForOnce() {
        Program p = Program.compile("x = A*0.5 + 0.1\n\n# a comment\ny = x*2\nx + y + x", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0.8;
        assertEquals(0.5 + 1.0 + 0.5, p.getValue(0.05), 1e-12);
        Program x = Program.compile("A*0.5 + 0.1", inputs()), y = Program.compile("A*2", inputs()), out = Program.compile("A + B + A", inputs());
        assertEquals(x.getOperatorCount() + y.getOperatorCount() + out.getOperatorCount(), p.getOperatorCount(), "each line once");
        // ; separates statements too
        assertEquals(p.getOperatorCount(), Program.compile("x = A*0.5 + 0.1; y = x*2; x + y + x", inputs()).getOperatorCount());
    }

    @Test
    void aStatefulComponentKeepsOneState() {
        // read three times, the integrator still steps once a tick
        Program p = Program.compile("i = integrate(1, 0)\ni + i + i", inputs());
        assertTrue(p.isValid(), p.error);
        double v = 0;
        for (int t = 0; t < 20; t++) v = p.getValue(0.05);
        Program one = Program.compile("integrate(1, 0)", inputs());
        double w = 0;
        for (int t = 0; t < 20; t++) w = one.getValue(0.05);
        assertEquals(3 * w, v, 1e-9);
    }

    @Test
    void stateIsSavedUnderTheComponentsName() {
        Program p = Program.compile("i = integrate(1, 0)\nj = integrate(2, 0)\ni + j", inputs());
        for (int t = 0; t < 20; t++) p.getValue(0.05);
        CompoundTag nbt = new CompoundTag();
        p.writeToNBT(nbt, "equation");
        // another line edited, the integrators keep their state
        Program q = Program.compile("j = integrate(2, 0)\nk = A\ni = integrate(1, 0)\ni + j + k", inputs());
        q.readFromNBT(nbt, "equation");
        a.v = 0;
        assertEquals(p.getValue(0), q.getValue(0), 1e-9);
    }

    @Test
    void definitionsComeFirstAndAreChecked() {
        Program fwd = Program.compile("x = y + 1\ny = A\nx", inputs());
        assertFalse(fwd.isValid());
        assertEquals(1, fwd.errorLine);
        assertTrue(fwd.error.contains("y is not defined above"), fwd.error);
        Program notDef = Program.compile("A + 1\nB", inputs());
        assertFalse(notDef.isValid());
        assertTrue(notDef.error.contains("definition"), notDef.error);
        assertFalse(Program.compile("A = B\nA", inputs()).isValid(), "an input's name");
        assertFalse(Program.compile("sin = A\nsin", inputs()).isValid(), "a function's name");
        assertFalse(Program.compile("x = A\nx = B\nx", inputs()).isValid(), "twice");
        assertFalse(Program.compile("", inputs()).isValid());
        assertNull(Program.compile("x = A\nx", inputs()).error);
    }

    @Test
    void tooBigIsRefused() {
        assertTrue(Program.tooBig("A".repeat(Program.MAX_CHARS + 1)) != null);
        assertTrue(Program.tooBig("A\n".repeat(Program.MAX_LINES)) != null);
        assertNull(Program.tooBig("x = A\nx"));
        assertFalse(Program.compile("A\n".repeat(Program.MAX_LINES + 1), inputs()).isValid());
    }

    @Test
    void theLatchStepsEvenInAnIfsBranchNotTaken() {
        // rs(set, reset): set by B while the if shows 0; then the if shows the latch (it stepped while unread)
        Program p = Program.compile("if(A, rs(B, 0), 0)", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0;
        b.v = 1;
        assertEquals(0, p.getValue(0.05), 0);
        a.v = 1;
        b.v = 0;
        assertEquals(1, p.getValue(0.05), 0, "latched while its branch wasn't taken");
    }

    @Test
    void anInputMayHaveADefault() {
        Program p = Program.compile("A = 0.25 * 2\nB = 1\nA + B", inputs(), java.util.Set.of("A", "B", "C"));
        assertTrue(p.isValid(), p.error);
        assertEquals(0.5, p.defaultOf("A"), 1e-12);
        assertEquals(1, p.defaultOf("B"), 0);
        assertTrue(Double.isNaN(p.defaultOf("C")));
        assertEquals(0, p.getOperatorCount() - 1, "a default costs nothing; A + B one operator");
        Program twice = Program.compile("A = 1\nA = 0\nA", inputs(), java.util.Set.of("A"));
        assertEquals("line 2: A's default is already set", twice.error);
        assertEquals("A", twice.errorToken);
        assertTrue(Program.compile("A = B\nA", inputs(), java.util.Set.of("A")).error.contains("must be a number"));
        // without defaults allowed, A is an input already
        assertTrue(Program.compile("A = 1\nA", inputs()).error.contains("already defined"));
    }

    @Test
    void aCommentMayFollowCode() {
        // AdventurAgent 2026-10-30: "half = sum / 2  # mean" was read as code
        Program p = Program.compile("sum = A + B  # both\nhalf = sum / 2  # mean\nhalf # the output; # and more", inputs());
        assertTrue(p.isValid(), p.error);
        a.v = 0.6;
        b.v = 0.2;
        assertEquals(0.4, p.getValue(0.05), 1e-12);
        assertEquals(2, p.getOperatorCount());
        // ; before a comment still separates; after it, it is comment
        assertTrue(Program.compile("x = A; x * 2 # x; y", inputs()).isValid());
    }

    /** A bus with fixed channel yellows, and what a program wrote to it. */
    final java.util.Map<String, Double> bus = new java.util.HashMap<>(), written = new java.util.LinkedHashMap<>();

    Program onBus(String source) {
        return Program.compile(source, inputs(), java.util.Set.of("A", "B", "C"), c -> bus.getOrDefault(mods.eln.bus.BusChannel.name(c), 0.0));
    }

    void wrote(int c, double v) {
        written.put(mods.eln.bus.BusChannel.name(c), v);
    }

    static int mask(String... names) {
        int m = 0;
        for (String n : names) m |= 1 << mods.eln.bus.BusChannel.byName(n);
        return m;
    }

    @Test
    void busChannelsAreReadAnywhereANameMayStand() {
        bus.put("cyan", 0.2);
        bus.put("blue", 0.5);
        Program p = onBus("low = bus.cyan < 0.3\nlow * bus.blue + bus.black");
        assertTrue(p.isValid(), p.error);
        assertTrue(p.hasOutput());
        assertEquals(0.5, p.getValue(0.05), 1e-12);
        assertEquals(mask("cyan", "blue", "black"), p.busChannels());
        assertEquals(3, p.getOperatorCount(), "a bus read costs nothing");
        // without a bus (the editor's check), the same program compiles; its channels read 0
        Program client = Program.compile("low = bus.cyan < 0.3\nlow * bus.blue + bus.black", inputs());
        assertTrue(client.isValid(), client.error);
        assertEquals(0, client.getValue(0.05), 0);
    }

    @Test
    void aBusDefinitionWritesItsChannel() {
        bus.put("yellow", 0.8);
        Program p = onBus("bus.orange = bus.yellow / 2\nbus.red = A > 0.5\nA");
        assertTrue(p.isValid(), p.error);
        a.v = 0.9;
        assertEquals(0.9, p.getValue(0.05), 1e-12);
        p.writeBus(this::wrote);
        assertEquals(java.util.Map.of("orange", 0.4, "red", 1.0), written);
        assertEquals(mask("orange", "yellow", "red"), p.busChannels());
        assertEquals(2, p.getOperatorCount(), "a write's operators are paid for");
    }

    @Test
    void aProgramMayEndWithABusDefinition() {
        Program p = onBus("x = A * 2\nbus.green = x");
        assertTrue(p.isValid(), p.error);
        assertFalse(p.hasOutput());
        a.v = 0.3;
        assertEquals(0, p.getValue(0.05), 0, "no output expression: the wire gives 0");
        p.writeBus(this::wrote);
        assertEquals(0.6, written.get("green"), 1e-12);
        // only a bus definition is a definition on the last line
        assertFalse(onBus("x = A").isValid());
        assertTrue(onBus("bus.white = 1").isValid());
    }

    @Test
    void readingAChannelThisProgramWritesReadsTheBus() {
        // bus.lime is the bus's level as the tick began: a counter with a tick's delay, no cycle
        bus.put("lime", 0.25);
        Program p = onBus("bus.lime = bus.lime + 0.1\nbus.lime");
        assertTrue(p.isValid(), p.error);
        assertEquals(0.25, p.getValue(0.05), 1e-12);
        p.writeBus(this::wrote);
        assertEquals(0.35, written.get("lime"), 1e-12);
    }

    @Test
    void onlyTheSixteenDyeNamesAreChannels() {
        Program teal = onBus("bus.teal * 2");
        assertFalse(teal.isValid());
        assertEquals("bus.teal", teal.errorToken);
        assertTrue(teal.error.contains("is not a bus channel"), teal.error);
        for (String n : new String[]{"white", "light_blue", "light_gray", "black"}) assertTrue(teal.error.contains(n), "it names the choices: " + teal.error);
        assertFalse(teal.error.contains("("), "no parenthesis: " + teal.error);
        assertFalse(onBus("bus.Red * 2").isValid(), "as Minecraft writes them, lower case");
        assertFalse(onBus("bus.silver = 1").isValid(), "Minecraft's name is light_gray");
        assertFalse(onBus("bus.3 = 1").isValid(), "not by number");
        assertFalse(onBus("bus. = 1").isValid());
        for (int c = 0; c < 16; c++) assertTrue(onBus("bus." + mods.eln.bus.BusChannel.name(c) + " = 1").isValid(), "channel " + c);
        Program twice = onBus("bus.red = A\nbus.red = B\n0");
        assertEquals("line 2: bus.red is already written above", twice.error);
        assertEquals(0, twice.busChannels(), "a program that fails claims nothing");
        assertTrue(onBus("x.y + 1").error.contains("x.y is not defined above"), "other dotted names are unknown");
    }

    @Test
    void aBusWritesStateIsSavedUnderItsName() {
        Program p = onBus("bus.pink = integrate(1, 0)\nbus.pink");
        for (int t = 0; t < 20; t++) p.getValue(0.05);
        CompoundTag nbt = new CompoundTag();
        p.writeToNBT(nbt, "equation");
        Program q = onBus("x = A\nbus.pink = integrate(1, 0)\nx");
        q.readFromNBT(nbt, "equation");
        q.getValue(0);
        p.getValue(0);
        java.util.Map<String, Double> w = new java.util.HashMap<>();
        q.writeBus((c, v) -> w.put(mods.eln.bus.BusChannel.name(c), v));
        p.writeBus(this::wrote);
        assertEquals(written.get("pink"), w.get("pink"), 1e-9);
        assertEquals(1.0, w.get("pink"), 1e-9);
    }
}
