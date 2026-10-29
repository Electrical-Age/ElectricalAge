package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftMotorElement;
import mods.eln.mechanical.ShaftNetwork;
import mods.eln.mechanical.ShaftSteamTurbineElement;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.Wp11Cases.*;

/**
 * The shaft Steam Turbine on the steam network, the 8 kW generator and the shaft motor (Baughn 2026-10-29, the shaft
 * redo): steam fed at the turbine's 20 bar rating, generators loaded by resistors on their terminals.
 */
public final class ShaftSteamCases {
    private ShaftSteamCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new DrivesGenerators(false));
        cases.add(new DrivesGenerators(true));
        cases.add(new ExhaustPipedOn());
        cases.add(new BoilerPipeTurbine());
        cases.add(new GovernorLoadStep());
        cases.add(new MotorSpinsUp());
        cases.add(new OldSaveLoads());
        cases.add(new Overspeed(false));
        cases.add(new Overspeed(true));
    }

    static final int GENERATOR_8KW = 16 + (4 << 6), MOTOR = 17 + (4 << 6);

    /** Saturated steam at the turbine's rating into its inlet while it is under it: a big boiler at 20 bar. */
    static void steam(ShaftSteamTurbineElement t) {
        double pa = ShaftSteamTurbineElement.NOMINAL_PA;
        if (t.inlet != null && t.inlet.P() < pa) EaUnits.addWater(t.inlet, 0.01, Water.tsat(pa), true);
    }

    /** Place a turbine at p facing XN (exhaust behind it, to the east), then parts along its shaft, alternately on
     *  each side, nearest first. Returns the turbine first, then the parts in order. */
    static TransparentNodeElement[] line(TestContext ctx, BlockPos p, int... parts) {
        TransparentNodeElement[] out = new TransparentNodeElement[parts.length + 1];
        out[0] = orient(ctx.placeTransparent(STEAM_TURBINE, p));
        Direction axis = out[0].front.left();
        for (int i = 0; i < parts.length; i++) {
            int k = i / 2 + 1;
            Direction d = i % 2 == 0 ? axis : axis.getInverse();
            BlockPos q = p;
            for (int j = 0; j < k; j++) q = off(q, d);
            out[i + 1] = orient(ctx.placeTransparent(parts[i], q));
        }
        return out;
    }

    /** Turn a just-placed shaft part to face XN and join its shaft to its neighbours' as it now lies. */
    static TransparentNodeElement orient(TransparentNodeElement e) {
        e.front = Direction.XN;
        e.reconnect();
        ((mods.eln.mechanical.ShaftElement) e).getShaft().connectShaft((mods.eln.mechanical.ShaftElement) e);
        return e;
    }

    /** A resistor drawing about `watts` at 3200 V from a generator's terminals (added to the simulator). */
    static Resistor load(GeneratorElement g, double watts) {
        ElectricalLoad in = field(g, "inputLoad");
        Resistor r = new Resistor(in, null).setR(3200.0 * 3200.0 / watts);
        Eln.simulator.addElectricalComponent(r);
        return r;
    }

    /**
     * Turbine + four 4 kW generators (+ a flywheel), each loaded to 3.5 kW, from rest on 20 bar steam: the turbine
     * brings the line up (spin-up time logged), then puts on the shaft what the steam gives: kg x ETA x the isentropic
     * drop to 1 atm, within 5 %, and that is what the generators deliver (/0.95, plus their drag).
     */
    static final class DrivesGenerators implements TestCase {
        final boolean flywheel;
        ShaftSteamTurbineElement t;
        GeneratorElement[] gens = new GeneratorElement[4];
        Resistor[] loads = new Resistor[4];
        IProcess feed;
        int tick;
        double upAt = -1, kg0, work0, kg1, work1, elec, drag, ke0, ke1, wind0, wind1, window;

        DrivesGenerators(boolean flywheel) {
            this.flywheel = flywheel;
        }

        public String id() {
            return flywheel ? "shaft_steam_turbine_line_with_flywheel" : "shaft_steam_turbine_drives_generators";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return flywheel ? 1800 : 1200;
        }

        public void build(TestContext ctx) {
            int[] parts = flywheel ? new int[]{GENERATOR, GENERATOR, GENERATOR, GENERATOR, FLYWHEEL} : new int[]{GENERATOR, GENERATOR, GENERATOR, GENERATOR};
            TransparentNodeElement[] l = line(ctx, ctx.at(5, 1, 5), parts);
            t = (ShaftSteamTurbineElement) l[0];
            for (int i = 0; i < 4; i++) {
                gens[i] = (GeneratorElement) l[i + 1];
                loads[i] = load(gens[i], 3500);
            }
            int from = flywheel ? 1500 : 900;
            Eln.simulator.addSlowProcess(feed = dt -> {
                steam(t);
                tick++;
                ShaftNetwork s = shaft(t);
                if (upAt < 0 && s.getRads() > 0.95 * ShaftSteamTurbineElement.RATED_RADS) upAt = tick * dt;
                if (tick == from) {
                    kg0 = t.totalKg;
                    work0 = t.totalWork;
                    ke0 = s.getEnergy();
                    wind0 = t.totalWindage;
                }
                if (tick > from) {
                    window += dt;
                    for (Resistor r : loads) elec += r.getP() * dt;
                    drag += gens.length * 0.02 * 20 * Math.max(s.getRads(), 10) * dt;
                    kg1 = t.totalKg;
                    work1 = t.totalWork;
                    ke1 = s.getEnergy();
                    wind1 = t.totalWindage;
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            ShaftNetwork s = shaft(t);
            String what = "shaft steam turbine" + (flywheel ? " + flywheel" : "");
            ctx.line(what + ": inertia " + String.format("%.4f kg m^2", s.getInertia()) + ", up to 95% of 800 rad/s in "
                + (upAt < 0 ? "never" : String.format("%.1f s", upAt)) + "; " + t.getWaila());
            ctx.check(what + ": one shaft (turbine, 4 generators" + (flywheel ? ", flywheel" : "") + ")",
                s.getElements().size() == (flywheel ? 6 : 5), s.getElements().size() + " elements");
            ctx.check(what + ": the steam brings the line up to speed", upAt > 0, String.format("%.0f rad/s", s.getRads()));
            double kg = kg1 - kg0, work = work1 - work0;
            double drop = Water.isentropicDrop(Water.tsat(ShaftSteamTurbineElement.NOMINAL_PA), ShaftSteamTurbineElement.NOMINAL_PA, Water.P_ATM);
            double expect = kg * ShaftSteamTurbineElement.ETA * drop;
            ctx.check(what + ": work = kg x ETA x isentropic drop (20 bar -> 1 atm), within 5%", kg > 0 && Math.abs(work - expect) < 0.05 * expect,
                String.format("%.0f J from %.4f kg; expected %.0f J", work, kg, expect));
            double taken = elec / 0.95 + drag * 0.95 + (wind1 - wind0) + (ke1 - ke0);
            ctx.check(what + ": the generators deliver what the shaft got (/0.95 + drag + windage + kinetic change), within 5%",
                Math.abs(taken - work) < 0.05 * work, String.format("work %.0f J, taken %.0f J (electrical %.0f J over %.1f s)", work, taken, elec, window));
            ctx.check(what + ": 14 kW of load held near rated speed (760-840 rad/s)", s.getRads() > 760 && s.getRads() < 840, String.format("%.0f rad/s", s.getRads()));
        }
    }

    /**
     * Its exhaust piped on (Baughn: the steam leaves with what's left): turbine 1's back port by an alloy pipe into a
     * second turbine's inlet, each with a loaded 8 kW generator. The second runs off the first one's exhaust; the first
     * expands only to the pressure between them, so it gets less per kg than venting would give.
     */
    static final class ExhaustPipedOn implements TestCase {
        ShaftSteamTurbineElement hp, lp;
        Resistor r, r2;
        IProcess feed;

        public String id() {
            return "shaft_steam_turbine_exhaust_piped_on";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(2, 1, 5);
            TransparentNodeElement[] a = line(ctx, p, GENERATOR_8KW);
            hp = (ShaftSteamTurbineElement) a[0];
            r = load((GeneratorElement) a[1], 6000);
            BlockPos b = p.east();
            PipeCases.lay(ctx, b, 1, 0, 8, 8, 8, 8, 8, 8, 16, 8);              // out of its back, then up
            PipeCases.lay(ctx, b.above(), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);      // up in, then east
            PipeCases.lay(ctx, b.above().east(), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);   // east in, down into the second
            TransparentNodeElement[] c = line(ctx, b.east(), GENERATOR_8KW);
            lp = (ShaftSteamTurbineElement) c[0];
            r2 = load((GeneratorElement) c[1], 12000);   // (a heavy load: its valve full open, as an LP turbine's would be)
            Eln.simulator.addSlowProcess(feed = dt -> steam(hp));
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            Eln.simulator.removeElectricalComponent(r);
            Eln.simulator.removeElectricalComponent(r2);
            ctx.line("shaft steam turbine piped on: first " + hp.getWaila() + "; second " + lp.getWaila());
            ctx.check("exhaust piped: the first turbine's exhaust is piped", hp.exhaustPiped, "");
            ctx.check("exhaust piped: the second turbine takes the first one's exhaust", lp.totalKg > 0.2 * hp.totalKg,
                String.format("%.3f kg of %.3f kg", lp.totalKg, hp.totalKg));
            ctx.check("exhaust piped: and turns its own shaft", shaft(lp).getRads() > 100, String.format("%.0f rad/s", shaft(lp).getRads()));
            // (two turbines rated alike in series: the second passes no more than the first, so the pressure between them
            // stays near the inlet's and the first does little; an LP turbine, built for the low pressure, fixes that)
            ctx.check("exhaust piped: the first expands only to the pipe's pressure, not to 1 atm", hp.exhaustPa() > Water.P_ATM * 1.05,
                String.format("%.2f bar", hp.exhaustPa() / 1e5));
            ctx.check("exhaust piped: both turbines do work", hp.totalWork > 0 && lp.totalWork > 0,
                String.format("%.0f J and %.0f J", hp.totalWork, lp.totalWork));
        }
    }

    /**
     * Boiler -> alloy pipe -> Steam Turbine -> shaft -> 8 kW generator (Baughn: steam by pipe): a boiler fired with
     * 30 kW runs the turbine, which carries a 2 kW load at speed while the boiler has water; the steam goes out of its
     * back into the air.
     */
    static final class BoilerPipeTurbine implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        ShaftSteamTurbineElement t;
        Resistor r;
        IProcess fire;
        double peakRads, peakW;

        public String id() {
            return "shaft_steam_turbine_boiler_pipe";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            BlockPos b = ctx.at(2, 1, 5);
            boiler = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), b);
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            PipeCases.lay(ctx, b.above(), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);           // up out of the boiler, then east
            PipeCases.lay(ctx, b.above().east(), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, b.above().east().east(), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);   // down into the turbine
            TransparentNodeElement[] l = line(ctx, b.east().east(), GENERATOR_8KW);
            t = (ShaftSteamTurbineElement) l[0];
            r = load((GeneratorElement) l[1], 2000);
            Eln.simulator.addSlowProcess(fire = dt -> {
                boiler.thermal.moveEnergyTo(30000 * dt);
                // (the boiler holds half a kilo of water: it runs dry in a minute or so; what it did while it had steam)
                if (shaft(t).getRads() > peakRads) peakRads = shaft(t).getRads();
                if (shaft(t).getRads() > 700) peakW = Math.max(peakW, t.shaftW);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(fire);
            Eln.simulator.removeElectricalComponent(r);
            ctx.line("boiler -> pipe -> shaft turbine: boiler " + boiler.getWaila() + "; turbine " + t.getWaila());
            ctx.check("boiler pipe: the turbine runs off the piped steam", t.totalKg > 0 && peakRads > 700,
                String.format("%.3f kg, peak %.0f rad/s", t.totalKg, peakRads));
            ctx.check("boiler pipe: and carries the 2 kW load at speed (1.5-4 kW on the shaft, some spinning it up)", peakW > 1500 && peakW < 4000, String.format("%.0f W", peakW));
            ctx.check("boiler pipe: its exhaust goes to the air", !t.exhaustPiped, "");
        }
    }

    /**
     * The governor (5 % droop): turbine + 8 kW generator at 4 kW, then 8 kW; the speed falls a little, within the droop
     * band, and it doesn't trip or run away.
     */
    static final class GovernorLoadStep implements TestCase {
        ShaftSteamTurbineElement t;
        GeneratorElement g;
        Resistor r;
        IProcess feed;
        int tick;
        double before, min = 1e9, max;

        public String id() {
            return "shaft_steam_turbine_governor_load_step";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            TransparentNodeElement[] l = line(ctx, ctx.at(5, 1, 5), GENERATOR_8KW);
            t = (ShaftSteamTurbineElement) l[0];
            g = (GeneratorElement) l[1];
            r = load(g, 4000);
            Eln.simulator.addSlowProcess(feed = dt -> {
                steam(t);
                tick++;
                double w = shaft(t).getRads();
                if (tick == 600) {
                    before = w;
                    r.setR(3200.0 * 3200.0 / 8000);
                }
                if (tick > 600) {
                    min = Math.min(min, w);
                    max = Math.max(max, w);
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            Eln.simulator.removeElectricalComponent(r);
            double after = shaft(t).getRads();
            ctx.line(String.format("governor: %.0f rad/s at 4 kW, %.0f at 8 kW (min %.0f, max %.0f); %s", before, after, min, max, t.getWaila()));
            ctx.check("governor: at 4 kW it runs inside the droop band (800-840 rad/s)", before > 795 && before < 841, String.format("%.0f", before));
            ctx.check("governor: at 8 kW it settles lower, still in the band", after < before && after > 790, String.format("%.0f", after));
            ctx.check("governor: the step doesn't pull it under 750 rad/s", min > 750, String.format("%.0f", min));
            ctx.check("governor: no trip, no burst", !t.tripped && !t.isBurnt() && max < 880, String.format("max %.0f", max));
        }
    }

    /** The shaft motor on 200 V spins a 4 kW generator up to its speed signal's absence: 800 rad/s, gently. */
    static final class MotorSpinsUp implements TestCase {
        ShaftMotorElement m;
        GeneratorElement g;
        double at5s = -1;
        int tick;
        IProcess watch;

        public String id() {
            return "shaft_motor_spins_up_generator";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(5, 1, 5);
            m = (ShaftMotorElement) ctx.placeTransparent(MOTOR, p);
            g = (GeneratorElement) ctx.placeTransparent(GENERATOR, off(p, m.front.left()));
            BlockPos cable = off(p, m.front), src = off(cable, m.front);
            ctx.placeSix(Ids.six("Medium Voltage Cable"), cable);
            var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(SOURCE, src);
            s.networkUnserialize(ctx.stream(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat(200f);
            }));
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (++tick == 10) at5s = shaft(m).getRads();
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            double w = shaft(m).getRads();
            ctx.line(String.format("motor: %.0f rad/s after 0.5 s, %.0f at the end; %s", at5s, w, m.getWaila()));
            ctx.check("motor: on the generator's shaft", shaft(m) == shaft(g), "");
            ctx.check("motor: no jump from rest (under 400 rad/s after 0.5 s: its torque limit)", at5s >= 0 && at5s < 400, String.format("%.0f", at5s));
            ctx.check("motor: brings it to about 800 rad/s (no signal: full speed)", w > 700 && w < 820, String.format("%.0f rad/s", w));
            ctx.check("motor: draws power to do it", m.getDrawnW() > 0, String.format("%.0f W", m.getDrawnW()));
        }
    }

    /**
     * Overspeed (Baughn 2026-10-29): an unloaded turbine on 20 bar steam. Automatic, the governor and the trip keep it
     * under 880 rad/s; under manual control (throttle full open) neither protects it: it races past the trip to the
     * shaft's burst speed and fails.
     */
    static final class Overspeed implements TestCase {
        final boolean manual;
        ShaftSteamTurbineElement t;
        BlockPos at;
        IProcess feed;
        double max;
        boolean burst, sawTrip;
        int tick;
        String status;

        Overspeed(boolean manual) {
            this.manual = manual;
        }

        public String id() {
            return manual ? "shaft_steam_turbine_manual_overspeeds" : "shaft_steam_turbine_automatic_trips";
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            at = ctx.at(3, 1, 1);
            t = (ShaftSteamTurbineElement) ctx.placeTransparent(STEAM_TURBINE, at);
            t.controls.manual = manual;
            Eln.simulator.addSlowProcess(feed = dt -> {
                if (t.isBurnt() || mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(at, ctx.level())) == null) {
                    burst = true;
                    return;
                }
                steam(t);
                if (++tick == 200) shaft(t).setRads(920);   // (as if a load fell off: over the trip)
                if (tick == 20) status = t.status();
                sawTrip |= t.tripped;
                if (tick < 200) max = Math.max(max, shaft(t).getRads());
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            String what = manual ? "manual control" : "automatic";
            ctx.line(String.format("overspeed, %s: max %.0f rad/s, burst %s", what, max, burst));
            if (manual) {
                ctx.check("overspeed: under manual control nothing stops it: past the trip to the burst speed, and it fails",
                    burst && max > 880, String.format("max %.0f rad/s, burst %s", max, burst));
                ctx.check("overspeed: manual control never trips", !sawTrip, "");
                ctx.check("overspeed: Jade says why: manual control, no governor or trip", status != null && status.startsWith("Manual control"), String.valueOf(status));
            } else {
                ctx.check("overspeed: automatic, the governor holds it under 880 rad/s", !burst && max < 880 && max > 700,
                    String.format("max %.0f rad/s", max));
                // (unloaded, only windage slows it: it coasts down, the valve shut until it's under 800)
                ctx.check("overspeed: automatic, pushed to 920 it trips and coasts down", sawTrip && t.tripped && !burst && shaft(t).getRads() < 915,
                    String.format("tripped %s, now %.0f rad/s", sawTrip, shaft(t).getRads()));
            }
        }
    }

    /**
     * A turbine saved by the 1.12 capability turbine (its tank and rate keys, no inlet) loads: the shaft speed comes
     * back, the old tank is dropped, the inlet starts as air.
     */
    static final class OldSaveLoads implements TestCase {
        ShaftSteamTurbineElement t;

        public String id() {
            return "shaft_steam_turbine_old_save_loads";
        }

        public void build(TestContext ctx) {
            t = (ShaftSteamTurbineElement) ctx.placeTransparent(STEAM_TURBINE, ctx.at(3, 1, 1));
            net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
            t.writeToNBT(old);
            for (String k : new ArrayList<>(old.keySet())) if (k.startsWith("inlet") || k.startsWith("exhaust")) old.remove(k);
            old.putFloat("shaftrads", 300f);
            old.putInt("tanktank", 7200);
            old.putFloat("procrc", 5f);
            t.readFromNBT(old);
        }

        public void measure(TestContext ctx) {
            ctx.check("old save: the shaft speed comes back (300 rad/s, nothing driving it but no drag)", shaft(t).getRads() > 299,
                String.format("%.1f rad/s", shaft(t).getRads()));
            ctx.check("old save: the inlet is air at ~1 atm (the old tank dropped)", t.inlet != null && t.inlet.P() < 1.2e5 && t.inlet.mVapour() < 1e-3,
                t.inlet == null ? "none" : String.format("%.2f bar", t.inlet.P() / 1e5));
        }
    }
}
