package mods.eln.fluid;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A fluid-core node as a fluid capability (buckets, other mods' pipes): the one view every EA device with a node uses
 * (Baughn 2026-11-04: an overfilled boiler showed "8000 / 8000" because its view kept the 1.12 tanks' cap while the
 * vessel filled past it; make that structurally impossible). It has no capacity of its own: what it shows is the
 * node's real contents, uncapped, and the room it offers is the node's real free liquid volume up to its liquid share
 * (a boiler's rated fill, a pipe run's 90 %), one budget shared by every slot.
 * <p>
 * Slots: LIQUID, the top layer's main liquid that is a fluid; GAS, the main vapour that is one; INTAKE, any fluid a
 * species is, put in. Before each fluid step the device calls sync (what was put in or taken out since goes into the
 * node), after it show. Quantity views (Jade, meters) read the node too; gauges may clamp (a gauge glass reads full).
 */
public class NodeFluidView extends FluidStacksResourceHandler {
    public static final int LIQUID = 0, GAS = 1, INTAKE = 2;

    /** What a device lets through its view. */
    public static final class Spec {
        boolean liquidIn = true, liquidOut = true, gasIn, gasOut, intake, gasWhenBoiling;
        java.util.function.IntPredicate liquids = id -> true;
        double liquidShare = 1, gasCeilingPa = 9e5, cushionPa;
        boolean aliases;

        /**
         * A layer still close to a mixture alias's proportions (PortBridge.ALIAS_OVERLAP) shows as that alias: the oil
         * pump's live crude is crude to a bucket or another mod's pipe, though no one species of it is 90 %.
         */
        public Spec aliases() {
            aliases = true;
            return this;
        }

        /** Liquid can be put in and taken out (default both). */
        public Spec liquid(boolean in, boolean out) {
            liquidIn = in;
            liquidOut = out;
            return this;
        }

        /** Only these liquid species, by id (a boiler: water). */
        public Spec liquids(java.util.function.IntPredicate which) {
            liquids = which;
            return this;
        }

        /** Gas can be put in (up to ceilingPa) and taken out; whenBoiling: only once the node is at 100 C or more (a cold
         *  boiler's vapour is no steam to give: Baughn). */
        public Spec gas(boolean in, boolean out, double ceilingPa, boolean whenBoiling) {
            gasIn = in;
            gasOut = out;
            gasCeilingPa = ceilingPa;
            gasWhenBoiling = whenBoiling;
            return this;
        }

        /** An intake slot any fluid a species is (liquid; gas too if gas can go in) can be put into. */
        public Spec intake() {
            intake = true;
            return this;
        }

        /** The share of the node's volume liquid may fill through the view (a boiler: its rated fill). */
        public Spec liquidShare(double share) {
            liquidShare = share;
            return this;
        }

        /**
         * A sealed vessel: liquid put in squeezes the gas over it, so the view takes no more than would raise that gas
         * to pa (isothermal: the air's partial pressure times the gas volume is kept, the vapour stays at its own).
         * Without it a tank filled from another mod's pipe went on to 100 % and burst (LabAssistant, review 2026-11-09).
         */
        public Spec cushion(double pa) {
            cushionPa = pa;
            return this;
        }
    }

    private final Spec spec;
    private final int[] shown = new int[3];
    private final Fluid[] shownFluid = new Fluid[3];
    /** Free room at the last show: liquid (m^3, any liquid: 1 mB of every liquid is the same volume), gas (kg of steam). */
    private double roomLiquidM3, roomGasKg;

    public NodeFluidView(Spec spec) {
        super(3, Integer.MAX_VALUE);
        this.spec = spec;
    }

    /** m^3 of liquid per mB (every species: FluidSpecies.kgPerMb scales by density). */
    static final double M3_PER_MB = EaUnits.KG_PER_MB_WATER / Water.RHO_L;

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        var p = FluidSpecies.of(resource.getFluid());
        if (p == null) return false;
        return switch (index) {
            case LIQUID -> !p.vapour() && spec.liquids.test(p.id());
            case GAS -> p.vapour();
            default -> spec.intake && (p.vapour() ? spec.gasIn : spec.liquids.test(p.id()));
        };
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        int have = stacks.get(index).getAmount();
        var p = FluidSpecies.of(resource.getFluid());
        if (p == null) return have;
        if (p.vapour()) {
            double pending = 0;   // (kg of steam-equivalent put in since the last show, every gas slot)
            for (int j = GAS; j <= INTAKE; j++) pending += pendingMb(j, true) * EaUnits.KG_PER_MB_STEAM;
            return have + (int) Math.floor(Math.max(0, roomGasKg - pending) / FluidSpecies.kgPerMb(p.id(), true));
        }
        double pending = (pendingMb(LIQUID, false) + pendingMb(INTAKE, false)) * M3_PER_MB;
        return have + (int) Math.floor(Math.max(0, roomLiquidM3 - pending) / M3_PER_MB);
    }

    /** What was put into slot j since the last show (mB), if it holds liquid (or gas). */
    private int pendingMb(int j, boolean gas) {
        var st = stacks.get(j);
        if (st.isEmpty()) return 0;
        var p = FluidSpecies.of(st.getFluid());
        if (p == null || p.vapour() != gas) return 0;
        return Math.max(0, st.getAmount() - shown[j]);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        boolean ok = switch (index) {
            case LIQUID -> spec.liquidIn;
            case GAS -> spec.gasIn;
            default -> spec.intake;
        };
        return ok ? super.insert(index, resource, amount, tx) : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        boolean ok = switch (index) {
            case LIQUID -> spec.liquidOut;
            case GAS -> spec.gasOut;
            default -> false;
        };
        return ok ? super.extract(index, resource, amount, tx) : 0;
    }

    /** The amount shown in slot (mB). */
    public int amount(int slot) {
        return stacks.get(slot).getAmount();
    }

    /** The fluid shown in slot, or null. */
    public Fluid fluid(int slot) {
        var st = stacks.get(slot);
        return st.isEmpty() ? null : st.getFluid();
    }

    /**
     * Put what was put in or taken out through the view since the last show into the node n (fluids put in arrive at
     * ambientK: other mods' water at the ambient; gas at its boiling point or the ambient).
     */
    public void sync(FluidNode n, double ambientK) {
        if (n == null) return;
        for (int i = LIQUID; i <= INTAKE; i++) {
            var st = stacks.get(i);
            Fluid f = st.isEmpty() ? shownFluid[i] : st.getFluid();
            int d = st.getAmount() - (i == INTAKE ? 0 : shown[i]);
            shown[i] = i == INTAKE ? 0 : st.getAmount();
            if (d == 0 || f == null) continue;
            var ph = FluidSpecies.of(f);
            if (ph == null) continue;
            double kg = Math.abs(d) * FluidSpecies.kgPerMb(ph.id(), ph.vapour());
            if (d > 0) {
                double T = ph.vapour() && ph.id() == SpeciesRegistry.WATER ? Math.max(ambientK, Water.TB) : ambientK;
                EaUnits.add(n, ph.id(), kg, T, ph.vapour());
            } else if (ph.vapour()) {
                if (ph.id() == SpeciesRegistry.WATER) EaUnits.drainVapour(n, kg);
                else EaUnits.drainVapour(n, ph.id(), kg);
            } else if (ph.id() == SpeciesRegistry.WATER) {
                double got = EaUnits.drainLiquid(n, kg);
                if (got < kg && n.T() < Water.TB) EaUnits.drainVapour(n, kg - got);   // (cold: the rest is vapour that would condense)
            } else {
                // (a mixture alias taken out: the layer of its parts, whole)
                double[][] mix = SpeciesRegistry.get().mixture(ph.id(), true);
                EaUnits.drainLayer(n, mix != null ? (int) mix[0][0] : ph.id(), kg);   // (its whole layer by share: a mixed liquid goes as it is)
            }
        }
        set(INTAKE, FluidResource.EMPTY, 0);
    }

    private FluidNode node;

    /** The node last shown (for the invariant GameTests: a view watches its device's real node). */
    public FluidNode node() {
        return node;
    }

    /** Show the node n: its top liquid, its main vapour (if gas comes out), and the room left. */
    public void show(FluidNode n) {
        node = n;
        if (n == null) {
            put(LIQUID, null, 0);
            put(GAS, null, 0);
            roomLiquidM3 = roomGasKg = 0;
        } else {
            int top = FluidSpecies.topLiquid(n);
            int id = top < 0 ? SpeciesRegistry.WATER : top;
            double kg = id == SpeciesRegistry.WATER ? waterShown(n) : EaUnits.layerLiquid(n, id);
            int alias = spec.aliases && id != SpeciesRegistry.WATER && kg > 0 ? aliasOf(n, id) : -1;
            if (alias >= 0) id = alias;
            // (a mixed layer counts as its main species only at PURE_SHARE of it; under that a bucket gets nothing)
            else if (id != SpeciesRegistry.WATER && kg > 0 && n.liquid(id) < EaUnits.PURE_SHARE * kg) kg = 0;
            put(LIQUID, FluidSpecies.fluid(id, false), (int) Math.round(kg / FluidSpecies.kgPerMb(id, false)));
            int gas = FluidSpecies.mainVapour(n);
            int gid = gas < 0 ? SpeciesRegistry.WATER : gas;
            boolean give = spec.gasOut && (!spec.gasWhenBoiling || n.T() >= Water.TB);
            put(GAS, FluidSpecies.fluid(gid, true), give && gas >= 0 ? (int) Math.floor(EaUnits.vapourAvailable(n, gid) / FluidSpecies.kgPerMb(gid, true)) : 0);
            double free = Math.max(0, spec.liquidShare * n.volume - n.liquidVolume());
            if (spec.cushionPa > 0) free = Math.min(free, cushionRoom(n.volume - n.liquidVolume(), n.P(), n.pVapour(), spec.cushionPa));
            roomLiquidM3 = free;
            roomGasKg = spec.gasIn ? Math.max(0, spec.gasCeilingPa - n.P()) * Math.max(1e-6, n.volume - n.liquidVolume())
                / (Water.R_V * Math.max(300, n.T())) : 0;
        }
        set(INTAKE, FluidResource.EMPTY, 0);
        for (int i = LIQUID; i <= GAS; i++) {
            shown[i] = stacks.get(i).getAmount();
            shownFluid[i] = fluid(i) != null ? fluid(i) : shownFluid[i];
        }
        shown[INTAKE] = 0;
    }

    /** The mixture alias (a fluid) that the layer species id is in still looks like, or -1. */
    static int aliasOf(FluidNode n, int id) {
        var reg = SpeciesRegistry.get();
        int layer = n.layerOf(id);
        for (int a = 2; a < reg.size(); a++) {
            double[][] mix = reg.mixture(a, true);
            if (mix == null || FluidSpecies.fluid(a, false) == null || n.layerOf((int) mix[0][0]) != layer) continue;
            if (EaUnits.layerOverlap(n, mix[0], mix[1]) >= PortBridge.ALIAS_OVERLAP) return a;
        }
        return -1;
    }

    /**
     * The liquid volume (m^3) that can go into a sealed gas space of vGas at pressure p (pVapour of it vapour) before the
     * gas reaches pa: the non-condensing part is squeezed isothermally (p - pVapour) * vGas = (pa - pVapour) * vGas'.
     * None once the gas is at pa or over; all of it if there is no gas that doesn't condense.
     */
    public static double cushionRoom(double vGas, double p, double pVapour, double pa) {
        if (vGas <= 0) return 0;
        double pNc = p - pVapour, head = pa - pVapour;
        if (head <= 0 || p >= pa) return 0;
        if (pNc <= 0) return vGas;
        return Math.max(0, vGas - pNc * vGas / head);
    }

    /** Water a bucket can take (kg): the liquid, and while it's under boiling its vapour (it would condense: a bucket
     *  poured into a cold box comes back 999 mB, a little evaporated into its air). */
    static double waterShown(FluidNode n) {
        return n.T() < Water.TB ? n.mWater : n.mLiquid();
    }

    /** Taken out of slot through the view since the last show (mB). */
    public int takenOut(int slot) {
        return Math.max(0, shown[slot] - stacks.get(slot).getAmount());
    }

    /** Set slot's contents directly (a save from before the device had a node: its old tank amounts). */
    protected void put(int index, Fluid fluid, int amount) {
        set(index, amount > 0 && fluid != null ? FluidResource.of(fluid) : FluidResource.EMPTY, fluid == null ? 0 : Math.max(0, amount));
        if (fluid != null) shownFluid[index] = fluid;
    }
}
