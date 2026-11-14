package mods.eln.fluid;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Vessels;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * A consuming device's feed vessel and inlet on the fluid core (a PortBridge's pull becomes a vessel the line fills):
 * a small stub node behind its flange (it merges into the pipe that meets it), the vessel that holds the feed, and a
 * filling valve from the stub into the vessel's top: a check valve the device opens by the room left under the
 * vessel's filling level (Vessels.fillOpening), so the vessel keeps a gas cap and never sees the line's pressure.
 * <p>
 * The inlet is passive: the line's pressure or head fills it (the old pull took from the whole run at its rate,
 * whatever the pressure). The vessel breathes through a float vent and a vacuum breaker, as a DeviceOutlet's basin, so
 * gas that comes down the line leaves by it.
 * <p>
 * The feed is the lowest liquid layer the device takes: one whose main species it takes, or one still in the
 * proportions of a mixture it takes (crude), drained whole (EaUnits.drainLayer). Water under it runs out of a drain
 * at the vessel's floor, onto the ground (a still's stripping steam condenses in every cut; nothing else gets it out
 * from under oil).
 * Any other liquid stays, and with no feed over it the device is blocked until the right feed dilutes it.
 * <p>
 * The device works on the vessel between sync and settle, in its beforeFluidStep, so the view never shows or takes
 * what the device has since used.
 */
public final class DeviceInlet {
    public static final double PORT_K = PortBridge.PORT_K, STUB_VOLUME = PortBridge.STUB_VOLUME, VENT_K = DeviceOutlet.VENT_K;
    /** What the drain passes (mB of water a second): a still's condensate and more, but a water main piped into the
     *  inlet by mistake fills the vessel and stops at the filling valve, it doesn't empty onto the floor (LabAgent). */
    public static final double DRAIN_MB_PER_S = 100;

    final String key;
    final double vesselM3;
    public FluidNode vessel, stub;
    FluidDevice.Link valve;
    FluidDevice.Vent tip, airIn;
    /** Its fluid capability: the feeds it takes go in; a layer still in crude's proportions shows as crude. */
    public final NodeFluidView view;
    private final Fluid[] feeds;

    private double[] loaded, loadedStub;
    private net.neoforged.neoforge.fluids.FluidStack legacyTank;

    /** Water run out of its drain in all (kg). */
    public double drainedKg;

    /**
     * key: the stub's save key (the old PortBridge's, whose stub it is; the vessel is saved as keyVessel);
     * ratedMb: the old tank's size, which is the vessel's filling level; feeds: the fluids the device takes.
     */
    public DeviceInlet(String key, double ratedMb, Fluid... feeds) {
        this.key = key;
        this.vesselM3 = ratedMb / Vessels.FILL_LEVEL * NodeFluidView.M3_PER_MB;
        this.feeds = feeds;
        this.view = new NodeFluidView(new NodeFluidView.Spec().intake().liquidShare(Vessels.FILL_LEVEL).aliases().liquids(this::takes));
    }

    /** Is species id (or the mixture id names) one of its feeds. */
    public boolean takes(int id) {
        for (Fluid f : feeds) {
            var ph = FluidSpecies.of(f);
            if (ph != null && ph.id() == id) return true;
        }
        return false;
    }

