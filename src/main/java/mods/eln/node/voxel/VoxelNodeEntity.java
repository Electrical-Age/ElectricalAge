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
public class VoxelNodeEntity extends NodeBlockEntity {
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
    public int isProvidingWeakPower(Direction side) {
        return 0;
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            int n = stream.readShort();
            List<VoxelPart> ps = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String kind = stream.readUTF();
                byte[] b = new byte[stream.readShort()];
                stream.readFully(b);
                VoxelPart p = VoxelPart.deserialize(kind, new DataInputStream(new java.io.ByteArrayInputStream(b)));
                if (p != null) ps.add(p);
            }
            clientParts = ps;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
