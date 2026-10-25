package mods.eln.oil;

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
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import mods.eln.fluid.FluidWorld;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BoilerDescriptor: a vessel (a fluid-core node, notes/pipes/DESIGN.md phase 2) heated through its shell, a thermal load
 * on its back and flanks; the front carries the phase signal (liquid or gas at half the gauge glass). Water comes in by pipe at the feedwater flange on its right flank (front.right(), through a check
 * valve), or by the fluid capability; steam leaves by pipe, or through the dome port under it
 * straight into a turbine below (the dome pipe draws at the top of the vessel and comes down the outside), or through
 * its top port. No safety valve of its own (Baughn: pressure is real; fit a safety valve part on the line); it is
 * rated BURST_PA, above any pipe, so a sealed plant bursts a pipe first, and a boiler with nowhere to go bursts.
 */
public class BoilerElement extends TransparentNodeElement implements FluidDevice {
    final BoilerDescriptor desc;
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    public final BoilerTanks tanks = new BoilerTanks();
    public static final double VOLUME = 0.008, SHELL_C = 700, SHELL_TO_WATER_K_PER_W = 0.005;
    public static final double BURST_PA = 350e5, PORT_K = 6e-11;
    /** The vessel (built in initialize, where the block's height is known). */
    public FluidNode vessel;
    /** The feedwater inlet (Baughn: a water input flange): the pipe stub behind the right flank's flange, into the
     *  vessel through a check valve, so a stopped feed pump doesn't take boiling water back. */
    public FluidNode feed;
    private FluidDevice.Link feedCheck;
    public static final double FEED_VOLUME = 0.0005, FEED_DRAW = 0.3;
    private double[] loaded, loadedFeed;
    private List<FluidPort> ports = List.of();
    private final List<Vent> vents = new java.util.ArrayList<>();
    public double steamRate;   // mB/s that left the vessel as steam, for WAILA
    /**
     * The phase signal (Baughn: read the boiler's state as one reads a cable's temperature): a level switch with its
     * probe at half height (half the water tank, fill()), high while it is in liquid, low in gas; on the front face, under the gauge glass (Baughn).
     * PROBE_BAND either way is its deadband, so a feed pump run off it doesn't chatter.
     */
    public final NbtElectricalGateOutput phaseOut = new NbtElectricalGateOutput("phaseOut");
    public final NbtElectricalGateOutputProcess phaseOutProcess = new NbtElectricalGateOutputProcess("phaseOutProcess", phaseOut);
    public static final double PROBE = 0.5, PROBE_BAND = 0.03;
    public boolean liquidAtProbe;
    double meterSteam, meterTime;

