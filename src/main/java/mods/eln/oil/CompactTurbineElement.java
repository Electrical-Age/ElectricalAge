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
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CompactTurbineDescriptor: one lumped boiler (a thermal load on its left flank) and a water tank. Its governor offers
 * power in proportion to how far the boiler is between T_START and T_RUN, so the boiler settles where the heat coming in
 * matches what the load takes (a short-of-heat turbine runs cooler and gives less); the heat taken is the power over ETA.
 * Heat the load doesn't take raises the boiler past T_BYPASS, where the bypass sends the steam to the condenser (heat to
 * the air: fuel, not water); past T_VENT the safety valve lets it out (water). The condenser returns all but
 * MAKEUP_SHARE of the steam it gets. Out of water it stops and only heats up; it doesn't fail.
 */
public class CompactTurbineElement extends TransparentNodeElement implements FluidDevice {
    final CompactTurbineDescriptor desc;
    public final NbtThermalLoad boiler = new NbtThermalLoad("boiler");
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    public final PowerSource source = new PowerSource("source", load);
    public final CompactTurbineTank tank = new CompactTurbineTank();
    /** Water in its tank (mB). */
    public double water;
    /** The governor's opening (0..1), and the heat to the bypass and the safety valve now (W). */
    public double governor, bypassW, ventW;
    /** Averaged over whole seconds, for Jade: power out, heat taken, water used (mB/s). */
    public double powerOut, heatIn, waterRate;
    double meterOut, meterHeat, meterWater, meterTime;
    /** The pipe on its top flange, and the well under it the float valve fills (the tank takes the well's water). */
    public FluidNode inlet, well;
    private FluidDevice.Link makeup;
    private List<FluidPort> ports = List.of();
    private double[] loadedIn, loadedWell;
    public static final double WELL_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;

