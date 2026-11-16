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
 */
public final class Flue {
    public static final double HEAT_SHARE = 0.8, SMOKE_SHARE = 0.35;
    /** The smoke leaves the fire at this temperature (K). */
    public static final double SMOKE_K = 573.15, CHOKED_PA = 3e5;
    static final double VOLUME = 0.002;

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

    public Flue(String key) {
        this.key = key;
    }

    /** Make its node at height z (m), restoring what was saved. */
    public void make(double z, double ambientK) {
        node = new FluidNode(VOLUME, z, 0.25, false);
        if (loaded != null) {
            FluidSaves.restore(node, loaded);
            node.update();
        } else node.fill(0, Water.P_ATM, ambientK);
        int id = SpeciesRegistry.get().id("eln:flue_gas");
        if (id >= 2) {
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

    /** Its port: the flange on top of the stack, the top face's centre, of the block at pos. */
    public FluidDevice.FluidPort port(long pos) {
        return new FluidDevice.FluidPort(node, pos, 1, 1.0, PortBridge.PORT_K);
    }

    public boolean piped() {
        return node != null && !FluidWorld.edgesOf(node).isEmpty();
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
