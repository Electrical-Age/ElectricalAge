package mods.eln.node.transparent;

import mods.eln.sim.process.destruct.Repair;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TransparentNode extends Node {

    public TransparentNodeElement element;
    public int elementId;
    public ServerPlayer removedByPlayer;

    @Override
    public boolean nodeAutoSave() {

        return false;
    }

    @Override
    public void onNeighborBlockChange() {
        super.onNeighborBlockChange();
        element.onNeighborBlockChange();
    }

    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt.getCompoundOrEmpty("node"));

        elementId = nbt.getShortOr("eid", (short) 0);
        // No device of this id in this version (removed or renamed), or it can't be built: throw, so that
        // NodeManager keeps the saved tag as an orphan instead of holding a node with no element.
        TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(elementId);
        if (descriptor == null) throw new IllegalStateException("no transparent node device with id " + elementId);
        try {
            element = (TransparentNodeElement) descriptor.ElementClass.getConstructor(TransparentNode.class, TransparentNodeDescriptor.class).newInstance(this, descriptor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot build transparent node device " + elementId, e);
        }
        element.readFromNBT(nbt.getCompoundOrEmpty("element"));

    }

    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(Utils.newNbtTagCompund(nbt, "node"));

        nbt.putShort("eid", (short) elementId);

        element.writeToNBT(Utils.newNbtTagCompund(nbt, "element"));

    }

    @Override
    public void onBreakBlock() {

        element.onBreakElement();
        super.onBreakBlock();
        invalidateCapabilities();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (element.isBurnt()) return null;
        return element.getElectricalLoad(side, lrdu);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        if (element.isBurnt()) return null;
        return element.getThermalLoad(side, lrdu);
    }

    @Override
    public int getSideConnectionMask(Direction side, LRDU lrdu) {
        return element.getConnectionMask(side, lrdu);
    }

    @Override
    public String multiMeterString(Direction side) {
        return element.multiMeterString(side);
    }

    @Override
    public String thermoMeterString(Direction side) {
        return element.thermoMeterString(side);
    }

    /** Item handler of the element on a face (26.x item capability), null if none or burnt. */
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getItemHandler(
        net.minecraft.core.Direction side) {
        if (element == null || element.isBurnt()) return null;
        return element.getItemHandler(side);
    }

    /** Forge Energy handler of the element on a face (26.x energy capability), null if none or burnt. */
    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler(net.minecraft.core.Direction side) {
        if (element == null || element.isBurnt()) return null;
        return element.getEnergyHandler(side);
    }

    /**
     * Tells capability caches (hoppers, pipes) to look again: the element bound, burnt out, was repaired or is gone.
     */
    public void invalidateCapabilities() {
        if (coordonate != null && coordonate.world() instanceof net.minecraft.server.level.ServerLevel l)
            l.invalidateCapabilities(coordonate.pos());
    }

    /** Fluid handler of the element (26.x fluid capability; fluid devices are ported with their batch). */
    public Object getFluidHandler() {
        return element.getFluidHandler();
    }

    @Override
    public void publishSerialize(DataOutputStream stream) {

        super.publishSerialize(stream);

        try {
            stream.writeShort(this.elementId);
            element.networkSerialize(stream);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public enum FrontType {
        BlockSide, PlayerView, PlayerViewHorizontal, BlockSideInv
    }

    ;

    @Override
    public void initializeFromThat(Direction side, LivingEntity entityLiving, ItemStack itemStack) {
        try {
            // Direction front = null;
            TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(itemStack);
            /*
			 * switch(descriptor.getFrontType()) { case BlockSide: front = side; break; case PlayerView: front = Utils.entityLivingViewDirection(entityLiving).getInverse(); break; case PlayerViewHorizontal: front = Utils.entityLivingHorizontalViewDirection(entityLiving).getInverse(); break;
			 * 
			 * }
			 */

            elementId = descriptor.parentItemDamage; // the 1.12 id, as in node NBT and client packets
            element = (TransparentNodeElement) descriptor.ElementClass.getConstructor(TransparentNode.class, TransparentNodeDescriptor.class).newInstance(this, descriptor);
            element.initializeFromThat(side, entityLiving, mods.eln.generic.ItemNbt.get(itemStack));
            invalidateCapabilities();
        } catch (InstantiationException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        } catch (NoSuchMethodException e) {

            e.printStackTrace();
        } catch (SecurityException e) {

            e.printStackTrace();
        }

    }

    @Override
    public void initializeFromNBT() {
        element.initialize();
        invalidateCapabilities();
    }

    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (element.isBurnt() && Repair.onActivated(element, entityPlayer)) return true;
        if (element.onBlockActivated(entityPlayer, side, vx, vy, vz)) return true;
        if (mods.eln.fluid.FluidBuckets.use(entityPlayer, element.getFluidHandler())) return true;   // 26.x: buckets in and out
        return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
    }

    @Override
    public boolean hasGui(Direction side) {
        if (element == null) return false;
        return element.hasGui();
    }

    public Container getInventory(Direction side) {
        if (element == null) return null;
        return element.getInventory();
    }

    @Override
    public int getBlockMetadata() {
        return element.transparentNodeDescriptor.tileEntityMetaTag.meta;
    }

    @Override
    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {
        super.networkUnserialize(stream, player);

        Direction side;
        try {
            if (elementId == stream.readShort()) {
                element.networkUnserialize(stream, player);
            } else {
                Utils.println("Transparent node unserialize miss");
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void connectJob() {
        super.connectJob();
        // A burnt element stays out of the simulation (see TransparentNodeElement.burn).
        if (!element.isBurnt()) element.connectJob();
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        if (!element.isBurnt()) element.disconnectJob();
    }

    @Override
    public void checkCanStay(boolean onCreate) {

        super.checkCanStay(onCreate);
        element.checkCanStay(onCreate);
    }

    public void dropElement(ServerPlayer entityPlayer) {
        // A burnt device is slag: its contents drop, the device itself doesn't.
        if (element != null && !element.isBurnt())
            if (Utils.mustDropItem(entityPlayer))
                dropItem(element.getDropItemStack());
    }

    @Override
    public String getNodeUuid() {

        return "t";
    }

    @Override
    public void unload() {
        super.unload();
        if (element != null)
            element.unload();
        invalidateCapabilities();

    }

}
