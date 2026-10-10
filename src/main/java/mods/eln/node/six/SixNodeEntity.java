package mods.eln.node.six;

import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Block entity of a six-node (1.12 SixNodeEntity). Server: everything goes through the SixNode. Client: which faces
 * hold an element (for the shape) and, from P3, the element renders.
 */
public class SixNodeEntity extends NodeBlockEntity {
    /** Client: 1.12 element ids per face, from the publish packet (0 = none). */
    public final int[] elementRenderIdList = new int[6];

    public SixNodeEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.SIX_NODE_ENTITY.get(), pos, state);
    }

    @Override
    public String getNodeUuid() {
        return "s";
    }

    /** Faces holding an element: server from the node, client from the last publish. */
    public int getSideMask() {
        int mask = 0;
        if (level != null && !level.isClientSide()) {
            NodeBase n = getNode();
            if (n instanceof SixNode) {
                SixNode s = (SixNode) n;
                for (int i = 0; i < 6; i++) if (s.sideElementList[i] != null) mask |= 1 << i;
            }
        } else {
            for (int i = 0; i < 6; i++) if (elementRenderIdList[i] != 0) mask |= 1 << i;
        }
        return mask;
    }

    @Override
    public int isProvidingWeakPower(Direction side) {
        if (level == null || level.isClientSide()) return 0;
        NodeBase n = getNode();
        return n instanceof SixNode ? ((SixNode) n).isProvidingWeakPower(side) : 0;
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            stream.readInt();  // cover block (not ported yet)
            stream.readByte(); // cover meta
            // then per face: element id, and if non-zero the element's own payload, which only its render can
            // parse; the renders come with P3, so only the first face is read here
            elementRenderIdList[0] = stream.readShort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
