package mods.eln.node.transparent;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.LRDU;
import mods.eln.node.ElnRenders;

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

    // ---- client: the element render (1.12 TransparentNodeEntity, P3)

    /** Client: the device's render (null = none, or not ported yet). */
    public TransparentNodeElementRender elementRender;

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            short id = stream.readShort();
            if (id == 0) {
                clientDestroy();
                elementRenderId = 0;
                return;
            }
            if (id != elementRenderId || elementRender == null) {
                elementRenderId = id;
                clientDestroy();
                TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(id);
                ElnRenders.Transparent f = descriptor == null ? null : ElnRenders.transparent(descriptor);
                if (f != null) elementRender = f.create(this, descriptor);
            }
            if (elementRender != null) elementRender.networkUnserialize(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        if (elementRender != null) elementRender.serverPacketUnserialize(stream);
    }

    @Override
    public void preparePacketForServer(java.io.DataOutputStream stream) {
        super.preparePacketForServer(stream);
        try {
            stream.writeShort(elementRenderId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        return elementRender == null ? null : elementRender.getCableRender(side, lrdu);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return elementRender == null ? super.cameraDrawOptimisation() : elementRender.cameraDrawOptimisation();
    }

    @Override
    public void clientRefresh(float deltaT) {
        if (elementRender != null) {
            elementRender.refresh(deltaT);
            if (elementRender.burnt) burntSmoke();
        }
    }

    @Override
    public void tileEntityNeighborSpawn() {
        if (elementRender != null) elementRender.notifyNeighborSpawn();
    }

    @Override
    public void clientDestroy() {
        if (elementRender != null) elementRender.destructor();
        elementRender = null;
    }
}
