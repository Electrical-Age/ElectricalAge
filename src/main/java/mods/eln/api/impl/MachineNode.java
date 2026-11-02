package mods.eln.api.impl;

import mods.eln.Eln;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.TripCause;
import mods.eln.bus.BusNet;
import mods.eln.bus.BusPeer;
import mods.eln.bus.BusPort;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.IDestructable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The node of a machine hosted at another mod's block entity (see MachineRegistry): never saved, tied to the block
 * entity object. Its machine is an ElementHost on these lists. Every face is a leaf: each power face has a terminal
 * of its own that its cables join, and one ElectricalConnection (the selector) joins one of them to the machine's
 * power node, so no current passes between faces. The selector holds its face until that face loses its cable, or
 * its line (under 2% of nominal) while another face's is live; then the connected face with the highest volts takes
 * over (chosen between steps). Signals use the host's gates; the bus face links to a voxel bus cable beside it
 * (BusPeer), never as a NodeConnection.
 */
public final class MachineNode extends GhostNode implements BusPeer {
    /** The node's own uuid (in no save; publishing is off). */
    public static final String UUID = "h";

    final ServerLevel level;
    final BlockEntity be;
    final MachineData data;
    final ElementHost host;
    private final MachineSpec spec;
    private final NbtElectricalLoad[] terminals = new NbtElectricalLoad[RelativeFace.values().length];
    private final BusPort busPort;
    private final ArrayList<NbtElectricalLoad> loads = new ArrayList<>();
    private final ArrayList<Component> components = new ArrayList<>();
    private final ArrayList<IProcess> slow = new ArrayList<>(), slowPre = new ArrayList<>();
    private Direction front;
    private RelativeFace active;
    private ElectricalConnection selector;
    private double ambientCelsius = Double.NaN;
    private boolean burning;
    private MachineCore.Saved lastSaved;
    private MachineData.Snapshot sent;
    private int sinceSync, connects;

    MachineNode(ServerLevel level, BlockEntity be, MachineSpec spec, MachineData data) {
        this.level = level;
        this.be = be;
        this.data = data;
        this.spec = spec;
        coordonate = new Coordonate(be.getBlockPos(), level);
        front = HostFaces.front(be.getBlockState());
        busPort = spec.busFace().isPresent() ? new BusPort() : null;
        for (RelativeFace f : spec.powerFaces()) {
            NbtElectricalLoad t = new NbtElectricalLoad("api.host.face" + f.ordinal());
            t.setRs(ElnDeviceRegistry.getSmallRs());
            terminals[f.ordinal()] = t;
            loads.add(t);
        }
        slowPre.add(dt -> select());   // (before the host plans its step)
        host = new ElementHost(spec, new Site(), busPort, loads, components, slow, slowPre);
        slow.add(dt -> afterStep());   // (after the host books the step: ElementHost's process is first)
        if (data.saved != null) host.loadState(data.saved);
    }

    /** Joins the world: into NodeManager, connected, and its EA neighbours reconnected (they stay loaded while it isn't). */
    void place() {
        neighborBlockRead();
        NodeManager.instance.addNode(this);
        connect();
        for (Direction d : Direction.values()) {
            NodeBase n = getNeighbor(d);
            if (n != null && n.isAdded() && !(n instanceof MachineNode)) n.reconnect();
        }
    }

    /** Leaves the world (the block entity went): its state into the attachment, then out of the simulation. */
    void retire() {
        data.saved = data.stateTag();
        data.node = null;
        if (NodeManager.instance.getNodeFromCoordonate(coordonate) == this) onBreakBlock();
        else disconnect();   // (already out of the node list: its dimension unloaded)
    }

    /** Reconnects if the block was turned (a state change that keeps the facing changes nothing). */
    void followFacing() {
        Direction f = HostFaces.front(be.getBlockState());
        if (f == front) return;
        front = f;
        reconnect();
    }

    public Direction front() {
        return front;
    }

    /** The power face the machine draws through now, or null. */
    public RelativeFace activeFace() {
        return active;
    }

    /** How many times the node has connected (tests: a block state change must not reconnect it). */
    public int connects() {
        return connects;
    }

    /** The terminal of power face f (tests), or null. */
    public ElectricalLoad terminal(RelativeFace f) {
        return terminals[f.ordinal()];
    }

    // ---- node

    @Override
    public String getNodeUuid() {
        return UUID;
    }

    @Override
    public void initializeFromThat(Direction front, LivingEntity entityLiving, ItemStack itemStack) {
    }

    @Override
    public void initializeFromNBT() {
    }

    /** Only what is loaded: a hosted block at a chunk's edge must not load the next chunk. Opaque faces don't matter here. */
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

    /** No client node to publish to (the block is another mod's): clear the flag only. */
    @Override
    public void publishToAllPlayer() {
        setNeedPublish(false);
    }

    private RelativeFace faceOf(Direction side) {
        return side == null ? null : Faces.relative(side, front);
    }

