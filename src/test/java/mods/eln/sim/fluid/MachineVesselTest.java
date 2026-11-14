package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vessels behind a recipe machine's flanges, built on the core as the machines build them (Vessels' rules), on
 * lines as the game makes them: a run of iron pipe is one node with its wall.
 */
public class MachineVesselTest {
    static final int W = SpeciesRegistry.WATER;
    static final double DT = 0.05, PORT_K = 6e-11, VENT_K = 1e-11, MB = EaUnits.KG_PER_MB_WATER / Water.RHO_L;

    /** kg in a mB of species id, as the game counts it: liquids by volume, gases as steam is, by moles. */
    static double kgPerMb(int id, boolean vapour) {
        Species s = SpeciesRegistry.get().byId(id);
        if (id == W) return vapour ? EaUnits.KG_PER_MB_STEAM : EaUnits.KG_PER_MB_WATER;
        if (vapour || s.gas()) return EaUnits.KG_PER_MB_STEAM * s.molarMass / SpeciesRegistry.get().byId(W).molarMass;
        return EaUnits.KG_PER_MB_WATER * s.rhoL / Water.RHO_L;
    }

    /** Wall-clock ms a step: a vessel brim-full of liquid with flow through it costs 15 to 30, what is built here under 3. */
    static double ms(FluidNetwork net) {
        return net.nanos / 1e6 / net.steps;
    }

    static FluidNode vessel(FluidNetwork net, double V) {
        FluidNode n = new FluidNode(V, 0, 1, false);
        n.fill(0, Water.P_ATM, 293.15);
        return net.add(n);
    }

    /** A flange's stub: it joins the pipe that meets it. */
    static FluidNode stub(FluidNetwork net) {
        FluidNode n = new FluidNode(2e-5, 0.4, 0.1, false);
        n.fill(0, Water.P_ATM, 293.15);
        return net.add(n);
    }

    /** A boundary of V m^3 at T holding the liquids given (id, kg pairs) under air at airPa. */
    static FluidNode fixed(FluidNetwork net, double V, double T, double airPa, double... idKg) {
        FluidNode n = new FluidNode(V, 0, 1, true);
        double vLiq = 0;
        for (int i = 0; i < idKg.length; i += 2) {
            int id = (int) idKg[i];
            n.addMass(id, idKg[i + 1]);
            vLiq += idKg[i + 1] / SpeciesRegistry.get().byId(id).rhoL;
        }
        n.mAir += airPa * (V - vLiq) / (Water.R_AIR * T);
        n.energy = n.energyAt(T);
        n.update();
        return net.add(n);
    }

    /** A run of n blocks of iron pipe full of air (one node, with its wall) from a (at drawA) to b (at drawB). */
    static FluidNode pipeRun(FluidNetwork net, FluidNode a, double drawA, FluidNode b, double drawB, int n) {
        FluidNode p = new FluidNode(n * 3e-5, 0.375, 0.25, false);
        p.fill(0, Water.P_ATM, 293.15);
        p.wallUA = 2 * n;
        p.wallT = 293.15;
        net.add(p);
        net.connect(a, drawA, p, 0.5, 2 * 3e-11 / n);
        net.connect(p, 0.5, b, drawB, 2 * 3e-11 / n);
        return p;
    }

    static FluidEdge fillingValve(FluidNetwork net, FluidNode stub, FluidNode v) {
        return net.connect(stub, 0.5, v, 1.0, PORT_K).kind(FluidEdge.Kind.CHECK);
    }

    /** An open vessel's breather, as DeviceOutlet's: a float vent with a dead band, and a vacuum breaker. */
    static final class Breather {
        final FluidEdge tip, airIn;

        Breather(FluidNetwork net, FluidNode v) {
            FluidNode atm = net.add(FluidNode.atmosphere(1));
            tip = net.connect(v, 1.0, atm, 0, VENT_K).kind(FluidEdge.Kind.CHECK);
            tip.gasOnly = true;
            airIn = net.connect(atm, 0, v, 1.0, VENT_K).kind(FluidEdge.Kind.CHECK);
        }

