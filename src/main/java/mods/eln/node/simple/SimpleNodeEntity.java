package mods.eln.node.simple;

import mods.eln.misc.Coordonate;
import mods.eln.misc.DescriptorManager;
import mods.eln.misc.Direction;
import mods.eln.node.INodeEntity;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/** The block entity of a {@link SimpleNodeBlock} (1.12 SimpleNodeEntity): forwards world events to its SimpleNode. */
public abstract class SimpleNodeEntity extends BlockEntity implements INodeEntity {

    private SimpleNode node;
    private boolean chunkUnloading = false;

    protected SimpleNodeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public SimpleNode getNode() {
        if (level == null || level.isClientSide()) return null;
        if (node == null || node.isDestructing()) {
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(worldPosition, level));
            node = n instanceof SimpleNode ? (SimpleNode) n : null;
        }
        return node;
    }

    @Override
    public void onChunkUnloaded() {
        chunkUnloading = true;
    }

    @Override
    public void setRemoved() {
        // 1.12 breakBlock -> onBreakBlock: the block is gone (not just unloaded), so its node goes too
        if (!chunkUnloading && level != null && !level.isClientSide()) {
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(worldPosition, level));
            if (n instanceof SimpleNode && !n.isDestructing()) n.onBreakBlock();
        }
        super.setRemoved();
    }

    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (level != null && !level.isClientSide()) {
            SimpleNode n = getNode();
            if (n == null) return false;
            n.onBlockActivated(entityPlayer, side, vx, vy, vz);
        }
        return true;
    }

    public void onNeighborBlockChange() {
        if (level != null && !level.isClientSide()) {
            SimpleNode n = getNode();
            if (n != null) n.onNeighborBlockChange();
        }
    }

    /** Server tick (1.12 ITickable.update); subclasses override. */
    public void update() {
    }

    //***************** Descriptor **************************
    public Object getDescriptor() {
        SimpleNodeBlock b = (SimpleNodeBlock) getBlockState().getBlock();
        return DescriptorManager.get(b.descriptorKey);
    }

    //***************** Network **************************

    /** Client: the node's front, from its publish packet. */
    public Direction front;

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        try {
            front = Direction.fromInt(stream.readByte());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (level == null || level.isClientSide()) return tag;
        SimpleNode n = getNode();
        if (n != null) {
            ByteArrayOutputStream p = n.getPublishPacket();
            if (p != null) tag.putByteArray(NodeBlockEntity.DESCRIPTION_KEY, p.toByteArray());
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        input.read(NodeBlockEntity.DESCRIPTION_KEY, com.mojang.serialization.Codec.BYTE_BUFFER).ifPresent(b -> {
            byte[] a = new byte[b.remaining()];
            b.get(a);
            NodeBlockEntity.clientDescriptionHandler.accept(a);
        });
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
    }

    //*********************** GUI ***************************
    @Override
    public AbstractContainerMenu newContainer(Direction side, Player player) {
        return null;
    }
}
