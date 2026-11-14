package mods.eln.fluid;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * A producing device's store and outlet on the fluid core (Baughn 2026-11-10: a PortBridge's push becomes the device's
 * pump): a vessel node that holds what the device made, a small outlet node behind its flange (it merges into the pipe
 * that meets it), and a centrifugal pump in the core's solve from the vessel's floor to the outlet, with the old push's
 * rate as its free flow and its head as the shut-off pressure (over 1 atm in the vessel).
 * <p>
 * The device hands over what it made (made()); the vessel takes it before the next fluid step. Its gas: a sealed vessel
 * (the oil pump's wellhead) sends it down the line through a check valve from its top (the casing gas joins the flow
 * line), so a live well's natural gas reaches the pipe as before; an open one (the separator's basin) breathes through a
 * float vent and a vacuum breaker, as the furnace's tank. Unpiped, the pump stands and the vessel's liquid goes into the
 * block on the flange's face through its view (FluidExport), as the old push did.
 */
public final class DeviceOutlet {
    public static final double PORT_K = PortBridge.PORT_K, OUTLET_VOLUME = PortBridge.STUB_VOLUME, VENT_K = 1e-11;
    /** The vent's dead band (Pa) and its float's shut level (liquid share), as the furnace's breather. */
    static final double VENT_DEADBAND_PA = 300.0, FLOAT_SHUT = 0.97;

    final String outletKey;
    private String vesselKey = "vessel", oldTankKey = "tank";
    final double vesselM3, headPa, mbPerS;
    double freeKgPerS;
    final boolean open;
    /** The one fluid it makes (only()), or null: any, the pump rated on crude. */
    private Fluid only;
    private final NodeFluidView.Spec spec = new NodeFluidView.Spec().intake().liquidShare(0.95).aliases();
    public FluidNode vessel, outlet;
    FluidDevice.Link pump, gasLine;
    FluidDevice.Vent tip, airIn;
    /** Its fluid capability: a view of the vessel that names a layer still in crude's proportions crude (Spec.aliases). */
    public final NodeFluidView view;

    /** What the device made since the last step, by species id (mB), and whether crude is live (with its gas). */
    private final java.util.Map<Integer, Double> made = new java.util.LinkedHashMap<>();
    private final boolean live;

    private double[] loaded, loadedOut;
    private net.neoforged.neoforge.fluids.FluidStack legacyTank;

    /** mB taken out of the vessel into the block on the flange (unpiped), for tests. */
    public long exported;

    /**
     * outletKey: the old PortBridge's save key (its stub's contents load as the outlet's); vesselMb: the old tank's size;
     * mbPerS and headPa: the old push's rate (mB/s of crude) and the pressure it pushed against; open: a basin that
     * breathes, else sealed with a gas line; live: crude made is live.
     */
    public DeviceOutlet(String outletKey, double vesselMb, double mbPerS, double headPa, boolean open, boolean live) {
        this.outletKey = outletKey;
        this.vesselM3 = vesselMb * EaUnits.KG_PER_MB_WATER / Water.RHO_L;
        this.headPa = headPa;
        this.mbPerS = mbPerS;
        int crude = SpeciesRegistry.get().id("eln:crude_oil");
        this.freeKgPerS = mbPerS * (crude >= 0 ? FluidSpecies.kgPerMb(crude, false) : EaUnits.KG_PER_MB_WATER);
        this.open = open;
        this.live = live;
        this.view = new NodeFluidView(spec);
    }

    /**
     * It makes this one fluid (a machine's product, where the pump and separator make crude): the pump's free flow is
     * mbPerS of it by its own weight, and the view takes no other liquid, as the old tank's filter.
     */
    public DeviceOutlet only(Fluid fluid) {
        only = fluid;
        spec.liquids(id -> {
            var ph = FluidSpecies.of(only);
            return ph != null && ph.id() == id;
        });
        return this;
    }

    /** Its save keys, for a device with more than one vessel: the vessel's, and the old fluid tank's (read()). */
    public DeviceOutlet keys(String vesselKey, String oldTankKey) {
        this.vesselKey = vesselKey;
        this.oldTankKey = oldTankKey;
        return this;
    }

