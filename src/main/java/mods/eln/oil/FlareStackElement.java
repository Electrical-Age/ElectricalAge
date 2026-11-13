package mods.eln.oil;

import mods.eln.fluid.FluidSpecies;
import mods.eln.fluid.FuelRegistry;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlareStackDescriptor: a fluid node behind its front flange (EA pipes, half way up the block). Its tip burns all the gas
 * pushed out of it (steam and air just go up the stack), and its bottom liquid layer at up to BURN_RATE mB/s (water
 * boils away). Its tip is open: a
 * gas vent out (gas only, its float shut when the liquid nears the top) lets out air and any gas past what it burns, and
 * a vacuum breaker lets air in as it burns liquid down. Tells the client what burns, for the visuals.
 */
public class FlareStackElement extends TransparentNodeElement implements FluidDevice {
    /** Its vessel (mB of liquid's volume, as its old tank) and its height in the block (m); the flange half way up. */
    public static final double VESSEL_MB = FlareStackDescriptor.TANK_MB;
    static final double VESSEL_M3 = VESSEL_MB * EaUnits.KG_PER_MB_WATER / Water.RHO_L, VESSEL_H = 1.0, FLANGE_Y = 0.5;
    /** Its flange's conductance (as the oil devices' ports) and the tip's vents' (small: a vent, not an opening). */
    static final double PORT_K = 6e-11, TIP_K = 1e-11;
    /**
     * The pressure difference (Pa) over which each tip vent starts to open, fully open at twice it: the gas over the air's
     * pressure leaves by the tip between steps (burn), so the gas vent only relieves a step's surge.
     */
    static final double TIP_DEADBAND_PA = 300.0;
    /** The liquid share of the vessel at which the gas vent's float has shut (it closes over the 2 % under it). */
    static final double FLOAT_SHUT = 0.97;

    public FluidNode vessel;
    private FluidDevice.Vent tip, airIn;
    /** Its fluid capability (buckets, other mods' pipes): a view of the vessel. */
    private final mods.eln.fluid.NodeFluidView view = new mods.eln.fluid.NodeFluidView(new mods.eln.fluid.NodeFluidView.Spec().intake().liquidShare(0.95));

    /** A save's vessel, or an old save's tank and its flange's PortBridge stub, put into the vessel when it is made. */
    private double[] loaded, legacyStub;
    private net.neoforged.neoforge.fluids.FluidStack legacyTank;

    public double burnRate;   // mB/s, averaged over whole seconds
    double carry, meterBurnt, meterTime;
    /** What it burnt (kg) since it was made: gas, and liquid (tests weigh it against what went in). */
    public double gasBurntKg, liquidBurntKg;
    String burning = "";
    /** What goes up the stack without a flame (Baughn 2026-11-11: the flare shouldn't "burn" water): a plume, and no "Burning" line. */
    static final java.util.Set<String> PLUME = java.util.Set.of("steam", "ic2steam", "water");

    /** What it burns or boils off most of, as the client is told ("" for nothing), and whether that goes up as a plume. */
    public String burning() {
        return burning;
    }

    public boolean plume() {
        return PLUME.contains(burning);
    }

