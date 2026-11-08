package mods.eln.transparentnode.polymeriser;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static mods.eln.transparentnode.polymeriser.PolymeriserDescriptor.*;

/** The Polymeriser (PolymeriserDescriptor): butadiene in at the back, Soul Dust as initiator, Raw Rubber out below. */
public class PolymeriserElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    public static final int SOUL_SLOT = 0, RUBBER_SLOT = 1;
    final PolymeriserDescriptor descriptor;
    public final ElementMachine machine;
    final PolymeriserInventory inventory = new PolymeriserInventory(this);
    final ButadieneFeed feed = new ButadieneFeed(TANK_MB);
    final PortBridge inPort = new PortBridge("butadieneIn", PortBridge.Mode.IN, PORT_MB_PER_S, 0);
    /** Energy toward the next Raw Rubber (J), Raw Rubber the current Soul Dust still initiates, made in all. */
    double progress;
    int initiator, made;
    double sinceRun = 1e9, sincePush;
    boolean running;
    String status = "idle";

    public PolymeriserElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (PolymeriserDescriptor) descriptor;
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    boolean roomForRubber() {
        ItemStack out = inventory.getItem(RUBBER_SLOT);
        return out.isEmpty() || out.is(ElnRegistry.RAW_RUBBER.get()) && out.getCount() < out.getMaxStackSize();
    }

    void tick(double dt) {
        boolean was = running;
        sinceRun += dt;
        boolean soul = initiator > 0 || inventory.getItem(SOUL_SLOT).is(ElnRegistry.SOUL_DUST.get());
        if (feed.asLiquid() < MB_PER_RUBBER - 0.5) status = "no butadiene";   // (vapour counts by mass: a part of a mB short is enough)
        else if (!soul) status = "no Soul Dust";
        else if (!roomForRubber()) status = "its Raw Rubber is full";
        else {
            status = "polymerising";
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            progress += got;
            if (rated > 0 && got / rated > 0.05) sinceRun = 0;
            else status = "no power";
            if (progress >= JOULES_PER_RUBBER) {
                progress -= JOULES_PER_RUBBER;
                if (initiator <= 0) {
                    inventory.removeItem(SOUL_SLOT, 1);
                    initiator = RUBBER_PER_SOUL_DUST;
                }
                initiator--;
                feed.take(MB_PER_RUBBER);
                ItemStack out = inventory.getItem(RUBBER_SLOT);
                if (out.isEmpty()) inventory.setItem(RUBBER_SLOT, new ItemStack(ElnRegistry.RAW_RUBBER.get()));
                else out.grow(1);
                inventory.setChanged();
                made++;
            }
        }
        running = sinceRun < 2;
        if (running != was) needPublish();
        sincePush += dt;
        if (sincePush >= 1 && !inventory.getItem(RUBBER_SLOT).isEmpty()) {
            sincePush = 0;
            var level = node.coordonate.world();
            var h = level == null ? null : level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, node.coordonate.pos().below(), net.minecraft.core.Direction.UP);
            if (h != null) {
                ItemStack out = inventory.getItem(RUBBER_SLOT);
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    int n = net.neoforged.neoforge.transfer.ResourceHandlerUtil.insertStacking(h, net.neoforged.neoforge.transfer.item.ItemResource.of(out), out.getCount(), tx);
                    tx.commit();
                    if (n > 0) {
                        inventory.removeItem(RUBBER_SLOT, n);
                        inventory.setChanged();
                    }
                }
            }
        }
    }

    // ---- tests

    /** (tests) pour butadiene (liquid or vapour) into its feed. */
    public int fillIn(FluidStack f) {
        return feed.fill(f);
    }

    public double butadieneMb() {
        return feed.asLiquid();
    }

    public int made() {
        return made;
    }

    public String status() {
        return status;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    /** Soul Dust in hand goes in; an empty hand takes the Raw Rubber. */
    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (node.coordonate.world().isClientSide()) return true;
        ItemStack hand = player.getMainHandItem();
        if (hand.is(ElnRegistry.SOUL_DUST.get())) {
            ItemStack in = inventory.getItem(SOUL_SLOT);
            int n = Math.min(hand.getCount(), 64 - in.getCount());
            if (n <= 0) return true;
            if (in.isEmpty()) inventory.setItem(SOUL_SLOT, hand.copyWithCount(n));
            else in.grow(n);
            hand.shrink(n);
            inventory.setChanged();
            return true;
        }
        ItemStack out = inventory.getItem(RUBBER_SLOT);
        if (out.isEmpty()) return false;
        inventory.setItem(RUBBER_SLOT, ItemStack.EMPTY);
        inventory.setChanged();
        if (!player.getInventory().add(out)) {
            var p = player.position();
            net.minecraft.world.Containers.dropItemStack(player.level(), p.x, p.y, p.z, out);
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
        if (inPort.stub == null) return java.util.List.of();
        Direction back = front.back();
        return java.util.List.of(inPort.port(node.coordonate.pos().asLong(), back.toEnumFacing().get3DDataValue(),
            PortBridge.endSpot(front, back, FLANGE_Y, FLANGE_Z)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        inPort.step(feed, dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void initialize() {
        inPort.make(node.coordonate.y + FLANGE_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
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
        feed.write(nbt, "butadiene");
        inPort.write(nbt);
        nbt.putDouble("progress", progress);
        nbt.putInt("initiator", initiator);
        nbt.putInt("made", made);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        feed.read(nbt, "butadiene");
        inPort.read(nbt);
        progress = nbt.getDoubleOr("progress", 0);
        initiator = nbt.getIntOr("initiator", 0);
        made = nbt.getIntOr("made", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        info.put(I18N.tr("Status"), I18N.tr(status));
        info.put(I18N.tr("Butadiene (blue)"), String.format("%.0f / %d mB", feed.asLiquid(), TANK_MB));
        info.put(I18N.tr("Soul Dust"), inventory.getItem(SOUL_SLOT).getCount() + (initiator > 0 ? " (+" + initiator + " rubber left)" : ""));
        info.put(I18N.tr("Raw Rubber"), String.valueOf(inventory.getItem(RUBBER_SLOT).getCount()));
        return info;
    }
}
