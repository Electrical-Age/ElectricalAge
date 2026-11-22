package mods.eln.fluid;

import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;

/**
 * A furnace's flue (Baughn 2026-11-03): its hot smoke (eln:flue_gas) out of a pipe flange on top of its stack into an
 * EA pipe, or up the stack into the air if nothing is piped there. The furnace's load gets HEAT_SHARE of its fire's
 * power and the smoke carries SMOKE_SHARE of it over the ambient, so a heat exchanger on the flue gets back a fair bonus
 * ("20 % is about right, but an additional 15 % bonus on top"). A flue choked over CHOKED_PA vents at the furnace.
 * Piped, the smoke goes in as a source inside the fluid step (FluidDevice.Flow, gated shut over CHOKED_PA), not as a lump
 * between steps (todo #498): a small exchanger on the flue sees a steady stream.
 * <p>
 * A chamber (the fuel heat furnace's firebox, todo #504; Baughn: the burner must feel its exhaust's back-pressure): its
 * node is where the combustion air comes in (the furnace's links) and burns. Its combustion is a sink of the air burnt
 * and a source of eln:flue_gas (that air and the fuel), the smoke carrying SMOKE_SHARE of the flame over what the sink
 * took, so the mass and the smoke's heat are kept; unpiped, its stack (a wide vent to the air) lets it out, piped, the
 * pipe on its flange takes it, and a pipe that backs up raises the chamber's pressure against the air coming in.
 */
public final class Flue {
    public static final double HEAT_SHARE = 0.8, SMOKE_SHARE = 0.35;
    /** The smoke leaves the fire at this temperature (K). */
    public static final double SMOKE_K = 573.15, CHOKED_PA = 3e5;
    static final double VOLUME = 0.002;
    /**
     * A chamber's stack (m^3, laminar as every vent): wide enough that an unpiped firebox stays within a few Pa of the
     * air at a Big burner's forced flow (~15 g/s of smoke at ~1100 K).
     */
    public static final double STACK_K = 4e-6;
    /** The hottest a chamber's smoke goes in (K): about a hydrocarbon's adiabatic flame with 20 % excess air. */
    public static final double FLAME_MAX_K = 2500;
    /** Steam a drowned burner flashes leaves into the chamber over about this long (s), not as one lump. */
    public static final double STEAM_S = 2.0;   // (longer than a background fluid step, CoreStep.MAX_SPAN ticks: a rate that holds for it)

    final String key;
    public FluidNode node;
    private double[] loaded;
    /** Smoke going up the stack into the air this step (burning, nothing piped): drawn as smoke. */
    public boolean smoking;
    /**
     * Its smoke into the node, a source inside the fluid step (eln:flue_gas at SMOKE_K, shut over CHOKED_PA); null when
     * the species isn't registered. Its device lists flows() as its fluidFlows. What a step cut short or a choked flue
     * didn't deliver is not carried: it went out at the furnace, as before (the smoke's heat is a bonus, booked nowhere).
     */
    public FluidDevice.Flow flow;
    private java.util.List<FluidDevice.Flow> flows = java.util.List.of();

    /** A chamber (the fuel heat furnace's firebox), or the plain flue (the coal oven's). */
    public final boolean chamber;
    /**
     * A chamber's stack (open unpiped, shut piped), its combustion's sink (the air burnt: what the node holds at its
     * draw) and its drowned burner's steam (a source of water vapour); null for a plain flue.
     */
    public FluidDevice.Vent stack;
    public FluidDevice.Flow sink, steam;
    /** Edges at the node that are its own device's (a chamber's air links): not a pipe on its flange. */
    public java.util.function.Predicate<mods.eln.sim.fluid.FluidEdge> own = e -> false;
    /** Steam still to go into a chamber (kg), and what went in (kg, since loaded). */
    private double steamOwed;
    /** A chamber's meters (kg, since loaded): air its flame burnt, smoke it made. */
    public double airBurntKg, smokeKg;

    public Flue(String key) {
        this(key, false);
    }

    public Flue(String key, boolean chamber) {
        this.key = key;
        this.chamber = chamber;
    }

    /** Make its node at height z (m), restoring what was saved. */
    public void make(double z, double ambientK) {
        node = new FluidNode(VOLUME, z, 0.25, false);
        if (loaded != null) {
            FluidSaves.restore(node, loaded);
            node.update();
        } else node.fill(0, Water.P_ATM, ambientK);
        int id = SpeciesRegistry.get().id("eln:flue_gas");
        if (chamber) {
            flow = FluidDevice.Flow.source(node, 0.5, id >= 2 ? id : SpeciesRegistry.AIR, SMOKE_K);
            flow.maxPa = CHOKED_PA;
            sink = new FluidDevice.Flow(node, 0.5, false);
            sink.phase = mods.eln.fluidcore.Network.Phase.GAS;
            steam = FluidDevice.Flow.source(node, 0.5, SpeciesRegistry.WATER, 400);
            steam.maxPa = CHOKED_PA;
            flows = java.util.List.of(flow, sink, steam);
            // (not gas-only: steam that condensed in it goes too; at its top, level with the air it opens to, so a standing
            // head of the stack's own gas doesn't hold its firebox over the air)
            stack = new FluidDevice.Vent(node, 1, STACK_K);
            stack.open = 1;
        } else if (id >= 2) {
            flow = FluidDevice.Flow.source(node, 0.5, id, SMOKE_K);
            flow.maxPa = CHOKED_PA;
            flows = java.util.List.of(flow);
        } else {
            flow = null;
            flows = java.util.List.of();
        }
    }

