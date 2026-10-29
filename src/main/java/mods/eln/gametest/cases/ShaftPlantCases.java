package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftMotorElement;
import mods.eln.mechanical.ShaftSteamTurbineElement;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.voxel.ValvePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.oil.PlantCondenserElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.core.BlockPos;

import java.util.List;

import static mods.eln.gametest.cases.ShaftSteamCases.*;
import static mods.eln.gametest.cases.Wp11Cases.*;

/**
 * The shaft redo, part 3 (Baughn 2026-10-29): the LP Steam Turbine, the Plant Steam Condenser with its motor-driven pump
 * and ejector, and a relief valve as the plant's dump.
 */
public final class ShaftPlantCases {
    private ShaftPlantCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new PlantGain());
        cases.add(new CondenserVacuum(true));
        cases.add(new CondenserVacuum(false));
        cases.add(new WetSteam(true));
        cases.add(new WetSteam(false));
        cases.add(new Flooding());
        cases.add(new DumpOnLoadRejection());
        cases.add(new FullPlantCoolingWater());
    }

    static final int LP_TURBINE = 18 + (4 << 6), CONDENSER = 52 + (82 << 6);

    /** A turbine of a kind at p facing XN, with parts along its shaft (as ShaftSteamCases.line). */
    static TransparentNodeElement[] turbineLine(TestContext ctx, int turbine, BlockPos p, int... parts) {
        TransparentNodeElement[] out = new TransparentNodeElement[parts.length + 1];
        out[0] = orient(ctx.placeTransparent(turbine, p));
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

    /** A plant condenser at p facing XN (or front), and a powered shaft motor against its north side (motor: or none). */
    static PlantCondenserElement condenser(TestContext ctx, BlockPos p, boolean motor) {
        return condenser(ctx, p, motor, Direction.XN);
    }

    static PlantCondenserElement condenser(TestContext ctx, BlockPos p, boolean motor, Direction front) {
        var c = (PlantCondenserElement) ctx.placeTransparent(CONDENSER, p);
        c.front = front;
        c.reconnect();
        if (motor) {
            BlockPos mp = p.north();
            var m = (ShaftMotorElement) ctx.placeTransparent(MOTOR, mp);
            m.front = Direction.ZN;   // (power from the north; its shaft runs east-west, clear of the condenser)
            m.reconnect();
            m.getShaft().connectShaft(m);
            BlockPos cable = mp.north(), src = cable.north();
            ctx.placeSix(Ids.six("Medium Voltage Cable"), cable);
            var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(SOURCE, src);
            s.networkUnserialize(ctx.stream(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat(200f);
            }));
        }
        return c;
    }

    /** Pipe out of a turbine's back (east of `at`), up, east one block, down into the top of the block east of that. */
    static void exhaustPipe(TestContext ctx, BlockPos turbine) {
        BlockPos b = turbine.east();
        PipeCases.lay(ctx, b, 1, 0, 8, 8, 8, 8, 8, 8, 16, 8);
        PipeCases.lay(ctx, b.above(), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
        PipeCases.lay(ctx, b.above().east(), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
    }

    /**
     * HP -> LP -> plant condenser against HP venting to air, the same throttled 20 bar steam, both loaded past what
     * they give (governors full open): the work per kg of steam, the plant's over the venting HP's (Baughn's ~+80 %).
     */
    static final class PlantGain extends PlumbingCases.Big {
        ShaftSteamTurbineElement hpA, hpB, lpB;
        PlantCondenserElement c;
        Resistor[] loads = new Resistor[3];
        IProcess feed;
        int tick;
        double kgA0, wA0, kgB0, wB0, wL0, kgA, wA, kgB, wB, wL;

        public String id() {
            return "shaft_plant_hp_lp_condenser_gain";
        }

        public int ticks() {
            return 1400;
        }

        public void build(TestContext ctx) {
            BlockPos hp = ctx.at(2, 1, 5), lp = ctx.at(4, 1, 5), cp = ctx.at(5, 1, 5), va = ctx.at(2, 1, 8);
            TransparentNodeElement[] a = turbineLine(ctx, STEAM_TURBINE, va, GENERATOR_8KW);
            TransparentNodeElement[] b = turbineLine(ctx, STEAM_TURBINE, hp, GENERATOR_8KW);
            TransparentNodeElement[] l = turbineLine(ctx, LP_TURBINE, lp, GENERATOR_8KW);
            hpA = (ShaftSteamTurbineElement) a[0];
            hpB = (ShaftSteamTurbineElement) b[0];
            lpB = (ShaftSteamTurbineElement) l[0];
            loads[0] = load((GeneratorElement) a[1], 9000);
            loads[1] = load((GeneratorElement) b[1], 9000);
            loads[2] = load((GeneratorElement) l[1], 9000);
            // (a quarter throttle: ~15 g/s, what the condenser's 40 kW shell can take)
            hpA.controls.throttle = 0.25;
            hpB.controls.throttle = 0.25;
            exhaustPipe(ctx, hp);
            c = condenser(ctx, cp, true, Direction.XP);   // (its back against the LP's back: the exhausts meet)
            Eln.simulator.addSlowProcess(feed = dt -> {
                steam(hpA);
                steam(hpB);
                if (++tick == 800) {
                    kgA0 = hpA.totalKg;
                    wA0 = hpA.totalWork;
                    kgB0 = hpB.totalKg;
                    wB0 = hpB.totalWork;
                    wL0 = lpB.totalWork;
                }
                kgA = hpA.totalKg - kgA0;
                wA = hpA.totalWork - wA0;
                kgB = hpB.totalKg - kgB0;
                wB = hpB.totalWork - wB0;
                wL = lpB.totalWork - wL0;
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            double perA = wA / Math.max(1e-9, kgA), perB = (wB + wL) / Math.max(1e-9, kgB);
            ctx.line(String.format("plant: HP venting %.0f kJ/kg (%.1f g/s); HP+LP+condenser %.0f kJ/kg (HP %.0f + LP %.0f; %.1f g/s): %+.0f %%",
                perA / 1000, kgA / 30 * 1000, perB / 1000, wB / Math.max(1e-9, kgB) / 1000, wL / Math.max(1e-9, kgB) / 1000, kgB / 30 * 1000, (perB / perA - 1) * 100));
            ctx.line("plant: HP " + hpB.getWaila() + "; LP " + lpB.getWaila() + "; condenser " + c.getWaila());
            ctx.check("plant: the LP turbine runs on the HP's exhaust", wL > 0, String.format("%.0f J", wL));
            ctx.check("plant: the condenser holds a vacuum (under 0.3 bar)", c.v.vessel.P() < 0.3e5, String.format("%.2f bar", c.v.vessel.P() / 1e5));
            ctx.check("plant: HP + LP + condenser gets over +50 % more work per kg than the HP venting to air", perB > 1.5 * perA,
                String.format("%.0f vs %.0f kJ/kg", perB / 1000, perA / 1000));
        }
    }

    /** A source of cold water at `pa` (fixed: a lake behind a pump) and a drain at 1 atm, on two faces of a block. */
    static mods.eln.sim.fluid.FluidDevice coolingLoop(PlantCondenserElement c, BlockPos inAt, int inFace, BlockPos outAt, int outFace, double pa) {
        var lake = new mods.eln.sim.fluid.FluidNode(1.0, inAt.getY(), 1.0, true).fill(0.9 * Water.RHO_L, pa, 288.15);
        var drain = new mods.eln.sim.fluid.FluidNode(1.0, outAt.getY(), 1.0, true).fill(0, Water.P_ATM, 288.15);
        var level = c.fluidLevel();
        var ports = List.of(
            new mods.eln.sim.fluid.FluidDevice.FluidPort(lake, inAt.asLong(), inFace, 0.0, mods.eln.oil.CondenserVessel.PORT_K,
                mods.eln.oil.TankElement.sideSpot(inFace, mods.eln.oil.TankElement.LOW)),
            new mods.eln.sim.fluid.FluidDevice.FluidPort(drain, outAt.asLong(), outFace, 0.0, mods.eln.oil.CondenserVessel.PORT_K,
                mods.eln.oil.TankElement.sideSpot(outFace, mods.eln.oil.TankElement.LOW)));
        var d = new mods.eln.sim.fluid.FluidDevice() {
            public Object fluidLevel() {
                return level;
            }

            public List<FluidPort> fluidPorts() {
                return ports;
            }
        };
        mods.eln.fluid.FluidWorld.add(d);
        return d;
    }

    /**
     * Baughn 2026-10-29: can a full 16 kW HP (~39 g/s) be condensed? HP (throttle full) -> LP -> plant condenser with
     * cooling water through its tubes from a 2 bar supply to a drain: the vacuum holds; the numbers are logged.
     */
    static final class FullPlantCoolingWater extends PlumbingCases.Big {
        ShaftSteamTurbineElement hp;
        PlantCondenserElement c;
        mods.eln.sim.fluid.FluidDevice loop;
        Resistor[] loads = new Resistor[2];
        IProcess feed;
        int tick;
        double kg0, cool0, heat0, w0, kg, cool, heat, work, window, pSum;

        public String id() {
            return "plant_condenser_cooling_water_full_hp";
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            // (the HP's back straight against the condenser's back: no crossover pipe to throttle it)
            BlockPos hpAt = ctx.at(3, 1, 5), cp = ctx.at(4, 1, 5);
            TransparentNodeElement[] b = turbineLine(ctx, STEAM_TURBINE, hpAt, GENERATOR_8KW, GENERATOR_8KW);
            hp = (ShaftSteamTurbineElement) b[0];
            loads[0] = load((GeneratorElement) b[1], 9000);
            loads[1] = load((GeneratorElement) b[2], 9000);
            c = condenser(ctx, cp, true, Direction.XP);
            // (front XP: its left is south, its right north)
            loop = coolingLoop(c, cp.south(), 2, cp.north(), 3, 2e5);
            Eln.simulator.addSlowProcess(feed = dt -> {
                steam(hp);
                if (++tick == 700) {
                    kg0 = hp.totalKg;
                    cool0 = c.totalCoolingKg;
                    heat0 = c.totalTubesJ;
                    w0 = hp.totalWork;
                }
                if (tick > 700) {
                    window += dt;
                    kg = hp.totalKg - kg0;
                    cool = c.totalCoolingKg - cool0;
                    heat = c.totalTubesJ - heat0;
                    work = hp.totalWork - w0;
                    pSum += c.v.vessel.P() * dt;
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            mods.eln.fluid.FluidWorld.remove(loop);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            double steam = kg / window, water = cool / window, q = heat / window, p = pSum / window;
            ctx.line(String.format("full HP into the condenser: steam %.1f g/s, %.1f kW on the shaft (%.0f kJ/kg); condenser %.3f bar; "
                + "%.1f kW into %.2f kg/s of cooling water (+%.1f K), shell %.1f kW",
                steam * 1000, work / window / 1000, work / Math.max(1e-9, kg) / 1000, p / 1e5, q / 1000, water, q / Math.max(1e-9, water) / 4186, c.v.heatW / 1000));
            ctx.line("full HP: condenser " + c.getWaila() + "; HP " + hp.getWaila());
            ctx.check("full HP: it runs at about full steam (over 33 g/s, 16 kW on the shaft)", steam > 0.033 && work / window > 16e3,
                String.format("%.1f g/s, %.1f kW", steam * 1000, work / window / 1000));
            ctx.check("full HP: the cooling water takes most of the heat (over 50 kW; the shell the rest)", q > 50e3, String.format("%.1f kW", q / 1000));
            ctx.check("full HP: the vacuum holds (under 0.3 bar)", p < 0.3e5, String.format("%.2f bar", p / 1e5));
        }
    }

    /** The plant condenser's air ejector runs on its shaft motor: with one, a vessel of air pulls down to a vacuum; with
     *  none, it stays at 1 atm. */
    static final class CondenserVacuum extends PlumbingCases.Big {
        final boolean motor;
        PlantCondenserElement c;

        CondenserVacuum(boolean motor) {
            this.motor = motor;
        }

        public String id() {
            return motor ? "plant_condenser_motor_pulls_vacuum" : "plant_condenser_no_motor_no_vacuum";
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            c = condenser(ctx, ctx.at(5, 1, 5), motor);
        }

        public void measure(TestContext ctx) {
            double p = c.v.vessel.P() / 1e5;
            ctx.line("plant condenser" + (motor ? " with its motor: " : " without a motor: ") + c.getWaila());
            if (motor) {
                ctx.check("condenser: its motor-driven ejector pulls a vacuum (under 0.2 bar)", p < 0.2, String.format("%.3f bar", p));
                ctx.check("condenser: the ejector loads the motor", c.pumpW > 50 && c.motor != null, String.format("%.0f W", c.pumpW));
            } else
                ctx.check("condenser: no motor, no vacuum (still ~1 atm)", p > 0.95, String.format("%.3f bar", p));
        }
    }

    /**
     * Wet steam in an LP turbine at speed: water in its inlet brakes the shaft and knocks; kept up past WET_BURN_S it
     * strips the blades; a short slug (5 s) only brakes it.
     */
    static final class WetSteam extends PlumbingCases.Big {
        final boolean lasting;
        ShaftSteamTurbineElement t;
        IProcess feed;
        int tick;
        double maxBrake;

        WetSteam(boolean lasting) {
            this.lasting = lasting;
        }

        public String id() {
            return lasting ? "lp_turbine_wet_steam_strips_blades" : "lp_turbine_short_slug_only_brakes";
        }

        public int ticks() {
            return 500;
        }

        public void build(TestContext ctx) {
            TransparentNodeElement[] l = turbineLine(ctx, LP_TURBINE, ctx.at(5, 1, 5), FLYWHEEL);
            t = (ShaftSteamTurbineElement) l[0];
            Eln.simulator.addSlowProcess(feed = dt -> {
                if (t.isBurnt() || t.inlet == null) return;
                tick++;
                if (tick < 3) t.getShaft().setRads(800);
                double pa = t.spec.nominalPa;
                if (t.inlet.P() < pa) EaUnits.addWater(t.inlet, 0.002, Water.tsat(pa), true);
                // water in the inlet: 20 % of it (over WATER_MAX), for all of the test or the first 5 s
                if (lasting || tick < 100) {
                    double want = 0.2 * t.inlet.volume * Water.RHO_L - t.inlet.mLiquid();
                    if (want > 0) EaUnits.addWater(t.inlet, want, Water.tsat(pa), false);
                }
                maxBrake = Math.max(maxBrake, t.brakeW);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            ctx.line(String.format("LP wet steam (%s): max braking %.0f W, %.1f s wet, burnt %s; %s", lasting ? "lasting" : "5 s slug", maxBrake,
                t.wetSeconds, t.getBurnt(), t.getWaila()));
            ctx.check("wet steam: it brakes the shaft (over 1 kW)", maxBrake > 1000, String.format("%.0f W", maxBrake));
            if (lasting)
                ctx.check("wet steam: kept up at speed, it strips the blades (water damage)", t.getBurnt() == mods.eln.sim.process.destruct.FailureCause.WATER,
                    "" + t.getBurnt());
            else
                ctx.check("wet steam: a 5 s slug only brakes it (no damage)", !t.isBurnt(), "" + t.getBurnt());
        }
    }

    /**
     * A flooded plant condenser (90 % water): its tubes drown, it cools far less, the vacuum goes, and the LP turbine
     * exhausting into it gets less per kg than into a working one (no bang).
     */
    static final class Flooding extends PlumbingCases.Big {
        ShaftSteamTurbineElement ok, flooded;
        PlantCondenserElement cOk, cFlooded;
        Resistor[] loads = new Resistor[2];
        IProcess feed;

        public String id() {
            return "plant_condenser_flooding_loses_vacuum";
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext ctx) {
            BlockPos a = ctx.at(2, 1, 3), b = ctx.at(2, 1, 7);
            TransparentNodeElement[] la = turbineLine(ctx, LP_TURBINE, a, GENERATOR_8KW);
            TransparentNodeElement[] lb = turbineLine(ctx, LP_TURBINE, b, GENERATOR_8KW);
            ok = (ShaftSteamTurbineElement) la[0];
            flooded = (ShaftSteamTurbineElement) lb[0];
            loads[0] = load((GeneratorElement) la[1], 9000);
            loads[1] = load((GeneratorElement) lb[1], 9000);
            exhaustPipe(ctx, a);
            exhaustPipe(ctx, b);
            cOk = condenser(ctx, a.east().east(), true);
            cFlooded = condenser(ctx, b.east().east(), true);
            var v = cFlooded.v.vessel;
            EaUnits.addWater(v, 0.9 * v.volume * Water.RHO_L, 300, false);
            Eln.simulator.addSlowProcess(feed = dt -> {
                for (var t : new ShaftSteamTurbineElement[]{ok, flooded})
                    if (t.inlet != null && t.inlet.P() < t.spec.nominalPa) EaUnits.addWater(t.inlet, 0.002, Water.tsat(t.spec.nominalPa), true);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            ctx.line("flooding: working " + cOk.getWaila() + " / LP " + ok.getWaila() + "; flooded " + cFlooded.getWaila() + " / LP " + flooded.getWaila());
            ctx.check("flooding: the flooded condenser loses its vacuum (over twice the working one's pressure)",
                cFlooded.v.vessel.P() > 2 * cOk.v.vessel.P(), String.format("%.2f vs %.2f bar", cFlooded.v.vessel.P() / 1e5, cOk.v.vessel.P() / 1e5));
            ctx.check("flooding: its LP turbine gets less per kg (power sags), and nothing bursts",
                flooded.jPerKg() < 0.8 * ok.jPerKg() && !flooded.isBurnt() && !cFlooded.isBurnt(),
                String.format("%.0f vs %.0f kJ/kg", flooded.jPerKg() / 1000, ok.jPerKg() / 1000));
        }
    }

    /**
     * Load rejection (Argon: a dump valve turns it into a triumph): a boiler's worth of steam (12 g/s) into an HP + LP
     * plant at load; at 30 s both loads fall off. The governors shut, the HP inlet's pressure climbs, and a relief valve
     * set at 24 bar from the HP's inlet line round both turbines into the condenser holds it there: no burst, no
     * overspeed.
     */
    static final class DumpOnLoadRejection extends PlumbingCases.Big {
        ShaftSteamTurbineElement hp, lp;
        PlantCondenserElement c;
        ValvePart dump;
        Resistor[] loads = new Resistor[2];
        IProcess feed;
        int tick;
        double maxP, maxRads, maxLift, pBefore;

        public String id() {
            return "shaft_plant_dump_valve_load_rejection";
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            BlockPos hpAt = ctx.at(2, 1, 5), lpAt = ctx.at(4, 1, 5), cp = ctx.at(6, 1, 5);
            TransparentNodeElement[] b = turbineLine(ctx, STEAM_TURBINE, hpAt, GENERATOR_8KW);
            TransparentNodeElement[] l = turbineLine(ctx, LP_TURBINE, lpAt, GENERATOR_8KW);
            hp = (ShaftSteamTurbineElement) b[0];
            lp = (ShaftSteamTurbineElement) l[0];
            loads[0] = load((GeneratorElement) b[1], 3000);
            loads[1] = load((GeneratorElement) l[1], 2500);
            exhaustPipe(ctx, hpAt);
            exhaustPipe(ctx, lpAt);
            c = condenser(ctx, cp, true);
            // the dump line: up from the HP's inlet port, east at y + 2 through the relief valve, down into the
            // condenser's back
            PipeCases.lay(ctx, hpAt.above(), 1, 8, 0, 8, 8, 16, 8);
            PipeCases.lay(ctx, hpAt.above(2), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            for (int x = 3; x <= 6; x++) PipeCases.lay(ctx, ctx.at(x, 3, 5), 1, 0, 8, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(7, 3, 5), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(7, 2, 5), 1, 8, 16, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(7, 1, 5), 1, 8, 16, 8, 8, 8, 8, 0, 8, 8);
            VoxelNode n = PipeCases.node(ctx, ctx.at(4, 3, 5));
            String err = n.addValve(8, 8, 8, ValvePart.PRESSURE);
            if (err != null) throw new IllegalStateException("dump valve: " + err);
            for (var part : n.parts) if (part instanceof ValvePart vp) dump = vp;
            dump.pmode = ValvePart.RELIEF;
            dump.setPa = 24e5;
            dump.flow = 1;   // (east: from the HP's line to the condenser)
            Eln.simulator.addSlowProcess(feed = dt -> {
                if (hp.inlet == null) return;
                // a boiler: 12 g/s of 20 bar saturated steam, whatever the inlet's pressure
                EaUnits.addWater(hp.inlet, 0.012 * dt, Water.tsat(20e5), true);
                tick++;
                if (tick == 600) {
                    pBefore = hp.inlet.P();
                    for (Resistor r : loads) r.setR(1e12);   // (the loads fall off)
                }
                if (tick > 600) {
                    maxP = Math.max(maxP, hp.inlet.P());
                    maxRads = Math.max(maxRads, Math.max(hp.getShaft().getRads(), lp.getShaft().getRads()));
                    maxLift = Math.max(maxLift, dump.lift);
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            ctx.line(String.format("load rejection: HP inlet %.1f bar before, max %.1f after; dump valve up to %.0f%% open; max %.0f rad/s; condenser %s",
                pBefore / 1e5, maxP / 1e5, maxLift * 100, maxRads, c.getWaila()));
            ctx.check("dump: the relief valve opens when the loads fall off", maxLift > 0.05, String.format("%.0f%%", maxLift * 100));
            ctx.check("dump: it holds the HP inlet near its 24 bar (under 27, far from the 60 bar burst)", maxP < 27e5 && !hp.isBurnt(),
                String.format("%.1f bar", maxP / 1e5));
            ctx.check("dump: the shafts stay under the trip's 880 rad/s", maxRads < 880, String.format("%.0f rad/s", maxRads));
        }
    }
}
