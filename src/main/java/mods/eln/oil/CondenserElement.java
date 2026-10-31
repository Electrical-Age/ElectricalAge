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
public class CondenserElement extends TransparentNodeElement implements FluidDevice, mods.eln.fluid.FluidPlacement.Turnable {
    final CondenserDescriptor desc;
    public final NbtThermalLoad shell = new NbtThermalLoad("shell");
    /** The vessel, its pump, ejector and makeup (shared with the plant condenser). */
    public final CondenserVessel v;
    public FluidNode vessel, outlet, inlet;
    public static final double MAKEUP_OPEN = CondenserVessel.MAKEUP_OPEN, MAKEUP_SHUT = CondenserVessel.MAKEUP_SHUT;
    private List<FluidPort> ports = List.of();
    public static final double OUTLET_VOLUME = CondenserVessel.OUTLET_VOLUME, PORT_K = CondenserVessel.PORT_K;
    /** The turbine exhausting into it, and the side it is on (3D value; -1: none). */
    public SteamGeneratorElement turbine;
    public int turbineSide = -1;
    /** Heat the shell took from the vessel (W, averaged) and condensate pumped (mB/s), for WAILA. */
    public double heatW, pumped;

    public CondenserElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (CondenserDescriptor) d;
        v = new CondenserVessel(shell, desc.volume, desc.nominalHeat, desc.pumpKgPerS);
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
        return v.floodShare();
    }

    /** The turbine's exhaust: m kg of steam (and air kg of air) with its enthalpy per kg after the work (J/kg). */
    public void exhaust(double m, double air, double hSteam, double tAir) {
        v.exhaust(m, air, hSteam, tAir);
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
        return v.pump == null ? List.of() : List.of(v.pump, v.makeup);
    }

    /** The condensate pump turns with the turbine's shaft; the ejector always works (its steam jet). */
    @Override
    public void beforeFluidStep(double dt) {
        v.before(dt, 1, 1, turbine == null ? 0 : turbine.omega);
    }

    @Override
    public void afterFluidStep(double dt) {
        v.after(dt);
        heatW = v.heatW;
        pumped = v.pumped;
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
        var c = node.coordonate;
        v.initialize(c.y, mods.eln.sim.Ambient.kelvin(c));
        vessel = v.vessel;
        outlet = v.outlet;
        inlet = v.inlet;
        remakePorts();
        connect();
    }

    @Override
    public Direction[] fronts() {
        return mods.eln.fluid.FluidPlacement.horizontal();
    }

    @Override
    public void remakePorts() {
        long at = node.coordonate.pos().asLong();
        ports = List.of(new FluidPort(outlet, at, front.toEnumFacing().get3DDataValue(), 0.0, PORT_K),   // condensate out, front
            new FluidPort(inlet, at, 1, 1.0, PORT_K));                                                      // makeup in, top (float valve)
    }

    /** The wrench turns it (AdventurAgent 2026-10-31: placed facing the player, nothing could turn it to a turbine). */
    @Override
    public boolean wrenchTurn(net.minecraft.world.entity.player.Player player) {
        if (!mods.eln.fluid.FluidPlacement.turn(this)) return false;
        findTurbine();
        return true;
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
        v.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        v.read(nbt, !nbt.contains("tank2"));
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Turbine"), turbine == null ? I18N.tr("none: place it against a turbine's exhaust side") : I18N.tr("connected"));
        if (vessel != null) {
            info.put(I18N.tr("Pressure"), String.format("%.2f bar of %s", vessel.P() / 1e5, EaUnits.contents(vessel)));
            info.put(I18N.tr("Temperature"), String.format("%.0f °C", vessel.T() - Water.T0));
            info.put(I18N.tr("Fill"), String.format("%.0f%% (%.0f mB)", floodShare() * 100, vessel.mLiquid() / EaUnits.KG_PER_MB_WATER));
        }
        info.put(I18N.tr("Heat rejected"), Utils.plotPower("", heatW));
        var makeup = v.makeup;
        if (makeup != null) info.put(I18N.tr("Makeup valve"), makeup.open >= 0.99 ? I18N.tr("open") : makeup.open <= 0.01 ? I18N.tr("shut (full enough)")
            : String.format("%.0f%% open", makeup.open * 100));
        // (Baughn: on a cold start nothing said why no water reached the boiler: the pump turns with the turbine)
        double open = v.pump == null ? 0 : v.pump.open;
        info.put(I18N.tr("Condensate pump"), turbine == null ? I18N.tr("stopped: no turbine")
            : open <= 0.01 ? I18N.tr("stopped: it turns with the turbine; feed the boiler directly to start")
            : String.format("%.0f%%, %.0f mB/s", Math.min(1, open) * 100, pumped));
        return info;
    }
}