    @Override
    public int getSideConnectionMask(Direction side, LRDU lrdu) {
        RelativeFace f = faceOf(side);
        if (f == null || spec.busFace().orElse(null) == f) return 0;
        return host.connectionMask(side, lrdu);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        RelativeFace f = faceOf(side);
        if (f == null) return null;
        if (terminals[f.ordinal()] != null) return terminals[f.ordinal()];
        return host.electricalLoad(side, lrdu);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public BusPort busPortOn(Direction side) {
        return busPort != null && faceOf(side) == spec.busFace().orElse(null) ? busPort : null;
    }

    @Override
    public void connectJob() {
        if (isDestructing()) return;
        connects++;
        ambientCelsius = mods.eln.sim.Ambient.celsius(level, coordonate.pos());
        Eln.simulator.addAllSlowProcess(slow);
        for (IProcess p : slowPre) Eln.simulator.addSlowPreProcess(p);
        Eln.simulator.addAllElectricalComponent(components);
        for (NbtElectricalLoad l : loads) Eln.simulator.addElectricalLoad(l);
        super.connectJob();
        select();
        if (busPort != null) {
            Direction side = Faces.resolve(spec.busFace().orElseThrow(), front);
            if (getNeighbor(side) instanceof VoxelNode v && v.isAdded())
                for (LRDU l : LRDU.values()) BusNet.link(busPort, v.busPortAt(side.getInverse(), l));
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        setActive(null);
        Eln.simulator.removeAllSlowProcess(slow);
        for (IProcess p : slowPre) Eln.simulator.removeSlowPreProcess(p);
        Eln.simulator.removeAllElectricalComponent(components);
        for (NbtElectricalLoad l : loads) Eln.simulator.removeElectricalLoad(l);
        if (busPort != null) BusNet.detach(List.of(busPort));
    }

    // ---- the selector

    private boolean connected(RelativeFace f) {
        Direction side = Faces.resolve(f, front);
        for (LRDU l : LRDU.values()) if (lrduCubeMask.get(side, l)) return true;
        return false;
    }

    private boolean live(RelativeFace f) {
        return Math.abs(terminals[f.ordinal()].getU()) >= MachineCore.UNPOWERED_RATIO * spec.nominalVolts();
    }

    /** Keeps the active face while it has its cable and (a consumer) its line; else picks again. */
    private void select() {
        boolean producer = spec.kind() == MachineSpec.Kind.PRODUCER;
        if (active != null && connected(active) && (producer || live(active))) return;
        RelativeFace pick = null;
        double best = -1;
        for (RelativeFace f : spec.powerFaces()) {
            if (!connected(f)) continue;
            double u = Math.abs(terminals[f.ordinal()].getU());
            if (u > best) {
                pick = f;
                best = u;
            }
        }
        // a dead line on a face still connected is kept unless another face's line is live
        if (active != null && connected(active) && !(pick != null && live(pick))) return;
        setActive(pick);
    }

    private void setActive(RelativeFace f) {
        if (f == active && (f == null) == (selector == null)) return;
        if (selector != null) Eln.simulator.removeElectricalComponent(selector);
        selector = null;
        active = f;
        if (f != null) {
            selector = new ElectricalConnection(host.powerLoad(), terminals[f.ordinal()]);
            Eln.simulator.addElectricalComponent(selector);
        }
    }

    // ---- after each step: the attachment's save and sync

    /** Marks the chunk unsaved when the machine's saved state moved; syncs clients on a state change, else at most once a second. */
    private void afterStep() {
        MachineCore.Saved s = host.core().snapshot();
        if (!s.equals(lastSaved)) {
            lastSaved = s;
            var chunk = level.getChunkSource().getChunkNow(coordonate.x >> 4, coordonate.z >> 4);
            if (chunk != null) chunk.markUnsaved();
        }
        MachineData.Snapshot v = MachineData.Snapshot.of(host);
        sinceSync++;
        if (sent == null || v.state() != sent.state() || sinceSync >= 20 && !v.near(sent)) {
            sent = v;
            sinceSync = 0;
            be.syncData(HostedMachines.DATA.get());
        }
    }

    /** The host's view of this node. */
    private final class Site implements ElementSite {
        @Override
        public Direction front() {
            return front;
        }

        @Override
        public boolean connected(Direction side, LRDU lrdu) {
            return lrduCubeMask.get(side, lrdu);
        }

        @Override
        public void checkThread() {
            ServerThread.check();
        }

        @Override
        public boolean burnt() {
            return burning;
        }

        /** EA can't burn another mod's block in place: it is destroyed (no drops) at the end of the tick. */
        @Override
        public void burn(TripCause cause, double severity) {
            if (burning) return;
            burning = true;
            BlockPos pos = coordonate.pos();
            Eln.simulator.queueDestruct(new IDestructable() {
                @Override
                public void destructImpl() {
                    if (level.getBlockEntity(pos) == be) level.destroyBlock(pos, false);
                }

                @Override
                public String describe() {
                    return "machine at " + pos + " (" + cause + ")";
                }
            });
        }

        @Override
        public double powerRs() {
            return ElnDeviceRegistry.getSmallRs();
        }

        @Override
        public double ambientCelsius() {
            return ambientCelsius;
        }

        @Override
        public double signalRs() {
            return ElnDeviceRegistry.signalCableDescriptor.electricalRs;
        }
    }
}
