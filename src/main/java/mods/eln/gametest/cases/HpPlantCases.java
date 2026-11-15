package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftSteamTurbineElement;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.oil.BoilerElement;
import mods.eln.oil.Oil;
import mods.eln.oil.PlantCondenserElement;
import mods.eln.oil.SteamValveControl;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.transparentnode.FuelHeatFurnaceContainer;
import mods.eln.transparentnode.FuelHeatFurnaceElement;
import net.minecraft.core.BlockPos;

import java.util.List;

import static mods.eln.gametest.cases.ShaftSteamCases.*;
import static mods.eln.gametest.cases.Wp11Cases.*;

/**
 * A furnace-fired HP steam plant from the devices a player builds it with (todo #137): Fuel Heat Furnaces with Big
 * burners on heavy oil against a Boiler's thermal faces, alloy pipe to an HP Steam Turbine with two loaded 8 kW
 * generators, its exhaust into a Plant Steam Condenser (shaft motor on its pump), the condensate piped back to the
 * boiler's feed flange. One boiler takes two furnaces (its third thermal face is its feed flange's), so this is a
 * slice of the 16 kW plant: what it measures per furnace and per boiler scales to the whole by arithmetic. FullPlant
 * is the whole plant with forced air: two boilers, four furnaces.
 */
