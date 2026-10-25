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
        var b = net.neoforged.neoforge.model.data.ModelData.builder().with(PARTS, clientParts).with(HOST, clientHost).with(LUGS, clientLugs);
        if (clientCarved != null && !clientCarved.isEmpty()) b.with(CARVED, hostBoxes());
        return b.build();
    }

    /**
     * Model data: the terminal plates beside the block (CableTerminals), the conductor height (px) per horizontal face
     * (index 3D value - 2; 0 = none). A floor cable ending at one draws a lug there, as six-node cables do (Baughn: the
     * steam turbine showed a bare cable end; the same code for every machine).
     */
    public static final net.neoforged.neoforge.model.data.ModelProperty<float[]> LUGS = new net.neoforged.neoforge.model.data.ModelProperty<>();

    /** Client: the terminal heights the mesh was built with; rechecked twice a second (a device's front can come later). */
    public volatile float[] clientLugs = new float[4];
    private float lugCheck;

    @Override
    public void clientRefresh(float deltaT) {
        super.clientRefresh(deltaT);
        lugCheck -= deltaT;
        if (lugCheck > 0) return;
        lugCheck = 0.5f;
        float[] k = lugHeights();
        if (java.util.Arrays.equals(k, clientLugs)) return;
        clientLugs = k;
        requestModelDataUpdate();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_IMMEDIATE);
    }

    /** The terminal heights beside the faces a floor cable here ends on (0 elsewhere). */
    float[] lugHeights() {
        float[] k = new float[4];
        if (level == null) return k;
        for (VoxelPart p : clientParts)
            if (p instanceof CablePart c && !c.isKnuckle() && c.face == 0)
                for (CablePart.Port port : c.cablePorts()) {
                    int f = port.face();
                    if (f < 2 || k[f - 2] > 0) continue;
                    var d = net.minecraft.core.Direction.from3DDataValue(f);
                    var be = level.getBlockEntity(worldPosition.relative(d));
                    k[f - 2] = mods.eln.cable.CableTerminals.heightAt(be, Direction.fromEnumFacing(d.getOpposite()));
                }
        return k;
    }

    /** Model data: what's left of a carved block (boxes), drawn in its textures instead of its model. */
    public static final net.neoforged.neoforge.model.data.ModelProperty<List<int[]>> CARVED = new net.neoforged.neoforge.model.data.ModelProperty<>();

    /** Model data: the imitated block (Mimic), drawn with the parts. */
    public static final net.neoforged.neoforge.model.data.ModelProperty<BlockState> HOST = new net.neoforged.neoforge.model.data.ModelProperty<>();

    /** Has the chisel taken anything from the imitated block? */
    public boolean isCarved() {
        if (level != null && !level.isClientSide()) return getNode() instanceof VoxelNode v && v.carved != null && !v.carved.isEmpty();
        return clientCarved != null && !clientCarved.isEmpty();
    }

    /** Client: the carved-away voxels as last published, or null. */
    public volatile java.util.BitSet clientCarved;

    /**
     * The block's shapes and what's left of the host, kept until the parts, host or carving change (Baughn: holding the
     * chisel lagged everyone nearby; every collision and outline query, from every entity each tick, rebuilt them box by
     * box from the carved voxels, slower with each carve).
     */
    public record Shapes(int stamp, BlockState host, int sideMask, int devices, List<int[]> hostBoxes, net.minecraft.world.phys.shapes.VoxelShape outline,
                         net.minecraft.world.phys.shapes.VoxelShape collision, net.minecraft.world.phys.shapes.VoxelShape carvedHost,
                         net.minecraft.world.phys.shapes.VoxelShape outlineCarved, net.minecraft.world.phys.shapes.VoxelShape collisionCarved) {
    }

    private volatile Shapes shapes;

    public Shapes shapes() {
        boolean server = level != null && !level.isClientSide();
        VoxelNode v = server && getNode() instanceof VoxelNode n ? n : null;
        int stamp = server ? (v == null ? -1 : v.stamp) : partsHash;
        BlockState h = host();
        int mask = getSideMask();
        int devices = java.util.Arrays.hashCode(elementRenderIdList);
        Shapes c = shapes;
        if (c != null && c.stamp() == stamp && c.host() == h && c.sideMask() == mask && c.devices() == devices) return c;
        List<VoxelPart> ps = parts();
        List<int[]> hb = server ? (v == null ? List.of() : v.hostBoxes()) : clientHostBoxes();
        // (each Shapes.or optimises its result again: AdventurAgent measured 2-3 us per box per query when only the parts
        // were kept; so the whole outline and collision of a carved block are kept too)
        var outline = VoxelNodeBlock.outlineOf(ps, deviceBoxes());
        var collision = VoxelNodeBlock.shapeOf(ps, 0);
        var carvedHost = VoxelNodeBlock.union(hb, 0);
        c = new Shapes(stamp, h, mask, devices, List.copyOf(hb), outline, collision, carvedHost,
            net.minecraft.world.phys.shapes.Shapes.or(outline, carvedHost), net.minecraft.world.phys.shapes.Shapes.or(collision, carvedHost));
        shapes = c;
        return c;
    }

    /** The model box of the device on each face (EA Direction int), null where there is none: server from the node,
     *  client from the last publish. */
    public int[][] deviceBoxes() {
        int[][] r = new int[6][];
        boolean server = level != null && !level.isClientSide();
        VoxelNode v = server && getNode() instanceof VoxelNode n ? n : null;
        for (int f = 0; f < 6; f++) {
            if (server) {
                if (v != null && v.sideElementList[f] != null) r[f] = v.elementBox(f);
            } else if (elementRenderIdList[f] != 0) {
                var d = mods.eln.Eln.sixNodeItem.getDescriptor(elementRenderIdList[f]);
                r[f] = d == null ? VoxelNode.plate(f) : d.footprintBox(f);
            }
        }
        return r;
    }

    /** What's left of the imitated block, as boxes: server from the node, client from the last publish. */
    public List<int[]> hostBoxes() {
        return shapes().hostBoxes();
    }

    private List<int[]> clientHostBoxes() {
        BlockState h = clientHost;
        if (h == null || level == null) return List.of();
        List<int[]> boxes = Mimic.boxes(h, level, worldPosition);
        java.util.BitSet c = clientCarved;
        if (c == null || c.isEmpty()) return boxes;
        java.util.BitSet s = Mimic.voxels(boxes);
        s.andNot(c);
        return Mimic.boxes(s);
    }

    /** Client: the imitated block as last published, or null. */
    public volatile BlockState clientHost;

    /** The imitated block: server from the node, client from the last publish. */
    public BlockState host() {
        if (level != null && !level.isClientSide()) return getNode() instanceof VoxelNode v ? v.host : null;
        return clientHost;
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
            int hostId = stream.readInt();
            byte[] cv = new byte[stream.readShort()];
            stream.readFully(cv);
            clientCarved = cv.length == 0 ? null : java.util.BitSet.valueOf(cv);
            int n = stream.readShort();
            int hash = n * 7919 + hostId + 31 * java.util.Arrays.hashCode(cv);
            clientHost = hostId == 0 ? null : net.minecraft.world.level.block.Block.stateById(hostId);
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