        void set(FluidNode v) {
            double dp = v.P() - Water.P_ATM;
            double fl = Math.max(0, Math.min(1, (0.97 - v.liquidVolume() / v.volume) / 0.02));
            tip.open = fl * Math.max(0, Math.min(1, (dp - 300) / 300));
            airIn.open = Math.max(0, Math.min(1, (-dp - 300) / 300));
        }
    }

    /**
     * A breathing inlet vessel of 1000 mB on a line of species id from a tank at srcPa: it fills, then the machine uses
     * mbPerS for 100 s, with water in the feed for a while that the machine drains. Whatever the line's pressure the
     * vessel holds its level and its gas cap near 1 atm, and the machine gets all it asks for.
     */
    void inlet(String id, double srcPa, double mbPerS) {
        int g = TestSpecies.id(id);
        Species s = SpeciesRegistry.get().byId(g);
        FluidNetwork net = new FluidNetwork();
        FluidNode src = fixed(net, 1, 293.15, srcPa, g, 0.8 * s.rhoL);
        FluidNode v = vessel(net, 1000 * MB), st = stub(net);
        pipeRun(net, src, 0.2, st, 0.5, 5);
        FluidEdge valve = fillingValve(net, st, v);
        Breather br = new Breather(net, v);
        double want = mbPerS * kgPerMb(g, false), used = 0, maxP = 0, minLevel = 1, maxLevel = 0;
        for (int i = 0; i < 2400; i++) {
            double t = i * DT;
            br.set(v);
            valve.open = Vessels.fillOpening(valve, v, DT);
            if (t >= 20) used += EaUnits.drainLayer(v, g, want * DT);
            if (t >= 60 && t < 90) EaUnits.addWater(v, EaUnits.KG_PER_MB_WATER * DT, 293.15, false);
            if (t >= 75) EaUnits.drainLiquid(v, v.mLiquid());
            net.step(DT);
            maxP = Math.max(maxP, v.P());
            double level = v.liquidVolume() / v.volume;
            maxLevel = Math.max(maxLevel, level);
            if (t >= 20) minLevel = Math.min(minLevel, level);
        }
        String at = id + " at " + srcPa / 1e5 + " bar: ";
        assertEquals(mbPerS * 100, used / kgPerMb(g, false), 0.01 * mbPerS * 100, at + "the machine got what it asked for (mB)");
        assertTrue(maxLevel < Vessels.FILL_LEVEL + 0.02, at + "never fuller than its filling level, was " + maxLevel);
        assertTrue(minLevel > Vessels.FILL_LEVEL - 0.05, at + "held its level while drawn from, fell to " + minLevel);
        assertTrue(maxP < 2.5e5, at + "the vessel never saw the line's pressure, peak " + maxP);
        assertEquals(Water.P_ATM, v.P(), 3000, at + "its gas cap at 1 atm");
        assertTrue(ms(net) < 5, at + "cheap to solve, ms a step " + ms(net));
    }

    @Test
    void anInletHoldsItsLevelAndItsGasCapOnAnyLine() {
        inlet("gasoline", Water.P_ATM, 10);
        inlet("gasoline", 6e5, 10);
        inlet("gasoline", 20e5, 10);
        inlet("heavy_oil", 6e5, 20);
    }

