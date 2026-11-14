package mods.eln.transparentnode.steamcracker;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.fluid.DeviceInlet;
import mods.eln.fluid.DeviceOutlet;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.oil.Butadiene;
import mods.eln.oil.Oil;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static mods.eln.transparentnode.steamcracker.SteamCrackerDescriptor.*;

/** The Steam Cracker (SteamCrackerDescriptor): gasoline and steam in at the back, butadiene and natural gas out at the front. */
public class SteamCrackerElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    final SteamCrackerDescriptor descriptor;
    public final ElementMachine machine;
    /** Its feed vessels: gasoline (it breathes; water drains) and steam (sealed; the condensate drains). */
    public final DeviceInlet gasolineIn = new DeviceInlet("gasolineIn", GASOLINE_TANK_MB, Oil.Cut.GASOLINE.source.get());
    public final DeviceInlet steamIn = new DeviceInlet("steamIn", STEAM_TANK_MB, Oil.STEAM.get()).sealed(STEAM_VESSEL_M3);
    /** Its product vessels, both sealed: butadiene leaves by a pump that keeps a sump, natural gas under its own pressure. */
    public final DeviceOutlet butadieneOut = new DeviceOutlet("butadieneOut", BUTADIENE_TANK_MB / 0.95, LIQUID_PORT_MB_PER_S, OUT_HEAD_PA, false, false)
        .only(Butadiene.SOURCE.get()).sump().keys("butadieneOutVessel", "butadieneTank");
    public final DeviceOutlet gasOut = new DeviceOutlet("gasOut", GAS_TANK_MB, LIQUID_PORT_MB_PER_S, OUT_HEAD_PA, false, false)
        .only(Oil.Cut.NATURALGAS.source.get()).gasOnly(GAS_VESSEL_M3).keys("gasOutVessel", "gasTank");
    /** Gasoline the power has paid for and not yet cracked (mB), and steam drawn out of its vessel for it (mB). */
    double carry, steamCharge;
    /** Gasoline cracked in all (mB). */
    double crackedAll;
    double sinceRun = 1e9;
    boolean running;
    String status = "idle";

    public SteamCrackerElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (SteamCrackerDescriptor) descriptor;
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    static final double STEAM_PER = STEAM_PER_100 / 100, BUTADIENE_PER = BUTADIENE_PER_100 / 100, GAS_PER = GAS_PER_100 / 100;

    boolean outletsHaveRoom(double mB) {
        return butadieneOut.room(mB * BUTADIENE_PER) && gasOut.room(mB * GAS_PER);
    }

    /** Its on/off lever (Nero 2026-11-08): off, it does nothing and draws nothing. */
    public final mods.eln.node.transparent.MachineSwitch onOff = new mods.eln.node.transparent.MachineSwitch();

    void tick(double dt) {
        if (!onOff.on) {
            if (running) {
                running = false;
                needPublish();
            }
            return;
        }
        boolean was = running;
        sinceRun += dt;
        // (the power is drawn here, a tick at a time; the gasoline is cracked before the next fluid step, in crack())
        if (gasolineIn.feedMb() <= 0) status = "no gasoline";
        else if (steamCharge < STEAM_PER) status = "no steam";
        else if (!outletsHaveRoom(1)) status = "an outlet is full";
        else {
            status = "cracking";
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            double share = rated > 0 ? got / rated : 0;
            carry += GASOLINE_MB_PER_S * share * dt;
            if (share > 0.05) sinceRun = 0;
            else status = "no power";
        }
        if (!status.equals("cracking")) carry = Math.min(carry, 1);
        running = sinceRun < 2;
        if (running != was) needPublish();
    }

    /** Crack what the power has paid for, by the gasoline and the steam it has and the room for its products. */
    void crack() {
        double move = Math.min(carry, Math.min(gasolineIn.feedMb(), steamCharge / STEAM_PER));
        if (move <= 0 || !outletsHaveRoom(move)) return;
        double got = gasolineIn.take(move);
        carry -= got;
        steamCharge = Math.max(0, steamCharge - got * STEAM_PER);
        butadieneOut.made(Butadiene.SOURCE.get(), got * BUTADIENE_PER);
        gasOut.made(Oil.Cut.NATURALGAS.source.get(), got * GAS_PER);
        crackedAll += got;
    }

    // ---- tests

    /** (tests) pour gasoline or steam (at 450 K) into its IN vessels. */
    public int fillIn(FluidStack f) {
        boolean steam = f.getFluid() == Oil.STEAM.get();
        (steam ? steamIn : gasolineIn).put(f.getFluid(), f.getAmount(), steam ? 450 : mods.eln.sim.Ambient.kelvin(node.coordonate));
        return f.getAmount();
    }

    /** (tests) put butadiene or natural gas in its OUT vessels. */
    public int fillOut(FluidStack f) {
        (f.getFluid() == Butadiene.SOURCE.get() ? butadieneOut : gasOut).put(f.getFluid(), f.getAmount(), mods.eln.sim.Ambient.kelvin(node.coordonate));
        return f.getAmount();
    }

    public boolean running() {
        return running;
    }

    public String status() {
        return status;
    }

    public double cracked() {
        return crackedAll;
    }

    public int gasolineMb() {
        return (int) Math.round(gasolineIn.feedMb());
    }

    /** Its steam: what it has drawn for the cracking, and the steam in its vessel (mB). */
    public int steamMb() {
        return (int) Math.round(steamCharge + steamIn.gasFeedKg() / mods.eln.sim.fluid.EaUnits.KG_PER_MB_STEAM);
    }

    public int butadieneMb() {
        return (int) Math.round(butadieneOut.holdsMb(Butadiene.SOURCE.get()));
    }

    public int gasMb() {
        return (int) Math.round(gasOut.holdsMb(Oil.Cut.NATURALGAS.source.get()));
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (onOff.activated(this, player, descriptor.toggle(), vx, vy, vz)) return true;
        return false;
    }

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (gasolineIn.stub == null) return java.util.List.of();
        long pos = node.coordonate.pos().asLong();
        Direction back = front.back();
        int b = back.toEnumFacing().get3DDataValue(), f = front.toEnumFacing().get3DDataValue();
        return java.util.List.of(
            gasolineIn.port(pos, b, PortBridge.endSpot(front, back, FLANGE_Y, FLANGE_Z_A)),
            steamIn.port(pos, b, PortBridge.endSpot(front, back, FLANGE_Y, FLANGE_Z_B)),
            butadieneOut.port(pos, f, PortBridge.endSpot(front, front, FLANGE_Y, FLANGE_Z_A)),
            gasOut.port(pos, f, PortBridge.endSpot(front, front, FLANGE_Y, FLANGE_Z_B)));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        var all = new java.util.ArrayList<Link>();
        all.addAll(gasolineIn.links());
        all.addAll(steamIn.links());
        all.addAll(butadieneOut.links());
        all.addAll(gasOut.links());
        return all;
    }

    @Override
    public java.util.List<Vent> fluidVents() {
        return gasolineIn.vents();
    }

    /** Switched on, it draws its steam a second ahead (with the steam everything else in the vessel's gas, which is
     *  how the air of a new line leaves), and cracks here, between the views' sync and show. */
    @Override
    public void beforeFluidStep(double dt) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate), perMb = mods.eln.sim.fluid.EaUnits.KG_PER_MB_STEAM;
        gasolineIn.sync(dt, t);
        steamIn.sync(dt, t);
        if (onOff.on) {
            if (steamCharge < STEAM_CHARGE_MB) steamCharge += steamIn.drawKg((STEAM_CHARGE_MB - steamCharge) * perMb) / perMb;
            crack();
        }
        gasolineIn.settle(dt);
        steamIn.settle(dt);
        butadieneOut.before(t);
        gasOut.before(t);
    }

    @Override
    public void afterFluidStep(double dt) {
        gasolineIn.after();
        steamIn.after();
        butadieneOut.after();
        gasOut.after();
    }

    @Override
    public void initialize() {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        double y = node.coordonate.y, z = y + FLANGE_Y / 16.0;
        gasolineIn.make(y, z, t);
        steamIn.make(y, z, t);
        butadieneOut.make(y, z, t);
        gasOut.make(y, z, t);
        connect();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (gasolineIn.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    /** Buckets and other mods' pipes: none (four streams on four flanges; only EA pipes tell them apart). */
    @Override
    public Object getFluidHandler() {
        return null;
    }

    // ---- electrical

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.powerLoad();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return machine.connectionMask(side);
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(machine.volts() > descriptor.nominalU * 0.5);
            stream.writeBoolean(running);
            stream.writeBoolean(onOff.on);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        onOff.write(nbt);
        gasolineIn.write(nbt);
        steamIn.write(nbt);
        butadieneOut.write(nbt);
        gasOut.write(nbt);
        nbt.putDouble("steamCharge", steamCharge);
        nbt.putDouble("crackedAll", crackedAll);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        onOff.read(nbt);
        gasolineIn.read(nbt, "gasolineTank", GASOLINE_TANK_MB);
        steamIn.read(nbt, "steamTank", STEAM_TANK_MB);
        butadieneOut.read(nbt, BUTADIENE_TANK_MB);
        gasOut.read(nbt, GAS_TANK_MB);
        steamCharge = nbt.getDoubleOr("steamCharge", 0);
        crackedAll = nbt.getDoubleOr("crackedAll", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        if (!onOff.on) info.put(I18N.tr("State"), I18N.tr("Switched off"));
        info.put(I18N.tr("Status"), I18N.tr(status));
        String wrong = gasolineIn.blockedBy();
        if (wrong != null) info.put(I18N.tr("Blocked"), I18N.tr("it holds %1$, which it can't crack", wrong));
        info.put(I18N.tr("Gasoline (blue C8)"), gasolineMb() + " / " + GASOLINE_TANK_MB + " mB");
        info.put(I18N.tr("Steam (blue H2O)"), mods.eln.sim.fluid.EaUnits.steamGrams(steamMb()) + String.format(", %.1f bar", steamIn.vessel == null ? 0 : steamIn.vessel.P() / 1e5));
        info.put(I18N.tr("Butadiene (red C4H6)"), butadieneMb() + " / " + BUTADIENE_TANK_MB + " mB");
        info.put(I18N.tr("Natural gas (red CH4)"), gasMb() + " mB" + String.format(", %.1f bar", gasOut.vessel == null ? 0 : gasOut.vessel.P() / 1e5));
        return info;
    }
}
