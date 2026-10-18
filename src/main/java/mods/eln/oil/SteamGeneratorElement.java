package mods.eln.oil;

import mods.eln.fluid.FuelRegistry;
import mods.eln.fluid.PreciseElementFluidHandler;
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
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

import mods.eln.fluid.FluidWorld;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SteamGeneratorDescriptor: steam in, electricity out of the front and back (like the fuel generator). The steam
 * collects in an inlet (a fluid-core node, notes/pipes/DESIGN.md phase 2): from a boiler set on top (its dome port
 * couples to the inlet port on top) or by pipe (the fluid capability). The power source offers what the inlet's steam
 * above 1 atm can give over the next second, up to nominal; the steam actually used (exhausted) follows the power the
 * load really takes, so an idle turbine keeps its steam.
 */
public class SteamGeneratorElement extends TransparentNodeElement implements FluidDevice {
    final SteamGeneratorDescriptor desc;
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    public final PowerSource source = new PowerSource("source", load);
    public final PreciseElementFluidHandler tank;
    final double jPerMb = FuelRegistry.INSTANCE.heatEnergyPerMilliBucket("steam");
    public static final double PORT_K = 3e-11;
    /** The inlet holds TANK_SECONDS of nominal steam at about this density (kg/m^3; ~5 bar). */
    static final double INLET_RHO = 2.5;
    public double steamRate;   // mB/s used, averaged over whole seconds
    double meterSteam, meterTime;
    public FluidNode inlet;
    private double[] loaded;
    private int shown;
    private List<FluidPort> ports = List.of();

    public SteamGeneratorElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (SteamGeneratorDescriptor) d;
        tank = new PreciseElementFluidHandler((int) Math.ceil(desc.nominalSteam() * SteamGeneratorDescriptor.TANK_SECONDS));
        tank.setFilter(new Fluid[]{Oil.STEAM.get()});
        electricalLoadList.add(load);
        electricalComponentList.add(source);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new Run());
    }

    /** Heat value of the inlet's steam per kg, as EA counts it: above liquid at 100 C (2.257 MJ/kg at 100 C). */
    double jPerKg() {
        return Water.hVapour(inlet.T()) - Water.hLiquid(Water.TB);
    }

    class Run implements IProcess {
        @Override
        public void process(double time) {
            if (inlet == null) return;
            // last step's output came from the inlet's steam: exhaust it
            double usedKg = source.getEffectiveP() * time / (SteamGeneratorDescriptor.EFFICIENCY * jPerKg());
            double mv = inlet.mVapour(), mg = mv + inlet.mAir;
            double got = usedKg > 0 && mv > 0 ? EaUnits.drainGas(inlet, usedKg * mg / mv) : 0;
            double used = got / EaUnits.KG_PER_MB_STEAM;
            meterSteam += used;
            meterTime += time;
            if (meterTime >= 1) {
                steamRate = meterSteam / meterTime;
                meterSteam = 0;
                meterTime = 0;
            }
            if (got > 0) {
                syncTank();
                show();
            }
            double offer = EaUnits.steamAvailable(inlet) * SteamGeneratorDescriptor.EFFICIENCY * jPerKg() / SteamGeneratorDescriptor.RESPONSE_SECONDS;
            source.setP(Math.min(desc.nominalP, offer));
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

    /** Take in the steam pipes put in since the last step (each step, and before saving). */
    void syncTank() {
        int in = tank.getFluidAmount() - shown;
        if (in > 0) EaUnits.addWater(inlet, in * EaUnits.KG_PER_MB_STEAM, Water.TB, true);
        shown = tank.getFluidAmount();
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncTank();
    }

    @Override
    public void afterFluidStep(double dt) {
        show();
    }

    /** The tank shows the inlet's steam above 1 atm (what pipes see and fill up to). */
    private void show() {
        int n = (int) Math.min(tank.getCapacity(), Math.floor(EaUnits.steamAvailable(inlet) / EaUnits.KG_PER_MB_STEAM));
        if (tank.getFluidAmount() > 0) tank.drain(tank.getFluidAmount(), true);
        if (n > 0) tank.fill(new net.neoforged.neoforge.fluids.FluidStack(Oil.STEAM.get(), n), true);
        shown = tank.getFluidAmount();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front || side == front.back()) ? load : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front || side == front.back()) ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        source.setUmax(desc.maxU);
        source.setImax(desc.nominalP * 5 / desc.maxU);
        var c = node.coordonate;
        double volume = desc.nominalSteam() * SteamGeneratorDescriptor.TANK_SECONDS * EaUnits.KG_PER_MB_STEAM / INLET_RHO;
        inlet = new FluidNode(volume, c.y, 1.0, false);
        if (loaded != null) {
            inlet.mWater = loaded[0];
            inlet.mAir = loaded[1];
            inlet.energy = loaded[2];
            inlet.update();
        } else {
            // new, or saved before the inlet: air, and the old tank's steam
            inlet.fill(0, Water.P_ATM, Water.T_AMBIENT);
            EaUnits.addWater(inlet, tank.getFluidAmount() * EaUnits.KG_PER_MB_STEAM, Water.TB, true);
        }
        show();
        ports = List.of(new FluidPort(inlet, c.pos().asLong(), 1, 0.5, PORT_K));   // the inlet on top
        connect();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (inlet != null) {
            syncTank();
            show();
        }
        tank.writeToNBT(nbt, "tank");
        if (inlet != null) {
            nbt.putDouble("inletW", inlet.mWater);
            nbt.putDouble("inletA", inlet.mAir);
            nbt.putDouble("inletU", inlet.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        if (nbt.contains("inletW"))
            loaded = new double[]{nbt.getDoubleOr("inletW", 0), nbt.getDoubleOr("inletA", 0), nbt.getDoubleOr("inletU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Steam"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Steam used"), String.format("%.0f mB/s", steamRate));
        if (inlet != null) info.put(I18N.tr("Inlet pressure"), String.format("%.2f bar", inlet.P() / 1e5));
        info.put(I18N.tr("Generated power"), Utils.plotPower("", source.getEffectiveP()));
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", load.getU()));
        return info;
    }
}
