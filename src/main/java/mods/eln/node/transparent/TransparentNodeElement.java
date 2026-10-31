package mods.eln.node.transparent;

import mods.eln.Eln;
import mods.eln.ghost.GhostObserver;
import mods.eln.misc.*;
import mods.eln.node.INodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sim.process.destruct.IBurnable;
import mods.eln.sim.process.destruct.ValueWatchdog;
import mods.eln.sound.IPlayer;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class TransparentNodeElement implements GhostObserver, IPlayer, INodeElement, IBurnable {

    public ArrayList<IProcess> slowProcessList = new ArrayList<IProcess>(4);

    public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
    public ArrayList<Component> electricalComponentList = new ArrayList<Component>(4);
    public ArrayList<State> electricalLoadList = new ArrayList<State>(4);

    public ArrayList<IProcess> thermalFastProcessList = new ArrayList<IProcess>(4);
    public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
    public ArrayList<NbtThermalLoad> thermalLoadList = new ArrayList<NbtThermalLoad>(4);


    public static final byte unserializeGroundedId = -127;
    public static final byte unserializeNulldId = -128;
    public TransparentNodeDescriptor transparentNodeDescriptor;

    public TransparentNodeDescriptor getDescriptor() {
        return transparentNodeDescriptor;
    }

    protected void serialiseItemStack(DataOutputStream stream, ItemStack stack) throws IOException {
        Utils.serialiseItemStack(stream, stack);
    }

    public void connectJob() {
        // If we are about to destruct ourselves, do not add any elements to the simulation anymore.
        if (node != null && node.isDestructing()) return;
        
        Eln.simulator.addAllSlowProcess(slowProcessList);

        Eln.simulator.addAllElectricalComponent(electricalComponentList);
        for (State load : electricalLoadList) Eln.simulator.addElectricalLoad(load);
        Eln.simulator.addAllElectricalProcess(electricalProcessList);

        Eln.simulator.addAllThermalConnection(thermalConnectionList);
        ambient = mods.eln.sim.Ambient.relative(node.coordonate);
        mods.eln.sim.Ambient.stamp(thermalLoadList, ambient);
        for (NbtThermalLoad load : thermalLoadList) Eln.simulator.addThermalLoad(load);
        Eln.simulator.addAllThermalFastProcess(thermalFastProcessList);
    }

    /** The ambient where it stands, on the Tc scale (Ambient; stamped on its thermal loads in connectJob). */
    public double ambient;

    public void disconnectJob() {
        Eln.simulator.removeAllSlowProcess(slowProcessList);

        Eln.simulator.removeAllElectricalComponent(electricalComponentList);
        for (State load : electricalLoadList) Eln.simulator.removeElectricalLoad(load);
        Eln.simulator.removeAllElectricalProcess(electricalProcessList);

        Eln.simulator.removeAllThermalConnection(thermalConnectionList);
        for (NbtThermalLoad load : thermalLoadList) Eln.simulator.removeThermalLoad(load);
        Eln.simulator.removeAllThermalFastProcess(thermalFastProcessList);
    }

    public TransparentNode node;
    public Direction front;
    public boolean grounded = true;


    public void onGroundedChangedByClient() {
        needPublish();
    }

    public byte networkUnserialize(DataInputStream stream, ServerPlayer player) {
        return networkUnserialize(stream);
    }

    public byte networkUnserialize(DataInputStream stream) {
        byte readed;
        try {
            switch (readed = stream.readByte()) {
                case unserializeGroundedId:
                    grounded = stream.readByte() != 0 ? true : false;
                    onGroundedChangedByClient();
                    return unserializeNulldId;
                default:
                    return readed;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return unserializeNulldId;
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

    public void preparePacketForClient(DataOutputStream stream) {
        node.preparePacketForClient(stream);
    }

    public void sendIdToAllClient(byte id) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(id);
        } catch (IOException e) {

            e.printStackTrace();
        }

        sendPacketToAllClient(bos);
    }


    public void sendStringToAllClient(byte id, String str) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(id);
            packet.writeUTF(str);
        } catch (IOException e) {

            e.printStackTrace();
        }

        sendPacketToAllClient(bos);
    }


    private void sendPacketToAllClient(ByteArrayOutputStream bos) {
        node.sendPacketToAllClient(bos);
    }

    public TransparentNodeElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        this.node = transparentNode;
        this.transparentNodeDescriptor = descriptor;
        if (descriptor.hasGhostGroup()) Eln.ghostManager.addObserver(this);
    }

    /** The element's fluid handler (26.x fluid capability), null if none; fluid devices come with their batch. */
    public Object getFluidHandler() {
        return null;
    }

    /**
     * The element's items as seen from a block face (26.x item capability: hoppers, pipes): the inventory's own sided
     * rules (getAccessibleSlotsFromSide / canInsertItem / canExtractItem), null where it exposes no slot on that face,
     * for the null side, and while burnt. Plain TransparentNodeElementInventory exposes nothing, so devices whose
     * slots are upgrades (cores, cables, regulators...) stay closed. WorldlyContainerWrapper defers setChanged (and
     * so inventoryChange) to the commit of the outermost transaction.
     */
    @Nullable
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getItemHandler(
        @Nullable net.minecraft.core.Direction side) {
        if (side == null || isBurnt()) return null;
        if (!(getInventory() instanceof net.minecraft.world.WorldlyContainer wc) || wc.getSlotsForFace(side).length == 0) return null;
        return new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(wc, side);
    }


    /**
     * The element's Forge Energy handler on a block face (26.x energy capability), null if none; TransparentNode
     * hides it while burnt.
     */
    @Nullable
    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler(@Nullable net.minecraft.core.Direction side) {
        return null;
    }


    public void onNeighborBlockChange() {
        checkCanStay(false);
    }


    public void checkCanStay(boolean onCreate) {
        Block block;
        boolean needDestroy = false;
        if (transparentNodeDescriptor.mustHaveFloor()) {
            if (!node.isBlockOpaque(Direction.YN)) {
                // (devices that may stand on more than solid blocks: a boiler on a turbine)
                var below = node.coordonate.world().getBlockState(node.coordonate.pos().below()).getBlock();
                if (!transparentNodeDescriptor.isFloor(below)) needDestroy = true;
            }
        }
        if (transparentNodeDescriptor.mustHaveCeiling()) {
            if (!node.isBlockOpaque(Direction.YP)) needDestroy = true;
        }
        if (transparentNodeDescriptor.mustHaveWallFrontInverse()) {
            if (!node.isBlockOpaque(front.getInverse())) needDestroy = true;
        }
        if (transparentNodeDescriptor.mustHaveWall()) {
            boolean wall = false;

            if (node.isBlockOpaque(Direction.XN)) wall = true;
            if (node.isBlockOpaque(Direction.XP)) wall = true;
            if (node.isBlockOpaque(Direction.ZN)) wall = true;
            if (node.isBlockOpaque(Direction.ZP)) wall = true;

            if (!wall) needDestroy = true;
        }

        if (needDestroy) {
            selfDestroy();
        }
    }

    /**
     * Lost its support (or a ghost of its group): broken like a block, with the break effects, and dropped with its
     * contents by the block entity's removal (onBreakElement). Baughn: devices vanished instead of dropping, because
     * physicalSelfDestruction (the failure path) drops nothing.
     */
    public void selfDestroy() {
        if (node.isDestructing()) return;
        node.coordonate.world().destroyBlock(node.coordonate.pos(), false);
    }
    /*
	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	*/

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

    public void onBreakElement() {
        if (useUuid()) stop(uuid);

        if (transparentNodeDescriptor.hasGhostGroup()) {
            Eln.ghostManager.removeObserver(node.coordonate);
            Eln.ghostManager.removeGhostAndBlockWithObserver(node.coordonate);
            //transparentNodeDescriptor.getGhostGroup(front).erase(node.coordonate);
        }
        node.dropInventory(getInventory());
        node.dropElement(node.removedByPlayer);

    }

    public ItemStack getDropItemStack() {
        ItemStack itemStack = transparentNodeDescriptor.newItemStack();
        mods.eln.generic.ItemNbt.set(itemStack, getItemStackNBT());
        return itemStack;
    }

    public CompoundTag getItemStackNBT() {
        return null;
    }


    public abstract ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu);

    public abstract ThermalLoad getThermalLoad(Direction side, LRDU lrdu);

    public abstract int getConnectionMask(Direction side, LRDU lrdu);


    public abstract String multiMeterString(Direction side);

    public abstract String thermoMeterString(Direction side);


    /** Bit in the header byte of networkSerialize: the element is burnt out. */
    public static final int HEADER_BURNT = 0x10;

    public void networkSerialize(DataOutputStream stream) {
        try {
            stream.writeByte(front.getInt() + (grounded ? 8 : 0) + (isBurnt() ? HEADER_BURNT : 0));
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    public void initializeFromThat(Direction front, LivingEntity entityLiving, CompoundTag itemStackNbt) {
        this.front = front;
        readItemStackNBT(itemStackNbt);
        initialize();
    }

    public abstract void initialize();

    public void readItemStackNBT(CompoundTag nbt) {

    }


    //  public abstract void destroyFrom(SixNode sixNode);

    public abstract boolean onBlockActivated(Player entityPlayer, Direction side,
                                             float vx, float vy, float vz);

    /**
     * The wrench key and a click turn the device a quarter turn, where it may turn (Baughn 2026-10-31: placing a
     * machine had turned its neighbours, and nothing turned them back). True when it turned; most devices don't.
     */
    public boolean wrenchTurn(Player player) {
        return false;
    }


    public void readFromNBT(CompoundTag nbt) {
        Container inv = getInventory();
        if (inv != null) {
            Utils.readFromNBT(nbt, "inv", inv);
        }

        INBTTReady.readAll(nbt, electricalLoadList, thermalLoadList, electricalComponentList,
            slowProcessList, electricalProcessList, thermalFastProcessList);

        byte b = nbt.getByteOr("others", (byte) 0);
        front = Direction.fromInt(b & 0x7);
        grounded = (b & 8) != 0;
        burnt = nbt.contains("burnt") ? FailureCause.fromOrdinal(nbt.getByteOr("burnt", (byte) 0) - 1) : null;
        burntKind = burnt != null ? Failure.Kind.fromOrdinal(nbt.getByteOr("burntKind", (byte) 0)) : null;
    }


    public void writeToNBT(CompoundTag nbt) {
        if (burnt != null) {
            nbt.putByte("burnt", (byte) (burnt.ordinal() + 1));
            nbt.putByte("burntKind", (byte) burntKind.ordinal());
        }

        Container inv = getInventory();
        if (inv != null) {
            Utils.writeToNBT(nbt, "inv", inv);
        }

        INBTTReady.writeAll(nbt, electricalLoadList, thermalLoadList, electricalComponentList,
            slowProcessList, electricalProcessList, thermalFastProcessList);


        nbt.putByte("others", (byte) (front.getInt() + (grounded ? 8 : 0)));
    }

    public void reconnect() {
        node.reconnect();
    }

    public void needPublish() {
        node.setNeedPublish(true);
    }


    public void connect() {
        node.connect();
    }

    public void disconnect() {
        node.disconnect();
    }

    @Override
    public void inventoryChange(Container inventory) {
    }

    public float getLightOpacity() {

        return 0f;
    }

    public Coordonate getGhostObserverCoordonate() {
        return node.coordonate;

    }

    public void ghostDestroyed(int UUID) {
        if (UUID == transparentNodeDescriptor.getGhostGroupUuid()) {
            selfDestroy();
        }
    }

    public boolean ghostBlockActivated(int UUID, Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (UUID == transparentNodeDescriptor.getGhostGroupUuid()) {
            return node.onBlockActivated(entityPlayer, side, vx, vy, vz);
        }
        return false;
    }


    public Level world() {

        return node.coordonate.world();
    }

    public Coordonate coordonate() {
        return node.coordonate;
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

    public void play(SoundCommand s) {
        s.addUuid(getUuid());
        s.set(node.coordonate);
        s.play();
    }

    public void unload() {

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
        return coordonate();
    }

    /** Burn out: stay in the world but leave the simulation (TransparentNode skips burnt elements) until repaired. */
    @Override
    public void burn(FailureCause cause, Failure.Kind kind) {
        if (burnt != null) return;
        node.disconnect();
        burnt = cause;
        burntKind = kind;
        node.connect();
        needPublish();
        node.invalidateCapabilities();
    }

    @Override
    public void repair() {
        if (burnt == null) return;
        node.disconnect();
        burnt = null;
        burntKind = null;
        for (NbtThermalLoad load : thermalLoadList) load.Tc = load.ambient;
        for (IProcess p : slowProcessList) if (p instanceof ValueWatchdog) ((ValueWatchdog) p).reset();
        for (IProcess p : electricalProcessList) if (p instanceof ValueWatchdog) ((ValueWatchdog) p).reset();
        for (IProcess p : thermalFastProcessList) if (p instanceof ValueWatchdog) ((ValueWatchdog) p).reset();
        node.connect();
        needPublish();
        node.invalidateCapabilities();
    }

    public Map<String, String> getWaila() {
        Map<String, String> wailaList = new HashMap<String, String>();
        wailaList.put("Info", multiMeterString(this.front));
        return wailaList;
    }
}
