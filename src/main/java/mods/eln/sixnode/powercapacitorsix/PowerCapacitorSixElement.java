package mods.eln.sixnode.powercapacitorsix;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.powerprimitives.PowerCapacitorCore;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The Power Capacitor: PowerCapacitorCore on a six-node, its + pin on the front's right and its - pin on the front's
 * left (the transparent node's + is on its front's left).
 */
public class PowerCapacitorSixElement extends SixNodeElement {

    final SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);
    public final PowerCapacitorCore core;

    public PowerCapacitorSixElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
        super(SixNode, side, descriptor);
        core = new PowerCapacitorCore((PowerCapacitorSixDescriptor) descriptor, inventory, new Failure(this).capacitor());
        core.addTo(electricalLoadList, electricalComponentList, electricalProcessList, slowProcessList);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (lrdu == front.right()) return core.positiveLoad;
        if (lrdu == front.left()) return core.negativeLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front.right()) return NodeBase.maskElectricalPower;
        if (lrdu == front.left()) return NodeBase.maskElectricalPower;
        return 0;
    }

    /** The voltage and the current (the transparent node shows the current alone). */
    @Override
    public String multiMeterString() {
        return core.multiMeterString(true);
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        return core.waila();
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        core.initialize();
    }

    @Override
    public void inventoryChanged() {
        super.inventoryChanged();
        core.setupPhysical();
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        core.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        core.readFromNBT(nbt);
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }
}
