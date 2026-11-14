package mods.eln.transparentnode.resincollector;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.fluid.DeviceOutlet;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.oil.Latex;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Industrial Resin Collector (IndustrialResinCollectorDescriptor). The trunk is the column of logs behind it (its
 * back face); it needs leaves, as the hand collector does (TreeResinCollectorElement.treeLeaves). Latex a second:
 * the hand collector's resin a second for that trunk (3/5 a day a log, at most MAX_LOGS of them, x the tree's factor) x SPEED_UP, at 250 mB a
 * resin, while it has power (less on a sagging line) and room in its basin.
 * <p>
 * The basin is a vessel on the fluid core that breathes, with a pump from its floor to the flange on its top (DeviceOutlet,
 * as the tar sand separator's): OUT_MB_PER_S of latex free, none at OUT_HEAD_PA. Unpiped, the latex goes into whatever
 * takes fluid above it.
 */
public class IndustrialResinCollectorElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    final IndustrialResinCollectorDescriptor descriptor;
    public final ElementMachine machine;
    /** Its basin and outlet, under the old PortBridge's key. TANK_MB is the basin's rated fill (95 %), so a full old tank loads with air over it. */
    public final DeviceOutlet outlet = new DeviceOutlet("latexOut", IndustrialResinCollectorDescriptor.TANK_MB / 0.95,
        IndustrialResinCollectorDescriptor.OUT_MB_PER_S, IndustrialResinCollectorDescriptor.OUT_HEAD_PA, true, false);
    /** The tree, looked at now and then: resin a second it would give a hand collector (0: not a tree). */
    double handRate;
    double sinceLook = 1e9;
    boolean running;
    /** Seconds since it last tapped: the amber LED stays lit a moment past each (AdventurAgent: it flickered off). */
    double sinceTap = 1e9;

    public IndustrialResinCollectorElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (IndustrialResinCollectorDescriptor) descriptor;
        outlet.only(Latex.SOURCE.get());
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
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
        sinceLook += dt;
        if (sinceLook > 5) {
            sinceLook = 0;
            handRate = handRate(node.coordonate.world(), node.coordonate.pos(), front.back());
        }
        boolean room = outlet.room(1);
        boolean was = running;
        sinceTap += dt;
        if (handRate > 0 && room) {
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            double share = rated > 0 ? got / rated : 0;
            if (share > 0.05) sinceTap = 0;
            outlet.made(Latex.SOURCE.get(), handRate * IndustrialResinCollectorDescriptor.SPEED_UP * Latex.MB_PER_RUBBER * share * dt);
        }
        running = sinceTap < 2;
        if (running != was) needPublish();
        // (an EA pipe on its flange takes it, its pump; else whatever takes fluid above it)
        if (node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
            outlet.export(level, node.coordonate.pos(), Direction.YP.toEnumFacing(), (int) Math.ceil(IndustrialResinCollectorDescriptor.OUT_MB_PER_S * dt));
    }

    /**
     * Resin a second a hand collector would get from the trunk behind (pos, toward back): 3/5 a day a log of the joined
     * column, x the tree's factor (TreeResinCollectorElement.treeFactor); 0 if it's not a tree (logs without leaves).
     */
    public static double handRate(Level level, BlockPos pos, Direction back) {
        BlockPos log = pos.relative(back.toEnumFacing());
        var block = level.getBlockState(log).getBlock();
        if (!TreeResinCollectorDescriptor.isWood(block)) return 0;
        int y0 = log.getY(), y1 = log.getY();
        while (TreeResinCollectorDescriptor.isWood(level.getBlockState(new BlockPos(log.getX(), y0 - 1, log.getZ())).getBlock())) y0--;
        while (TreeResinCollectorDescriptor.isWood(level.getBlockState(new BlockPos(log.getX(), y1 + 1, log.getZ())).getBlock())) y1++;
        if (TreeResinCollectorElement.treeLeaves(level, log) < 4) return 0;
        // (Baughn 2026-11-07: a fig's 10-13 logs gave one collector ~270 Raw Rubber an hour; it counts at most MAX_LOGS)
        int logs = Math.min(y1 - y0 + 1, IndustrialResinCollectorDescriptor.MAX_LOGS);
        return 3.0 / 5 / (60 * 24) * logs * TreeResinCollectorElement.treeFactor(block);
    }

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (outlet.outlet == null) return java.util.List.of();
        return java.util.List.of(outlet.port(node.coordonate.pos().asLong(), Direction.YP.toEnumFacing().get3DDataValue(), FluidPort.CENTRE));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        return outlet.links();
    }

    @Override
    public java.util.List<Vent> fluidVents() {
        return outlet.vents();
    }

    @Override
    public void beforeFluidStep(double dt) {
        outlet.before(mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void afterFluidStep(double dt) {
        outlet.after();
    }

    @Override
    public void initialize() {
        outlet.make(node.coordonate.y, node.coordonate.y + 1, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (outlet.outlet != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    @Override
    public Object getFluidHandler() {
        return outlet.view;
    }

    /** Latex in its basin (mB). */
    public int latex() {
        return (int) Math.round(outlet.liquidMb());
    }

    /** Put mB of latex straight into its basin (tests). */
    public void putLatex(int mB) {
        outlet.put(Latex.SOURCE.get(), mB, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (onOff.activated(this, player, descriptor.toggle(), vx, vy, vz)) return true;
        return false;
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
        outlet.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        onOff.read(nbt);
        // (an old save's tank goes into the basin, its PortBridge stub's contents into the outlet)
        outlet.read(nbt, IndustrialResinCollectorDescriptor.TANK_MB);
        // (and the latex it had made that wasn't yet a whole mB)
        outlet.made(Latex.SOURCE.get(), nbt.getDoubleOr("carry", 0));
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        if (!onOff.on) info.put(I18N.tr("State"), I18N.tr("Switched off"));
        info.put(I18N.tr("Latex"), latex() + " / " + IndustrialResinCollectorDescriptor.TANK_MB + " mB");
        info.put(I18N.tr("Tapping"), handRate <= 0 ? I18N.tr("no tree behind it (a trunk with leaves)")
            : String.format("%.0f mB/h", handRate * IndustrialResinCollectorDescriptor.SPEED_UP * Latex.MB_PER_RUBBER * 3600) + (running ? "" : " " + I18N.tr("(stopped)")));
        return info;
    }
}
