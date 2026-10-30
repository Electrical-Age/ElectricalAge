package mods.eln.node.six;

import mods.eln.Eln;
import mods.eln.ghost.GhostObserver;
import mods.eln.misc.*;
import mods.eln.node.INodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sim.process.destruct.IBurnable;
import mods.eln.sim.process.destruct.ValueWatchdog;
import mods.eln.sound.IPlayer;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class SixNodeElement implements GhostObserver, IPlayer, INodeElement, IBurnable {
    //private static Class[] idToClass = new Class[256];
    //private static Class[] idToRenderClass = new Class[256];

    public ArrayList<IProcess> slowProcessList = new ArrayList<IProcess>(4);

    public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
    public ArrayList<Component> electricalComponentList = new ArrayList<Component>(4);
    public ArrayList<NbtElectricalLoad> electricalLoadList = new ArrayList<NbtElectricalLoad>(4);

    public ArrayList<IProcess> thermalProcessList = new ArrayList<IProcess>(4);
    public ArrayList<IProcess> thermalSlowProcessList = new ArrayList<IProcess>(4);
    public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
    public ArrayList<NbtThermalLoad> thermalLoadList = new ArrayList<NbtThermalLoad>(4);

    public SixNode sixNode;
    public Direction side;
    /**
     * Where the element sits in its node: slots 0..5 are the six faces, centred (side.getInt()); a voxel block's further
     * slots hold devices placed off centre on a face (offset: the model's centre from the face's centre, world
     * voxels). Packets, menus and saves address an element by its slot.
     */
    public int slot;
    public int ox, oy, oz;
    /** The cable-stub mask of an element in a slot past the six faces (the faces' are in sixNode.lrduElementMask). */
    public final mods.eln.misc.LRDUMask slotMask = new mods.eln.misc.LRDUMask();
    public SixNodeDescriptor sixNodeElementDescriptor;

    public int isProvidingWeakPower() {
        return 0;
    }

    @Override
    public void inventoryChange(Container inventory) {
        inventoryChanged();
    }

    protected void inventoryChanged() {
    }

    public void play(SoundCommand s) {
        s.addUuid(getUuid());
        s.set(sixNode.coordonate);
        s.play();
    }

    public Coordonate getCoordonate() {
        return sixNode.coordonate;
    }

    protected boolean onBlockActivatedRotate(Player entityPlayer) {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.getNextClockwise();
            sixNode.reconnect();
            sixNode.setNeedPublish(true);
            return true;
        }
        return false;
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos) {
        sixNode.sendPacketToAllClient(bos);
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos, double range) {
        sixNode.sendPacketToAllClient(bos, range);
    }

    public void sendPacketToClient(ByteArrayOutputStream bos, ServerPlayer player) {
        sixNode.sendPacketToClient(bos, player);
    }

    public void notifyNeighbor() {
        sixNode.notifyNeighbor();
    }

    /** The ambient where it stands, on the Tc scale (Ambient; stamped on its thermal loads in connectJob). */
    public double ambient;

    public void connectJob() {
        // If we are about to destruct ourselves, do not add any elements to the simulation anymore.
        if (sixNode != null && sixNode.isDestructing()) return;

        Eln.simulator.addAllElectricalComponent(electricalComponentList);
        Eln.simulator.addAllThermalConnection(thermalConnectionList);

        for (NbtElectricalLoad load : electricalLoadList)
            Eln.simulator.addElectricalLoad(load);
        ambient = mods.eln.sim.Ambient.relative(sixNode.coordonate);
        mods.eln.sim.Ambient.stamp(thermalLoadList, ambient);
        for (NbtThermalLoad load : thermalLoadList)
            Eln.simulator.addThermalLoad(load);


        for (IProcess process : slowProcessList)
            Eln.simulator.addSlowProcess(process);
        for (IProcess process : electricalProcessList)
            Eln.simulator.addElectricalProcess(process);
        for (IProcess process : thermalProcessList)
            Eln.simulator.addThermalFastProcess(process);
        for (IProcess process : thermalSlowProcessList)
            Eln.simulator.addThermalSlowProcess(process);

    }

    public void networkUnserialize(DataInputStream stream) {

    }

    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {
        networkUnserialize(stream);
    }

    public int getLightValue() {
        return 0;
    }

    public boolean hasGui() {
        return false;
    }

    public Container getInventory() {
        return null;
    }

    public SixNodeElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        this.sixNode = sixNode;
        this.side = side;
        this.sixNodeElementDescriptor = descriptor;
        this.slot = side.getInt();
        this.itemStackDamageId = descriptor.parentItemDamage;

        if (descriptor.hasGhostGroup())
            Eln.ghostManager.addObserver(this);
    }

    public void preparePacketForClient(DataOutputStream stream) {
        sixNode.preparePacketForClient(stream, this);
    }

    public abstract ElectricalLoad getElectricalLoad(LRDU lrdu);

    public abstract ThermalLoad getThermalLoad(LRDU lrdu);

    public abstract int getConnectionMask(LRDU lrdu);

    public abstract String multiMeterString();

    public abstract String thermoMeterString();

    public LRDU front = LRDU.Up;

    private int itemStackDamageId;

    /** Bit in the header byte of networkSerialize: the element is burnt out. */
    public static final int HEADER_BURNT = 0x40;

    public void networkSerialize(DataOutputStream stream) {

        try {
            stream.writeByte(stubMask().mask + (front.dir << 4) + (isBurnt() ? HEADER_BURNT : 0));
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /** This element's cable-stub mask (by face for slots 0..5, its own past them). */
    public mods.eln.misc.LRDUMask stubMask() {
        return slot < 6 ? sixNode.lrduElementMask.get(side) : slotMask;
    }

    /* public void initializeFromThat(SixNodeDescriptor descriptor)
     {
         this.descriptor = descriptor;
         initialize();
     }*/
    public abstract void initialize();


    @Override
    public void stop(int uuid) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.packetDestroyUuid);
            stream.writeInt(uuid);

            sendPacketToAllClient(bos);
        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    public void destroy(ServerPlayer entityPlayer) {
        if (useUuid()) {
            stop(uuid);
        }

        if (sixNodeElementDescriptor.hasGhostGroup()) {
            Eln.ghostManager.removeObserver(sixNode.coordonate);
            sixNodeElementDescriptor.getGhostGroup(side, front).erase(sixNode.coordonate);
        }

        sixNode.dropInventory(getInventory());
        //	getCoordonate().world().getWorldInfo().
        // A burnt device is slag: its contents drop, the device itself doesn't.
        if (Utils.mustDropItem(entityPlayer) && !isBurnt())
            sixNode.dropItem(getDropItemStack());
    }

    /**
     * Called when a player right-clicks the SixNode.
     * @param entityPlayer Player.
     * @param side Something to do with player viewpoint?
     * @param vx Ditto?
     * @param vy ?
     * @param vz ?
     * @return True if we've done something, otherwise false.
     */
    public boolean onBlockActivated(Player entityPlayer, Direction side,
                                             float vx, float vy, float vz) {
        return onBlockActivatedRotate(entityPlayer);
    }

	/*
    public void onBreakElement()
	{
		sixNode.dropInventory(getInventory());
		
	}*/

    public ItemStack getDropItemStack() {
        return sixNodeElementDescriptor.newItemStack();
    }

    public void readFromNBT(CompoundTag nbt) {
        front = front.readFromNBT(nbt, "sixFront");
        burnt = nbt.contains("burnt") ? FailureCause.fromOrdinal(nbt.getByteOr("burnt", (byte) 0) - 1) : null;
        burntKind = burnt != null ? Failure.Kind.fromOrdinal(nbt.getByteOr("burntKind", (byte) 0)) : null;

        Container inv = getInventory();
        if (inv != null) {
            Utils.readFromNBT(nbt, "inv", inv);
        }

        INBTTReady.readAll(nbt, electricalLoadList, thermalLoadList, electricalComponentList,
            slowProcessList, electricalProcessList, thermalProcessList, thermalSlowProcessList);

    }

    public void writeToNBT(CompoundTag nbt) {
        front.writeToNBT(nbt, "sixFront");
        if (burnt != null) {
            nbt.putByte("burnt", (byte) (burnt.ordinal() + 1));
            nbt.putByte("burntKind", (byte) burntKind.ordinal());
        }

        Container inv = getInventory();
        if (inv != null) {
            Utils.writeToNBT(nbt, "inv", inv);
        }

        INBTTReady.writeAll(nbt, electricalLoadList, thermalLoadList, electricalComponentList,
            slowProcessList, electricalProcessList, thermalProcessList, thermalSlowProcessList);

    }

    public void reconnect() {
        sixNode.reconnect();
    }

    public void needPublish() {
        sixNode.setNeedPublish(true);
    }

    public void disconnectJob() {
        Eln.simulator.removeAllElectricalComponent(electricalComponentList);
        Eln.simulator.removeAllThermalConnection(thermalConnectionList);

        for (NbtElectricalLoad load : electricalLoadList)
            Eln.simulator.removeElectricalLoad(load);
        for (NbtThermalLoad load : thermalLoadList)
            Eln.simulator.removeThermalLoad(load);

        for (IProcess process : slowProcessList)
            Eln.simulator.removeSlowProcess(process);
        for (IProcess process : electricalProcessList)
            Eln.simulator.removeElectricalProcess(process);
        for (IProcess process : thermalProcessList)
            Eln.simulator.removeThermalFastProcess(process);
        for (IProcess process : thermalSlowProcessList)
            Eln.simulator.removeThermalSlowProcess(process);

    }

    public boolean canConnectRedstone() {

        return false;
    }

    public Coordonate getGhostObserverCoordonate() {
        return sixNode.coordonate;

    }

    public void ghostDestroyed(int UUID) {
        if (UUID == sixNodeElementDescriptor.getGhostGroupUuid()) {
            selfDestroy();
        }
    }

    public boolean ghostBlockActivated(int UUID, Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (UUID == sixNodeElementDescriptor.getGhostGroupUuid()) {
            sixNode.onSlotActivated(entityPlayer, slot, vx, vy, vz);
        }
        return false;
    }

    private void selfDestroy() {
        sixNode.deleteSlot(null, slot);
    }

    private int uuid = 0;

    public int getUuid() {
        if (uuid == 0) {
            uuid = Utils.getUuid();
        }
        return uuid;
    }

    public boolean useUuid() {
        return uuid != 0;
    }

    public void globalBoot() {

    }

    public void unload() {

    }

    public boolean playerAskToBreak() {
        return true;
    }

    /** Set when the device burnt out (see Failure); null while it works. */
    @Nullable
    private FailureCause burnt;
    @Nullable
    private Failure.Kind burntKind;

    @Override
    @Nullable
    public Failure.Kind getBurntKind() {
        return burntKind;
    }

    @Override
    @Nullable
    public FailureCause getBurnt() {
        return burnt;
    }

    @Override
    public Coordonate getBurnableCoordonate() {
        return getCoordonate();
    }

    /** Burn out: stay in the world but leave the simulation (SixNode skips burnt elements) until repaired. */
    @Override
    public void burn(FailureCause cause, Failure.Kind kind) {
        if (burnt != null) return;
        sixNode.disconnect();
        burnt = cause;
        burntKind = kind;
        sixNode.connect();
        needPublish();
    }

    @Override
    public void repair() {
        if (burnt == null) return;
        sixNode.disconnect();
        burnt = null;
        burntKind = null;
        for (NbtThermalLoad load : thermalLoadList) load.Tc = load.ambient;
        for (IProcess p : slowProcessList) if (p instanceof ValueWatchdog) ((ValueWatchdog) p).reset();
        for (IProcess p : thermalSlowProcessList) if (p instanceof ValueWatchdog) ((ValueWatchdog) p).reset();
        for (IProcess p : electricalProcessList) if (p instanceof ValueWatchdog) ((ValueWatchdog) p).reset();
        sixNode.connect();
        needPublish();
    }

    @Nullable
    public Map<String, String> getWaila() {
        Map<String, String> wailaList = new HashMap<String, String>();
        String info = multiMeterString();
        if (info != null) wailaList.put("Info", info);   // (Baughn: Jade showed "null" for a resin collector)
        return wailaList;
    }

    /**
     * Several terminals on one edge (board #45: the three-channel data logger's inputs): in a voxel block, each is a
     * connection point of its own on that edge, offset voxels from the edge's middle toward the world side
     * side.applyLRDU(lrdu.left()); an edge with any of these has no middle point. Empty: one point an edge, as usual.
     */
    public java.util.List<EdgePin> edgePins() {
        return java.util.List.of();
    }

    /** A terminal on an edge (edgePins): the edge, where along it, the mask and the load it connects. */
    public record EdgePin(mods.eln.misc.LRDU lrdu, int offset, int mask, mods.eln.sim.ElectricalLoad load) {
    }
}
