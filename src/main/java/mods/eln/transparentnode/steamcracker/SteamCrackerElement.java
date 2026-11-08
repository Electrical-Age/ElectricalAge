package mods.eln.transparentnode.steamcracker;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.fluid.ElementFluidHandler;
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
    final ElementFluidHandler gasolineTank = new ElementFluidHandler(GASOLINE_TANK_MB);
    final ElementFluidHandler steamTank = new ElementFluidHandler(STEAM_TANK_MB);
    final ElementFluidHandler butadieneTank = new ElementFluidHandler(BUTADIENE_TANK_MB);
    final ElementFluidHandler gasTank = new ElementFluidHandler(GAS_TANK_MB);
    final PortBridge gasolineIn = new PortBridge("gasolineIn", PortBridge.Mode.IN, LIQUID_PORT_MB_PER_S, 0);
    final PortBridge steamIn = new PortBridge("steamIn", PortBridge.Mode.IN, STEAM_PORT_MB_PER_S, 0);
    final PortBridge butadieneOut = new PortBridge("butadieneOut", PortBridge.Mode.OUT, LIQUID_PORT_MB_PER_S, OUT_HEAD_PA);
    final PortBridge gasOut = new PortBridge("gasOut", PortBridge.Mode.OUT, LIQUID_PORT_MB_PER_S, OUT_HEAD_PA);
    /** The parts of a mB toward the next whole one: gasoline cracked, steam used, butadiene and gas made. */
    double carry, steamCarry, butadieneCarry, gasCarry;
    /** Gasoline cracked in all (mB). */
    double crackedAll;
    double sinceRun = 1e9;
    boolean running;
    String status = "idle";

    public SteamCrackerElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (SteamCrackerDescriptor) descriptor;
        gasolineTank.setFilter(new net.minecraft.world.level.material.Fluid[]{Oil.Cut.GASOLINE.source.get()});
        steamTank.setFilter(new net.minecraft.world.level.material.Fluid[]{Oil.STEAM.get()});
        butadieneTank.setFilter(new net.minecraft.world.level.material.Fluid[]{Butadiene.SOURCE.get()});
        gasTank.setFilter(new net.minecraft.world.level.material.Fluid[]{Oil.Cut.NATURALGAS.source.get()});
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    /** Whole mB of gasoline it could crack now, by its feeds and the room for its products (ignoring power). */
    int canCrack() {
        double steamPer = STEAM_PER_100 / 100, butPer = BUTADIENE_PER_100 / 100, gasPer = GAS_PER_100 / 100;
        double n = gasolineTank.getFluidAmount();
        n = Math.min(n, (steamTank.getFluidAmount() + steamCarry) / steamPer);
        n = Math.min(n, (butadieneTank.getCapacity() - butadieneTank.getFluidAmount() - butadieneCarry) / butPer);
        n = Math.min(n, (gasTank.getCapacity() - gasTank.getFluidAmount() - gasCarry) / gasPer);
        return (int) Math.floor(Math.max(0, n));
    }

    void tick(double dt) {
        boolean was = running;
        sinceRun += dt;
        int can = canCrack();
        if (gasolineTank.getFluidAmount() <= 0) status = "no gasoline";
        else if (steamTank.getFluidAmount() < STEAM_PER_100 / 100) status = "no steam";
        else if (can <= 0) status = "an outlet is full";
        else status = "cracking";
        if (can > 0) {
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            double share = rated > 0 ? got / rated : 0;
            carry += GASOLINE_MB_PER_S * share * dt;
            int move = (int) Math.min(Math.floor(carry), can);
            if (move > 0) {
                carry -= move;
                gasolineTank.drain(move, true);
                steamCarry -= move * STEAM_PER_100 / 100;
                int steam = (int) Math.ceil(-steamCarry);
                steamTank.drain(steam, true);
                steamCarry += steam;
                butadieneCarry += move * BUTADIENE_PER_100 / 100;
                int b = (int) Math.floor(butadieneCarry + 1e-9);
                if (b > 0) {
                    butadieneTank.fill(new FluidStack(Butadiene.SOURCE.get(), b), true);
                    butadieneCarry -= b;
                }
                gasCarry += move * GAS_PER_100 / 100;
                int g = (int) Math.floor(gasCarry + 1e-9);
                if (g > 0) {
                    gasTank.fill(new FluidStack(Oil.Cut.NATURALGAS.source.get(), g), true);
                    gasCarry -= g;
                }
                crackedAll += move;
            }
            if (share > 0.05) sinceRun = 0;
            else status = "no power";
        } else carry = Math.min(carry, 1);
        running = sinceRun < 2;
        if (running != was) needPublish();
    }

    // ---- tests

    /** (tests) pour gasoline or steam into its IN tanks. */
    public int fillIn(FluidStack f) {
        return f.getFluid() == Oil.STEAM.get() ? steamTank.fill(f, true) : gasolineTank.fill(f, true);
    }

    /** (tests) put butadiene or natural gas in its OUT tanks. */
    public int fillOut(FluidStack f) {
        return f.getFluid() == Butadiene.SOURCE.get() ? butadieneTank.fill(f, true) : gasTank.fill(f, true);
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
        return gasolineTank.getFluidAmount();
    }

    public int steamMb() {
        return steamTank.getFluidAmount();
    }

    public int butadieneMb() {
        return butadieneTank.getFluidAmount();
    }

    public int gasMb() {
        return gasTank.getFluidAmount();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
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
    public void beforeFluidStep(double dt) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        gasolineIn.step(gasolineTank, dt, t);
        steamIn.step(steamTank, dt, t);
        butadieneOut.step(butadieneTank, dt, t);
        gasOut.step(gasTank, dt, t);
    }

    @Override
    public void initialize() {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        double z = node.coordonate.y + FLANGE_Y / 16.0;
        gasolineIn.make(z, t);
        steamIn.make(z, t);
        butadieneOut.make(z, t);
        gasOut.make(z, t);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        gasolineTank.writeToNBT(nbt, "gasolineTank");
        steamTank.writeToNBT(nbt, "steamTank");
        butadieneTank.writeToNBT(nbt, "butadieneTank");
        gasTank.writeToNBT(nbt, "gasTank");
        gasolineIn.write(nbt);
        steamIn.write(nbt);
        butadieneOut.write(nbt);
        gasOut.write(nbt);
        nbt.putDouble("steamCarry", steamCarry);
        nbt.putDouble("butadieneCarry", butadieneCarry);
        nbt.putDouble("gasCarry", gasCarry);
        nbt.putDouble("crackedAll", crackedAll);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        gasolineTank.readFromNBT(nbt, "gasolineTank");
        steamTank.readFromNBT(nbt, "steamTank");
        butadieneTank.readFromNBT(nbt, "butadieneTank");
        gasTank.readFromNBT(nbt, "gasTank");
        gasolineIn.read(nbt);
        steamIn.read(nbt);
        butadieneOut.read(nbt);
        gasOut.read(nbt);
        steamCarry = nbt.getDoubleOr("steamCarry", 0);
        butadieneCarry = nbt.getDoubleOr("butadieneCarry", 0);
        gasCarry = nbt.getDoubleOr("gasCarry", 0);
        crackedAll = nbt.getDoubleOr("crackedAll", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        info.put(I18N.tr("Status"), I18N.tr(status));
        info.put(I18N.tr("Gasoline (blue C8)"), gasolineTank.getFluidAmount() + " / " + gasolineTank.getCapacity() + " mB");
        info.put(I18N.tr("Steam (blue H2O)"), mods.eln.sim.fluid.EaUnits.steamGrams(steamTank.getFluidAmount()) + " / "
            + mods.eln.sim.fluid.EaUnits.steamGrams(steamTank.getCapacity()));
        info.put(I18N.tr("Butadiene (red C4H6)"), butadieneTank.getFluidAmount() + " / " + butadieneTank.getCapacity() + " mB");
        info.put(I18N.tr("Natural gas (red CH4)"), gasTank.getFluidAmount() + " / " + gasTank.getCapacity() + " mB");
        return info;
    }
}
