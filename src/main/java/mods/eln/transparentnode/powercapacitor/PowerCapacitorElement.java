package mods.eln.transparentnode.powercapacitor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.powerprimitives.PowerCapacitorCore;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

/**
 * The full-block Power Capacitor: PowerCapacitorCore on a transparent node, its + pin on the front's left and its -
 * pin on the front's right, both on the floor (the six-node's + is on its front's right). Jade shows the base
 * class's line, the multimeter's (the six-node lists capacity and charge).
 */
public class PowerCapacitorElement extends TransparentNodeElement {

    final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);
    public final PowerCapacitorCore core;

    public PowerCapacitorElement(TransparentNode transparentNode,
                                 TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        core = new PowerCapacitorCore((PowerCapacitorDescriptor) descriptor, inventory, new Failure(this).capacitor());
        core.addTo(electricalLoadList, electricalComponentList, electricalProcessList, slowProcessList);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front.left()) return core.positiveLoad;
        if (side == front.right()) return core.negativeLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front.left()) return node.maskElectricalPower;
        if (side == front.right()) return node.maskElectricalPower;
        return 0;
    }

    /** The current alone (the six-node shows the voltage too). */
    @Override
    public String multiMeterString(Direction side) {
        return core.multiMeterString(false);
    }

    @Override
    public java.util.Map<String, String> getWaila() {
        return core.waila();
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        core.initialize();
        connect();   // (the six-node element doesn't: its node connects it)
    }

    @Override
    public void inventoryChange(Container inventory) {
        super.inventoryChange(inventory);
        core.setupPhysical();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side,
                                    float vx, float vy, float vz) {
        return false;
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