    /** Make the nodes: the vessel at y (its floor, m; one block tall), the outlet at its flange's height. */
    public void make(double y, double flangeY, double ambientK) {
        var product = only == null ? null : FluidSpecies.of(only);
        if (product != null) freeKgPerS = mbPerS * FluidSpecies.kgPerMb(product.id(), false);
        FluidNode v = new FluidNode(vesselM3, y, 1.0, false);
        if (loaded != null) {
            FluidSaves.restore(v, loaded);
            v.update();
        } else {
            v.fill(0, Water.P_ATM, ambientK);
            if (legacyTank != null && !legacyTank.isEmpty()) {
                var ph = FluidSpecies.of(legacyTank.getFluid());
                if (ph != null) EaUnits.add(v, ph.id(), legacyTank.getAmount() * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), ambientK, ph.vapour());
            }
        }
        FluidNode o = new FluidNode(OUTLET_VOLUME, flangeY, 0.1, false);
        if (loadedOut != null) {
            FluidSaves.restore(o, loadedOut);
            o.update();
        } else o.fill(0, Water.P_ATM, ambientK);
        o.lumpable = true;   // (it merges into the pipe on its flange)
        o.resistance = 1 / PORT_K;
        loaded = loadedOut = null;
        legacyTank = null;
        vessel = v;
        outlet = o;
        // (a centrifugal pump in the core's solve: the old push's rate free, none at its head over the vessel's 1 atm)
        pump = new FluidDevice.Link(v, 0, o, 0.5, PORT_K).pump(freeKgPerS, Math.max(0, headPa - Water.P_ATM));
        if (open) {
            tip = new FluidDevice.Vent(v, 1.0, VENT_K);
            tip.gasOnly = true;
            airIn = new FluidDevice.Vent(v, 1.0, VENT_K);
            airIn.inward = true;
        } else {
            // (the casing gas: a check valve from the vessel's top into the flow line; unpiped, a vent to the air lets it
            // off, as the liquid goes into the block there without it)
            gasLine = new FluidDevice.Link(v, 1.0, o, 0.5, PORT_K).kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
            tip = new FluidDevice.Vent(v, 1.0, VENT_K);
            tip.gasOnly = true;
        }
        view.show(v);
    }

    public FluidDevice.FluidPort port(long pos, int face, int at) {
        return new FluidDevice.FluidPort(outlet, pos, face, 0.5, PORT_K, at);
    }

    public List<FluidDevice.Link> links() {
        if (pump == null) return List.of();
        return gasLine == null ? List.of(pump) : List.of(pump, gasLine);
    }

    public List<FluidDevice.Vent> vents() {
        if (tip == null) return List.of();
        return airIn == null ? List.of(tip) : List.of(tip, airIn);
    }

    /** True if a pipe meets the flange: the outlet is in a run, or has an edge besides its own links. */
    public boolean piped() {
        if (outlet == null) return false;
        if (FluidWorld.runSize(outlet) > 1) return true;
        for (var e : FluidWorld.edgesOf(outlet))
            if ((pump == null || e != pump.edge) && (gasLine == null || e != gasLine.edge)) return true;
        return false;
    }

    /** The device made mB of fluid (taken into the vessel before the next step: a live well's crude with its gas). */
    public void made(Fluid fluid, double mB) {
        var ph = FluidSpecies.of(fluid);
        if (ph == null || mB <= 0) return;
        made.merge(ph.id(), mB, Double::sum);
    }

    /** Before a fluid step: what was put in or taken out through the view, what was made, then the pump and vents. */
    public void before(double ambientK) {
        if (vessel == null) return;
        view.sync(vessel, ambientK);
        for (var e : made.entrySet()) {
            int id = e.getKey();
            EaUnits.add(vessel, id, e.getValue() * FluidSpecies.kgPerMb(id, false), ambientK, false, live);
        }
        made.clear();
        boolean piped = piped();
        // (unpiped, the flange is shut: the pump stands and the view gives the liquid to the block there instead)
        pump.speed = piped ? 1 : 0;
        if (gasLine != null) gasLine.open = piped ? 1 : 0;
        if (tip != null) {
            double dp = vessel.P() - Water.P_ATM;
            double fl = Math.max(0, Math.min(1, (FLOAT_SHUT - vessel.liquidVolume() / vessel.volume) / 0.02));
            tip.open = (open || !piped ? 1 : 0) * fl * Math.max(0, Math.min(1, (dp - VENT_DEADBAND_PA) / VENT_DEADBAND_PA));
            if (airIn != null) airIn.open = Math.max(0, Math.min(1, (-dp - VENT_DEADBAND_PA) / VENT_DEADBAND_PA));
        }
        view.show(vessel);
    }

    public void after() {
        if (vessel != null) view.show(vessel);
    }

    /** Unpiped: push up to max mB of the vessel's liquid into the block on face (FluidExport). */
    public int export(ServerLevel level, BlockPos pos, Direction face, int max) {
        if (vessel == null || piped()) return 0;
        int n = FluidExport.push(view, level, pos, face, max);
        exported += n;
        return n;
    }

    /** Its liquid (mB of liquid's volume), and how much more it holds before it counts as full. */
    public double liquidMb() {
        return vessel == null ? 0 : vessel.liquidVolume() * Water.RHO_L / EaUnits.KG_PER_MB_WATER;
    }

    public double capacityMb() {
        return 0.95 * vesselM3 * Water.RHO_L / EaUnits.KG_PER_MB_WATER;
    }

    /** Room for mB more (with what's waiting to go in), under the head's pressure in the vessel and at the flange. */
    public boolean room(double mB) {
        if (vessel == null) return false;
        double waiting = 0;
        for (double d : made.values()) waiting += d;
        if (liquidMb() + waiting + mB > capacityMb()) return false;
        return vessel.P() < headPa && (outlet == null || FluidWorld.nodeFor(outlet).P() < headPa);
    }

    /** Put mB of fluid straight into the vessel at ambientK (tests; crude live if this outlet's is). */
    public void put(Fluid fluid, int mB, double ambientK) {
        put(fluid, mB, ambientK, live);
    }

    public void put(Fluid fluid, int mB, double ambientK, boolean live) {
        if (vessel == null) return;
        var ph = FluidSpecies.of(fluid);
        if (ph == null) return;
        EaUnits.add(vessel, ph.id(), mB * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), ambientK, ph.vapour(), live);
        view.show(vessel);
    }

    public void write(CompoundTag nbt) {
        if (vessel != null) FluidSaves.put(nbt, vesselKey, vessel);
        if (outlet != null) {
            FluidWorld.writeBackOf(outlet);
            FluidSaves.put(nbt, outletKey, outlet);
        }
    }

    /** Read a save; an old one (no vessel) has its fluid tank under "tank" (keys()), which goes into the vessel. */
    public void read(CompoundTag nbt, int oldTankMb) {
        loaded = FluidSaves.array(nbt, vesselKey);
        loadedOut = FluidSaves.array(nbt, outletKey);
        if (loaded == null) {
            var old = new ElementFluidHandler(oldTankMb);
            old.readFromNBT(nbt, oldTankKey);
            legacyTank = old.getFluid().copy();
        }
    }
}
