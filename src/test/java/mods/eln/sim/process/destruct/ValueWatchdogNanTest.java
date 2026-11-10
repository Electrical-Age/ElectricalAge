package mods.eln.sim.process.destruct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** One NaN sample must not switch a watchdog off: it used to make its timeout NaN, and it never fired again. */
public class ValueWatchdogNanTest {
    static final class Probe extends ValueWatchdog {
        double value;

        Probe() {
            max = 1;
            min = -1;
            timeoutReset = 1;
        }

        @Override
        double getValue() {
            return value;
        }

        @Override
        FailureCause cause() {
            return FailureCause.OVERVOLTAGE;
        }
    }

    @Test
    void aNanSampleIsSkippedAndTheWatchdogStillTrips() {
        Probe w = new Probe();
        int[] failed = {0};
        w.set(new IDestructable() {
            public void destructImpl() {
                failed[0]++;
            }

            public String describe() {
                return "probe";
            }
        });
        w.process(0.05);
        w.value = Double.NaN;
        w.process(0.05);
        w.value = Double.POSITIVE_INFINITY;
        w.process(0.05);
        assertEquals(0, failed[0], "a non-finite sample is no reason to fail");
        // 10 over the limit: overflow 9 x 0.5..1.5 per second against a budget of 1 (the first sample is forgiven)
        w.value = 10;
        for (int t = 0; t < 20; t++) w.process(0.05);
        assertEquals(true, failed[0] >= 1, "trips on a real overflow after the NaN");
    }
}
