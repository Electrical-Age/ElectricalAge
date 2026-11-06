package mods.eln.api.docs;

import mods.eln.api.Machine;
import mods.eln.api.lowlevel.PortStatus;
import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.BenchMachine;
import mods.eln.api.testkit.ElnTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** docs/api.md's samples do what the page says. */
public class DocSamplesRunTest {
    @Test
    void theDynamoFeedsItsRatedPowerAndBurnsItsFuel() {
        ElnTestKit.verifySpec(Dynamo.SPEC);
        Dynamo d = new Dynamo();
        d.addFuel(1e6);
        Bench b = ElnTestKit.bench().load(230.0 * 230 / 1500);   // a load that takes 1.5 kW at 230 V
        b.add(Dynamo.SPEC).owner(d::tick);
        b.ticks(60);
        assertEquals(1500, b.loadWatts(), 1500 * 0.02);
        assertTrue(d.fuelJoules() < 1e6 - 50 * 1500 * 0.05, "burnt " + (1e6 - d.fuelJoules()) + " J");
    }

    @Test
    void theGateRunsWhileEnabledAndSaysSoOnItsSignalAndTheBus() {
        ElnTestKit.verifySpec(Gate.SPEC);
        Bench b = ElnTestKit.bench().supply(50, 0.01);
        BenchMachine g = b.add(Gate.SPEC).owner(Gate::tick);
        g.input("enable", 0);
        b.ticks(30);
        assertTrue(g.output("busy").isEmpty(), "disabled: the output floats");
        g.input("enable", 1);
        b.ticks(5);
        assertEquals(1, g.output("busy").getAsDouble());
        assertEquals(1, b.busLevel(Gate.BUSY_CHANNEL));
        g.input("speed", 0.5);
        b.ticks(5);
        double before = g.joulesTaken();
        b.tick();
        assertEquals(0.5 * g.ratedJoulesPerTick(), g.joulesTaken() - before, 1e-9);
    }

    /** On a bench (no world) a burn only latches: the handler is the hosted block's. */
    @Test
    void thePressBurnsForGood() {
        ElnTestKit.verifySpec(Press.SPEC);
        Bench b = ElnTestKit.bench().supply(320, 0.01);
        BenchMachine p = b.add(Press.SPEC).owner(m -> m.takeJoules(m.ratedJoulesPerTick()));
        b.ticks(5);
        assertEquals(mods.eln.api.MachineState.OVERVOLTAGE_TRIPPED, p.state());
        assertTrue(p.lastTrip().orElseThrow().burns());
    }

    @Test
    void theKilnKeepsItsContractAndWarms() {
        ElnTestKit.verifySpec(Kiln.SPEC);
        Bench b = ElnTestKit.bench().supply(230, 0.01);
        BenchMachine k = b.add(Kiln.SPEC).owner(Kiln::tick);
        b.ticks(600);   // 30 s of a 400 s time constant, toward about 24 K over ambient
        assertTrue(k.celsius() > 21, "it warms: " + k.celsius());
    }

    @Test
    void theLoadBankDrawsAndItsGuardOpensItOverTheLimit() {
        LoadBank bank = new LoadBank(null);
        Bench b = ElnTestKit.bench().supply(230, 0.01).attach(bank.circuit).connect(bank.circuit, bank.in);
        b.ticks(3);
        assertEquals(230.0 * 230 / 100, bank.bank.watts(), 529 * 0.01);
        assertEquals("no probe", bank.probeReading());
        assertFalse(bank.guard.tripped());
        b.supplyVolts(300);
        b.ticks(6);
        assertTrue(bank.guard.tripped());
        assertEquals(Double.POSITIVE_INFINITY, bank.bank.ohms());
        LoadBank linked = new LoadBank(net.minecraft.core.BlockPos.ZERO);
        ElnTestKit.bench().attach(linked.circuit).ticks(1);
        assertEquals(PortStatus.NOT_HOSTED.name(), linked.probeReading(), "a bench has no world to probe");
    }

    @Test
    void theTooltipReadsLikeEasMeters() {
        Bench b = ElnTestKit.bench().supply(230, 0.05);
        BenchMachine m = b.add(Mill.SPEC).owner(x -> x.takeJoules(x.ratedJoulesPerTick()));
        b.ticks(40);
        java.util.List<String> lines = Mill.tooltip(m.view());
        assertEquals(2, lines.size());
        assertEquals("running", lines.get(1));
        assertTrue(lines.get(0).endsWith("kW") && lines.get(0).contains("V  "), lines.toString());
        assertFalse(m.view() instanceof Machine, "the view is read-only");
    }
}