    /** Its device's fluidFlows: the smoke source (none before make, or without the species). */
    public java.util.List<FluidDevice.Flow> flows() {
        return flows;
    }

    /**
     * Its port: the flange on top of the stack, the top face's centre, of the block at pos. A chamber's is its flue collar,
     * as wide as its stack (the pipe on it sets the back-pressure), drawing from its bottom (condensed steam goes too).
     */
    public FluidDevice.FluidPort port(long pos) {
        return chamber ? new FluidDevice.FluidPort(node, pos, 1, 0.0, STACK_K) : new FluidDevice.FluidPort(node, pos, 1, 1.0, PortBridge.PORT_K);
    }

    public boolean piped() {
        if (node == null) return false;
        for (var e : FluidWorld.edgesOf(node)) if ((stack == null || e != stack.edge) && !own.test(e)) return true;
        return false;
    }

    /** A chamber's vents: its stack (none before make, or for a plain flue). */
    public java.util.List<FluidDevice.Vent> vents() {
        return stack == null ? java.util.List.of() : java.util.List.of(stack);
    }

    /** A chamber: steam of kg (a drowned burner's flash) to go into it over STEAM_S. */
    public void addSteam(double kg) {
        steamOwed += Math.max(0, kg);
    }

    /**
     * Before a chamber's fluid step: a flame of flameW (W, what the burner draws now) burning airKgS of air (coming in at
     * inletK) and fuelKgS of fuel. The sink takes the air burnt; the source puts in that and the fuel as flue gas, at the
     * temperature whose enthalpy is SMOKE_SHARE of the flame over that of what the sink takes (the node's gas, at its
     * temperature, at least inletK), by bisection on the flue gas's enthalpy, at most FLAME_MAX_K. So what leaves the
     * chamber carries SMOKE_SHARE of the flame over the incoming air, piped or not (fuel_furnace_flue_back_pressure: from
     * the incoming air's enthalpy alone, a piped flue got half of it, the sink taking the hot mix). Returns whether
     * smoking changed.
     */
    public boolean burn(double flameW, double airKgS, double fuelKgS, double inletK, double dt) {
        if (node == null || flow == null) return false;
        boolean piped = piped();
        stack.open = piped ? 0 : 1;
        double q = SMOKE_SHARE * Math.max(0, flameW), smoke = airKgS + fuelKgS;
        sink.rate = Math.max(0, airKgS);
        flow.rate = q > 0 && smoke > 0 ? smoke : 0;
        if (flow.rate > 0) {
            var sp = SpeciesRegistry.get().byId(flow.shares.length - 1);
            double t0 = Math.max(inletK, node.T()), want = sp.hVapour(t0) + q / smoke, lo = t0, hi = FLAME_MAX_K;
            if (sp.hVapour(hi) <= want) lo = hi;
            else for (int i = 0; i < 40; i++) {
                double mid = 0.5 * (lo + hi);
                if (sp.hVapour(mid) < want) lo = mid;
                else hi = mid;
            }
            flow.kelvin = lo;
        }
        steam.rate = steamOwed > 1e-9 && dt > 0 ? Math.max(steamOwed / STEAM_S, 0) : 0;
        boolean was = smoking;
        smoking = !piped && q > 0;
        return smoking != was;
    }

    /** After a chamber's fluid step: book what its flows moved. */
    public void afterBurn(double dt) {
        if (sink == null) return;
        airBurntKg += Math.max(0, sink.delivered) * dt;
        smokeKg += Math.max(0, flow.delivered) * dt;
        steamOwed = Math.max(0, steamOwed - Math.max(0, steam.delivered) * dt);
        if (steamOwed < 1e-9) steamOwed = 0;
    }

    /** Before a fluid step: the smoke of a fire of power fireW (W) into the pipe, as the source's rate; returns whether
     *  smoking changed. */
    public boolean step(double fireW, double dt, double ambientK) {
        if (node == null) return false;
        boolean piped = piped();
        double q = SMOKE_SHARE * fireW;
        if (flow != null) {
            var sp = SpeciesRegistry.get().byId(flow.shares.length - 1);
            flow.rate = piped && q > 0 ? q / Math.max(1, sp.hVapour(SMOKE_K) - sp.hVapour(ambientK)) : 0;
        }
        boolean was = smoking;
        smoking = !piped && q > 0;
        return smoking != was;
    }

    /** After a fluid step: nothing piped, what it held went up the stack (as the gas turbine's exhaust). */
    public void afterStep(double ambientK) {
        if (chamber) return;   // (its stack lets it out)
        if (node != null && !piped()) node.fill(0, Water.P_ATM, ambientK);
    }

    /** Jade's line. */
    public String jade() {
        if (node == null) return "";
        return piped() ? String.format(mods.eln.i18n.I18N.tr("piped, %.2f bar"), FluidWorld.nodeFor(node).P() / 1e5)
            : mods.eln.i18n.I18N.tr("smoke out of its stack (a pipe on its top flange takes it)");
    }

    public void write(CompoundTag nbt) {
        if (node != null) {
            FluidWorld.writeBackOf(node);
            FluidSaves.put(nbt, key, node);
        }
    }

    public void read(CompoundTag nbt) {
        loaded = FluidSaves.array(nbt, key);
    }
}