    /**
     * A sealed steam inlet of vesselM3 on a 2 bar steam line, the machine using 125 mB/s of steam and draining the
     * condensate. mix: by Vessels.drawMix; else steam only, which leaves the air the vessel began with in it.
     * Returns the mB/s it got over the last 60 s.
     */
    double steamInlet(double vesselM3, boolean mix) {
        FluidNetwork net = new FluidNetwork();
        FluidNode src = new FluidNode(1, 0, 1, true);
        src.mWater = 2e5 / (Water.R_V * 400);
        src.mAir = 0;
        src.energy = src.energyAt(400);
        src.update();
        net.add(src);
        FluidNode v = vessel(net, vesselM3), st = stub(net);
        pipeRun(net, src, 0.5, st, 0.5, 5);
        FluidEdge valve = fillingValve(net, st, v);
        double want = 125 * EaUnits.KG_PER_MB_STEAM, got = 0;
        for (int i = 0; i < 2400; i++) {
            double m = mix ? Vessels.drawMix(v, W, want * DT) : Math.min(want * DT, v.mVapour());
            if (!mix) EaUnits.drainVapour(v, m);
            if (i >= 1200) got += m;
            EaUnits.drainLiquid(v, v.mLiquid());
            valve.open = Vessels.fillOpening(valve, v, DT);
            net.step(DT);
            assertTrue(!mix || v.P() > Water.P_ATM - 2000, "the draw never pulls the vessel under 1 atm, was " + v.P());
        }
        return got / 60 / EaUnits.KG_PER_MB_STEAM;
    }

    @Test
    void aSealedSteamInletClearsItsAirThroughTheDraw() {
        assertEquals(125, steamInlet(4000 * MB, true), 1, "mB/s of steam by drawing the mix");
        assertTrue(steamInlet(4000 * MB, false) < 5, "drawing steam alone leaves the vessel's air in it, and the steam out");
    }

    /**
     * The cracker's butadiene outlet (a sealed vessel, a pump that keeps a sump, no gas line) to the polymeriser's
     * sealed inlet (a filling valve; it uses liquid first, then the gas mix) over blocks blocks of iron pipe. The
     * cracker makes 4 mB/s while its vessel is under 6 bar; the polymeriser, with an inlet of inletMb, stands from
     * stallFrom to stallTo s.
     */
    static final class ButadieneLine {
        final FluidNetwork net = new FluidNetwork();
        final int b = TestSpecies.id("butadiene");
        final double per = kgPerMb(b, false);
        final FluidNode out, in, pipe;
        final FluidEdge pump, valve;
        double made, used, blocked, t, minOutT = 1e9, maxOutP, minInP = 1e9, maxInLevel;

        ButadieneLine(int blocks, double inletMb) {
            out = vessel(net, 1000 * MB);
            FluidNode st = stub(net), inStub = stub(net);
            pump = net.connect(out, 0, st, 0.5, PORT_K).kind(FluidEdge.Kind.PUMP);
            pump.qMax = 200 * per;
            pump.h0 = 6e5 - Water.P_ATM;
            in = vessel(net, inletMb * MB);
            pipe = pipeRun(net, st, 0.5, inStub, 0.5, blocks);
            valve = fillingValve(net, inStub, in);
        }

        /** Run to end s; returns the mB the polymeriser used in that time. */
        double run(double end, boolean stalled) {
            double before = used;
            while (t < end) {
                if (out.P() < 6e5 && out.liquidVolume() < 0.95 * out.volume) {
                    EaUnits.add(out, b, 4 * per * DT, 293.15, false);
                    made += 4 * per * DT;
                } else blocked += DT;
                pump.speed = Vessels.sumpSpeed(out);
                valve.open = Vessels.fillOpening(valve, in, DT);
                if (!stalled) {
                    double want = 4 * per * DT, l = EaUnits.drainLayer(in, b, want);
                    used += l + Vessels.drawMix(in, b, want - l);
                }
                net.step(DT);
                t += DT;
                if (t > 60) minOutT = Math.min(minOutT, out.T());
                maxOutP = Math.max(maxOutP, out.P());
                minInP = Math.min(minInP, in.P());
                maxInLevel = Math.max(maxInLevel, in.liquidVolume() / in.volume);
            }
            return (used - before) / per;
        }
    }

