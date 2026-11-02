package mods.eln.api.impl.lowlevel;

import mods.eln.api.impl.ApiTrace;
import mods.eln.api.lowlevel.Load;
import mods.eln.api.lowlevel.PortKind;
import mods.eln.api.lowlevel.PortLink;
import mods.eln.api.lowlevel.PortPosition;
import mods.eln.api.lowlevel.PortRef;
import mods.eln.api.lowlevel.PortStatus;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Circuit.connect: a load's ElectricalConnection to an EA port, resolved before every step (the circuit's slow
 * pre-process, outside the electrical step). The connection is not in the port node's connection list, so the node
 * dropping its own links doesn't drop it: resolve() checks every tick that the node at the place is the same object,
 * in the simulation, and gives the same load for the port, and drops the link before any step if not (a link left
 * on a load removed from the simulation would pull that load back into the next solve).
 */
final class PortLinkImpl implements PortLink {
    private static final Logger LOG = LoggerFactory.getLogger("eln");
    private static final LRDU[] ANY = {LRDU.Left, LRDU.Right, LRDU.Up, LRDU.Down};

    private final CircuitImpl circuit;
    private final CircuitImpl.LoadImpl load;
    private final PortRef port;
    private final List<Consumer<PortStatus>> listeners = new ArrayList<>();
    private PortStatus status = PortStatus.NOT_HOSTED;
    private ElectricalConnection connection;
    private ElectricalLoad target;

    PortLinkImpl(CircuitImpl circuit, CircuitImpl.LoadImpl load, PortRef port) {
        this.circuit = circuit;
        this.load = load;
        this.port = port;
    }

    @Override
    public PortRef port() {
        return port;
    }

    @Override
    public Load load() {
        return load;
    }

    @Override
    public PortStatus status() {
        return status;
    }

    @Override
    public PortLink onStatus(Consumer<PortStatus> listener) {
        circuit.check();
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @Override
    public double amps() {
        return connection == null ? 0 : -connection.getCurrent(); // (made load to port: its current runs from the load)
    }

    @Override
    public void remove() {
        circuit.check();
        CircuitImpl.Site s = circuit.site();
        if (s != null) drop(s.simulator());
        circuit.forget(this);
        removed();
    }

    /** REMOVED, for good (the link was removed or its circuit retired). */
    void removed() {
        target = null;
        connection = null;
        setStatus(PortStatus.REMOVED, null);
    }

    /** The EA mask of a kind of port. */
    static int mask(PortKind kind) {
        return kind == PortKind.POWER ? NodeBase.maskElectricalPower : NodeBase.maskElectricalGate;
    }

    /** EA's LRDU for a position; ANY: null. */
    static LRDU lrdu(PortPosition p) {
        return switch (p) {
            case ANY -> null;
            case LEFT -> LRDU.Left;
            case RIGHT -> LRDU.Right;
            case UP -> LRDU.Up;
            case DOWN -> LRDU.Down;
        };
    }

    /**
     * Why a port is or isn't there, from what was found: a node at the place (or else whether its chunk is
     * loaded), the node in the simulation, a port of the link's kind, a port of another kind.
     */
    static PortStatus classify(boolean node, boolean chunkLoaded, boolean simulated, boolean ofKind, boolean otherKind) {
        if (!node) return chunkLoaded ? PortStatus.NO_NODE : PortStatus.NOT_LOADED;
        if (!simulated) return PortStatus.NOT_SIMULATED;
        if (ofKind) return PortStatus.CONNECTED;
        return otherKind ? PortStatus.WRONG_KIND : PortStatus.NO_PORT;
    }

    /** Before a step: finds the port as it is now and links to it (or drops a link that went stale). */
    void resolve(CircuitImpl.Site site) {
        if (status == PortStatus.REMOVED) return;
        ServerLevel level = site.level();
        if (level == null) {
            setStatus(PortStatus.NOT_HOSTED, null);
            return;
        }
        NodeBase node = NodeManager.instance == null ? null : NodeManager.instance.getNodeFromCoordonate(new Coordonate(port.pos(), level));
        ElectricalLoad found = null;
        boolean other = false;
        if (node != null && node.isAdded()) {
            Direction side = Direction.fromEnumFacing(port.side());
            LRDU at = lrdu(port.position());
            for (LRDU l : at == null ? ANY : new LRDU[]{at}) {
                found = node.getElectricalLoad(side, l, mask(port.kind()));
                if (found != null) break;
                if (node.getElectricalLoad(side, l) != null) other = true;
            }
        }
        PortStatus s = classify(node != null, node != null || level.isLoaded(port.pos()), node != null && node.isAdded(),
            found != null, other);
        if (found != target || found == null) {
            drop(site.simulator());
            if (found != null) {
                target = found;
                connection = new ElectricalConnection(load.state, found);
                site.simulator().addElectricalComponent(connection);
            }
        }
        setStatus(s, site);
    }

    /** Takes the link out of the simulation, if it is in. */
    void drop(Simulator sim) {
        if (connection != null) sim.removeElectricalComponent(connection);
        connection = null;
        target = null;
    }

    private void setStatus(PortStatus s, CircuitImpl.Site site) {
        if (s == status) return;
        status = s;
        if (ApiTrace.enabled())
            ApiTrace.log("port link %s of %s -> %s: %s (load %.3f V, port %s, link %s)", load.name, circuit, port, s, load.state.getU(),
                target == null ? "-" : String.format(java.util.Locale.ROOT, "%.3f V", target.getU()),
                target == null ? "-" : String.format(java.util.Locale.ROOT, "%.4g ohm", load.state.getRs() + target.getRs()));
        for (Consumer<PortStatus> l : List.copyOf(listeners)) {
            try {
                l.accept(s);
            } catch (RuntimeException e) { // one bad listener mustn't skip the others
                LOG.error("{}: a port link status listener threw on {}", circuit, s, e);
            }
        }
    }
}
