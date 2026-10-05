package mods.eln.sim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * WP16b: watts vs joules-per-step on thermal loads. movePowerTo(W) adds a power for ONE step of the load
 * (a slow load steps once per tick, a fast one 20 times); moveEnergyTo(J) is folded in as J / dt at the load's
 * next step, so it is right from a process at any rate (electrical step, fast or slow thermal step).
 */
public class ThermalUnitsTest {

    /** Deposits watts * time as energy each call; counts the joules. */
    static final class EnergyHeater implements IProcess {
        final ThermalLoad load;
        final double watts;
        double joules;

        EnergyHeater(ThermalLoad load, double watts) {
            this.load = load;
            this.watts = watts;
        }

        @Override
        public void process(double time) {
            load.moveEnergyTo(watts * time);
            joules += watts * time;
        }
    }

    static ThermalLoad lossless(boolean slow) {
        ThermalLoad l = new ThermalLoad(0, 1e30, 1, 50);
        if (slow) l.setAsSlow();
        return l;
    }

    /** 100 W as energy into C = 50 J/K for 10 s -> 20 K, whichever list the heater runs on and whatever the load. */
    @Test
    public void moveEnergyIsRateInvariant() {
        for (double electricalHz : new double[]{20, 40}) {
            Simulator sim = new Simulator(SimHarness.TICK, 1 / electricalHz, 50, SimHarness.THERMAL_PERIOD);
            sim.init();
            mods.eln.Eln.simulator = sim;
            ThermalLoad slowFromElectrical = lossless(true), slowFromFast = lossless(true), slowFromSlow = lossless(true);
            ThermalLoad fastFromElectrical = lossless(false), fastFromFast = lossless(false);
            for (ThermalLoad l : new ThermalLoad[]{slowFromElectrical, slowFromFast, slowFromSlow, fastFromElectrical, fastFromFast})
                sim.addThermalLoad(l);
            EnergyHeater se = new EnergyHeater(slowFromElectrical, 100), sf = new EnergyHeater(slowFromFast, 100),
                ss = new EnergyHeater(slowFromSlow, 100), fe = new EnergyHeater(fastFromElectrical, 100),
                ff = new EnergyHeater(fastFromFast, 100);
            sim.addElectricalProcess(se);
            sim.addThermalFastProcess(sf);
            sim.addSlowProcess(ss);
            sim.addElectricalProcess(fe);
            sim.addThermalFastProcess(ff);
            for (int i = 0; i < 200; i++) sim.tick(new net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent(
                net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START));
            String at = " (electrical " + electricalHz + " Hz)";
            // T = deposited energy / C, exactly, whatever the caller's rate (the electrical loop may run one step
            // more than 20 Hz x 10 s, see SimulatorTimingTest, so compare with what was deposited)
            assertEquals("slow <- electrical" + at, se.joules / 50, slowFromElectrical.Tc, 1e-9);
            assertEquals("slow <- fast" + at, sf.joules / 50, slowFromFast.Tc, 1e-9);
            // the slow-process list runs after the slow thermal step: its last tick is still pending
            assertEquals("slow <- slow" + at, (ss.joules - 100 * 0.05) / 50, slowFromSlow.Tc, 1e-9);
            assertEquals("fast <- electrical" + at, fe.joules / 50, fastFromElectrical.Tc, 100 * 0.05 / 50 + 1e-9);
            assertEquals("fast <- fast" + at, ff.joules / 50, fastFromFast.Tc, 1e-9);
            assertEquals("about 100 W x 10 s" + at, 1000, se.joules, 100 / electricalHz + 1e-9);
            assertEquals("Pc reports watts" + at, 100, slowFromFast.Pc, 1e-6);
        }
    }

    /** The trap moveEnergyTo exists for: movePowerTo from a fast process onto a slow load counts 20 x. */
    @Test
    public void movePowerFromFastProcessOnSlowLoadCounts20x() {
        SimHarness h = new SimHarness();
        ThermalLoad slow = lossless(true);
        h.sim.addThermalLoad(slow);
        h.sim.addThermalFastProcess(ThermalTest.heater(slow, 100));
        h.seconds(10);
        assertEquals(20 * 20, slow.Tc, 1e-6);
    }

    /**
     * Generator thermal sizing (GeneratorDescriptor, Wp11Content: 4000 W, 800 rad/s, eff 0.95, six-node
     * initializer 130 K / 30 s): heat at nominal = 4000 (1/0.95 - 1) + 0.02 * 20 * 800 * 0.05 = 226.5 W,
     * setMaximalPower(226.5 / 0.65) -> steady 0.65 * 130 = 84.5 K; the warm limit is reached at ~1.58 x nominal.
     * Mirrors GeneratorShaftProcess's heat path (energy per electrical step via moveEnergyTo).
     */
    @Test
    public void generatorSizingKeepsNominalUnderTheWarmLimit() {
        final double eff = 0.95, nominalP = 4000, rads = 800, drag = 0.02 * rads; // J per tick
        double nominalHeat = nominalP * (1 / eff - 1) + drag * 20 * (1 - eff);
        assertEquals(226.5, nominalHeat, 0.1);
        for (final double load : new double[]{1.0, 1.2, 1.6}) {
            SimHarness h = new SimHarness();
            ThermalLoadInitializer init = new ThermalLoadInitializer(130, -100, 30, 1000);
            init.setMaximalPower(nominalHeat / 0.65);
            final ThermalLoad thermal = new ThermalLoad();
            init.applyTo(thermal);
            thermal.setAsSlow();
            h.sim.addThermalLoad(thermal);
            h.sim.addElectricalProcess(new IProcess() {
                @Override
                public void process(double time) {
                    double e = load * nominalP * time;
                    double dragE = drag * time * 20;
                    thermal.moveEnergyTo(e / eff - e + dragE * (1 - eff));
                }
            });
            h.seconds(300); // 10 tau
            double tss = 130 * (load * nominalP * (1 / eff - 1) + drag * 20 * (1 - eff)) / (nominalHeat / 0.65);
            assertEquals("load " + load, tss, thermal.Tc, 0.01 * tss);
            if (load == 1.0) assertEquals(0.65 * 130, thermal.Tc, 1);
            if (load == 1.2) assertTrue("1.2 x under the limit: " + thermal.Tc, thermal.Tc < 130);
            if (load == 1.6) assertTrue("1.6 x over the limit: " + thermal.Tc, thermal.Tc > 130);
        }
    }
}
