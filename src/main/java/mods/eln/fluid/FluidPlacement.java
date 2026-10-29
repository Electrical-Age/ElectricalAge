package mods.eln.fluid;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.fluid.FluidDevice;

/**
 * A device just placed turns to meet its neighbours' ports (AdventurAgent's client check, 2026-10-29: an LP turbine and
 * a plant condenser placed side by side never coupled: facing from the player's view put the LP's exhaust and the
 * condenser's inlets on other faces, and nothing in the world says which way a port faces). Of the fronts it may take
 * (a turbine: its own or its back, so its shaft stays along the view), it takes the one whose ports meet the most ports
 * of the devices and pipes already beside it; its own front when none does better. Then the devices beside it that may
 * turn get the same chance (a condenser placed first, its inlets away from where the LP then came), never losing a
 * coupling they had.
 */
public final class FluidPlacement {
    private FluidPlacement() {
    }

    /** A device that turns to meet its neighbours' ports: the fronts it may take, and its ports remade from its front. */
    public interface Turnable {
        Direction[] fronts();

        void remakePorts();
    }

    /**
     * Placed: E and each turnable device beside it take, together, the pair of fronts whose ports meet the most (a pair:
     * a condenser placed first with its inlets away from where an LP comes, the LP's exhaust away from it, meet only
     * when both turn), keeping what they had on a tie; then E alone against the rest (pipes, fixed devices).
     */
    public static void placed(TransparentNodeElement e) {
        if (!(e instanceof Turnable te) || !(e instanceof FluidDevice de)) return;
        for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP, Direction.YN, Direction.YP})
            if (e.node.getNeighbor(d) instanceof mods.eln.node.transparent.TransparentNode n && n.element != e
                && n.element instanceof Turnable tn && n.element instanceof FluidDevice dn) {
                TransparentNodeElement ne = n.element;
                Direction e0 = e.front, n0 = ne.front, eBest = e0, nBest = n0;
                int most = coupled(e, de) + coupled(ne, dn);
                for (Direction ef : te.fronts())
                    for (Direction nf : tn.fronts()) {
                        e.front = ef;
                        ne.front = nf;
                        te.remakePorts();
                        tn.remakePorts();
                        int c = coupled(e, de) + coupled(ne, dn);
                        if (c > most) {
                            most = c;
                            eBest = ef;
                            nBest = nf;
                        }
                    }
                settle(e, te, e0, eBest);
                settle(ne, tn, n0, nBest);
            }
        // alone against what can't turn
        Direction placed = e.front, best = placed;
        int most = coupled(e, de);
        for (Direction c : te.fronts()) {
            e.front = c;
            te.remakePorts();
            int n = coupled(e, de);
            if (n > most) {
                most = n;
                best = c;
            }
        }
        settle(e, te, placed, best);
    }

    static void settle(TransparentNodeElement e, Turnable t, Direction was, Direction to) {
        e.front = to;
        t.remakePorts();
        if (to != was) {
            e.reconnect();
            e.needPublish();
        }
    }

    /** Its ports that meet a port of whatever is beside them (a pipe's end, a device's flange). */
    public static int coupled(TransparentNodeElement e, FluidDevice d) {
        int n = 0;
        var level = e.node.coordonate.world();
        for (FluidDevice.FluidPort p : d.fluidPorts()) {
            var f = net.minecraft.core.Direction.from3DDataValue(p.face());
            var there = net.minecraft.core.BlockPos.of(p.pos()).relative(f);
            var other = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(there, level));
            FluidDevice o = other instanceof FluidDevice fd ? fd
                : other instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof FluidDevice fe ? fe : null;
            if (o == null || o == d) continue;
            for (FluidDevice.FluidPort q : o.fluidPorts())
                if (q.pos() == there.asLong() && q.face() == f.getOpposite().get3DDataValue() && q.at() == p.at()) {
                    n++;
                    break;
                }
        }
        return n;
    }
}
