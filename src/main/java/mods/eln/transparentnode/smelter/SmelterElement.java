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
    public static final int inSlotId = 0, outSlotId = 1;
    final TransparentNodeElementInventory inventory = new SmelterInventory(2, 64, this);

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

    static final double ENERGY_PER_ITEM = mods.eln.misc.RecipesList.electricalFurnaceEnergyPerSmelt;

    /** What the input makes now: food chars above 280 °C, as in the oven. */
    public ItemStack result() {
        ItemStack in = inventory.getItem(inSlotId);
        if (Utils.isEmpty(in)) return ItemStack.EMPTY;
        if (mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.chars(thermalLoad.Tc)
            && mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.charrable(in))
            return new ItemStack(mods.eln.registry.ElnRegistry.CHARCOAL_NUGGET.get());
        return Utils.smeltResult(node.coordonate.world(), in);
    }

    boolean canSmelt() {
        ItemStack r = result();
        if (r.isEmpty()) return false;
        ItemStack out = inventory.getItem(outSlotId);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(out, r) && out.getCount() + r.getCount() <= Math.min(out.getMaxStackSize(), inventory.getMaxStackSize());
    }

    void smeltOne() {
        ItemStack r = result();
        if (r.isEmpty() || !canSmelt()) return;
        ItemStack out = inventory.getItem(outSlotId);
        if (out.isEmpty()) inventory.setItem(outSlotId, r.copy());
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
