package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.ReservoirData;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The steam injection wellhead (SteamInjectionDescriptor): an inlet node behind a port on its top. Each fluid step,
 * while the lake under its well is an oil lake in its seepage tail with room for more credit, it takes steam (vapour
 * only: condensate stays in its line) from its inlet at INJECT_KG_PER_S_PA over the pressure the well needs, up to
 * MAX_KG_S: the atmosphere plus the column of crude in the casing. Otherwise it takes nothing, and the pressure in its
 * line builds up.
 */
public class SteamInjectionElement extends TransparentNodeElement implements FluidDevice {
    public static final double INLET_VOLUME = 0.0005, PORT_K = BoilerElement.PORT_K;
    /** Steam it takes per Pa over what the well needs (kg/s/Pa: 10 g/s at 1 bar over), and the most (kg/s). */
    public static final double INJECT_KG_PER_S_PA = 1e-7, MAX_KG_S = 0.02;

    public FluidNode inlet;
    private double[] loaded;
    private List<FluidPort> ports = List.of();
    /** The lake under its well (0: none), its depth (blocks); steam injected now (kg/s), totals since loaded (kg). */
    int source, depth;
    double kgPerS, totalKg;
    String lake = "no well casing down to an oil lake", why = "";
    int look;

    public SteamInjectionElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    /** The pressure (Pa) steam needs to go down a well this deep: the atmosphere and the column of crude in the casing. */
    public static double neededPa(int depth) {
        return Water.P_ATM + OilPumpDescriptor.CRUDE_KG_PER_L * 1000 * 9.81 * depth;
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (inlet == null || !(node.coordonate.world() instanceof ServerLevel level)) return;
        if (look-- <= 0) {
            look = 40;
            int[] w = OilPumpElement.well(level, node.coordonate.pos());
            source = w[0];
            depth = w[1];
        }
        Reservoir r = source > 0 ? ReservoirData.get(level).byId(source) : null;
        kgPerS = 0;
        if (!OilRecovery.oil(r)) {
            lake = "no well casing down to an oil lake";
            why = "";
            return;
        }
        OilRecovery rec = OilRecovery.get(level);
        lake = OilRecovery.describe(level, r);
        double over = inlet.P() - neededPa(depth);
        if (rec.regime(r) == OilRecovery.Regime.PRIMARY) why = "the lake still flows: no steam needed";
        else if (rec.boost(r) >= OilRecovery.BOOST_CAP_MB - 1) why = "the lake has all the steam it can take";
        else if (over <= 0) why = String.format("needs %.1f bar to push down %d blocks", neededPa(depth) / 1e5, depth);
        else {
            double kg = Math.min(Math.min(MAX_KG_S, INJECT_KG_PER_S_PA * over) * dt, EaUnits.steamAvailable(inlet));
            kg = rec.inject(r, kg);
            if (kg > 0) EaUnits.drainVapour(inlet, kg);
            kgPerS = kg / dt;
            totalKg += kg;
            why = "";
        }
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
    public void connectJob() {
        super.connectJob();
        if (inlet != null) {
            ports = List.of(new FluidPort(inlet, node.coordonate.pos().asLong(), 1, 1.0, PORT_K));   // steam in: its top
            FluidWorld.add(this);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public void initialize() {
        inlet = new FluidNode(INLET_VOLUME, node.coordonate.y, 1.0, false);
        if (loaded != null) {
            inlet.mWater = loaded[0];
            inlet.mAir = loaded[1];
            inlet.energy = loaded[2];
            inlet.update();
        } else inlet.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
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
        return "";
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (inlet != null) CondenserVessel.put(nbt, "inlet", inlet);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loaded = CondenserVessel.get(nbt, "inlet");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put(I18N.tr("Lake"), lake);
        info.put(I18N.tr("Steam in"), why.isEmpty() ? String.format("%.1f g/s", kgPerS * 1000) : why);
        if (inlet != null) info.put(I18N.tr("Inlet pressure"), String.format("%.2f bar of %s", inlet.P() / 1e5, EaUnits.contents(inlet)));
        return info;
    }
}
