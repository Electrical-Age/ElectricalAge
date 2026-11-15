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
        cases.add(new FullPlant("hp_plant_full_forced_air_16kw", 4800));
        cases.add(new FullPlant("hp_plant_full_15min", 18000));
        cases.add(new Flooded(true));
        cases.add(new Flooded(false));
        cases.add(new LargeBoilerBody());
        cases.add(new LargeBoilerPlant());
        cases.add(new LargeBoilerBurst());
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

    /** The 2x2x2 Large Boiler's id (16 + (83 << 6); 12 was the one-block prototype's, never shipped). */
    static final int LARGE_BOILER = 16 + (83 << 6);

    /** Place a Large Boiler with its front west (XN) at p: the fake player looks east. Null if it can't be placed. */
    static BoilerElement largeBoiler(TestContext ctx, BlockPos p) {
        var level = ctx.level();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setYRot(270f);
        var d = Eln.transparentNodeItem.getDescriptor(LARGE_BOILER);
        var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, Direction.YN);
        player.setYRot(0f);
        return node == null ? null : (BoilerElement) node.element;
    }

    /** The world positions of a Large Boiler's eight blocks (front XN at origin o): x back, y up, z right. */
    static BlockPos[] cells(BlockPos o) {
        BlockPos[] out = new BlockPos[8];
        int i = 0;
        for (int x = 0; x <= 1; x++)
            for (int y = 0; y <= 1; y++)
                for (int z = 0; z <= 1; z++) out[i++] = o.offset(x, y, z);
        return out;
    }

    /**
     * The 2x2x2 Large Boiler's body: placed, it fills its eight blocks (seven ghosts, each with its face node); a ninth
     * block in the way refuses a second one ("Not enough space"); breaking one ghost takes the whole boiler.
     */
    static final class LargeBoilerBody extends PlumbingCases.Big {
        BoilerElement a;
        BlockPos o, o2;
        boolean refused, ghostsOk, nodesOk, portsOk;
        int tick, thermalFaces, signalFaces;

        public String id() {
            return "large_boiler_2x2_body";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            o = ctx.at(2, 1, 2);
            a = largeBoiler(ctx, o);
            if (a == null) throw new IllegalStateException("the Large Boiler could not be placed");
            ghostsOk = a.front == Direction.XN;
            nodesOk = true;
            BlockPos[] c = cells(o);
            for (int i = 1; i < 8; i++) {
                var co = new mods.eln.misc.Coordonate(c[i], ctx.level());
                ghostsOk &= Eln.ghostManager.getGhost(co) != null && ctx.level().getBlockState(c[i]).getBlock() instanceof mods.eln.ghost.GhostBlock;
                nodesOk &= mods.eln.node.NodeManager.instance.getNodeFromCoordonate(co) instanceof mods.eln.oil.LargeBoilerElement.FaceNode;
            }
            // its faces as designed: 12 thermal (back and flanks), the signal on the origin's front, pipe ports where the pipe tool finds them
            for (BlockPos q : c) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(q, ctx.level()));
                if (n == null) continue;
                for (Direction side : Direction.values()) {
                    int m = n.getSideConnectionMask(side, mods.eln.misc.LRDU.Down);
                    if ((m & mods.eln.node.NodeBase.maskThermal) != 0 && n.getThermalLoad(side, mods.eln.misc.LRDU.Down) == a.thermal) thermalFaces++;
                    if ((m & mods.eln.node.NodeBase.maskElectricalOutputGate) != 0 && n.getElectricalLoad(side, mods.eln.misc.LRDU.Down) == a.phaseOut) signalFaces++;
                }
            }
            var lv = ctx.level();
            portsOk = mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(0, 0, 1), net.minecraft.core.Direction.WEST) != null       // feed, ghost (0,0,1)
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 1, 0), net.minecraft.core.Direction.UP) != null          // steam, ghost (1,1,0)
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 1, 1), net.minecraft.core.Direction.UP) != null          // steam, ghost (1,1,1)
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o, net.minecraft.core.Direction.WEST) == null                         // the signal face: no pipe
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(0, 1, 0), net.minecraft.core.Direction.UP) == null           // top front: plain
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 0, 0), net.minecraft.core.Direction.DOWN) == null;        // underneath: plain
            // a stone where a second one's top back right block would go
            o2 = ctx.at(6, 1, 2);
            ctx.level().setBlockAndUpdate(o2.offset(1, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            refused = largeBoiler(ctx, o2) == null && ctx.level().getBlockState(o2).isAir();
            // break its top back right ghost
            ctx.level().setBlockAndUpdate(c[7], net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }

        public void measure(TestContext ctx) {
            ctx.check("large boiler 2x2: placed facing west, its seven other blocks are ghosts", ghostsOk, "");
            ctx.check("large boiler 2x2: each ghost block has its face node", nodesOk, "");
            ctx.check("large boiler 2x2: twelve thermal faces, all its one shell (back and both flanks)", thermalFaces == 12, "" + thermalFaces);
            ctx.check("large boiler 2x2: one level-signal face (the origin's front)", signalFaces == 1, "" + signalFaces);
            ctx.check("large boiler 2x2: the pipe tool finds the feed flange (front low right) and both steam outlets (top rear) on its ghost blocks, and nothing on plain faces", portsOk, "");
            ctx.check("large boiler 2x2: a block in its volume refuses it, and nothing is placed", refused, "");
            boolean gone = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(o, ctx.level())) == null;
            boolean ghostsGone = true, nodesGone = true;
            BlockPos[] c = cells(o);
            for (int i = 1; i < 8; i++) {
                var co = new mods.eln.misc.Coordonate(c[i], ctx.level());
                ghostsGone &= Eln.ghostManager.getGhost(co) == null && !(ctx.level().getBlockState(c[i]).getBlock() instanceof mods.eln.ghost.GhostBlock);
                nodesGone &= mods.eln.node.NodeManager.instance.getNodeFromCoordonate(co) == null;
            }
            ctx.check("large boiler 2x2: breaking one ghost breaks the boiler", gone && a.node.isDestructing() , "node gone " + gone);
            ctx.check("large boiler 2x2: ... with all its ghost blocks and face nodes", ghostsGone && nodesGone, "ghosts " + ghostsGone + ", face nodes " + nodesGone);
        }
    }

    /**
     * A 16 kW HP plant on one 2x2x2 Large Boiler (todo #472): four Fuel Heat Furnaces with Big burners on a 1.6 bar air
     * main, their backs against four of its twelve thermal faces (the origin's own left flank and three ghost faces);
     * steam out of its rear top right outlet (a ghost block's port) to the HP turbine; the condensate back into its
     * feed flange (front, low right: a ghost block's port). About 67 kW into the water at 20 bar, no fixture. Also its
     * level signal on the origin's front.
     */
    static final class LargeBoilerPlant extends PlumbingCases.Big {
        static final int WATCH = 3000, TICKS = 4800;
        BoilerElement boiler;
        final FuelHeatFurnaceElement[] f = new FuelHeatFurnaceElement[4];
        final ThermalLoad[] fT = new ThermalLoad[4];
        final FluidDevice[] air = new FluidDevice[4];
        final FluidNode[] mains = new FluidNode[4];
        ShaftSteamTurbineElement hp;
        PlantCondenserElement c;
        FluidDevice cooling, meter;
        final Resistor[] loads = new Resistor[2];
        IProcess clock;
        int tick;
        boolean counting;
        double window, waterJ, meterS, pSum, shellSum, shellMax, fillLo = 1e9, fillHi, sigErr;
        final double[] furnaceMax = new double[4];
        double steam0, work0, pumped0;

        public String id() {
            return "large_boiler_2x2_hp_plant_20bar";
        }

        public int ticks() {
            return TICKS;
        }

        public boolean checkLimitCycles() {
            return false;
        }

        public void build(TestContext ctx) {
            BlockPos bp = ctx.at(2, 1, 4), tp = ctx.at(5, 1, 5), cp = ctx.at(6, 1, 5);
            boiler = largeBoiler(ctx, bp);
            if (boiler == null) throw new IllegalStateException("the Large Boiler could not be placed");
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(boiler.desc.waterMb, 0);
            boiler.showTanks();
            // four furnaces: two on its left (north) flank, two on its right (south) flank, lower row
            f[0] = furnace(ctx, bp.north(), Direction.ZN, 1900);                  // the origin's own face
            f[1] = furnace(ctx, bp.offset(1, 0, -1), Direction.ZN, 1900);        // ghost (1,0,0)
            f[2] = furnace(ctx, bp.offset(0, 0, 2), Direction.ZP, 1900);         // ghost (0,0,1), above its feed flange's block
            f[3] = furnace(ctx, bp.offset(1, 0, 2), Direction.ZP, 1900);         // ghost (1,0,1)
            for (int i = 0; i < 4; i++) {
                fT[i] = Batch1Cases.<ThermalLoad>field(f[i], "thermalLoad");
                air[i] = OilCases.FurnacePressure.onFlange(f[i], mains[i] = OilCases.FurnacePressure.main(bp.getY(), 1.6e5, false));
            }
            // steam: from the rear top right outlet (3,2,5) up, east, down into the turbine's inlet
            PipeCases.lay(ctx, ctx.at(3, 3, 5), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(4, 3, 5), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, ctx.at(5, 3, 5), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            PipeCases.lay(ctx, ctx.at(5, 2, 5), 1, 8, 16, 8, 8, 0, 8);
            TransparentNodeElement[] line = ShaftPlantCases.turbineLine(ctx, STEAM_TURBINE, tp, GENERATOR_8KW, GENERATOR_8KW);
            hp = (ShaftSteamTurbineElement) line[0];
            loads[0] = load((GeneratorElement) line[1], 11000);
            loads[1] = load((GeneratorElement) line[2], 11000);
            hp.controls.inventory.setItem(SteamValveControl.REGULATOR_SLOT, Eln.sharedItem.getDescriptor(8 + (3 << 6)).newItemStack());
            hp.controls.targetBar = 20;
            hp.controls.computeInventory();
            c = ShaftPlantCases.condenser(ctx, cp, true, Direction.XP);
            cooling = ShaftPlantCases.coolingLoop(c, cp.south(), 2, cp.north(), 3, 2e5);
            // condensate: out of the condenser's front (east), south to z 7, west to x 1, north to z 5, east into the feed flange
            PipeCases.lay(ctx, ctx.at(7, 1, 5), 1, 0, 4, 8, 8, 4, 8, 8, 8, 8, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(7, 1, 6), 1, 8, 8, 0, 8, 8, 16);
            PipeCases.lay(ctx, ctx.at(7, 1, 7), 1, 8, 8, 0, 8, 8, 8, 0, 8, 8);
            for (int x = 6; x >= 2; x--) PipeCases.lay(ctx, ctx.at(x, 1, 7), 1, 16, 8, 8, 0, 8, 8);
            PipeCases.lay(ctx, ctx.at(1, 1, 7), 1, 16, 8, 8, 8, 8, 8, 8, 8, 0);
            PipeCases.lay(ctx, ctx.at(1, 1, 6), 1, 8, 8, 16, 8, 8, 0);
            PipeCases.lay(ctx, ctx.at(1, 1, 5), 1, 8, 8, 16, 8, 8, 8, 16, 8, 8);
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
                if (tick == 0) {
                    EaUnits.addWater(c.v.vessel, 0.28 * c.v.vessel.volume * Water.RHO_L, 320, false);
                    var run = mods.eln.fluid.FluidWorld.nodeFor(boiler.feed);
                    run.fill(0.995 * run.volume * Water.RHO_L, Water.P_ATM, 300);
                    run.update();
                }
                tick++;
                for (FluidNode m : mains) m.fill(0, Water.P_ATM + 1.6e5, 293.15);
                if (tick == WATCH) {
                    counting = true;
                    steam0 = hp.totalKg;
                    work0 = hp.totalWork;
                    pumped0 = c.v.totalPumpedKg;
                }
                if (tick > WATCH) {
                    window += dt;
                    pSum += boiler.vessel.P() * dt;
                    shellSum += boiler.thermal.Tc * dt;
                    shellMax = Math.max(shellMax, boiler.thermal.Tc);
                    for (int i = 0; i < 4; i++) furnaceMax[i] = Math.max(furnaceMax[i], fT[i].Tc);
                    fillLo = Math.min(fillLo, boiler.fill());
                    fillHi = Math.max(fillHi, boiler.fill());
                    sigErr = Math.max(sigErr, Math.abs(boiler.phaseOut.getU() - Eln.SVU * Math.max(0, Math.min(1, boiler.fill()))));
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(clock);
            mods.eln.fluid.FluidWorld.remove(meter);
            mods.eln.fluid.FluidWorld.remove(cooling);
            for (FluidDevice d : air) mods.eln.fluid.FluidWorld.remove(d);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            String id = id();
            double w = Math.max(1e-9, window);
            double water = waterJ / Math.max(1e-9, meterS), steam = (hp.totalKg - steam0) / w, shaftW = (hp.totalWork - work0) / w;
            double pumped = (c.v.totalPumpedKg - pumped0) / w, p = pSum / w, shell = shellSum / w;
            ctx.line(String.format("%s: %.2f kW into the water, boiler %.2f bar, shell %.0f K over ambient (max %.0f), fill %.0f..%.0f %%; furnaces up to %.0f / %.0f / %.0f / %.0f K over ambient; "
                    + "steam %.2f g/s, shaft %.2f kW, pumped back %.2f g/s; level signal off its gauge glass by up to %.3f V; heat limit %.1f kW at 20 bar",
                id, water / 1000, p / 1e5, shell, shellMax, fillLo * 100, fillHi * 100, furnaceMax[0], furnaceMax[1], furnaceMax[2], furnaceMax[3],
                steam * 1000, shaftW / 1000, pumped * 1000, sigErr, boiler.desc.heatLimitW(20e5) / 1000));
            ctx.line(id + ": boiler " + boiler.getWaila() + "; HP " + hp.getWaila());
            boolean joined = false;
            for (var q : c.fluidPorts()) joined |= mods.eln.fluid.FluidWorld.networkOf(q.node()) == mods.eln.fluid.FluidWorld.networkOf(boiler.feed);
            boolean burnt = boiler.isBurnt() || hp.isBurnt();
            for (var x : f) burnt |= x.isBurnt();
            ctx.check(id + ": nothing burnt out", !burnt, "");
            ctx.check(id + ": the feed flange on a ghost block takes the condensate (one network with the condenser)", joined && mods.eln.fluid.FluidWorld.networkOf(boiler.feed) != null, "");
            ctx.check(id + ": the steam outlet on a ghost block feeds the turbine, and the condensate comes back (90 %+)", steam > 15e-3 && pumped > 0.9 * steam,
                String.format("%.2f of %.2f g/s", pumped * 1000, steam * 1000));
            ctx.check(id + ": four furnaces on four faces (the origin's and three ghosts') put 60 kW+ into the water", water > 60e3, String.format("%.1f kW", water / 1000));
            ctx.check(id + ": every furnace passes its heat on (none runs hot: under 450 K over ambient)",
                furnaceMax[0] < 450 && furnaceMax[1] < 450 && furnaceMax[2] < 450 && furnaceMax[3] < 450,
                String.format("%.0f %.0f %.0f %.0f K", furnaceMax[0], furnaceMax[1], furnaceMax[2], furnaceMax[3]));
            ctx.check(id + ": it holds 20 bar (within 1.5 bar)", Math.abs(p / 1e5 - 20) < 1.5, String.format("%.2f bar", p / 1e5));
            ctx.check(id + ": its shell stays well under its limit (320 K of 400 over ambient)", shellMax < 320, String.format("%.0f K", shellMax));
            ctx.check(id + ": it neither boils dry nor overfills", fillLo > 0.3 && fillHi < 1.02, String.format("%.0f..%.0f %%", fillLo * 100, fillHi * 100));
            ctx.check(id + ": one HP turbine at rated power (16 kW+ on the shaft)", shaftW > 16e3, String.format("%.2f kW", shaftW / 1000));
            ctx.check(id + ": its level signal (the origin's front) follows the gauge glass (within 0.1 V)", sigErr < 0.1 && boiler.phaseOut.getU() > 1, String.format("%.3f V off, %.2f V", sigErr, boiler.phaseOut.getU()));
        }
    }


    /**
     * A Boiler and a Large Boiler, each full to its mark with water at 212 C (20 bar): what would flash if each burst,
     * and the explosion that makes. Nothing is detonated (the explosion would take the neighbouring tests with it): the
     * numbers are the ones BoilerElement hands to Failure when its pressure passes its rating.
     */
    static final class LargeBoilerBurst extends PlumbingCases.Big {
        BoilerElement small, big;
        double eSmall, eBig;
        float sSmall, sBig;

        public String id() {
            return "large_boiler_burst_is_larger";
        }

        public int ticks() {
            return 20;
        }

        BoilerElement hot(TestContext ctx, int id, BlockPos p) {
            return hot((BoilerElement) ctx.placeTransparent(id, p));
        }

        BoilerElement hot(BoilerElement b) {
            b.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            b.fromTanks(b.desc.waterMb, 0);
            b.vessel.energy += b.vessel.mWater * (Water.uLiquid(485.15) - Water.uLiquid(372.15));
            b.vessel.update();
            return b;
        }

        public void build(TestContext ctx) {
            small = hot(ctx, 8 + (82 << 6), ctx.at(2, 1, 2));
            big = largeBoiler(ctx, ctx.at(5, 1, 2));
            hot(big);
            eSmall = small.flashEnergy();
            eBig = big.flashEnergy();
            sSmall = small.burstStrength();
            sBig = big.burstStrength();
            ctx.line(String.format("large_boiler_burst_is_larger: Boiler %.2f kg of water at %.0f C, %.1f bar: %.0f kJ would flash, explosion %.2f (bursts at %.0f bar); "
                    + "Large Boiler %.2f kg at %.0f C, %.1f bar: %.0f kJ, explosion %.2f (bursts at %.0f bar); heat limits at 20 bar %.1f and %.1f kW",
                small.vessel.mWater, small.vessel.T() - Water.T0, small.vessel.P() / 1e5, eSmall / 1000, sSmall, small.desc.burstPa / 1e5,
                big.vessel.mWater, big.vessel.T() - Water.T0, big.vessel.P() / 1e5, eBig / 1000, sBig, big.desc.burstPa / 1e5,
                small.desc.heatLimitW(20e5) / 1000, big.desc.heatLimitW(20e5) / 1000));
        }

        public void measure(TestContext ctx) {
            ctx.check("large boiler: full and hot, it holds four times the Boiler's flash energy (within 10 %)", eSmall > 100e3 && Math.abs(eBig / eSmall / 4 - 1) < 0.10,
                String.format("%.0f against %.0f kJ", eBig / 1000, eSmall / 1000));
            ctx.check("large boiler: the Boiler's burst is TNT at most (4), the Large Boiler's larger, up to 6", sSmall <= 4 && sBig > sSmall + 1 && sBig <= 6,
                String.format("%.2f against %.2f", sBig, sSmall));
            ctx.check("large boiler: the same energy makes the same explosion in both (the formula is shared; only the cap differs)",
                mods.eln.sim.process.destruct.Failure.explosionStrength(60e3, small.desc.maxBoom) == mods.eln.sim.process.destruct.Failure.explosionStrength(60e3, big.desc.maxBoom), "");
            ctx.check("large boiler: it bursts at a higher pressure than the Boiler, and takes four times the heat at 20 bar",
                big.desc.burstPa > small.desc.burstPa && Math.abs(big.desc.heatLimitW(20e5) / small.desc.heatLimitW(20e5) - 4) < 0.01,
                String.format("%.0f against %.0f bar, %.1f against %.1f kW", big.desc.burstPa / 1e5, small.desc.burstPa / 1e5, big.desc.heatLimitW(20e5) / 1000, small.desc.heatLimitW(20e5) / 1000));
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
            if (airPa > 0) {
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

        FullPlant(String id, int ticks) {
            this.id = id;
            this.ticks = ticks;
            endurance = ticks > 4800;
        }
        static final double BAR = 20, AIR_PA = 1.6e5, LOAD_W = 22000;
        final BoilerElement[] b = new BoilerElement[2];
        final FuelHeatFurnaceElement[] f = new FuelHeatFurnaceElement[4];
        final ThermalLoad[] fT = new ThermalLoad[4];
        final FluidDevice[] air = new FluidDevice[4];
        final FluidNode[] mains = new FluidNode[4];
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
                air[i] = OilCases.FurnacePressure.onFlange(f[i], mains[i] = OilCases.FurnacePressure.main(a.getY(), AIR_PA, false));
            }
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
                for (FluidNode m : mains) m.fill(0, Water.P_ATM + AIR_PA, 293.15);
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
            for (FluidDevice d : air) mods.eln.fluid.FluidWorld.remove(d);
            for (Resistor r : loads) Eln.simulator.removeElectricalComponent(r);
            String id = id();
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
            ctx.check(id + ": heavy oil within 10 % of 47.5 buckets/h", Math.abs(mB * 3.6 / 47.5 - 1) < 0.10, String.format("%.1f buckets/h", mB * 3.6));
        }
    }
}
