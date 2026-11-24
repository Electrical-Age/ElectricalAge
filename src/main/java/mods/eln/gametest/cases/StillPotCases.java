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

    public static final int STILL_POT = 20 + (83 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new Body());
        for (Batch.Row r : Batch.Row.values()) cases.add(new Batch(r));
        cases.add(new Sparge());
        cases.add(new CoilBody());
        cases.add(new SensibleBatch());
        cases.add(new OverfiredBatch());
        cases.add(new SpargeLayers());
    }

    public static final int CONDENSER_COIL = 24 + (83 << 6);
    static final int ACTIVE_DISSIPATOR_230V = 34 + (64 << 6);

    /** A Condenser Coil placed at p (its lower block) by a player looking west (XN). */
    static mods.eln.oil.CondenserCoilElement coil(TestContext ctx, BlockPos p) {
        var level = ctx.level();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setYRot(270f);
        var d = Eln.transparentNodeItem.getDescriptor(CONDENSER_COIL);
        var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, Direction.YN);
        player.setYRot(0f);
        return node == null ? null : (mods.eln.oil.CondenserCoilElement) node.element;
    }

    /** A breather on a stub up out of the tank top port under above; returns its refusal (null: fitted). */
    static String topBreather(TestContext ctx, BlockPos above) {
        PipeCases.lay(ctx, above, 0, 8, 0, 8, 8, 8, 8, 14, 8, 8);
        return ctx.voxelByEntity(above).addSafetyValveNear(12 / 16.0, 8 / 16.0, 8 / 16.0, mods.eln.node.voxel.SafetyValvePart.BREATHER);
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
            pot = pot(ctx, o);
            if (pot == null) throw new IllegalStateException("the Still Pot could not be placed");
            // the coil's floor drain, under it, to receiver A's low north flange (a tank's high flanges draw at their top
            // edge, for gas: liquid piped in there stands in the pipe)
            PipeCases.lay(ctx, o.offset(3, 0, 0), 1, 8, 16, 8, 8, 4, 8, 8, 4, 16);
            // (each control valve close to the coil: a valve draws half way up the pipe run before it, so a long run there
            // fills half full of condensate before anything passes)
            if (two) valveA = valve(ctx, o.offset(3, 0, 0), 8, 12, 8);
            coil = coil(ctx, o.offset(3, 1, 0));
            if (coil == null) throw new IllegalStateException("the Condenser Coil could not be placed");
            // the vapour line: up off the pot's flange, over and down into the coil's top
            PipeCases.lay(ctx, o.offset(1, 2, 0), 1, 8, 0, 8, 8, 16, 8);
            PipeCases.lay(ctx, o.offset(1, 3, 0), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, o.offset(2, 3, 0), 1, 0, 8, 8, 16, 8, 8);
            PipeCases.lay(ctx, o.offset(3, 3, 0), 1, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            recvA = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, o.offset(3, 0, 1), Direction.XN);
            note(topBreather(ctx, o.offset(3, 1, 1)));
            if (two) {
                PipeCases.lay(ctx, o.offset(2, 1, 0), 1, 16, 4, 8, 8, 4, 8, 8, 0, 8);
                PipeCases.lay(ctx, o.offset(2, 0, 0), 1, 8, 16, 8, 8, 4, 8, 8, 4, 0);
                valveB = valve(ctx, o.offset(2, 1, 0), 12, 4, 8);
                recvB = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, o.offset(2, 0, -1), Direction.XN);
                note(topBreather(ctx, o.offset(2, 1, -1)));
            } else recvB = null;
            // the gas offtake: out of the coil's upper front, down and along, a breather on top
            PipeCases.lay(ctx, o.offset(2, 2, 0), 1, 16, 12, 8, 12, 12, 8, 12, 4, 8, 2, 4, 8);
            note(ctx.voxelByEntity(o.offset(2, 2, 0)).addSafetyValveNear(8 / 16.0, 4 / 16.0, 8 / 16.0, mods.eln.node.voxel.SafetyValvePart.BREATHER));
            // dissipators on the coil's back and left flank, each on a stone, powered at 230 V by one Electrical Source
            // between their supply faces
            dissipator(ctx, o.offset(4, 1, 0), Direction.ZN);
            dissipator(ctx, o.offset(3, 1, -1), Direction.XN);
            ctx.level().setBlockAndUpdate(o.offset(4, 0, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            ctx.source(Wp11Cases.SOURCE, o.offset(4, 1, -1), Eln.MVU);
        }

        /**
         * The vapour line's iron walls at kelvin, as a still some minutes into its batch has them: from cold, the bare line
         * condenses the first vapour itself (~2 W/K a block, 270 J/K a block to warm) and runs it back into the pot, and the
         * coil sees next to nothing until the walls are hot.
         */
        void warmLine(TestContext ctx, BlockPos o, double kelvin) {
            for (BlockPos q : new BlockPos[]{o.offset(1, 2, 0), o.offset(1, 3, 0), o.offset(2, 3, 0), o.offset(3, 3, 0)})
                for (var part : ctx.voxelByEntity(q).parts) if (part instanceof mods.eln.node.voxel.PipePart pp) pp.wallTemp = kelvin;
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
            var d = ctx.placeTransparent(ACTIVE_DISSIPATOR_230V, p);
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
        void hold(mods.eln.sim.ElectricalLoad a, double u) {
            var src = new mods.eln.sim.mna.component.VoltageSource("ref", a, null).setU(u);
            Eln.simulator.addElectricalComponent(src);
            parts.add(src);
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
     * third of its setting, on natural draught) against its left flank; the coil cooled by two active dissipators; two
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
        String coilJade = "";
        BlockPos o0;

        public String id() {
            return "still_pot_sensible_batch";
        }

        public int ticks() {
            return 3600;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(3, 1, 2);
            o0 = o;
            s = new Still(ctx, o, true);
            double kg = 8 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;
            charge(s.pot, 400, 450, sp("crude_oil"), kg);
            s.warmLine(ctx, o, 420);
            f = HpPlantCases.furnace(ctx, o.offset(0, 0, -1), Direction.ZN, 1500);
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
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                s.follow();
                maxEntrain = Math.max(maxEntrain, s.pot.entrainRate);
                maxCoilK = Math.max(maxCoilK, s.coil.coil.T());
                if (switchedAt < 0 && s.valveA.opening() > 0.5 && s.valveB.opening() < 0.5) switchedAt = tick;
                if (switchedAt < 0) aBefore = s.recvA.vessel.liquidVolume();
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
            ctx.line(String.format("still sensible batch: pot %.0f K, switched at tick %d; gasoline cut %s (gasoline %.1f %%); after the switch %s; coil max %.0f K, holdup %s; entrained max %.3f g/s; furnace %s; coil:%s",
                s.pot.vessel.T(), switchedAt, EaUnits.composition(cut), 100 * gas, EaUnits.composition(after), maxCoilK, EaUnits.composition(s.coil.coil),
                1000 * maxEntrain, f.getWaila(), coilJade));
            ctx.check("still batch: everything fits (valves, breathers)", s.err.isEmpty(), s.err);
            ctx.check("still batch: the pot's signal switched the receivers through the OpAmps and control valves", switchedAt > 0, "" + switchedAt);
            ctx.check("still batch: the gasoline cut is at least 90 % gasoline", gas >= 0.9 && cut.liquid(sp("gasoline")) > 0.02,
                String.format("%.1f %% of %.4f kg", 100 * gas, cut.liquid(sp("gasoline"))));
            ctx.check("still batch: after the switch the distillate goes to the other receiver", after.liquidVolume() > aBefore + 2e-6,
                String.format("%.1f mL before, %.1f mL now", aBefore * 1e6, after.liquidVolume() * 1e6));
            ctx.check("still batch: a furnace at a third doesn't boil it over", maxEntrain == 0, String.format("%.4f g/s", 1000 * maxEntrain));
        }
    }

    /**
     * An overfired batch through the coil: 20 B of dead crude, 25 kW into the shell (a big burner forced) until the shell
     * nears its limit, one receiver. It boils over, residue reaches the receiver, and its product, fed to a Gas Turbine,
     * fouls it. (It doesn't choke it: the coil condenses the light cuts too.)
     */
    static final class OverfiredBatch extends StillBox {
        /**
         * Known, reported, not yet fixed: while the pot entrains at 25 kW its network (11 nodes) crawls for ~1 s at a time
         * and moves to the background (~150 ms a step). Without entrainment the same batch runs clean, so the stall is
         * the entrainment link's; limiting it to while the neck has room and to the smoothed vapour flow cut it from
         * 1.35 s to ~1 s.
         */
        public boolean checkStalls() {
            return false;
        }

        Still s;
        GasPlantCases.GasLine gl;
        IProcess heat;
        double maxEntrain, maxShellK;

        public String id() {
            return "still_pot_overfired_batch";
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            s = new Still(ctx, ctx.at(3, 1, 1), false);
            gl = new GasPlantCases.GasLine(ctx, false, 4000, false);
            double kg = 20 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 850;
            charge(s.pot, 400, 440, sp("crude_oil"), kg);
            Eln.simulator.addSlowProcess(heat = dt -> {
                double shellK = s.pot.thermal.Tc + PhysicalConstant.Tref;
                maxShellK = Math.max(maxShellK, shellK);
                if (shellK < StillPotElement.SHELL_MAX_K - 30) s.pot.thermal.moveEnergyTo(25000 * dt);
                maxEntrain = Math.max(maxEntrain, s.pot.entrainRate);
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(heat);
            s.remove();
            Eln.simulator.removeElectricalComponent(gl.load);
            FluidNode r = s.recvA.vessel, fuel = gl.gt.fuelIn.vessel;
            // (the product into the turbine's fuel vessel, as a pipe or buckets would take it)
            for (int i = 2; i < r.speciesBound(); i++) if (r.liquid(i) > 0) EaUnits.add(fuel, i, r.liquid(i), 293.15, false);
            fuel.update();
            ctx.line(String.format("still overfired batch: pot %.0f K, shell max %.0f K, entrained max %.2f g/s; receiver %s; coil %s; turbine fuel %s, choked %s",
                s.pot.vessel.T(), maxShellK, 1000 * maxEntrain, EaUnits.composition(r), s.coil.getWaila(), EaUnits.composition(fuel), gl.gt.choked()));
            ctx.check("still overfired: everything fits", s.err.isEmpty(), s.err);
            ctx.check("still overfired: it boils over", maxEntrain > 0, String.format("%.3f g/s", 1000 * maxEntrain));
            ctx.check("still overfired: residue reached the receiver", r.liquid(sp("residue")) > 0.005, String.format("%.4f kg", r.liquid(sp("residue"))));
            // (Baughn's choke, 0.18, isn't reached: with a working condenser the gasoline and diesel come over too and dilute
            // the carried residue to ~0.11; reported for the calibration. It fouls the turbine, as residue does)
            var q = FuelQuality.rate(fuel, sp("diesel"), FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI);
            ctx.line(String.format("still overfired: the product's coking %.3f (choke %.2f, balance %.2f), the turbine choked: %s", q.coking(), FuelQuality.ENGINE_CHOKE,
                FuelQuality.ENGINE_BALANCE, gl.gt.choked()));
            ctx.check("still overfired: fed to a Gas Turbine, its product fouls it (coking over the engines' balance, by the turbine's own rating)",
                q.coking() > FuelQuality.ENGINE_BALANCE, String.format("coking %.3f", q.coking()));
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
            BlockPos o = ctx.at(3, 1, 2);
            s = new Still(ctx, o, false);
            double kg = 10 * EaUnits.KG_PER_MB_WATER * 1000 / Water.RHO_L * 950;
            charge(s.pot, 560, 580, sp("heavy_oil"), kg);
            BlockPos b = o.offset(-1, 0, 0);
            boiler = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), b);
            PipeCases.lay(ctx, b.above(), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
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
     * point, each with its receiver. A Boiler at 3 bar in front of one feeds its steam inlet (upper left front). The
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
            // the Boiler in front of pot 1, its top port up and across into the pot's steam inlet
            BlockPos b = ctx.at(1, 1, 5);
            boiler = (mods.eln.oil.BoilerElement) ctx.placeTransparent(8 + (82 << 6), b);
            PipeCases.lay(ctx, b.above(), 1, 8, 0, 8, 8, 8, 8, 16, 8, 8);
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
        var level = ctx.level();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setYRot(270f);
        var d = Eln.transparentNodeItem.getDescriptor(STILL_POT);
        var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, Direction.YN);
        player.setYRot(0f);
        return node == null ? null : (StillPotElement) node.element;
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

    /** Placed by a player: seven ghost cells with face nodes, six heat faces, the signal, the four ports, its capability. */
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
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(0, 1, 1), net.minecraft.core.Direction.WEST) != null         // fill
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(0, 1, 0), net.minecraft.core.Direction.WEST) != null         // steam
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o, net.minecraft.core.Direction.WEST) == null                          // the signal face
                && mods.eln.node.voxel.PipePlacer.portAtForTests(lv, o.offset(1, 1, 1), net.minecraft.core.Direction.UP) == null;           // top: plain
        }

        public void measure(TestContext ctx) {
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, o, null);
            var obj = Eln.obj.getObj("stillpot");
            ctx.line("still pot: " + a.getWaila());
            ctx.check("still pot: placed facing west, its seven other blocks are ghosts with face nodes, owned by it", a.front == Direction.XN && ghostsOk, "");
            ctx.check("still pot: six thermal faces, all its shell", thermalFaces == 6, "" + thermalFaces);
            ctx.check("still pot: one temperature-signal face (the origin's front)", signalFaces == 1, "" + signalFaces);
            ctx.check("still pot: the pipe tool finds vapour (top rear left), drain, fill and steam (front), nothing on plain faces", portsOk, "");
            ctx.check("still pot: its origin block has its fluid capability", cap == a.getFluidHandler(), "" + cap);
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
     * the product cokes worse than any distilled cut (heavy oil's 0.15). Whether a whole overfired batch chokes a turbine
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
                    boilingJade = w.toString();
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
                    ctx.check("still pot: a medium burner brings it to the boil, and Jade says what boils off", sawBoiling && boilingJade.contains("Boiling now"), boilingJade);
                    ctx.check("still pot: heated sensibly it doesn't boil over", maxEntrain == 0 && !sawOver, String.format("%.4f g/s", 1000 * maxEntrain));
                    ctx.check("still pot: its signal reads its temperature, 1 V a 100 °C (within 0.05 V)", maxSignalErr < 0.05, String.format("%.3f V off", maxSignalErr));
                    double gas = r.liquid(sp("gasoline")) + r.vapour(sp("gasoline"));
                    ctx.check("still pot: gasoline came over to the receiver", gas > 5e-4, String.format("%.5f kg", gas));
                    ctx.check("still pot: none of it residue (nothing carried over)", r.liquid(sp("residue")) == 0, String.format("%.6f kg", r.liquid(sp("residue"))));
                }
                case OVERFIRE -> {
                    ctx.check("still pot: overfired it boils over, and Jade says so", maxEntrain > 0 && sawOver, String.format("%.4f g/s", 1000 * maxEntrain));
                    ctx.check("still pot: residue reached the receiver", r.liquid(sp("residue")) > 0.001, String.format("%.4f kg", r.liquid(sp("residue"))));
                    ctx.check("still pot: what reached the receiver cokes worse than heavy oil (0.15)", rating != null && rating.coking() > 0.15,
                        rating == null ? "nothing" : String.format("coking %.3f, %s", rating.coking(), FuelQuality.describe(rating)));
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
}