    public FlareStackElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 1, 0.5));
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        if (vessel == null) return List.of();
        int face = front.toEnumFacing().get3DDataValue();
        return List.of(new FluidPort(vessel, node.coordonate.pos().asLong(), face, FLANGE_Y / VESSEL_H, PORT_K, TankElement.sideSpot(face, 8)));
    }

    @Override
    public List<FluidDevice.Vent> fluidVents() {
        if (tip == null) return List.of();
        return List.of(tip, airIn);
    }

    /** Set the tip's two vents by the vessel's pressure against the air's, each opening over TIP_DEADBAND_PA. */
    private void setTip(FluidNode v) {
        double dp = v.P() - Water.P_ATM;
        double fl = Math.max(0, Math.min(1, (FLOAT_SHUT - v.liquidVolume() / v.volume) / 0.02));
        tip.open = fl * Math.max(0, Math.min(1, (dp - TIP_DEADBAND_PA) / TIP_DEADBAND_PA));
        airIn.open = Math.max(0, Math.min(1, (-dp - TIP_DEADBAND_PA) / TIP_DEADBAND_PA));
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (vessel == null) return;
        view.sync(vessel, mods.eln.sim.Ambient.kelvin(node.coordonate));
        burn(vessel, dt);
        setTip(vessel);
    }

    @Override
    public void afterFluidStep(double dt) {
        if (vessel != null) view.show(vessel);
    }

    /**
     * Burn out of the vessel, between steps: the gas over the air's pressure goes out of its tip, each species by its
     * share, and what burns burns (steam and air just go up); then up to BURN_RATE x dt mB of the bottom liquid layer
     * whole (drainLayer: every species in it by its share; water just boils away up the stack).
     */
    void burn(FluidNode v, double dt) {
        double budget = FlareStackDescriptor.BURN_RATE * dt + carry, start = budget;
        int most = -1;
        boolean mostVapour = false;
        double mostMb = 0;
        // (its tip: the gas over the air's pressure leaves by it every step, each species by its share; what burns, burns,
        // and steam and air just go up the stack)
        double gasMb = 0, f = 1 - Water.P_ATM / v.P();
        if (f > 0) {
            double T = v.T();
            var reg = SpeciesRegistry.get();
            for (int id = 0; id < v.speciesBound(); id++) {
                double kg = v.vapour(id) * f;
                if (kg <= 0) continue;
                if (id == SpeciesRegistry.AIR) {
                    v.mAir -= kg;
                    v.energy -= kg * Water.hAir(T);
                    continue;
                }
                double per = FluidSpecies.kgPerMb(id, true);
                if (id == SpeciesRegistry.WATER) {
                    v.mWater -= kg;
                    v.energy -= kg * v.hVapour(T);
                } else {
                    v.addMass(id, -kg);
                    v.energy -= kg * reg.byId(id).hVapour(T);
                    gasBurntKg += kg;
                }
                gasMb += kg / per;
                if (kg / per > mostMb) { most = id; mostVapour = true; mostMb = kg / per; }
            }
            v.update();
        }
        int id = bottomLayer(v);
        if (id >= 0 && budget > 0) {
            double per = FluidSpecies.kgPerMb(id, false);
            double kg = id == SpeciesRegistry.WATER ? v.mLiquid() : EaUnits.layerLiquid(v, id);
            double mb = Math.min(budget, kg / per);
            double got = id == SpeciesRegistry.WATER ? EaUnits.drainLiquid(v, mb * per) : EaUnits.drainLayer(v, id, mb * per);
            liquidBurntKg += got;
            mb = got / per;
            budget -= mb;
            if (mb > mostMb) { most = id; mostVapour = false; mostMb = mb; }
        }
        double burnt = start - budget + gasMb;
        // (a fraction of a mB carries to the next step only while it burns: an empty flare doesn't save up)
        carry = start - budget > 0 ? Math.min(1, budget) : 0;
        meterBurnt += burnt;
        meterTime += dt;
        if (meterTime >= 1) {
            burnRate = meterBurnt / meterTime;
            meterBurnt = 0;
            meterTime = 0;
        }
        String what = most < 0 || mostMb < 1e-3 ? "" : name(most, mostVapour);
        if (!what.equals(burning)) {
            burning = what;
            needPublish();
        }
    }

    /** The fluid name of species id in that phase (or its other phase's: gasoline vapour burns as gasoline). */
    static String name(int id, boolean vapour) {
        var f = FluidSpecies.fluid(id, vapour);
        if (f == null) f = FluidSpecies.fluid(id, !vapour);
        return FuelRegistry.INSTANCE.fluidName(f);
    }

    /** The bottom liquid layer's main species (-1: none; a layer under a milligram doesn't count). */
    static int bottomLayer(FluidNode v) {
        for (int l = 0; l < v.layers(); l++) {
            int best = -1;
            double most = 0, total = 0;
            for (int id = 0; id < v.speciesBound(); id++)
                if (v.layerOf(id) == l) {
                    total += v.liquid(id);
                    if (v.liquid(id) > most) {
                        best = id;
                        most = v.liquid(id);
                    }
                }
            if (total > 1e-6) return best;
        }
        return -1;
    }

    /** Put mB of fluid into its vessel at the ambient, as a bucket would (a mixture alias dead: crude as its cuts). */
    public void putFluid(net.minecraft.world.level.material.Fluid fluid, int mB) {
        if (vessel == null) return;
        var ph = FluidSpecies.of(fluid);
        if (ph == null) return;
        EaUnits.add(vessel, ph.id(), mB * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), mods.eln.sim.Ambient.kelvin(node.coordonate), ph.vapour());
        view.show(vessel);
    }

    /** Its liquid (mB of liquid's volume). */
    public double liquidMb() {
        return vessel == null ? 0 : vessel.liquidVolume() * Water.RHO_L / EaUnits.KG_PER_MB_WATER;
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (vessel != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(burning);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        makeVessel(mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    /** The vessel, from its save, or an old save's tank and stub, or air at the ambient. */
    private void makeVessel(double amb) {
        FluidNode v = new FluidNode(VESSEL_M3, node.coordonate.y, VESSEL_H, false);
        if (loaded != null) {
            mods.eln.fluid.FluidSaves.restore(v, loaded);
            v.update();
        } else {
            v.fill(0, Water.P_ATM, amb);
            if (legacyTank != null && !legacyTank.isEmpty()) {
                var ph = FluidSpecies.of(legacyTank.getFluid());
                if (ph != null) EaUnits.add(v, ph.id(), legacyTank.getAmount() * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), amb, ph.vapour());
            }
            if (legacyStub != null) {
                FluidNode stub = new FluidNode(mods.eln.fluid.PortBridge.STUB_VOLUME, 0, 0.1, false);
                mods.eln.fluid.FluidSaves.restore(stub, legacyStub);
                stub.update();
                if (stub.mWater > 0) EaUnits.add(v, SpeciesRegistry.WATER, stub.mWater, amb, false);
                var reg = SpeciesRegistry.get();
                for (int j = 2; j < stub.speciesBound(); j++) {
                    double m = stub.liquid(j) + stub.vapour(j);
                    if (m > 0) EaUnits.add(v, j, m, amb, reg.byId(j).gas());
                }
            }
        }
        loaded = null;
        legacyTank = null;
        legacyStub = null;
        vessel = v;
        tip = new FluidDevice.Vent(v, 1.0, TIP_K);
        tip.gasOnly = true;
        airIn = new FluidDevice.Vent(v, 1.0, TIP_K);
        airIn.inward = true;
        setTip(v);
        view.show(v);
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return view;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (vessel != null) {
            mods.eln.fluid.FluidWorld.writeBackOf(vessel);
            mods.eln.fluid.FluidSaves.put(nbt, "vessel", vessel);
        }
        nbt.putDouble("carry", carry);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loaded = mods.eln.fluid.FluidSaves.array(nbt, "vessel");
        if (loaded == null) {
            // (an old save: its 4-bucket fluid tank and its flange's PortBridge stub go into the vessel when it is made)
            var old = new mods.eln.fluid.ElementFluidHandler(FlareStackDescriptor.TANK_MB);
            old.readFromNBT(nbt, "tank");
            legacyTank = old.getFluid().copy();
            legacyStub = mods.eln.fluid.FluidSaves.array(nbt, "inlet");
        }
        carry = nbt.getDoubleOr("carry", 0.0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        if (vessel != null) {
            info.put(I18N.tr("Contents"), EaUnits.composition(vessel));
            info.put(I18N.tr("Tank"), String.format("%.0f / %.0f mB of liquid", liquidMb(), VESSEL_MB));
        }
        if (plume()) info.put(I18N.tr("Boiling off"), String.format(I18N.tr("%.0f mB/s of water, no flame"), burnRate));
        else info.put(I18N.tr("Burning"), String.format("%.0f mB/s", burnRate));
        return info;
    }
}