    public CompactTurbineElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (CompactTurbineDescriptor) d;
        thermalLoadList.add(boiler);
        electricalLoadList.add(load);
        electricalComponentList.add(source);
        thermalFastProcessList.add(new Heat());
        slowProcessList.add(new Govern());
        slowProcessList.add(new NodePeriodicPublishProcess(node, 1, 0.5));
        slowProcessList.add(new ThermalLoadWatchDog().set(boiler).setTMax(CompactTurbineDescriptor.T_MAX - PhysicalConstant.Tamb)
            .set(new Failure(this).machine()));
    }

    /** The boiler's temperature (C). */
    public double celsius() {
        return boiler.Tc + PhysicalConstant.Tamb;
    }

    /** The governor: power offered by the boiler's temperature, none without water. */
    class Govern implements IProcess {
        @Override
        public void process(double time) {
            takePiped();
            double t = celsius();
            governor = water <= 0 ? 0 : Math.max(0, Math.min(1,
                (t - CompactTurbineDescriptor.T_START) / (CompactTurbineDescriptor.T_RUN - CompactTurbineDescriptor.T_START)));
            source.setUmax(desc.maxU);
            source.setP(governor * desc.nominalP);
        }
    }

    /** The boiler's heat out: to the load (over ETA), the bypass and the safety valve; the water that costs. */
    class Heat implements IProcess {
        @Override
        public void process(double time) {
            double t = celsius(), out = Math.max(0, source.getEffectiveP());
            boolean wet = water > 0;
            double run = wet ? out / CompactTurbineDescriptor.ETA : 0;
            bypassW = wet ? Math.max(0, Math.min(CompactTurbineDescriptor.BYPASS_MAX, (t - CompactTurbineDescriptor.T_BYPASS) * CompactTurbineDescriptor.BYPASS_K)) : 0;
            ventW = wet ? Math.max(0, Math.min(CompactTurbineDescriptor.VENT_MAX, (t - CompactTurbineDescriptor.T_VENT) * CompactTurbineDescriptor.VENT_K)) : 0;
            double q = run + bypassW + ventW;
            boiler.movePowerTo(-q);
            double used = ((run + bypassW) * CompactTurbineDescriptor.MAKEUP_SHARE + ventW) / CompactTurbineDescriptor.J_PER_MB * time;
            water = Math.max(0, water - used);
            meterOut += (wet ? out : 0) * time;
            meterHeat += q * time;
            meterWater += used;
            meterTime += time;
            if (meterTime >= 1) {
                powerOut = meterOut / meterTime;
                heatIn = meterHeat / meterTime;
                waterRate = meterWater / meterTime;
                meterOut = meterHeat = meterWater = meterTime = 0;
            }
        }
    }

    /** Take in what pipes put in through the capability since the last call, and show the tank. */
    void takePiped() {
        water = Math.min(CompactTurbineDescriptor.WATER_MB, water + tank.takeIn());
        tank.show(water);
    }

    @Override
    public Object getFluidHandler() {
        return tank;
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
        return makeup == null ? List.of() : List.of(makeup);
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (well == null) return;
        // the tank takes the well's water; the float valve shuts a bucket short of full
        double room = CompactTurbineDescriptor.WATER_MB - water, m = Math.min(well.mLiquid(), room * EaUnits.KG_PER_MB_WATER);
        if (m > 0) {
            double T = well.T();
            well.mWater -= m;
            well.energy -= m * Water.hLiquid(T);
            well.update();
            water += m / EaUnits.KG_PER_MB_WATER;
            tank.show(water);
        }
        makeup.open = water < CompactTurbineDescriptor.WATER_MB - 1000 ? 1 : 0;
    }

    @Override
    public void afterFluidStep(double dt) {
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (well != null) FluidWorld.add(this);
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
        return side == front.left() ? boiler : null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front || side == front.back()) return NodeBase.maskElectricalPower;
        return side == front.left() ? NodeBase.maskThermal : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T:", boiler.Tc);
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        boiler.set(CompactTurbineDescriptor.BOILER_RS, CompactTurbineDescriptor.BOILER_RP, CompactTurbineDescriptor.BOILER_C);
        source.setUmax(desc.maxU);
        source.setImax(desc.nominalP * 2 / desc.nominalU);
        var c = node.coordonate;
        inlet = new FluidNode(WELL_VOLUME, c.y + 1, 0.05, false);
        restore(inlet, loadedIn);
        inlet.lumpable = true;   // (it merges into the pipe on its flange)
        inlet.resistance = 1 / PORT_K;
        well = new FluidNode(WELL_VOLUME, c.y + 0.5, 0.25, false);
        restore(well, loadedWell);
        makeup = new FluidDevice.Link(inlet, 0, well, 1.0, PORT_K).kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
        ports = List.of(new FluidPort(inlet, c.pos().asLong(), 1, 0.0, PORT_K));   // the flange on top
        tank.show(water);
        connect();
    }

    private void restore(FluidNode n, double[] saved) {
        if (saved != null) {
            n.mWater = saved[0];
            n.mAir = saved[1];
            n.energy = saved[2];
            n.update();
        } else n.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    /** A water bucket fills it by hand. */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        var hand = player.getMainHandItem();
        if (!hand.is(net.minecraft.world.item.Items.WATER_BUCKET)) return false;
        takePiped();
        if (CompactTurbineDescriptor.WATER_MB - water < 1000) return false;
        water += 1000;
        tank.show(water);
        if (!player.isCreative()) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
        return true;
    }

    /** For the render: the rotor's speed (the governor while it makes power) and the safety valve's share. */
    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(water > 0 && celsius() > CompactTurbineDescriptor.T_START ? (float) Math.max(0.15, powerOut / desc.nominalP) : 0);
            stream.writeFloat((float) Math.min(1, ventW / 2000));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("water", water);
        if (well != null) {
            nbt.putDouble("inletW", inlet.mWater);
            nbt.putDouble("inletA", inlet.mAir);
            nbt.putDouble("inletU", inlet.energy);
            nbt.putDouble("wellW", well.mWater);
            nbt.putDouble("wellA", well.mAir);
            nbt.putDouble("wellU", well.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        water = nbt.getDoubleOr("water", 0);
        if (nbt.contains("inletW"))
            loadedIn = new double[]{nbt.getDoubleOr("inletW", 0), nbt.getDoubleOr("inletA", 0), nbt.getDoubleOr("inletU", 0)};
        if (nbt.contains("wellW"))
            loadedWell = new double[]{nbt.getDoubleOr("wellW", 0), nbt.getDoubleOr("wellA", 0), nbt.getDoubleOr("wellU", 0)};
    }

    /** What it is doing, in a line (Jade). */
    public String state() {
        double t = celsius();
        if (water <= 0) return I18N.tr("out of water: a bucket, or a pipe on top");
        if (t < 100) return I18N.tr("heating up: it boils at 100 °C");
        if (ventW > 0) return I18N.tr("venting: more heat than it can use or shed");
        if (governor < 0.9) return I18N.tr("short of heat: add combustion chambers for more power");
        if (bypassW > 0) return I18N.tr("running; the heat the load doesn't take goes to its condenser");
        return I18N.tr("running");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("State"), state());
        info.put(I18N.tr("Generated power"), Utils.plotPower("", powerOut) + " / " + Utils.plotPower("", desc.nominalP));
        // (Erisi's chat review: nobody knew they needed more combustion chambers)
        info.put(I18N.tr("Heat in"), Utils.plotPower("", heatIn) + " / " + Utils.plotPower("", desc.nominalHeat()) + " " + I18N.tr("for full power"));
        info.put(I18N.tr("Boiler"), String.format("%.0f °C", celsius()));
        info.put(I18N.tr("Water"), String.format("%.0f / %d mB (%.2f mB/s)", water, CompactTurbineDescriptor.WATER_MB, waterRate));
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", load.getU()));
        return info;
    }
}