    public BoilerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (BoilerDescriptor) d;
        thermalLoadList.add(thermal);
        electricalLoadList.add(phaseOut);
        electricalComponentList.add(phaseOutProcess);
        slowProcessList.add((mods.eln.sim.IProcess) t -> {
            if (vessel == null) return;
            double level = fill();
            if (liquidAtProbe ? level < PROBE - PROBE_BAND : level > PROBE + PROBE_BAND) liquidAtProbe = !liquidAtProbe;
            phaseOutProcess.state(liquidAtProbe);
        });
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        // dry and very hot: it fails (no explosion this round)
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(400).set(new Failure(this).machine()));
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        if (feed == null) return ports;
        // (the feed flange turns with the boiler: a wrench turns it, and the network is rebuilt on the reconnect)
        var all = new java.util.ArrayList<>(ports);
        all.add(new FluidPort(feed, node.coordonate.pos().asLong(), front.right().toEnumFacing().get3DDataValue(), 0.5, PORT_K));
        return all;
    }

    @Override
    public List<Link> fluidLinks() {
        return feedCheck == null ? List.of() : List.of(feedCheck);
    }

    @Override
    public List<Vent> fluidVents() {
        return vents;
    }

    /** How full it is, as its gauge glass shows it: liquid against the water tank's capacity (0..1; the vessel's
     *  own volume is mostly steam space). */
    public double fill() {
        return vessel.mLiquid() / (BoilerTanks.WATER_MB * EaUnits.KG_PER_MB_WATER);
    }

    /** Take in what pipes put in or took out since the last step (each step, and before saving). */
    public void syncTanks() {
        int in = tanks.takeWaterIn();
        if (in > 0) EaUnits.addWater(vessel, in * EaUnits.KG_PER_MB_WATER, Water.T_AMBIENT, false);
        int out = tanks.takeSteamOut();
        if (out > 0) EaUnits.drainVapour(vessel, out * EaUnits.KG_PER_MB_STEAM);
        meterSteam += out;
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncTanks();
        // the shell heats the water (EA thermal temperatures are relative to the ambient, Tref)
        double shellK = thermal.Tc + mods.eln.sim.PhysicalConstant.Tref;
        double q = (shellK - vessel.T()) / SHELL_TO_WATER_K_PER_W;
        vessel.heat += q;
        thermal.moveEnergyTo(-q * dt);
    }

    @Override
    public void afterFluidStep(double dt) {
        // steam out through the ports and the valve, for the meter
        double gone = 0;
        for (FluidEdge e : FluidWorld.edgesOf(vessel)) gone += (e.a == vessel ? e.fVapour : -e.fVapour) * dt;
        meterSteam += Math.max(0, gone) / EaUnits.KG_PER_MB_STEAM;
        meterTime += dt;
        if (meterTime >= 1) {
            steamRate = meterSteam / meterTime;
            meterSteam = 0;
            meterTime = 0;
        }
        showTanks();
        // over its rating the vessel bursts: what's in it flashes (explosion strength by that energy)
        if (vessel.P() > BURST_PA) {
            var v = vessel;
            new mods.eln.sim.process.destruct.Failure(this).pressureVessel(
                () -> Math.max(0, v.energy - v.mWater * Water.uLiquid(Water.TB) - v.mAir * Water.uAir(Water.T_AMBIENT)))
                .fail(mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE, v.P());
        }
    }

    /** A new boiler, or one saved before the vessel (old saves): its tanks' water and steam (mB), air for the rest, at
     *  the shell's temperature. Public for the GameTests. */
    public void fromTanks(int waterMb, int steamMb) {
        double T = thermal.Tc + mods.eln.sim.PhysicalConstant.Tref;
        vessel.fill(waterMb * EaUnits.KG_PER_MB_WATER, Water.P_ATM, T);
        EaUnits.addWater(vessel, steamMb * EaUnits.KG_PER_MB_STEAM, T, true);
    }

    public void showTanks() {
        tanks.show((int) Math.floor(vessel.mLiquid() / EaUnits.KG_PER_MB_WATER), (int) Math.floor(EaUnits.steamAvailable(vessel) / EaUnits.KG_PER_MB_STEAM));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (vessel != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return side == front && lrdu == LRDU.Down ? phaseOut : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return side == front ? null : thermal;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP) return 0;
        // (one face can't take both: a heater against it would get the signal into its power load)
        return side == front ? NodeBase.maskElectricalOutputGate : NodeBase.maskThermal;
    }

    @Override
    public String multiMeterString(Direction side) {
        return side == front ? Utils.plotSignal(phaseOut.getU(), phaseOut.getCurrent()) : "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    @Override
    public void initialize() {
        // a well-insulated kettle: the water's heat capacity is the vessel's now (8 B is ~540 g in EA's scale, ~2.3
        // kJ/K); the shell keeps ~0.7 kJ/K and loses ~80 W at 100 degrees. From cold, a 1 kW heater brings it to the
        // boil in ~5 minutes.
        thermal.set(0.01, 1.0, SHELL_C);
        var c = node.coordonate;
        vessel = new FluidNode(VOLUME, c.y, 1.0, false);
        if (loaded != null) {
            vessel.mWater = loaded[0];
            vessel.mAir = loaded[1];
            vessel.energy = loaded[2];
            vessel.update();
        } else fromTanks(tanks.water(), tanks.steam());
        showTanks();
        long pos = c.pos().asLong();
        feed = new FluidNode(FEED_VOLUME, c.y + FEED_DRAW, 0.1, false);
        if (loadedFeed != null) {
            feed.mWater = loadedFeed[0];
            feed.mAir = loadedFeed[1];
            feed.energy = loadedFeed[2];
            feed.update();
        } else feed.fill(0, Water.P_ATM, Water.T_AMBIENT);
        feedCheck = new Link(feed, 0, vessel, FEED_DRAW, PORT_K).kind(FluidEdge.Kind.CHECK);
        ports = List.of(new FluidPort(vessel, pos, 0, 0.97, PORT_K),    // the dome pipe's flange, underneath
            new FluidPort(vessel, pos, 1, 1.0, PORT_K));                  // the top port
        connect();
    }

    /** A water bucket fills it by hand (Baughn: buckets should work, pipes just automate it). */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        var hand = player.getMainHandItem();
        if (!hand.is(net.minecraft.world.item.Items.WATER_BUCKET)) return false;
        if (BoilerTanks.WATER_MB - tanks.water() < 1000) return false;
        tanks.fillWater(1000);
        if (!player.isCreative()) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
        return true;
    }

    /** For the gauges (BoilerRender): the fill and the water's temperature (C). */
    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(vessel == null ? 0 : (float) fill());
            stream.writeFloat(vessel == null ? 0 : (float) (vessel.T() - Water.T0));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getFluidHandler() {
        return tanks;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (vessel != null) {
            syncTanks();
            showTanks();
        }
        tanks.writeToNBT(nbt, "boiler");
        nbt.putBoolean("liquidAtProbe", liquidAtProbe);
        if (vessel != null) {
            nbt.putDouble("vesselW", vessel.mWater);
            nbt.putDouble("vesselA", vessel.mAir);
            nbt.putDouble("vesselU", vessel.energy);
        }
        if (feed != null) {
            nbt.putDouble("feedW", feed.mWater);
            nbt.putDouble("feedA", feed.mAir);
            nbt.putDouble("feedU", feed.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tanks.readFromNBT(nbt, "boiler");
        liquidAtProbe = nbt.getBooleanOr("liquidAtProbe", false);
        if (nbt.contains("vesselW"))
            loaded = new double[]{nbt.getDoubleOr("vesselW", 0), nbt.getDoubleOr("vesselA", 0), nbt.getDoubleOr("vesselU", 0)};
        if (nbt.contains("feedW"))
            loadedFeed = new double[]{nbt.getDoubleOr("feedW", 0), nbt.getDoubleOr("feedA", 0), nbt.getDoubleOr("feedU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        if (vessel != null) {
            info.put(I18N.tr("Water temperature"), String.format("%.0f °C", vessel.T() - Water.T0));
            info.put(I18N.tr("Pressure"), String.format("%.2f bar", vessel.P() / 1e5));
        }
        info.put(I18N.tr("Water"), tanks.water() + " / " + BoilerTanks.WATER_MB + " mB");
        info.put(I18N.tr("Steam"), tanks.steam() + " / " + BoilerTanks.STEAM_MB + " mB");
        info.put(I18N.tr("Steam made"), String.format("%.0f mB/s", steamRate));
        info.put(I18N.tr("At half height"), liquidAtProbe ? I18N.tr("liquid") : I18N.tr("gas"));
        return info;
    }
}
