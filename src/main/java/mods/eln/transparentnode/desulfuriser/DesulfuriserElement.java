package mods.eln.transparentnode.desulfuriser;

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
import mods.eln.oil.Oil;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** The Desulfuriser (DesulfuriserDescriptor): a cut in at the back, treated, out at the front; sulfur left behind. */
public class DesulfuriserElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    final DesulfuriserDescriptor descriptor;
    public final ElementMachine machine;
    final ElementFluidHandler inTank = new ElementFluidHandler(DesulfuriserDescriptor.TANK_MB);
    final ElementFluidHandler outTank = new ElementFluidHandler(DesulfuriserDescriptor.TANK_MB);
    final PortBridge inPort = new PortBridge("cutIn", PortBridge.Mode.IN, DesulfuriserDescriptor.PORT_MB_PER_S, 0);
    final PortBridge outPort = new PortBridge("cutOut", PortBridge.Mode.OUT, DesulfuriserDescriptor.PORT_MB_PER_S, DesulfuriserDescriptor.OUT_HEAD_PA);
    /** Treated toward the next Sulfur Dust (mB), dust waiting, treated in all (mB), and the part of a mB treated. */
    double treatedToward, treatedAll, carry;
    int sulfur;
    double sinceRun = 1e9, sincePush;
    boolean running;

    public DesulfuriserElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (DesulfuriserDescriptor) descriptor;
        Fluid[] cuts = {Oil.Cut.GASOLINE.source.get(), Oil.Cut.DIESEL.source.get(), Oil.Cut.HEAVYOIL.source.get(), Oil.CRUDE.get()};
        inTank.setFilter(cuts);
        outTank.setFilter(cuts);
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    void tick(double dt) {
        boolean was = running;
        sinceRun += dt;
        FluidStack in = inTank.getFluid();
        int room = outTank.getCapacity() - outTank.getFluidAmount();
        boolean sameFluid = outTank.getFluidAmount() == 0 || in.isEmpty() || outTank.getFluid().getFluid() == in.getFluid();
        if (!in.isEmpty() && room > 0 && sameFluid && sulfur < DesulfuriserDescriptor.MAX_SULFUR) {
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            double share = rated > 0 ? got / rated : 0;
            carry += DesulfuriserDescriptor.TREAT_MB_PER_S * share * dt;
            int move = (int) Math.min(Math.floor(carry), Math.min(in.getAmount(), room));
            if (move > 0) {
                carry -= move;
                Fluid f = in.getFluid();
                inTank.drain(move, true);
                outTank.fill(new FluidStack(f, move), true);
                treatedToward += move;
                treatedAll += move;
                while (treatedToward >= DesulfuriserDescriptor.MB_PER_SULFUR) {
                    treatedToward -= DesulfuriserDescriptor.MB_PER_SULFUR;
                    sulfur++;
                }
            }
            if (share > 0.05) sinceRun = 0;
        } else carry = Math.min(carry, 1);
        running = sinceRun < 2;
        if (running != was) needPublish();
        if (!outPort.piped() && node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
            mods.eln.fluid.FluidExport.push(outTank, level, node.coordonate.pos(), front.toEnumFacing(), (int) Math.ceil(DesulfuriserDescriptor.PORT_MB_PER_S * dt));
        sincePush += dt;
        if (sincePush >= 1 && sulfur > 0) {
            sincePush = 0;
            var level = node.coordonate.world();
            var h = level == null ? null : level.getCapability(Capabilities.Item.BLOCK, node.coordonate.pos().below(), net.minecraft.core.Direction.UP);
            if (h != null)
                try (Transaction tx = Transaction.openRoot()) {
                    int gotItems = ResourceHandlerUtil.insertStacking(h, ItemResource.of(new ItemStack(mods.eln.registry.ElnRegistry.SULFUR_DUST.get())), sulfur, tx);
                    tx.commit();
                    sulfur -= gotItems;
                }
        }
    }

    /** (tests) pour a cut into its IN tank. */
    public int fillIn(FluidStack f) {
        return inTank.fill(f, true);
    }

    /** (tests) */
    public double treated() {
        return treatedAll;
    }

    public int inMb() {
        return inTank.getFluidAmount();
    }

    public int outMb() {
        return outTank.getFluidAmount();
    }

    public int sulfur() {
        return sulfur;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (sulfur <= 0 || node.coordonate.world().isClientSide()) return sulfur > 0;
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.SULFUR_DUST.get(), sulfur);
        sulfur = 0;
        if (!player.getInventory().add(s)) {
            var p = player.position();
            net.minecraft.world.Containers.dropItemStack(player.level(), p.x, p.y, p.z, s);
        }
        return true;
    }

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (inPort.stub == null || outPort.stub == null) return java.util.List.of();
        long pos = node.coordonate.pos().asLong();
        Direction back = front.back();
        return java.util.List.of(
            inPort.port(pos, back.toEnumFacing().get3DDataValue(), PortBridge.endSpot(front, back, DesulfuriserDescriptor.FLANGE_Y, DesulfuriserDescriptor.FLANGE_Z)),
            outPort.port(pos, front.toEnumFacing().get3DDataValue(), PortBridge.endSpot(front, front, DesulfuriserDescriptor.FLANGE_Y, DesulfuriserDescriptor.FLANGE_Z)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inPort.step(inTank, dt, t);
        outPort.step(outTank, dt, t);
    }

    @Override
    public void initialize() {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inPort.make(node.coordonate.y + DesulfuriserDescriptor.FLANGE_Y / 16.0, t);
        outPort.make(node.coordonate.y + DesulfuriserDescriptor.FLANGE_Y / 16.0, t);
        connect();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inPort.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    /** Buckets and other mods' pipes see the treated cut. */
    @Override
    public Object getFluidHandler() {
        return outTank;
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
        inTank.writeToNBT(nbt, "cutInTank");
        outTank.writeToNBT(nbt, "cutOutTank");
        inPort.write(nbt);
        outPort.write(nbt);
        nbt.putDouble("treatedToward", treatedToward);
        nbt.putDouble("treatedAll", treatedAll);
        nbt.putInt("sulfur", sulfur);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        inTank.readFromNBT(nbt, "cutInTank");
        outTank.readFromNBT(nbt, "cutOutTank");
        inPort.read(nbt);
        outPort.read(nbt);
        treatedToward = nbt.getDoubleOr("treatedToward", 0);
        treatedAll = nbt.getDoubleOr("treatedAll", 0);
        sulfur = nbt.getIntOr("sulfur", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        info.put(I18N.tr("In (blue)"), inTank.getFluidAmount() + " / " + inTank.getCapacity() + " mB");
        info.put(I18N.tr("Out (red)"), outTank.getFluidAmount() + " / " + outTank.getCapacity() + " mB");
        info.put(I18N.tr("Sulfur Dust"), String.valueOf(sulfur));
        return info;
    }
}
