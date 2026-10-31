package mods.eln.fluid;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.fluid.FluidDevice;

/**
 * Fluid devices that the wrench turns (Baughn 2026-10-31). They used to turn themselves and their neighbours on placement
 * to meet the most ports (AdventurAgent, 2026-10-29: an LP turbine and a plant condenser placed side by side never
 * coupled), but that turned machines the player had set, and nothing turned them back. Now a device stays as placed,
 * facing the player, and the wrench key with a click gives it a quarter turn; its ports follow.
 */
public final class FluidPlacement {
    private FluidPlacement() {
    }

    /** A device the wrench turns: the fronts it may take, and its ports remade from its front. */
    public interface Turnable {
        Direction[] fronts();

        void remakePorts();
    }

    /** The four horizontal fronts: a lone device may take any. */
    public static Direction[] horizontal() {
        return new Direction[]{Direction.XN, Direction.ZN, Direction.XP, Direction.ZP};
    }

    /** A quarter turn to the next front it may take (going round by right()); false when it may take no other. */
    public static boolean turn(TransparentNodeElement e) {
        if (!(e instanceof Turnable t)) return false;
        var allowed = java.util.List.of(t.fronts());
        Direction f = e.front;
        for (int i = 0; i < 3; i++) {
            f = f.right();
            if (allowed.contains(f)) {
                settle(e, t, e.front, f);
                return true;
            }
        }
        return false;
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
