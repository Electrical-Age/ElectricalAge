package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Dimensions;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * The in-world anchor of a node (1.12 NodeBlockEntity). Server: finds its node in NodeManager by position; the node
 * itself is saved by ElnWorldSaves, not here. Client: receives the node's publish bytes (EA protocol) and keeps what
 * the renderer needs. Removing the block removes the node (setRemoved, except on chunk unload, where nodes stay).
 */
public abstract class NodeBlockEntity extends BlockEntity implements INodeEntity {
    public static final String DESCRIPTION_KEY = "elnDescription";

    /** Client: hands the description bytes to the client packet handler (set by the client setup, P3). */
    public static Consumer<byte[]> clientDescriptionHandler = b -> {
    };

    boolean redstone = false;
    int lastLight = 0xFF;
    boolean firstUnserialize = true;
    private boolean chunkUnloading = false;

    Node node = null;

    protected NodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---- client: publish bytes ----

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        int light = 0;
        try {
            if (firstUnserialize) {
                firstUnserialize = false;
                Utils.notifyNeighbours(level, worldPosition);
            }
            byte b = stream.readByte();
            light = b & 0xF;
            boolean newRedstone = (b & 0x10) != 0;
            if (redstone != newRedstone) {
                redstone = newRedstone;
                Utils.notifyNeighbours(level, worldPosition);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (lastLight != light) {
            lastLight = light;
            if (level != null) level.getLightEngine().checkBlock(worldPosition);
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
    }

    public abstract int isProvidingWeakPower(Direction side);

    public boolean canConnectRedstone(Direction side) {
        return redstone;
    }

    @Override
    public AbstractContainerMenu newContainer(Direction side, Player player) {
        return null;
    }

    public boolean cameraDrawOptimisation() {
        return true;
    }

    /** Light the block emits: client from the last publish, server from the node. */
    public int getLightValue() {
        if (level == null) return 0;
        if (level.isClientSide()) return lastLight == 0xFF ? 0 : lastLight;
        Node n = getNode();
        return n == null ? 0 : n.getLightValue();
    }

    // ---- server: node lifecycle ----

    void onBlockPlacedBy(Direction front, LivingEntity entityLiving, int metadata) {
    }

    public void onBlockAdded() {
        if (level != null && !level.isClientSide() && getNode() == null) {
            level.removeBlock(worldPosition, false);
        }
    }

    public void onBreakBlock() {
        if (level != null && !level.isClientSide()) {
            Node n = getNode();
            if (n != null) n.onBreakBlock();
        }
    }

    @Override
    public void onChunkUnloaded() {
        chunkUnloading = true;
    }

    @Override
    public void setRemoved() {
        // The block is gone (broken, replaced, cleared by a GameTest...), not just unloaded: remove the node, so it
        // can't stay in the simulation and connect to whatever is built here next.
        if (!chunkUnloading && level != null && !level.isClientSide()) {
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(worldPosition, level));
            if (n != null && !n.isDestructing()) n.onBreakBlock();
        }
        super.setRemoved();
    }

    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (level != null && !level.isClientSide()) {
            Node n = getNode();
            if (n == null) return false;
            n.onBlockActivated(entityPlayer, side, vx, vy, vz);
        }
        return true;
    }

    public void onNeighborBlockChange() {
        if (level != null && !level.isClientSide()) {
            Node n = getNode();
            if (n != null) n.onNeighborBlockChange();
        }
    }

    /** Selftest reload: drop the cached node so the next getNode() looks it up again. */
    public void forgetNode() {
        node = null;
    }

    @Nullable
    public Node getNode() {
        if (level == null || level.isClientSide()) return null;
        if (node == null || node.isDestructing()) {
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(worldPosition, level));
            node = n instanceof Node ? (Node) n : null;
        }
        return node;
    }

    // ---- client sync: the publish packet rides in the update tag (1.12: same) ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (level == null || level.isClientSide()) return tag;
        Node n = getNode();
        if (n != null) {
            ByteArrayOutputStream p = n.getPublishPacket();
            if (p != null) tag.putByteArray(DESCRIPTION_KEY, p.toByteArray());
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        input.read(DESCRIPTION_KEY, com.mojang.serialization.Codec.BYTE_BUFFER).ifPresent(b -> {
            byte[] a = new byte[b.remaining()];
            b.get(a);
            clientDescriptionHandler.accept(a);
        });
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
    }

    public void preparePacketForServer(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.packetPublishForNode);
            stream.writeInt(worldPosition.getX());
            stream.writeInt(worldPosition.getY());
            stream.writeInt(worldPosition.getZ());
            stream.writeInt(Dimensions.id(level));
            stream.writeUTF(getNodeUuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        mods.eln.network.ElnNet.sendToServer(bos);
    }

    public int getCableDry(Direction side, LRDU lrdu) {
        return 0;
    }
}
