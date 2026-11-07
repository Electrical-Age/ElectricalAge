package mods.eln.transparentnode.resincollector;

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
import mods.eln.oil.Latex;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Industrial Resin Collector (IndustrialResinCollectorDescriptor). The trunk is the column of logs behind it (its
 * back face); it needs leaves, as the hand collector does (TreeResinCollectorElement.treeLeaves). Latex a second:
 * the hand collector's resin a second for that trunk (3/5 a day a log, x the tree's factor) x SPEED_UP, at 250 mB a
 * resin, while it has power (less on a sagging line) and room in its tank.
 */
public class IndustrialResinCollectorElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    final IndustrialResinCollectorDescriptor descriptor;
    public final ElementMachine machine;
    final ElementFluidHandler tank = new ElementFluidHandler(IndustrialResinCollectorDescriptor.TANK_MB);
    final PortBridge outPort = new PortBridge("latexOut", PortBridge.Mode.OUT, IndustrialResinCollectorDescriptor.OUT_MB_PER_S,
        IndustrialResinCollectorDescriptor.OUT_HEAD_PA);
    /** Latex made but not yet a whole mB. */
    double carry;
    /** The tree, looked at now and then: resin a second it would give a hand collector (0: not a tree). */
    double handRate;
    double sinceLook = 1e9;
    boolean running;

    public IndustrialResinCollectorElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (IndustrialResinCollectorDescriptor) descriptor;
        tank.setFilter(new Fluid[]{Latex.SOURCE.get()});
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    void tick(double dt) {
        sinceLook += dt;
        if (sinceLook > 5) {
            sinceLook = 0;
            handRate = handRate(node.coordonate.world(), node.coordonate.pos(), front.back());
        }
        boolean room = tank.getCapacity() - tank.getFluidAmount() >= 1;
        boolean was = running;
        running = false;
        if (handRate > 0 && room) {
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            double share = rated > 0 ? got / rated : 0;
            running = share > 0.05;
            carry += handRate * IndustrialResinCollectorDescriptor.SPEED_UP * Latex.MB_PER_RUBBER * share * dt;
            int whole = (int) Math.floor(carry);
            if (whole > 0) {
                carry -= whole;
                tank.fill(new FluidStack(Latex.SOURCE.get(), whole), true);
            }
        }
        if (running != was) needPublish();
        if (!outPort.piped() && node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
            mods.eln.fluid.FluidExport.push(tank, level, node.coordonate.pos(), Direction.YP.toEnumFacing(),
                (int) Math.ceil(IndustrialResinCollectorDescriptor.OUT_MB_PER_S * dt));
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
        return 3.0 / 5 / (60 * 24) * (y1 - y0 + 1) * TreeResinCollectorElement.treeFactor(block);
    }

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (outPort.stub == null) return java.util.List.of();
        return java.util.List.of(outPort.port(node.coordonate.pos().asLong(), Direction.YP.toEnumFacing().get3DDataValue(), FluidPort.CENTRE));
    }

    @Override
    public void beforeFluidStep(double dt) {
        outPort.step(tank, dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void initialize() {
        outPort.make(node.coordonate.y + 1, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (outPort.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    /** Latex in its tank (mB; tests). */
    public int latex() {
        return tank.getFluidAmount();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt, "tank");
        outPort.write(nbt);
        nbt.putDouble("carry", carry);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        outPort.read(nbt);
        carry = nbt.getDoubleOr("carry", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        info.put(I18N.tr("Latex"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Tapping"), handRate <= 0 ? I18N.tr("no tree behind it (a trunk with leaves)")
            : String.format("%.0f mB/h", handRate * IndustrialResinCollectorDescriptor.SPEED_UP * Latex.MB_PER_RUBBER * 3600) + (running ? "" : " " + I18N.tr("(stopped)")));
        return info;
    }
}
