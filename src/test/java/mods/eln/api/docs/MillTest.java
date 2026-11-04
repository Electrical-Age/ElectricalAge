package mods.eln.api.docs;

import mods.eln.api.MachineState;
import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.BenchMachine;
import mods.eln.api.testkit.ElnTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** docs/api.md's testing section: the mill in JUnit, no world. */
public class MillTest {
    // doc:begin verify
    @Test
    void millSpecKeepsTheContract() {
        ElnTestKit.verifySpec(Mill.SPEC);   // throws an AssertionError with a readable report if not
    }
    // doc:end verify

    // doc:begin bench
    @Test
    void millGrindsAboutThreeItemsInFiveSecondsAtNominal() {
        Bench bench = ElnTestKit.bench().supply(200, 0.05);   // 200 V behind 0.05 ohm
        Mill.Work work = new Mill.Work();
        int[] items = {0};
        BenchMachine mill = bench.add(Mill.SPEC).owner(m -> {
            if (work.tick(m, true)) items[0]++;
        });
        bench.ticks(100);                                     // 5 s, in the game's tick order
        assertEquals(MachineState.RUNNING, mill.state());
        assertTrue(items[0] >= 2 && items[0] <= 3, bench.trace()); // 99 J a tick from the second ask on
    }
    // doc:end bench

    @Test
    void aMillOnAHalfVoltageLineStops() {
        Bench bench = ElnTestKit.bench().supply(100, 0.05);
        Mill.Work work = new Mill.Work();
        int[] items = {0};
        BenchMachine mill = bench.add(Mill.SPEC).owner(m -> {
            if (work.tick(m, true)) items[0]++;
        });
        bench.ticks(100);
        assertEquals(MachineState.UNDERVOLTAGE, mill.state());
        assertEquals(0, items[0]);
        assertEquals("undervoltage", Mill.tooltip(mill.view()).get(1));
    }
}
