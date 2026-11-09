package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gameplay scenarios for the fluid core (M3), from the mod's GameTests' physics. */
class ScenarioTest {
    /** A trace of Newton on the pump's first step (FLUIDCORE_DEBUG=1; notes/fluid-review/pump-newton.txt). */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "FLUIDCORE_DEBUG", matches = "1")
    void debugPumpFirstStep() throws Exception {
        Network net = new Network();
        var res = net.add(new Network.Node(1, 0, 1, false).fill(600, RHO_AIR * 0.4, 293));
        var riser = net.add(new Network.Node(0.005, 0, 5, false).fill(0, RHO_AIR * 0.005, 293));
        var tank = net.add(new Network.Node(1, 5, 1, false).fill(0, RHO_AIR, 293));
        var pump = net.connect(res, 0, riser, 0, 1e-5);
        pump.boost = 1.2e5;
        pump.kind = Network.Kind.CHECK;
        net.connect(riser, 1, tank, 0, 1e-5);
        Network.DEBUG = new StringBuilder();
        Network.MAX_DEPTH = 0;
        try {
            net.step(DT);
            java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/pump-newton.txt"), Network.DEBUG.toString());
        } finally {
            Network.DEBUG = null;
            Network.MAX_DEPTH = 5;
        }
    }

    static final double DT = 0.05, RHO_AIR = 1.2;

    static double water(Network net) {
        double w = 0;
        for (var n : net.nodes) if (!n.fixed) w += n.mw();
        return w;
    }

    /** A pump lifts water 5 m up a riser into a tank when its head beats the column's, and not when it doesn't. */
    @Test
    void pumpLiftsWaterUpARiserOnlyWithHeadEnough() {
        for (double boost : new double[]{1.2e5, 3e4}) {
            Network net = new Network();
            var res = net.add(new Network.Node(1, 0, 1, false).fill(600, RHO_AIR * 0.4, 293));
            var riser = net.add(new Network.Node(0.005, 0, 5, false).fill(0, RHO_AIR * 0.005, 293));
            var tank = net.add(new Network.Node(1, 5, 1, false).fill(0, RHO_AIR, 293));
            var pump = net.connect(res, 0, riser, 0, 1e-5);
            pump.boost = boost;
            pump.kind = Network.Kind.CHECK;
            net.connect(riser, 1, tank, 0, 1e-5);
            double w0 = water(net);
            String fail = "";
            for (int s = 0; s < 1200; s++) {
                long f0 = net.stats.failures;
                net.step(DT);
                if (net.stats.failures > f0) fail += String.format("step %d (boost %.0f): norms %s, riser %.4g kg level %.4f P %.0f; tank %.3g kg; ", s, boost,
                    net.stats.lastFail.stream().map(v -> String.format("%.2e", v)).toList(), riser.mw(), riser.level(), riser.P(), tank.mw()) + java.util.Arrays.toString(net.stats.lastRows);
            }
            assertEquals(w0, water(net), 1e-9 * w0, "water kept");
            assertEquals("", fail, "failures");
            if (boost > 1e5) assertTrue(tank.mw() > 10, "1.2 bar lifts water into the tank: " + tank.mw() + " kg");
            else assertTrue(tank.mw() < 0.1, "0.3 bar can't lift 5 m: " + tank.mw() + " kg");
        }
    }

    /** A heated boiler's safety valve (opening at 10 bar over the air) holds it near 10 bar and vents steam. */
    @Test
    void safetyValveHoldsABoiler() {
        Network net = new Network();
        var boiler = net.add(new Network.Node(0.05, 0, 1, false).fill(30, RHO_AIR * 0.02, 373));
        boiler.heat = 200e3;   // (30 kg from 100 C to 180 C, 10 bar, in about a minute)
        var air = net.add(new Network.Node(1000, 1, 1, true).fill(0, RHO_AIR * 1000, 293));
        var valve = net.connect(boiler, 1, air, 0, 1e-6);
        valve.kind = Network.Kind.CHECK;
        valve.boost = -10e5;   // (opens once the boiler is 10 bar over the air)
        double peak = 0, w0 = boiler.mw();
        for (int s = 0; s < 4000; s++) {
            net.step(DT);
            peak = Math.max(peak, boiler.P());
        }
        assertTrue(peak < 11.5e5 + air.P(), "peak " + peak + " Pa");
        assertTrue(boiler.P() > 9e5, "it stays up near the setpoint: " + boiler.P());
        assertTrue(boiler.mw() < w0, "it vented steam: " + boiler.mw() + " of " + w0);
        assertEquals(0, net.stats.failures, "failures");
    }

    /** A tiny pipe between a big hot vessel and a big cold tank: no wild pressures, nothing lost, water gets through. */
    @Test
    void shortHotPipeBetweenBigVessels() {
        Network net = new Network();
        var hot = net.add(new Network.Node(1, 0, 1, false).fill(900, 0.001, 453));
        var pipe = net.add(new Network.Node(1e-4, 0, 0.1, false).fill(0.09, RHO_AIR * 1e-4, 293));
        var cold = net.add(new Network.Node(1, 0, 1, false).fill(400, RHO_AIR * 0.6, 300));
        net.connect(hot, 0, pipe, 0, 1e-5);
        net.connect(pipe, 0, cold, 0, 1e-5);
        double w0 = water(net), c0 = cold.mw(), worst = 0;
        for (int s = 0; s < 1000; s++) {
            net.step(DT);
            double lo = Math.min(hot.P(), cold.P()), hi = Math.max(hot.P(), cold.P());
            worst = Math.max(worst, Math.max(pipe.P() - hi, lo - pipe.P()));
        }
        assertEquals(w0, water(net), 1e-9 * w0, "water kept");
        assertTrue(worst < 2e5, "the pipe stays within 2 bar of its ends' range: " + worst + " Pa past it");
        assertTrue(cold.mw() > c0 + 1, "water got through: " + (cold.mw() - c0) + " kg");
        assertEquals(0, net.stats.failures, "failures");
    }

    /** A tank on a tank, joined floor to ceiling: with the exchange the upper drains into the lower; without it, it gas-locks. */
    @Test
    void tankStackDrainsThroughTheExchange() {
        for (double ex : new double[]{1e-6, 0}) {
            Network net = new Network();
            var upper = net.add(new Network.Node(0.5, 1, 1, false).fill(300, RHO_AIR * 0.2, 293));
            var lower = net.add(new Network.Node(0.5, 0, 1, false).fill(0, RHO_AIR * 0.5, 293));
            var e = net.connect(upper, 0, lower, 1, 1e-7);
            e.exchange = ex;
            double w0 = water(net);
            for (int s = 0; s < 2000; s++) net.step(DT);
            assertEquals(w0, water(net), 1e-9 * w0, "water kept");
            if (ex > 0) assertTrue(upper.mw() < 10, "with the exchange the upper drains: " + upper.mw() + " kg left");
            else assertTrue(upper.mw() > 150, "without it, most stays up (gas lock): " + upper.mw() + " kg");
            assertEquals(0, net.stats.failures, "failures");
        }
    }
}
