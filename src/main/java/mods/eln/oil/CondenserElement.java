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
 * the turbine turns) pushes water from the vessel's bottom to the front flange. Makeup water by the flange on top,
 * through a float valve: open below MAKEUP_OPEN of its volume as water, shut by MAKEUP_SHUT (Baughn's makeup pump
 * filled one to the flood line).
 */
public class CondenserElement extends TransparentNodeElement implements FluidDevice {
    final CondenserDescriptor desc;
    public final NbtThermalLoad shell = new NbtThermalLoad("shell");
    public FluidNode vessel, outlet, inlet;
    private FluidDevice.Link pump, makeup;
    /** The float valve: open below this share of the vessel as water, shut at the other. */
    public static final double MAKEUP_OPEN = 0.15, MAKEUP_SHUT = 0.25;
    private List<FluidPort> ports = List.of();
    private double[] loaded, loadedOut, loadedIn;
    /** Saved before the tanks shrank (no "tank2" tag): its water is trimmed once on load. */
    private boolean oldTank;
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
        return pump == null ? List.of() : List.of(pump, makeup);
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (vessel == null) return;
        // the shell takes the vessel's heat (EA thermal temperatures are on the 20 C scale)
        double shellK = shell.Tc + mods.eln.sim.PhysicalConstant.Tref;
        double q = (vessel.T() - shellK) / (CondenserDescriptor.DT_VESSEL / desc.nominalHeat);
        // (at most half the way to the shell's temperature in a step: a vessel of thin air has next to no heat capacity,
        // and the explicit step overshot to -193 C: AdventurAgent)
        double toShell = (vessel.energy - mods.eln.sim.fluid.Flash.energyAt(vessel.mWater, vessel.mAir, vessel.volume, shellK)) / dt;
        q = toShell >= 0 ? Math.max(0, Math.min(q, 0.5 * toShell)) : Math.min(0, Math.max(q, 0.5 * toShell));
        vessel.heat -= q;
        shell.moveEnergyTo(q * dt);
        meterHeat += q * dt;
        // the air ejector, down to EJECTOR_FLOOR_PA of air (an empty vessel with nothing in it has no temperature:
        // AdventurAgent saw -193 C)
        double tNow = vessel.T(), floor = CondenserDescriptor.EJECTOR_FLOOR_PA * vessel.volume / (Water.R_AIR * Math.max(250, tNow));
        double air = Math.max(0, vessel.mAir - floor) * (1 - Math.exp(-dt / CondenserDescriptor.EJECTOR_TAU));
        if (air > 0) {
            double T = vessel.T();
            vessel.mAir -= air;
            vessel.energy -= air * Water.uAir(T);
            vessel.update();
        }
        // the makeup's float valve
        if (makeup != null) makeup.open = Math.max(0, Math.min(1, (MAKEUP_SHUT - floodShare()) / (MAKEUP_SHUT - MAKEUP_OPEN)));
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
        inlet = new FluidNode(OUTLET_VOLUME, c.y, 0.25, false);
        restore(inlet, loadedIn, OUTLET_VOLUME);
        inlet.lumpable = true;
        inlet.resistance = 1 / PORT_K;
        // (into the vessel's top: the makeup falls in, it doesn't run back out)
        makeup = new FluidDevice.Link(inlet, 0, vessel, 1.0, PORT_K).kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
        // g = rho k / mu: its condensate flow against 1 atm, none at the shutoff
        double dp = CondenserDescriptor.PUMP_PA - Water.P_ATM;
        pump = new FluidDevice.Link(vessel, 0, outlet, 0, desc.pumpKgPerS / dp * Water.MU_L / Water.RHO_L).kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
        pump.boost = dp;
        pump.open = 0;
        long at = c.pos().asLong();
        ports = List.of(new FluidPort(outlet, at, front.toEnumFacing().get3DDataValue(), 0.0, PORT_K),   // condensate out, front
            new FluidPort(inlet, at, 1, 1.0, PORT_K));                                                      // makeup in, top (float valve)
        connect();
    }

    private void restore(FluidNode n, double[] saved, double volume) {
        if (saved != null) {
            // (a vessel saved before Baughn's smaller tanks, fuller than the float valve's level of its new one: the excess
            // water goes, with its share of the energy; else it was over FLOOD at once, and the turbine took water damage)
            double max = n == vessel && oldTank ? MAKEUP_SHUT * volume * Water.RHO_L : Double.MAX_VALUE, keep = saved[0] > max ? max / saved[0] : 1;
            n.mWater = saved[0] * keep;
            n.mAir = saved[1];
            n.energy = saved[2] * keep;
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
        nbt.putBoolean("tank2", true);
        if (vessel != null) {
            nbt.putDouble("vesselW", vessel.mWater);
            nbt.putDouble("vesselA", vessel.mAir);
            nbt.putDouble("vesselU", vessel.energy);
            nbt.putDouble("outletW", outlet.mWater);
            nbt.putDouble("outletA", outlet.mAir);
            nbt.putDouble("outletU", outlet.energy);
            nbt.putDouble("inletW", inlet.mWater);
            nbt.putDouble("inletA", inlet.mAir);
            nbt.putDouble("inletU", inlet.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        oldTank = !nbt.contains("tank2");
        if (nbt.contains("vesselW"))
            loaded = new double[]{nbt.getDoubleOr("vesselW", 0), nbt.getDoubleOr("vesselA", 0), nbt.getDoubleOr("vesselU", 0)};
        if (nbt.contains("outletW"))
            loadedOut = new double[]{nbt.getDoubleOr("outletW", 0), nbt.getDoubleOr("outletA", 0), nbt.getDoubleOr("outletU", 0)};
        if (nbt.contains("inletW"))
            loadedIn = new double[]{nbt.getDoubleOr("inletW", 0), nbt.getDoubleOr("inletA", 0), nbt.getDoubleOr("inletU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Turbine"), turbine == null ? I18N.tr("none: place it against a turbine's exhaust side") : I18N.tr("connected"));
        if (vessel != null) {
            info.put(I18N.tr("Pressure"), String.format("%.2f bar", vessel.P() / 1e5));
            info.put(I18N.tr("Temperature"), String.format("%.0f °C", vessel.T() - Water.T0));
            info.put(I18N.tr("Fill"), String.format("%.0f%% (%.0f mB)", floodShare() * 100, vessel.mLiquid() / EaUnits.KG_PER_MB_WATER));
        }
        info.put(I18N.tr("Heat rejected"), Utils.plotPower("", heatW));
        if (makeup != null) info.put(I18N.tr("Makeup valve"), makeup.open >= 0.99 ? I18N.tr("open") : makeup.open <= 0.01 ? I18N.tr("shut (full enough)")
            : String.format("%.0f%% open", makeup.open * 100));
        // (Baughn: on a cold start nothing said why no water reached the boiler: the pump turns with the turbine)
        double open = pump == null ? 0 : pump.open;
        info.put(I18N.tr("Condensate pump"), turbine == null ? I18N.tr("stopped: no turbine")
            : open <= 0.01 ? I18N.tr("stopped: it turns with the turbine; feed the boiler directly to start")
            : String.format("%.0f%%, %.0f mB/s", Math.min(1, open) * 100, pumped));
        return info;
    }
}
