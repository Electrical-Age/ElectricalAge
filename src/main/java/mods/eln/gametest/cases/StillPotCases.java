package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.oil.StillPotElement;
import mods.eln.oil.TankElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.FuelQuality;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * The Still Pot (refinery M2, todo #591): its body, ports and faces; a charge heated to the boil; a pot overfired
 * boils over and its product chokes a turbine, one heated sensibly doesn't; boiled down to residue it warns and its
 * watchdog stops it at its shell limit. Each pot stands at o, placed facing west (local x, y, z = world offsets); its
 * vapour flange (top of (1,1,0)) runs up, back over one block and down into a receiver tank behind it, with a breather on
 * the line. Heat goes into its shell as a burner's would, straight into its thermal load.
 */
public final class StillPotCases {
    private StillPotCases() {
    }


    public static void addTo(List<TestCase> cases) {
        cases.add(new Body());
        for (Batch.Row r : Batch.Row.values()) cases.add(new Batch(r));
        cases.add(new Sparge());
        cases.add(new CoilBody());
        cases.add(new SensibleBatch());
        cases.add(new OverfiredBatch());
        cases.add(new SpargeLayers());
        cases.add(new ValvesShutMidBoil());
        cases.add(new ShutInRelief());
        cases.add(new BurstTakesGhosts());
        cases.add(new ResidueDrains());
        cases.add(new SecondPass());
    }


    /** The lines a player sees in Jade without sneaking: the first ElnJadePlugin.HEADLINE. */
    static String headline(java.util.Map<String, String> w) {
        return w.entrySet().stream().limit(mods.eln.compat.jade.ElnJadePlugin.HEADLINE).toList().toString();
    }

    /** A Condenser Coil placed at p (its lower block) by a player looking west (XN). */
    static mods.eln.oil.CondenserCoilElement coil(TestContext ctx, BlockPos p) {
        return (mods.eln.oil.CondenserCoilElement) ctx.placeByPlayer(Ids.transparent("Condenser Coil"), p, 270f);
    }

    /**
     * A whole still from a pot at o (facing west; local offsets = world offsets): its vapour up and over into a Condenser
     * Coil at o+(3,1,0) standing on a pipe; the coil's floor drains down into receiver A at o+(3,0,1) (its low flange); with two
     * receivers, its front low outlet down into receiver B at o+(2,0,-1), each line through a control valve; its gas
     * offtake to a breather; each receiver breathes through a breather on its top. 230 V active thermal dissipators on
     * the coil's back and left, powered by an Electrical Source between them.
     */
    static final class Still {
        final StillPotElement pot;
        final mods.eln.oil.CondenserCoilElement coil;
        final TankElement recvA, recvB;
        mods.eln.node.voxel.ValvePart valveA, valveB;
        final List<mods.eln.sim.mna.component.Component> parts = new java.util.ArrayList<>();
        String err = "";

        Still(TestContext ctx, BlockPos o, boolean two) {
            // the pot stands a block up, on stone, so its vapour flange (y +2) is level with the coil's top: the vapour
            // line never rises above the coil's inlet (a line that does is one lumped run whose lowest point is the pot's
            // flange: liquid in it can't reach the coil, the vapour bubbles through it, and the network crawls)
            for (int x = 0; x <= 1; x++)
                for (int z = 0; z <= 1; z++) ctx.level().setBlockAndUpdate(o.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            pot = pot(ctx, o);
            if (pot == null) throw new IllegalStateException("the Still Pot could not be placed");
            // the coil's floor drain, under it, to receiver A's low north flange (a tank's high flanges draw at their top
            // edge, for gas: liquid piped in there stands in the pipe)
            PipeCases.lay(ctx, o.offset(3, -1, 0), 1, 8, 16, 8, 8, 4, 8, 8, 4, 16);
            // (each control valve close to the coil: a valve draws half way up the pipe run before it, so a long run there
            // fills half full of condensate before anything passes)
            if (two) valveA = valve(ctx, o.offset(3, -1, 0), 8, 12, 8);
            coil = coil(ctx, o.offset(3, 0, 0));
            if (coil == null) throw new IllegalStateException("the Condenser Coil could not be placed");
            // the vapour line: up off the pot's flange, along and down into the coil's top, level with the flange
            PipeCases.lay(ctx, o.offset(1, 2, 0), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, o.offset(2, 2, 0), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, o.offset(3, 2, 0), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            recvA = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, o.offset(3, -1, 1), Direction.XN);
            note(PlumbingCases.Breather.stub(ctx, o.offset(3, 0, 1)));
            if (two) {
                PipeCases.lay(ctx, o.offset(2, 0, 0), 1, 16, 4, 8, 8, 4, 8, 8, 0, 8);
                PipeCases.lay(ctx, o.offset(2, -1, 0), 1, 8, 16, 8, 8, 4, 8, 8, 4, 0);
                valveB = valve(ctx, o.offset(2, 0, 0), 12, 4, 8);
                recvB = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, o.offset(2, -1, -1), Direction.XN);
                note(PlumbingCases.Breather.stub(ctx, o.offset(2, 0, -1)));
            } else recvB = null;
            // the gas offtake: out of the coil's upper front, down and along, a breather on top
            PipeCases.lay(ctx, o.offset(2, 1, 0), 1, 16, 12, 8, 12, 12, 8, 12, 4, 8, 2, 4, 8);
            note(ctx.voxelByEntity(o.offset(2, 1, 0)).addSafetyValveNear(8 / 16.0, 4 / 16.0, 8 / 16.0, mods.eln.node.voxel.SafetyValvePart.BREATHER));
            // dissipators on the coil's back and left flank, each on a stone, powered at 230 V by one Electrical Source
            // between their supply faces
            dissipator(ctx, o.offset(4, 0, 0), Direction.ZN);
            dissipator(ctx, o.offset(3, 0, -1), Direction.XN);
            ctx.level().setBlockAndUpdate(o.offset(4, -1, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            power = ctx.source(Wp11Cases.SOURCE, o.offset(4, 0, -1), Eln.MVU);
        }

        /** The dissipators' supply. */
        mods.eln.sixnode.electricalsource.ElectricalSourceElement power;

        /**
         * The vapour line's iron walls at kelvin, as a still some minutes into its batch has them: from cold, the bare line
         * condenses the first vapour itself (~2 W/K a block, 270 J/K a block to warm) and runs it back into the pot, and the
         * coil sees next to nothing until the walls are hot.
         */
        void warmLine(TestContext ctx, BlockPos o, double kelvin) {
            for (BlockPos q : new BlockPos[]{o.offset(1, 2, 0), o.offset(2, 2, 0), o.offset(3, 2, 0)})
                for (var part : ctx.voxelByEntity(q).parts) if (part instanceof mods.eln.node.voxel.PipePart pp) pp.wallTemp = kelvin;
        }

        /** Every node of the pot's network, by owner (GameTests: what a running still costs). */
        String nodes(TestContext ctx, BlockPos o) {
            var net = mods.eln.fluid.FluidWorld.networkOf(pot.vessel);
            if (net == null) return "none";
            java.util.Map<FluidNode, String> names = new java.util.IdentityHashMap<>();
            names.put(pot.vessel, "pot");
            names.put(mods.eln.fluid.FluidWorld.nodeFor(pot.feed), "pot fill stub (lumped)");
            names.put(mods.eln.fluid.FluidWorld.nodeFor(pot.sparge), "pot steam stub (lumped)");
            names.put(coil.coil, "coil");
            names.put(recvA.vessel, "receiver tank A");
            if (recvB != null) names.put(recvB.vessel, "receiver tank B");
            String[][] blocks = {{"1,2,0", "vapour line"}, {"3,-1,0", "coil drain to A (pipe, valve)"}, {"2,0,0", "coil front outlet to B (pipe, valve)"},
                {"2,-1,0", "pipe to B"}, {"2,1,0", "gas offtake leg (pipe, breather)"}, {"3,0,1", "A's breather stub (pipe)"}, {"2,0,-1", "B's breather stub (pipe)"}};
            for (String[] b : blocks) {
                String[] xyz = b[0].split(",");
                var vn = ctx.voxelByEntity(o.offset(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
                if (vn == null) continue;
                int k = 0;
                for (var part : vn.parts)
                    if (part instanceof mods.eln.node.voxel.PipePart pp && pp.fluid != null) names.putIfAbsent(mods.eln.fluid.FluidWorld.nodeFor(pp.fluid), b[1] + " #" + (k++));
            }
            StringBuilder out = new StringBuilder(net.nodes.size() + " nodes:");
            for (FluidNode n : net.nodes) out.append(" [").append(n.fixed ? "atmosphere (fixed)" : names.getOrDefault(n, "? " + String.format("%.2e m3 z %.2f", n.volume, n.z))).append("]");
            return out.toString();
        }

        void note(String e) {
            if (e != null) err += e + "; ";
        }

        mods.eln.node.voxel.ValvePart valve(TestContext ctx, BlockPos p, int x, int y, int z) {
            note(ctx.voxelByEntity(p).addValve(x, y, z, mods.eln.node.voxel.ValvePart.CONTROL));
            for (var q : ctx.voxelByEntity(p).parts) if (q instanceof mods.eln.node.voxel.ValvePart v) return v;
            return null;
        }

        void dissipator(TestContext ctx, BlockPos p, Direction front) {
            ctx.level().setBlockAndUpdate(p.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            var d = ctx.placeTransparent(Ids.transparent("230V Active Thermal Dissipator"), p);
            d.front = front;
            d.reconnect();
            dissipators.add(d);
        }

        final List<mods.eln.node.transparent.TransparentNodeElement> dissipators = new java.util.ArrayList<>();

        /**
         * A signal from a to b, as a signal cable would carry it: b driven at a's voltage, followed every tick (follow()).
         * (A resistor between the two loads broke the MNA's inter-system split when the test removed its parts.)
         */
        void wire(mods.eln.sim.ElectricalLoad a, mods.eln.sim.ElectricalLoad b) {
            var src = new mods.eln.sim.mna.component.VoltageSource("wire", b, null).setU(a.getU());
            Eln.simulator.addElectricalComponent(src);
            parts.add(src);
            follows.add(new Object[]{a, src});
        }

        final List<Object[]> follows = new java.util.ArrayList<>();

        void follow() {
            for (Object[] f : follows) ((mods.eln.sim.mna.component.VoltageSource) f[1]).setU(((mods.eln.sim.ElectricalLoad) f[0]).getU());
        }

        /** A fixed voltage on a signal load (a signal source set to it). */
        mods.eln.sim.mna.component.VoltageSource hold(mods.eln.sim.ElectricalLoad a, double u) {
            var src = new mods.eln.sim.mna.component.VoltageSource("ref", a, null).setU(u);
            Eln.simulator.addElectricalComponent(src);
            parts.add(src);
            return src;
        }

        void remove() {
            for (var c : parts) Eln.simulator.removeElectricalComponent(c);
        }

        /** Liquid (kg) by species in a receiver, and the share of one. */
        static double share(FluidNode r, int id) {
            double all = 0;
            for (int i = 2; i < r.speciesBound(); i++) all += r.liquid(i);
            return all > 0 ? r.liquid(id) / all : 0;
        }
    }

    /** The Condenser Coil placed: its ghost cell with a face node, six heat faces, its four ports, its model. */
    static final class CoilBody extends PlumbingCases.Big {
        mods.eln.oil.CondenserCoilElement c;
        BlockPos o;
        boolean ghostOk, portsOk;
        int thermalFaces;

        public String id() {
            return "condenser_coil_body";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            o = ctx.at(3, 2, 3);
            PipeCases.lay(ctx, o.below(), 1, 8, 16, 8, 8, 0, 8);   // (it stands on a pipe)
            c = coil(ctx, o);
            if (c == null) throw new IllegalStateException("the Condenser Coil could not be placed");
            var co = new mods.eln.misc.Coordonate(o.above(), ctx.level());
            ghostOk = ctx.level().getBlockState(o.above()).getBlock() instanceof mods.eln.ghost.GhostBlock
                && mods.eln.ghost.GhostBlock.owner(ctx.level(), o.above()) == c
                && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(co) instanceof mods.eln.oil.GhostCells.FaceNode;
            for (BlockPos q : new BlockPos[]{o, o.above()}) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(q, ctx.level()));
                if (n == null) continue;
                for (Direction side : Direction.values()) {
                    int m = n.getSideConnectionMask(side, mods.eln.misc.LRDU.Down);
                    if ((m & mods.eln.node.NodeBase.maskThermal) != 0 && n.getThermalLoad(side, mods.eln.misc.LRDU.Down) == c.thermal) thermalFaces++;
                }
            }
            var lv = ctx.level();
            portsOk = mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.above(), net.minecraft.core.Direction.UP) != null
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.above(), net.minecraft.core.Direction.WEST) != null
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o, net.minecraft.core.Direction.DOWN) != null
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o, net.minecraft.core.Direction.WEST) != null
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o, net.minecraft.core.Direction.EAST) == null;
        }

        public void measure(TestContext ctx) {
            var obj = Eln.obj.getObj("condensercoil");
            ctx.line("condenser coil: " + c.getWaila());
            ctx.check("condenser coil: placed on a pipe facing west, its upper block a ghost with a face node", c.front == Direction.XN && ghostOk, "");
            ctx.check("condenser coil: six thermal faces (back and flanks of both blocks), all its shell", thermalFaces == 6, "" + thermalFaces);
            ctx.check("condenser coil: the pipe tool finds vapour in (top), gas off (front high), condensate (underneath, front low)", portsOk, "");
            ctx.check("condenser coil: its model loads", obj != null && obj.getPart("main") != null, "" + obj);
        }
    }

    /** A bigger box for a whole still. */
    abstract static class StillBox implements TestCase {
        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        public int floorDepth() {
            return 24;
        }
    }

    /**
     * A sensible batch, end to end: 8 B of dead crude in the pot, heated by a real Fuel Heat Furnace (a Big burner, at a
     * third of its setting, on natural draught) against its one heat face (back, lower right); the coil cooled by two active dissipators; two
     * receivers switched by the pot's temperature signal through two OpAmp chips against a 1.82 V reference (182 °C, the
     * gasoline cut's end) driving the control valves: the gasoline receiver's open below it, the other's above. The
     * gasoline cut is at least 90 % gasoline, what comes over after the switch goes to the other receiver, nothing boils
     * over. (The chips' pins are driven at their sources' voltages each tick, as cables would carry them. The vapour
     * line's walls start warm, as some minutes into a batch.)
     */
    static final class SensibleBatch extends StillBox {
        Still s;
        mods.eln.transparentnode.FuelHeatFurnaceElement f;
        IProcess watch;
        int tick, switchedAt = -1;
        double maxEntrain, maxCoilK, aBefore;
        final double[] win = new double[5];
        final mods.eln.transparentnode.FuelGeneratorElement[] gens = new mods.eln.transparentnode.FuelGeneratorElement[2];
        double[] lastCounts;
        mods.eln.sim.fluid.FluidNetwork lastNet;
        int nets;
        String coilJade = "";
        BlockPos o0;
        int boilingHeadlines;

        public String id() {
            return "still_pot_sensible_batch";
        }

        public int ticks() {
            return 3600;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 2, 2);
            o0 = o;
            s = new Still(ctx, o, true);
            double kg = 8 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;
            charge(s.pot, 400, 450, sp("crude_oil"), kg);
            s.warmLine(ctx, o, 420);
            // (against its one heat face, the back of its lower right block)
            ctx.level().setBlockAndUpdate(o.offset(2, -1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            f = HpPlantCases.furnace(ctx, o.offset(2, 0, 1), Direction.XP, 1500);
            f.networkUnserialize(ctx.stream(q -> {
                q.writeByte(mods.eln.transparentnode.FuelHeatFurnaceElement.Companion.getSetManualControlValueEvent());
                q.writeFloat(0.35f);
            }));
            // the cut switch: OpAmp A (pot over the reference) opens receiver B's valve, OpAmp B (reference over the pot) A's
            var opA = ctx.placeSix(Ids.six("OpAmp"), ctx.at(8, 1, 8));
            var opB = ctx.placeSix(Ids.six("OpAmp"), ctx.at(8, 1, 6));
            double ref = (455 - Water.T0) / 100;
            s.wire(s.pot.tempOut, opA.getElectricalLoad(opA.front.inverse()));
            s.hold(opA.getElectricalLoad(opA.front.left()), ref);
            s.wire(opA.getElectricalLoad(opA.front), s.valveA.signal);
            s.hold(opB.getElectricalLoad(opB.front.inverse()), ref);
            s.wire(s.pot.tempOut, opB.getElectricalLoad(opB.front.left()));
            s.wire(opB.getElectricalLoad(opB.front), s.valveB.signal);
            for (int i = 0; i < 2; i++) gens[i] = (mods.eln.transparentnode.FuelGeneratorElement) ctx.placeTransparent(Wp10bCases.FUEL_GENERATOR_50V, ctx.at(15 + 2 * i, 1, 15));
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                s.follow();
                if (tick % 20 == 0 && headline(s.pot.getWaila()).contains("Boiling now")) boilingHeadlines++;
                maxEntrain = Math.max(maxEntrain, s.pot.entrainRate);
                maxCoilK = Math.max(maxCoilK, s.coil.coil.T());
                if (switchedAt < 0 && s.valveA.opening() > 0.5 && s.valveB.opening() < 0.5) switchedAt = tick;
                if (switchedAt < 0) aBefore = s.recvA.vessel.liquidVolume();
                var nw = mods.eln.fluid.FluidWorld.networkOf(s.pot.vessel);
                // (the steady cost, ticks 600-1800: each step's own time and counts, whatever rebuilds the network meanwhile)
                if (nw != null && tick > 600 && tick <= 1800 && nw.coreStats() != null) {
                    var st = nw.coreStats();
                    if (nw != lastNet) {
                        lastNet = nw;
                        lastCounts = null;
                        nets++;
                    }
                    double[] now = {st.iterations, st.solves, st.substeps};
                    if (lastCounts != null) {
                        win[0] += nw.lastStepMs();
                        for (int k = 0; k < 3; k++) win[k + 1] += now[k] - lastCounts[k];
                        win[4]++;
                    }
                    lastCounts = now;
                }
                if (tick % 600 == 0) {
                    var net = mods.eln.fluid.FluidWorld.networkOf(s.pot.vessel);
                    coilJade += " | t" + tick + " pot " + Math.round(s.pot.vessel.T()) + " K shell " + Math.round(s.pot.thermal.Tc + PhysicalConstant.Tref) + " K, valves "
                        + String.format("%.2f/%.2f", s.valveA.opening(), s.valveB.opening()) + ", added " + s.pot.node.isAdded() + " burnt " + s.pot.isBurnt()
                        + " destructing " + s.pot.node.isDestructing() + " coil added " + s.coil.node.isAdded() + ", net " + (net == null ? "none" : net.stats()) + "; coil " + s.coil.getWaila();
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            s.remove();
            // (receiver B, on the coil's front low outlet, takes the gasoline cut; A, on its floor drain, what comes after:
            // the drain's dead leg under the coil fills with the first condensate while A's valve is shut, and goes on to A)
            FluidNode cut = s.recvB.vessel, after = s.recvA.vessel;
            double gas = Still.share(cut, sp("gasoline"));
            ctx.line("still sensible batch: " + s.nodes(ctx, o0));
            if (win[4] > 0)
                ctx.line(String.format("still sensible batch: ticks 600-1800 (%d networks, rebuilt between): %.2f ms a step, %.1f iterations, %.1f solves, %.2f sub-steps a step",
                    nets, win[0] / win[4], win[1] / win[4], win[2] / win[4], win[3] / win[4]));
            ctx.line(String.format("still sensible batch: pot %.0f K, switched at tick %d; gasoline cut %s (gasoline %.1f %%); after the switch %s; coil max %.0f K, holdup %s; entrained max %.3f g/s; furnace %s; coil:%s",
                s.pot.vessel.T(), switchedAt, EaUnits.composition(cut), 100 * gas, EaUnits.composition(after), maxCoilK, EaUnits.composition(s.coil.coil),
                1000 * maxEntrain, f.getWaila(), coilJade));
            ctx.check("still batch: everything fits (valves, breathers)", s.err.isEmpty(), s.err);
            ctx.check("still batch: the pot's signal switched the receivers through the OpAmps and control valves", switchedAt > 0, "" + switchedAt);
            ctx.check("still batch: the gasoline cut is at least 90 % gasoline", gas >= 0.9 && cut.liquid(sp("gasoline")) > 0.02,
                String.format("%.1f %% of %.4f kg", 100 * gas, cut.liquid(sp("gasoline"))));
            // (a piston engine's choke, Baughn 2026-11-24: a sensible still's cuts don't choke a Fuel Generator)
            boolean anyChoked = false;
            FluidNode[] cuts = {cut, after};
            for (int i = 0; i < 2; i++) {
                var v = gens[i].inlet.vessel;
                for (int j = 2; j < cuts[i].speciesBound(); j++) if (cuts[i].liquid(j) > 0) EaUnits.add(v, j, cuts[i].liquid(j), 293.15, false);
                v.update();
                anyChoked |= gens[i].choked();
            }
            ctx.check("still batch: neither receiver's product chokes a Fuel Generator", !anyChoked, "");
            ctx.check("still batch: after the switch the distillate goes to the other receiver", after.liquidVolume() > aBefore + 2e-6,
                String.format("%.1f mL before, %.1f mL now", aBefore * 1e6, after.liquidVolume() * 1e6));
            ctx.check("still batch: while it distils, Jade's headline says what boils off (read each second)", boilingHeadlines > 30, boilingHeadlines + " s");
            ctx.check("still batch: a furnace at a third doesn't boil it over", maxEntrain == 0, String.format("%.4f g/s", 1000 * maxEntrain));
        }
    }

    /**
     * An overfired batch through the coil: 20 B of dead crude, OVERFIRE_W into the shell (over the onset: a big burner
     * pushed) until the shell nears its limit, one receiver. It boils over, residue reaches the receiver, and its product, fed to a Gas Turbine,
     * fouls it. (It doesn't choke it: the coil condenses the light cuts too.)
     */
    static final class OverfiredBatch extends StillBox {
        /**
         * 15 kW: at 25 kW (a big burner forced) the coil, cooled by two dissipators, is overwhelmed (its outlet ~190 °C,
         * ~7.5 g/s of vapour out of its breather) and the network crawls for seconds at a time; reported with a replayable
         * dump. 15 kW boils over and runs clean.
         */
        static final double OVERFIRE_W = 15000;

        Still s;
        GasPlantCases.GasLine gl;
        IProcess heat;
        int tick;
        double residue0;
        double maxEntrain, maxShellK;

        public String id() {
            return "still_pot_overfired_batch";
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            s = new Still(ctx, ctx.at(3, 2, 1), false);
            gl = new GasPlantCases.GasLine(ctx, false, 4000, false);
            double kg = 20 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;
            charge(s.pot, 400, 440, sp("crude_oil"), kg);
            residue0 = s.pot.vessel.mass(sp("residue"));
            Eln.simulator.addSlowProcess(heat = dt -> {
                tick++;
                double shellK = s.pot.thermal.Tc + PhysicalConstant.Tref;
                maxShellK = Math.max(maxShellK, shellK);
                if (shellK < StillPotElement.SHELL_MAX_K - 30) s.pot.thermal.moveEnergyTo(OVERFIRE_W * dt);
                maxEntrain = Math.max(maxEntrain, s.pot.entrainRate);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            s.remove();
            Eln.simulator.removeElectricalComponent(gl.load);
            FluidNode r = s.recvA.vessel, fuel = gl.gt.fuelIn.vessel;
            // (residue never boils and can't pass a breather: it is all still somewhere in the still's network, carried
            // over before each step outside the core's solve, through background steps too)
            double residue = 0;
            var still = mods.eln.fluid.FluidWorld.networkOf(s.pot.vessel);
            if (still != null) for (FluidNode n : still.nodes) residue += n.mass(sp("residue"));
            ctx.check("still overfired: every gram of residue is still in the still (entrainment keeps the books)", Math.abs(residue - residue0) < 1e-6 * residue0 + 1e-9,
                String.format("%.6f -> %.6f kg", residue0, residue));
            // (the product into the turbine's fuel vessel, as a pipe or buckets would take it)
            for (int i = 2; i < r.speciesBound(); i++) if (r.liquid(i) > 0) EaUnits.add(fuel, i, r.liquid(i), 293.15, false);
            fuel.update();
            var net = mods.eln.fluid.FluidWorld.networkOf(s.pot.vessel);
            ctx.line("still overfired batch: network " + (net == null ? "none" : net.stats()));
            ctx.line(String.format("still overfired batch: pot %.0f K, shell max %.0f K, entrained max %.2f g/s; receiver %s; coil %s; turbine fuel %s, choked %s",
                s.pot.vessel.T(), maxShellK, 1000 * maxEntrain, EaUnits.composition(r), s.coil.getWaila(), EaUnits.composition(fuel), gl.gt.choked()));
            ctx.check("still overfired: everything fits", s.err.isEmpty(), s.err);
            ctx.check("still overfired: it boils over", maxEntrain > 0, String.format("%.3f g/s", 1000 * maxEntrain));
            // (what entrainment does, by mass: ~7 g of residue carried through the coil to the receiver at 15 kW; with the
            // coil condensing the light cuts too the product's coking is ~0.03: it fouls engines, chokes none, logged)
            ctx.check("still overfired: carried residue reached the receiver (over 5 g)", r.liquid(sp("residue")) > 0.005, String.format("%.4f kg", r.liquid(sp("residue"))));
            var q = FuelQuality.rate(fuel, sp("diesel"), FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI);
            ctx.line(String.format("still overfired: the product's coking %.3f (turbine choke %.2f, piston %.2f, balance %.2f), the turbine choked: %s", q.coking(),
                FuelQuality.TURBINE_CHOKE, FuelQuality.PISTON_CHOKE, FuelQuality.ENGINE_BALANCE, gl.gt.choked()));
            ctx.check("still overfired: the product fouls an engine and chokes none (over the balance, under the piston choke)",
                q.coking() > FuelQuality.ENGINE_BALANCE && q.coking() < FuelQuality.PISTON_CHOKE && !gl.gt.choked(), String.format("coking %.3f", q.coking()));
        }
    }

    /**
     * Steam sparging through the coil: 10 B of heavy oil at 560 K, a Boiler's steam into the pot's steam inlet, the coil
     * condensing both: the receiver holds water under the oil.
     */
    static final class SpargeLayers extends StillBox {
        Still s;
        mods.eln.oil.BoilerElement boiler;
        IProcess heat;

        public String id() {
            return "still_pot_sparge_layers";
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 2, 2);
            s = new Still(ctx, o, false);
            double kg = 10 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 950;
            charge(s.pot, 560, 580, sp("heavy_oil"), kg);
            boiler = steam(ctx, o);
            double T = Water.tsat(3e5);
            boiler.vessel.fill(0.5 * mods.eln.oil.BoilerElement.RATED_KG, 3e5, T);
            boiler.thermal.Tc = T + 20 - PhysicalConstant.Tref;
            Eln.simulator.addSlowProcess(heat = dt -> {
                boiler.thermal.moveEnergyTo(3000 * dt);
                s.pot.thermal.moveEnergyTo(1500 * dt);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            s.remove();
            FluidNode r = s.recvA.vessel;
            int heavy = sp("heavy_oil");
            ctx.line(String.format("still sparge layers: pot %.0f K; receiver %s, layers %d (water in %d, heavy oil in %d); coil %s; boiler %s",
                s.pot.vessel.T(), EaUnits.composition(r), r.layers(), r.layerOf(SpeciesRegistry.WATER), r.layerOf(heavy), s.coil.getWaila(), boiler.getWaila()));
            ctx.check("still sparge: everything fits", s.err.isEmpty(), s.err);
            ctx.check("still sparge: the receiver holds water under the heavy oil", r.layers() >= 2 && r.layerOf(SpeciesRegistry.WATER) == 0 && r.layerOf(heavy) > 0,
                r.layers() + " layers, water " + r.layerOf(SpeciesRegistry.WATER) + ", oil " + r.layerOf(heavy));
        }
    }

    /**
     * A Boiler beside the pot at o (two blocks to its right, at its rear), its top port up, over and down into the pot's
     * steam inlet low on its right flank; on a stone if the pot stands raised.
     */
    static mods.eln.oil.BoilerElement steam(TestContext ctx, BlockPos o) {
        BlockPos b = o.offset(1, 0, 3);
        if (ctx.level().getBlockState(b.below()).isAir()) ctx.level().setBlockAndUpdate(b.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var boiler = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), b);
        PipeCases.lay(ctx, o.offset(1, 1, 3), 1, 8, 0, 8, 8, 8, 8, 8, 8, 0);
        PipeCases.lay(ctx, o.offset(1, 1, 2), 1, 8, 8, 16, 8, 8, 8, 8, 0, 8);
        PipeCases.lay(ctx, o.offset(1, 0, 2), 1, 8, 16, 8, 8, 4, 8, 8, 4, 0);
        return boiler;
    }

    /** The vapour line from a pot at o: up off its flange, back over a block, down into a receiver tank behind it; a
     *  breather on top of the line. Returns the receiver; err gets the breather's refusal (null: fitted). */
    static TankElement receiver(TestContext ctx, BlockPos o, String[] err) {
        BlockPos line = o.offset(1, 2, 0);
        PipeCases.lay(ctx, line, 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
        PipeCases.lay(ctx, o.offset(2, 2, 0), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
        PipeCases.lay(ctx, o.offset(2, 1, 0), 1, 8, 16, 8, 8, 0, 8);
        TankElement recv = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, o.offset(2, 0, 0), Direction.XP);
        err[0] = ctx.voxelByEntity(line).addSafetyValveNear(12 / 16.0, 8 / 16.0, 8 / 16.0, mods.eln.node.voxel.SafetyValvePart.BREATHER);
        return recv;
    }

    /**
     * Steam sparging (Baughn: the still has a steam input): two pots of heavy oil at 560 K, far under its 700 K boiling
     * point, each with its receiver. A Boiler at 3 bar beside one feeds its steam inlet (low on its right flank). The
     * steam carries heavy oil over at that temperature, the other pot hardly any.
     */
    static final class Sparge extends PlumbingCases.Big {
        final StillPotElement[] pot = new StillPotElement[2];
        final TankElement[] recv = new TankElement[2];
        mods.eln.oil.BoilerElement boiler;
        IProcess heat;
        String err;

        public String id() {
            return "still_pot_steam_sparge";
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            String[] e = new String[1];
            int heavy = sp("heavy_oil");
            double kg = 10 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 950;   // (10 B of heavy oil)
            for (int i = 0; i < 2; i++) {
                BlockPos o = ctx.at(2, 1, 1 + 4 * i);
                pot[i] = pot(ctx, o);
                if (pot[i] == null) throw new IllegalStateException("the Still Pot could not be placed");
                recv[i] = receiver(ctx, o, e);
                if (e[0] != null) err = e[0];
                charge(pot[i], 560, 580, heavy, kg);
            }
            boiler = steam(ctx, ctx.at(2, 1, 5));
            double T = Water.tsat(3e5);
            boiler.vessel.fill(0.5 * mods.eln.oil.BoilerElement.RATED_KG, 3e5, T);
            boiler.thermal.Tc = T + 20 - PhysicalConstant.Tref;
            Eln.simulator.addSlowProcess(heat = dt -> {
                boiler.thermal.moveEnergyTo(4000 * dt);
                for (StillPotElement p : pot) p.thermal.moveEnergyTo(1500 * dt);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            int heavy = sp("heavy_oil");
            double[] got = new double[2];
            for (int i = 0; i < 2; i++) got[i] = recv[i].vessel.liquid(heavy) + recv[i].vessel.vapour(heavy);
            FluidNode r = recv[1].vessel;
            ctx.line(String.format("still pot steam sparge: heavy oil over %.2f g without steam, %.2f g with; pots %.0f / %.0f K; receiver with steam: %s, layers %d (water in %d, oil in %d); boiler %s",
                1000 * got[0], 1000 * got[1], pot[0].vessel.T(), pot[1].vessel.T(), EaUnits.composition(r), r.layers(), r.layerOf(SpeciesRegistry.WATER), r.layerOf(heavy), boiler.getWaila()));
            ctx.check("still pot: the breathers fit", err == null, "" + err);
            ctx.check("still pot: steam carries heavy oil over far under its boiling point (over 5x as much as without)", got[1] > 5 * got[0] && got[1] > 0.005,
                String.format("%.4f vs %.4f kg", got[1], got[0]));
            // (the steam itself goes on through the warm receiver and out of its breather: condensing it, and the water
            // layer under the oil, need the condenser)
            ctx.check("still pot: the boiler's steam went through the pot", boiler.steamRate > 0, String.format("%.1f mB/s", boiler.steamRate));
        }
    }

    /** A Still Pot placed at p by a player looking west (XN). */
    static StillPotElement pot(TestContext ctx, BlockPos p) {
        return (StillPotElement) ctx.placeByPlayer(Ids.transparent("Still Pot"), p, 270f);
    }

    /** Charge the pot: these species (id, kg pairs) as liquid at T (K), air at 1 atm over them; the shell at shellK. */
    static void charge(StillPotElement pot, double T, double shellK, double... idKg) {
        FluidNode v = pot.vessel;
        v.fill(0, Water.P_ATM, T);
        v.mAir = 0;
        for (int i = 0; i < idKg.length; i += 2) EaUnits.add(v, (int) idKg[i], idKg[i + 1], T, false, false);
        v.mAir = Water.P_ATM * Math.max(0.05 * v.volume, v.volume - v.liquidVolume()) / (Water.R_AIR * T);
        v.energy = v.energyAt(T);
        v.update();
        pot.thermal.Tc = shellK - PhysicalConstant.Tref;
    }

    static int sp(String name) {
        return SpeciesRegistry.get().id("eln:" + name);
    }

    /** Placed by a player: seven ghost cells with face nodes, one heat face, the signal, the four ports, its capability. */
    static final class Body extends PlumbingCases.Big {
        StillPotElement a;
        BlockPos o;
        boolean ghostsOk = true, portsOk;
        int thermalFaces, signalFaces;

        public String id() {
            return "still_pot_body";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            o = ctx.at(2, 1, 2);
            a = pot(ctx, o);
            if (a == null) throw new IllegalStateException("the Still Pot could not be placed");
            BlockPos[] c = HpPlantCases.cells(o);
            for (int i = 1; i < 8; i++) {
                var co = new mods.eln.misc.Coordonate(c[i], ctx.level());
                ghostsOk &= ctx.level().getBlockState(c[i]).getBlock() instanceof mods.eln.ghost.GhostBlock
                    && mods.eln.ghost.GhostBlock.owner(ctx.level(), c[i]) == a
                    && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(co) instanceof mods.eln.oil.GhostCells.FaceNode;
            }
            for (BlockPos q : c) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(q, ctx.level()));
                if (n == null) continue;
                for (Direction side : Direction.values()) {
                    int m = n.getSideConnectionMask(side, mods.eln.misc.LRDU.Down);
                    if ((m & mods.eln.node.NodeBase.maskThermal) != 0 && n.getThermalLoad(side, mods.eln.misc.LRDU.Down) == a.thermal) thermalFaces++;
                    if ((m & mods.eln.node.NodeBase.maskElectricalOutputGate) != 0 && n.getElectricalLoad(side, mods.eln.misc.LRDU.Down) == a.tempOut) signalFaces++;
                }
            }
            var lv = ctx.level();
            portsOk = mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 1, 0), net.minecraft.core.Direction.UP) != null      // vapour
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(0, 0, 1), net.minecraft.core.Direction.WEST) != null         // drain
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 0, 0), net.minecraft.core.Direction.NORTH) != null        // fill, left flank
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 0, 1), net.minecraft.core.Direction.SOUTH) != null        // steam, right flank
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(0, 1, 1), net.minecraft.core.Direction.WEST) == null         // upper front: plain
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o, net.minecraft.core.Direction.WEST) == null                          // the signal face
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 1, 1), net.minecraft.core.Direction.UP) == null;           // top: plain
            // unpiped, its fill and steam stubs aren't linked into the pot (no node in its network); a pipe on the fill links it
            linksUnpiped = a.fluidLinks().size();
            PipeCases.lay(ctx, o.offset(1, 0, -1), 0, 8, 4, 16, 8, 4, 8);
            linksPiped = a.fluidLinks().size();
        }

        int linksUnpiped, linksPiped;

        public void measure(TestContext ctx) {
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, o, null);
            var obj = Eln.obj.getObj("stillpot");
            ctx.line("still pot: " + a.getWaila());
            ctx.check("still pot: placed facing west, its seven other blocks are ghosts with face nodes, owned by it", a.front == Direction.XN && ghostsOk, "");
            ctx.check("still pot: one thermal face, its shell (Baughn: one is enough)", thermalFaces == 1, "" + thermalFaces);
            ctx.check("still pot: one temperature-signal face (the origin's front)", signalFaces == 1, "" + signalFaces);
            ctx.check("still pot: the pipe tool finds vapour (top rear left), drain (front), fill and steam (low flanks), nothing on plain faces", portsOk, "");
            ctx.check("still pot: its origin block has its fluid capability", cap == a.getFluidHandler(), "" + cap);
            ctx.check("still pot: unpiped inlets add nothing to its network; a pipe on the fill links its stub", linksUnpiped == 0 && linksPiped == 1,
                linksUnpiped + " then " + linksPiped);
            ctx.check("still pot: its model loads", obj != null && obj.getPart("main") != null, "" + obj);
            ctx.check("still pot: cold, it boils nothing and doesn't entrain", a.boiling() == null && a.entrainRate == 0, a.getWaila().toString());
        }
    }

    /**
     * A charge heated by a burner's power into the shell (a slow process), the vapour line to a receiver behind it.
     * <ul>
     * <li>CALM: 20 B of dead crude near its bubble point, 4 kW (a medium burner): it boils, Jade says what, the signal
     * reads its temperature at 1 V a 100 °C, and nothing is carried over;</li>
     * <li>OVERFIRE: the same at 25 kW (a big burner forced): it boils over, Jade says so, residue reaches the receiver, and
     * the product chokes a Fuel Generator (over PISTON_CHOKE). Whether a whole overfired batch chokes a turbine
     * (the step-0 sketch: 0.26) needs the condenser: without one its vapour must all leave by the breather, the line
     * pressurises and, run longer, the network stalls;</li>
     * <li>RESIDUE: residue with a little heavy oil, 10 kW: "only residue left", then its watchdog burns it at its shell
     * limit, before the shell runs far past it.</li>
     * </ul>
     */
    static final class Batch extends PlumbingCases.Big {
        enum Row { CALM, OVERFIRE, RESIDUE }

        final Row row;
        StillPotElement pot;
        TankElement recv;
        BlockPos o;
        IProcess heat;
        int tick;
        double maxEntrain, maxShellK, maxSignalErr, burnShellK = Double.NaN;
        boolean sawBoiling, sawOver, sawResidue, sawResidueBeforeBurn;
        mods.eln.transparentnode.FuelGeneratorElement gen;
        String err, jade = "", boilingJade = "";

        Batch(Row row) {
            this.row = row;
        }

        public String id() {
            return "still_pot_" + row.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int ticks() {
            return row == Row.CALM ? 400 : 700;
        }

        public void build(TestContext ctx) {
            o = ctx.at(2, 1, 2);
            pot = pot(ctx, o);
            if (pot == null) throw new IllegalStateException("the Still Pot could not be placed");
            String[] e = new String[1];
            recv = receiver(ctx, o, e);
            err = e[0];
            double kg = 20 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;   // (20 B of crude)
            switch (row) {
                // (both from a shell a little over the crude: the burner alone makes the difference. A shell left at 700 K
                // by a last batch boils a fresh charge over on any burner)
                case CALM, OVERFIRE -> charge(pot, 400, 440, sp("crude_oil"), kg);
                case RESIDUE -> charge(pot, 650, 820, sp("residue"), 6 * kg / 20, sp("heavy_oil"), 0.05 * kg / 20);
            }
            if (row == Row.OVERFIRE) gen = (mods.eln.transparentnode.FuelGeneratorElement) ctx.placeTransparent(Wp10bCases.FUEL_GENERATOR_50V, ctx.at(7, 1, 7));
            double watts = row == Row.CALM ? 4000 : row == Row.OVERFIRE ? 25000 : 10000;
            Eln.simulator.addSlowProcess(heat = dt -> {
                if (pot.isBurnt()) {
                    if (Double.isNaN(burnShellK)) burnShellK = pot.thermal.Tc + PhysicalConstant.Tref;
                    return;
                }
                pot.thermal.moveEnergyTo(watts * dt);
                maxShellK = Math.max(maxShellK, pot.thermal.Tc + PhysicalConstant.Tref);
                maxEntrain = Math.max(maxEntrain, pot.entrainRate);
                var w = pot.getWaila();
                if (pot.boiling() != null && !sawBoiling) {
                    sawBoiling = true;
                    boilingJade = headline(w);
                }
                sawOver |= w.keySet().stream().anyMatch(k -> k.equals("Boiling over"));
                if (w.containsKey("Only residue left")) {
                    sawResidue = true;
                    sawResidueBeforeBurn = true;
                }
                double expect = Math.max(0, Math.min(5, (pot.vessel.T() - Water.T0) / 100));
                if (++tick > 10) maxSignalErr = Math.max(maxSignalErr, Math.abs(pot.tempOut.getU() - expect));
                jade = w.toString();
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            FluidNode r = recv.vessel;
            int main = -1;
            double most = 0;
            for (int i = 2; i < r.speciesBound(); i++)
                if (r.liquid(i) > most) {
                    most = r.liquid(i);
                    main = i;
                }
            var rating = main < 0 ? null : FuelQuality.rate(r, main, FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI);
            ctx.line(String.format("still pot (%s): pot %.0f K, shell max %.0f K (burnt at %.0f K), entrained max %.2f g/s, receiver %s, coking %s; jade %s; first boiling %s",
                row, pot.vessel.T(), maxShellK, burnShellK, 1000 * maxEntrain, EaUnits.composition(r), rating == null ? "-" : String.format("%.3f", rating.coking()), jade, boilingJade));
            ctx.check("still pot: the breather fits on its vapour line", err == null, "" + err);
            switch (row) {
                case CALM -> {
                    ctx.check("still pot: a medium burner brings it to the boil, and Jade's headline (what a player sees without sneaking) says what boils off",
                        sawBoiling && boilingJade.contains("Boiling now"), boilingJade);
                    ctx.check("still pot: heated sensibly it doesn't boil over", maxEntrain == 0 && !sawOver, String.format("%.4f g/s", 1000 * maxEntrain));
                    ctx.check("still pot: its signal reads its temperature, 1 V a 100 °C (within 0.05 V)", maxSignalErr < 0.05, String.format("%.3f V off", maxSignalErr));
                    double gas = r.liquid(sp("gasoline")) + r.vapour(sp("gasoline"));
                    ctx.check("still pot: gasoline came over to the receiver", gas > 5e-4, String.format("%.5f kg", gas));
                    ctx.check("still pot: none of it residue (nothing carried over)", r.liquid(sp("residue")) == 0, String.format("%.6f kg", r.liquid(sp("residue"))));
                }
                case OVERFIRE -> {
                    ctx.check("still pot: overfired it boils over, and Jade says so", maxEntrain > 0 && sawOver, String.format("%.4f g/s", 1000 * maxEntrain));
                    ctx.check("still pot: residue reached the receiver", r.liquid(sp("residue")) > 0.001, String.format("%.4f kg", r.liquid(sp("residue"))));
                    // (a piston engine's limit, Baughn 2026-11-24: an overfired product (coking ~0.15) chokes a Fuel Generator; a
                    // gas turbine only fouls on it, it keeps heavy oil. Fed as a pipe or buckets would: into its vessel)
                    for (int i = 2; i < r.speciesBound(); i++) if (r.liquid(i) > 0) EaUnits.add(gen.inlet.vessel, i, r.liquid(i), 293.15, false);
                    gen.inlet.vessel.update();
                    ctx.check("still pot: what reached the receiver chokes a Fuel Generator", gen.choked(),
                        rating == null ? "nothing" : String.format("coking %.3f, %s", rating.coking(), FuelQuality.describe(rating, FuelQuality.PISTON_CHOKE)));
                }
                case RESIDUE -> {
                    ctx.check("still pot: boiled down to residue, Jade warns", sawResidueBeforeBurn, jade);
                    ctx.check("still pot: its watchdog stops it at its shell limit", pot.isBurnt(), String.format("shell max %.0f K", maxShellK));
                    ctx.check("still pot: ... before the shell runs past it (within 5 K)", maxShellK < StillPotElement.SHELL_MAX_K + 5,
                        String.format("%.0f K", maxShellK));
                }
            }
        }
    }

    /**
     * Both receiver valves shut mid-boil (a player switching cuts badly): the coil holds what condenses and its gas offtake
     * vents the rest through its breather, so nothing builds pressure and nothing bursts; the network keeps up (the
     * harness's stall check). A line with no breather on it would pressurise until its weakest pipe burst (an iron pipe's
     * rating, PipePart.burstPa), as any sealed steam plant.
     */
    static final class ValvesShutMidBoil extends StillBox {
        Still s;
        IProcess heat;
        mods.eln.sim.mna.component.VoltageSource openB;
        int tick;
        double maxPotPa, maxCoilPa, coilLiquid0;

        public String id() {
            return "still_pot_valves_shut_mid_boil";
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 2, 2);
            s = new Still(ctx, o, true);
            s.warmLine(ctx, o, 420);
            double kg = 8 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;
            charge(s.pot, 400, 450, sp("crude_oil"), kg);
            openB = s.hold(s.valveB.signal, Eln.SVU);   // (the gasoline receiver open)
            s.hold(s.valveA.signal, 0);
            Eln.simulator.addSlowProcess(heat = dt -> {
                tick++;
                s.pot.thermal.moveEnergyTo(3000 * dt);
                if (tick == 600) {
                    openB.setU(0);   // (both shut)
                    coilLiquid0 = s.coil.coil.liquidVolume();
                }
                if (tick > 600) {
                    maxPotPa = Math.max(maxPotPa, s.pot.vessel.P());
                    maxCoilPa = Math.max(maxCoilPa, s.coil.coil.P());
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            s.remove();
            ctx.line(String.format("still valves shut mid-boil: pot max %.3f bar, coil max %.3f bar, coil liquid %.1f -> %.1f mL; coil %s; pot %s",
                maxPotPa / 1e5, maxCoilPa / 1e5, coilLiquid0 * 1e6, s.coil.coil.liquidVolume() * 1e6, s.coil.getWaila(), s.pot.getWaila()));
            ctx.check("still shut in: everything fits", s.err.isEmpty(), s.err);
            ctx.check("still shut in: the coil holds what condenses (its liquid rises)", s.coil.coil.liquidVolume() > coilLiquid0 + 1e-6,
                String.format("%.1f -> %.1f mL", coilLiquid0 * 1e6, s.coil.coil.liquidVolume() * 1e6));
            ctx.check("still shut in: no pressure builds (its gas offtake's breather vents the rest; under 1.3 bar)", maxPotPa < 1.3e5 && maxCoilPa < 1.3e5,
                String.format("pot %.3f, coil %.3f bar", maxPotPa / 1e5, maxCoilPa / 1e5));
        }
    }

    /**
     * Shut in completely mid-boil (AdventurAgent's demo: both receiver valves shut and liquid over the gas offtake's
     * breather held a copper pot at 13 bar): at tick 600 under 5 kW the gasoline receiver's valve shuts, the offtake's
     * breather goes, the coil's dissipators lose power and the coil stands full of condensate (so what boils off has
     * nowhere to go), and the burner goes up to 10 kW. The pot's own safety valve holds it near RELIEF_PA, venting to the air (counted by species), Jade
     * says so, nothing bursts, the network doesn't stall.
     */
    static final class ShutInRelief extends StillBox {
        Still s;
        IProcess heat;
        mods.eln.sim.mna.component.VoltageSource openB;
        int tick;
        double maxPotPa, minPotPaLate = Double.MAX_VALUE;
        double[] vented0 = new double[0];
        String jade = "";

        public String id() {
            return "still_pot_shut_in_relieves";
        }

        public int ticks() {
            return 3000;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 2, 2);
            s = new Still(ctx, o, true);
            s.warmLine(ctx, o, 420);
            double kg = 8 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;
            charge(s.pot, 400, 450, sp("crude_oil"), kg);
            openB = s.hold(s.valveB.signal, Eln.SVU);
            s.hold(s.valveA.signal, 0);
            Eln.simulator.addSlowProcess(heat = dt -> {
                tick++;
                s.pot.thermal.moveEnergyTo((tick > 600 ? 10000 : 5000) * dt);
                if (tick == 600) {
                    openB.setU(0);
                    vented0 = s.pot.ventedKg.clone();   // (a hot charge flashes a little out of it at first)
                    var off = ctx.voxelByEntity(o.offset(2, 1, 0));
                    for (int i = off.parts.size() - 1; i >= 0; i--) if (off.parts.get(i) instanceof mods.eln.node.voxel.SafetyValvePart) off.removePart(i, false);
                    ctx.setVolts(s.power, 0);
                    // (as the demo's: the coil full of its condensate, nowhere for vapour to condense)
                    var c = s.coil.coil;
                    EaUnits.add(c, sp("gasoline"), Math.max(0, 0.97 * c.volume - c.liquidVolume()) * 740, c.T(), false, false);
                    c.update();
                    mods.eln.fluid.FluidWorld.changed(s.coil);
                }
                if (tick > 600) maxPotPa = Math.max(maxPotPa, s.pot.vessel.P());
                if (tick > 2600) minPotPaLate = Math.min(minPotPaLate, s.pot.vessel.P());
                if (tick > 600 && s.pot.reliefRate > StillPotElement.RELIEVING_KG_S && jade.isEmpty()) jade = "" + s.pot.getWaila();
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            s.remove();
            double vented = 0;
            for (int i = 2; i < s.pot.ventedKg.length; i++) vented += s.pot.ventedKg[i] - (i < vented0.length ? vented0[i] : 0);
            ctx.line(String.format("still shut in: pot max %.3f bar, late min %.3f bar (relief %.2f, burst %.2f), vented %.1f g of oil vapour, relief %.3f g/s; jade relieving %s; pot %s",
                maxPotPa / 1e5, minPotPaLate / 1e5, StillPotElement.RELIEF_PA / 1e5, StillPotElement.BURST_PA / 1e5, vented * 1000, s.pot.reliefRate * 1000, jade, s.pot.getWaila()));
            ctx.check("still shut in: everything fits", s.err.isEmpty(), s.err);
            ctx.check("still shut in: its safety valve holds it near the setting (never 10 % over it)", maxPotPa < 1.1 * StillPotElement.RELIEF_PA && maxPotPa > 0.97 * StillPotElement.RELIEF_PA,
                String.format("%.3f bar", maxPotPa / 1e5));
            ctx.check("still shut in: it vents oil vapour to the air, booked by species (after the shut-in)", vented > 0, String.format("%.4f kg", vented));
            ctx.check("still shut in: Jade says it relieves", jade.contains(mods.eln.i18n.I18N.tr("Relieving")), jade);
            ctx.check("still shut in: the pot is still there (no burst)", s.pot.node != null && !s.pot.node.isDestructing(), "");
        }
    }

    /**
     * A pot that bursts (past BURST_PA, its relief overwhelmed: here the failure itself, as the pot raises it) goes with
     * its seven ghost blocks: no node left in any of its cells (a failure's destruction drops no block, so the ghosts
     * stayed behind).
     */
    static final class BurstTakesGhosts extends PlumbingCases.Big {
        StillPotElement pot;
        final List<BlockPos> cells = new java.util.ArrayList<>();

        public String id() {
            return "still_pot_burst_takes_its_ghosts";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 1, 3);
            pot = pot(ctx, o);
            if (pot == null) throw new IllegalStateException("the Still Pot could not be placed");
            for (int x = 0; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = 0; z <= 1; z++) cells.add(o.offset(x, y, z));
            new mods.eln.sim.process.destruct.Failure(pot).pressureVessel(() -> 0).fail(mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE, StillPotElement.BURST_PA * 1.1);
        }

        public void measure(TestContext ctx) {
            int left = 0;
            for (BlockPos q : cells) if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(q, ctx.level())) != null) left++;
            ctx.check("still pot burst: it and its ghosts are gone (no node in its eight cells)", left == 0 && pot.node.isDestructing(), left + " left");
        }
    }

    /**
     * Hot residue drains, cold barely moves (its viscosity, refinery M1b): two pots of 6 B of residue on stones, one at
     * 520 K, one at 300 K, each breathing through a breather on its vapour flange and drained from its bottoms flange,
     * down a large line at its temperature towards a tank's high side flange; the tank breathes (a hot drain into a closed
     * tank pushes back with its own air). The drain is at the pot's floor, so the hot one empties (into the line, in
     * ~100 ticks), while the cold one loses ~2 %.
     */
    static final class ResidueDrains extends PlumbingCases.Big {
        final StillPotElement[] pot = new StillPotElement[2];
        final TankElement[] tank = new TankElement[2];
        final double[] kelvin = {520, 300};
        final double[][] left = new double[2][6];
        double kg;
        int tick;
        IProcess watch;
        String err;

        public String id() {
            return "still_pot_hot_residue_drains";
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            for (int i = 0; i < 2; i++) {
                BlockPos o = ctx.at(4, 2, 1 + 4 * i);
                for (int x = 0; x <= 1; x++)
                    for (int z = 0; z <= 1; z++) ctx.level().setBlockAndUpdate(o.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                pot[i] = pot(ctx, o);
                if (pot[i] == null) throw new IllegalStateException("the Still Pot could not be placed");
                String e = PlumbingCases.Breather.stub(ctx, o.offset(1, 2, 0));
                if (e != null) err = e;
                // the drain line (a large pipe, as for thick liquid): out of the low flange, down a block, into the tank's high east flange
                PipeCases.lay(ctx, o.offset(-1, 0, 1), 2, 16, 4, 8, 8, 4, 8, 8, 0, 8);
                PipeCases.lay(ctx, o.offset(-1, -1, 1), 2, 8, 16, 8, 8, 12, 8, 0, 12, 8);
                tank[i] = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, o.offset(-2, -1, 1), Direction.XN);
                e = PlumbingCases.Breather.stub(ctx, o.offset(-2, 0, 1));
                if (e != null) err = e;
                for (BlockPos q : new BlockPos[]{o.offset(-1, 0, 1), o.offset(-1, -1, 1)}) PipeCases.warm(ctx, q, kelvin[i]);   // (a line in service)
                kg = 6 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 1050;
                charge(pot[i], kelvin[i], kelvin[i], sp("residue"), kg);
            }
            Eln.simulator.addSlowProcess(watch = dt -> {
                if (tick % 100 == 0 && tick / 100 < 6)
                    for (int i = 0; i < 2; i++) left[i][tick / 100] = pot[i].vessel.liquid(sp("residue")) / kg;
                tick++;
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            double hot = pot[0].vessel.liquid(sp("residue")) / kg, cold = pot[1].vessel.liquid(sp("residue")) / kg;
            ctx.line(String.format("still residue drains: left in the pots (share of 6 B) every 100 ticks: hot (%.0f K) %s, cold (%.0f K) %s; now %.4f, %.5f; "
                + "viscosity hot %s, cold %s; tanks %s, %s", kelvin[0], java.util.Arrays.toString(left[0]), kelvin[1], java.util.Arrays.toString(left[1]), hot, cold,
                EaUnits.viscosity(pot[0].vessel), EaUnits.viscosity(pot[1].vessel), tank[0].getWaila(), tank[1].getWaila()));
            ctx.check("still residue: the breathers fit", err == null, "" + err);
            ctx.check("still residue: the hot pot drains out (under 3 % left)", hot < 0.03, String.format("%.4f left", hot));
            ctx.check("still residue: cold residue barely moves (under a tenth of what the hot lost)", 1 - cold < 0.1 * (1 - hot), String.format("%.5f against %.4f", 1 - cold, 1 - hot));
        }
    }

    /**
     * A second pass of the gasoline heart (the plan: >= 98 %): the sensible batch's gasoline cut (97.6 % gasoline, the rest
     * diesel) through the pot again, cut when the pot passes 385 K (the player's controller: here the test sets the
     * valves): its first receiver is at least 98 % gasoline and holds most of the charge's gasoline. (6 B is ~0.31 kg of
     * gasoline: a game bucket is 67.5 mL.)
     */
    static final class SecondPass extends StillBox {
        Still s;
        IProcess heat;
        mods.eln.sim.mna.component.VoltageSource openA, openB;
        int switchedAt = -1, tick;
        double gasoline0;

        public String id() {
            return "still_pot_second_pass";
        }

        public int ticks() {
            return 3000;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 2, 2);
            s = new Still(ctx, o, true);
            s.warmLine(ctx, o, 400);
            double kg = 6 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 740;
            charge(s.pot, 340, 380, sp("gasoline"), 0.976 * kg, sp("diesel"), 0.024 * kg);
            gasoline0 = 0.976 * kg;
            openB = s.hold(s.valveB.signal, Eln.SVU);
            openA = s.hold(s.valveA.signal, 0);
            Eln.simulator.addSlowProcess(heat = dt -> {
                tick++;
                s.pot.thermal.moveEnergyTo(3000 * dt);
                if (switchedAt < 0 && s.pot.vessel.T() > 385) {
                    switchedAt = tick;
                    openB.setU(0);
                    openA.setU(Eln.SVU);
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            s.remove();
            FluidNode heart = s.recvB.vessel;
            double share = Still.share(heart, sp("gasoline"));
            ctx.line(String.format("still second pass: switched at tick %d (pot %.0f K now, %s); heart %s, %.4f kg (gasoline %.2f %%, %.0f %% of the charge's %.4f kg); tail %s",
                switchedAt, s.pot.vessel.T(), EaUnits.composition(s.pot.vessel), EaUnits.composition(heart), heart.liquid(sp("gasoline")), 100 * share,
                100 * heart.liquid(sp("gasoline")) / gasoline0, gasoline0, EaUnits.composition(s.recvA.vessel)));
            ctx.check("still second pass: everything fits", s.err.isEmpty(), s.err);
            ctx.check("still second pass: the pot's temperature made the cut", switchedAt >= 0, "tick " + switchedAt);
            ctx.check("still second pass: the heart is at least 98 % gasoline", share >= 0.98, String.format("%.2f %%", 100 * share));
            ctx.check("still second pass: the heart holds most of the charge's gasoline (over 70 %)", heart.liquid(sp("gasoline")) > 0.7 * gasoline0,
                String.format("%.4f of %.4f kg", heart.liquid(sp("gasoline")), gasoline0));
        }
    }

}
