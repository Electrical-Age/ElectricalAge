package mods.eln.sixnode.powerinductorsix;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.powerprimitives.PowerInductorCore;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The Power Inductor: PowerInductorCore on a six-node, its + pin on the front's right and its - pin on the front's
 * left (the transparent node's + is on its front's left).
 */
public class PowerInductorSixElement extends SixNodeElement {

    final SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);
    public final PowerInductorCore core;

    public PowerInductorSixElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
        super(SixNode, side, descriptor);
        core = new PowerInductorCore((PowerInductorSixDescriptor) descriptor, inventory);
        core.addTo(electricalLoadList, electricalComponentList);
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
        core.setupPhysical();
    }

    @Override
    public void inventoryChanged() {
        super.inventoryChanged();
        core.setupPhysical();
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        core.readFromNBT();
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
