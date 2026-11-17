package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.oil.Oil;
import mods.eln.sim.IProcess;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.transparentnode.FuelHeatFurnaceContainer;
import mods.eln.transparentnode.FuelHeatFurnaceElement;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The fuel heat furnace as a combustion chamber (todo #504; Baughn 2026-11-17: the burner must feel its exhaust's
 * back-pressure, flanges on the sides or back): its firebox (the flue's node) takes air by natural draught or by its damper
 * from the air port on its left flank, burns it into flue gas, and lets it out up its stack or into the pipe on its flue.
 */
public final class CombustionCases {
    private CombustionCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Bench("fuel_furnace_natural_draught"));
        cases.add(new Bench("fuel_furnace_flue_back_pressure"));
        cases.add(new BlowerSpeed());
        cases.add(new Drown());
        cases.add(new AirPortAbuse());
        cases.add(new PortSpots());
    }

    /** The big burner's rating (W). */
    static double bigW() {
        return mods.eln.item.FuelBurnerDescriptor.Companion.powerForType(0);
    }

    /** A Fuel Heat Furnace at p facing front, a Big burner in it, lit at full (on: else its switch is left off), with mB of diesel. */
    static FuelHeatFurnaceElement furnace(TestContext ctx, BlockPos p, Direction front, int mB, boolean on) {
        var f = (FuelHeatFurnaceElement) ctx.placeTransparent(Wp10bCases.FUEL_HEAT_FURNACE, p);
        f.front = front;
        f.reconnect();
        f.getInventory().setItem(FuelHeatFurnaceContainer.Companion.getFuelBurnerSlot(), Eln.sharedItemStackOne.getDescriptor(2 + (124 << 6)).newItemStack());
        f.inventoryChange(f.getInventory());
        if (on) f.networkUnserialize(ctx.stream(o -> o.writeByte(FuelHeatFurnaceElement.Companion.getMainSwitchToggleEvent())));
        f.networkUnserialize(ctx.stream(o -> {
            o.writeByte(FuelHeatFurnaceElement.Companion.getSetManualControlValueEvent());
            o.writeFloat(1f);
        }));
        f.putFuel(Oil.Cut.DIESEL.source.get(), mB);
        return f;
    }

    /** The block its air port opens into, and the port's face as seen from there (3D data value) and spot. */
    static BlockPos airBlock(FuelHeatFurnaceElement f) {
        return f.node.coordonate.pos().relative(f.front.left().toEnumFacing());
    }

    static Object[] airFixture(FuelHeatFurnaceElement f, FluidNode main) {
        var left = f.front.left();
        return new Object[]{airBlock(f), left.toEnumFacing().getOpposite().get3DDataValue(), main,
            mods.eln.fluid.PortBridge.flankSpot(f.front, left, FuelHeatFurnaceElement.AIR_X, FuelHeatFurnaceElement.AIR_Y)};
    }

    /** Its firebox's gauge pressure (Pa). */
    static double fireboxGauge(FuelHeatFurnaceElement f) {
        return mods.eln.fluid.FluidWorld.nodeFor(f.getFlue().node).P() - Water.P_ATM;
    }

    /** What left node n through its edges this step (kg/s; into n counts negative), and its enthalpy (W). */
    static double[] outOf(FluidNode n) {
        double kg = 0, w = 0;
        for (FluidEdge e : mods.eln.fluid.FluidWorld.edgesOf(n)) {
            double s = e.a == n ? 1 : e.b == n ? -1 : 0;
            kg += s * e.flow;
            w += s * e.fEnthalpy;
        }
        return new double[]{kg, w};
    }

    /** A fluid device with no ports whose afterFluidStep runs step (dt). */
    static FluidDevice after(Object level, java.util.function.DoubleConsumer step) {
        var d = new FluidDevice() {
            public Object fluidLevel() {
                return level;
            }

            public List<FluidPort> fluidPorts() {
                return List.of();
            }

            public void afterFluidStep(double dt) {
                step.accept(dt);
            }
        };
        mods.eln.fluid.FluidWorld.add(d);
        return d;
    }

    /**
     * Rigs of one furnace each (front XN, a row along x), metered over a window:
     * <ul>
     * <li>fuel_furnace_natural_draught: natural draught with nothing on its flue (fires its setting, its firebox within a
     * few Pa of the air, its smoke carrying SMOKE_SHARE of the flame over the ambient); its flue piped into the open air
     * through an iron pipe's end; its flue into air held 75 and 300 Pa over the atmosphere (DRAUGHT_PA 150: a little
     * less, and starved);</li>
     * <li>fuel_furnace_flue_back_pressure: forced air (a main at DAMPER_DP on its air port): its flue unpiped (FORCED_MAX
     * x); piped into the open air (the mass balance: air and fuel in, flue gas out); its flue's way out narrowed half way
     * through (a valve closing: the fire drops); its flue dead-ended into a closed 1 L volume (the fire goes out, the
     * firebox stalls at the main's pressure, under the choke, Newton converges).</li>
     * </ul>
     */
    static final class Bench extends PlumbingCases.Big {
        final String id;
        final boolean natural;

        /** One rig: the furnace, the air main on its air port (null: natural draught), and what is on its flue. */
        static final class Rig {
            final String name;
            FuelHeatFurnaceElement f;
            FluidNode main, flueEnd;
            double mainPa = Double.NaN, flueEndPa = Double.NaN, flueK;
            boolean deadEnd;
            FluidDevice airFx, flueFx;
            /** Sums over the window (s, J of flame, kg of air in, fuel, flue gas out, smoke W over the ambient x s), and the second window's. */
            double t, flameJ, airKg, fuelKg, outKg, smokeWs, t2, flameJ2, gaugeS, gaugeMax, ratioS, ratioMin = 1e9, ratioMax = -1e9;
            mods.eln.oil.AirBlowerElement blower;
            String stats = "";
            double flame0, air0, fuel0;
            String jade = "";

            Rig(String name) {
                this.name = name;
            }
        }

        final List<Rig> rigs = new ArrayList<>();
        IProcess clock;
        FluidDevice meter;
        int tick;
        static final int W0 = 100, SWAP = 160, W1 = 200, END = 280;

        Bench(String id) {
            this.id = id;
            this.natural = id.contains("natural");
        }

        public String id() {
            return id;
        }

        public int ticks() {
            return END + 10;
        }

        public boolean checkLimitCycles() {
            return false;
        }

        Rig rig(TestContext ctx, String name, int x, int z) {
            Rig r = new Rig(name);
            r.f = furnace(ctx, ctx.at(x, 1, z), Direction.XN, 1500, true);
            rigs.add(r);
            return r;
        }

        void flue(TestContext ctx, Rig r, double pa, double k, boolean dead) {
            BlockPos up = r.f.node.coordonate.pos().above();
            r.flueEndPa = pa;
            r.flueK = k;
            r.deadEnd = dead;
            r.flueEnd = dead ? new FluidNode(0.001, up.getY(), 0.1, false).fill(0, Water.P_ATM, 293.15)
                : new FluidNode(1.0, up.getY(), 1.0, true).fill(0, Water.P_ATM + pa, 293.15);
            r.flueFx = GasPlantCases.fixtures(r.f.fluidLevel(), new Object[]{up, 0, r.flueEnd, FluidDevice.FluidPort.CENTRE, k});
        }

        void air(Rig r, double pa) {
            r.mainPa = pa;
            r.main = OilCases.FurnacePressure.main(r.f.node.coordonate.pos().getY(), pa, false);
            r.airFx = GasPlantCases.fixtures(r.f.fluidLevel(), airFixture(r.f, r.main));
        }

        public void build(TestContext ctx) {
            double wide = mods.eln.fluid.Flue.STACK_K, pipe = 2 * mods.eln.node.voxel.PipePart.K_PER_BLOCK[0] * 16;
            if (natural) {
                rig(ctx, "unpiped", 1, 3);
                flue(ctx, rig(ctx, "piped to the open air", 3, 3), 0, pipe, false);
                flue(ctx, rig(ctx, "flue held at +75 Pa", 5, 3), 75, wide, false);
                flue(ctx, rig(ctx, "flue held at +300 Pa", 7, 3), 300, wide, false);
                // (switched off, and switched on at setting 0, nothing piped: does its network sleep?)
                Rig off = new Rig("off");
                off.f = furnace(ctx, ctx.at(6, 1, 7), Direction.XN, 1500, false);
                rigs.add(off);
                Rig idle = rig(ctx, "on, set to 0", 3, 7);
                idle.f.networkUnserialize(ctx.stream(o -> {
                    o.writeByte(FuelHeatFurnaceElement.Companion.getSetManualControlValueEvent());
                    o.writeFloat(0f);
                }));
            } else {
                double dp = FuelHeatFurnaceElement.DAMPER_DP;
                air(rig(ctx, "forced, unpiped", 1, 3), dp);
                Rig m = rig(ctx, "forced, piped to the open air", 3, 3);
                air(m, dp);
                flue(ctx, m, 0, pipe, false);
                Rig v = rig(ctx, "forced, its flue's valve closing", 5, 3);
                air(v, dp);
                flue(ctx, v, 0, wide, false);
                Rig d = rig(ctx, "forced, flue dead-ended", 7, 3);
                air(d, dp);
                flue(ctx, d, 0, pipe, true);
                // (an Air Blower in front of it, piped by the router to its air port; its flue dead-ended)
                Rig bd = rig(ctx, "blower, flue dead-ended", 5, 7);
                bd.blower = HpPlantCases.blowerOnAirPort(ctx, bd.f, 1.0);
                flue(ctx, bd, 0, pipe, true);
            }
            meter = after(rigs.get(0).f.fluidLevel(), dt -> {
                if (tick < W0) return;
                for (Rig r : rigs) {
                    if (r.f.getFlue().node == null) continue;
                    double g = fireboxGauge(r.f);
                    r.gaugeMax = Math.max(r.gaugeMax, g);
                    if (tick >= SWAP) continue;
                    r.t += dt;
                    r.gaugeS += g * dt;
                    r.ratioS += r.f.getAirRatio() * dt;
                    r.ratioMin = Math.min(r.ratioMin, r.f.getAirRatio());
                    r.ratioMax = Math.max(r.ratioMax, r.f.getAirRatio());
                    // (out of its firebox: by its stack, or into what is on its flue)
                    double[] o = r.flueEnd != null ? outOf(r.flueEnd) : null;
                    double kg = 0, w = 0;
                    if (o != null) {
                        kg = -o[0];
                        w = -o[1];
                    } else if (r.f.getFlue().stack != null && r.f.getFlue().stack.edge != null) {
                        var e = r.f.getFlue().stack.edge;
                        kg = e.flow;
                        w = e.fEnthalpy;
                    }
                    r.outKg += kg * dt;
                    // (over the ambient: the flue gas's enthalpy at 293 K for what left)
                    var sp = mods.eln.sim.fluid.SpeciesRegistry.get().byId(mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:flue_gas"));
                    r.smokeWs += (w - kg * sp.hVapour(293.15)) * dt;
                }
            });
            Eln.simulator.addSlowProcess(clock = dt -> {
                tick++;
                for (Rig r : rigs) {
                    if (r.main != null) r.main.fill(0, Water.P_ATM + r.mainPa, 293.15);
                    if (r.flueEnd != null && !r.deadEnd) r.flueEnd.fill(0, Water.P_ATM + r.flueEndPa, 293.15);
                    if (tick == W0) {
                        r.flame0 = r.f.getFlameJ();
                        r.air0 = r.f.getAirInKg();
                        r.fuel0 = r.f.getFuelBurntKg() + r.f.getGasBurntKg();
                    }
                    if (tick == SWAP) {
                        r.flameJ = r.f.getFlameJ() - r.flame0;
                        r.airKg = r.f.getAirInKg() - r.air0;
                        r.fuelKg = r.f.getFuelBurntKg() + r.f.getGasBurntKg() - r.fuel0;
                        r.jade = "" + r.f.getWaila();
                        if (r.name.contains("valve")) {
                            // (its valve nearly shut: the way out of its flue narrowed a thousandfold)
                            mods.eln.fluid.FluidWorld.remove(r.flueFx);
                            r.flueFx = GasPlantCases.fixtures(r.f.fluidLevel(), new Object[]{r.f.node.coordonate.pos().above(), 0, r.flueEnd, FluidDevice.FluidPort.CENTRE, 1e-12});
                        }
                    }
                    if (tick == W1) r.flame0 = r.f.getFlameJ();
                    if (tick == END) {
                        r.stats = GasPlantCases.netStats(r.f.getFlue().node);
                        r.flameJ2 = r.f.getFlameJ() - r.flame0;
                        r.t2 = (END - W1) / 20.0;
                        if (!r.name.contains("valve")) r.jade = "" + r.f.getWaila();
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            mods.eln.fluid.FluidWorld.remove(meter);
            for (Rig r : rigs) {
                if (r.airFx != null) mods.eln.fluid.FluidWorld.remove(r.airFx);
                if (r.flueFx != null) mods.eln.fluid.FluidWorld.remove(r.flueFx);
            }
            double nominal = bigW(), win = (SWAP - W0) / 20.0;
            for (Rig r : rigs) {
                double t = Math.max(1e-9, r.t), fire = r.flameJ / win / nominal, fire2 = r.flameJ2 / Math.max(1e-9, r.t2) / nominal;
                double air = r.airKg / win, fuel = r.fuelKg / win, out = r.outKg / t, smoke = r.smokeWs / t;
                double share = smoke / Math.max(1e-9, r.flameJ / win);
                ctx.line(String.format("%s: %s: fire %.3f x its rating (later %.3f), air ratio %.2f (%.2f..%.2f); air in %.2f g/s, fuel %.3f g/s, out of the firebox %.2f g/s"
                        + " (%.2f %% of air + fuel), smoke %.2f kW over the ambient (%.3f of the flame); firebox %.1f Pa gauge (max %.0f); burnt %b; %s",
                    id, r.name, fire, fire2, r.ratioS / t, r.ratioMin, r.ratioMax, air * 1000, fuel * 1000, out * 1000, out / Math.max(1e-12, air + fuel) * 100, smoke / 1000, share,
                    r.gaugeS / t, r.gaugeMax, r.f.isBurnt(), r.jade));
                var fl = r.f.getFlue();
                if (fl.sink != null && fl.node != null) {
                    FluidNode fb = mods.eln.fluid.FluidWorld.nodeFor(fl.node);
                    int fg = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:flue_gas");
                    double gas = fb.mAir + (fg >= 2 ? fb.vapour(fg) : 0);
                    ctx.line(String.format("%s: %s: firebox %.0f K, %.0f %% of its gas air; the sink took %.2f g/s at %.0f kJ/kg; the smoke went in at %.0f K; its network %s",
                        id, r.name, fb.T(), gas > 0 ? 100 * fb.mAir / gas : 0, fl.sink.delivered * 1000, fl.sink.delivered > 0 ? fl.sink.deliveredW / fl.sink.delivered / 1000 : 0,
                        fl.flow.kelvin, r.stats));
                }
                boolean finite = Double.isFinite(fire) && Double.isFinite(r.gaugeMax) && !r.f.isBurnt();
                if (r.name.equals("on, set to 0") || r.name.equals("off")) {
                    ctx.check(id + ": on at setting 0: no fire", fire < 1e-3, String.format("%.4f", fire));
                    continue;
                }
                ctx.check(id + ": " + r.name + ": finite, nothing burnt", finite, String.format("fire %.3f, max gauge %.0f Pa", fire, r.gaugeMax));
                switch (r.name) {
                    case "unpiped" -> {
                        ctx.check(id + ": natural draught fires its setting (within 3 %)", Math.abs(fire - 1) < 0.03, String.format("%.3f", fire));
                        ctx.check(id + ": its firebox stays within 5 Pa of the air", Math.abs(r.gaugeS / t) < 5 && r.gaugeMax < 10, String.format("%.1f Pa (max %.1f)", r.gaugeS / t, r.gaugeMax));
                        ctx.check(id + ": its smoke carries SMOKE_SHARE of the flame over the ambient (within 10 %)",
                            Math.abs(share / mods.eln.fluid.Flue.SMOKE_SHARE - 1) < 0.10, String.format("%.3f", share));
                        ctx.check(id + ": Jade says natural draught", r.jade.contains("natural draught"), r.jade);
                    }
                    case "piped to the open air" -> ctx.check(id + ": piped, its air doesn't hunt (the ratio within 0.1 over the window)", r.ratioMax - r.ratioMin < 0.1,
                        String.format("%.2f..%.2f", r.ratioMin, r.ratioMax));
                    case "flue held at +75 Pa" -> ctx.check(id + ": a flue backed up half the draught fires it less, but it burns (0.5-0.85 x)", fire > 0.5 && fire < 0.85, String.format("%.3f", fire));
                    case "flue held at +300 Pa" -> {
                        ctx.check(id + ": a flue backed up past the draught starves it (under 0.1 x)", fire < 0.1, String.format("%.3f", fire));
                        ctx.check(id + ": Jade says starved", r.jade.contains("starved"), r.jade);
                    }
                    case "forced, unpiped" -> {
                        ctx.check(id + ": forced air at DAMPER_DP fires FORCED_MAX x (within 4 %)", Math.abs(fire / FuelHeatFurnaceElement.FORCED_MAX - 1) < 0.04, String.format("%.3f", fire));
                        ctx.check(id + ": Jade says forced", r.jade.contains("forced"), r.jade);
                    }
                    case "forced, piped to the open air" -> {
                        ctx.check(id + ": air and fuel in = flue gas out of the firebox (within 1 %)", air > 0 && Math.abs(out / (air + fuel) - 1) < 0.01,
                            String.format("%.3f in, %.3f out g/s", (air + fuel) * 1000, out * 1000));
                        ctx.check(id + ": its smoke into the pipe carries SMOKE_SHARE of the flame over the ambient (within 10 %)",
                            Math.abs(share / mods.eln.fluid.Flue.SMOKE_SHARE - 1) < 0.10, String.format("%.3f", share));
                        ctx.check(id + ": its piped flue's back-pressure shows in its firebox (over 300 Pa)", r.gaugeS / t > 300, String.format("%.0f Pa", r.gaugeS / t));
                    }
                    case "forced, its flue's valve closing" -> ctx.check(id + ": its flue's valve closing throttles the fire (to under half)", fire > 2 && fire2 < 0.5 * fire,
                        String.format("%.3f, then %.3f", fire, fire2));
                    case "blower, flue dead-ended" -> {
                        String stats = r.stats;
                        var m = java.util.regex.Pattern.compile("([0-9.]+) sub-steps").matcher(stats);
                        double sub = m.find() ? Double.parseDouble(m.group(1)) : 99;
                        ctx.line(id + ": blower, flue dead-ended: its network " + stats + "; blower " + r.blower.getWaila());
                        ctx.check(id + ": a blower on a dead-ended flue: its fire goes (under 0.15 x)", fire2 < 0.15, String.format("%.3f then %.3f", fire, fire2));
                        ctx.check(id + ": ... its firebox stalls under the choke (CHOKED_PA)", r.gaugeMax < mods.eln.fluid.Flue.CHOKED_PA,
                            String.format("max %.0f Pa", r.gaugeMax));
                        ctx.check(id + ": ... and its network's solve converges (under 1.5 sub-steps a step)", sub < 1.5, stats);
                    }
                    case "forced, flue dead-ended" -> {
                        ctx.check(id + ": dead-ended, its fire goes (under 0.15 x)", fire2 < 0.15, String.format("%.3f then %.3f", fire, fire2));
                        ctx.check(id + ": its firebox stalls under the blower's head plus a margin, under the choke", r.gaugeMax < mods.eln.fluid.Flue.CHOKED_PA,
                            String.format("max %.0f Pa", r.gaugeMax));
                    }
                    default -> {
                    }
                }
            }
        }
    }

    /**
     * Two furnaces, each with an Air Blower piped to its air port (unpiped flues): one at full speed, one slowed to 55 % (by its
     * supply): the slowed one fires between its setting and FORCED_MAX, for a fraction of the full one's power.
     */
    static final class BlowerSpeed extends PlumbingCases.Big {
        FuelHeatFurnaceElement[] f = new FuelHeatFurnaceElement[2];
        mods.eln.oil.AirBlowerElement[] b = new mods.eln.oil.AirBlowerElement[2];
        static final double[] SPEED = {1.0, 0.55};
        IProcess clock;
        int tick;
        final double[] flame0 = new double[2], w = new double[2], gauge = new double[2];
        double t;

        public String id() {
            return "fuel_furnace_blower_speed";
        }

        public int ticks() {
            return 250;
        }

        public boolean checkLimitCycles() {
            return false;
        }

        public void build(TestContext ctx) {
            for (int i = 0; i < 2; i++) {
                f[i] = furnace(ctx, ctx.at(2 + 4 * i, 1, 5), Direction.XN, 1500, true);
                // (the blower two blocks north of its air port, its outlet facing east; the router lays the pipe)
                BlockPos bp = ctx.at(2 + 4 * i, 1, 2);
                b[i] = HpPlantCases.blower(ctx, bp, Direction.XP, false);
                HpPlantCases.power(b[i], SPEED[i] * Eln.MVU);
                var start = mods.eln.node.voxel.PipePlacer.portAtForTests(ctx.level(), bp, net.minecraft.core.Direction.EAST);
                var end = mods.eln.node.voxel.PipePlacer.portAtForTests(ctx.level(), f[i].node.coordonate.pos(), f[i].front.left().toEnumFacing());
                String err = mods.eln.node.voxel.PipePlacer.routeForTests(ctx.level(), 1, start, end);
                if (err != null) throw new IllegalStateException("blower to the air port: " + err);
            }
            Eln.simulator.addSlowProcess(clock = dt -> {
                tick++;
                if (tick == 100) for (int i = 0; i < 2; i++) flame0[i] = f[i].getFlameJ();
                if (tick > 100 && tick <= 220) {
                    t += dt;
                    for (int i = 0; i < 2; i++) {
                        w[i] += b[i].wantW * dt;
                        gauge[i] += (mods.eln.fluid.FluidWorld.nodeFor(f[i].getWindbox()).P() - Water.P_ATM) * dt;
                    }
                }
                if (tick == 220) for (int i = 0; i < 2; i++) flame0[i] = f[i].getFlameJ() - flame0[i];
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            double s = Math.max(1e-9, t), fire[] = new double[2], pw[] = new double[2];
            for (int i = 0; i < 2; i++) {
                fire[i] = flame0[i] / s / bigW();
                pw[i] = w[i] / s;
                ctx.line(String.format("blower at %.0f %%: fire %.3f x its rating, blower %.0f W, windbox %.3f bar gauge; furnace %s; blower %s",
                    SPEED[i] * 100, fire[i], pw[i], gauge[i] / s / 1e5, f[i].getWaila(), b[i].getWaila()));
            }
            ctx.check("blower speed: neither furnace burnt out on its bench", !f[0].isBurnt() && !f[1].isBurnt(), f[0].isBurnt() + " " + f[1].isBurnt());
            ctx.check("blower at full speed: the fire at FORCED_MAX (within 5 %)", Math.abs(fire[0] / FuelHeatFurnaceElement.FORCED_MAX - 1) < 0.05, String.format("%.3f", fire[0]));
            ctx.check("blower at 55 %: an intermediate fire (1.2 x .. 0.95 FORCED_MAX)", fire[1] > 1.2 && fire[1] < 0.95 * FuelHeatFurnaceElement.FORCED_MAX, String.format("%.3f", fire[1]));
            ctx.check("blower at 55 %: for under half the full blower's power", pw[1] < 0.5 * pw[0], String.format("%.0f of %.0f W", pw[1], pw[0]));
        }
    }

    /**
     * Water poured into a burning furnace (Baughn 2026-11-11: the fire drowns): its flashed steam goes up its stack, or up
     * the pipe on its flue, over about a second, not as one lump; its firebox's pressure stays modest and it empties.
     */
    static final class Drown extends PlumbingCases.Big {
        FuelHeatFurnaceElement open, piped;
        FluidNode end, mainO, mainP;
        FluidDevice fx, meter;
        double maxOpen, maxPiped, steamed;
        int tick;
        IProcess clock;

        public String id() {
            return "fuel_furnace_drown_flue";
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            open = furnace(ctx, ctx.at(2, 1, 4), Direction.XN, 1000, true);
            piped = furnace(ctx, ctx.at(6, 1, 4), Direction.XN, 1000, true);
            BlockPos up = piped.node.coordonate.pos().above();
            end = new FluidNode(1.0, up.getY(), 1.0, true).fill(0, Water.P_ATM, 293.15);
            // (both on forced air, so both burn hot: a starved burner flashes less of the water)
            mainO = OilCases.FurnacePressure.main(up.getY(), FuelHeatFurnaceElement.DAMPER_DP, false);
            mainP = OilCases.FurnacePressure.main(up.getY(), FuelHeatFurnaceElement.DAMPER_DP, false);
            fx = GasPlantCases.fixtures(piped.fluidLevel(), new Object[]{up, 0, end, FluidDevice.FluidPort.CENTRE, 2 * mods.eln.node.voxel.PipePart.K_PER_BLOCK[0] * 16},
                airFixture(open, mainO), airFixture(piped, mainP));
            meter = after(open.fluidLevel(), dt -> {
                if (open.getFlue().node == null || piped.getFlue().node == null) return;
                maxOpen = Math.max(maxOpen, fireboxGauge(open));
                maxPiped = Math.max(maxPiped, fireboxGauge(piped));
            });
            Eln.simulator.addSlowProcess(clock = dt -> {
                tick++;
                end.fill(0, Water.P_ATM, 293.15);
                mainO.fill(0, Water.P_ATM + FuelHeatFurnaceElement.DAMPER_DP, 293.15);
                mainP.fill(0, Water.P_ATM + FuelHeatFurnaceElement.DAMPER_DP, 293.15);
                if (tick == 100) {
                    maxOpen = maxPiped = 0;
                    open.putFuel(net.minecraft.world.level.material.Fluids.WATER, 300);
                    piped.putFuel(net.minecraft.world.level.material.Fluids.WATER, 300);
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            mods.eln.fluid.FluidWorld.remove(meter);
            mods.eln.fluid.FluidWorld.remove(fx);
            FluidNode fo = mods.eln.fluid.FluidWorld.nodeFor(open.getFlue().node), fp = mods.eln.fluid.FluidWorld.nodeFor(piped.getFlue().node);
            String what = String.format("steamed %.1f g / %.1f g; firebox peak %.0f Pa unpiped, %.0f Pa piped; water left in them %.3f g / %.3f g; on %b / %b",
                open.getSteamedKg() * 1000, piped.getSteamedKg() * 1000, maxOpen, maxPiped, fo.mWater * 1000, fp.mWater * 1000, open.switchedOn(), piped.switchedOn());
            ctx.line("drown: " + what);
            ctx.check("drown: both fires go out and flash steam", open.getSteamedKg() > 1e-3 && piped.getSteamedKg() > 1e-3 && !open.switchedOn() && !piped.switchedOn(), what);
            ctx.check("drown: unpiped, the steam goes up its stack (its firebox under 0.05 bar)", maxOpen < 5e3, what);
            ctx.check("drown: piped, up its flue's pipe (its firebox under 1 bar, under the choke)", maxPiped < 1e5, what);
            ctx.check("drown: the steam leaves both fireboxes (under 1 g of the 19 g left: piped, what condenses lies under its flue)", fo.mWater < 1e-3 && fp.mWater < 1e-3, what);
        }
    }

    /**
     * Its air port abused with its switch off: water under 10 bar of air on one, air at 10 bar on another. Nothing gets
     * into its firebox (the damper is shut and is a check valve), nothing burns, nothing bursts.
     */
    static final class AirPortAbuse extends PlumbingCases.Big {
        FuelHeatFurnaceElement wet, high;
        FluidNode water, air;
        FluidDevice fx;
        IProcess clock;

        public String id() {
            return "fuel_furnace_air_port_abuse";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            wet = furnace(ctx, ctx.at(2, 1, 4), Direction.XN, 1000, false);
            high = furnace(ctx, ctx.at(6, 1, 4), Direction.XN, 1000, false);
            water = GasPlantCases.water(wet.node.coordonate.pos().getY() + 1, 10e5, 293.15);
            air = OilCases.FurnacePressure.main(high.node.coordonate.pos().getY(), 9e5, false);
            fx = GasPlantCases.fixtures(wet.fluidLevel(), airFixture(wet, water), airFixture(high, air));
            Eln.simulator.addSlowProcess(clock = dt -> air.fill(0, Water.P_ATM + 9e5, 293.15));
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            ctx.line("air port, switched off: its firebox's network " + GasPlantCases.netStats(wet.getFlue().node) + "; its tank's " + GasPlantCases.netStats(wet.getVessel()));
            mods.eln.fluid.FluidWorld.remove(fx);
            for (var f : new FuelHeatFurnaceElement[]{wet, high}) {
                FluidNode w = mods.eln.fluid.FluidWorld.nodeFor(f.getWindbox()), fb = mods.eln.fluid.FluidWorld.nodeFor(f.getFlue().node);
                String what = String.format("windbox %.2f bar gauge, %.1f g water; firebox %.0f Pa gauge, %.3f g water; air in %.4f g; flame %.0f J; burnt %b",
                    (w.P() - Water.P_ATM) / 1e5, w.mWater * 1000, fb.P() - Water.P_ATM, fb.mWater * 1000, f.getAirInKg() * 1000, f.getFlameJ(), f.isBurnt());
                String name = f == wet ? "water at 10 bar" : "air at 10 bar";
                ctx.line("air port, switched off, " + name + ": " + what);
                ctx.check("air port, switched off, " + name + ": it reaches the windbox", w.P() - Water.P_ATM > 5e5, what);
                ctx.check("air port, switched off, " + name + ": nothing gets into the firebox, nothing burns or bursts",
                    Math.abs(fb.P() - Water.P_ATM) < 100 && fb.mWater < 1e-6 && f.getAirInKg() < 1e-6 && f.getFlameJ() == 0 && !f.isBurnt(), what);
            }
        }
    }

    /**
     * Its flanges where the model draws them (mods.eln.cad.models.FuelHeater: the fuel flange on the back at y 6, z 8; the
     * air port on the left flank at x 6, y 8), for each of the four fronts: a pipe laid by hand from the drawn point
     * outward joins its tank or its windbox; one laid where the old front flange was joins nothing.
     */
    static final class PortSpots extends PlumbingCases.Big {
        static final Direction[] FRONTS = {Direction.XN, Direction.XP, Direction.ZN, Direction.ZP};
        final FuelHeatFurnaceElement[] f = new FuelHeatFurnaceElement[4];

        public String id() {
            return "fuel_furnace_port_spots";
        }

        public int ticks() {
            return 20;
        }

        /** Model point (px, front XN, right z+) to the block's local voxel point for a device facing front. */
        static int[] world(Direction front, double mx, double my, double mz) {
            int[] d = vec(front.back()), r = vec(front.right());
            double[] p = {8, 8, 8};
            for (int k = 0; k < 3; k++) p[k] += (mx - 8) * d[k] + (mz - 8) * r[k];
            p[1] += my - 8;
            return new int[]{(int) Math.round(p[0]), (int) Math.round(p[1]), (int) Math.round(p[2])};
        }

        static int[] vec(Direction d) {
            var n = d.toEnumFacing().getUnitVec3i();
            return new int[]{n.getX(), n.getY(), n.getZ()};
        }

        /** A pipe in the block beyond the face of p (direction out) from the point q (p's local voxel, on that face) straight out. */
        static void pipeOut(TestContext ctx, BlockPos p, Direction out, int[] q) {
            int[] n = vec(out);
            int[] a = q.clone(), b = q.clone();
            for (int k = 0; k < 3; k++) if (n[k] != 0) {
                a[k] = n[k] > 0 ? 0 : 16;
                b[k] = n[k] > 0 ? 16 : 0;
            }
            PipeCases.lay(ctx, p.relative(out.toEnumFacing()), 1, a[0], a[1], a[2], b[0], b[1], b[2]);
        }

        public void build(TestContext ctx) {
            BlockPos[] at = {ctx.at(2, 1, 2), ctx.at(7, 1, 2), ctx.at(2, 1, 7), ctx.at(7, 1, 7)};
            for (int i = 0; i < 4; i++) {
                Direction fr = FRONTS[i];
                f[i] = furnace(ctx, at[i], fr, 500, false);
                pipeOut(ctx, at[i], fr.back(), world(fr, 16, FuelHeatFurnaceElement.FUEL_Y, FuelHeatFurnaceElement.FUEL_Z));
                pipeOut(ctx, at[i], fr.left(), world(fr, FuelHeatFurnaceElement.AIR_X, FuelHeatFurnaceElement.AIR_Y, 0));
                pipeOut(ctx, at[i], fr, world(fr, 0, 4, 8));   // (where the old front flange was)
            }
        }

        public void measure(TestContext ctx) {
            for (int i = 0; i < 4; i++) {
                var v = f[i].getVessel();
                var w = f[i].getWindbox();
                long fuelEdges = mods.eln.fluid.FluidWorld.edgesOf(v).stream().filter(e -> e.kind != FluidEdge.Kind.CHECK || e.gasOnly == false).count();
                boolean fuel = String.valueOf(f[i].getWaila()).contains("back flange");
                var dam = f[i].getDamper();
                boolean air = mods.eln.fluid.FluidWorld.edgesOf(w).stream().anyMatch(e -> dam == null || e != dam.edge);
                var frontPipe = PipeCases.node(ctx, f[i].node.coordonate.pos().relative(FRONTS[i].toEnumFacing()));
                boolean frontJoined = false;
                if (frontPipe != null) for (var part : frontPipe.parts)
                    if (part instanceof mods.eln.node.voxel.PipePart pp && pp.fluid != null)
                        frontJoined |= mods.eln.fluid.FluidWorld.networkOf(pp.fluid) == mods.eln.fluid.FluidWorld.networkOf(v) && mods.eln.fluid.FluidWorld.networkOf(v) != null;
                String what = "front " + FRONTS[i] + ": " + f[i].getWaila() + ", edges at the tank " + fuelEdges;
                ctx.check("port spots, front " + FRONTS[i] + ": a pipe on the drawn back flange feeds its tank", fuel, what);
                ctx.check("port spots, front " + FRONTS[i] + ": a pipe on the drawn left-flank air port meets its windbox", air, what);
                ctx.check("port spots, front " + FRONTS[i] + ": a pipe where the old front flange was joins nothing", !frontJoined, what);
            }
        }
    }
}