    /** Make the nodes: the vessel at y (its floor, m; one block tall), the stub at its flange's height. */
    public void make(double y, double flangeY, double ambientK) {
        FluidNode v = new FluidNode(vesselM3, y, 1.0, false);
        FluidNode s = new FluidNode(STUB_VOLUME, flangeY, 0.1, false);
        s.fill(0, Water.P_ATM, ambientK);
        if (loaded != null) {
            FluidSaves.restore(v, loaded);
            v.update();
        } else {
            // (an old save: its tank goes into the vessel at the ambient)
            v.fill(0, Water.P_ATM, ambientK);
            if (legacyTank != null && !legacyTank.isEmpty()) {
                var ph = FluidSpecies.of(legacyTank.getFluid());
                if (ph != null) EaUnits.add(v, ph.id(), legacyTank.getAmount() * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), ambientK, ph.vapour());
            }
        }
        if (loadedStub != null) {
            FluidSaves.restore(s, loadedStub);
            s.update();
        }
        s.lumpable = true;   // (it merges into the pipe on its flange)
        s.resistance = 1 / PORT_K;
        loaded = loadedStub = null;
        legacyTank = null;
        vessel = v;
        stub = s;
        valve = new FluidDevice.Link(s, 0.5, v, 1.0, PORT_K).kind(FluidEdge.Kind.CHECK);
        valve.open = 0;
        tip = new FluidDevice.Vent(v, 1.0, VENT_K);
        tip.gasOnly = true;
        airIn = new FluidDevice.Vent(v, 1.0, VENT_K);
        airIn.inward = true;
        view.show(v);
    }

    public FluidDevice.FluidPort port(long pos, int face, int at) {
        return new FluidDevice.FluidPort(stub, pos, face, 0.5, PORT_K, at);
    }

    public List<FluidDevice.Link> links() {
        return valve == null ? List.of() : List.of(valve);
    }

    public List<FluidDevice.Vent> vents() {
        return tip == null ? List.of() : List.of(tip, airIn);
    }

    /** Before a fluid step, first: what was put in or taken out through the view, then the water out of the drain. */
    public void sync(double dt, double ambientK) {
        if (vessel == null) return;
        view.sync(vessel, ambientK);
        drainedKg += EaUnits.drainLiquid(vessel, Math.min(vessel.mLiquid(), DRAIN_MB_PER_S * EaUnits.KG_PER_MB_WATER * dt));
    }

    /** Before a fluid step, last (the device has used what it uses): the filling valve and the breather. */
    public void settle(double dt) {
        if (vessel == null) return;
        valve.open = valve.edge == null ? 0 : Vessels.fillOpening(valve.edge, vessel, dt);
        double dp = vessel.P() - Water.P_ATM;
        double fl = Math.max(0, Math.min(1, (DeviceOutlet.FLOAT_SHUT - vessel.liquidVolume() / vessel.volume) / 0.02));
        tip.open = fl * Math.max(0, Math.min(1, (dp - DeviceOutlet.VENT_DEADBAND_PA) / DeviceOutlet.VENT_DEADBAND_PA));
        airIn.open = Math.max(0, Math.min(1, (-dp - DeviceOutlet.VENT_DEADBAND_PA) / DeviceOutlet.VENT_DEADBAND_PA));
        view.show(vessel);
    }

    public void after() {
        if (vessel != null) view.show(vessel);
    }

    /** The species the feed layer is counted as, and (index 1) a species in it; null if the vessel holds no feed. */
    private int[] feed() {
        FluidNode v = vessel;
        if (v == null) return null;
        for (int l = 0; l < v.layers(); l++) {
            int main = -1;
            double most = 0, total = 0;
            for (int id = 2; id < v.speciesBound(); id++)
                if (v.layerOf(id) == l && v.liquid(id) > 0) {
                    total += v.liquid(id);
                    if (v.liquid(id) > most) {
                        main = id;
                        most = v.liquid(id);
                    }
                }
            if (total <= 1e-9) continue;
            int alias = NodeFluidView.aliasOf(v, main);
            if (alias >= 0 && takes(alias)) return new int[]{alias, main};
            if (takes(main)) return new int[]{main, main};
        }
        return null;
    }

    /** The feed in the vessel (mB of the fluid it counts as). */
    public double feedMb() {
        int[] f = feed();
        return f == null ? 0 : EaUnits.layerLiquid(vessel, f[1]) / FluidSpecies.kgPerMb(f[0], false);
    }

    /** Take up to mB of the feed out of the vessel, every species in its layer by its share; returns the mB taken. */
    public double take(double mB) {
        int[] f = feed();
        if (f == null || mB <= 0) return 0;
        double per = FluidSpecies.kgPerMb(f[0], false);
        return EaUnits.drainLayer(vessel, f[1], mB * per) / per;
    }

    /** What takeMix took: mB of the fluid the feed counts as, the kg of every species in it (by id), and its temperature. */
    public record Taken(double mB, double[] kg, double kelvin) {
    }

    /** As take, for a device that passes its feed on: it hands on the same mix, not the main species alone. */
    public Taken takeMix(double mB) {
        int[] f = feed();
        if (f == null || mB <= 0) return null;
        FluidNode v = vessel;
        double per = FluidSpecies.kgPerMb(f[0], false), T = v.T();
        double[] kg = new double[v.speciesBound()];
        for (int id = 2; id < kg.length; id++) kg[id] = v.mass(id);
        double got = EaUnits.drainLayer(v, f[1], mB * per);
        if (got <= 0) return null;
        for (int id = 2; id < kg.length; id++) kg[id] = Math.max(0, kg[id] - v.mass(id));
        return new Taken(got / per, kg, T);
    }

    /** With no feed in the vessel: the liquid that is in it instead and keeps the feed out (its name), or null. */
    public String blockedBy() {
        FluidNode v = vessel;
        if (v == null || feed() != null) return null;
        var reg = SpeciesRegistry.get();
        int main = -1;
        double most = 1e-3 * v.volume;   // (m^3: a film doesn't block it)
        for (int id = 0; id < v.speciesBound(); id++) {
            if (id == 1) continue;
            double m3 = (id == 0 ? v.mLiquid() : v.liquid(id)) / reg.byId(id).rhoL;
            if (m3 > most) {
                main = id;
                most = m3;
            }
        }
        return main < 0 ? null : reg.byId(main).displayName();
    }

    /** Its liquid (mB of liquid's volume), and the vessel's rated fill. */
    public double liquidMb() {
        return vessel == null ? 0 : vessel.liquidVolume() / NodeFluidView.M3_PER_MB;
    }

    public double ratedMb() {
        return Vessels.FILL_LEVEL * vesselM3 / NodeFluidView.M3_PER_MB;
    }

    /** Put mB of fluid straight into the vessel at ambientK, as a bucket would (tests). */
    public void put(Fluid fluid, int mB, double ambientK) {
        if (vessel == null) return;
        var ph = FluidSpecies.of(fluid);
        if (ph == null) return;
        EaUnits.add(vessel, ph.id(), mB * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), ambientK, ph.vapour());
        view.show(vessel);
    }

    public void write(CompoundTag nbt) {
        if (vessel != null) FluidSaves.put(nbt, key + "Vessel", vessel);
        if (stub != null) {
            FluidWorld.writeBackOf(stub);
            FluidSaves.put(nbt, key, stub);
        }
    }

    /** Read a save; an old one (no vessel) has its fluid tank of oldTankMb under oldTankKey, which goes into the vessel. */
    public void read(CompoundTag nbt, String oldTankKey, int oldTankMb) {
        loaded = FluidSaves.array(nbt, key + "Vessel");
        loadedStub = FluidSaves.array(nbt, key);
        if (loaded == null) {
            var old = new ElementFluidHandler(oldTankMb);
            old.readFromNBT(nbt, oldTankKey);
            legacyTank = old.getFluid().copy();
        }
    }
}
