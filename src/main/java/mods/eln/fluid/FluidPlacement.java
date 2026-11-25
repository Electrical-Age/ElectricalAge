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

    /**
     * A device the wrench turns (TransparentNodeElement.wrenchTurn turns any Turnable): the fronts it may take, any
     * horizontal one unless it says otherwise, and its ports remade from its front (in initialize too).
     */
    public interface Turnable {
        default Direction[] fronts() {
            return horizontal();
        }

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

    /** Its ports that couple to a port of whatever is beside them (a pipe's end, a device's flange): partners. */
    public static int coupled(TransparentNodeElement e, FluidDevice d) {
        int n = 0;
        var level = e.node.coordonate.world();
        for (FluidDevice.FluidPort p : d.fluidPorts()) if (!partners(level, p).isEmpty()) n++;
        return n;
    }

    /**
     * The ports in the world that p couples to (FluidWorld.couples): of the block in front of its face, whatever node is
     * there. Placement asks this, whether or not the nodes are in the fluid world now (a Stasis-frozen one isn't).
     */
    public static java.util.List<FluidDevice.FluidPort> partners(net.minecraft.world.level.Level level, FluidDevice.FluidPort p) {
        java.util.List<FluidDevice.FluidPort> out = new java.util.ArrayList<>();
        for (FluidDevice.FluidPort q : portsAt(level, net.minecraft.core.BlockPos.of(FluidWorld.facing(p)))) if (FluidWorld.couples(p, q)) out.add(q);
        return out;
    }

    /**
     * A block entity with fluid ports that is not an EA node (the Tank Loading Station: a rail, so a vanilla block): its
     * ports as pipes are placed against them, whether or not it is in the fluid world now.
     */
    public interface PortBlockEntity {
        java.util.List<FluidDevice.FluidPort> placementPorts();
    }

    /** The fluid ports on a block: a voxel block's pipe ends, a device's ports, a multiblock's ports on that block, a PortBlockEntity's. */
    public static java.util.List<FluidDevice.FluidPort> portsAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        java.util.List<FluidDevice.FluidPort> ports = new java.util.ArrayList<>();
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        if (n instanceof mods.eln.node.voxel.VoxelNode v) ports.addAll(v.fluidPorts());
        // (a multiblock's ports name their own blocks, at its node or on its ghost blocks: asked before FluidDevice, which
        // it is too and would hand out all its ports at its node)
        else if (n instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof GhostPortHost h) ports.addAll(h.portsAt(pos.asLong()));
        else if (n instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof FluidDevice d) ports.addAll(d.fluidPorts());
        else if (n instanceof GhostPortHost g) ports.addAll(g.portsAt(pos.asLong()));
        // (a block that is no EA node: a block entity with flanges, the Tank Loading Station, which must be a rail; only
        // in a loaded chunk, which getBlockEntity would load)
        else if (n == null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PortBlockEntity b) ports.addAll(b.placementPorts());
        return ports;
    }
}
