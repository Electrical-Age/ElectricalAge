package mods.eln.api.impl.testkit;

import mods.eln.api.Brownout;
import mods.eln.api.MachineSpec;
import mods.eln.api.Tier;
import mods.eln.api.testkit.ElnTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Brownout.SLOW consumer's line is read from the solver (MnaMachine.theveninOhms, Baughn 2026-11-04: no guessing):
 * on a 200 V supply behind 0.01 ohm and a line of 8 ohm it sees 8.01 ohm and the line's maximum power E^2 / 4R, at
 * once and whatever its own draw; with a resistor beside it, the Thevenin equivalent of the two.
 */
public class LineSeenTest {
    static final MachineSpec SLOW = MachineSpec.consumer(200).ratedWatts(2000).onBrownout(Brownout.SLOW).build();

    @Test
    void theLineAsTheSolverSeesIt() {
        var b = ElnTestKit.bench().supply(200, 0.01).line(8);
        var m = (BenchMachineImpl) b.add(SLOW).owner(x -> x.takeJoules(x.ratedJoulesPerTick()));
        b.ticks(30);
        double r = 8.01;
        assertEquals(200 * 200 / (4 * r), m.core.lineWattsLimit(), 1e-6 * 200 * 200 / (4 * r), "alone");
        double rl = 20;
        b.load(rl);
        b.ticks(30);
        double e = 200 * rl / (rl + r), rth = r * rl / (r + rl);
        assertEquals(e * e / (4 * rth), m.core.lineWattsLimit(), 1e-6 * e * e / (4 * rth), "with a 20 ohm load beside it");
    }
}
