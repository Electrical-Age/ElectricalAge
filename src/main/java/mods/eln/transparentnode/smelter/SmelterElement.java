package mods.eln.transparentnode.smelter;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalResistor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The smelter (SmelterDescriptor). Heat comes in through a thermal cable on its back into thermalLoad; while it has
 * something to smelt and its controls have power, the work takes heat out of it at the electrical oven's rate for that
 * temperature (smeltResistor, R = T / PfT(T)), 1000 J an item. Its controls: a 20 W load on a side (LV cable).
 */
public class SmelterElement extends TransparentNodeElement {
    /** The input, then OUT_SLOTS output slots from outSlotId: a small chest, so no hopper is needed under it (Baughn). */
    public static final int inSlotId = 0, outSlotId = 1, OUT_SLOTS = 4;
    final TransparentNodeElementInventory inventory = new SmelterInventory(outSlotId + OUT_SLOTS, 64, this);

    final NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    final ThermalResistor smeltResistor = new ThermalResistor(thermalLoad, ThermalLoad.externalLoad);
    final NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    final Resistor control = new Resistor(electricalLoad, null);
    SmelterDescriptor descriptor;

    double energyCounter;
    boolean smelting;
    ItemStack lastIn = ItemStack.EMPTY;

    public SmelterElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (SmelterDescriptor) descriptor;
        thermalLoadList.add(thermalLoad);
        thermalFastProcessList.add(smeltResistor);
        electricalLoadList.add(electricalLoad);
        electricalComponentList.add(control);
        slowProcessList.add(process);
        slowProcessList.add(pull);
        slowProcessList.add(new ThermalLoadWatchDog().set(thermalLoad).setTMax(SmelterDescriptor.T_MAX).set(new Failure(this).machine()));
        slowProcessList.add(new VoltageStateWatchDog().set(electricalLoad).setUNominal(mods.eln.Eln.LVU).set(new Failure(this).machine()));
        slowProcessList.add(new mods.eln.node.NodePeriodicPublishProcess(node, 2, 1));
    }

    /** The work, each slow step: does it have something to smelt and power for its controls; how much heat it took. */
    final IProcess process = time -> {
        descriptor.refreshTo(thermalLoad);
        ItemStack in = inventory.getItem(inSlotId);
        if (!ItemStack.isSameItemSameComponents(in, lastIn)) {
            energyCounter = 0;
            lastIn = in.copy();
        }
        boolean powered = electricalLoad.getU() > SmelterDescriptor.CONTROL_MIN * mods.eln.Eln.LVU;
        smelting = powered && canSmelt();
        if (smelting) {
            energyCounter += smeltResistor.getP() * time;
            if (energyCounter >= ENERGY_PER_ITEM) {
                energyCounter -= ENERGY_PER_ITEM;
                smeltOne();
            }
            double t = Math.abs(thermalLoad.Tc) + 1;
            smeltResistor.setR(t / Math.max(1e-6, descriptor.PfT.getValue(t)));
        } else smeltResistor.highImpedance();
    };

    /** Seconds between pulls: a hopper's 8 ticks. */
    static final double PULL_PERIOD = 0.4;
    double pullTimer;

    /**
     * Its top is a hopper's funnel, so it acts as one (Baughn): one item every PULL_PERIOD from the inventory above into
     * the input slot, only something it can smelt now and only while the slot can take it.
     */
    final IProcess pull = time -> {
        pullTimer += time;
        if (pullTimer < PULL_PERIOD) return;
        pullTimer = 0;
        pullOne();
    };

    boolean pullOne() {
        net.minecraft.world.level.Level world = node.coordonate.world();
        net.minecraft.core.BlockPos above = node.coordonate.pos().above();
        if (!world.isLoaded(above)) return false;
        ResourceHandler<ItemResource> h = world.getCapability(Capabilities.Item.BLOCK, above, net.minecraft.core.Direction.DOWN);
        if (h == null && world.getBlockEntity(above) instanceof Container c) h = VanillaContainerWrapper.of(c);
        if (h == null) return false;
        ItemStack in = inventory.getItem(inSlotId);
        for (int i = 0; i < h.size(); i++) {
            ItemResource r = h.getResource(i);
            if (r.isEmpty() || h.getAmountAsLong(i) <= 0) continue;
            ItemStack one = r.toStack(1);
            if (result(one).isEmpty()) continue;
            if (!in.isEmpty() && (!ItemStack.isSameItemSameComponents(in, one) || in.getCount() >= Math.min(in.getMaxStackSize(), inventory.getMaxStackSize())))
                continue;
            try (Transaction tx = Transaction.openRoot()) {
                if (h.extract(i, r, 1, tx) != 1) continue;
                tx.commit();
            }
            if (in.isEmpty()) inventory.setItem(inSlotId, one);
            else in.grow(1);
            inventory.setChanged();
            return true;
        }
        return false;
    }

    static final double ENERGY_PER_ITEM = mods.eln.misc.RecipesList.electricalFurnaceEnergyPerSmelt;

    /** What the input makes now: food chars above 280 °C, as in the oven. */
    public ItemStack result() {
        return result(inventory.getItem(inSlotId));
    }

    /** What stack in would make now. */
    ItemStack result(ItemStack in) {
        if (Utils.isEmpty(in)) return ItemStack.EMPTY;
        if (mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.chars(thermalLoad.Tc)
            && mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.charrable(in))
            return new ItemStack(mods.eln.registry.ElnRegistry.CHARCOAL_NUGGET.get());
        return Utils.smeltResult(node.coordonate.world(), in);
    }

    boolean canSmelt() {
        ItemStack r = result();
        return !r.isEmpty() && outFor(r) >= 0;
    }

    /** The output slot r goes into: a stack of it with room first, else an empty one; -1 if full. */
    int outFor(ItemStack r) {
        int empty = -1;
        for (int i = outSlotId; i < outSlotId + OUT_SLOTS; i++) {
            ItemStack out = inventory.getItem(i);
            if (out.isEmpty()) {
                if (empty < 0) empty = i;
            } else if (ItemStack.isSameItemSameComponents(out, r) && out.getCount() + r.getCount() <= Math.min(out.getMaxStackSize(), inventory.getMaxStackSize()))
                return i;
        }
        return empty;
    }

    void smeltOne() {
        ItemStack r = result();
        if (r.isEmpty()) return;
        int slot = outFor(r);
        if (slot < 0) return;
        ItemStack out = inventory.getItem(slot);
        if (out.isEmpty()) inventory.setItem(slot, r.copy());
        else out.grow(r.getCount());
        inventory.removeItem(inSlotId, 1);
        inventory.setChanged();
        needPublish();
    }

    /** Progress on the current item, 0..1. */
    public double progress() {
        return smelting ? Math.min(1, energyCounter / ENERGY_PER_ITEM) : 0;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front.left() || side == front.right()) ? electricalLoad : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front.getInverse() ? thermalLoad : null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front.getInverse()) return NodeBase.maskThermal;
        if (side == front.left() || side == front.right()) return NodeBase.maskElectricalPower;
        return 0;
    }

    @Override
    public void initialize() {
        descriptor.applyTo(thermalLoad);
        ElnDeviceRegistry.lowVoltageCableDescriptor.applyTo(electricalLoad);
        control.setR(mods.eln.Eln.LVU * mods.eln.Eln.LVU / SmelterDescriptor.CONTROL_W);
        smeltResistor.highImpedance();
        connect();
    }

    @Override
    public void inventoryChange(Container inventory) {
        super.inventoryChange(inventory);
        needPublish();
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getI());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T:", thermalLoad.Tc) + Utils.plotPower(" smelting:", smelting ? smeltResistor.getP() : 0);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) thermalLoad.Tc);
            stream.writeFloat((float) progress());
            stream.writeFloat((float) (smelting ? smeltResistor.getP() / ENERGY_PER_ITEM : 0));   // (progress a second)
            stream.writeBoolean(electricalLoad.getU() > SmelterDescriptor.CONTROL_MIN * mods.eln.Eln.LVU);
            Utils.serialiseItemStack(stream, Utils.isEmpty(inventory.getItem(inSlotId)) ? null : inventory.getItem(inSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("energyCounter", energyCounter);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        energyCounter = nbt.getDoubleOr("energyCounter", 0);
        lastIn = inventory.getItem(inSlotId).copy();
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.Tc));
        info.put(I18N.tr("Smelting"), smelting ? Utils.plotPower("", smeltResistor.getP()) : I18N.tr("No"));
        return info;
    }
}
