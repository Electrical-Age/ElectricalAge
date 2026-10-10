package mods.eln.node.transparent;

import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.io.DataInputStream;
import java.io.IOException;

/** Block entity of a transparent-node device (1.12 TransparentNodeEntity). The element render comes with P3. */
public class TransparentNodeEntity extends NodeBlockEntity {
    /** Client: the 1.12 element id from the last publish. */
    public short elementRenderId;

    public TransparentNodeEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), pos, state);
    }

    @Override
    public String getNodeUuid() {
        return "t";
    }

    @Override
    public int isProvidingWeakPower(Direction side) {
        if (level == null || level.isClientSide()) return 0;
        NodeBase n = getNode();
        return n instanceof TransparentNode ? ((TransparentNode) n).isProvidingWeakPower(side) : 0;
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            elementRenderId = stream.readShort();
            // the element's payload follows, for its render (P3)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
