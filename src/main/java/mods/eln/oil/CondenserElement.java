package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.nbt.NbtThermalLoad;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The condenser (CondenserDescriptor): a vessel the turbine beside it exhausts into (SteamGeneratorElement.condenser),
 * its shell an EA thermal load cooled to the ambient through its own air cooler and through thermal cables on its sides.
 * Each fluid step: the shell takes the vessel's heat, the air ejector pulls air out, the condensate pump (open as far as
 * the turbine turns) pushes water from the vessel's bottom to the front flange. Makeup water by the flange on top.
 */
public class CondenserElement extends TransparentNodeElement implements FluidDevice {
    final CondenserDescriptor desc;
    public final NbtThermalLoad shell = new NbtThermalLoad("shell");
    public FluidNode vessel, outlet;
    private FluidDevice.Link pump;
    private List<FluidPort> ports = List.of();
    private double[] loaded, loadedOut;
    public static final double OUTLET_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;
    /** The turbine exhausting into it, and the side it is on (3D value; -1: none). */
    public SteamGeneratorElement turbine;
    public int turbineSide = -1;
    /** Heat the shell took from the vessel (W, averaged) and condensate pumped (mB/s), for WAILA. */
    public double heatW, pumped;
    double meterHeat, meterPump, meterTime;

    public CondenserElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (CondenserDescriptor) d;
        thermalLoadList.add(shell);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(t -> findTurbine());
    }

    /** The turbine whose exhaust side faces it, if any (published when it changes: the models join). */
    void findTurbine() {
        SteamGeneratorElement found = null;
        int side = -1;
        for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
            if (node.getNeighbor(d) instanceof TransparentNode t && t.element instanceof SteamGeneratorElement s && s.front.right() == d.getInverse()) {
                found = s;
                side = d.getInt();
            }
        }
        turbine = found;
        if (side != turbineSide) {
            turbineSide = side;
            needPublish();
        }
    }

    /** Liquid as a share of the vessel. */
    public double floodShare() {
        return vessel == null ? 0 : vessel.mLiquid() / Water.RHO_L / vessel.volume;
    }

    /** The turbine's exhaust: m kg of steam (and air kg of air) with its enthalpy per kg after the work (J/kg). */
    public void exhaust(double m, double air, double hSteam, double tAir) {
        if (vessel == null || m <= 0 && air <= 0) return;
        vessel.mWater += m;
        vessel.mAir += air;
        vessel.energy += m * hSteam + air * Water.hAir(tAir);
        vessel.update();
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    @Override
    public List<FluidDevice.Link> fluidLinks() {
        return pump == null ? List.of() : List.of(pump);
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (vessel == null) return;
        // the shell takes the vessel's heat (EA thermal temperatures are on the 20 C scale)
        double shellK = shell.Tc + mods.eln.sim.PhysicalConstant.Tref;
        double q = (vessel.T() - shellK) / (CondenserDescriptor.DT_VESSEL / desc.nominalHeat);
        vessel.heat -= q;
        shell.moveEnergyTo(q * dt);
        meterHeat += q * dt;
        // the air ejector
        double air = vessel.mAir * (1 - Math.exp(-dt / CondenserDescriptor.EJECTOR_TAU));
        if (air > 0) {
            double T = vessel.T();
            vessel.mAir -= air;
            vessel.energy -= air * Water.uAir(T);
            vessel.update();
        }
        // the condensate pump, on the turbine's shaft
        if (pump != null) {
            pump.open = turbine == null ? 0 : Math.max(0, Math.min(1.2, turbine.omega));
            pump.boost = CondenserDescriptor.PUMP_PA - Water.P_ATM;
        }
    }

    @Override
    public void afterFluidStep(double dt) {
        if (pump != null && pump.edge != null) meterPump += Math.max(0, pump.edge.fLiquid) / EaUnits.KG_PER_MB_WATER * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            heatW = meterHeat / meterTime;
            pumped = meterPump / meterTime;
            meterHeat = meterPump = meterTime = 0;
        }
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (vessel != null) FluidWorld.add(this);
        findTurbine();
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    /** Thermal cables on its sides (not its front, the condensate flange; not the turbine's side). */
    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return side == front || side.getInt() == turbineSide ? null : shell;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP || side == front) return 0;
        return NodeBase.maskThermal;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T:", shell.Tc);
    }

    @Override
    public void initialize() {
        // the shell: its air cooler takes the nominal heat DT_SHELL over the ambient
        shell.set(0.01, CondenserDescriptor.DT_SHELL / desc.nominalHeat, desc.nominalHeat * 10 / CondenserDescriptor.DT_SHELL);
        var c = node.coordonate;
        vessel = new FluidNode(desc.volume, c.y, 1.0, false);
        restore(vessel, loaded, desc.volume);
        outlet = new FluidNode(OUTLET_VOLUME, c.y, 0.25, false);
        restore(outlet, loadedOut, OUTLET_VOLUME);
        outlet.lumpable = true;
        outlet.resistance = 1 / PORT_K;
        // g = rho k / mu: its condensate flow against 1 atm, none at the shutoff
        double dp = CondenserDescriptor.PUMP_PA - Water.P_ATM;
        pump = new FluidDevice.Link(vessel, 0, outlet, 0, desc.pumpKgPerS / dp * Water.MU_L / Water.RHO_L).kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
        pump.boost = dp;
        pump.open = 0;
        long at = c.pos().asLong();
        ports = List.of(new FluidPort(outlet, at, front.toEnumFacing().get3DDataValue(), 0.0, PORT_K),   // condensate out, front
            new FluidPort(vessel, at, 1, 1.0, PORT_K));                                                     // makeup in, top
        connect();
    }

    private void restore(FluidNode n, double[] saved, double volume) {
        if (saved != null) {
            n.mWater = saved[0];
            n.mAir = saved[1];
            n.energy = saved[2];
            n.update();
        } else n.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(turbineSide);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (vessel != null) {
            nbt.putDouble("vesselW", vessel.mWater);
            nbt.putDouble("vesselA", vessel.mAir);
            nbt.putDouble("vesselU", vessel.energy);
            nbt.putDouble("outletW", outlet.mWater);
            nbt.putDouble("outletA", outlet.mAir);
            nbt.putDouble("outletU", outlet.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        if (nbt.contains("vesselW"))
            loaded = new double[]{nbt.getDoubleOr("vesselW", 0), nbt.getDoubleOr("vesselA", 0), nbt.getDoubleOr("vesselU", 0)};
        if (nbt.contains("outletW"))
            loadedOut = new double[]{nbt.getDoubleOr("outletW", 0), nbt.getDoubleOr("outletA", 0), nbt.getDoubleOr("outletU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Turbine"), turbine == null ? I18N.tr("none: place it against a turbine's exhaust side") : I18N.tr("connected"));
        if (vessel != null) {
            info.put(I18N.tr("Pressure"), String.format("%.2f bar", vessel.P() / 1e5));
            info.put(I18N.tr("Temperature"), String.format("%.0f °C", vessel.T() - Water.T0));
            info.put(I18N.tr("Condensate"), String.format("%.0f mB (%.0f%% full)", vessel.mLiquid() / EaUnits.KG_PER_MB_WATER, floodShare() * 100));
        }
        info.put(I18N.tr("Heat rejected"), Utils.plotPower("", heatW));
        info.put(I18N.tr("Condensate pumped"), String.format("%.0f mB/s", pumped));
        return info;
    }
}
