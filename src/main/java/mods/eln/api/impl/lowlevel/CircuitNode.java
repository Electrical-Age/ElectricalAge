package mods.eln.api.impl.lowlevel;

import mods.eln.Eln;
import mods.eln.api.RelativeFace;
import mods.eln.api.impl.ApiTrace;
import mods.eln.api.impl.Faces;
import mods.eln.api.impl.HostFaces;
import mods.eln.api.impl.MachineNode;
import mods.eln.api.impl.ServerThread;
import mods.eln.api.lowlevel.FaceConnection;
import mods.eln.api.lowlevel.HostedNode;
import mods.eln.api.lowlevel.PortKind;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumMap;
import java.util.Map;

/**
 * The node of a low-level circuit hosted at a block entity (Circuit.hostAt): never saved, tied to the block entity
 * object as a hosted machine's node is (MachineNode; CircuitHosts runs the lifecycle). Each given face offers its
 * load to cables, as power or a signal gate by the load's kind, at every cable position. Connected, its circuit is
 * attached to EA's simulator; disconnected, detached.
 */
final class CircuitNode extends GhostNode implements HostedNode {
    final ServerLevel level;
    final BlockEntity be;
    final CircuitImpl circuit;
    private final Map<RelativeFace, FaceConnection> faces = new EnumMap<>(RelativeFace.class);
    private Direction front;
    private boolean placed, retired;

    CircuitNode(ServerLevel level, BlockEntity be, CircuitImpl circuit, FaceConnection[] given) {
        this.level = level;
        this.be = be;
        this.circuit = circuit;
        for (FaceConnection f : given)
            if (faces.put(f.face(), f) != null) throw new IllegalArgumentException("face " + f.face() + " is given twice");
        coordonate = new Coordonate(be.getBlockPos(), level);
        front = HostFaces.front(be.getBlockState());
    }

    /** Joins the world: into NodeManager, connected, and its EA neighbours reconnected. */
    void place() {
        neighborBlockRead();
        NodeManager.instance.addNode(this);
        connect();
        placed = true;
        for (Direction d : Direction.values()) {
            NodeBase n = getNeighbor(d);
            if (n != null && n.isAdded() && !(n instanceof CircuitNode) && !(n instanceof MachineNode)) n.reconnect();
        }
        if (ApiTrace.enabled()) ApiTrace.log("host %s at %s facing %s: faces %s", circuit, blockPos(), front, faces.keySet());
    }

    /** Leaves the world for good (the block entity went), and the circuit with it. */
    void retire() {
        if (retired) return;
        retired = true;
        if (placed) {
            if (NodeManager.instance != null && NodeManager.instance.getNodeFromCoordonate(coordonate) == this) onBreakBlock();
            else disconnect();
        }
        circuit.retire();
        if (ApiTrace.enabled()) ApiTrace.log("retire %s at %s", circuit, blockPos());
    }

    /** The world stopped: retired with nothing to undo (its node manager and simulator are cleared). */
    void abandon() {
        retired = true;
        circuit.abandon();
    }

    /** Reconnects if the block was turned. */
    void followFacing() {
        Direction f = HostFaces.front(be.getBlockState());
        if (f == front) return;
        front = f;
        reconnect();
    }

    // ---- HostedNode

    @Override
    public BlockPos blockPos() {
        return coordonate.pos();
    }

    @Override
    public boolean hosted() {
        return placed && !retired;
    }

    @Override
    public boolean retired() {
        return retired;
    }

    @Override
    public boolean connected(RelativeFace face) {
        Direction side = Faces.resolve(face, front);
        for (LRDU l : LRDU.values()) if (lrduCubeMask.get(side, l)) return true;
        return false;
    }

    // ---- node

    @Override
    public String getNodeUuid() {
        return MachineNode.UUID;
    }

    @Override
    public void initializeFromThat(Direction front, LivingEntity entityLiving, ItemStack itemStack) {
    }

    @Override
    public void initializeFromNBT() {
    }

    /** Only what is loaded: a hosted block at a chunk's edge must not load the next chunk. */
    @Override
    public void neighborBlockRead() {
        neighborOpaque = 0;
        neighborWrapable = 0;
        for (Direction d : Direction.values()) {
            BlockPos np = coordonate.pos().relative(d.toEnumFacing());
            if (!level.isLoaded(np)) continue;
            if (isBlockWrappable(level.getBlockState(np), level, np)) neighborWrapable |= 1 << d.getInt();
        }
    }

    /** No client node to publish to (the block is another mod's). */
    @Override
    public void publishToAllPlayer() {
        setNeedPublish(false);
    }

    private FaceConnection at(Direction side) {
        return side == null ? null : faces.get(Faces.relative(side, front));
    }

    @Override
    public int getSideConnectionMask(Direction side, LRDU lrdu) {
        FaceConnection f = at(side);
        if (f == null) return 0;
        return f.kind() == PortKind.POWER ? maskElectricalPower : maskElectricalGate;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        FaceConnection f = at(side);
        return f == null ? null : circuit.own(f.load()).state();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public void connectJob() {
        if (isDestructing()) return;
        circuit.attach(new Site());
        super.connectJob();
        if (ApiTrace.enabled()) {
            StringBuilder b = new StringBuilder();
            for (RelativeFace f : faces.keySet()) b.append(' ').append(f).append(connected(f) ? "=linked" : "=open");
            ApiTrace.log("connect %s at %s:%s; %s", circuit, blockPos(), b, circuit.describe());
        }
    }

    @Override
    public void disconnectJob() {
        if (ApiTrace.enabled()) ApiTrace.log("disconnect %s at %s; %s", circuit, blockPos(), circuit.describe());
        super.disconnectJob();
        circuit.detach();
    }

    /** The game's site: EA's simulator, the server thread, a cable node's series resistances, this level. */
    private final class Site implements CircuitImpl.Site {
        @Override
        public Simulator simulator() {
            return Eln.simulator;
        }

        @Override
        public void checkThread() {
            ServerThread.check();
        }

        @Override
        public double powerRs() {
            return ElnDeviceRegistry.getSmallRs();
        }

        @Override
        public double signalRs() {
            return ElnDeviceRegistry.signalCableDescriptor.electricalRs;
        }

        @Override
        public ServerLevel level() {
            return level;
        }

        @Override
        public String where() {
            return blockPos().toShortString();
        }
    }
}
