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
     * <li>OVERFIRE: the same at 25 kW (a big burner forced): it boils over, Jade says so, and what reaches the receiver
     * chokes a turbine;</li>
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
            return row == Row.RESIDUE ? 600 : 400;
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
                // (calm: the shell a little over the crude, as a burner on its thermostat brings it; overfired: hot)
                case CALM -> charge(pot, 400, 440, sp("crude_oil"), kg);
                case OVERFIRE -> charge(pot, 400, 700, sp("crude_oil"), kg);
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
                    ctx.check("still pot: what reached the receiver chokes a turbine", rating != null && rating.choked(),
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
