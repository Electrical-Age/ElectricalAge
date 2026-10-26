package mods.eln.node.six;

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

/**
 * Block entity of a six-node (1.12 SixNodeEntity). Server: everything goes through the SixNode. Client: which faces
 * hold an element (for the shape) and, from P3, the element renders.
 */
public class SixNodeEntity extends NodeBlockEntity {
    /** Client: 1.12 element ids per face, from the publish packet (0 = none). */
    public final short[] elementRenderIdList = new short[SixNode.SLOTS];

    public SixNodeEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.SIX_NODE_ENTITY.get(), pos, state);
    }

    /** For subclasses with their own type (VoxelNodeEntity). */
    protected SixNodeEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** A voxel node's parts, sent before the elements (SixNode.publishBeforeElements). */
    protected void readBeforeElements(DataInputStream stream) throws IOException {
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

    /** How many devices the block holds (every slot): server from the node, client from the last publish. */
    public int deviceCount() {
        int n = 0;
        if (level != null && !level.isClientSide()) {
            if (getNode() instanceof SixNode s) for (SixNodeElement e : s.sideElementList) if (e != null) n++;
        } else {
            for (short id : elementRenderIdList) if (id != 0) n++;
        }
        return n;
    }

    @Override
    public int isProvidingWeakPower(Direction side) {
        if (level == null || level.isClientSide()) return 0;
        NodeBase n = getNode();
        return n instanceof SixNode ? ((SixNode) n).isProvidingWeakPower(side) : 0;
    }

    // ---- client: element renders (1.12 SixNodeEntity, P3)

    /** Client: the render of each face's element (null = none, or no render ported yet). */
    public final SixNodeElementRender[] elementRenderList = new SixNodeElementRender[SixNode.SLOTS];
    private static boolean warnedMissingRender;

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            readBeforeElements(stream);
            stream.readInt();  // cover block (1.12 sixNodeCacheBlock; not ported)
            stream.readByte(); // cover meta
            for (int idx = 0; idx < 6; idx++) {
                short id = stream.readShort();
                if (id == 0) {
                    if (elementRenderList[idx] != null) elementRenderList[idx].destructor();
                    elementRenderIdList[idx] = 0;
                    elementRenderList[idx] = null;
                    continue;
                }
                if (id != elementRenderIdList[idx] || elementRenderList[idx] == null) {
                    elementRenderIdList[idx] = id;
                    if (elementRenderList[idx] != null) elementRenderList[idx].destructor();
                    elementRenderList[idx] = null;
                    SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(id);
                    ElnRenders.Six f = descriptor == null ? null : ElnRenders.six(descriptor);
                    if (f != null) elementRenderList[idx] = f.create(this, Direction.fromInt(idx), descriptor);
                }
                if (elementRenderList[idx] == null) {
                    // no render to parse this element's payload: the faces after it can't be read this time
                    if (!warnedMissingRender) {
                        warnedMissingRender = true;
                        Eln.LOG.warn("No render for six-node element {} (P3 in progress); further faces skipped", id);
                    }
                    return;
                }
                elementRenderList[idx].publishUnserialize(stream);
            }
            readSlots(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** The slots past the six faces (SixNode.publishSerialize): devices placed off centre. */
    private void readSlots(DataInputStream stream) throws IOException {
        boolean[] seen = new boolean[SixNode.SLOTS];
        int n = stream.readUnsignedByte();
        for (int k = 0; k < n; k++) {
            int slot = stream.readUnsignedByte();
            Direction face = Direction.fromInt(stream.readByte());
            int ox = stream.readByte(), oy = stream.readByte(), oz = stream.readByte(), pins = stream.readByte();
            short id = stream.readShort();
            byte[] payload = new byte[stream.readInt()];
            stream.readFully(payload);
            if (slot < 6 || slot >= SixNode.SLOTS || face == null) continue;
            seen[slot] = true;
            SixNodeElementRender r = elementRenderList[slot];
            if (id != elementRenderIdList[slot] || r == null || r.side != face) {
                if (r != null) r.destructor();
                r = null;
                elementRenderIdList[slot] = id;
                SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(id);
                ElnRenders.Six f = descriptor == null ? null : ElnRenders.six(descriptor);
                if (f != null) r = f.create(this, face, descriptor);
                elementRenderList[slot] = r;
            }
            if (r == null) continue;
            r.slot = slot;
            r.ox = ox;
            r.oy = oy;
            r.oz = oz;
            r.pinMask = pins;
            r.publishUnserialize(new DataInputStream(new java.io.ByteArrayInputStream(payload)));
        }
        for (int slot = 6; slot < SixNode.SLOTS; slot++) {
            if (seen[slot]) continue;
            if (elementRenderList[slot] != null) elementRenderList[slot].destructor();
            elementRenderList[slot] = null;
            elementRenderIdList[slot] = 0;
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            int side = stream.readByte();
            int id = stream.readShort();
            if (side >= 0 && side < SixNode.SLOTS && elementRenderIdList[side] == id && elementRenderList[side] != null)
                elementRenderList[side].serverPacketUnserialize(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        side = side.applyLRDU(lrdu);
        SixNodeElementRender r = elementRenderList[side.getInt()];
        return r == null ? null : r.getCableRender(lrdu);
    }

    @Override
    public int getCableDry(Direction side, LRDU lrdu) {
        side = side.applyLRDU(lrdu);
        SixNodeElementRender r = elementRenderList[side.getInt()];
        return r == null ? 0 : r.getCableDry(lrdu);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        for (SixNodeElementRender e : elementRenderList) if (e != null && !e.cameraDrawOptimisation()) return false;
        return true;
    }

    @Override
    public void clientRefresh(float deltaT) {
        for (SixNodeElementRender e : elementRenderList) {
            if (e == null) continue;
            e.refresh(deltaT);
            if (e.burnt) burntSmoke();
        }
    }

    @Override
    public void tileEntityNeighborSpawn() {
        for (SixNodeElementRender e : elementRenderList) if (e != null) e.notifyNeighborSpawn();
    }

    @Override
    public void clientDestroy() {
        for (int i = 0; i < SixNode.SLOTS; i++) {
            if (elementRenderList[i] != null) elementRenderList[i].destructor();
            elementRenderList[i] = null;
        }
    }
}