    @Test
    void butadieneReachesThePolymeriserAsLiquidOrGas() {
        ButadieneLine l = new ButadieneLine(5, 2000);
        l.run(100, false);
        assertTrue(l.used / l.per > 60, "the line starts within 100 s (its pipes boil the liquid off), used mB " + l.used / l.per);
        assertEquals(0, l.run(300, true), 1e-9, "stalled");
        assertEquals(4 * 150, l.run(450, false), 6, "mB in 150 s after a stall: the full 4 mB/s");
        assertEquals(0, l.blocked, 1e-9, "the cracker never waited: the line holds a 200 s stall");
        assertTrue(l.minOutT > 280, "the outlet vessel keeps its sump and stays warm, coldest " + l.minOutT);
        assertTrue(l.maxOutP < 4e5, "under its own vapour and the air it began with, peak " + l.maxOutP);
        assertTrue(l.minInP > Water.P_ATM - 2000, "the polymeriser never draws its inlet under 1 atm, was " + l.minInP);
        assertTrue(ms(l.net) < 10, "cheap to solve, ms a step " + ms(l.net));
    }

    @Test
    void aStalledButadieneLineFillsBlocksTheCrackerAndResumes() {
        ButadieneLine l = new ButadieneLine(1, 500);
        l.run(650, true);
        assertTrue(l.blocked > 20, "the cracker waits once the line, the inlet and its own vessel are full, s " + l.blocked);
        assertTrue(l.maxOutP < 6.1e5, "at its pump's head, peak " + l.maxOutP);
        assertTrue(l.maxInLevel < Vessels.FILL_LEVEL + 0.02, "the inlet shut at its level, was " + l.maxInLevel);
        double madeBefore = l.made;
        assertEquals(4 * 60, l.run(710, false), 3, "mB in 60 s once the polymeriser runs again");
        assertEquals(4 * 60, (l.made - madeBefore) / l.per, 12, "and the cracker makes as much again");
    }

    /**
     * A gas outlet: natural gas made at 3 mB/s into a sealed vessel while it is under 6 bar, a check valve from its top
     * into the line, no pump. The line ends at a tap, shut at first, then open to a tank at 2 bar.
     */
    @Test
    void aGasOutletBlocksAtItsHeadAndDeliversUnderIt() {
        int ng = TestSpecies.id("natural_gas");
        double per = kgPerMb(ng, true), make = 3 * per, made = 0, blocked = 0, maxP = 0, madeOpen = 0;
        FluidNetwork net = new FluidNetwork();
        FluidNode out = vessel(net, 1000 * MB), st = stub(net);
        net.connect(out, 1.0, st, 0.5, PORT_K).kind(FluidEdge.Kind.CHECK);
        FluidNode sink = fixed(net, 1, 293.15, 2e5), pipe = pipeRun(net, st, 0.5, sink, 0.5, 5);
        FluidEdge tap = net.edges.get(net.edges.size() - 1);
        for (int i = 0; i < 9000; i++) {
            double t = i * DT;
            tap.open = t < 350 ? 0 : 1;
            if (out.P() < 6e5) {
                EaUnits.add(out, ng, make * DT, 293.15, true);
                made += make * DT;
                if (t >= 400) madeOpen += make * DT;
            } else blocked += DT;
            net.step(DT);
            maxP = Math.max(maxP, out.P());
        }
        assertTrue(blocked > 30 && blocked < 350, "it stopped making gas against the shut tap, s " + blocked);
        assertTrue(maxP < 6.1e5, "at its head, peak " + maxP);
        assertEquals(3 * 50, madeOpen / per, 1.5, "mB made in the last 50 s, the tap open");
        assertEquals(2e5, out.P(), 0.2e5, "the vessel at the line's pressure");
        assertTrue(pipe.P() < 2.2e5, "and the line at the tank's");
        assertTrue(ms(net) < 5, "cheap to solve, ms a step " + ms(net));
    }
}