public final class HpPlantCases {
    private HpPlantCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FurnacePlant("hp_plant_furnace_fired_20bar", 20, 0, 4500, 6880, 3240, 9.9, false));
        cases.add(new FurnacePlant("hp_plant_furnace_fired_10bar", 10, 0, 4500, 6970, 3100, 10.3, false));
        cases.add(new FurnacePlant("hp_plant_furnace_fired_forced_air", 20, 1.6e5, 11000, 16680, 8230, 3.9, false));
        cases.add(new FurnacePlant("hp_plant_boiler_limit_under", 20, 2.07e5, 13000, 19520, 9540, 3.4, false));
        cases.add(new FurnacePlant("hp_plant_boiler_limit_over", 20, 2.6e5, 15000, 0, 0, 0, true));
        cases.add(new FullPlant("hp_plant_full_forced_air_16kw", 4800, false));
        cases.add(new FullPlant("hp_plant_full_15min", 18000, false));
        cases.add(new FullPlant("hp_plant_full_blowers_16kw", 4800, true));
        cases.add(new Flooded(true));
        cases.add(new Flooded(false));
        // the same slice with real Air Blowers (todo #471) in place of the air mains, and a blower alone, its outlet blocked
        var withBlowers = new FurnacePlant("hp_plant_furnace_fired_blowers", 20, 1.6e5, 11000, 16680, 8230, 3.9, false);
        withBlowers.blowers = true;
        cases.add(withBlowers);
        cases.add(new BlowerBlocked());
    }

    static final int AIR_BLOWER = 20 + (84 << 6);

    /** An Air Blower at p on MVU (a source straight on its load), its outlet (its right flank) facing out. */
    static mods.eln.oil.AirBlowerElement blower(TestContext ctx, BlockPos p, Direction out, boolean powered) {
        var b = (mods.eln.oil.AirBlowerElement) ctx.placeTransparent(AIR_BLOWER, p);
        for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) if (d.right() == out) b.front = d;
        b.reconnect();
        if (powered) power(b, Eln.MVU);
        return b;
    }

    static void power(mods.eln.oil.AirBlowerElement b, double u) {
        Eln.simulator.addElectricalComponent(new mods.eln.sim.mna.component.VoltageSource("blower test", b.load, null).setU(u));
    }

    /** An Air Blower with nothing on its outlet: it stalls at its shut-off pressure, moves nothing, draws its idling. Unpowered: nothing. */
    static final class BlowerBlocked extends PlumbingCases.Big {
        mods.eln.oil.AirBlowerElement on, off, half;
        /** Its curve: blowers into air held at these gauge pressures (Pa) on their outlets. */
        static final double[] BACK = {0, 0.5e5, 1.0e5, 1.5e5};
        final mods.eln.oil.AirBlowerElement[] curve = new mods.eln.oil.AirBlowerElement[BACK.length];
        final FluidDevice[] sinks = new FluidDevice[BACK.length];

        public String id() {
            return "air_blower_blocked_outlet_stalls";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            on = blower(ctx, ctx.at(3, 1, 3), Direction.XP, true);
            off = blower(ctx, ctx.at(3, 1, 6), Direction.XP, false);
            half = blower(ctx, ctx.at(6, 1, 3), Direction.XP, false);
            power(half, Eln.MVU / 2);
            for (int i = 0; i < BACK.length; i++) {
                BlockPos p = ctx.at(1 + 2 * i, 1, 8);
                curve[i] = blower(ctx, p, Direction.ZN, true);
                var sink = new FluidNode(1.0, p.getY(), 1.0, true).fill(0, Water.P_ATM + BACK[i], 293.15);
                int face = Direction.ZP.toEnumFacing().get3DDataValue();
                sinks[i] = GasPlantCases.fixtures(curve[i].fluidLevel(), new Object[]{p.relative(Direction.ZN.toEnumFacing()), face, sink,
                    mods.eln.oil.TankElement.sideSpot(face, mods.eln.oil.InlinePumpElement.LINE_Y)});
            }
        }

        public void measure(TestContext ctx) {
            double g = mods.eln.fluid.FluidWorld.nodeFor(on.outlet).P() - Water.P_ATM, g0 = mods.eln.fluid.FluidWorld.nodeFor(off.outlet).P() - Water.P_ATM;
            ctx.line("air blower, blocked: " + on.getWaila() + String.format("; wants %.1f W, %.3f g/s, %.3f g in all", on.wantW, on.kgPerS * 1000, on.totalKg * 1000)
                + "; unpowered: " + off.getWaila());
            double shut = mods.eln.oil.AirBlowerDescriptor.SHUTOFF_PA;
            double gh = mods.eln.fluid.FluidWorld.nodeFor(half.outlet).P() - Water.P_ATM;
            ctx.check("blower: at half its voltage it stalls at a quarter of its shut-off pressure (within 5 %)", Math.abs(gh - 0.25 * shut) < 0.05 * 0.25 * shut,
                String.format("%.3f bar gauge, %.1f W", gh / 1e5, half.wantW));
            for (int i = 0; i < BACK.length; i++) {
                var b = curve[i];
                mods.eln.fluid.FluidWorld.remove(sinks[i]);
                var in = b.intake;
                double rise = mods.eln.fluid.FluidWorld.nodeFor(b.outlet).P() - in.P(), rho = in.P() * 0.02897 / (8.314 * in.T());
                double expectKg = rho * mods.eln.oil.AirBlowerDescriptor.FREE_M3_S * Math.sqrt(Math.max(0, 1 - rise / shut));
                double expectW = b.kgPerS / rho * rise / mods.eln.oil.AirBlowerDescriptor.MOTOR_ETA + mods.eln.oil.AirBlowerDescriptor.BLOWER_IDLE_W;
                ctx.line(String.format("air blower curve: against %.2f bar gauge: outlet %.3f bar over its inlet, %.1f g/s (%.4f m^3/s of inlet air; parabola %.1f g/s), %.0f W (flow x rise / motor + idle: %.0f W)",
                    BACK[i] / 1e5, rise / 1e5, b.kgPerS * 1000, b.kgPerS / rho, expectKg * 1000, b.wantW, expectW));
                ctx.check("blower curve at " + BACK[i] / 1e5 + " bar: its flow is on its parabola (within 10 %)", Math.abs(b.kgPerS - expectKg) < 0.1 * expectKg, String.format("%.1f of %.1f g/s", b.kgPerS * 1000, expectKg * 1000));
                ctx.check("blower curve at " + BACK[i] / 1e5 + " bar: it draws the work on the air over its motor's efficiency, plus idling (within 10 %)",
                    Math.abs(b.wantW - expectW) < 0.1 * expectW, String.format("%.0f of %.0f W", b.wantW, expectW));
            }
            ctx.check("blower: blocked, it stalls at its shut-off pressure (within 5 %)", Math.abs(g - shut) < 0.05 * shut, String.format("%.3f bar gauge", g / 1e5));
            ctx.check("blower: that is under the furnace tank's relief (3 bar) and burst (6 bar)", g < 3e5, String.format("%.3f bar gauge", g / 1e5));
            ctx.check("blower: stalled, it moves no air (under 0.05 g/s)", on.kgPerS < 5e-5, String.format("%.4f g/s", on.kgPerS * 1000));
            ctx.check("blower: stalled, it draws its idling only (under 1.5 x)", on.wantW > 1 && on.wantW < 1.5 * mods.eln.oil.AirBlowerDescriptor.BLOWER_IDLE_W, String.format("%.1f W", on.wantW));
            ctx.check("blower: unpowered, no pressure and no power", Math.abs(g0) < 500 && off.wantW == 0, String.format("%.0f Pa, %.1f W", g0, off.wantW));
        }
    }

    /**
     * Real flooding still strips an HP turbine's blades (#474 took away only its own wet exhaust): a turbine at speed
     * on rated steam runs five seconds unharmed, then water is held at 20 % of its inlet (carried over from a boiler run
     * too full) or of its exhaust (backed up from a flooded condenser: more than its steam carries off), and it fails
     * on water.
     */
    static final class Flooded extends PlumbingCases.Big {
        final boolean inlet;
        ShaftSteamTurbineElement t;
        IProcess feed;
        int tick;
        boolean cleanOk;
        double cleanWet;

        Flooded(boolean inlet) {
            this.inlet = inlet;
        }

        public String id() {
            return inlet ? "hp_turbine_water_in_inlet_strips_blades" : "hp_turbine_flooded_exhaust_strips_blades";
        }

        public int ticks() {
            return 500;
        }

        public void build(TestContext ctx) {
            TransparentNodeElement[] l = ShaftPlantCases.turbineLine(ctx, STEAM_TURBINE, ctx.at(5, 1, 5), FLYWHEEL);
            t = (ShaftSteamTurbineElement) l[0];
            Eln.simulator.addSlowProcess(feed = dt -> {
                if (t.inlet == null) return;
                if (t.isBurnt()) {
                    if (tick < 100) cleanOk = false;
                    return;
                }
                tick++;
                if (tick < 3) t.getShaft().setRads(800);
                double pa = t.spec.nominalPa;
                if (t.inlet.P() < pa) EaUnits.addWater(t.inlet, 0.002, Water.tsat(pa), true);
                if (tick < 100) {
                    cleanOk = true;
                    cleanWet = Math.max(cleanWet, t.wetShare());
                } else {
                    FluidNode n = inlet ? t.inlet : t.exhaust;
                    double want = 0.2 * n.volume * Water.RHO_L - n.mLiquid();
                    if (want > 0) EaUnits.addWater(n, want, n.T(), false);
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(feed);
            String id = id();
            ctx.line(String.format("%s: wettest in 5 s of clean running %.2f %%; after %d ticks: %s; carried out of the exhaust %.1f g; %s", id, cleanWet * 100, tick, t.getBurnt(),
                t.totalCarriedKg * 1000, t.getWaila()));
            ctx.check(id + ": five seconds on clean steam do no harm (water under 2 %)", cleanOk && cleanWet < 0.02, String.format("%.2f %%", cleanWet * 100));
            ctx.check(id + ": water held at 20 % of its " + (inlet ? "inlet" : "exhaust") + " strips the blades (water damage)",
                t.getBurnt() == mods.eln.sim.process.destruct.FailureCause.WATER, "" + t.getBurnt());
        }
    }

    /** A Fuel Heat Furnace at p, its fuel flange towards front (its thermal face is its back), a Big burner in it, lit,
     *  at full setting, with mB of heavy oil in its tank. */
    static FuelHeatFurnaceElement furnace(TestContext ctx, BlockPos p, Direction front, int mB) {
        var f = (FuelHeatFurnaceElement) ctx.placeTransparent(Wp10bCases.FUEL_HEAT_FURNACE, p);
        f.front = front;
        f.reconnect();
        f.getInventory().setItem(FuelHeatFurnaceContainer.Companion.getFuelBurnerSlot(), Eln.sharedItemStackOne.getDescriptor(2 + (124 << 6)).newItemStack());
        f.inventoryChange(f.getInventory());
        f.networkUnserialize(ctx.stream(o -> o.writeByte(FuelHeatFurnaceElement.Companion.getMainSwitchToggleEvent())));
        f.networkUnserialize(ctx.stream(o -> {
            o.writeByte(FuelHeatFurnaceElement.Companion.getSetManualControlValueEvent());
            o.writeFloat(1f);
        }));
        f.putFuel(Oil.Cut.HEAVYOIL.source.get(), mB);
        return f;
    }

    static final class FurnacePlant extends PlumbingCases.Big {
        static final int WATCH = 3000, TICKS = 4800;
        final String id;
        final double bar, airPa, loadW, expectFurnaceW, expectShaftW, expectFurnaces16;
        final boolean expectTrip;
        /** The tick the boiler failed at (-1: it didn't), and what it showed just before: its boiling (W), shell (K over ambient), pressure (bar), fill (0..1). */
        int tripTick = -1;
        double lastBoilW, lastShell, lastBar, lastFill;
        BoilerElement boiler;
        final FuelHeatFurnaceElement[] f = new FuelHeatFurnaceElement[2];
        final ThermalLoad[] fT = new ThermalLoad[2];
        final FluidDevice[] air = new FluidDevice[2];
        final FluidNode[] mains = new FluidNode[2];
        /** Real Air Blowers on the fuel flanges in place of the mains; their power, air and outlet gauge summed over the window. */
        boolean blowers;
        final mods.eln.oil.AirBlowerElement[] blow = new mods.eln.oil.AirBlowerElement[2];
        IProcess blowClock;
        double blowT, blowS, blowJ, blowKg, blowPaS;
        ShaftSteamTurbineElement hp;
        PlantCondenserElement c;
        final Resistor[] loads = new Resistor[2];
        IProcess clock;
        FluidDevice meter;
        int tick;
        boolean counting;
        // over the watch: sums (x dt) and counters' starts
        double window, waterJ, meterS, elecJ, pSum, shellSum, shellMax, radsSum, condSum, overSum, gaugeSum, fillLo = 1e9, fillHi, floodHi;
        final double[] furnaceSum = new double[2], furnaceMax = new double[2], given0 = new double[2], kg0 = new double[2], mb0 = new double[2];
        double steam0, work0, pumped0, boilerKg0;

        /**
         * @param bar              the turbine's pressure regulator's setting
         * @param airPa            air on the furnaces' fuel flanges (Pa gauge at the main; 0: natural draught)
         * @param loadW            the two generators' resistors together, at their nominal volts
         * @param expectFurnaceW   heat into the water per furnace, pinned within 10 % (0: not pinned)
         * @param expectShaftW     the turbine's shaft power, pinned within 10 %
         * @param expectFurnaces16 furnaces fired like this for 16 kW on the shaft, by proportion, pinned within 12 %
         * @param expectTrip       the boiler is fired past its limit: its shell's watchdog fails it
         */
        FurnacePlant(String id, double bar, double airPa, double loadW, double expectFurnaceW, double expectShaftW, double expectFurnaces16, boolean expectTrip) {
            this.id = id;
            this.bar = bar;
            this.airPa = airPa;
            this.loadW = loadW;
            this.expectFurnaceW = expectFurnaceW;
            this.expectShaftW = expectShaftW;
            this.expectFurnaces16 = expectFurnaces16;
            this.expectTrip = expectTrip;
        }

        public String id() {
            return id;
        }

        public int ticks() {
            return TICKS;
        }

        public boolean checkLimitCycles() {
            return false;   // (burner, regulator and feed float each hunt a little: the averages are what is pinned)
        }

        public void build(TestContext ctx) {
            BlockPos bp = ctx.at(2, 1, 5), tp = ctx.at(5, 1, 5), cp = ctx.at(6, 1, 5);
            boiler = (BoilerElement) ctx.placeTransparent(8 + (82 << 6), bp);
            boiler.front = Direction.XN;   // (feed flange south; thermal faces north, east and south)
            boiler.reconnect();
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            // two furnaces, their backs against the boiler's north and east faces
            f[0] = furnace(ctx, bp.north(), Direction.ZN, 1900);
            f[1] = furnace(ctx, bp.east(), Direction.XP, 1900);
            for (int i = 0; i < 2; i++) fT[i] = Batch1Cases.<ThermalLoad>field(f[i], "thermalLoad");
            if (blowers) {
                for (int i = 0; i < 2; i++) blow[i] = blower(ctx, f[i].node.coordonate.pos().relative(f[i].front.toEnumFacing()), f[i].front.back(), true);
                Eln.simulator.addSlowProcess(blowClock = dt -> {
                    blowT += dt;
                    if (blowT < WATCH / 20.0) return;
                    blowS += dt;
                    for (var b : blow) {
                        blowJ += b.wantW * dt;
                        blowKg += b.kgPerS * dt;
                        blowPaS += (mods.eln.fluid.FluidWorld.nodeFor(b.outlet).P() - Water.P_ATM) * dt;
                    }
                });
            } else if (airPa > 0) {
                // (the Air Blower that doesn't exist yet: an air main on each fuel flange, kept at its pressure)
                double y = bp.getY();
                for (int i = 0; i < 2; i++) air[i] = OilCases.FurnacePressure.onFlange(f[i], mains[i] = OilCases.FurnacePressure.main(y, airPa, false));
            }
            // steam: alloy pipe up out of the boiler, east over the furnace, down into the turbine's inlet
            PipeCases.lay(ctx, ctx.at(2, 2, 5), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(3, 2, 5), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(4, 2, 5), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(5, 2, 5), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            TransparentNodeElement[] line = ShaftPlantCases.turbineLine(ctx, STEAM_TURBINE, tp, GENERATOR_8KW, GENERATOR_8KW);
            hp = (ShaftSteamTurbineElement) line[0];
            loads[0] = load((GeneratorElement) line[1], loadW / 2);
            loads[1] = load((GeneratorElement) line[2], loadW / 2);
            // (the player's pressure regulator in the turbine: it opens the valve as the boiler passes its set pressure;
            // the load is a little more than the steam gives, so the governor stays open and the regulator has the valve)
            hp.controls.inventory.setItem(SteamValveControl.REGULATOR_SLOT, Eln.sharedItem.getDescriptor(8 + (3 << 6)).newItemStack());
            hp.controls.targetBar = bar;
            hp.controls.computeInventory();
            // the condenser's back against the turbine's exhaust, a powered shaft motor on its pump
            c = ShaftPlantCases.condenser(ctx, cp, true, Direction.XP);
            // condensate: alloy pipe out of the condenser's front (east), south, west along z 7, north into the boiler's feed flange
            PipeCases.lay(ctx, ctx.at(7, 1, 5), 1, 0, 4, 8, 8, 4, 8, 8, 8, 8, 8, 8, 16);   // (its flange is low on the face)
            PipeCases.lay(ctx, ctx.at(7, 1, 6), 1, 8, 8, 0, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(7, 1, 7), 1, 8, 8, 0, 8, 8, 8, 0, 8, 8);
            for (int x = 6; x >= 3; x--) PipeCases.lay(ctx, ctx.at(x, 1, 7), 1, 16, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(2, 1, 7), 1, 16, 8, 8, 8, 8, 8, 8, 8, 0);
            PipeCases.lay(ctx, ctx.at(2, 1, 6), 1, 8, 8, 16, 8, 8, 0);
            meter = new FluidDevice() {
                public Object fluidLevel() {
                    return boiler.fluidLevel();
                }

                public List<FluidPort> fluidPorts() {
                    return List.of();
                }

                public void afterFluidStep(double dt) {
                    if (!counting || boiler.vessel == null) return;
                    waterJ += boiler.vessel.wallJ;
                    meterS += dt;
                }
            };
            mods.eln.fluid.FluidWorld.add(meter);
            Eln.simulator.addSlowProcess(clock = dt -> {
                if (c.v.vessel == null || boiler.vessel == null) return;
                if (tick == 0) {   // (as a plant that has run: water in the hotwell, in the pump's level band, and the feed run full)
                    EaUnits.addWater(c.v.vessel, 0.28 * c.v.vessel.volume * Water.RHO_L, 320, false);
                    var run = mods.eln.fluid.FluidWorld.nodeFor(boiler.feed);
                    run.fill(0.995 * run.volume * Water.RHO_L, Water.P_ATM, 300);
                    run.update();
                }
                tick++;
                if (tripTick < 0 && boiler.isBurnt()) tripTick = tick;
                if (tripTick < 0) {
                    lastBoilW = boiler.boilW;
                    lastShell = boiler.thermal.Tc;
                    lastBar = boiler.vessel.P() / 1e5;
                    lastFill = boiler.fill();
                }
                for (FluidNode m : mains) if (m != null) m.fill(0, Water.P_ATM + airPa, 293.15);
                if (tick == WATCH) {
                    counting = true;
                    for (int i = 0; i < 2; i++) {
                        given0[i] = f[i].getHeatGivenJ();
                        kg0[i] = f[i].getFuelBurntKg();
                        mb0[i] = f[i].fuelMb();
                    }
                    steam0 = hp.totalKg;
                    work0 = hp.totalWork;
                    pumped0 = c.v.totalPumpedKg;
                    boilerKg0 = boiler.vessel.mLiquid();
                }
                if (tick > WATCH) {
                    window += dt;
                    for (Resistor r : loads) elecJ += r.getP() * dt;
                    pSum += boiler.vessel.P() * dt;
                    shellSum += boiler.thermal.Tc * dt;
                    shellMax = Math.max(shellMax, boiler.thermal.Tc);
                    radsSum += shaft(hp).getRads() * dt;
                    condSum += c.v.vessel.P() * dt;
                    for (int i = 0; i < 2; i++) {
                        furnaceSum[i] += fT[i].Tc * dt;
                        furnaceMax[i] = Math.max(furnaceMax[i], fT[i].Tc);
                        overSum += f[i].overfire() * dt / 2;
                        gaugeSum += (f[i].getVessel() == null ? 0 : f[i].getVessel().P() - Water.P_ATM) * dt / 2;
                    }
                    fillLo = Math.min(fillLo, boiler.fill());
                    fillHi = Math.max(fillHi, boiler.fill());
                    floodHi = Math.max(floodHi, c.floodShare());
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            mods.eln.fluid.FluidWorld.remove(meter);
            for (FluidDevice d : air) if (d != null) mods.eln.fluid.FluidWorld.remove(d);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            if (blowers) {
                Eln.simulator.removeSlowProcess(blowClock);
                double s = Math.max(1e-9, blowS);
                ctx.line(String.format("%s: BLOWERS (2, one a furnace): %.0f W each, %.2f g/s of air each, %.3f bar gauge at their outlets; ", id, blowJ / s / 2, blowKg / s / 2 * 1000, blowPaS / s / 2 / 1e5)
                    + blow[0].getWaila() + " / " + blow[1].getWaila());
                ctx.check(id + ": the blowers feed the furnaces' tanks (over 1 bar gauge at their outlets)", blowPaS / s / 2 > 1e5, String.format("%.3f bar", blowPaS / s / 2 / 1e5));
            }
            double w = Math.max(1e-9, window), fire = 0, kg = 0, mB = 0;
            for (int i = 0; i < 2; i++) {
                fire += (f[i].getHeatGivenJ() - given0[i]) / w;
                kg += (f[i].getFuelBurntKg() - kg0[i]) / w;
                mB += (mb0[i] - f[i].fuelMb()) / w;
            }
            double water = waterJ / Math.max(1e-9, meterS), steam = (hp.totalKg - steam0) / w, shaftW = (hp.totalWork - work0) / w, elec = elecJ / w;
            double pumped = (c.v.totalPumpedKg - pumped0) / w, p = pSum / w, shell = shellSum / w, over = overSum / w;
            double perFurnace = water / 2, share = water / Math.max(1e-9, fire), perKg = shaftW / Math.max(1e-9, steam);
            // what a whole 16 kW plant takes at this slice's numbers: heat by shaft power, furnaces and fuel by heat
            double heat16 = water * 16e3 / Math.max(1e-9, shaftW), furnaces16 = heat16 / Math.max(1e-9, perFurnace), bucketsH = mB * 3.6 / 2;
            // the boiler's own limit at this pressure: its shell's watchdog (400 K over ambient) through 0.005 K/W into the water
            double boilerLimit = shell <= 0 ? 0 : water * (400 - (shell - water * BoilerElement.SHELL_TO_WATER_K_PER_W)) / (water * BoilerElement.SHELL_TO_WATER_K_PER_W);
            ctx.line(String.format("%s: fire %.2f kW (2 furnaces, overfire x%.2f, tank %.2f bar gauge), %.2f kW into the water (%.2f kW a furnace, %.1f %% of the fire); "
                    + "boiler %.2f bar, shell %.0f K over ambient (max %.0f), fill %.0f..%.0f %%; furnaces %.0f / %.0f K over ambient (max %.0f / %.0f); "
                    + "steam %.2f g/s, shaft %.2f kW (%.0f kJ/kg), electric %.2f kW, %.0f rad/s; condenser %.3f bar, up to %.0f %% full, pumped %.2f g/s; "
                    + "boiler water %+.1f g over %.0f s; fuel %.3f g/s = %.2f mB/s = %.2f buckets/h a furnace; coke %.3f / %.3f",
                id, fire / 1000, over, gaugeSum / w / 1e5, water / 1000, perFurnace / 1000, share * 100,
                p / 1e5, shell, shellMax, fillLo * 100, fillHi * 100, furnaceSum[0] / w, furnaceSum[1] / w, furnaceMax[0], furnaceMax[1],
                steam * 1000, shaftW / 1000, perKg / 1000, elec / 1000, radsSum / w, condSum / w / 1e5, floodHi * 100, pumped * 1000,
                (boiler.vessel == null ? 0 : boiler.vessel.mLiquid() - boilerKg0) * 1000, w, kg * 1000, mB, bucketsH, f[0].getCoke(), f[1].getCoke()));
            ctx.line(String.format("%s: scaled to 16 kW on the shaft: %.1f kW into the water, %.1f furnaces at this firing, %.1f buckets/h of heavy oil; "
                    + "one boiler's limit at this pressure %.1f kW (%.1f such furnaces)",
                id, heat16 / 1000, furnaces16, furnaces16 * bucketsH, boilerLimit / 1000, boilerLimit / Math.max(1e-9, perFurnace)));
            ctx.line(id + ": boiler " + boiler.getWaila() + "; furnace " + f[0].getWaila() + "; HP " + hp.getWaila() + "; condenser " + c.getWaila());
            if (expectTrip) {
                // fired past what its shell can pass into the water: the shell passes its 400 K and, once its watchdog's
                // budget (400 K s over the limit) is spent, the boiler fails
                double limit = (400 - (Water.tsat(bar * 1e5) - 293.15)) / BoilerElement.SHELL_TO_WATER_K_PER_W;
                ctx.line(String.format("%s: the boiler failed at tick %d, boiling %.1f kW at %.1f bar, its shell %.0f K over ambient (limit by its constants %.1f kW)",
                    id, tripTick, lastBoilW / 1000, lastBar, lastShell, limit / 1000));
                ctx.check(id + ": fired past its limit, the boiler fails", boiler.isBurnt() && tripTick > 0, "tick " + tripTick);
                ctx.check(id + ": it failed on its shell's temperature (400-430 K over ambient), full of water", lastShell > 400 && lastShell < 430 && lastFill > 0.9,
                    String.format("%.0f K, %.0f %% full", lastShell, lastFill * 100));
                ctx.check(id + ": it was boiling over its limit, by under 15 %, when it failed", lastBoilW > limit && lastBoilW < 1.15 * limit,
                    String.format("%.1f kW of %.1f", lastBoilW / 1000, limit / 1000));
                return;
            }
            boolean joined = false;
            for (var q : c.fluidPorts()) joined |= mods.eln.fluid.FluidWorld.networkOf(q.node()) == mods.eln.fluid.FluidWorld.networkOf(boiler.feed);
            ctx.check(id + ": the condenser's pump feeds the boiler (one network)", joined && mods.eln.fluid.FluidWorld.networkOf(boiler.feed) != null, "");
            ctx.check(id + ": nothing burnt out", !boiler.isBurnt() && !f[0].isBurnt() && !f[1].isBurnt() && !hp.isBurnt(),
                String.format("boiler %b, furnaces %b %b, turbine %b", boiler.isBurnt(), f[0].isBurnt(), f[1].isBurnt(), hp.isBurnt()));
            ctx.check(id + ": the boiler holds its pressure (within 1.5 bar of the regulator's)", Math.abs(p / 1e5 - bar) < 1.5, String.format("%.2f bar", p / 1e5));
            ctx.check(id + ": the boiler neither boils dry nor overfills", fillLo > 0.3 && fillHi < 1.02, String.format("%.0f..%.0f %%", fillLo * 100, fillHi * 100));
            ctx.check(id + ": the condensate goes back (90 %+ of the steam)", steam > 1e-3 && pumped > 0.9 * steam, String.format("%.2f of %.2f g/s", pumped * 1000, steam * 1000));
            ctx.check(id + ": the condenser holds a vacuum (under 0.3 bar)", condSum / w < 0.3e5, String.format("%.3f bar", condSum / w / 1e5));
            // the balance, pinned: a change to burners, furnace or boiler shells, the turbine or the condenser that moves
            // what a 16 kW plant takes shows here
            ctx.check(id + ": the fire is the burners' (10 kW each x their overfire, less coke; within 6 %)",
                Math.abs(fire / (2 * 10e3 * over * (1 - (f[0].getCoke() + f[1].getCoke()) / 2)) - 1) < 0.06, String.format("%.2f kW at x%.2f", fire / 1000, over));
            ctx.check(id + ": 65-78 % of the fire reaches the water", share > 0.65 && share < 0.78, String.format("%.1f %%", share * 100));
            if (expectFurnaceW > 0) {
                ctx.check(id + String.format(": heat into the water per furnace within 10 %% of %.2f kW", expectFurnaceW / 1000), Math.abs(perFurnace / expectFurnaceW - 1) < 0.10,
                    String.format("%.2f kW", perFurnace / 1000));
                ctx.check(id + String.format(": shaft power within 10 %% of %.2f kW", expectShaftW / 1000), Math.abs(shaftW / expectShaftW - 1) < 0.10,
                    String.format("%.2f kW", shaftW / 1000));
                ctx.check(id + String.format(": furnaces for 16 kW, by proportion, within 12 %% of %.1f", expectFurnaces16), Math.abs(furnaces16 / expectFurnaces16 - 1) < 0.12,
                    String.format("%.1f", furnaces16));
            }
            // (the shell over the water by the heat through 0.005 K/W: the boiler's limit follows from its 400 K watchdog)
            double wall = shell - water * BoilerElement.SHELL_TO_WATER_K_PER_W - (Water.tsat(p) - 293.15);
            ctx.check(id + ": the shell stands over the boiling water by the heat through its wall (within 8 K), and under its 400 K limit",
                Math.abs(wall) < 8 && shellMax < 400, String.format("%+.1f K off, shell max %.0f K", wall, shellMax));
            ctx.check(id + ": the generators deliver most of the shaft power (over 80 %)", elec > 0.8 * shaftW && elec < shaftW, String.format("%.2f of %.2f kW", elec / 1000, shaftW / 1000));
        }
    }

    /**
     * The whole 16 kW plant with forced air: two Boilers on one steam header and one feed line, two Big-burner furnaces
     * on each with air at 1.6 bar gauge on their fuel flanges (the Air Blower that doesn't exist yet), the HP turbine
     * with two loaded 8 kW generators, the Plant Condenser with cooling water through its tubes, its pump returning
     * the condensate to both boilers. Rated shaft power is reached, not scaled to.
     */
    static final class FullPlant extends PlumbingCases.Big {
        static final int WATCH = 3000;
        final String id;
        final int ticks;
        /** The long run (15 minutes: #474, the turbine must not flood on its own exhaust): refuelled as a player would, and
         *  pinned on what must hold over it, not on the four-minute plant's ratings (the burners coke). */
        final boolean endurance;
        double wetMax;

        static final double BAR = 20, AIR_PA = 1.6e5, LOAD_W = 22000;
        final BoilerElement[] b = new BoilerElement[2];
        final FuelHeatFurnaceElement[] f = new FuelHeatFurnaceElement[4];
        final ThermalLoad[] fT = new ThermalLoad[4];
        final FluidDevice[] air = new FluidDevice[4];
        final FluidNode[] mains = new FluidNode[4];
        /**
         * Real Air Blowers (one a furnace) in place of the air mains, slowed to BLOWER_SPEED so their outlets stand at the
         * mains' 1.6 bar with today's furnaces' air (126 g/s each); their power, air and outlet gauge summed over the window.
         */
        final boolean blowers;
        static final double BLOWER_SPEED = 0.932;
        final mods.eln.oil.AirBlowerElement[] blow = new mods.eln.oil.AirBlowerElement[4];
        IProcess blowClock;
        double blowT, blowS, blowJ, blowKg, blowPaS;

        FullPlant(String id, int ticks, boolean blowers) {
            this.id = id;
            this.ticks = ticks;
            this.blowers = blowers;
            endurance = ticks > 4800;
        }
        ShaftSteamTurbineElement hp;
        PlantCondenserElement c;
        FluidDevice loop, meter;
        final Resistor[] loads = new Resistor[2];
        IProcess clock;
        int tick;
        boolean counting;
        double window, meterS, elecJ, radsSum, condSum, furnaceMax, fillLo = 1e9, fillHi, floodHi;
        final double[] waterJ = new double[2], pSum = new double[2], shellMax = new double[2];
        double fire0, mb0, steam0, work0, pumped0, tubes0, boilerKg0, hotwell0, ejected0, taken0;
        double[] inv0;
        /** The tick the turbine failed at (-1: it didn't), and the water in its exhaust then (share of its volume). */
        int hpTrip = -1;
        final StringBuilder wetLog = new StringBuilder();
        double wetLast, refuelled, refuelled0, all0, allEnd, drained0, carried0;

        public String id() {
            return id;
        }

        public int ticks() {
            return ticks;
        }

        public boolean checkLimitCycles() {
            return false;   // (as the slices: burners, regulator and feed floats each hunt a little)
        }

        double fire() {
            double j = 0;
            for (var x : f) j += x.getHeatGivenJ();
            return j;
        }

        double fuelMb() {
            double m = 0;
            for (var x : f) m += x.fuelMb();
            return m;
        }

        /** Water (kg) in the plant's vessels, as the fluid core holds them: hotwell, pump outlet, makeup stub, turbine inlet and exhaust, boilers, feed runs. */
        /** All the water (kg, liquid and steam) in every node of the plant's fluid networks: pipes too. */
        double allWater() {
            java.util.Set<mods.eln.sim.fluid.FluidNetwork> nets = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (FluidNode n : new FluidNode[]{hp.inlet, hp.exhaust, c.v.vessel, c.v.outlet, b[0].vessel, b[0].feed, b[1].vessel, b[1].feed}) {
                var net = mods.eln.fluid.FluidWorld.networkOf(n);
                if (net != null) nets.add(net);
            }
            // (not the cooling water: the condenser's tubes and the test's supply and drain on them)
            java.util.Set<FluidNode> cooling = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            cooling.add(c.tubes);
            for (var port : loop.fluidPorts()) cooling.add(port.node());
            double m = 0;
            for (var net : nets) for (FluidNode n : net.nodes) if (!cooling.contains(n)) m += n.mWater;
            return m;
        }

        double[] inventory() {
            FluidNode[] n = {c.v.vessel, c.v.outlet, c.v.inlet, hp.inlet, hp.exhaust, b[0].vessel, b[1].vessel, b[0].feed, b[1].feed};
            double[] out = new double[n.length];
            java.util.Set<FluidNode> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (int i = 0; i < n.length; i++) {
                FluidNode r = n[i] == null ? null : mods.eln.fluid.FluidWorld.nodeFor(n[i]);
                if (r == null) r = n[i];
                out[i] = r == null || !seen.add(r) ? 0 : r.mWater;
            }
            return out;
        }

        double boilerKg() {
            return b[0].vessel.mLiquid() + b[1].vessel.mLiquid();
        }

        public void build(TestContext ctx) {
            BlockPos a = ctx.at(2, 1, 3), bb = ctx.at(2, 1, 6), tp = ctx.at(5, 1, 4), cp = ctx.at(6, 1, 4);
            // boiler A's feed flange faces south, B's north: one feed pipe between them; two thermal faces left on each
            for (int i = 0; i < 2; i++) {
                b[i] = (BoilerElement) ctx.placeTransparent(8 + (82 << 6), i == 0 ? a : bb);
                b[i].front = i == 0 ? Direction.XN : Direction.XP;
                b[i].reconnect();
                b[i].thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
                b[i].fromTanks(8000, 0);
                b[i].showTanks();
            }
            f[0] = furnace(ctx, a.north(), Direction.ZN, 1900);
            f[1] = furnace(ctx, a.east(), Direction.XP, 1900);
            f[2] = furnace(ctx, bb.west(), Direction.XN, 1900);
            f[3] = furnace(ctx, bb.south(), Direction.ZP, 1900);
            for (int i = 0; i < 4; i++) {
                fT[i] = Batch1Cases.<ThermalLoad>field(f[i], "thermalLoad");
                if (blowers) {
                    blow[i] = blower(ctx, f[i].node.coordonate.pos().relative(f[i].front.toEnumFacing()), f[i].front.back(), false);
                    power(blow[i], BLOWER_SPEED * Eln.MVU);
                } else air[i] = OilCases.FurnacePressure.onFlange(f[i], mains[i] = OilCases.FurnacePressure.main(a.getY(), AIR_PA, false));
            }
            if (blowers) Eln.simulator.addSlowProcess(blowClock = dt -> {
                blowT += dt;
                if (blowT < WATCH / 20.0) return;
                blowS += dt;
                for (var bl : blow) {
                    blowJ += bl.wantW * dt;
                    blowKg += bl.kgPerS * dt;
                    blowPaS += (mods.eln.fluid.FluidWorld.nodeFor(bl.outlet).P() - Water.P_ATM) * dt;
                }
            });
            // steam header: up out of each boiler, along x 2 over the feed pipe, a tee east to the turbine's inlet
            PipeCases.lay(ctx, ctx.at(2, 2, 3), 1, 8, 0, 8, 8, 8, 8, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(2, 2, 4), 1, 8, 8, 0, 8, 8, 16);
            String err = PipeCases.node(ctx, ctx.at(2, 2, 4)).addJoined(new mods.eln.node.voxel.PipePart(1, new int[]{8, 8, 8, 16, 8, 8}), 8, 8, 8);
            if (err != null) throw new IllegalStateException("steam tee: " + err);
            PipeCases.lay(ctx, ctx.at(2, 2, 5), 1, 8, 8, 0, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(2, 2, 6), 1, 8, 8, 0, 8, 8, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(3, 2, 4), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(4, 2, 4), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(5, 2, 4), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            TransparentNodeElement[] line = ShaftPlantCases.turbineLine(ctx, STEAM_TURBINE, tp, GENERATOR_8KW, GENERATOR_8KW);
            hp = (ShaftSteamTurbineElement) line[0];
            loads[0] = load((GeneratorElement) line[1], LOAD_W / 2);
            loads[1] = load((GeneratorElement) line[2], LOAD_W / 2);
            hp.controls.inventory.setItem(SteamValveControl.REGULATOR_SLOT, Eln.sharedItem.getDescriptor(8 + (3 << 6)).newItemStack());
            hp.controls.targetBar = BAR;
            hp.controls.computeInventory();
            c = ShaftPlantCases.condenser(ctx, cp, true, Direction.XP);
            // (front XP: its left is south, its right north; cooling water from a 2 bar supply through its tubes)
            loop = ShaftPlantCases.coolingLoop(c, cp.south(), 2, cp.north(), 3, 2e5);
            // condensate: out of the condenser's front (east), south, west along z 6, north, west into a tee between the feed flanges
            PipeCases.lay(ctx, ctx.at(7, 1, 4), 1, 0, 4, 8, 8, 4, 8, 8, 8, 8, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(7, 1, 5), 1, 8, 8, 0, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(7, 1, 6), 1, 8, 8, 0, 8, 8, 8, 0, 8, 8);
            for (int x = 6; x >= 4; x--) PipeCases.lay(ctx, ctx.at(x, 1, 6), 1, 16, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(3, 1, 6), 1, 16, 8, 8, 8, 8, 8, 8, 8, 0);
            PipeCases.lay(ctx, ctx.at(3, 1, 5), 1, 8, 8, 16, 8, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(2, 1, 5), 1, 8, 8, 0, 8, 8, 16);
            err = PipeCases.node(ctx, ctx.at(2, 1, 5)).addJoined(new mods.eln.node.voxel.PipePart(1, new int[]{8, 8, 8, 16, 8, 8}), 8, 8, 8);
            if (err != null) throw new IllegalStateException("feed tee: " + err);
            PipeCases.lay(ctx, ctx.at(2, 1, 4), 1, 8, 8, 16, 8, 8, 0);
            meter = new FluidDevice() {
                public Object fluidLevel() {
                    return b[0].fluidLevel();
                }

                public List<FluidPort> fluidPorts() {
                    return List.of();
                }

                public void afterFluidStep(double dt) {
                    if (!counting || b[0].vessel == null || b[1].vessel == null) return;
                    for (int i = 0; i < 2; i++) waterJ[i] += b[i].vessel.wallJ;
                    meterS += dt;
                }
            };
            mods.eln.fluid.FluidWorld.add(meter);
            Eln.simulator.addSlowProcess(clock = dt -> {
                if (c.v.vessel == null || b[0].vessel == null || b[1].vessel == null) return;
                if (tick == 0) {
                    EaUnits.addWater(c.v.vessel, 0.28 * c.v.vessel.volume * Water.RHO_L, 320, false);
                    for (int i = 0; i < 2; i++) {
                        var run = mods.eln.fluid.FluidWorld.nodeFor(b[i].feed);
                        run.fill(0.995 * run.volume * Water.RHO_L, Water.P_ATM, 300);
                        run.update();
                    }
                }
                tick++;
                for (FluidNode m : mains) if (m != null) m.fill(0, Water.P_ATM + AIR_PA, 293.15);
                if (hpTrip < 0 && hp.isBurnt()) hpTrip = tick;
                if (hpTrip < 0) wetLast = hp.wetShare();
                if (tick == ticks - 2) allEnd = allWater();
                if (tick > 600) wetMax = Math.max(wetMax, hp.exhaust.mLiquid() / Water.RHO_L / hp.exhaust.volume);
                if (endurance && tick % 1200 == 0) for (var x : f) if (x.fuelMb() < 1000) {
                    x.putFuel(Oil.Cut.HEAVYOIL.source.get(), 900);
                    refuelled += 900;
                }
                if (tick % 1200 == 0) wetLog.append(String.format(" %d: %.1f %%", tick, hp.wetShare() * 100));
                if (tick == WATCH) {
                    counting = true;
                    fire0 = fire();
                    mb0 = fuelMb();
                    refuelled0 = refuelled;
                    steam0 = hp.totalKg;
                    work0 = hp.totalWork;
                    pumped0 = c.v.totalPumpedKg;
                    tubes0 = c.totalTubesJ;
                    boilerKg0 = boilerKg();
                    hotwell0 = c.v.vessel.mLiquid();
                    inv0 = inventory();
                    all0 = allWater();
                    drained0 = hp.totalDrainedKg;
                    carried0 = hp.totalCarriedKg;
                    ejected0 = c.v.totalEjectedSteamKg;
                    taken0 = c.v.totalSteamKg;
                }
                if (tick > WATCH) {
                    window += dt;
                    for (Resistor r : loads) elecJ += r.getP() * dt;
                    for (int i = 0; i < 2; i++) {
                        pSum[i] += b[i].vessel.P() * dt;
                        shellMax[i] = Math.max(shellMax[i], b[i].thermal.Tc);
                        fillLo = Math.min(fillLo, b[i].fill());
                        fillHi = Math.max(fillHi, b[i].fill());
                    }
                    for (var t : fT) furnaceMax = Math.max(furnaceMax, t.Tc);
                    radsSum += shaft(hp).getRads() * dt;
                    condSum += c.v.vessel.P() * dt;
                    floodHi = Math.max(floodHi, c.floodShare());
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            mods.eln.fluid.FluidWorld.remove(meter);
            mods.eln.fluid.FluidWorld.remove(loop);
            for (FluidDevice d : air) if (d != null) mods.eln.fluid.FluidWorld.remove(d);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            String id = id();
            if (blowers) {
                Eln.simulator.removeSlowProcess(blowClock);
                double s = Math.max(1e-9, blowS), all = blowJ / s, made = elecJ / Math.max(1e-9, window);
                ctx.line(String.format("%s: BLOWERS (4, one a furnace, at %.1f %% speed): %.2f kW each, %.2f g/s of air each, %.3f bar gauge at their outlets; "
                        + "the plant makes %.2f kW electric and its blowers draw %.2f kW: net %+.2f kW",
                    id, BLOWER_SPEED * 100, all / 4 / 1000, blowKg / s / 4 * 1000, blowPaS / s / 4 / 1e5, made / 1000, all / 1000, (made - all) / 1000));
                ctx.check(id + ": the blowers hold the mains' pressure on the furnaces' flanges (1.6 bar gauge within 0.1)", Math.abs(blowPaS / s / 4 - AIR_PA) < 0.1e5,
                    String.format("%.3f bar", blowPaS / s / 4 / 1e5));
                // (todo #471: as the furnace's breather is coded, forced air costs several times what the plant makes; this pins that)
                ctx.check(id + ": KNOWN PROBLEM: the blowers draw more than the plant makes (the furnaces' breathers pass ~20x the combustion air)", all > made,
                    String.format("%.1f kW drawn, %.1f kW made", all / 1000, made / 1000));
            }
            // (measured, not pinned: the wet part of the turbine's own exhaust collects in its exhaust, 1.7 % of its volume a
            // minute here; run for 16800 ticks, it fails on water at 10 %, tick 8448, and the boilers after it)
            ctx.line(String.format("%s: water in the turbine's exhaust (share of its volume) by tick:%s; %s", id, wetLog,
                hpTrip < 0 ? "the turbine runs" : String.format("the turbine failed at tick %d with %.1f %% (%s)", hpTrip, wetLast * 100, hp.getBurnt())));
            boolean burnt = hp.isBurnt() || b[0].isBurnt() || b[1].isBurnt();
            for (var x : f) burnt |= x.isBurnt();
            ctx.check(id + ": nothing burnt out", !burnt, String.format("boilers %b %b, turbine %b", b[0].isBurnt(), b[1].isBurnt(), hp.isBurnt()));
            if (burnt || b[0].vessel == null || b[1].vessel == null) return;
            double w = Math.max(1e-9, window), ms = Math.max(1e-9, meterS);
            double fire = (fire() - fire0) / w, mB = (mb0 + refuelled - refuelled0 - fuelMb()) / w, wA = waterJ[0] / ms, wB = waterJ[1] / ms, water = wA + wB;
            double steam = (hp.totalKg - steam0) / w, shaftW = (hp.totalWork - work0) / w, elec = elecJ / w, pumped = (c.v.totalPumpedKg - pumped0) / w;
            double tubes = (c.totalTubesJ - tubes0) / w, pA = pSum[0] / w, pB = pSum[1] / w;
            ctx.line(String.format("%s: fire %.2f kW (4 furnaces, max %.0f K over ambient), %.2f kW into the water (boilers %.2f + %.2f kW, %.1f %% of the fire); "
                    + "boilers %.2f / %.2f bar, shells max %.0f / %.0f K over ambient, fill %.0f..%.0f %%; steam %.2f g/s, shaft %.2f kW (%.0f kJ/kg), electric %.2f kW, %.0f rad/s; "
                    + "condenser %.3f bar, up to %.0f %% full, pumped %.2f g/s, %.1f kW into the cooling water; boiler water %+.1f g and hotwell %+.1f g over %.0f s (condenser took %.2f g/s of steam, its ejector threw out %.2f g/s); fuel %.2f mB/s = %.1f buckets/h; coke %.3f",
                id, fire / 1000, furnaceMax, water / 1000, wA / 1000, wB / 1000, 100 * water / Math.max(1e-9, fire),
                pA / 1e5, pB / 1e5, shellMax[0], shellMax[1], fillLo * 100, fillHi * 100, steam * 1000, shaftW / 1000, shaftW / Math.max(1e-9, steam) / 1000, elec / 1000, radsSum / w,
                condSum / w / 1e5, floodHi * 100, pumped * 1000, tubes / 1000, (boilerKg() - boilerKg0) * 1000, (c.v.vessel.mLiquid() - hotwell0) * 1000, w, (c.v.totalSteamKg - taken0) / w * 1000, (c.v.totalEjectedSteamKg - ejected0) / w * 1000, mB, mB * 3.6, f[0].getCoke()));
            double[] inv = inventory();
            StringBuilder sb = new StringBuilder(id + ": water, g/s over the window (hotwell, pump outlet, makeup stub, turbine inlet, turbine exhaust, boiler A, boiler B, feed A, feed B):");
            double net = 0;
            for (int i = 0; i < inv.length; i++) {
                sb.append(String.format(" %+.2f", (inv[i] - inv0[i]) / w * 1000));
                net += (inv[i] - inv0[i]) / w;
            }
            double lost = (all0 - allEnd) / w, drained = (hp.totalDrainedKg - drained0) / w, ejected = (c.v.totalEjectedSteamKg - ejected0) / w;
            ctx.line(sb + String.format("; together %+.2f g/s. All the plant's water, pipes and steam too: %+.3f g/s; of that the turbine's inlet casing drain took %.3f g/s and the "
                    + "condenser's ejector %.3f g/s (unaccounted %.3f g/s); %.2f g/s of water went out of the turbine's exhaust with its steam",
                net * 1000, -lost * 1000, drained * 1000, ejected * 1000, (lost - drained - ejected) * 1000, (hp.totalCarriedKg - carried0) / w * 1000));
            if (endurance) ctx.check(id + ": the water that leaves the cycle is the inlet casing drain's and the ejector's (unaccounted under 0.05 g/s)",
                Math.abs(lost - drained - ejected) < 0.05e-3, String.format("%.3f g/s", (lost - drained - ejected) * 1000));
            ctx.line(id + ": boiler " + b[0].getWaila() + "; furnace " + f[0].getWaila() + "; HP " + hp.getWaila() + "; condenser " + c.getWaila());
            // #474: the wet share of the exhaust leaves with the steam (it reached 10 % and stripped the blades at tick 8448)
            ctx.check(id + ": the turbine's exhaust stays drained of its own wet steam (water under 2 % of its volume)", wetMax < 0.02, String.format("%.2f %%", wetMax * 100));
            if (endurance) {
                ctx.check(id + ": still near rated power after 15 minutes (over 14 kW on the shaft: the burners coke)", shaftW > 14e3, String.format("%.2f kW", shaftW / 1000));
                ctx.check(id + ": the boilers hold their pressure and water (within 1.5 bar; neither dry nor overfull)",
                    Math.abs(pA / 1e5 - BAR) < 1.5 && Math.abs(pB / 1e5 - BAR) < 1.5 && fillLo > 0.3 && fillHi < 1.02, String.format("%.2f / %.2f bar, fill %.0f..%.0f %%", pA / 1e5, pB / 1e5, fillLo * 100, fillHi * 100));
                ctx.check(id + ": the condensate goes back (97 %+ of the steam)", steam > 1e-3 && pumped > 0.97 * steam, String.format("%.2f of %.2f g/s", pumped * 1000, steam * 1000));
                return;
            }
            ctx.check(id + ": rated power on the shaft (over 16 kW, within 8 % of 17.1 kW)", shaftW > 16e3 && Math.abs(shaftW / 17100 - 1) < 0.08, String.format("%.2f kW", shaftW / 1000));
            ctx.check(id + ": the generators deliver over 14 kW", elec > 14e3 && elec < shaftW, String.format("%.2f kW", elec / 1000));
            ctx.check(id + ": both boilers hold their pressure (within 1.5 bar of the regulator's)", Math.abs(pA / 1e5 - BAR) < 1.5 && Math.abs(pB / 1e5 - BAR) < 1.5,
                String.format("%.2f / %.2f bar", pA / 1e5, pB / 1e5));
            ctx.check(id + ": the boilers share the heat and the feed (each 45-55 % of the heat; neither dry nor overfull)",
                wA > 0.45 * water && wA < 0.55 * water && fillLo > 0.3 && fillHi < 1.02, String.format("%.2f + %.2f kW, fill %.0f..%.0f %%", wA / 1000, wB / 1000, fillLo * 100, fillHi * 100));
            ctx.check(id + ": the shells stay under their 400 K limit", shellMax[0] < 400 && shellMax[1] < 400, String.format("%.0f / %.0f K", shellMax[0], shellMax[1]));
            ctx.check(id + ": the condensate goes back (90 %+ of the steam)", steam > 1e-3 && pumped > 0.9 * steam, String.format("%.2f of %.2f g/s", pumped * 1000, steam * 1000));
            ctx.check(id + ": the condenser holds a vacuum (under 0.3 bar)", condSum / w < 0.3e5, String.format("%.3f bar", condSum / w / 1e5));
            // (with blowers the tanks lose 15 % more oil for 2 % less fire: measured 54.8 buckets/h; the blowers' air is hot
            // from its compression and 125 g/s of it sweeps each tank, so oil vapour leaving by the breathers is the likely
            // cause, not checked: todo #471)
            double oil = blowers ? 54.8 : 47.5;
            ctx.check(id + ": heavy oil within 10 % of " + oil + " buckets/h", Math.abs(mB * 3.6 / oil - 1) < 0.10, String.format("%.1f buckets/h", mB * 3.6));
        }
    }
}
