package mods.eln.node.voxel;

import mods.eln.misc.Direction;
import mods.eln.node.NodeBlockEntity;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Block entity of a voxel node. Server: everything goes through the VoxelNode. Client: the parts, from the publish
 * packet, for the shape and the renderer.
 */
public class VoxelNodeEntity extends mods.eln.node.six.SixNodeEntity {
    /** Model data: the parts, for the block model (client/render/VoxelModel, baked into the chunk mesh). */
    public static final net.neoforged.neoforge.model.data.ModelProperty<List<VoxelPart>> PARTS = new net.neoforged.neoforge.model.data.ModelProperty<>();

    @Override
    public net.neoforged.neoforge.model.data.ModelData getModelData() {
        return net.neoforged.neoforge.model.data.ModelData.of(PARTS, clientParts);
    }

    /** Client: the parts as last published. */
    public volatile List<VoxelPart> clientParts = List.of();

    public VoxelNodeEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.VOXEL_NODE_ENTITY.get(), pos, state);
    }

    @Override
    public String getNodeUuid() {
        return VoxelNode.UUID;
    }

    /** The parts: server from the node, client from the last publish. */
    public List<VoxelPart> parts() {
        if (level != null && !level.isClientSide()) return getNode() instanceof VoxelNode v ? v.parts : List.of();
        return clientParts;
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        // re-mesh the chunk section if the parts changed (elements publish often: meters, lamps)
        if (!partsChanged) return;
        partsChanged = false;
        requestModelDataUpdate();
        if (level != null && level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_IMMEDIATE);
    }

    @Override
    protected void readBeforeElements(DataInputStream stream) throws IOException {
        {
            int n = stream.readShort();
            int hash = n;
            List<VoxelPart> ps = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String kind = stream.readUTF();
                byte[] b = new byte[stream.readShort()];
                stream.readFully(b);
                hash = hash * 31 + kind.hashCode() * 17 + java.util.Arrays.hashCode(b);
                VoxelPart p = VoxelPart.deserialize(kind, new DataInputStream(new java.io.ByteArrayInputStream(b)));
                if (p != null) ps.add(p);
            }
            if (hash != partsHash || clientParts.size() != ps.size()) {
                partsHash = hash;
                partsChanged = true;
                clientParts = List.copyOf(ps);
            }
        }
    }

    /** An element's stub toward a voxel cable here (CableRender asks): the cable at EA's (side, edge) slot. */
    @Override
    public mods.eln.cable.CableRenderDescriptor getCableRender(Direction side, mods.eln.misc.LRDU lrdu) {
        var r = super.getCableRender(side, lrdu);
        if (r != null) return r;
        CablePart c = cableAt(side, lrdu);
        if (c == null) return null;
        return CablePart.TIERS[c.tier].render();
    }

    private CablePart cableAt(Direction side, mods.eln.misc.LRDU lrdu) {
        int f = side.toEnumFacing().get3DDataValue(), a = side.applyLRDU(lrdu).toEnumFacing().get3DDataValue();
        for (VoxelPart p : parts())
            if (p instanceof CablePart c && !c.isKnuckle() && c.face == a)
                for (CablePart.Port port : c.cablePorts()) if (port.face() == f) return c;
        return null;
    }

    private int partsHash;
    private boolean partsChanged;
}
